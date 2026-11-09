package com.textcraft.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TextUtilsTest {
    @Test
    void wordCount() {
        assertEquals(0, TextUtils.wordCount(""));
        assertEquals(0, TextUtils.wordCount("   \n "));
        assertEquals(3, TextUtils.wordCount("one two\nthree"));
        assertEquals(2, TextUtils.wordCount("  spaced   out  "));
    }

    @Test
    void extensionAndBaseName() {
        assertEquals("docx", TextUtils.extension("Report.DOCX"));
        assertEquals("", TextUtils.extension("README"));
        assertEquals("notes", TextUtils.baseName("notes.txt"));
        assertEquals("a.b", TextUtils.baseName("a.b.c"));
    }

    @Test
    void csvQuotesAndEscapes() {
        assertEquals("\"say \"\"hi\"\", ok\"", TextUtils.csv("say \"hi\", ok"));
    }

    @Test
    void htmlEscape() {
        assertEquals("&lt;b&gt; &amp; &quot;x&quot;", TextUtils.escapeHtml("<b> & \"x\""));
    }
}
