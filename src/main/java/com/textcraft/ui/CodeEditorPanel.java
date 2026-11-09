package com.textcraft.ui;

import com.textcraft.compiler.Language;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * Code mode editor (RSyntaxTextArea). Syntax colours follow the light/dark theme.
 * OOP: implements TextEditor, so menus and the AI panel treat it like the rich editor.
 */
public class CodeEditorPanel extends JPanel implements TextEditor, ThemeChangeListener {
    private final RSyntaxTextArea area = new RSyntaxTextArea(25, 80);
    private final RTextScrollPane scroll = new RTextScrollPane(area);
    private final List<DocumentChangeListener> listeners = new ArrayList<>();
    private final StatusBar statusBar;
    private boolean dirty;
    private boolean loading;

    public CodeEditorPanel(StatusBar statusBar) {
        super(new BorderLayout());
        this.statusBar = statusBar;
        area.setCodeFoldingEnabled(false); // lighter on old PCs
        area.setAntiAliasingEnabled(true);
        area.setTabSize(4);
        area.setTabsEmulated(true);
        scroll.setLineNumbersEnabled(true);
        add(scroll, BorderLayout.CENTER);
        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });
        setSyntax(Language.PYTHON);
        ThemeManager.getInstance().addListener(this);
        onThemeChanged(ThemeManager.getInstance().getResolved(), ThemeManager.getInstance().palette());
    }

    private void changed() {
        if (loading) return;
        dirty = true;
        updateCounts();
        listeners.forEach(DocumentChangeListener::onDocumentChanged);
    }

    private void updateCounts() {
        int len = area.getDocument().getLength();
        if (len > 200_000) { // big file: skip the live word count (old PCs)
            statusBar.setCounts(-1, len);
            return;
        }
        statusBar.setCounts(com.textcraft.util.TextUtils.wordCount(area.getText()), len);
    }

    // ---- syntax ------------------------------------------------------------------------------

    public void setSyntax(Language l) {
        switch (l) {
            case CPP: area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS); break;
            case JAVA: area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA); break;
            default: area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON); break;
        }
    }

    /** Syntax colours for files that are not runnable (json, xml, css...). */
    public void setSyntaxForExtension(String ext) {
        switch (ext.toLowerCase()) {
            case "js": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT); break;
            case "ts": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT); break;
            case "json": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON); break;
            case "xml": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML); break;
            case "css": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSS); break;
            case "sql": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL); break;
            case "sh": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL); break;
            case "c": case "h": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C); break;
            case "hpp": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS); break;
            case "yaml": case "yml": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML); break;
            case "properties": case "ini": case "cfg": area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE); break;
            default: area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE); break;
        }
    }

    // ---- text in / out -----------------------------------------------------------------------

    public void loadText(String text) {
        loading = true;
        try {
            area.setText(text);
            area.setCaretPosition(0);
            area.discardAllEdits();
        } finally {
            loading = false;
        }
        dirty = false;
        updateCounts();
    }

    /** Printing always uses the light colour scheme on white paper. */
    public void print() throws PrinterException {
        applySyntaxTheme(false);
        try {
            area.print();
        } finally {
            applySyntaxTheme(ThemeManager.getInstance().isDark());
        }
    }

    // ---- TextEditor ----------------------------------------------------------------------------

    @Override public JComponent component() { return this; }

    @Override public String getAllText() { return area.getText(); }

    @Override
    public String getSelectedText() {
        String s = area.getSelectedText();
        return s == null ? "" : s;
    }

    @Override public int getSelectionStart() { return area.getSelectionStart(); }

    @Override public int getSelectionEnd() { return area.getSelectionEnd(); }

    @Override
    public void select(int start, int end) {
        area.select(start, end);
        area.requestFocusInWindow();
    }

    @Override
    public void replaceRange(int start, int end, String text) {
        area.beginAtomicEdit(); // remove + insert = ONE undo step
        try {
            area.replaceRange(text, start, end);
        } finally {
            area.endAtomicEdit();
        }
    }

    @Override public void replaceSelection(String text) { replaceRange(area.getSelectionStart(), area.getSelectionEnd(), text); }

    @Override public void insertAtCaret(String text) { replaceSelection(text); }

    @Override
    public void insertBelowSelection(String text) {
        try {
            int line = area.getLineOfOffset(area.getSelectionEnd());
            int pos = area.getLineEndOffset(line);
            if (pos > 0 && area.getText(pos - 1, 1).equals("\n")) pos--;
            replaceRange(pos, pos, "\n" + text);
        } catch (BadLocationException e) {
            replaceRange(area.getDocument().getLength(), area.getDocument().getLength(), "\n" + text);
        }
    }

    @Override public void undo() { area.undoLastAction(); }

    @Override public void redo() { area.redoLastAction(); }

    @Override public void cut() { area.cut(); }

    @Override public void copy() { area.copy(); }

    @Override public void paste() { area.paste(); }

    @Override public void selectAll() { area.selectAll(); }

    @Override public boolean isDirty() { return dirty; }

    @Override public void setDirty(boolean dirty) { this.dirty = dirty; }

    @Override public void addChangeListener(DocumentChangeListener l) { listeners.add(l); }

    @Override public void applyLiteMode(boolean lite) {
        area.setAntiAliasingEnabled(!lite);
        area.setAnimateBracketMatching(!lite);
    }

    @Override public void focusEditor() { area.requestFocusInWindow(); }

    // ---- theme -----------------------------------------------------------------------------

    @Override
    public void onThemeChanged(Theme resolved, Palette p) {
        applySyntaxTheme(resolved == Theme.DARK);
    }

    /** Loads the RSyntaxTextArea colour scheme (dark.xml / idea.xml) and re-applies our code font. */
    private void applySyntaxTheme(boolean dark) {
        String res = "/org/fife/ui/rsyntaxtextarea/themes/" + (dark ? "dark.xml" : "idea.xml");
        try (InputStream in = RSyntaxTextArea.class.getResourceAsStream(res)) {
            if (in != null) org.fife.ui.rsyntaxtextarea.Theme.load(in).apply(area);
        } catch (IOException e) {
            // keep the current colours; the editor still works
        }
        area.setFont(codeFont());
    }

    private static Font codeFont() {
        List<String> fonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : new String[]{"JetBrains Mono", "Consolas"}) {
            if (fonts.contains(f)) return new Font(f, Font.PLAIN, 13);
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 13);
    }
}
