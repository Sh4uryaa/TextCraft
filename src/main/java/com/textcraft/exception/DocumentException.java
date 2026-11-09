package com.textcraft.exception;

/** OOP: custom checked exception - file reading/writing and document problems. */
public class DocumentException extends Exception {
    private static final long serialVersionUID = 1L;

    public DocumentException(String message) {
        super(message);
    }

    public DocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
