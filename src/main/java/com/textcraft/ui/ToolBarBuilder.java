package com.textcraft.ui;

import com.textcraft.io.DocAttrs;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.text.StyleConstants;

/**
 * The ONE toolbar row. Buttons are text glyphs, so they take the theme text colour automatically.
 * Bold/Italic/Underline/Strike show a pressed state that follows the selection.
 * In Code mode formatting controls are greyed (not hidden) and a green Run button appears.
 */
public class ToolBarBuilder implements ThemeChangeListener {
    private static final String[] FONTS = {"Arial", "Times New Roman", "Courier New", "Verdana", "Georgia", "Calibri"};
    private static final String[] SIZES = {"8", "9", "10", "11", "12", "14", "16", "18", "24", "36", "48"};
    private static final String[] STYLES = {DocAttrs.NORMAL, DocAttrs.TITLE, DocAttrs.H1, DocAttrs.H2, DocAttrs.H3};

    private final MainFrame f;
    private final List<JComponent> formattingControls = new ArrayList<>();
    private JToggleButton bold, italic, underline, strike, mic;
    private JComboBox<String> styleBox, fontBox, sizeBox, zoomBox;
    private JButton runButton;
    private boolean syncing; // true while we set combo values ourselves (so no action fires)

    public ToolBarBuilder(MainFrame frame) {
        this.f = frame;
    }

    public JToolBar build() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        button(bar, "↶", "Undo (Ctrl+Z)", false, f::undo);
        button(bar, "↷", "Redo (Ctrl+Y)", false, f::redo);
        button(bar, "🖨", "Print (Ctrl+P)", false, f::print);
        bar.addSeparator();

        zoomBox = combo(bar, "Zoom", new String[]{"75%", "100%", "125%", "150%", "200%"}, 1, 72, false);
        zoomBox.addActionListener(e -> {
            if (!syncing) f.setZoom(Integer.parseInt(((String) zoomBox.getSelectedItem()).replace("%", "")) / 100.0);
        });
        styleBox = combo(bar, "Paragraph style", STYLES, 0, 100, true);
        styleBox.addActionListener(e -> {
            if (!syncing) f.format(r -> r.setParagraphStyle((String) styleBox.getSelectedItem()));
        });
        fontBox = combo(bar, "Font", FONTS, 0, 130, true);
        fontBox.addActionListener(e -> {
            if (!syncing) f.format(r -> r.setFontFamily((String) fontBox.getSelectedItem()));
        });
        button(bar, "−", "Smaller text", true, () -> f.format(r -> r.adjustFontSize(-1)));
        sizeBox = combo(bar, "Font size", SIZES, 4, 50, true);
        sizeBox.setEditable(true);
        sizeBox.addActionListener(e -> {
            if (syncing) return;
            try {
                int size = Integer.parseInt(String.valueOf(sizeBox.getSelectedItem()).trim());
                f.format(r -> r.setFontSize(size));
            } catch (NumberFormatException ex) {
                ErrorHandler.toast("Please type a number for the font size.");
            }
        });
        button(bar, "+", "Larger text", true, () -> f.format(r -> r.adjustFontSize(1)));
        bar.addSeparator();

        bold = toggle(bar, "B", "Bold (Ctrl+B)", Font.BOLD, () -> f.format(RichEditorPanel::toggleBold));
        italic = toggle(bar, "I", "Italic (Ctrl+I)", Font.ITALIC, () -> f.format(RichEditorPanel::toggleItalic));
        underline = toggle(bar, "U", "Underline (Ctrl+U)", Font.PLAIN, () -> f.format(RichEditorPanel::toggleUnderline));
        strike = toggle(bar, "S", "Strikethrough", Font.PLAIN, () -> f.format(RichEditorPanel::toggleStrike));

        JButton color = button(bar, "A", "Text color", true, null);
        color.addActionListener(e -> colorMenu(color, true));
        JButton hi = button(bar, "✎", "Highlight color", true, null);
        hi.addActionListener(e -> colorMenu(hi, false));
        bar.addSeparator();

        JButton align = button(bar, "☰", "Align", true, null);
        align.addActionListener(e -> alignMenu(align));
        button(bar, "•", "Bulleted list", true, () -> f.format(RichEditorPanel::toggleBullets));
        button(bar, "1.", "Numbered list", true, () -> f.format(RichEditorPanel::toggleNumbering));
        bar.addSeparator();

        button(bar, "AI", "AI side panel (Ctrl+Shift+A)", false, f::toggleSidePanel);
        mic = toggle(bar, "🎤", "Voice typing: click to start, click again to stop (Ctrl+Alt+M)", Font.PLAIN, f::toggleVoice);
        button(bar, "🔊", "Read aloud (Ctrl+Alt+R)", false, f::readAloud);

