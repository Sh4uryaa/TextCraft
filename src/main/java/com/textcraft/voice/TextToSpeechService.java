package com.textcraft.voice;

/** OOP: Interface (Strategy) - read text aloud. */
public interface TextToSpeechService {
    /** @param speed 0.5 (slow) .. 2.0 (fast), 1.0 = normal */
    void speak(String text, double speed);

    /** Pause / resume. Not supported on Windows, where it stops instead. */
    void pause();

    void stop();

    boolean isSpeaking();
}
