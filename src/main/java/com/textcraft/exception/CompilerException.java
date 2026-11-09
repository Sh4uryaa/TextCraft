package com.textcraft.exception;

/** Code runner problems (Judge0 unreachable, local compiler missing, timeout). */
public class CompilerException extends Exception {
    private static final long serialVersionUID = 1L;

    public CompilerException(String message) {
        super(message);
    }

    public CompilerException(String message, Throwable cause) {
        super(message, cause);
    }
}
