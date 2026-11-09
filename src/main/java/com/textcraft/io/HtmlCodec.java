package com.textcraft.io;

import com.textcraft.exception.DocumentException;
import com.textcraft.util.TextUtils;
import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Converts between a StyledDocument and a small, predictable HTML subset.
 * Used for cloud storage (rich documents are stored as HTML), the HTML handler and Markdown import.
 * Own code instead of the JDK HTML kit because the kit cannot fill a plain StyledDocument.
 */
public final class HtmlCodec {
    private static final String RULE = "────────────────";

    private HtmlCodec() { }

    // ===== document -> HTML ==========================================================

    public static String toHtml(StyledDocument doc, boolean standalone) {
        StringBuilder sb = new StringBuilder();
        if (standalone) {
            sb.append("<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\"><title>Document</title></head>\n")
              .append("<body style=\"font-family:Arial;font-size:12pt;color:#000000;background:#ffffff\">\n");
        }
        try {
            int len = doc.getLength();
            Element root = doc.getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                Element p = root.getElement(i);
                String style = DocAttrs.styleOf(p.getAttributes());
                String tag = "p";
                String cls = "";
                if (DocAttrs.H1.equals(style)) tag = "h1";
                else if (DocAttrs.H2.equals(style)) tag = "h2";
                else if (DocAttrs.H3.equals(style)) tag = "h3";
                else if (DocAttrs.TITLE.equals(style)) cls = " class=\"title\"";
                String css = paragraphCss(p.getAttributes());
                sb.append('<').append(tag).append(cls);
                if (!css.isEmpty()) sb.append(" style=\"").append(css).append('"');
                sb.append('>');
                boolean any = false;
                for (int j = 0; j < p.getElementCount(); j++) {
                    Element leaf = p.getElement(j);
                    int s = leaf.getStartOffset();
                    int e = Math.min(leaf.getEndOffset(), len);
                    if (e <= s) continue;
                    String text = doc.getText(s, e - s).replace("\n", "");
                    if (text.isEmpty()) continue;
                    sb.append(runHtml(leaf.getAttributes(), text));
                    any = true;
                }
                if (!any) sb.append("<br>");
                sb.append("</").append(tag).append(">\n");
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException("Document changed while converting", e);
        }
        if (standalone) sb.append("</body></html>\n");
        return sb.toString();
    }

    private static String paragraphCss(AttributeSet a) {
        StringBuilder css = new StringBuilder();
        switch (StyleConstants.getAlignment(a)) {
            case StyleConstants.ALIGN_CENTER: css.append("text-align:center;"); break;
            case StyleConstants.ALIGN_RIGHT: css.append("text-align:right;"); break;
            case StyleConstants.ALIGN_JUSTIFIED: css.append("text-align:justify;"); break;
            default: break;
        }
        float ls = StyleConstants.getLineSpacing(a);
        if (ls > 0.01f) css.append(String.format(Locale.ROOT, "line-height:%.2f;", 1f + ls));
        return css.toString();
    }

