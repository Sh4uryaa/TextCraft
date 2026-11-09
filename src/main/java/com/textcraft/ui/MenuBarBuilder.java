package com.textcraft.ui;

import com.textcraft.ai.AIAction;
import com.textcraft.ai.Tone;
import com.textcraft.compiler.Language;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.text.StyleConstants;

/** Builds every menu from the spec and wires each item to a MainFrame action. */
public class MenuBarBuilder implements ThemeChangeListener {
    private static final int CTRL = InputEvent.CTRL_DOWN_MASK;
    private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
    private static final int ALT = InputEvent.ALT_DOWN_MASK;

    private final MainFrame f;
    private final ThemeManager themes = ThemeManager.getInstance();

    private JCheckBoxMenuItem codeModeItem;
    private JCheckBoxMenuItem liteItem;
    private JMenu formatMenu;
    private JMenu recentMenu;
    private JMenuItem signInItem;
    private JMenuItem signOutItem;
    private final List<JRadioButtonMenuItem> themeItems = new ArrayList<>();
    private final List<JRadioButtonMenuItem> languageItems = new ArrayList<>();
    private final List<JRadioButtonMenuItem> voiceLangItems = new ArrayList<>();

    public MenuBarBuilder(MainFrame frame) {
        this.f = frame;
    }

    public JMenuBar build() {
        JMenuBar bar = new JMenuBar();
        bar.add(fileMenu());
        bar.add(editMenu());
        bar.add(viewMenu());
        bar.add(insertMenu());
        bar.add(formatMenu());
        bar.add(toolsMenu());
        bar.add(aiMenu());
        bar.add(studyMenu());
        bar.add(codeMenu());
        bar.add(helpMenu());
        themes.addListener(this);
        return bar;
    }

    // ---- state updates called by MainFrame ---------------------------------------------------

    public void setCodeModeSelected(boolean on) {
        codeModeItem.setSelected(on);
        formatMenu.setEnabled(!on);
    }

    public void setLiteSelected(boolean on) {
        liteItem.setSelected(on);
    }

    public void setLanguageSelected(Language l) {
        for (JRadioButtonMenuItem r : languageItems) r.setSelected(r.getText().equals(l.label()));
    }

    public void setVoiceLanguageSelected(String code) {
        for (JRadioButtonMenuItem r : voiceLangItems) r.setSelected(r.getText().equals(code));
    }

    public void setSignedIn(boolean signedIn) {
        signInItem.setEnabled(!signedIn);
        signOutItem.setEnabled(signedIn);
    }

    public void setRecent(List<File> files) {
        recentMenu.removeAll();
        recentMenu.setEnabled(!files.isEmpty());
        for (File file : files) {
            JMenuItem mi = new JMenuItem(file.getName());
            mi.setToolTipText(file.getAbsolutePath());
            mi.addActionListener(e -> f.openPath(file));
            recentMenu.add(mi);
        }
    }

    @Override
    public void onThemeChanged(Theme resolved, Palette p) { // keep the View > Theme radios in step with Ctrl+Alt+D
        for (JRadioButtonMenuItem r : themeItems) {
            r.setSelected(r.getText().equalsIgnoreCase(themes.getChoice().name()));
        }
    }

    // ---- menus -----------------------------------------------------------------------------------

    private JMenu fileMenu() {
        JMenu m = new JMenu("File");
        item(m, "New", KeyEvent.VK_N, CTRL, f::newDocument);
        item(m, "Open...", KeyEvent.VK_O, CTRL, f::openFile);
        recentMenu = new JMenu("Open recent");
        m.add(recentMenu);
        item(m, "Open from cloud...", 0, 0, f::openFromCloud);
        item(m, "Save to cloud", 0, 0, f::saveToCloud);
        item(m, "Save", KeyEvent.VK_S, CTRL, f::save);
        item(m, "Save As...", KeyEvent.VK_S, CTRL | SHIFT, f::saveAs);
        item(m, "Make a copy...", 0, 0, f::makeCopy);
        item(m, "Version history", 0, 0, f::versionHistory);
        JMenu export = new JMenu("Export");
        for (String ext : new String[]{"pdf", "docx", "html", "md", "txt", "rtf"}) {
            item(export, ext.toUpperCase(), 0, 0, () -> f.export(ext));
        }
        m.add(export);
        m.addSeparator();
        item(m, "Rename", 0, 0, f::renameDocument);
        item(m, "Delete cloud copy", 0, 0, f::deleteCloudCopy);
        item(m, "Print...", KeyEvent.VK_P, CTRL, f::print);
        m.addSeparator();
        signInItem = item(m, "Sign in with Google...", 0, 0, f::signIn);
        signOutItem = item(m, "Sign out", 0, 0, f::signOut);
        signOutItem.setEnabled(false);
        m.addSeparator();
        item(m, "Exit", 0, 0, f::exit);
        return m;
    }

