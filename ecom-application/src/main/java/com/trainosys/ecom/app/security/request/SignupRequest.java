package com.trainosys.ecom.app.security.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SIGNUP REQUEST: the JSON body of {@code POST /api/auth/signup}.
 *
 * <pre>
 * {
 *   "username": "juan",
 *   "email": "juan@example.com",
 *   "password": "secret123",
 *   "firstName": "Juan",
 *   "lastName": "Dela Cruz"
 * }
 * </pre>
 *
 * <p>Validation rules (slide 6, input validation): checked BEFORE the controller method runs,
 * because AuthController marks the parameter {@code @Valid}. One broken rule -> 400 Bad Request.
 * Never trust what the client sends.
 *
 * <p><b>No {@code role} field, on purpose.</b> If the request could say {@code "role": "ADMIN"},
 * anyone could make themselves an admin. Sign up always creates a CUSTOMER; admins come from
 * DataInitializer (or, in a real app, from another admin).
 */
public class SignupRequest {
    /** Required, 3 to 20 characters. Must be unique (checked in AuthController). */
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    /** Required, at most 50 characters, must look like an email (name@domain). Must be unique. */
    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    /** Required, 6 to 40 characters. Arrives as plain text, saved only as a BCrypt hash. */
    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    /** Optional. */
    private String firstName;

    /** Optional. */
    private String lastName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