        runButton = new JButton("▶ Run");
        runButton.setToolTipText("Run code (F5)");
        runButton.setFocusable(false);
        runButton.setForeground(Color.WHITE);
        runButton.addActionListener(e -> f.runCode());
        runButton.setVisible(false);
        bar.addSeparator();
        bar.add(runButton);
        onThemeChanged(ThemeManager.getInstance().getResolved(), ThemeManager.getInstance().palette());
        ThemeManager.getInstance().addListener(this);
        return bar;
    }

    // ---- state ---------------------------------------------------------------------------------

    /** Pressed state and combo values follow the caret / selection. */
    public void syncFormat(RichEditorPanel r) {
        syncing = true;
        try {
            bold.setSelected(r.isBold());
            italic.setSelected(r.isItalic());
            underline.setSelected(r.isUnderline());
            strike.setSelected(r.isStrike());
            styleBox.setSelectedItem(r.currentParagraphStyle());
            fontBox.setSelectedItem(r.currentFontFamily());
            sizeBox.setSelectedItem(String.valueOf(r.currentFontSize()));
        } finally {
            syncing = false;
        }
    }

    public void setZoomShown(double zoom) {
        syncing = true;
        try {
            zoomBox.setSelectedItem(Math.round(zoom * 100) + "%");
        } finally {
            syncing = false;
        }
    }

    public void setMicActive(boolean active) {
        mic.setSelected(active);
    }

    public void setCodeMode(boolean code) {
        formattingControls.forEach(c -> c.setEnabled(!code));
        runButton.setVisible(code);
    }

    @Override
    public void onThemeChanged(Theme t, Palette p) {
        if (runButton != null) runButton.setBackground(p.ok);
    }

    // ---- builders ------------------------------------------------------------------------------

    private JButton button(JToolBar bar, String glyph, String tip, boolean formatting, Runnable action) {
        JButton b = new JButton(glyph);
        b.setToolTipText(tip);
        b.setFocusable(false);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setPreferredSize(new Dimension(32, 30));
        if (action != null) b.addActionListener(e -> action.run());
        if (formatting) formattingControls.add(b);
        bar.add(b);
        return b;
    }

    private JToggleButton toggle(JToolBar bar, String glyph, String tip, int style, Runnable action) {
        JToggleButton b = new JToggleButton(glyph);
        b.setToolTipText(tip);
        b.setFocusable(false);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setPreferredSize(new Dimension(32, 30));
        b.setFont(b.getFont().deriveFont(style));
        b.addActionListener(e -> action.run());
        if (!glyph.equals("🎤")) formattingControls.add(b);
        bar.add(b);
        return b;
    }

    private JComboBox<String> combo(JToolBar bar, String tip, String[] items, int selected, int width, boolean formatting) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setSelectedIndex(selected);
        c.setToolTipText(tip);
        c.setFocusable(false);
        c.setPreferredSize(new Dimension(width, 28));
        c.setMaximumSize(new Dimension(width, 28));
        if (formatting) formattingControls.add(c);
        bar.add(c);
        return c;
    }

    private void colorMenu(JComponent anchor, boolean text) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem auto = new JMenuItem(text ? "Automatic (follows theme)" : "No highlight");
        auto.addActionListener(e -> f.format(r -> { if (text) r.setTextColor(null); else r.setHighlight(null); }));
        menu.add(auto);
        JMenuItem pick = new JMenuItem("Choose color...");
        pick.addActionListener(e -> {
            Color c = JColorChooser.showDialog(anchor, text ? "Text color" : "Highlight color", text ? Color.RED : Color.YELLOW);
            if (c != null) f.format(r -> { if (text) r.setTextColor(c); else r.setHighlight(c); });
        });
        menu.add(pick);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void alignMenu(JComponent anchor) {
        JPopupMenu menu = new JPopupMenu();
        String[] names = {"Left", "Center", "Right", "Justify"};
        int[] values = {StyleConstants.ALIGN_LEFT, StyleConstants.ALIGN_CENTER, StyleConstants.ALIGN_RIGHT, StyleConstants.ALIGN_JUSTIFIED};
        for (int i = 0; i < names.length; i++) {
            final int v = values[i];
            JMenuItem mi = new JMenuItem(names[i]);
            mi.addActionListener(e -> f.format(r -> r.setAlignment(v)));
            menu.add(mi);
        }
        menu.show(anchor, 0, anchor.getHeight());
    }
}
