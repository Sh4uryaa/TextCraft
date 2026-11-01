package com.novaedit.core.buffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PieceTableTest {

    @Test
    @DisplayName("Initial content and basic properties")
    void testInitialContent() {
        PieceTable pt = new PieceTable("Hello, World!\nSecond line.");
        assertThat(pt.length()).isEqualTo(26);
        assertThat(pt.lineCount()).isEqualTo(2);
        assertThat(pt.getText()).isEqualTo("Hello, World!\nSecond line.");
        assertThat(pt.slice(0, 5)).isEqualTo("Hello");
        assertThat(pt.slice(7, 12)).isEqualTo("World");
        assertThat(pt.charAt(0)).isEqualTo('H');
        assertThat(pt.charAt(13)).isEqualTo('\n');
    }

    @Test
    @DisplayName("Empty piece table initialization and modifications")
    void testEmptyPieceTable() {
        PieceTable pt = new PieceTable();
        assertThat(pt.length()).isEqualTo(0);
        assertThat(pt.lineCount()).isEqualTo(1);
        assertThat(pt.getText()).isEmpty();

        pt.insert(0, "ABC");
        assertThat(pt.getText()).isEqualTo("ABC");
        assertThat(pt.length()).isEqualTo(3);

        pt.delete(1, 1);
        assertThat(pt.getText()).isEqualTo("AC");
        assertThat(pt.length()).isEqualTo(2);
    }

    @Test
    @DisplayName("Insert operations at beginning, middle, and end")
    void testInserts() {
        PieceTable pt = new PieceTable("World");
        pt.insert(0, "Hello ");
        assertThat(pt.getText()).isEqualTo("Hello World");

        pt.insert(11, "!");
        assertThat(pt.getText()).isEqualTo("Hello World!");

        pt.insert(5, ",");
        assertThat(pt.getText()).isEqualTo("Hello, World!");
    }

    @Test
    @DisplayName("Delete operations at beginning, middle, and end")
    void testDeletes() {
        PieceTable pt = new PieceTable("0123456789");
        pt.delete(0, 2);
        assertThat(pt.getText()).isEqualTo("23456789");

        pt.delete(6, 2);
        assertThat(pt.getText()).isEqualTo("234567");

        pt.delete(2, 2);
        assertThat(pt.getText()).isEqualTo("2367");
    }

    @Test
    @DisplayName("Out of bounds operations throw appropriate exceptions")
    void testOutOfBounds() {
        PieceTable pt = new PieceTable("Test");
        assertThatThrownBy(() -> pt.charAt(10)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> pt.charAt(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> pt.insert(10, "A")).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> pt.delete(2, 5)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("Line counting and offset translation with various line endings")
    void testLineCalculations() {
        String text = "Line 0\nLine 1\r\nLine 2\rLine 3";
        PieceTable pt = new PieceTable(text);

        assertThat(pt.lineCount()).isEqualTo(4);
        assertThat(pt.offsetOfLine(0)).isEqualTo(0);
        assertThat(pt.offsetOfLine(1)).isEqualTo(7);
        assertThat(pt.offsetOfLine(2)).isEqualTo(15);
        assertThat(pt.offsetOfLine(3)).isEqualTo(22);

        assertThat(pt.lineOfOffset(0)).isEqualTo(0);
        assertThat(pt.lineOfOffset(5)).isEqualTo(0);
        assertThat(pt.lineOfOffset(7)).isEqualTo(1);
        assertThat(pt.lineOfOffset(14)).isEqualTo(1);
        assertThat(pt.lineOfOffset(15)).isEqualTo(2);
        assertThat(pt.lineOfOffset(21)).isEqualTo(2);
        assertThat(pt.lineOfOffset(22)).isEqualTo(3);
    }

    @Test
    @DisplayName("Random-edit fuzz test against StringBuilder oracle (5000 iterations)")
    void testRandomEditFuzzOracle() {
        StringBuilder oracle = new StringBuilder("The quick brown fox jumps over the lazy dog.\nPack my box with five dozen liquor jugs.\n");
        PieceTable pt = new PieceTable(oracle.toString());
        Random random = new Random(42);

        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 \n\r\t";

        for (int i = 0; i < 5000; i++) {
            int op = random.nextInt(3);
            if (op == 0 || oracle.length() == 0) {
                // Insert
                int offset = random.nextInt(oracle.length() + 1);
                int len = random.nextInt(10) + 1;
                StringBuilder insertText = new StringBuilder();
                for (int j = 0; j < len; j++) {
                    insertText.append(alphabet.charAt(random.nextInt(alphabet.length())));
                }
                String s = insertText.toString();
                oracle.insert(offset, s);
                pt.insert(offset, s);
            } else if (op == 1) {
                // Delete
                int offset = random.nextInt(oracle.length());
                int maxLen = Math.min(oracle.length() - offset, random.nextInt(15) + 1);
                oracle.delete(offset, offset + maxLen);
                pt.delete(offset, maxLen);
            } else {
                // Slice verification
                int start = random.nextInt(oracle.length());
                int end = start + random.nextInt(oracle.length() - start + 1);
                String oracleSlice = oracle.substring(start, end);
                String ptSlice = pt.slice(start, end);
                assertThat(ptSlice).isEqualTo(oracleSlice);
            }

            // Verify state
            assertThat(pt.length()).isEqualTo(oracle.length());
            if (i % 500 == 0) {
                assertThat(pt.getText()).isEqualTo(oracle.toString());
            }
        }

        // Final full verification
        assertThat(pt.getText()).isEqualTo(oracle.toString());
    }
}
