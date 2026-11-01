package com.novaedit.core.cursor;

/**
 * Immutable Caret position representing character offset, line, and column.
 * Line and column are 0-indexed internally.
 */
public record Caret(int offset, int line, int column) {

    public static Caret at(int offset, int line, int column) {
        return new Caret(Math.max(0, offset), Math.max(0, line), Math.max(0, column));
    }

    public static Caret atOffset(int offset) {
        return new Caret(Math.max(0, offset), 0, 0);
    }
}