    private static String runHtml(AttributeSet a, String text) {
        StringBuilder open = new StringBuilder();
        StringBuilder close = new StringBuilder();
        StringBuilder css = new StringBuilder();
        if (a.isDefined(StyleConstants.FontFamily)) {
            css.append("font-family:").append(StyleConstants.getFontFamily(a)).append(';');
        }
        if (a.isDefined(StyleConstants.FontSize)) {
            css.append("font-size:").append(StyleConstants.getFontSize(a)).append("pt;");
        }
        if (a.isDefined(StyleConstants.Foreground)) {
            css.append("color:").append(hex(StyleConstants.getForeground(a))).append(';');
        }
        if (a.isDefined(StyleConstants.Background)) {
            css.append("background-color:").append(hex(StyleConstants.getBackground(a))).append(';');
        }
        if (css.length() > 0) {
            open.append("<span style=\"").append(css).append("\">");
            close.insert(0, "</span>");
        }
        if (StyleConstants.isBold(a)) { open.append("<b>"); close.insert(0, "</b>"); }
        if (StyleConstants.isItalic(a)) { open.append("<i>"); close.insert(0, "</i>"); }
        if (StyleConstants.isUnderline(a)) { open.append("<u>"); close.insert(0, "</u>"); }
        if (StyleConstants.isStrikeThrough(a)) { open.append("<s>"); close.insert(0, "</s>"); }
        return open + TextUtils.escapeHtml(text) + close;
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ===== HTML -> document ==========================================================

    /** Replaces the content of doc with the parsed HTML. */
    public static void fromHtml(String html, StyledDocument doc) throws DocumentException {
        try {
            doc.remove(0, doc.getLength());
            Builder b = new Builder(doc);
            new ParserDelegator().parse(new StringReader(html), b, true);
            b.finish();
        } catch (IOException | BadLocationException e) {
            throw new DocumentException("Could not read the HTML", e);
        }
    }

    /** Receives parser events and writes them into the document. */
    private static final class Builder extends HTMLEditorKit.ParserCallback {
        private final StyledDocument doc;
        private int bold, italic, underline, strike, skip;
        private final Deque<SimpleAttributeSet> spans = new ArrayDeque<>();
        private final Deque<int[]> lists = new ArrayDeque<>(); // [0]=ordered?1:0, [1]=counter
        private int blockCount;
        private boolean inBlock, atBlockStart;
        private int blockStart = -1;
        private String blockStyle = DocAttrs.NORMAL;
        private int blockAlign = StyleConstants.ALIGN_LEFT;
        private float blockSpacing;
        private int blockSize;       // default size for headings/title, 0 = none
        private boolean blockBold;

        Builder(StyledDocument doc) {
            this.doc = doc;
        }

        @Override
        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            String n = t.toString().toLowerCase(Locale.ROOT);
            switch (n) {
                case "head": case "title": case "style": case "script": skip++; break;
                case "b": case "strong": bold++; break;
                case "i": case "em": italic++; break;
                case "u": underline++; break;
                case "s": case "strike": case "del": strike++; break;
                case "span": case "font": spans.push(parseCss(a)); break;
                case "h1": startBlock(DocAttrs.H1, a); break;
                case "h2": startBlock(DocAttrs.H2, a); break;
                case "h3": startBlock(DocAttrs.H3, a); break;
                case "p":
                    Object cls = a.getAttribute(HTML.Attribute.CLASS);
                    startBlock("title".equals(String.valueOf(cls)) ? DocAttrs.TITLE : DocAttrs.NORMAL, a);
                    break;
                case "ul": lists.push(new int[]{0, 0}); break;
                case "ol": lists.push(new int[]{1, 0}); break;
                case "li":
                    startBlock(DocAttrs.NORMAL, a);
                    String prefix = "• ";
                    if (!lists.isEmpty() && lists.peek()[0] == 1) prefix = (++lists.peek()[1]) + ". ";
                    insert(prefix);
                    break;
                default: break;
            }
        }

        @Override
        public void handleEndTag(HTML.Tag t, int pos) {
            String n = t.toString().toLowerCase(Locale.ROOT);
            switch (n) {
                case "head": case "title": case "style": case "script": if (skip > 0) skip--; break;
                case "b": case "strong": if (bold > 0) bold--; break;
                case "i": case "em": if (italic > 0) italic--; break;
                case "u": if (underline > 0) underline--; break;
                case "s": case "strike": case "del": if (strike > 0) strike--; break;
                case "span": case "font": if (!spans.isEmpty()) spans.pop(); break;
                case "ul": case "ol": if (!lists.isEmpty()) lists.pop(); break;
                case "h1": case "h2": case "h3": case "p": case "li":
                    inBlock = false;
                    blockSize = 0;
                    blockBold = false;
                    break;
                default: break;
            }
        }

        @Override
        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            String n = t.toString().toLowerCase(Locale.ROOT);
            if (skip > 0) return;
            if ("br".equals(n)) {
                if (atBlockStart || !inBlock) {
                    atBlockStart = false;
                } else {
                    insert("\n");
                }
            } else if ("hr".equals(n)) {
                startBlock(DocAttrs.NORMAL, a);
                insert(RULE);
                inBlock = false;
            }
        }

        @Override
        public void handleText(char[] data, int pos) {
            if (skip > 0) return;
            String text = new String(data).replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
            if (!inBlock) {
                if (text.trim().isEmpty()) return;
                startBlock(DocAttrs.NORMAL, new SimpleAttributeSet());
            }
            if (atBlockStart) text = text.replaceFirst("^\\s+", "");
            if (text.isEmpty()) return;
            insert(text);
        }

        void finish() {
            applyBlock();
        }

