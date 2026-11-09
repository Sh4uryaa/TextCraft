package com.textcraft.auth;

/** Bad login, bad registration data, or Google sign-in problems (message is safe to show to the user). */
public class AuthException extends Exception {
    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
