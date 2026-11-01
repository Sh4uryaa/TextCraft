package com.novaedit.core.buffer;

/**
 * TextBuffer represents the abstract text storage interface.
 * Implemented by high performance data structures like PieceTable.
 */
public interface TextBuffer {
    /**
     * Total number of characters in the buffer.
     */
    int length();

    /**
     * Total number of lines in the buffer (minimum 1).
     */
    int lineCount();

    /**
     * Character at the given offset.
     */
    char charAt(int offset);

    /**
     * Get the entire text buffer as a String.
     */
    String getText();

    /**
     * Extract a substring in range [start, end).
     */
    String slice(int start, int end);

    /**
     * Insert text at the specified character offset.
     */
    void insert(int offset, String text);

    /**
     * Delete length characters starting at the specified character offset.
     */
    void delete(int offset, int length);

    /**
     * Get character offset where the given 0-indexed line starts.
     */
    int offsetOfLine(int line);

    /**
     * Get 0-indexed line number for the given character offset.
     */
    int lineOfOffset(int offset);

    /**
     * Length of the specified 0-indexed line.
     */
    int lineLength(int line);
}
