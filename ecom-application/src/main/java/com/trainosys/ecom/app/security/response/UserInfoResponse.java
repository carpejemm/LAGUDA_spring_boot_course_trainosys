package com.trainosys.ecom.app.security.response;

import java.util.List;

/**
 * USER INFO RESPONSE: what the API tells the client about the logged-in user.
 *
 * <p>Returned by:
 * <ul>
 *   <li>{@code POST /api/auth/signin}, with the new token:
 *       <pre>{ "id": 1, "username": "admin", "roles": ["ROLE_ADMIN"], "jwtToken": "eyJ..." }</pre>
 *       The client keeps jwtToken and sends it back as {@code Authorization: Bearer <jwtToken>}.
 *       (Postman's Sign In script saves it into {{jwtToken}}.)</li>
 *   <li>{@code GET /api/auth/user}, without a token ({@code "jwtToken": null}): no new token is made there.</li>
 * </ul>
 *
 * <p>The front end uses {@code roles} to decide what to SHOW (e.g. hide the "Add product" button
 * for customers). The server still checks the role on every request: hiding a button is not security.
 *
 * <p>Never put the password (not even the hash) in a response.
 */
public class UserInfoResponse {
    private Long id;
    private String jwtToken;
    private String username;
    /** e.g. ["ROLE_CUSTOMER"] */
    private List<String> roles;

    public UserInfoResponse(Long id, String username, List<String> roles, String jwtToken) {
        this.id = id;
        this.username = username;
        this.roles = roles;
        this.jwtToken = jwtToken;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
