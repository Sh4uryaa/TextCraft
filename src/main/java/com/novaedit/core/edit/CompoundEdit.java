package com.novaedit.core.edit;

import com.novaedit.core.document.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite pattern for a group of edit commands (e.g. Replace All, Block Indent)
 * treated as a single undo/redo step.
 */
public class CompoundEdit implements EditCommand {
    private final List<EditCommand> commands = new ArrayList<>();
    private final String description;

    public CompoundEdit(String description) {
        this.description = description != null ? description : "Compound Edit";
    }

    public void add(EditCommand command) {
        if (command != null) {
            commands.add(command);
        }
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    public int size() {
        return commands.size();
    }

    public List<EditCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    @Override
    public void apply(Document document) {
        for (EditCommand command : commands) {
            command.apply(document);
        }
    }

    @Override
    public void undo(Document document) {
        // Revert in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo(document);
        }
    }

    @Override
    public String getDescription() {
        return description;
    }
}
