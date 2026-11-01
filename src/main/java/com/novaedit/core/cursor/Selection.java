package com.novaedit.core.cursor;

import com.novaedit.core.document.Document;

/**
 * Text selection spanning between an anchor position and an active caret position.
 */
public record Selection(int anchorOffset, int caretOffset) {

    public static Selection collapsed(int offset) {
        return new Selection(offset, offset);
    }

    public static Selection range(int start, int end) {
        return new Selection(start, end);
    }

    public boolean isCollapsed() {
        return anchorOffset == caretOffset;
    }

    public int getStart() {
        return Math.min(anchorOffset, caretOffset);
    }

    public int getEnd() {
        return Math.max(anchorOffset, caretOffset);
    }

    public int getLength() {
        return getEnd() - getStart();
    }

    public String getSelectedText(Document document) {
        if (isCollapsed() || document == null) {
            return "";
        }
        return document.slice(getStart(), getEnd());
    }
}
