package com.textcraft.io;

import com.textcraft.exception.DocumentException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

/**
 * OOP: Inheritance - shared code for all handlers (extension check, UTF-8 helpers, new document).
 */
public abstract class AbstractFormatHandler implements FileFormatHandler {
    private final Set<String> extensions; // OOP: collections - Set of supported extensions

    protected AbstractFormatHandler(String... exts) {
        this.extensions = new HashSet<>(Arrays.asList(exts));
    }

    @Override
    public boolean supports(String ext) {
        return ext != null && extensions.contains(ext.toLowerCase());
    }

    public Set<String> extensions() {
        return extensions;
    }

    protected StyledDocument newDocument() {
        return new DefaultStyledDocument();
    }

    protected static String readString(File f) throws DocumentException {
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DocumentException("Cannot read " + f.getName() + ": " + e.getMessage(), e);
        }
    }

    protected static void writeString(File f, String text) throws DocumentException {
        try {
            Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DocumentException("Cannot write " + f.getName() + ": " + e.getMessage(), e);
        }
    }

    protected static String plainText(StyledDocument d) throws DocumentException {
        try {
            return d.getText(0, d.getLength());
        } catch (BadLocationException e) {
            throw new DocumentException("Document changed while saving", e);
        }
    }

    /** True when the first 8 KB contain a NUL byte, i.e. the file is probably not text. */
    public static boolean looksBinary(File f) {
        try (InputStream in = Files.newInputStream(f.toPath())) {
            byte[] buf = new byte[8192];
            int n = in.read(buf);
            for (int i = 0; i < n; i++) {
                if (buf[i] == 0) return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /** Plain text helper used by the code editor too. */
    public static String readPlainText(File f) throws DocumentException {
        return readString(f);
    }
}