    private JMenu editMenu() {
        JMenu m = new JMenu("Edit");
        item(m, "Undo", KeyEvent.VK_Z, CTRL, f::undo);
        item(m, "Redo", KeyEvent.VK_Y, CTRL, f::redo);
        m.addSeparator();
        item(m, "Cut", KeyEvent.VK_X, CTRL, f::cut);
        item(m, "Copy", KeyEvent.VK_C, CTRL, f::copy);
        item(m, "Paste", KeyEvent.VK_V, CTRL, f::paste);
        item(m, "Select all", KeyEvent.VK_A, CTRL, f::selectAll);
        m.addSeparator();
        item(m, "Find & replace...", KeyEvent.VK_F, CTRL, f::findReplace);
        return m;
    }

    private JMenu viewMenu() {
        JMenu m = new JMenu("View");
        JMenu zoom = new JMenu("Zoom");
        for (int z : new int[]{75, 100, 125, 150, 200}) item(zoom, z + "%", 0, 0, () -> f.setZoom(z / 100.0));
        m.add(zoom);

        JMenu theme = new JMenu("Theme");
        ButtonGroup g = new ButtonGroup();
        for (Theme t : Theme.values()) {
            String name = t.name().charAt(0) + t.name().substring(1).toLowerCase();
            JRadioButtonMenuItem r = new JRadioButtonMenuItem(name, t == themes.getChoice());
            r.addActionListener(e -> f.setTheme(t));
            g.add(r);
            theme.add(r);
            themeItems.add(r);
        }
        m.add(theme);
        item(m, "Switch light/dark", KeyEvent.VK_D, CTRL | ALT, f::toggleTheme);

        liteItem = new JCheckBoxMenuItem("Lite mode");
        liteItem.addActionListener(e -> f.setLiteMode(liteItem.isSelected()));
        m.add(liteItem);
        item(m, "Word count...", 0, 0, f::showWordCount);
        item(m, "Side panel", KeyEvent.VK_A, CTRL | SHIFT, f::toggleSidePanel);
        return m;
    }

    private JMenu insertMenu() {
        JMenu m = new JMenu("Insert");
        item(m, "Horizontal line", 0, 0, () -> f.format(r -> r.insertHorizontalLine()));
        item(m, "Date/time", 0, 0, f::insertDateTime);
        item(m, "Special characters...", 0, 0, f::showSpecialChars);
        return m;
    }

    private JMenu formatMenu() {
        JMenu m = new JMenu("Format");
        formatMenu = m;
        item(m, "Bold", KeyEvent.VK_B, CTRL, () -> f.format(RichEditorPanel::toggleBold));
        item(m, "Italic", KeyEvent.VK_I, CTRL, () -> f.format(RichEditorPanel::toggleItalic));
        item(m, "Underline", KeyEvent.VK_U, CTRL, () -> f.format(RichEditorPanel::toggleUnderline));
        item(m, "Strikethrough", 0, 0, () -> f.format(RichEditorPanel::toggleStrike));
        JMenu styles = new JMenu("Paragraph styles");
        item(styles, "Normal", 0, 0, () -> f.format(r -> r.setParagraphStyle(com.textcraft.io.DocAttrs.NORMAL)));
        item(styles, "Title", 0, 0, () -> f.format(r -> r.setParagraphStyle(com.textcraft.io.DocAttrs.TITLE)));
        item(styles, "Heading 1", KeyEvent.VK_1, CTRL | ALT, () -> f.format(r -> r.setParagraphStyle(com.textcraft.io.DocAttrs.H1)));
        item(styles, "Heading 2", KeyEvent.VK_2, CTRL | ALT, () -> f.format(r -> r.setParagraphStyle(com.textcraft.io.DocAttrs.H2)));
        item(styles, "Heading 3", KeyEvent.VK_3, CTRL | ALT, () -> f.format(r -> r.setParagraphStyle(com.textcraft.io.DocAttrs.H3)));
        m.add(styles);
        JMenu align = new JMenu("Align");
        item(align, "Left", 0, 0, () -> f.format(r -> r.setAlignment(StyleConstants.ALIGN_LEFT)));
        item(align, "Center", 0, 0, () -> f.format(r -> r.setAlignment(StyleConstants.ALIGN_CENTER)));
        item(align, "Right", 0, 0, () -> f.format(r -> r.setAlignment(StyleConstants.ALIGN_RIGHT)));
        item(align, "Justify", 0, 0, () -> f.format(r -> r.setAlignment(StyleConstants.ALIGN_JUSTIFIED)));
        m.add(align);
        JMenu spacing = new JMenu("Line spacing");
        for (float s : new float[]{1.0f, 1.15f, 1.5f, 2.0f}) item(spacing, String.valueOf(s), 0, 0, () -> f.format(r -> r.setLineSpacing(s)));
        m.add(spacing);
        JMenu lists = new JMenu("Lists");
        item(lists, "Bulleted list", 0, 0, () -> f.format(RichEditorPanel::toggleBullets));
        item(lists, "Numbered list", 0, 0, () -> f.format(RichEditorPanel::toggleNumbering));
        m.add(lists);
        item(m, "Clear formatting", 0, 0, () -> f.format(RichEditorPanel::clearFormatting));
        return m;
    }

