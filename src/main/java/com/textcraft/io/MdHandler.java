package com.textcraft.io;

import com.textcraft.exception.DocumentException;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import java.io.File;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyleConstants;

/**
 * Markdown. Reading: flexmark turns Markdown into HTML, HtmlCodec turns that into a document.
 * Writing: a small own writer (headings, bold, italic, strike, bullet lists).
 */
public class MdHandler extends AbstractFormatHandler {
    public MdHandler() {
        super("md", "markdown");
    }

    @Override
    public StyledDocument read(File f) throws DocumentException {
        Parser parser = Parser.builder().build();
        Node node = parser.parse(readString(f));
        String html = HtmlRenderer.builder().build().render(node);
        StyledDocument d = newDocument();
        HtmlCodec.fromHtml(html, d);
        return d;
    }

    @Override
    public void write(StyledDocument d, File f) throws DocumentException {
        writeString(f, toMarkdown(d));
    }

    static String toMarkdown(StyledDocument d) throws DocumentException {
        StringBuilder sb = new StringBuilder();
        try {
            int len = d.getLength();
            Element root = d.getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                Element p = root.getElement(i);
                String style = DocAttrs.styleOf(p.getAttributes());
                StringBuilder line = new StringBuilder();
                for (int j = 0; j < p.getElementCount(); j++) {
                    Element leaf = p.getElement(j);
                    int s = leaf.getStartOffset();
                    int e = Math.min(leaf.getEndOffset(), len);
                    if (e <= s) continue;
                    String text = d.getText(s, e - s).replace("\n", "");
                    if (text.isEmpty()) continue;
                    boolean heading = !DocAttrs.NORMAL.equals(style);
                    boolean bold = StyleConstants.isBold(leaf.getAttributes()) && !heading;
                    boolean italic = StyleConstants.isItalic(leaf.getAttributes());
                    boolean strike = StyleConstants.isStrikeThrough(leaf.getAttributes());
                    if (bold) text = "**" + text + "**";
                    if (italic) text = "*" + text + "*";
                    if (strike) text = "~~" + text + "~~";
                    line.append(text);
                }
                String l = line.toString();
                if (l.startsWith("• ")) l = "- " + l.substring(2);
                switch (style) {
                    case DocAttrs.TITLE: case DocAttrs.H1: l = "# " + l; break;
                    case DocAttrs.H2: l = "## " + l; break;
                    case DocAttrs.H3: l = "### " + l; break;
                    default: break;
                }
                sb.append(l).append("\n\n");
            }
        } catch (BadLocationException e) {
            throw new DocumentException("Document changed while saving", e);
        }
        return sb.toString().replaceAll("\n{3,}", "\n\n").trim() + "\n";
    }
}
