package com.textcraft.io;

import com.textcraft.exception.DocumentException;
import java.io.File;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

/** Plain text, code and data files (also the fallback for unknown extensions). */
public class TxtHandler extends AbstractFormatHandler {
    public TxtHandler() {
        super("txt", "log", "csv", "json", "xml", "py", "java", "c", "cpp", "h", "hpp", "js", "ts", "css",
                "sql", "sh", "bat", "yaml", "yml", "ini", "properties", "cfg", "tsv");
    }

    @Override
    public StyledDocument read(File f) throws DocumentException {
        StyledDocument d = newDocument();
        try {
            d.insertString(0, readString(f).replace("\r\n", "\n").replace('\r', '\n'), null);
        } catch (BadLocationException e) {
            throw new DocumentException("Cannot read " + f.getName(), e);
        }
        return d;
    }

    @Override
    public void write(StyledDocument d, File f) throws DocumentException {
        writeString(f, plainText(d));
    }
}