    private JMenu toolsMenu() {
        JMenu m = new JMenu("Tools");
        item(m, "Voice typing (start/stop)", KeyEvent.VK_M, CTRL | ALT, f::toggleVoice);
        JMenu lang = new JMenu("Voice language");
        ButtonGroup g = new ButtonGroup();
        for (String code : new String[]{"en-US", "en-IN", "hi-IN"}) {
            JRadioButtonMenuItem r = new JRadioButtonMenuItem(code, code.equals("en-US"));
            r.addActionListener(e -> f.setVoiceLanguage(code));
            g.add(r);
            lang.add(r);
            voiceLangItems.add(r);
        }
        m.add(lang);
        item(m, "Read aloud", KeyEvent.VK_R, CTRL | ALT, f::readAloud);
        item(m, "Pause / resume reading", 0, 0, f::pauseReading);
        item(m, "Stop", 0, 0, f::stopReading);
        m.addSeparator();
        item(m, "Settings...", 0, 0, f::showSettings);
        return m;
    }

    private JMenu aiMenu() {
        JMenu m = new JMenu("AI");
        item(m, "Fix grammar", 0, 0, () -> f.ai(AIAction.FIX_GRAMMAR, null));
        item(m, "Improve", 0, 0, () -> f.ai(AIAction.IMPROVE, null));
        JMenu tone = new JMenu("Tone");
        for (Tone t : Tone.values()) {
            String name = t.name().charAt(0) + t.name().substring(1).toLowerCase();
            item(tone, name, 0, 0, () -> f.ai(AIAction.TONE, t));
        }
        m.add(tone);
        item(m, "Shorter", 0, 0, () -> f.ai(AIAction.SHORTER, null));
        item(m, "Longer", 0, 0, () -> f.ai(AIAction.LONGER, null));
        item(m, "Summarize", 0, 0, () -> f.ai(AIAction.SUMMARIZE, null));
        item(m, "Explain", 0, 0, () -> f.ai(AIAction.EXPLAIN, null));
        item(m, "Translate...", 0, 0, () -> f.ai(AIAction.TRANSLATE, null));
        item(m, "Ask AI...", 0, 0, () -> f.ai(AIAction.ASK, null));
        return m;
    }

    private JMenu studyMenu() {
        JMenu m = new JMenu("Study");
        item(m, "Flashcards", KeyEvent.VK_F, CTRL | ALT, () -> f.study("flashcards"));
        item(m, "Quiz", 0, 0, () -> f.study("quiz"));
        item(m, "Key points", 0, 0, () -> f.study("keypoints"));
        item(m, "My study sets...", 0, 0, f::showStudySets);
        return m;
    }

    private JMenu codeMenu() {
        JMenu m = new JMenu("Code");
        codeModeItem = new JCheckBoxMenuItem("Code mode");
        codeModeItem.addActionListener(e -> f.setCodeMode(codeModeItem.isSelected()));
        m.add(codeModeItem);
        JMenu lang = new JMenu("Language");
        ButtonGroup g = new ButtonGroup();
        for (Language l : Language.values()) {
            JRadioButtonMenuItem r = new JRadioButtonMenuItem(l.label(), l == Language.PYTHON);
            r.addActionListener(e -> f.setLanguage(l));
            g.add(r);
            lang.add(r);
            languageItems.add(r);
        }
        m.add(lang);
        item(m, "Run", KeyEvent.VK_F5, 0, f::runCode);
        item(m, "Stop", 0, 0, f::stopCode);
        item(m, "Clear output", 0, 0, f::clearOutput);
        return m;
    }

    private JMenu helpMenu() {
        JMenu m = new JMenu("Help");
        item(m, "Shortcuts", 0, 0, f::showShortcuts);
        item(m, "About", 0, 0, f::showAbout);
        return m;
    }

    private JMenuItem item(JMenu menu, String label, int key, int mods, Runnable action) {
        JMenuItem mi = new JMenuItem(label);
        if (key != 0) mi.setAccelerator(KeyStroke.getKeyStroke(key, mods));
        mi.addActionListener(e -> action.run());
        menu.add(mi);
        return mi;
    }
}
