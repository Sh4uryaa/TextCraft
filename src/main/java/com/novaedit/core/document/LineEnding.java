package com.novaedit.core.document;

/**
 * Line ending representations for cross-platform compatibility.
 */
public enum LineEnding {
    LF("\n", "LF"),
    CRLF("\r\n", "CRLF"),
    CR("\r", "CR");

    private final String sequence;
    private final String displayName;

    LineEnding(String sequence, String displayName) {
        this.sequence = sequence;
        this.displayName = displayName;
    }

    public String getSequence() {
        return sequence;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LineEnding detect(String text) {
        if (text == null || text.isEmpty()) {
            return System.lineSeparator().equals("\r\n") ? CRLF : LF;
        }
        int crlf = 0;
        int lf = 0;
        int cr = 0;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                if (i + 1 < len && text.charAt(i + 1) == '\n') {
                    crlf++;
                    i++;
                } else {
                    cr++;
                }
            } else if (c == '\n') {
                lf++;
            }
        }
        if (crlf >= lf && crlf >= cr && crlf > 0) {
            return CRLF;
        }
        if (lf >= crlf && lf >= cr && lf > 0) {
            return LF;
        }
        if (cr > 0) {
            return CR;
        }
        return System.lineSeparator().equals("\r\n") ? CRLF : LF;
    }
}
