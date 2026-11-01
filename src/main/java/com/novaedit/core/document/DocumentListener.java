package com.novaedit.core.document;

/**
 * Listener for document modifications and state changes.
 */
public interface DocumentListener {
    default void onInserted(int offset, String text) {}
    default void onDeleted(int offset, String deletedText) {}
    default void onDirtyStateChanged(boolean isDirty) {}
}
