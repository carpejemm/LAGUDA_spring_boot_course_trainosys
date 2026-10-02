package com.trainosys.ecom.app.security;

import com.trainosys.ecom.app.security.jwt.AuthEntryPointJwt;
import com.trainosys.ecom.app.security.jwt.AuthTokenFilter;
import com.trainosys.ecom.app.security.jwt.JwtUtils;
import com.trainosys.ecom.app.security.services.UserDetailsServiceImpl;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SECURITY CONFIG: the "rule book" of the whole Auth module (slide 25).
 *
 * <p>Spring Security puts a chain of filters in front of every controller. Every HTTP request
 * passes through that chain BEFORE it reaches ProductController, CartController, etc.
 * This class decides what the chain does:
 *
 * <pre>
 *   request
 *     -> AuthTokenFilter            (ours) reads the JWT, logs the user in for this request
 *     -> ... Spring's own filters ...
 *     -> URL rules (authorizeHttpRequests below)
 *          public?          -> let it through
 *          needs login?     -> no user  -> AuthEntryPointJwt -> 401 JSON
 *          needs ADMIN?     -> customer -> 403 Forbidden
 *     -> controller
 * </pre>
 *
 * <p>Without this class (step 0 of the guide) Spring Security locks EVERY URL, creates a
 * random password on each start, and blocks POST/PUT/DELETE with CSRF. Declaring our own
 * {@link SecurityFilterChain} bean replaces all of those defaults.
 *
 * <p>{@code @Configuration}: this class creates beans (the @Bean methods).
 * {@code @EnableWebSecurity}: switches on Spring Security's web support with OUR chain.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ---------------------------------------------------------------------------------------------
    // Dependencies (constructor injection, same style as our services)
    // ---------------------------------------------------------------------------------------------

    /** Creates and checks tokens. Needed by AuthTokenFilter. */
    private final JwtUtils jwtUtils;

    /** Loads a user from the database by username. Needed by AuthTokenFilter. */
    private final UserDetailsServiceImpl userDetailsService;

    /** Writes the JSON 401 when someone is not logged in. */
    private final AuthEntryPointJwt unauthorizedHandler;

    public SecurityConfig(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService,
                          AuthEntryPointJwt unauthorizedHandler) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    // ---------------------------------------------------------------------------------------------
    // Beans
    // ---------------------------------------------------------------------------------------------

    /**
     * PASSWORD ENCODER: BCrypt (slides 9-13).
     *
     * <p>Used in two places:
     * <ul>
     *   <li>Sign up / DataInitializer: {@code encode("admin123")} -> {@code "$2a$10$<22-char salt><hash>"}.
     *       Only this hash is saved. The plain password is never stored.</li>
     *   <li>Sign in: {@code matches("admin123", storedHash)}. Spring calls it for us inside
     *       the DaoAuthenticationProvider. It reads the salt back out of the stored hash.</li>
     * </ul>
     *
     * <p>Hashing is one-way: there is no "decode". Every call to encode() uses a new random salt,
     * so the same password gives a different hash every time (check USER_TABLE after a restart).
     * {@code $2a$} = bcrypt version, {@code 10} = cost factor (2^10 rounds; slower = harder to brute force).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AUTHENTICATION MANAGER: the object that answers "is this username + password correct?".
     *
     * <p>We never build it by hand. Spring sees exactly one {@code UserDetailsService} bean
     * ({@link UserDetailsServiceImpl}) and one {@code PasswordEncoder} bean (above), and wires them
     * into a {@code DaoAuthenticationProvider} automatically (slide 8):
     *
     * <pre>
     *   authenticationManager.authenticate(username, password)
     *     -> DaoAuthenticationProvider
     *         -> userDetailsService.loadUserByUsername(username)   (database)
     *         -> passwordEncoder.matches(password, user.getPassword())
     *     -> success: an Authentication with the user + roles
     *     -> failure: throws BadCredentialsException
     * </pre>
     *
     * <p>We only expose it as a bean so AuthController can inject it for /signin.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * THE SECURITY FILTER CHAIN: every rule for every request.
     *
     * <p>Read it top to bottom. Each {@code .something(...)} call configures one feature.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // -----------------------------------------------------------------------------
                // 1. CSRF OFF
                // CSRF (Cross-Site Request Forgery) protection guards apps that log in with a
                // browser SESSION cookie: another website could make your browser send a request
                // with your session. It blocks POST/PUT/DELETE without a CSRF token (403).
                // We are a stateless REST API: no session, so we turn it off.
                // (Step 0 of the guide: POST with the right password still failed because of this.)
                // -----------------------------------------------------------------------------
                .csrf(csrf -> csrf.disable())

                // -----------------------------------------------------------------------------
                // 2. WHAT TO ANSWER WHEN NOBODY IS LOGGED IN
                // A protected URL + no valid token -> AuthEntryPointJwt.commence() -> JSON 401.
                // Without this line Spring answers 403 for "not logged in" too, and the client
                // can't tell "sign in again" (401) from "you're not allowed" (403).
                // -----------------------------------------------------------------------------
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))

                // -----------------------------------------------------------------------------
                // 3. STATELESS
                // Spring never creates an HttpSession, so the server remembers NOTHING between
                // requests. Every request must bring its own proof: the JWT.
                // This is why JWT apps scale easily: any server with the secret can check a token.
                // -----------------------------------------------------------------------------
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // -----------------------------------------------------------------------------
                // 4. URL RULES (authorization)
                // Checked TOP TO BOTTOM, the FIRST match wins. So:
                //   - specific rules first, general rules last
                //   - anyRequest() must be the very last rule
                //
                //   permitAll()        -> anyone, logged in or not
                //   hasRole("ADMIN")   -> logged in AND has authority "ROLE_ADMIN"
                //                         (hasRole adds the "ROLE_" prefix by itself)
                //   authenticated()    -> logged in, any role
                // -----------------------------------------------------------------------------
                .authorizeHttpRequests(auth -> auth
                        // The H2 console is a separate servlet. PathRequest.toH2Console() matches its
                        // URLs (/h2-console/**) correctly. It has its own login (sa / blank).
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        // When a controller fails (e.g. validation -> 400), Spring FORWARDS to /error
                        // to build the JSON error. If /error were locked, every error would turn into 401.
                        .requestMatchers("/error").permitAll()
                        // You can't require a login to log in. Sign out is open so it works even
                        // with an expired token.
                        .requestMatchers("/api/auth/signin", "/api/auth/signup", "/api/auth/signout").permitAll()
                        // Browsing the catalogue is public, but only for GET.
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        // Any other method on products (POST, PUT, DELETE) reaches this rule: admins only.
                        // Least privilege (slide 6): customers only get what customers need.
                        .requestMatchers("/api/products/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        // Fail-safe default (slide 6): anything not listed above (cart, orders,
                        // /api/auth/username, /api/auth/user, and any endpoint we add later and forget)
                        // needs a login. Forgetting a rule locks a URL instead of opening it.
                        .anyRequest().authenticated())

                // -----------------------------------------------------------------------------
                // 5. H2 CONSOLE FRAMES
                // The H2 console draws its page with <frame>s. By default Spring Security sends
                // "X-Frame-Options: DENY" (protects against click-jacking), which shows a blank page.
                // sameOrigin() allows frames that come from our own server.
                // -----------------------------------------------------------------------------
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                // -----------------------------------------------------------------------------
                // 6. OUR JWT FILTER
                // Put AuthTokenFilter BEFORE Spring's UsernamePasswordAuthenticationFilter, so the
                // user from the token is already known when the URL rules run.
                // Created with "new" on purpose (not @Component): a @Component filter would ALSO be
                // registered by Spring Boot as a normal servlet filter, outside the security chain.
                // -----------------------------------------------------------------------------
                .addFilterBefore(new AuthTokenFilter(jwtUtils, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
