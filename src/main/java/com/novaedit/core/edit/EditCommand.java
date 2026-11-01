package com.novaedit.core.edit;

import com.novaedit.core.document.Document;

/**
 * Command pattern for document modifications.
 * Enables undo/redo and macro recording.
 */
public interface EditCommand {
    /**
     * Apply the edit to the document.
     */
    void apply(Document document);

    /**
     * Revert the edit on the document.
     */
    void undo(Document document);

    /**
     * Human-readable description of the edit action.
     */
    String getDescription();
}
