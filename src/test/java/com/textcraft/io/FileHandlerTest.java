package com.textcraft.io;

import static org.junit.jupiter.api.Assertions.*;

import com.textcraft.exception.DocumentException;
import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Every handler: write a small formatted document, read it back, text and key formatting must survive. */
class FileHandlerTest {
    @TempDir
    Path dir;

    private static StyledDocument sample() throws Exception {
        DefaultStyledDocument d = new DefaultStyledDocument();
        SimpleAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        d.insertString(0, "Hello ", null);
        d.insertString(d.getLength(), "World", bold);
        d.insertString(d.getLength(), "\nSecond line", null);
        return d;
    }

    private static String text(StyledDocument d) throws Exception {
        return d.getText(0, d.getLength());
    }

    private static boolean hasBold(StyledDocument d) {
        for (int i = 0; i < d.getLength(); i++) {
            if (StyleConstants.isBold(d.getCharacterElement(i).getAttributes())) return true;
        }
        return false;
    }

    private void roundTrip(String name, boolean checkBold) throws Exception {
        File f = dir.resolve(name).toFile();
        FileFormatHandler h = FileHandlerFactory.forFile(f);
        h.write(sample(), f);
        assertTrue(f.length() > 0);
        StyledDocument back = h.read(f);
        String t = text(back).replace("\r", "");
        assertTrue(t.contains("Hello"), name + " lost text: " + t);
        assertTrue(t.contains("World"), name + " lost text: " + t);
        assertTrue(t.contains("Second line"), name + " lost text: " + t);
        if (checkBold) assertTrue(hasBold(back), name + " lost bold");
    }

    @Test void txt() throws Exception { roundTrip("a.txt", false); }
    @Test void html() throws Exception { roundTrip("a.html", true); }
    @Test void rtf() throws Exception { roundTrip("a.rtf", true); }
    @Test void docx() throws Exception { roundTrip("a.docx", true); }
    @Test void markdown() throws Exception { roundTrip("a.md", true); }

    @Test
    void pdfWritesAndReadsTextBack() throws Exception {
        roundTrip("a.pdf", false);
    }

    @Test
    void unknownExtensionFallsBackToPlainText() throws Exception {
        File f = dir.resolve("data.weird").toFile();
        Files.write(f.toPath(), "plain content".getBytes("UTF-8"));
        assertEquals("plain content", text(FileHandlerFactory.forFile(f).read(f)));
        assertFalse(FileHandlerFactory.isKnown("weird"));
    }

    @Test
    void factoryPicksHandlersByExtension() {
        assertTrue(FileHandlerFactory.forExtension("DOCX") instanceof DocxHandler);
        assertTrue(FileHandlerFactory.forExtension("md") instanceof MdHandler);
        assertTrue(FileHandlerFactory.forExtension("pdf") instanceof PdfHandler);
        assertTrue(FileHandlerFactory.isCode("py"));
        assertFalse(FileHandlerFactory.isCode("docx"));
    }

    @Test
    void binaryDetection() throws Exception {
        File f = dir.resolve("b.bin").toFile();
        Files.write(f.toPath(), new byte[]{1, 2, 0, 3});
        assertTrue(AbstractFormatHandler.looksBinary(f));
    }

    @Test
    void missingFileGivesDocumentException() {
        assertThrows(DocumentException.class, () -> new TxtHandler().read(dir.resolve("nope.txt").toFile()));
    }

    @Test
    void automaticColourIsNotStoredAsBlack() throws Exception {
        DefaultStyledDocument src = new DefaultStyledDocument();
        SimpleAttributeSet black = new SimpleAttributeSet();
        StyleConstants.setForeground(black, Color.BLACK);
        SimpleAttributeSet red = new SimpleAttributeSet();
        StyleConstants.setForeground(red, Color.RED);
        src.insertString(0, "auto", black);
        src.insertString(4, "red", red);
        DefaultStyledDocument dst = new DefaultStyledDocument();
        DocCopy.copy(src, dst);
        Element a = dst.getCharacterElement(0);
        Element r = dst.getCharacterElement(5);
        assertFalse(a.getAttributes().isDefined(StyleConstants.Foreground));
        assertEquals(Color.RED, StyleConstants.getForeground(r.getAttributes()));
    }
}
