package com.textcraft.voice;

import com.textcraft.exception.VoiceException;

/** OOP: Interface (Strategy) - turns recorded audio into text. */
public interface SpeechRecognizer {
    /** @param pcm16kMono raw 16 kHz, 16-bit, mono, little-endian samples */
    String recognize(byte[] pcm16kMono, String langCode) throws VoiceException;
}
