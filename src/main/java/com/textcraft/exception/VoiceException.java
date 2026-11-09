package com.textcraft.exception;

/** Voice typing problems: no microphone, offline, missing key, quota. */
public class VoiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public VoiceException(String message) {
        super(message);
    }

    public VoiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
