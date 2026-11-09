package com.textcraft.ui;

import com.textcraft.exception.DocumentException;
import com.textcraft.io.DocAttrs;
import com.textcraft.io.DocCopy;
import com.textcraft.util.Debouncer;
import com.textcraft.util.TextUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

/**
 * The A4-style page with the rich text editor (JTextPane + StyledDocument).
 * Features: character/paragraph formatting, automatic text colour, zoom, undo/redo, word count.
 * OOP: implements TextEditor (polymorphism with CodeEditorPanel).
 */
public class RichEditorPanel extends JPanel implements TextEditor, ThemeChangeListener {
    public static final int PAGE_W = 794;
    public static final int PAGE_H = 1123;
    private static final int PAGE_PADDING = 72;
    private static final int HUGE_FILE_CHARS = 200_000;

    /**
     * StyleContext that (1) multiplies font sizes by the zoom factor and
     * (2) returns the theme text colour when no colour was chosen ("automatic" colour).
     */
    private static final class ThemedStyleContext extends StyleContext {
        private static final long serialVersionUID = 1L;
        float zoom = 1f;
        Color autoColor = Color.BLACK;

        @Override
        public Font getFont(AttributeSet a) {
            Font f = super.getFont(a);
            return zoom == 1f ? f : f.deriveFont(f.getSize2D() * zoom);
        }

        @Override
        public Color getForeground(AttributeSet a) {
            Object fg = a.getAttribute(StyleConstants.Foreground);
            return fg instanceof Color ? (Color) fg : autoColor;
        }
    }

    private final ThemedStyleContext ctx = new ThemedStyleContext();
    private final JTextPane pane = new JTextPane();
    private final UndoManager undo = new UndoManager();
    private final List<DocumentChangeListener> listeners = new ArrayList<>();
    private final Debouncer countDebouncer = new Debouncer(300);
    private final StatusBar statusBar;
    private CompoundEdit compound;
    private Runnable formatListener = () -> { };
    private double zoom = 1.0;
    private boolean dirty;
    private boolean loading;
    private boolean lite;
    private int defaultSize = DocAttrs.DEFAULT_SIZE;

    public RichEditorPanel(StatusBar statusBar) {
        super(new BorderLayout());
        this.statusBar = statusBar;
        add(pane, BorderLayout.CENTER);
        pane.setFont(new Font(DocAttrs.DEFAULT_FONT, Font.PLAIN, DocAttrs.DEFAULT_SIZE));
        installDocument(newDocument());
        applyMargin();
        // Swing calls the newest listener first, so wait until the editor kit has updated its input attributes.
        pane.addCaretListener(e -> SwingUtilities.invokeLater(() -> formatListener.run()));
        ThemeManager.getInstance().addListener(this);
        onThemeChanged(ThemeManager.getInstance().getResolved(), ThemeManager.getInstance().palette());
    }

    /** Default font for text without its own formatting (Settings). */
    public void setDefaults(String family, int size) {
        Style def = ctx.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(def, family);
        StyleConstants.setFontSize(def, size);
        defaultSize = size;
        pane.repaint();
    }

    private DefaultStyledDocument newDocument() {
        DefaultStyledDocument d = new DefaultStyledDocument(ctx);
        Style def = ctx.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(def, DocAttrs.DEFAULT_FONT);
        StyleConstants.setFontSize(def, DocAttrs.DEFAULT_SIZE);
        return d;
    }

