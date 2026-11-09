package com.textcraft.io;

import com.textcraft.exception.DocumentException;
import java.io.File;
import javax.swing.text.StyledDocument;

/** OOP: Interface (Strategy) - one implementation per file type. */
public interface FileFormatHandler {
    boolean supports(String ext);

    StyledDocument read(File f) throws DocumentException;

    void write(StyledDocument d, File f) throws DocumentException;
}
