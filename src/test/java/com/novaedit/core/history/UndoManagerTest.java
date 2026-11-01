package com.novaedit.core.history;

import com.novaedit.core.document.Document;
import com.novaedit.core.edit.DeleteCommand;
import com.novaedit.core.edit.InsertCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UndoManagerTest {

    @Test
    @DisplayName("Single edit undo and redo")
    void testBasicUndoRedo() {
        Document doc = new Document("Hello");
        UndoManager undoMgr = new UndoManager(doc);

        undoMgr.execute(new InsertCommand(5, " World"));
        assertThat(doc.getText()).isEqualTo("Hello World");
        assertThat(undoMgr.canUndo()).isTrue();
        assertThat(undoMgr.canRedo()).isFalse();

        undoMgr.undo();
        assertThat(doc.getText()).isEqualTo("Hello");
        assertThat(undoMgr.canUndo()).isFalse();
        assertThat(undoMgr.canRedo()).isTrue();

        undoMgr.redo();
        assertThat(doc.getText()).isEqualTo("Hello World");
        assertThat(undoMgr.canUndo()).isTrue();
        assertThat(undoMgr.canRedo()).isFalse();
    }

    @Test
    @DisplayName("Typing burst coalescing merges consecutive character inserts")
    void testTypingBurstCoalescing() {
        Document doc = new Document("");
        UndoManager undoMgr = new UndoManager(doc);

        // Type "cat" in rapid succession
        undoMgr.execute(new InsertCommand(0, "c"));
        undoMgr.execute(new InsertCommand(1, "a"));
        undoMgr.execute(new InsertCommand(2, "t"));

        assertThat(doc.getText()).isEqualTo("cat");
        // Should coalesce into 1 undo entry
        assertThat(undoMgr.getUndoCount()).isEqualTo(1);

        undoMgr.undo();
        assertThat(doc.getText()).isEmpty();

        undoMgr.redo();
        assertThat(doc.getText()).isEqualTo("cat");
    }

    @Test
    @DisplayName("Compound edit undoes all inner operations in one step")
    void testCompoundEdit() {
        Document doc = new Document("foo and foo");
        UndoManager undoMgr = new UndoManager(doc);

        // Replace all "foo" with "bar"
        undoMgr.beginCompoundEdit("Replace All");
        // Replace second occurrence at offset 8
        undoMgr.execute(new DeleteCommand(8, "foo"));
        undoMgr.execute(new InsertCommand(8, "bar"));
        // Replace first occurrence at offset 0
        undoMgr.execute(new DeleteCommand(0, "foo"));
        undoMgr.execute(new InsertCommand(0, "bar"));
        undoMgr.endCompoundEdit();

        assertThat(doc.getText()).isEqualTo("bar and bar");
        assertThat(undoMgr.getUndoCount()).isEqualTo(1);

        undoMgr.undo();
        assertThat(doc.getText()).isEqualTo("foo and foo");

        undoMgr.redo();
        assertThat(doc.getText()).isEqualTo("bar and bar");
    }

    @Test
    @DisplayName("Save point tracking")
    void testSavePoint() {
        Document doc = new Document("Initial");
        UndoManager undoMgr = new UndoManager(doc);

        undoMgr.markSaved();
        assertThat(undoMgr.isAtSavePoint()).isTrue();

        undoMgr.execute(new InsertCommand(7, "!"));
        assertThat(undoMgr.isAtSavePoint()).isFalse();

        undoMgr.undo();
        assertThat(undoMgr.isAtSavePoint()).isTrue();
    }
}
