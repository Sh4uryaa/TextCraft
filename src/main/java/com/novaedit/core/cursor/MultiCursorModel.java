package com.novaedit.core.cursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manages one primary cursor and zero or more secondary cursors for multi-cursor editing.
 */
public class MultiCursorModel {
    private Selection primarySelection;
    private final List<Selection> secondarySelections = new ArrayList<>();

    public MultiCursorModel() {
        this.primarySelection = Selection.collapsed(0);
    }

    public synchronized Selection getPrimarySelection() {
        return primarySelection;
    }

    public synchronized void setPrimarySelection(Selection selection) {
        this.primarySelection = selection != null ? selection : Selection.collapsed(0);
    }

    public synchronized void setPrimaryCaret(int offset) {
        this.primarySelection = Selection.collapsed(offset);
    }

    public synchronized List<Selection> getSecondarySelections() {
        return Collections.unmodifiableList(new ArrayList<>(secondarySelections));
    }

    public synchronized List<Selection> getAllSelections() {
        List<Selection> all = new ArrayList<>();
        all.add(primarySelection);
        all.addAll(secondarySelections);
        all.sort(Comparator.comparingInt(Selection::getStart));
        return all;
    }

    public synchronized void addSecondaryCursor(int offset) {
        addSecondarySelection(Selection.collapsed(offset));
    }

    public synchronized void addSecondarySelection(Selection selection) {
        if (selection == null) return;
        // Don't add duplicate
        if (!selection.equals(primarySelection) && !secondarySelections.contains(selection)) {
            secondarySelections.add(selection);
        }
    }

    public synchronized void clearSecondaryCursors() {
        secondarySelections.clear();
    }

    public synchronized boolean hasMultipleCursors() {
        return !secondarySelections.isEmpty();
    }
}