        private void startBlock(String style, AttributeSet a) {
            applyBlock();
            if (blockCount > 0) insertRaw("\n", new SimpleAttributeSet());
            blockCount++;
            inBlock = true;
            atBlockStart = true;
            blockStart = doc.getLength();
            blockStyle = style;
            blockSize = DocAttrs.sizeFor(style);
            blockBold = DocAttrs.boldFor(style);
            if (blockSize == DocAttrs.DEFAULT_SIZE) blockSize = 0;
            blockAlign = StyleConstants.ALIGN_LEFT;
            blockSpacing = 0f;
            Object css = a.getAttribute(HTML.Attribute.STYLE);
            if (css != null) {
                String s = css.toString().toLowerCase(Locale.ROOT).replace(" ", "");
                if (s.contains("text-align:center")) blockAlign = StyleConstants.ALIGN_CENTER;
                else if (s.contains("text-align:right")) blockAlign = StyleConstants.ALIGN_RIGHT;
                else if (s.contains("text-align:justify")) blockAlign = StyleConstants.ALIGN_JUSTIFIED;
                int i = s.indexOf("line-height:");
                if (i >= 0) {
                    try {
                        String num = s.substring(i + 12).split("[;\"]")[0];
                        blockSpacing = Math.max(0f, Float.parseFloat(num) - 1f);
                    } catch (NumberFormatException ignored) {
                        blockSpacing = 0f;
                    }
                }
            }
        }

        private void applyBlock() {
            if (blockStart < 0) return;
            SimpleAttributeSet pa = new SimpleAttributeSet();
            pa.addAttribute(DocAttrs.STYLE_KEY, blockStyle);
            StyleConstants.setAlignment(pa, blockAlign);
            if (blockSpacing > 0f) StyleConstants.setLineSpacing(pa, blockSpacing);
            doc.setParagraphAttributes(blockStart, Math.max(0, doc.getLength() - blockStart), pa, false);
            blockStart = -1;
        }

        private void insert(String text) {
            insertRaw(text, currentAttrs());
            atBlockStart = false;
        }

        private void insertRaw(String text, AttributeSet attrs) {
            try {
                doc.insertString(doc.getLength(), text, attrs);
            } catch (BadLocationException e) {
                throw new IllegalStateException(e);
            }
        }

        private SimpleAttributeSet currentAttrs() {
            SimpleAttributeSet a = new SimpleAttributeSet();
            if (bold > 0 || blockBold) StyleConstants.setBold(a, true);
            if (italic > 0) StyleConstants.setItalic(a, true);
            if (underline > 0) StyleConstants.setUnderline(a, true);
            if (strike > 0) StyleConstants.setStrikeThrough(a, true);
            if (blockSize > 0) StyleConstants.setFontSize(a, blockSize);
            // inner spans win over outer spans: iterate from outermost to innermost
            for (java.util.Iterator<SimpleAttributeSet> it = spans.descendingIterator(); it.hasNext();) {
                a.addAttributes(it.next());
            }
            return a;
        }

        private SimpleAttributeSet parseCss(AttributeSet tagAttrs) {
            SimpleAttributeSet out = new SimpleAttributeSet();
            Object css = tagAttrs.getAttribute(HTML.Attribute.STYLE);
            if (css == null) return out;
            for (String decl : css.toString().split(";")) {
                int c = decl.indexOf(':');
                if (c < 0) continue;
                String key = decl.substring(0, c).trim().toLowerCase(Locale.ROOT);
                String val = decl.substring(c + 1).trim();
                try {
                    switch (key) {
                        case "font-family":
                            StyleConstants.setFontFamily(out, val.split(",")[0].replace("'", "").replace("\"", "").trim());
                            break;
                        case "font-size":
                            StyleConstants.setFontSize(out, parseSize(val));
                            break;
                        case "color":
                            StyleConstants.setForeground(out, Color.decode(val));
                            break;
                        case "background-color":
                            StyleConstants.setBackground(out, Color.decode(val));
                            break;
                        default: break;
                    }
                } catch (NumberFormatException ignored) {
                    // ignore values we do not understand (e.g. named colours)
                }
            }
            return out;
        }

        private int parseSize(String v) {
            v = v.toLowerCase(Locale.ROOT);
            if (v.endsWith("px")) return Math.round(Float.parseFloat(v.replace("px", "")) * 0.75f);
            return Math.round(Float.parseFloat(v.replace("pt", "")));
        }
    }
}
