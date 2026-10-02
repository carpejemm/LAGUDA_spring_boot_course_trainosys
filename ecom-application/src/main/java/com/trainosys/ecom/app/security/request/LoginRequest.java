package com.trainosys.ecom.app.security.request;

import jakarta.validation.constraints.NotBlank;

/**
 * LOGIN REQUEST: the JSON body of {@code POST /api/auth/signin}.
 *
 * <pre>
 * { "username": "admin", "password": "admin123" }
 * </pre>
 *
 * <p>The password arrives in plain text. That's normal: the server needs it to compare with the
 * BCrypt hash. It must travel over HTTPS in production, and it is never stored or logged.
 *
 * <p>Validation (slide 6, input validation): {@code @NotBlank} = not null, not "" and not only spaces.
 * It only runs because AuthController marks the parameter {@code @Valid}.
 * A blank field -> 400 Bad Request, and the controller method never runs.
 */
public class LoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
