package com.trainosys.ecom.app.controller;

import com.trainosys.ecom.app.model.User;
import com.trainosys.ecom.app.model.UserRole;
import com.trainosys.ecom.app.repository.UserRepository;
import com.trainosys.ecom.app.security.jwt.JwtUtils;
import com.trainosys.ecom.app.security.request.LoginRequest;
import com.trainosys.ecom.app.security.request.SignupRequest;
import com.trainosys.ecom.app.security.response.MessageResponse;
import com.trainosys.ecom.app.security.response.UserInfoResponse;
import com.trainosys.ecom.app.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AUTH CONTROLLER: the Auth module's API (slide 27).
 *
 * <pre>
 *   Method  URL                   Access      Body            Response
 *   ------  --------------------  ----------  --------------  --------------------------------------------
 *   POST    /api/auth/signin      public      LoginRequest    200 UserInfoResponse + cookie | 401 Bad credentials
 *   POST    /api/auth/signup      public      SignupRequest   200 MessageResponse | 400 taken / invalid
 *   POST    /api/auth/signout     public      -               200 MessageResponse, cookie cleared
 *   GET     /api/auth/username    logged in   -               200 "admin" | 401
 *   GET     /api/auth/user        logged in   -               200 UserInfoResponse (no token) | 401
 * </pre>
 *
 * <p>"public" / "logged in" come from SecurityConfig's URL rules, not from this class.
 * Seeded accounts (DataInitializer): admin / admin123 (ADMIN), user1 / password123 (CUSTOMER).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    /** Checks username + password (UserDetailsServiceImpl + BCrypt, wired by Spring). */
    private final AuthenticationManager authenticationManager;
    /** Creates the token and the cookie. */
    private final JwtUtils jwtUtils;
    /** Sign up: check duplicates and save the new user. */
    private final UserRepository userRepository;
    /** Sign up: hash the password before saving. */
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                          UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * SIGN IN: check the password ONCE, then hand out a token for the next 24 hours.
     *
     * <pre>
     *   { "username": "admin", "password": "admin123" }
     *     -> authenticationManager.authenticate(...)
     *          -> UserDetailsServiceImpl.loadUserByUsername("admin")     database
     *          -> BCrypt matches("admin123", "$2a$10$...")
     *     -> JwtUtils.generateJwtCookie(user)                             new signed token
     *     -> 200 + Set-Cookie: trainosys-jwt=eyJ...; HttpOnly
     *        + { "id": 1, "username": "admin", "roles": ["ROLE_ADMIN"], "jwtToken": "eyJ..." }
     * </pre>
     *
     * <p>{@code ResponseEntity<?>}: success returns a UserInfoResponse, failure a MessageResponse.
     * {@code @Valid}: blank username or password -> 400 before this method runs.
     */
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication;
        try {
            // An "unauthenticated" token holding what the user typed. authenticate() checks it and
            // returns an "authenticated" one with the UserDetailsImpl and roles inside.
            // Loads the user (UserDetailsServiceImpl) and checks the password (PasswordEncoder)
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        } catch (AuthenticationException exception) {
            // Wrong password AND unknown username both land here with the SAME message,
            // so an attacker can't find out which usernames exist.
            return new ResponseEntity<>(new MessageResponse("Bad credentials"), HttpStatus.UNAUTHORIZED);
        }

        // Marks this request as logged in. The app is stateless, so this is forgotten after the
        // response: from now on the token proves who the user is.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // getPrincipal() is the UserDetailsImpl that UserDetailsServiceImpl built
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Generates the JWT and wraps it in an HttpOnly cookie (see JwtUtils)
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        // [ROLE_ADMIN] as plain Strings for the JSON
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Token goes out twice: as a cookie (browsers) and in the body (Postman's Bearer header)
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), roles, jwtCookie.getValue()));
    }

    /**
     * SIGN UP: create a new CUSTOMER account.
     *
     * <p>Order of checks:
     * <ol>
     *   <li>{@code @Valid}: SignupRequest rules (username 3-20, valid email, password 6-40) -> 400</li>
     *   <li>username already used -> 400 "Error: Username is already taken!"</li>
     *   <li>email already used -> 400 "Error: Email is already in use!"</li>
     *   <li>save with a BCrypt-hashed password -> 200 "User registered successfully!"</li>
     * </ol>
     *
     * <p>Sign up does NOT log the user in. The client calls /signin next to get a token.
     */
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        // Never save the plain-text password: "secret123" -> "$2a$10$<salt><hash>" (one-way)
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        // New accounts are always customers. Admins are created by DataInitializer.
        // (SignupRequest has no role field, so nobody can sign up as ADMIN.)
        user.setRole(UserRole.CUSTOMER);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * WHO AM I (username only). Needs a login (anyRequest().authenticated() in SecurityConfig).
     *
     * <p>{@code Authentication authentication}: Spring passes in what AuthTokenFilter put in the
     * SecurityContext for this request. {@code getName()} is the username from the token.
     */
    @GetMapping("/username")
    public String currentUserName(Authentication authentication) {
        return authentication.getName();
    }

    /**
     * WHO AM I (id, username, roles). Needs a login.
     *
     * <p>{@code @AuthenticationPrincipal UserDetailsImpl userDetails}: Spring passes in the
     * logged-in user, already cast to our class, so we can call getId(). This is the same way
     * CartController and OrderController find "my" cart, instead of trusting an X-User-ID header.
     * On a permitAll() URL with no token it would be null.
     */
    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> getUserDetails(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        // No new token here, so jwtToken is null
        return ResponseEntity.ok(new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), roles, null));
    }

    /**
     * SIGN OUT: tell the browser to delete the cookie
     * ({@code Set-Cookie: trainosys-jwt=; Path=/api; Max-Age=0}).
     *
     * <p>Important: the token itself is NOT cancelled. Stateless means the server keeps no list of
     * tokens, so there is nothing to delete on the server. A token copied before sign out still works
     * until it expires. Real systems use short-lived tokens + refresh tokens, or a deny list.
     * In Postman, also clear {{jwtToken}}.
     */
    @PostMapping("/signout")
    public ResponseEntity<MessageResponse> signoutUser() {
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You've been signed out!"));
    }
}
