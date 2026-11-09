package com.textcraft.ui;

import javax.swing.JComponent;

/**
 * OOP: Interface - what MainFrame needs from either editor (rich text page or code editor),
 * so menus, find/replace and the AI panel work with both.
 */
public interface TextEditor {
    JComponent component();

    String getAllText();

    String getSelectedText();

    int getSelectionStart();

    int getSelectionEnd();

    void select(int start, int end);

    /** One undoable edit. */
    void replaceRange(int start, int end, String text);

    void replaceSelection(String text);

    void insertAtCaret(String text);

    void insertBelowSelection(String text);

    void undo();

    void redo();

    void cut();

    void copy();

    void paste();

    void selectAll();

    boolean isDirty();

    void setDirty(boolean dirty);

    void addChangeListener(DocumentChangeListener l);

    void applyLiteMode(boolean lite);

    void focusEditor();
}
