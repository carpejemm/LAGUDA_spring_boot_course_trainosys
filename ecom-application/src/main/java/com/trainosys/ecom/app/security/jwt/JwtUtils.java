package com.trainosys.ecom.app.security.jwt;

import com.trainosys.ecom.app.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT UTILS: everything about tokens in one place (slide 22).
 * Generate a token, read the username out of it, validate it, and move it in and out of
 * the request (header or cookie).
 *
 * <h2>What a JWT looks like (slide 19)</h2>
 * <pre>
 *   eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiJhZG1pbiIsImlhdCI6MTc5MDg5ODAxNSwiZXhwIjoxNzkwOTg0NDE1fQ . njhZkZJkvr...
 *   \__ HEADER _________/   \__ PAYLOAD (the "claims") _______________________________________/   \_ SIGNATURE
 *   {"alg":"HS256"}         {"sub":"admin","iat":1790898015,"exp":1790984415}                    HMACSHA256(header.payload, secret)
 * </pre>
 * <ul>
 *   <li><b>Header + payload are only Base64URL-encoded, NOT encrypted.</b> Anyone can read them (jwt.io).
 *       Never put a password or anything secret in a token.</li>
 *   <li><b>The signature</b> is computed with our secret key. Change one character of the payload
 *       (e.g. "sub":"user1" -> "sub":"admin") and the signature no longer matches -> rejected.
 *       Only someone with the secret can make a valid token.</li>
 *   <li>{@code sub} = subject (who), {@code iat} = issued at, {@code exp} = expires at
 *       (seconds since 1 Jan 1970).</li>
 * </ul>
 *
 * <p>Library: jjwt 0.13 ({@code io.jsonwebtoken}). We compile against {@code jjwt-api};
 * {@code jjwt-impl} and {@code jjwt-jackson} do the work at runtime (see pom.xml).
 *
 * <p>{@code @Component}: Spring creates one JwtUtils and injects it into SecurityConfig and AuthController.
 */
@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // ---------------------------------------------------------------------------------------------
    // Settings from application.yml (app.jwt.*)
    // @Value("${...}") copies a property into the field when Spring creates this bean.
    // A typo in the key = startup error "Could not resolve placeholder ...".
    // ---------------------------------------------------------------------------------------------

    /**
     * Base64 text of a 256-bit (32-byte) random key. HS256 refuses shorter keys (WeakKeyException).
     * Make one with: {@code openssl rand -base64 32}.
     * Whoever has this secret can create tokens for ANY user, so in real projects it comes from an
     * environment variable or a vault, never from a file in git.
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /** How long a token is valid, in MILLISECONDS. 86400000 = 24 hours. */
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /** Name of the cookie that carries the token for browsers ("trainosys-jwt"). */
    @Value("${app.jwt.cookie-name}")
    private String jwtCookie;

    // ---------------------------------------------------------------------------------------------
    // Reading the token from a request
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the token from the header (slide 18): {@code Authorization: Bearer <token>}.
     *
     * <p>"Bearer" means "whoever bears (carries) this token is allowed in", like a ticket.
     * {@code substring(7)} cuts off the 7 characters {@code "Bearer "} (6 letters + 1 space).
     *
     * @return the token, or {@code null} if the header is missing or doesn't start with "Bearer "
     *         (common mistake: sending only the token, without "Bearer ")
     */
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Reads the token from the {@code trainosys-jwt} cookie (slides 28-31).
     * The browser sends cookies back automatically, so a browser front end needs no code to attach it.
     *
     * @return the token, or {@code null} if the cookie isn't there
     */
    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        if (cookie != null) {
            return cookie.getValue();
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // Cookies (sent back in the "Set-Cookie" response header)
    // ---------------------------------------------------------------------------------------------

    /**
     * Builds the login cookie, e.g.
     * {@code Set-Cookie: trainosys-jwt=eyJ...; Path=/api; Max-Age=86400; Expires=...; HttpOnly}
     *
     * <ul>
     *   <li>{@code path("/api")}: the browser only sends it to /api/... URLs.</li>
     *   <li>{@code maxAge}: in SECONDS (ms / 1000), so the cookie dies with the token.</li>
     *   <li>{@code httpOnly(true)}: JavaScript on the page CANNOT read it (document.cookie hides it).
     *       An injected script (XSS) can't steal the token. A token kept in localStorage CAN be stolen.</li>
     *   <li>{@code secure(false)}: allows plain http://localhost. With HTTPS set it to true, so the
     *       cookie is never sent unencrypted.</li>
     * </ul>
     */
    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromUsername(userPrincipal.getUsername());
        return ResponseCookie.from(jwtCookie, jwt)
                .path("/api")
                .maxAge(jwtExpirationMs / 1000)
                .httpOnly(true)
                // Set to true when the app runs on HTTPS
                .secure(false)
                .build();
    }

    /**
     * Builds the "delete me" cookie for sign out: same name, same path, empty value, {@code Max-Age=0}.
     * A browser deletes a cookie when it receives one with the same name + path and Max-Age 0.
     *
     * <p>Note: this removes the cookie, it does NOT cancel the token. The server keeps no list of
     * tokens (stateless), so a copied token stays valid until its "exp".
     */
    public ResponseCookie getCleanJwtCookie() {
        return ResponseCookie.from(jwtCookie, "")
                .path("/api")
                .maxAge(0)
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // Creating, reading and checking tokens
    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a signed token for a username. Called once, at sign in.
     *
     * <pre>
     *   subject    -> "sub": "admin"         who the token belongs to
     *   issuedAt   -> "iat": now
     *   expiration -> "exp": now + 24 h      after this, validateJwtToken() returns false
     *   signWith   -> HS256 signature with our secret key
     *   compact    -> "header.payload.signature" as one String
     * </pre>
     *
     * <p>Only the username goes in. Roles are NOT stored in the token: AuthTokenFilter loads them
     * from the database on every request, so a role change works on the next request.
     */
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    /**
     * Reads "sub" (the username) out of a token.
     * {@code verifyWith(key())} + {@code parseSignedClaims} also check the signature, so this
     * only returns a username we signed ourselves. Call validateJwtToken() first.
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Is this token genuine and still valid? Parsing it checks everything at once:
     * format, signature (made with OUR secret?) and expiry ("exp" in the past?).
     * Each problem throws a different exception; we log which one and answer {@code false}.
     *
     * <p>We never throw from here: a bad token just means "not logged in". The URL rules then
     * answer 401 for protected URLs, while public URLs still work.
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            // Not a JWT at all, e.g. "abc.def.ghi" or a token cut short when copying
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            // Correct signature, but "exp" is in the past: the user must sign in again
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            // A JWT of a kind we don't accept (e.g. an unsigned one)
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (SignatureException e) {
            // Edited payload, or signed with a different secret: someone tampered with it
            logger.error("JWT signature does not match: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // Empty or null token
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Turns the Base64 secret from application.yml into the HMAC-SHA key that signs and verifies.
     * The SAME key does both (symmetric, "HS" = HMAC + SHA). Change the secret and every token
     * issued before becomes invalid.
     */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
