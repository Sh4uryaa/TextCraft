package com.textcraft.voice;

import static org.junit.jupiter.api.Assertions.*;

import com.textcraft.exception.VoiceException;
import org.junit.jupiter.api.Test;

class VoiceCommandsTest {
    @Test
    void spokenCommandsBecomePunctuationAndBreaks() {
        assertEquals("Hello, world.", VoiceCommands.apply("Hello comma world full stop"));
        assertEquals("Really?", VoiceCommands.apply("Really question mark"));
        assertEquals("One\nTwo", VoiceCommands.apply("One new line Two"));
        assertEquals("One\n\nTwo", VoiceCommands.apply("One new paragraph Two"));
    }

    @Test
    void textWithoutCommandsIsUnchanged() {
        assertEquals("nothing special here", VoiceCommands.apply("nothing special here"));
    }

    @Test
    void speechAnswerIsParsed() throws Exception {
        String body = "{\"results\":[{\"alternatives\":[{\"transcript\":\"hello world\"}]},"
                + "{\"alternatives\":[{\"transcript\":\"second part\"}]}]}";
        assertEquals("hello world second part", GoogleSpeechRecognizer.parse(200, body));
        assertEquals("", GoogleSpeechRecognizer.parse(200, "{}"));
        assertThrows(VoiceException.class, () -> GoogleSpeechRecognizer.parse(429, "{}"));
        assertThrows(VoiceException.class, () -> GoogleSpeechRecognizer.parse(403, "{}"));
    }
}
