package com.textcraft.io;

import com.textcraft.exception.DocumentException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

/** RTF through the JDK's RTFEditorKit. */
public class RtfHandler extends AbstractFormatHandler {
    public RtfHandler() {
        super("rtf");
    }

    @Override
    public StyledDocument read(File f) throws DocumentException {
        StyledDocument d = newDocument();
        try (InputStream in = Files.newInputStream(f.toPath())) {
            new RTFEditorKit().read(in, d, 0);
        } catch (IOException | BadLocationException e) {
            throw new DocumentException("Cannot read RTF " + f.getName() + ": " + e.getMessage(), e);
        }
        return d;
    }

    @Override
    public void write(StyledDocument d, File f) throws DocumentException {
        try (OutputStream out = Files.newOutputStream(f.toPath())) {
            new RTFEditorKit().write(out, d, 0, d.getLength());
        } catch (IOException | BadLocationException e) {
            throw new DocumentException("Cannot write RTF " + f.getName() + ": " + e.getMessage(), e);
        }
    }
}
