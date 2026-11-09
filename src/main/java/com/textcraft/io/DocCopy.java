package com.textcraft.io;

import com.textcraft.exception.DocumentException;
import java.awt.Color;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Copies text and the formatting TextCraft understands from one StyledDocument to another.
 * Handlers read into a plain DefaultStyledDocument; the editor then copies it into its own document.
 * Black foregrounds are dropped so the text stays "automatic" (follows the theme).
 */
public final class DocCopy {
    private DocCopy() { }

    public static void copy(StyledDocument src, StyledDocument dst) throws DocumentException {
        try {
            dst.remove(0, dst.getLength());
            int len = src.getLength();
            Element root = src.getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                Element p = root.getElement(i);
                for (int j = 0; j < p.getElementCount(); j++) {
                    Element leaf = p.getElement(j);
                    int s = leaf.getStartOffset();
                    int e = Math.min(leaf.getEndOffset(), len);
                    if (e <= s) continue;
                    dst.insertString(dst.getLength(), src.getText(s, e - s), charAttrs(leaf));
                }
            }
            for (int i = 0; i < root.getElementCount(); i++) {
                Element p = root.getElement(i);
                int s = p.getStartOffset();
                if (s > len) continue;
                int e = Math.min(p.getEndOffset(), len);
                dst.setParagraphAttributes(s, Math.max(0, e - s), paraAttrs(p), false);
            }
        } catch (BadLocationException e) {
            throw new DocumentException("Could not copy the document", e);
        }
    }

    private static SimpleAttributeSet charAttrs(Element leaf) {
        SimpleAttributeSet out = new SimpleAttributeSet();
        Object v;
        if ((v = leaf.getAttributes().getAttribute(StyleConstants.FontFamily)) != null) {
            out.addAttribute(StyleConstants.FontFamily, v);
        }
        if ((v = leaf.getAttributes().getAttribute(StyleConstants.FontSize)) != null) {
            out.addAttribute(StyleConstants.FontSize, v);
        }
        copyTrue(leaf, out, StyleConstants.Bold);
        copyTrue(leaf, out, StyleConstants.Italic);
        copyTrue(leaf, out, StyleConstants.Underline);
        copyTrue(leaf, out, StyleConstants.StrikeThrough);
        v = leaf.getAttributes().getAttribute(StyleConstants.Foreground);
        if (v instanceof Color && !Color.BLACK.equals(v)) {
            out.addAttribute(StyleConstants.Foreground, v);
        }
        if ((v = leaf.getAttributes().getAttribute(StyleConstants.Background)) != null) {
            out.addAttribute(StyleConstants.Background, v);
        }
        return out;
    }

    private static void copyTrue(Element leaf, SimpleAttributeSet out, Object key) {
        if (Boolean.TRUE.equals(leaf.getAttributes().getAttribute(key))) out.addAttribute(key, Boolean.TRUE);
    }

    private static SimpleAttributeSet paraAttrs(Element p) {
        SimpleAttributeSet out = new SimpleAttributeSet();
        Object v;
        if ((v = p.getAttributes().getAttribute(DocAttrs.STYLE_KEY)) != null) {
            out.addAttribute(DocAttrs.STYLE_KEY, v);
        }
        if ((v = p.getAttributes().getAttribute(StyleConstants.Alignment)) != null) {
            out.addAttribute(StyleConstants.Alignment, v);
        }
        if ((v = p.getAttributes().getAttribute(StyleConstants.LineSpacing)) != null) {
            out.addAttribute(StyleConstants.LineSpacing, v);
        }
        return out;
    }
}
