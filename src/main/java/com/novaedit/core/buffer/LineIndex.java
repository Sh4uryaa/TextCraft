package com.novaedit.core.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LineIndex tracks line starting offsets for fast conversion between
 * character offsets and (line, column) coordinates.
 * Line numbers are 0-indexed internally (line 0 is the first line).
 */
public class LineIndex {
    // List of starting character offsets for each line.
    // By definition, line 0 starts at offset 0.
    private final List<Integer> lineStarts = new ArrayList<>();

    public LineIndex() {
        lineStarts.add(0);
    }

    public synchronized void rebuild(CharSequence text) {
        lineStarts.clear();
        lineStarts.add(0);
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                if (i + 1 < len && text.charAt(i + 1) == '\n') {
                    i++; // Treat CRLF as a single line break
                }
                lineStarts.add(i + 1);
            } else if (c == '\n') {
                lineStarts.add(i + 1);
            }
        }
    }

    public synchronized int getLineCount() {
        return lineStarts.size();
    }

    /**
     * Returns the 0-indexed line number containing the given character offset.
     */
    public synchronized int getLineOfOffset(int offset, int totalLength) {
        if (offset < 0) {
            return 0;
        }
        if (offset > totalLength) {
            offset = totalLength;
        }
        int idx = Collections.binarySearch(lineStarts, offset);
        if (idx >= 0) {
            return idx;
        }
        // binarySearch returns (-(insertion point) - 1)
        int insertionPoint = -idx - 1;
        return Math.max(0, insertionPoint - 1);
    }

    /**
     * Returns the character offset where the given 0-indexed line starts.
     */
    public synchronized int getOffsetOfLine(int line, int totalLength) {
        if (line <= 0) {
            return 0;
        }
        if (line >= lineStarts.size()) {
            return totalLength;
        }
        return lineStarts.get(line);
    }

    /**
     * Returns the length in characters of the specified line (including line terminator if present).
     */
    public synchronized int getLineLength(int line, int totalLength) {
        if (line < 0 || line >= lineStarts.size()) {
            return 0;
        }
        int start = lineStarts.get(line);
        int end = (line + 1 < lineStarts.size()) ? lineStarts.get(line + 1) : totalLength;
        return Math.max(0, end - start);
    }

    public synchronized List<Integer> getLineStarts() {
        return Collections.unmodifiableList(new ArrayList<>(lineStarts));
    }
}
