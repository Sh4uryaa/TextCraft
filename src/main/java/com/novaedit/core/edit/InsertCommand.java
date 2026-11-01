package com.novaedit.core.edit;

import com.novaedit.core.document.Document;

/**
 * EditCommand that inserts text at a specific character offset.
 */
public class InsertCommand implements EditCommand {
    private final int offset;
    private final String text;

    public InsertCommand(int offset, String text) {
        this.offset = offset;
        this.text = text != null ? text : "";
    }

    @Override
    public void apply(Document document) {
        if (document != null && !text.isEmpty()) {
            document.insert(offset, text);
        }
    }

    @Override
    public void undo(Document document) {
        if (document != null && !text.isEmpty()) {
            document.delete(offset, text.length());
        }
    }

    public int getOffset() {
        return offset;
    }

    public String getText() {
        return text;
    }

    @Override
    public String getDescription() {
        return "Insert \"" + (text.length() > 20 ? text.substring(0, 20) + "..." : text) + "\"";
    }
}
