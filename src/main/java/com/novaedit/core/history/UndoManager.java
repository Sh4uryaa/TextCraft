package com.novaedit.core.history;

import com.novaedit.core.document.Document;
import com.novaedit.core.edit.CompoundEdit;
import com.novaedit.core.edit.DeleteCommand;
import com.novaedit.core.edit.EditCommand;
import com.novaedit.core.edit.InsertCommand;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * UndoManager manages undo and redo operations with grouped typing bursts
 * and compound transaction support.
 */
public class UndoManager {
    private static final long TYPING_BURST_THRESHOLD_MS = 500;

    private final Document document;
    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();

    private CompoundEdit activeCompoundEdit = null;
    private int compoundDepth = 0;

    private long lastEditTime = 0;
    private EditCommand lastRecordedCommand = null;

    private int savePointUndoCount = 0;

    public UndoManager(Document document) {
        this.document = document;
    }

    /**
     * Executes the command and records it for undo/redo.
     */
    public synchronized void execute(EditCommand command) {
        if (command == null) return;

        // Apply to document
        command.apply(document);

        // Record in active compound edit if in transaction
        if (activeCompoundEdit != null) {
            activeCompoundEdit.add(command);
            return;
        }

        long now = System.currentTimeMillis();
        boolean coalesced = false;

        // Check for grouped typing burst coalescing
        if (lastRecordedCommand != null && (now - lastEditTime) <= TYPING_BURST_THRESHOLD_MS) {
            if (command instanceof InsertCommand newInsert && lastRecordedCommand instanceof InsertCommand prevInsert) {
                // If inserting consecutively at previous end, coalesce
                if (newInsert.getOffset() == prevInsert.getOffset() + prevInsert.getText().length()
                        && !newInsert.getText().contains("\n") && !prevInsert.getText().contains("\n")) {
                    undoStack.pop();
                    InsertCommand merged = new InsertCommand(prevInsert.getOffset(), prevInsert.getText() + newInsert.getText());
                    undoStack.push(merged);
                    lastRecordedCommand = merged;
                    coalesced = true;
                }
            } else if (command instanceof DeleteCommand newDel && lastRecordedCommand instanceof DeleteCommand prevDel) {
                // If backspacing consecutively (newDel.offset == prevDel.offset - newDel.length)
                if (newDel.getOffset() + newDel.getDeletedText().length() == prevDel.getOffset()
                        && !newDel.getDeletedText().contains("\n") && !prevDel.getDeletedText().contains("\n")) {
                    undoStack.pop();
                    DeleteCommand merged = new DeleteCommand(newDel.getOffset(), newDel.getDeletedText() + prevDel.getDeletedText());
                    undoStack.push(merged);
                    lastRecordedCommand = merged;
                    coalesced = true;
                }
            }
        }

        if (!coalesced) {
            undoStack.push(command);
            lastRecordedCommand = command;
        }

        redoStack.clear();
        lastEditTime = now;
    }

    public synchronized void beginCompoundEdit(String description) {
        if (compoundDepth == 0) {
            activeCompoundEdit = new CompoundEdit(description);
        }
        compoundDepth++;
    }

    public synchronized void endCompoundEdit() {
        if (compoundDepth > 0) {
            compoundDepth--;
            if (compoundDepth == 0 && activeCompoundEdit != null) {
                if (!activeCompoundEdit.isEmpty()) {
                    undoStack.push(activeCompoundEdit);
                    redoStack.clear();
                    lastRecordedCommand = activeCompoundEdit;
                    lastEditTime = System.currentTimeMillis();
                }
                activeCompoundEdit = null;
            }
        }
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public synchronized void undo() {
        if (!canUndo()) return;

        EditCommand cmd = undoStack.pop();
        cmd.undo(document);
        redoStack.push(cmd);
        lastRecordedCommand = null;
    }

    public synchronized void redo() {
        if (!canRedo()) return;

        EditCommand cmd = redoStack.pop();
        cmd.apply(document);
        undoStack.push(cmd);
        lastRecordedCommand = null;
    }

    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
        lastRecordedCommand = null;
        savePointUndoCount = 0;
    }

    public synchronized void markSaved() {
        savePointUndoCount = undoStack.size();
    }

    public synchronized boolean isAtSavePoint() {
        return undoStack.size() == savePointUndoCount;
    }

    public synchronized int getUndoCount() {
        return undoStack.size();
    }

    public synchronized int getRedoCount() {
        return redoStack.size();
    }
}
