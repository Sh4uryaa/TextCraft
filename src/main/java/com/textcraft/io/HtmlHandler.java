package com.textcraft.io;

import com.textcraft.exception.DocumentException;
import java.io.File;
import javax.swing.text.StyledDocument;

/** HTML files. Export always writes a white page with automatic (black) text. */
public class HtmlHandler extends AbstractFormatHandler {
    public HtmlHandler() {
        super("html", "htm");
    }

    @Override
    public StyledDocument read(File f) throws DocumentException {
        StyledDocument d = newDocument();
        HtmlCodec.fromHtml(readString(f), d);
        return d;
    }

    @Override
    public void write(StyledDocument d, File f) throws DocumentException {
        writeString(f, HtmlCodec.toHtml(d, true));
    }
}
