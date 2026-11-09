package com.textcraft.ai;

/** OOP: Enum - every AI action in the AI menu. */
public enum AIAction {
    FIX_GRAMMAR("Fix grammar"),
    IMPROVE("Improve"),
    TONE("Change tone"),
    SHORTER("Shorter"),
    LONGER("Longer"),
    SUMMARIZE("Summarize"),
    EXPLAIN("Explain"),
    TRANSLATE("Translate"),
    ASK("Ask AI");

    private final String label;

    AIAction(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Explain/Summarize/Ask produce new text, they do not rewrite the selection. */
    public boolean rewrites() {
        return this != EXPLAIN && this != ASK && this != SUMMARIZE;
    }
}
