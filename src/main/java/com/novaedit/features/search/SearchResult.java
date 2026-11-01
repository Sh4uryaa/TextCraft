package com.novaedit.features.search;

/**
 * Represents a matched occurrence within a document.
 */
public record SearchResult(int start, int end, int line, String text) {
    public int length() {
        return end - start;
    }
}
