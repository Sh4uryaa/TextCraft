package com.novaedit.core.document;

import com.novaedit.core.buffer.PieceTable;
import com.novaedit.core.buffer.TextBuffer;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Document represents an open text file or in-memory document.
 * Coordinates TextBuffer, encoding, line ending, and listeners.
 */
public class Document {
    private final TextBuffer buffer;
    private Path filePath;
    private LineEnding lineEnding;
    private EncodingInfo encodingInfo;
    private boolean dirty;
    private final List<DocumentListener> listeners = new CopyOnWriteArrayList<>();

    public Document() {
        this("", null);
    }

    public Document(String initialContent) {
        this(initialContent, null);
    }

    public Document(String initialContent, Path filePath) {
        this.buffer = new PieceTable(initialContent != null ? initialContent : "");
        this.filePath = filePath;
        this.lineEnding = LineEnding.detect(initialContent);
        this.encodingInfo = EncodingInfo.UTF_8;
        this.dirty = false;
    }

    public synchronized void insert(int offset, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        buffer.insert(offset, text);
        setDirty(true);
        for (DocumentListener listener : listeners) {
            listener.onInserted(offset, text);
        }
    }

    public synchronized void delete(int offset, int length) {
        if (length <= 0) {
            return;
        }
        String deleted = buffer.slice(offset, offset + length);
        buffer.delete(offset, length);
        setDirty(true);
        for (DocumentListener listener : listeners) {
            listener.onDeleted(offset, deleted);
        }
    }

    public synchronized int length() {
        return buffer.length();
    }

    public synchronized int lineCount() {
        return buffer.lineCount();
    }

    public synchronized char charAt(int offset) {
        return buffer.charAt(offset);
    }

    public synchronized String getText() {
        return buffer.getText();
    }

    public synchronized String slice(int start, int end) {
        return buffer.slice(start, end);
    }

    public synchronized int offsetOfLine(int line) {
        return buffer.offsetOfLine(line);
    }

    public synchronized int lineOfOffset(int offset) {
        return buffer.lineOfOffset(offset);
    }

    public synchronized int lineLength(int line) {
        return buffer.lineLength(line);
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public LineEnding getLineEnding() {
        return lineEnding;
    }

    public void setLineEnding(LineEnding lineEnding) {
        this.lineEnding = lineEnding;
    }

    public EncodingInfo getEncodingInfo() {
        return encodingInfo;
    }

    public void setEncodingInfo(EncodingInfo encodingInfo) {
        this.encodingInfo = encodingInfo;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        if (this.dirty != dirty) {
            this.dirty = dirty;
            for (DocumentListener listener : listeners) {
                listener.onDirtyStateChanged(dirty);
            }
        }
    }

    public void addListener(DocumentListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(DocumentListener listener) {
        listeners.remove(listener);
    }

    public TextBuffer getBuffer() {
        return buffer;
    }
}
