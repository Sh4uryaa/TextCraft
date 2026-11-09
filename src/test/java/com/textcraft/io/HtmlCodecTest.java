package com.textcraft.io;

import static org.junit.jupiter.api.Assertions.*;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.junit.jupiter.api.Test;

class HtmlCodecTest {
    @Test
    void headingsListsAndStylesRoundTrip() throws Exception {
        String html = "<h1>Title</h1><p>Plain <b>bold</b> and <i>italic</i></p><ul><li>one</li><li>two</li></ul>"
                + "<ol><li>first</li></ol><p style=\"text-align:center;\">centred</p>";
        DefaultStyledDocument d = new DefaultStyledDocument();
        HtmlCodec.fromHtml(html, d);
        String text = d.getText(0, d.getLength());
        assertTrue(text.startsWith("Title\n"));
        assertTrue(text.contains("• one"));
        assertTrue(text.contains("1. first"));
        assertEquals(DocAttrs.H1, DocAttrs.styleOf(d.getParagraphElement(0).getAttributes()));
        int centred = text.indexOf("centred");
        assertEquals(StyleConstants.ALIGN_CENTER, StyleConstants.getAlignment(d.getParagraphElement(centred).getAttributes()));
        int bold = text.indexOf("bold");
        assertTrue(StyleConstants.isBold(d.getCharacterElement(bold).getAttributes()));
    }

    @Test
    void toHtmlThenBackKeepsTextAndColour() throws Exception {
        DefaultStyledDocument d = new DefaultStyledDocument();
        SimpleAttributeSet red = new SimpleAttributeSet();
        StyleConstants.setForeground(red, java.awt.Color.RED);
        StyleConstants.setFontSize(red, 18);
        d.insertString(0, "a < b & c", red);
        String html = HtmlCodec.toHtml(d, false);
        assertTrue(html.contains("&lt;"));
        DefaultStyledDocument back = new DefaultStyledDocument();
        HtmlCodec.fromHtml(html, back);
        assertEquals("a < b & c", back.getText(0, back.getLength()));
        assertEquals(java.awt.Color.RED, StyleConstants.getForeground(back.getCharacterElement(0).getAttributes()));
        assertEquals(18, StyleConstants.getFontSize(back.getCharacterElement(0).getAttributes()));
    }

    @Test
    void automaticColourIsNotWrittenToHtml() throws Exception {
        DefaultStyledDocument d = new DefaultStyledDocument();
        d.insertString(0, "plain", null);
        assertFalse(HtmlCodec.toHtml(d, false).contains("color:"));
    }

    @Test
    void standaloneExportIsAWhitePageWithBlackText() throws Exception {
        DefaultStyledDocument d = new DefaultStyledDocument();
        d.insertString(0, "x", null);
        String html = HtmlCodec.toHtml(d, true);
        assertTrue(html.contains("color:#000000"));
        assertTrue(html.contains("background:#ffffff"));
    }

    @Test
    void emptyDocumentDoesNotFail() throws Exception {
        DefaultStyledDocument d = new DefaultStyledDocument();
        DefaultStyledDocument back = new DefaultStyledDocument();
        HtmlCodec.fromHtml(HtmlCodec.toHtml(d, false), back);
        assertEquals("", back.getText(0, back.getLength()));
    }
}
