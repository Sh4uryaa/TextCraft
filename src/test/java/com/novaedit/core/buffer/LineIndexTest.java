package com.novaedit.core.buffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LineIndexTest {

    @Test
    @DisplayName("Line index correctly handles LF, CRLF, and CR line endings")
    void testLineEndings() {
        LineIndex index = new LineIndex();
        String text = "Line 1\nLine 2\r\nLine 3\rLine 4";
        index.rebuild(text);

        assertThat(index.getLineCount()).isEqualTo(4);

        // Offsets
        assertThat(index.getOffsetOfLine(0, text.length())).isEqualTo(0);
        assertThat(index.getOffsetOfLine(1, text.length())).isEqualTo(7);
        assertThat(index.getOffsetOfLine(2, text.length())).isEqualTo(15);
        assertThat(index.getOffsetOfLine(3, text.length())).isEqualTo(22);

        // Lines for offset
        assertThat(index.getLineOfOffset(0, text.length())).isEqualTo(0);
        assertThat(index.getLineOfOffset(6, text.length())).isEqualTo(0);
        assertThat(index.getLineOfOffset(7, text.length())).isEqualTo(1);
        assertThat(index.getLineOfOffset(14, text.length())).isEqualTo(1);
        assertThat(index.getLineOfOffset(15, text.length())).isEqualTo(2);
        assertThat(index.getLineOfOffset(21, text.length())).isEqualTo(2);
        assertThat(index.getLineOfOffset(22, text.length())).isEqualTo(3);
        assertThat(index.getLineOfOffset(28, text.length())).isEqualTo(3);
    }

    @Test
    @DisplayName("Empty and single line text line index")
    void testSingleLine() {
        LineIndex index = new LineIndex();
        index.rebuild("Hello");
        assertThat(index.getLineCount()).isEqualTo(1);
        assertThat(index.getOffsetOfLine(0, 5)).isEqualTo(0);
        assertThat(index.getLineOfOffset(2, 5)).isEqualTo(0);
    }
}
