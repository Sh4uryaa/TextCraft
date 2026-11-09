package com.textcraft.io;

import com.textcraft.exception.DocumentException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/** Word .docx through Apache POI: paragraphs, bold, italic, underline, strike, size, font, colour, headings. */
public class DocxHandler extends AbstractFormatHandler {
    public DocxHandler() {
        super("docx");
    }

    @Override
    public StyledDocument read(File f) throws DocumentException {
        StyledDocument d = newDocument();
        try (InputStream in = Files.newInputStream(f.toPath()); XWPFDocument docx = new XWPFDocument(in)) {
            boolean first = true;
            for (XWPFParagraph p : docx.getParagraphs()) {
                if (!first) d.insertString(d.getLength(), "\n", null);
                first = false;
                int start = d.getLength();
                for (XWPFRun r : p.getRuns()) {
                    String text = r.text();
                    if (text == null || text.isEmpty()) continue;
                    SimpleAttributeSet a = new SimpleAttributeSet();
                    if (r.isBold()) StyleConstants.setBold(a, true);
                    if (r.isItalic()) StyleConstants.setItalic(a, true);
                    if (r.getUnderline() != null && r.getUnderline() != UnderlinePatterns.NONE) {
                        StyleConstants.setUnderline(a, true);
                    }
                    if (r.isStrikeThrough()) StyleConstants.setStrikeThrough(a, true);
                    Double size = r.getFontSizeAsDouble();
                    if (size != null && size > 0) StyleConstants.setFontSize(a, (int) Math.round(size));
                    if (r.getFontFamily() != null) StyleConstants.setFontFamily(a, r.getFontFamily());
                    String color = r.getColor();
                    if (color != null && color.matches("[0-9A-Fa-f]{6}") && !"000000".equals(color)) {
                        StyleConstants.setForeground(a, Color.decode("#" + color));
                    }
                    d.insertString(d.getLength(), text.replace("\n", " "), a);
                }
                SimpleAttributeSet pa = new SimpleAttributeSet();
                pa.addAttribute(DocAttrs.STYLE_KEY, styleFromDocx(p.getStyle()));
                ParagraphAlignment al = p.getAlignment();
                if (al == ParagraphAlignment.CENTER) StyleConstants.setAlignment(pa, StyleConstants.ALIGN_CENTER);
                else if (al == ParagraphAlignment.RIGHT) StyleConstants.setAlignment(pa, StyleConstants.ALIGN_RIGHT);
                else if (al == ParagraphAlignment.BOTH) StyleConstants.setAlignment(pa, StyleConstants.ALIGN_JUSTIFIED);
                d.setParagraphAttributes(start, Math.max(0, d.getLength() - start), pa, false);
            }
        } catch (IOException | BadLocationException | RuntimeException e) {
            throw new DocumentException("Cannot read Word file " + f.getName() + ": " + e.getMessage(), e);
        }
        return d;
    }

    @Override
    public void write(StyledDocument d, File f) throws DocumentException {
        try (XWPFDocument docx = new XWPFDocument(); OutputStream out = Files.newOutputStream(f.toPath())) {
            int len = d.getLength();
            Element root = d.getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                Element p = root.getElement(i);
                XWPFParagraph para = docx.createParagraph();
                String style = DocAttrs.styleOf(p.getAttributes());
                String docxStyle = styleToDocx(style);
                if (docxStyle != null) para.setStyle(docxStyle);
                switch (StyleConstants.getAlignment(p.getAttributes())) {
                    case StyleConstants.ALIGN_CENTER: para.setAlignment(ParagraphAlignment.CENTER); break;
                    case StyleConstants.ALIGN_RIGHT: para.setAlignment(ParagraphAlignment.RIGHT); break;
                    case StyleConstants.ALIGN_JUSTIFIED: para.setAlignment(ParagraphAlignment.BOTH); break;
                    default: break;
                }
                for (int j = 0; j < p.getElementCount(); j++) {
                    Element leaf = p.getElement(j);
                    int s = leaf.getStartOffset();
                    int e = Math.min(leaf.getEndOffset(), len);
                    if (e <= s) continue;
                    String text = d.getText(s, e - s).replace("\n", "");
                    if (text.isEmpty()) continue;
                    XWPFRun run = para.createRun();
                    run.setText(text);
                    javax.swing.text.AttributeSet a = leaf.getAttributes();
                    run.setBold(StyleConstants.isBold(a));
                    run.setItalic(StyleConstants.isItalic(a));
                    if (StyleConstants.isUnderline(a)) run.setUnderline(UnderlinePatterns.SINGLE);
                    run.setStrikeThrough(StyleConstants.isStrikeThrough(a));
                    run.setFontFamily(a.isDefined(StyleConstants.FontFamily)
                            ? StyleConstants.getFontFamily(a) : DocAttrs.DEFAULT_FONT);
                    run.setFontSize(a.isDefined(StyleConstants.FontSize)
                            ? StyleConstants.getFontSize(a) : DocAttrs.DEFAULT_SIZE);
                    if (a.isDefined(StyleConstants.Foreground)) {
                        Color c = StyleConstants.getForeground(a);
                        run.setColor(String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()));
                    }
                }
            }
            docx.write(out);
        } catch (IOException | BadLocationException | RuntimeException e) {
            throw new DocumentException("Cannot write Word file " + f.getName() + ": " + e.getMessage(), e);
        }
    }

    private static String styleFromDocx(String s) {
        if (s == null) return DocAttrs.NORMAL;
        switch (s.toLowerCase().replace(" ", "")) {
            case "title": return DocAttrs.TITLE;
            case "heading1": return DocAttrs.H1;
            case "heading2": return DocAttrs.H2;
            case "heading3": return DocAttrs.H3;
            default: return DocAttrs.NORMAL;
        }
    }

    private static String styleToDocx(String s) {
        switch (s) {
            case DocAttrs.TITLE: return "Title";
            case DocAttrs.H1: return "Heading1";
            case DocAttrs.H2: return "Heading2";
            case DocAttrs.H3: return "Heading3";
            default: return null;
        }
    }
}
