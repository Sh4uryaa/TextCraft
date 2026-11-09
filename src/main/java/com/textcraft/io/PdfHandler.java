package com.textcraft.io;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.textcraft.exception.DocumentException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * PDF: text extraction on open (PDFBox) and simple export on save (OpenPDF).
 * Layout, images and fonts of an opened PDF are NOT preserved - the UI says so.
 */
public class PdfHandler extends AbstractFormatHandler {
    public PdfHandler() {
        super("pdf");
    }

    @Override
    public StyledDocument read(File f) throws DocumentException {
        StyledDocument d = newDocument();
        try (PDDocument pdf = PDDocument.load(f)) {
            String text = new PDFTextStripper().getText(pdf).replace("\r\n", "\n").trim();
            d.insertString(0, text, null);
        } catch (IOException | BadLocationException e) {
            throw new DocumentException("Cannot read PDF " + f.getName() + ": " + e.getMessage(), e);
        }
        return d;
    }

    @Override
    public void write(StyledDocument d, File f) throws DocumentException {
        com.lowagie.text.Document pdf = new com.lowagie.text.Document(PageSize.A4, 72, 72, 72, 72);
        OutputStream out = null;
        try {
            out = Files.newOutputStream(f.toPath());
            PdfWriter.getInstance(pdf, out);
            pdf.open();
            int len = d.getLength();
            javax.swing.text.Element root = d.getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                javax.swing.text.Element p = root.getElement(i);
                Paragraph para = new Paragraph();
                switch (StyleConstants.getAlignment(p.getAttributes())) {
                    case StyleConstants.ALIGN_CENTER: para.setAlignment(Element.ALIGN_CENTER); break;
                    case StyleConstants.ALIGN_RIGHT: para.setAlignment(Element.ALIGN_RIGHT); break;
                    case StyleConstants.ALIGN_JUSTIFIED: para.setAlignment(Element.ALIGN_JUSTIFIED); break;
                    default: break;
                }
                boolean any = false;
                for (int j = 0; j < p.getElementCount(); j++) {
                    javax.swing.text.Element leaf = p.getElement(j);
                    int s = leaf.getStartOffset();
                    int e = Math.min(leaf.getEndOffset(), len);
                    if (e <= s) continue;
                    String text = d.getText(s, e - s).replace("\n", "");
                    if (text.isEmpty()) continue;
                    javax.swing.text.AttributeSet a = leaf.getAttributes();
                    int style = com.lowagie.text.Font.NORMAL;
                    if (StyleConstants.isBold(a)) style |= com.lowagie.text.Font.BOLD;
                    if (StyleConstants.isItalic(a)) style |= com.lowagie.text.Font.ITALIC;
                    if (StyleConstants.isUnderline(a)) style |= com.lowagie.text.Font.UNDERLINE;
                    if (StyleConstants.isStrikeThrough(a)) style |= com.lowagie.text.Font.STRIKETHRU;
                    float size = a.isDefined(StyleConstants.FontSize)
                            ? StyleConstants.getFontSize(a) : DocAttrs.DEFAULT_SIZE;
                    Color color = a.isDefined(StyleConstants.Foreground)
                            ? StyleConstants.getForeground(a) : Color.BLACK; // automatic text = black on white
                    para.add(new Chunk(text, FontFactory.getFont(FontFactory.HELVETICA, size, style, color)));
                    any = true;
                }
                if (!any) para.add(new Chunk(" "));
                pdf.add(para);
            }
        } catch (IOException | BadLocationException | com.lowagie.text.DocumentException e) {
            throw new DocumentException("Cannot write PDF " + f.getName() + ": " + e.getMessage(), e);
        } finally {
            // close the PDF first (it flushes into the stream), then the stream
            if (pdf.isOpen()) pdf.close();
            if (out != null) {
                try { out.close(); } catch (IOException ignored) { /* nothing more to do */ }
            }
        }
    }
}
