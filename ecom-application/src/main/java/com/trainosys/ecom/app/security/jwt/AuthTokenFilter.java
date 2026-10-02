package com.trainosys.ecom.app.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * AUTH TOKEN FILTER: turns a JWT into a logged-in user, on every request (slide 23).
 *
 * <p>Because the app is STATELESS, the server forgets who you are after every response.
 * This filter "logs you in again" for each request, using the token you send:
 *
 * <pre>
 *   GET /api/cart   Authorization: Bearer eyJ...
 *     1. parseJwt()             -> "eyJ..."                 (header first, then cookie)
 *     2. validateJwtToken()     -> signature OK, not expired
 *     3. getUserNameFromJwt()   -> "user1"
 *     4. loadUserByUsername()   -> UserDetailsImpl(id=2, ROLE_CUSTOMER)   (fresh from the database)
 *     5. SecurityContextHolder  -> "this request is user1, role CUSTOMER"
 *     6. filterChain.doFilter() -> continue to the URL rules and the controller
 * </pre>
 *
 * <p><b>The filter never blocks a request.</b> No token, bad token, expired token: it just skips
 * steps 2-5 and continues. It only answers "WHO is this?". SecurityConfig's URL rules then decide
 * "is that enough?" (public URL -> OK, protected -> 401 via AuthEntryPointJwt, wrong role -> 403).
 *
 * <p>{@code OncePerRequestFilter}: Spring guarantees doFilterInternal() runs once per request,
 * even if the request is forwarded internally (e.g. to /error).
 *
 * <p>Not a {@code @Component}: SecurityConfig creates it with {@code new} and adds it to the
 * security chain. (A @Component filter would also be registered as a plain servlet filter.)
 */
public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    /** Reads and checks the token. */
    private final JwtUtils jwtUtils;

    /** Loads the user and roles from the database (our UserDetailsServiceImpl). */
    private final UserDetailsService userDetailsService;

    public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Runs for EVERY request (public ones too), before any controller.
     *
     * @param filterChain the rest of the chain. Calling {@code doFilter} passes the request on.
     *                    Forgetting it = the request never reaches the controller (empty response).
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Find the token (null if there is none)
            String jwt = parseJwt(request);

            // 2. Only continue with a genuine, unexpired token
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // 3. The token only carries the username ("sub")
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                // 4. Load the CURRENT user and roles from the database.
                //    If the user was deleted (e.g. a signed-up user after an H2 restart), this throws
                //    UsernameNotFoundException -> caught below -> request stays anonymous -> 401.
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. Build Spring Security's "logged in" object:
                //    principal   = userDetails (controllers get it with @AuthenticationPrincipal)
                //    credentials = null, the token already proved who this is (no password here)
                //    authorities = the roles, e.g. [ROLE_ADMIN], used by hasRole("ADMIN")
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // Extra info about the request (client IP address, session id) for logging/auditing
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Store it in the SecurityContext. It lives only for THIS request (this thread).
                //    From here on, Spring Security treats this request as logged in
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Never let a token problem crash the request: log it and continue as "not logged in"
            logger.error("Cannot set user authentication: {}", e.getMessage());
        }

        // 7. ALWAYS continue: if no user was set, the URL rules decide (401/403)
        filterChain.doFilter(request, response);
    }

    /**
     * Where can the token be? Two places, checked in this order:
     * <ol>
     *   <li>{@code Authorization: Bearer <token>} header: Postman, mobile apps, other services</li>
     *   <li>{@code trainosys-jwt} cookie: browsers (set by /signin, sent back automatically)</li>
     * </ol>
     */
    private String parseJwt(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromHeader(request);
        if (jwt == null) {
            jwt = jwtUtils.getJwtFromCookies(request);
        }
        return jwt;
    }
}
