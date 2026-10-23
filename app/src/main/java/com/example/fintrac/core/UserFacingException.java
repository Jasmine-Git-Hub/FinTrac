package com.example.fintrac.core;

/**
 * A failure whose message is written for the user and can be shown as-is.
 * Unchecked so it can be thrown from inside Firestore transactions.
 */
public class UserFacingException extends RuntimeException {
    public UserFacingException(String message) {
        super(message);
    }
}
