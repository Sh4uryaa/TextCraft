package com.novaedit.core.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PieceTable implementation of TextBuffer.
 * Maintains an original buffer, an append-only add buffer, and a list of pieces.
 * Highly efficient for text editing with minimal memory copying.
 */
public class PieceTable implements TextBuffer {

    public enum BufferType {
        ORIGINAL, ADD
    }

    public static final class Piece {
        public final BufferType bufferType;
        public final int start;
        public final int length;

        public Piece(BufferType bufferType, int start, int length) {
            this.bufferType = bufferType;
            this.start = start;
            this.length = length;
        }

        public Piece withLength(int newLength) {
            return new Piece(bufferType, start, newLength);
        }

        public Piece subPiece(int offsetIntoPiece, int subLength) {
            return new Piece(bufferType, start + offsetIntoPiece, subLength);
        }

        @Override
        public String toString() {
            return "Piece[" + bufferType + ", start=" + start + ", len=" + length + "]";
        }
    }

    private final String originalBuffer;
    private final StringBuilder addBuffer;
    private final List<Piece> pieces;
    private final LineIndex lineIndex;
    private int cachedLength;

    public PieceTable() {
        this("");
    }

    public PieceTable(String initialContent) {
        if (initialContent == null) {
            initialContent = "";
        }
        this.originalBuffer = initialContent;
        this.addBuffer = new StringBuilder();
        this.pieces = new ArrayList<>();
        this.lineIndex = new LineIndex();

        if (!initialContent.isEmpty()) {
            this.pieces.add(new Piece(BufferType.ORIGINAL, 0, initialContent.length()));
            this.cachedLength = initialContent.length();
        } else {
            this.cachedLength = 0;
        }
        rebuildLineIndex();
    }

    @Override
    public synchronized int length() {
        return cachedLength;
    }

    @Override
    public synchronized int lineCount() {
        return lineIndex.getLineCount();
    }

    @Override
    public synchronized char charAt(int offset) {
        if (offset < 0 || offset >= cachedLength) {
            throw new IndexOutOfBoundsException("Offset " + offset + " out of bounds for length " + cachedLength);
        }

        int current = 0;
        for (Piece piece : pieces) {
            if (offset < current + piece.length) {
                int indexInPiece = offset - current;
                int bufferIndex = piece.start + indexInPiece;
                return (piece.bufferType == BufferType.ORIGINAL)
                        ? originalBuffer.charAt(bufferIndex)
                        : addBuffer.charAt(bufferIndex);
            }
            current += piece.length;
        }
        throw new IndexOutOfBoundsException("Offset " + offset + " out of bounds");
    }

    @Override
    public synchronized String getText() {
        if (cachedLength == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(cachedLength);
        for (Piece piece : pieces) {
            appendPiece(sb, piece, 0, piece.length);
        }
        return sb.toString();
    }

    @Override
    public synchronized String slice(int start, int end) {
        if (start < 0) start = 0;
        if (end > cachedLength) end = cachedLength;
        if (start >= end) return "";

        StringBuilder sb = new StringBuilder(end - start);
        int current = 0;
        for (Piece piece : pieces) {
            int pieceStart = current;
            int pieceEnd = current + piece.length;

            if (pieceEnd > start && pieceStart < end) {
                int overlapStart = Math.max(start, pieceStart);
                int overlapEnd = Math.min(end, pieceEnd);
                int offsetInPiece = overlapStart - pieceStart;
                int lenInPiece = overlapEnd - overlapStart;
                appendPiece(sb, piece, offsetInPiece, lenInPiece);
            }
            current += piece.length;
            if (current >= end) break;
        }
        return sb.toString();
    }

    @Override
    public synchronized void insert(int offset, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (offset < 0 || offset > cachedLength) {
            throw new IndexOutOfBoundsException("Insert offset " + offset + " out of bounds [0, " + cachedLength + "]");
        }

        int addStart = addBuffer.length();
        addBuffer.append(text);
        int addLen = text.length();
        Piece newPiece = new Piece(BufferType.ADD, addStart, addLen);

        if (pieces.isEmpty()) {
            pieces.add(newPiece);
        } else if (offset == 0) {
            pieces.add(0, newPiece);
        } else if (offset == cachedLength) {
            // Check if we can coalesce with last piece
            Piece last = pieces.get(pieces.size() - 1);
            if (last.bufferType == BufferType.ADD && last.start + last.length == addStart) {
                pieces.set(pieces.size() - 1, new Piece(BufferType.ADD, last.start, last.length + addLen));
            } else {
                pieces.add(newPiece);
            }
        } else {
            // Split piece at offset
            int current = 0;
            for (int i = 0; i < pieces.size(); i++) {
                Piece p = pieces.get(i);
                if (offset == current) {
                    pieces.add(i, newPiece);
                    break;
                } else if (offset > current && offset < current + p.length) {
                    int leftLen = offset - current;
                    int rightLen = p.length - leftLen;
                    Piece leftPiece = p.subPiece(0, leftLen);
                    Piece rightPiece = p.subPiece(leftLen, rightLen);

                    pieces.set(i, leftPiece);
                    pieces.add(i + 1, newPiece);
                    pieces.add(i + 2, rightPiece);
                    break;
                }
                current += p.length;
            }
        }

        cachedLength += addLen;
        rebuildLineIndex();
    }

    @Override
    public synchronized void delete(int offset, int length) {
        if (length <= 0) {
            return;
        }
        if (offset < 0 || offset + length > cachedLength) {
            throw new IndexOutOfBoundsException("Delete range [" + offset + ", " + (offset + length)
                    + "] out of bounds for length " + cachedLength);
        }

        int end = offset + length;
        List<Piece> updated = new ArrayList<>();
        int current = 0;

        for (Piece p : pieces) {
            int pieceStart = current;
            int pieceEnd = current + p.length;

            if (pieceEnd <= offset || pieceStart >= end) {
                // Entirely outside delete range
                updated.add(p);
            } else {
                // Overlaps with delete range
                if (pieceStart < offset) {
                    // Left piece before delete
                    updated.add(p.subPiece(0, offset - pieceStart));
                }
                if (pieceEnd > end) {
                    // Right piece after delete
                    int rightOffset = end - pieceStart;
                    updated.add(p.subPiece(rightOffset, pieceEnd - end));
                }
            }
            current += p.length;
        }

        pieces.clear();
        pieces.addAll(updated);
        cachedLength -= length;
        rebuildLineIndex();
    }

    @Override
    public synchronized int offsetOfLine(int line) {
        return lineIndex.getOffsetOfLine(line, cachedLength);
    }

    @Override
    public synchronized int lineOfOffset(int offset) {
        return lineIndex.getLineOfOffset(offset, cachedLength);
    }

    @Override
    public synchronized int lineLength(int line) {
        return lineIndex.getLineLength(line, cachedLength);
    }

    private void appendPiece(StringBuilder sb, Piece piece, int offsetInPiece, int len) {
        int bufferStart = piece.start + offsetInPiece;
        if (piece.bufferType == BufferType.ORIGINAL) {
            sb.append(originalBuffer, bufferStart, bufferStart + len);
        } else {
            sb.append(addBuffer, bufferStart, bufferStart + len);
        }
    }

    private void rebuildLineIndex() {
        lineIndex.rebuild(new CharSequence() {
            @Override
            public int length() {
                return cachedLength;
            }

            @Override
            public char charAt(int index) {
                return PieceTable.this.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return slice(start, end);
            }
        });
    }

    public synchronized List<Piece> getPieces() {
        return Collections.unmodifiableList(new ArrayList<>(pieces));
    }
}
