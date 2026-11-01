package com.novaedit.features.macro;

import com.novaedit.core.document.Document;
import com.novaedit.core.edit.CompoundEdit;
import com.novaedit.core.edit.EditCommand;
import com.novaedit.core.history.UndoManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records sequences of user edits and plays them back on demand.
 */
public class MacroRecorder {
    private boolean recording = false;
    private final List<EditCommand> recordedCommands = new ArrayList<>();

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized void startRecording() {
        recordedCommands.clear();
        recording = true;
    }

    public synchronized void stopRecording() {
        recording = false;
    }

    public synchronized void record(EditCommand command) {
        if (recording && command != null) {
            recordedCommands.add(command);
        }
    }

    public synchronized void playback(Document document, UndoManager undoManager) {
        if (recordedCommands.isEmpty() || document == null) {
            return;
        }

        if (undoManager != null) {
            undoManager.beginCompoundEdit("Macro Playback");
            try {
                for (EditCommand command : recordedCommands) {
                    undoManager.execute(command);
                }
            } finally {
                undoManager.endCompoundEdit();
            }
        } else {
            for (EditCommand command : recordedCommands) {
                command.apply(document);
            }
        }
    }

    public synchronized List<EditCommand> getRecordedCommands() {
        return Collections.unmodifiableList(recordedCommands);
    }

    public synchronized int getCommandCount() {
        return recordedCommands.size();
    }
}
