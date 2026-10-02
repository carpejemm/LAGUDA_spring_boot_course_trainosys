package com.trainosys.ecom.app.security.response;

/**
 * MESSAGE RESPONSE: a one-line JSON answer, e.g.
 * <pre>
 * { "message": "User registered successfully!" }
 * { "message": "Error: Username is already taken!" }
 * { "message": "Bad credentials" }
 * { "message": "You've been signed out!" }
 * </pre>
 *
 * <p>Why not return a plain String? JSON is easier for a front end to read (response.message),
 * and every auth endpoint answers in the same shape. The HTTP status (200, 400, 401) says
 * success or failure; the message says why.
 */
public class MessageResponse {
    private String message;

    public MessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
