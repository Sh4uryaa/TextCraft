package com.novaedit.features.autocomplete;

/**
 * Autocomplete item presented in the code completion popup.
 */
public record CompletionItem(String label, String insertText, String detail, Kind kind) {

    public enum Kind {
        KEYWORD,
        SNIPPET,
        WORD,
        METHOD,
        CLASS
    }
}