    private void installDocument(DefaultStyledDocument d) {
        pane.setDocument(d);
        d.addUndoableEditListener(e -> {
            if (compound != null) compound.addEdit(e.getEdit());
            else undo.addEdit(e.getEdit());
        });
        d.addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });
    }

    private void changed() {
        if (loading) return;
        dirty = true;
        revalidate();
        countDebouncer.call(() -> SwingUtilities.invokeLater(this::updateCounts));
        listeners.forEach(DocumentChangeListener::onDocumentChanged);
    }

    private void updateCounts() {
        int len = doc().getLength();
        if (lite && len > HUGE_FILE_CHARS) { // Lite mode: no live word count on huge files
            statusBar.setCounts(-1, len);
            return;
        }
        String text = getAllText();
        statusBar.setCounts(TextUtils.wordCount(text), text.length());
    }

    private StyledDocument doc() {
        return (StyledDocument) pane.getDocument();
    }

    // ---- page size / zoom ---------------------------------------------------------------------

    @Override
    public Dimension getPreferredSize() {
        Dimension content = pane.getPreferredSize();
        return new Dimension((int) (PAGE_W * zoom), Math.max((int) (PAGE_H * zoom), content.height));
    }

    private void applyMargin() {
        int pad = (int) (PAGE_PADDING * zoom);
        pane.setMargin(new Insets(pad, pad, pad, pad));
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double z) {
        zoom = Math.max(0.5, Math.min(2.0, z));
        ctx.zoom = (float) zoom;
        applyMargin();
        // Re-create the views so they pick up the new font sizes (same document, no data change).
        int ss = pane.getSelectionStart();
        int se = pane.getSelectionEnd();
        StyledDocument d = doc();
        pane.setDocument(new DefaultStyledDocument());
        pane.setDocument(d);
        pane.select(Math.min(ss, d.getLength()), Math.min(se, d.getLength()));
        revalidate();
        repaint();
    }

    // ---- character formatting -------------------------------------------------------------------

    private AttributeSet currentAttributes() {
        int s = pane.getSelectionStart();
        int e = pane.getSelectionEnd();
        if (s != e) return doc().getCharacterElement(s).getAttributes();
        return pane.getInputAttributes();
    }

    public boolean isBold() { return StyleConstants.isBold(currentAttributes()); }

    public boolean isItalic() { return StyleConstants.isItalic(currentAttributes()); }

    public boolean isUnderline() { return StyleConstants.isUnderline(currentAttributes()); }

    public boolean isStrike() { return StyleConstants.isStrikeThrough(currentAttributes()); }

    public String currentFontFamily() { return StyleConstants.getFontFamily(currentAttributes()); }

    public int currentFontSize() { return StyleConstants.getFontSize(currentAttributes()); }

    public String currentParagraphStyle() {
        return DocAttrs.styleOf(doc().getParagraphElement(pane.getSelectionStart()).getAttributes());
    }

    public void setFormatListener(Runnable r) {
        this.formatListener = r;
    }

    private void applyChar(SimpleAttributeSet a) {
        int s = pane.getSelectionStart();
        int e = pane.getSelectionEnd();
        if (s != e) doc().setCharacterAttributes(s, e - s, a, false);
        pane.getInputAttributes().addAttributes(a); // typing continues with this format
        formatListener.run();
        pane.requestFocusInWindow();
    }

    public void toggleBold() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setBold(a, !isBold());
        applyChar(a);
    }

    public void toggleItalic() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setItalic(a, !isItalic());
        applyChar(a);
    }

    public void toggleUnderline() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setUnderline(a, !isUnderline());
        applyChar(a);
    }

    public void toggleStrike() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setStrikeThrough(a, !isStrike());
        applyChar(a);
    }

    public void setFontFamily(String family) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, family);
        applyChar(a);
    }

    public void setFontSize(int size) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontSize(a, Math.max(6, Math.min(96, size)));
        applyChar(a);
    }

    public void adjustFontSize(int delta) {
        setFontSize(currentFontSize() + delta);
    }

    /** Pass null for "automatic" (follows the theme, nothing stored). */
    public void setTextColor(Color c) {
        if (c == null) {
            removeCharAttr(StyleConstants.Foreground);
        } else {
            SimpleAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setForeground(a, c);
            applyChar(a);
        }
    }

    /** Pass null to remove the highlight. */
    public void setHighlight(Color c) {
        if (c == null) {
            removeCharAttr(StyleConstants.Background);
        } else {
            SimpleAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setBackground(a, c);
            applyChar(a);
        }
    }

    private void removeCharAttr(Object key) {
        int s = pane.getSelectionStart();
        int e = pane.getSelectionEnd();
        beginCompound();
        try {
            int pos = s;
            while (pos < e) {
                Element el = doc().getCharacterElement(pos);
                int end = Math.min(el.getEndOffset(), e);
                SimpleAttributeSet m = new SimpleAttributeSet(el.getAttributes().copyAttributes());
                m.removeAttribute(key);
                doc().setCharacterAttributes(pos, end - pos, m, true);
                pos = end;
            }
        } finally {
            endCompound();
        }
        pane.getInputAttributes().removeAttribute(key);
        formatListener.run();
    }

    // ---- paragraph formatting ---------------------------------------------------------------

    public void setParagraphStyle(String style) {
        int s = pane.getSelectionStart();
        int e = pane.getSelectionEnd();
        beginCompound();
        try {
            SimpleAttributeSet p = new SimpleAttributeSet();
            p.addAttribute(DocAttrs.STYLE_KEY, style);
            doc().setParagraphAttributes(s, e - s, p, false);

            SimpleAttributeSet c = new SimpleAttributeSet();
            StyleConstants.setFontSize(c, DocAttrs.NORMAL.equals(style) ? defaultSize : DocAttrs.sizeFor(style));
            StyleConstants.setBold(c, DocAttrs.boldFor(style));
            int from = doc().getParagraphElement(s).getStartOffset();
            int to = Math.min(doc().getParagraphElement(e).getEndOffset(), doc().getLength());
            if (to > from) doc().setCharacterAttributes(from, to - from, c, false);
            pane.getInputAttributes().addAttributes(c);
        } finally {
            endCompound();
        }
        formatListener.run();
        pane.requestFocusInWindow();
    }

    public void setAlignment(int alignment) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setAlignment(a, alignment);
        doc().setParagraphAttributes(pane.getSelectionStart(), pane.getSelectionEnd() - pane.getSelectionStart(), a, false);
        pane.requestFocusInWindow();
    }

    public void setLineSpacing(float multiple) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(a, Math.max(0f, multiple - 1f)); // Swing stores the EXTRA spacing
        doc().setParagraphAttributes(pane.getSelectionStart(), pane.getSelectionEnd() - pane.getSelectionStart(), a, false);
        pane.requestFocusInWindow();
    }

    public void toggleBullets() {
        toggleList(false);
    }

    public void toggleNumbering() {
        toggleList(true);
    }

    /** Lists are stored as text prefixes ("• " or "1. ") so every export format keeps them. */
    private void toggleList(boolean numbered) {
        StyledDocument d = doc();
        int len = d.getLength();
        int endPos = d.getParagraphElement(pane.getSelectionEnd()).getEndOffset();
        List<int[]> paras = new ArrayList<>();
        int pos = d.getParagraphElement(pane.getSelectionStart()).getStartOffset();
        while (pos < endPos && pos <= len) {
            Element p = d.getParagraphElement(pos);
            paras.add(new int[]{p.getStartOffset(), Math.min(p.getEndOffset(), len)});
            pos = p.getEndOffset();
        }
        beginCompound();
        try {
            boolean allHave = true;
            for (int[] p : paras) {
                String t = d.getText(p[0], p[1] - p[0]);
                boolean has = numbered ? t.matches("(?s)^\\d+\\. .*") : t.startsWith("• ");
                if (!has) allHave = false;
            }
            for (int i = paras.size() - 1; i >= 0; i--) { // backwards so offsets stay valid
                int ps = paras.get(i)[0];
                String t = d.getText(ps, paras.get(i)[1] - ps);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(• |\\d+\\. )").matcher(t);
                if (m.find()) d.remove(ps, m.end());
                if (!allHave) {
                    String prefix = numbered ? (i + 1) + ". " : "• ";
                    d.insertString(ps, prefix, d.getCharacterElement(ps).getAttributes());
                }
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        } finally {
            endCompound();
        }
        pane.requestFocusInWindow();
    }

    public void clearFormatting() {
        int s = pane.getSelectionStart();
        int e = pane.getSelectionEnd();
        if (s == e) { // no selection: whole current paragraph
            Element p = doc().getParagraphElement(s);
            s = p.getStartOffset();
            e = Math.min(p.getEndOffset(), doc().getLength());
        }
        beginCompound();
        try {
            doc().setCharacterAttributes(s, e - s, SimpleAttributeSet.EMPTY, true);
            doc().setParagraphAttributes(s, e - s, SimpleAttributeSet.EMPTY, true);
        } finally {
            endCompound();
        }
        resetInputAttributes();
        formatListener.run();
    }

    private void resetInputAttributes() {
        javax.swing.text.MutableAttributeSet in = pane.getInputAttributes();
        in.removeAttributes(in.copyAttributes()); // copy first: do not modify the set while iterating it
    }

    // ---- insert menu ---------------------------------------------------------------------------

    public void insertHorizontalLine() {
        insertAtCaret("\n────────────────────\n");
    }

    public void insertDateTime() {
        insertAtCaret(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
    }

    // ---- undo support --------------------------------------------------------------------------

    private void beginCompound() {
        if (compound == null) compound = new CompoundEdit();
    }

    private void endCompound() {
        if (compound != null) {
            compound.end();
            if (compound.isSignificant()) undo.addEdit(compound);
            compound = null;
        }
    }

    @Override
    public void undo() {
        if (undo.canUndo()) undo.undo();
    }

    @Override
    public void redo() {
        if (undo.canRedo()) undo.redo();
    }

    @Override
    public void applyLiteMode(boolean lite) {
        this.lite = lite;
        undo.setLimit(lite ? 100 : 1000); // Lite mode: undo history capped at 100
        updateCounts();
    }

    // ---- TextEditor ------------------------------------------------------------------------------

    @Override
    public JComponent component() {
        return this;
    }

    @Override
    public String getAllText() {
        try {
            return doc().getText(0, doc().getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    @Override
    public String getSelectedText() {
        String s = pane.getSelectedText();
        return s == null ? "" : s;
    }

    @Override
    public int getSelectionStart() {
        return pane.getSelectionStart();
    }

    @Override
    public int getSelectionEnd() {
        return pane.getSelectionEnd();
    }

    @Override
    public void select(int start, int end) {
        pane.select(start, end);
        pane.requestFocusInWindow();
    }

    @Override
    public void replaceRange(int start, int end, String text) {
        beginCompound(); // remove + insert become ONE undo step
        try {
            // Pure insertion uses the formatting the user switched on at the caret (bold, heading...);
            // replacing a range keeps the formatting of the text being replaced.
            AttributeSet keep = start == end && start == pane.getCaretPosition()
                    ? new SimpleAttributeSet(pane.getInputAttributes().copyAttributes())
                    : new SimpleAttributeSet(
                            doc().getCharacterElement(Math.min(start, Math.max(0, doc().getLength() - 1))).getAttributes().copyAttributes());
            ((DefaultStyledDocument) doc()).replace(start, end - start, text, keep);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        } finally {
            endCompound();
        }
    }

    @Override
    public void replaceSelection(String text) {
        replaceRange(pane.getSelectionStart(), pane.getSelectionEnd(), text);
    }

    @Override
    public void insertAtCaret(String text) {
        replaceRange(pane.getSelectionStart(), pane.getSelectionEnd(), text);
    }

    @Override
    public void insertBelowSelection(String text) {
        Element p = doc().getParagraphElement(pane.getSelectionEnd());
        int pos = Math.min(p.getEndOffset() - 1, doc().getLength());
        replaceRange(pos, pos, "\n" + text);
    }

    @Override public void cut() { pane.cut(); }

    @Override public void copy() { pane.copy(); }

    @Override public void paste() { pane.paste(); }

    @Override public void selectAll() { pane.selectAll(); }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public void addChangeListener(DocumentChangeListener l) {
        listeners.add(l);
    }

    @Override
    public void focusEditor() {
        pane.requestFocusInWindow();
    }

    // ---- document in / out ---------------------------------------------------------------------

    /** The live document (handlers write from it). */
    public StyledDocument getStyledDocument() {
        return doc();
    }

    /** Replace the content with a document produced by a file handler. */
    public void loadFrom(StyledDocument src) throws DocumentException {
        loading = true;
        try {
            DocCopy.copy(src, doc());
        } finally {
            loading = false;
        }
        undo.discardAllEdits();
        dirty = false;
        pane.setCaretPosition(0);
        resetInputAttributes();
        updateCounts();
        revalidate();
        repaint();
    }

    /** Replace the content with plain text. */
    public void loadText(String text) {
        loading = true;
        try {
            doc().remove(0, doc().getLength());
            doc().insertString(0, text, null);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        } finally {
            loading = false;
        }
        undo.discardAllEdits();
        dirty = false;
        pane.setCaretPosition(0);
        updateCounts();
        revalidate();
    }

    public void clear() {
        loadText("");
    }

    /** {words, chars, chars without spaces, paragraphs} */
    public int[] wordStats() {
        String t = getAllText();
        return new int[]{TextUtils.wordCount(t), t.length(), TextUtils.charsWithoutSpaces(t), TextUtils.paragraphCount(t)};
    }

    /** Prints on a white page with black automatic text, whatever the theme is. */
    public void print() throws PrinterException {
        Color oldAuto = ctx.autoColor;
        Color oldBg = pane.getBackground();
        ctx.autoColor = Color.BLACK;
        pane.setBackground(Color.WHITE);
        try {
            pane.print();
        } finally {
            ctx.autoColor = oldAuto;
            pane.setBackground(oldBg);
            pane.repaint();
        }
    }

    // ---- theme -----------------------------------------------------------------------------

    @Override
    public void onThemeChanged(Theme resolved, Palette p) {
        ctx.autoColor = p.text;
        setBackground(p.page);
        pane.setBackground(p.page);
        pane.setForeground(p.text);
        pane.setCaretColor(p.text);
        pane.setSelectionColor(p.selection);
        pane.setSelectedTextColor(p.text);
        setBorder(javax.swing.BorderFactory.createLineBorder(p.border));
        pane.repaint();
    }
}
