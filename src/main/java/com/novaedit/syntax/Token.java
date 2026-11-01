package com.novaedit.syntax;

/**
 * Represents a highlighted token range.
 */
public record Token(int start, int end, TokenType type) {
    public int length() {
        return end - start;
    }

    public String getCssClass() {
        return type.getCssClass();
    }
}
