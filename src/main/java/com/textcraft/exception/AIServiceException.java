package com.textcraft.exception;

/** AI provider problems: bad key, timeout, rate limit (429), offline. */
public class AIServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
