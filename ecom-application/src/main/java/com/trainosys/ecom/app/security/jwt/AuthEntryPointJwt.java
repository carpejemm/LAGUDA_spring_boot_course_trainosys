package com.trainosys.ecom.app.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AUTH ENTRY POINT: the "please sign in" answer, a JSON 401 (slide 24).
 *
 * <p>Spring Security calls {@link #commence} when a request needs a login and nobody is logged in:
 * no token, an expired token, a tampered token, a missing "Bearer ".
 *
 * <pre>
 *   401 Unauthorized  "Who are you?"        -> this class        -> sign in again
 *   403 Forbidden     "I know you, but no"  -> Spring's default  -> wrong role (e.g. customer on /api/users)
 * </pre>
 * Slide 5: authentication always comes first. Fail it = 401. Pass it but not allowed = 403.
 *
 * <p>Example response:
 * <pre>
 * {
 *   "status": 401,
 *   "error": "Unauthorized",
 *   "message": "Full authentication is required to access this resource",
 *   "path": "/api/cart"
 * }
 * </pre>
 *
 * <p>Registered in SecurityConfig with {@code .exceptionHandling(e -> e.authenticationEntryPoint(...))}.
 * Without it, "not logged in" is an empty 403, the same as "wrong role" (step 4 of the guide).
 *
 * <p>{@code @Component} so Spring can inject it into SecurityConfig.
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    /**
     * Writes the 401 response by hand.
     *
     * <p>Why by hand? This runs inside the security filter chain, BEFORE any controller, so there
     * is no {@code ResponseEntity} or {@code @RestController} to turn an object into JSON for us.
     * We set the status and content type ourselves and let Jackson's ObjectMapper write the body.
     *
     * @param authException why the login is missing, e.g. "Full authentication is required to access this resource".
     *                      The real reason (expired, bad signature) is in JwtUtils' log, not here.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Shows up in the IntelliJ console for every rejected request
        logger.error("Unauthorized error: {}", authException.getMessage());

        // Status and content type must be set BEFORE writing the body
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // LinkedHashMap keeps the keys in the order we put them (a HashMap would shuffle them)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        // getServletPath(): the URL that was called, e.g. "/api/cart"
        body.put("path", request.getServletPath());

        // Map -> JSON text, straight into the HTTP response
        new ObjectMapper().writeValue(response.getOutputStream(), body);
    }
}
