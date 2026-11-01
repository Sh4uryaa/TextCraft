package com.novaedit.core.edit;

import com.novaedit.core.document.Document;

/**
 * EditCommand that deletes text from a specific character offset.
 */
public class DeleteCommand implements EditCommand {
    private final int offset;
    private final String deletedText;

    public DeleteCommand(int offset, String deletedText) {
        this.offset = offset;
        this.deletedText = deletedText != null ? deletedText : "";
    }

    @Override
    public void apply(Document document) {
        if (document != null && !deletedText.isEmpty()) {
            document.delete(offset, deletedText.length());
        }
    }

    @Override
    public void undo(Document document) {
        if (document != null && !deletedText.isEmpty()) {
            document.insert(offset, deletedText);
        }
    }

    public int getOffset() {
        return offset;
    }

    public String getDeletedText() {
        return deletedText;
    }

    @Override
    public String getDescription() {
        return "Delete \"" + (deletedText.length() > 20 ? deletedText.substring(0, 20) + "..." : deletedText) + "\"";
    }
}
