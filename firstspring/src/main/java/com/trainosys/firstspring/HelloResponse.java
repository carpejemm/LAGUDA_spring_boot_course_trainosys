package com.trainosys.firstspring;

public class HelloResponse {
    private String message;

    // Constructor that initializes the message field with the provided value
    public HelloResponse(String message) {
        this.message = message;
    }

    // Getter method for the message field
    public String getMessage() {
        return message;
    }

    // Setter method for the message field
    public void setMessage(String message) {
        this.message = message;
    }
}
