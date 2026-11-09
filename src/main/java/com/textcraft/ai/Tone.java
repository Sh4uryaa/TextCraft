package com.textcraft.ai;

/** OOP: Enum - tones for the "Tone" AI action. */
public enum Tone {
    FORMAL("formal"), CASUAL("casual"), FRIENDLY("friendly"), PROFESSIONAL("professional");

    private final String word;

    Tone(String word) {
        this.word = word;
    }

    public String word() {
        return word;
    }
}
