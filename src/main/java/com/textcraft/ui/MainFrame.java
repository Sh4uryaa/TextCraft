package com.textcraft.ui;

import com.textcraft.ai.AIAction;
import com.textcraft.ai.Tone;
import com.textcraft.app.AppContext;
import com.textcraft.auth.SessionListener;
import com.textcraft.auth.SessionManager;
import com.textcraft.compiler.Language;
import com.textcraft.exception.DocumentException;
import com.textcraft.io.AbstractFormatHandler;
import com.textcraft.io.DocCopy;
import com.textcraft.io.FileFormatHandler;
import com.textcraft.io.FileHandlerFactory;
import com.textcraft.io.HtmlCodec;
import com.textcraft.model.DocType;
import com.textcraft.model.Document;
import com.textcraft.model.User;
import com.textcraft.model.UserSettings;
import com.textcraft.service.AutoSaveService;
import com.textcraft.service.RecentFilesService;
import com.textcraft.service.SettingsService;
import com.textcraft.util.AppExecutors;
import com.textcraft.util.HttpUtil;
import com.textcraft.util.TextUtils;
import com.textcraft.voice.QuotaGuard;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

/**
 * Main window (Docs style): menu bar, one toolbar, centred A4 page on a workspace background,
 * collapsible right side panel, bottom output panel (Code mode), status bar.
 * It only coordinates: files go through io handlers, cloud through services, never SQL or HTTP here.
 * Heavy panels (rich editor, code editor, AI, Study, Output) are created on first use.
 */
public class MainFrame extends JFrame implements ThemeChangeListener, SessionListener {
    private enum Mode { WELCOME, RICH, CODE }

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // ---- UI parts -------------------------------------------------------------------------------
    private final StatusBar statusBar = new StatusBar();
    private final ToolBarBuilder toolbar = new ToolBarBuilder(this);
    private final MenuBarBuilder menus = new MenuBarBuilder(this);
    private final JTextField titleField = new JTextField("Untitled document", 24);
    private final JLabel savedLabel = new JLabel(" ");
    private final JButton themeButton = new JButton();
    private final JButton avatarButton = new JButton("Sign in");
    private final JPanel header = new JPanel(new BorderLayout());
    private final CardLayout centerLayout = new CardLayout();
    private final JPanel center = new JPanel(centerLayout);
    private final JPanel workspace = new JPanel(new GridBagLayout());
    private final JScrollPane richScroll = new JScrollPane(workspace);
    private final JPanel codeCard = new JPanel(new BorderLayout());
    private final JPanel sideHost = new JPanel(new BorderLayout());
    private final WelcomePanel welcome;

    // ---- lazily created parts ---------------------------------------------------------------------
    private RichEditorPanel rich;
    private CodeEditorPanel code;
    private OutputPanel output;
    private JTabbedPane side;
    private AIPanel aiPanel;
    private StudyPanel studyPanel;
    private FindReplaceDialog findDialog;
    private SpecialCharsDialog specialDialog;

    // ---- services and state ---------------------------------------------------------------------------
    private final RecentFilesService recent = new RecentFilesService();
    private final SettingsService settings = SettingsService.getInstance();
    private final SessionManager session = SessionManager.getInstance();
    private Mode mode = Mode.WELCOME;
    private File currentFile;
    private Document cloudDoc;
    private Language language = Language.PYTHON;
    private boolean runnableDoc = true;
    private String docTitle = "Untitled document";
    private boolean recording;
    private boolean running;
    private File lastDir;

    public MainFrame() {
        super("TextCraft");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(1280, 720); // fits a 1366x768 screen
        setMinimumSize(new Dimension(900, 520));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exit(); }
        });

        welcome = new WelcomePanel(this::newDocument, this::openFile, this::openFromCloud, this::signIn, this::openPath);
        setJMenuBar(menus.build());
        JToolBar bar = toolbar.build();
        JPanel north = new JPanel(new BorderLayout());
        north.add(buildHeader(), BorderLayout.NORTH);
        north.add(bar, BorderLayout.SOUTH);

        richScroll.setBorder(BorderFactory.createEmptyBorder());
        richScroll.getVerticalScrollBar().setUnitIncrement(24);
        center.add(welcome, "welcome");
        center.add(richScroll, "rich");
        center.add(codeCard, "code");
        centerLayout.show(center, "welcome");

        sideHost.setPreferredSize(new Dimension(340, 0));
        sideHost.setVisible(false);

        add(north, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(sideHost, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        ThemeManager.getInstance().addListener(this);
        session.addListener(this);
        ErrorHandler.install(this);
        menus.setCodeModeSelected(false);
        menus.setLiteSelected(settings.get().isLiteMode());
        menus.setVoiceLanguageSelected(settings.get().getVoiceLanguage());
        applyLite(settings.get().isLiteMode());
        refreshRecent();
        onThemeChanged(ThemeManager.getInstance().getResolved(), ThemeManager.getInstance().palette());

        new AutoSaveService().start(
                () -> { TextEditor e = currentEditor(); return e != null && e.isDirty(); },
                this::draftSnapshot,
                time -> statusBar.setAutosave(time));
    }

    // =============================================================================================
    // layout pieces
    // =============================================================================================

    /** Editable title and "Saved to cloud" label on the left, theme button and account menu on the right. */
    private JPanel buildHeader() {
        titleField.setFont(titleField.getFont().deriveFont(16f));
        titleField.putClientProperty("JTextField.placeholderText", "Document title");
        titleField.addActionListener(e -> commitTitle());
        titleField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitTitle(); }
        });
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        left.setOpaque(false);
        left.add(titleField);
        left.add(savedLabel);

        themeButton.setToolTipText("Switch theme (Ctrl+Alt+D)");
        themeButton.setFocusable(false);
        themeButton.addActionListener(e -> toggleTheme());
        avatarButton.setToolTipText("Account");
        avatarButton.setFocusable(false);
        avatarButton.addActionListener(e -> accountMenu());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        right.add(themeButton);
        right.add(avatarButton);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void accountMenu() {
        JPopupMenu menu = new JPopupMenu();
        User u = session.getUser();
        if (u != null) {
            JMenuItem who = new JMenuItem(u.getDisplayName() + "  (" + u.getEmail() + ")");
            who.setEnabled(false);
            menu.add(who);
            JMenuItem out = new JMenuItem("Sign out");
            out.addActionListener(e -> signOut());
            menu.add(out);
        } else {
            JMenuItem in = new JMenuItem("Sign in with Google or email...");
            in.addActionListener(e -> signIn());
            menu.add(in);
        }
        menu.show(avatarButton, 0, avatarButton.getHeight());
    }

    private void ensureRich() {
        if (rich != null) return;
        rich = new RichEditorPanel(statusBar);
        UserSettings s = settings.get();
        rich.setDefaults(s.getDefaultFont(), s.getDefaultFontSize());
        rich.applyLiteMode(s.isLiteMode());
        rich.setFormatListener(() -> toolbar.syncFormat(rich));
        rich.addChangeListener(this::onDocumentChanged);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTH;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.insets = new Insets(24, 24, 24, 24);
        workspace.add(rich, gc);
    }

    private void ensureCode() {
        if (code != null) return;
        code = new CodeEditorPanel(statusBar);
        code.applyLiteMode(settings.get().isLiteMode());
        code.addChangeListener(this::onDocumentChanged);
        codeCard.add(code, BorderLayout.CENTER);
    }

    private void ensureOutput() {
        if (output != null) return;
        output = new OutputPanel(this::clearOutput, this::stopCode, this::explainError);
        codeCard.add(output, BorderLayout.SOUTH);
        codeCard.revalidate();
    }

    private void showMode(Mode m) {
        mode = m;
        centerLayout.show(center, m == Mode.WELCOME ? "welcome" : m == Mode.RICH ? "rich" : "code");
        statusBar.setMode(m == Mode.CODE ? "Code (" + language.label() + ")" : "Rich text");
        toolbar.setCodeMode(m == Mode.CODE);
        menus.setCodeModeSelected(m == Mode.CODE);
        if (m == Mode.CODE) ensureOutput();
        refreshTitleAndLabel();
        TextEditor e = currentEditor();
        if (e != null) e.focusEditor();
        if (m == Mode.RICH) {
            toolbar.syncFormat(rich);
            toolbar.setZoomShown(rich.getZoom());
        }
    }

    private TextEditor currentEditor() {
        return mode == Mode.RICH ? rich : mode == Mode.CODE ? code : null;
    }

    // =============================================================================================
    // title and state labels
    // =============================================================================================

    private void onDocumentChanged() {
        refreshTitleAndLabel();
    }

    private void refreshTitleAndLabel() {
        TextEditor e = currentEditor();
        boolean dirty = e != null && e.isDirty();
        if (e == null) savedLabel.setText(" ");
        else if (dirty) savedLabel.setText("Unsaved changes");
        else if (cloudDoc != null) savedLabel.setText("Saved to cloud");
        else if (currentFile != null) savedLabel.setText("Saved");
        else savedLabel.setText("Not saved");
        setTitle("TextCraft - " + docTitle + (dirty ? " *" : ""));
    }

    private void setDocTitle(String t) {
        docTitle = t;
        titleField.setText(t);
        refreshTitleAndLabel();
    }

    private void commitTitle() {
        String t = titleField.getText().trim();
        if (t.isEmpty()) {
            titleField.setText(docTitle);
            return;
        }
        if (t.equals(docTitle)) return;
        docTitle = t;
        refreshTitleAndLabel();
        if (cloudDoc != null && session.canUseCloud()) {
            Document d = cloudDoc;
            long uid = session.getUser().getId();
            d.setTitle(t);
            AppExecutors.asyncChecked(() -> { AppContext.get().documents().rename(uid, d.getId(), t); return Boolean.TRUE; },
                    r -> { if (!r.isOk()) ErrorHandler.error(r.error()); });
        }
    }

    public void renameDocument() {
        titleField.requestFocusInWindow();
        titleField.selectAll();
    }

    // =============================================================================================
    // File menu: new / open / save
    // =============================================================================================

    public void newDocument() {
        if (!confirmDiscard()) return;
        ensureRich();
        rich.clear();
        currentFile = null;
        cloudDoc = null;
        setDocTitle("Untitled document");
        showMode(Mode.RICH);
    }

    public void openFile() {
        JFileChooser fc = new JFileChooser(lastDir);
        fc.setDialogTitle("Open");
        fc.setAcceptAllFileFilterUsed(true);
        fc.addChoosableFileFilter(new FileNameExtensionFilter(
                "Documents (docx, pdf, md, rtf, html, txt)", "docx", "pdf", "md", "rtf", "html", "htm", "txt"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        lastDir = fc.getCurrentDirectory();
        openPath(fc.getSelectedFile());
    }

    /** Opens a file: handler chosen by extension (Factory), read on a worker thread. */
    public void openPath(File f) {
        if (!confirmDiscard()) return;
        if (!f.isFile()) {
            ErrorHandler.error("The file could not be found:\n" + f.getAbsolutePath());
            return;
        }
        String ext = TextUtils.extension(f.getName());
        boolean asCode = FileHandlerFactory.isCode(ext);
        if (!FileHandlerFactory.isKnown(ext) && AbstractFormatHandler.looksBinary(f)) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "\"" + f.getName() + "\" looks like a binary file, not text.\nOpen it as plain text anyway?",
                    "Binary file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
        }
        if ("pdf".equals(ext)) ErrorHandler.toast("PDF opened as text only. Layout, images and fonts are not preserved.");
        ErrorHandler.toast("Opening " + f.getName() + "...");
        AppExecutors.<Object>asyncChecked(() -> asCode
                ? (Object) AbstractFormatHandler.readPlainText(f)
                : (Object) FileHandlerFactory.forFile(f).read(f), r -> {
            if (!r.isOk()) {
                ErrorHandler.error("Could not open " + f.getName() + ":\n" + r.error());
                return;
            }
            try {
                if (asCode) {
                    showCodeText((String) r.get(), ext);
                } else {
                    ensureRich();
                    rich.loadFrom((StyledDocument) r.get());
                    showMode(Mode.RICH);
                }
            } catch (DocumentException e) {
                ErrorHandler.error("Could not open " + f.getName() + ":\n" + e.getMessage());
                return;
            }
            currentFile = f;
            cloudDoc = null;
            setDocTitle(TextUtils.baseName(f.getName()));
            recent.add(f);
            refreshRecent();
        });
    }

    private void showCodeText(String text, String ext) {
        ensureCode();
        code.loadText(text);
        Language l = Language.fromExtension(ext);
        runnableDoc = l != null;
        if (l != null) {
            language = l;
            code.setSyntax(l);
            menus.setLanguageSelected(l);
        } else {
            code.setSyntaxForExtension(ext);
        }
        showMode(Mode.CODE);
    }

    public void save() {
        if (currentEditor() == null) {
            ErrorHandler.toast("There is nothing to save yet.");
            return;
        }
        if (currentFile == null) saveAs();
        else writeFile(currentFile, true);
    }

    public void saveAs() {
        if (currentEditor() == null) {
            ErrorHandler.toast("There is nothing to save yet.");
            return;
        }
        File target = chooseTarget("Save As", null);
        if (target != null) writeFile(target, true);
    }

    public void makeCopy() {
        if (currentEditor() == null) {
            ErrorHandler.toast("There is nothing to copy yet.");
            return;
        }
        File target = chooseTarget("Make a copy", null);
        if (target != null) writeFile(target, false);
    }

    public void export(String ext) {
        if (currentEditor() == null) {
            ErrorHandler.toast("There is nothing to export yet.");
            return;
        }
        File target = chooseTarget("Export as " + ext.toUpperCase(), ext);
        if (target != null) writeFile(target, false);
    }

    /** Save dialog. forcedExt != null fixes the format (Export). Asks before overwriting. */
    private File chooseTarget(String title, String forcedExt) {
        JFileChooser fc = new JFileChooser(lastDir);
        fc.setDialogTitle(title);
        String base = docTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
        FileNameExtensionFilter first = null;
        if (forcedExt != null) {
            first = new FileNameExtensionFilter(forcedExt.toUpperCase() + " file", forcedExt);
            fc.setAcceptAllFileFilterUsed(false);
            fc.addChoosableFileFilter(first);
        } else if (mode == Mode.CODE) {
            fc.setAcceptAllFileFilterUsed(true);
            forcedExt = currentFile != null ? TextUtils.extension(currentFile.getName()) : language.extension();
        } else {
            fc.setAcceptAllFileFilterUsed(false);
            String[][] formats = {{"Word document (.docx)", "docx"}, {"PDF (.pdf) - layout is not preserved when reopened", "pdf"},
                    {"Plain text (.txt)", "txt"}, {"Markdown (.md)", "md"}, {"HTML (.html)", "html"}, {"Rich Text (.rtf)", "rtf"}};
            for (String[] fm : formats) {
                FileNameExtensionFilter flt = new FileNameExtensionFilter(fm[0], fm[1]);
                fc.addChoosableFileFilter(flt);
                if (first == null) first = flt;
            }
        }
        if (first != null) fc.setFileFilter(first);
        fc.setSelectedFile(new File(base + (forcedExt != null ? "." + forcedExt : first != null ? "." + first.getExtensions()[0] : "")));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        lastDir = fc.getCurrentDirectory();
        File f = fc.getSelectedFile();
        if (fc.getFileFilter() instanceof FileNameExtensionFilter) {
            FileNameExtensionFilter flt = (FileNameExtensionFilter) fc.getFileFilter();
            if (!flt.accept(f) || TextUtils.extension(f.getName()).isEmpty()) {
                f = new File(f.getParentFile(), TextUtils.baseName(f.getName()) + "." + flt.getExtensions()[0]);
            }
        } else if (TextUtils.extension(f.getName()).isEmpty() && forcedExt != null) {
            f = new File(f.getParentFile(), f.getName() + "." + forcedExt);
        }
        if (f.exists()) {
            int ok = JOptionPane.showConfirmDialog(this, "\"" + f.getName() + "\" already exists. Replace it?",
                    "Confirm", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return null;
        }
        return f;
    }

    /** What gets written: a copy of the page (rich) or the raw text (code). Taken on the EDT, written on a worker. */
    private Object snapshot() throws DocumentException {
        if (mode == Mode.CODE) return code.getAllText();
        DefaultStyledDocument copy = new DefaultStyledDocument();
        DocCopy.copy(rich.getStyledDocument(), copy);
        return copy;
    }

    private static void writeTo(File target, Object snap) throws DocumentException {
        String ext = TextUtils.extension(target.getName());
        try {
            if (snap instanceof String) {
                String text = (String) snap;
                boolean richFormat = "pdf".equals(ext) || "docx".equals(ext) || "rtf".equals(ext) || "html".equals(ext) || "md".equals(ext);
                if (richFormat) {
                    DefaultStyledDocument d = new DefaultStyledDocument();
                    d.insertString(0, text, null);
                    FileHandlerFactory.forExtension(ext).write(d, target);
                } else {
                    Files.write(target.toPath(), text.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                FileHandlerFactory.forExtension(ext).write((StyledDocument) snap, target);
            }
        } catch (javax.swing.text.BadLocationException | java.io.IOException e) {
            throw new DocumentException("Cannot write " + target.getName() + ": " + e.getMessage(), e);
        }
    }

    /** @param adopt true for Save/Save As (the file becomes the current file), false for Export/Make a copy */
    private void writeFile(File target, boolean adopt) {
        Object snap;
        try {
            snap = snapshot();
        } catch (DocumentException e) {
            ErrorHandler.error("Could not save", e);
            return;
        }
        final TextEditor ed = currentEditor();
        final String ext = TextUtils.extension(target.getName());
        AppExecutors.asyncChecked(() -> { writeTo(target, snap); return Boolean.TRUE; }, r -> {
            if (!r.isOk()) {
                ErrorHandler.error("Could not save " + target.getName() + ":\n" + r.error());
                return;
            }
            if (adopt) {
                currentFile = target;
                if (ed != null) ed.setDirty(false);
                AutoSaveService.clearDraft();
                recent.add(target);
                refreshRecent();
                refreshTitleAndLabel();
            }
            ErrorHandler.toast((adopt ? "Saved " : "Exported ") + target.getName()
                    + ("pdf".equals(ext) ? " (reopening a PDF reads text only)" : ""));
        });
    }

    // =============================================================================================
    // cloud
    // =============================================================================================

    /** True when signed in and the database is reachable; otherwise explains what to do. */
    private boolean requireCloud() {
        if (!session.isDatabaseAvailable()) {
            ErrorHandler.info("Cloud storage is not available right now (offline or no database configured).\n"
                    + "You can keep working with local files.");
            return false;
        }
        if (!session.isSignedIn()) {
            signIn();
            return false; // the user can repeat the command after signing in
        }
        return true;
    }

    private String contentForCloud() {
        return mode == Mode.CODE ? code.getAllText() : HtmlCodec.toHtml(rich.getStyledDocument(), false);
    }

    public void saveToCloud() {
        TextEditor ed = currentEditor();
        if (ed == null) {
            ErrorHandler.toast("There is nothing to save yet.");
            return;
        }
        if (!requireCloud()) return;
        long uid = session.getUser().getId();
        Document d = cloudDoc != null ? cloudDoc : new Document();
        d.setTitle(docTitle);
        d.setDocType(mode == Mode.CODE ? DocType.CODE : DocType.RICH);
        d.setLanguage(mode == Mode.CODE ? language.label() : null);
        String content = contentForCloud();
        ErrorHandler.toast("Saving to the cloud...");
        AppExecutors.asyncChecked(() -> AppContext.get().documents().saveToCloud(uid, d, content), r -> {
            if (!r.isOk()) {
                ErrorHandler.error("Could not save to the cloud:\n" + r.error());
                return;
            }
            cloudDoc = r.get();
            ed.setDirty(false);
            AutoSaveService.clearDraft();
            refreshTitleAndLabel();
            ErrorHandler.toast("Saved to cloud");
        });
    }

    public void openFromCloud() {
        if (!requireCloud()) return;
        if (!confirmDiscard()) return;
        long uid = session.getUser().getId();
        Optional<Document> pick = new CloudDialog(this, uid).showAndGet();
        if (!pick.isPresent()) return;
        long id = pick.get().getId();
        AppExecutors.asyncChecked(() -> AppContext.get().documents().open(uid, id), r -> {
            if (!r.isOk()) {
                ErrorHandler.error("Could not open the document:\n" + r.error());
                return;
            }
            Document d = r.get();
            loadContent(d.getDocType(), d.getContent() == null ? "" : d.getContent(), d.getLanguage());
            currentFile = null;
            cloudDoc = d;
            setDocTitle(d.getTitle());
        });
    }

    public void versionHistory() {
        if (cloudDoc == null) {
            ErrorHandler.info("Version history is available for cloud documents.\nUse File > Save to cloud first.");
            return;
        }
        if (!requireCloud()) return;
        Optional<com.textcraft.model.DocumentVersion> v = new VersionHistoryDialog(this, session.getUser().getId(),
                cloudDoc.getId(), cloudDoc.getDocType()).showAndGet();
        v.ifPresent(ver -> {
            loadContent(cloudDoc.getDocType(), ver.getContent() == null ? "" : ver.getContent(), cloudDoc.getLanguage());
            TextEditor e = currentEditor();
            if (e != null) e.setDirty(true); // restoring is an edit: save to keep it
            refreshTitleAndLabel();
            ErrorHandler.toast("Version restored. Save to keep it.");
        });
    }

    public void deleteCloudCopy() {
        if (cloudDoc == null) {
            ErrorHandler.info("This document has no cloud copy.");
            return;
        }
        if (!requireCloud()) return;
        int ok = JOptionPane.showConfirmDialog(this, "Delete the cloud copy of \"" + docTitle + "\"?\n"
                + "Your open document stays here, but the saved versions are removed.",
                "Delete cloud copy", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        long uid = session.getUser().getId();
        long id = cloudDoc.getId();
        AppExecutors.asyncChecked(() -> { AppContext.get().documents().delete(uid, id); return Boolean.TRUE; }, r -> {
            if (r.isOk()) {
                cloudDoc = null;
                refreshTitleAndLabel();
                ErrorHandler.toast("Cloud copy deleted");
            } else {
                ErrorHandler.error(r.error());
            }
        });
    }

    /** Shows content from the cloud, a version or an autosave draft. */
    private void loadContent(DocType type, String content, String languageLabel) {
        if (type == DocType.CODE) {
            ensureCode();
            code.loadText(content);
            for (Language l : Language.values()) {
                if (l.label().equals(languageLabel)) {
                    language = l;
                    code.setSyntax(l);
                    menus.setLanguageSelected(l);
                }
            }
            runnableDoc = true;
            showMode(Mode.CODE);
        } else {
            ensureRich();
            try {
                DefaultStyledDocument d = new DefaultStyledDocument();
                HtmlCodec.fromHtml(content, d);
                rich.loadFrom(d);
            } catch (DocumentException e) {
                ErrorHandler.error("Could not open the document", e);
                return;
            }
            showMode(Mode.RICH);
        }
    }

    // =============================================================================================
    // sign in / account
    // =============================================================================================

    public void signIn() {
        if (!session.isDatabaseAvailable()) {
            ErrorHandler.info("Sign-in needs the cloud database, which is not available right now.\n"
                    + "Check db.url in config/app.properties. Local files still work.");
            return;
        }
        new LoginDialog(this).setVisible(true);
    }

    public void signOut() {
        AppContext.get().auth().signOut();
        ErrorHandler.toast("Signed out");
    }

    @Override
    public void onSessionChanged(User u) {
        menus.setSignedIn(u != null);
        avatarButton.setIcon(null);
        avatarButton.setText(u == null ? "Sign in" : initial(u));
        avatarButton.setToolTipText(u == null ? "Sign in" : u.getDisplayName() + " (" + u.getEmail() + ")");
        if (u == null) return;
        ErrorHandler.toast("Signed in as " + u.getDisplayName());
        loadAvatar(u.getAvatarUrl());
        AppExecutors.async(() -> { settings.loadForUser(u); return Boolean.TRUE; }, r -> applySettings());
        refreshVoiceLabel(false);
    }

    private static String initial(User u) {
        String n = u.getDisplayName() == null || u.getDisplayName().isEmpty() ? u.getEmail() : u.getDisplayName();
        return n.substring(0, 1).toUpperCase();
    }

    /** Google profile picture, downloaded in the background and drawn as a circle. */
    private void loadAvatar(String url) {
        if (url == null || !url.startsWith("https://")) return;
        AppExecutors.asyncChecked(() -> {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<byte[]> resp = HttpUtil.client().send(req, HttpResponse.BodyHandlers.ofByteArray());
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(resp.body()));
            if (img == null) throw new java.io.IOException("not an image");
            BufferedImage out = new BufferedImage(28, 28, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setClip(new Ellipse2D.Float(0, 0, 28, 28));
            g.drawImage(img, 0, 0, 28, 28, null);
            g.dispose();
            return new ImageIcon(out);
        }, r -> {
            if (r.isOk() && session.isSignedIn()) {
                avatarButton.setText("");
                avatarButton.setIcon(r.get());
            } // on failure the initial letter stays: the avatar is cosmetic
        });
    }

    // =============================================================================================
    // Edit
    // =============================================================================================

    private void withEditor(Consumer<TextEditor> action) {
        TextEditor e = currentEditor();
        if (e == null) ErrorHandler.toast("Create or open a document first.");
        else action.accept(e);
    }

    public void undo() { withEditor(TextEditor::undo); }

    public void redo() { withEditor(TextEditor::redo); }

    public void cut() { withEditor(TextEditor::cut); }

    public void copy() { withEditor(TextEditor::copy); }

    public void paste() { withEditor(TextEditor::paste); }

    public void selectAll() { withEditor(TextEditor::selectAll); }

    public void findReplace() {
        withEditor(e -> {
            if (findDialog == null) findDialog = new FindReplaceDialog(this, this::currentEditor);
            findDialog.open(e.getSelectedText());
        });
    }

    /** Run a formatting action on the rich editor; explains politely when that is not possible. */
    public void format(Consumer<RichEditorPanel> action) {
        if (mode == Mode.CODE) ErrorHandler.toast("Formatting is not available in Code mode.");
        else if (mode == Mode.WELCOME) ErrorHandler.toast("Create or open a document first.");
        else action.accept(rich);
    }

    public void print() {
        if (currentEditor() == null) {
            ErrorHandler.toast("There is nothing to print yet.");
            return;
        }
        try {
            if (mode == Mode.CODE) code.print();
            else rich.print(); // always a white page with black automatic text
        } catch (java.awt.print.PrinterException e) {
            ErrorHandler.error("Printing failed", e);
        }
    }

    // =============================================================================================
    // View, Insert
    // =============================================================================================

    public void setZoom(double z) {
        if (mode != Mode.RICH) {
            ErrorHandler.toast("Zoom works on documents, not in Code mode.");
            return;
        }
        rich.setZoom(z);
        toolbar.setZoomShown(rich.getZoom());
    }

    public void setTheme(Theme t) {
        ThemeManager.getInstance().apply(t);
        settings.get().setTheme(t.name());
        AppExecutors.io().submit(settings::save);
    }

    public void toggleTheme() {
        setTheme(ThemeManager.getInstance().isDark() ? Theme.LIGHT : Theme.DARK);
    }

    public void setLiteMode(boolean lite) {
        settings.get().setLiteMode(lite);
        AppExecutors.io().submit(settings::save);
        applyLite(lite);
    }

    private void applyLite(boolean lite) {
        UIManager.put("ScrollPane.smoothScrolling", !lite); // no animation in Lite mode
        if (rich != null) rich.applyLiteMode(lite);
        if (code != null) code.applyLiteMode(lite);
        menus.setLiteSelected(lite);
    }

    public void showWordCount() {
        withEditor(e -> {
            String t = e.getAllText();
            JOptionPane.showMessageDialog(this,
                    "Words: " + TextUtils.wordCount(t) + "\nCharacters: " + t.length()
                            + "\nCharacters (no spaces): " + TextUtils.charsWithoutSpaces(t)
                            + "\nParagraphs / lines: " + TextUtils.paragraphCount(t),
                    "Word count", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public void toggleSidePanel() {
        if (sideHost.isVisible()) {
            sideHost.setVisible(false);
            revalidate();
        } else {
            showSide(side != null && side.getSelectedIndex() >= 0 ? side.getTitleAt(side.getSelectedIndex()) : "AI");
        }
    }

    private void showSide(String which) {
        if (side == null) {
            side = new JTabbedPane();
            sideHost.add(side, BorderLayout.CENTER);
        }
        if ("AI".equals(which) && aiPanel == null) {
            aiPanel = new AIPanel(this::currentEditor);
            side.addTab("AI", aiPanel);
        }
        if ("Study".equals(which) && studyPanel == null) {
            studyPanel = new StudyPanel(this::currentEditor, () -> cloudDoc == null ? null : cloudDoc.getId(), () -> docTitle);
            side.addTab("Study", studyPanel);
        }
        for (int i = 0; i < side.getTabCount(); i++) {
            if (side.getTitleAt(i).equals(which)) side.setSelectedIndex(i);
        }
        sideHost.setVisible(true);
        revalidate();
    }

    public void insertDateTime() {
        withEditor(e -> e.insertAtCaret(LocalDateTime.now().format(STAMP)));
    }

    public void showSpecialChars() {
        withEditor(e -> {
            if (specialDialog == null) specialDialog = new SpecialCharsDialog(this, s -> withEditor(ed -> ed.insertAtCaret(s)));
            specialDialog.setVisible(true);
        });
    }

    // =============================================================================================
    // AI, Study
    // =============================================================================================

    private static final String[] LANGUAGES = {"English", "Hindi", "Spanish", "French", "German", "Chinese", "Japanese", "Arabic"};

    public void ai(AIAction action, Tone tone) {
        if (currentEditor() == null) {
            ErrorHandler.toast("Create or open a document first.");
            return;
        }
        String extra = null;
        if (action == AIAction.TRANSLATE) {
            extra = (String) JOptionPane.showInputDialog(this, "Translate into:", "Translate", JOptionPane.QUESTION_MESSAGE,
                    null, LANGUAGES, "English");
            if (extra == null) return;
        } else if (action == AIAction.ASK) {
            extra = JOptionPane.showInputDialog(this, "What do you want to ask about the text?", "Ask AI", JOptionPane.QUESTION_MESSAGE);
            if (extra == null || extra.trim().isEmpty()) return;
        }
        showSide("AI");
        aiPanel.run(action, tone, extra);
    }

    private void explainError(String errorText) {
        showSide("AI");
        aiPanel.runOnText(AIAction.EXPLAIN, "Explain this program error in simple words and say how to fix it:\n" + errorText);
    }

    public void study(String kind) {
        if (currentEditor() == null) {
            ErrorHandler.toast("Create or open a document first.");
            return;
        }
        showSide("Study");
        switch (kind) {
            case "flashcards": studyPanel.flashcards(); break;
            case "quiz": studyPanel.quiz(); break;
            default: studyPanel.keyPoints(); break;
        }
    }

    public void showStudySets() {
        if (!requireCloud()) return;
        new StudySetsDialog(this, session.getUser().getId(),
                set -> { showSide("Study"); studyPanel.showFlashcards(set); },
                quiz -> { showSide("Study"); studyPanel.showQuiz(quiz); }).setVisible(true);
    }

    // =============================================================================================
    // Code mode
    // =============================================================================================

    public void setCodeMode(boolean on) {
        if (on == (mode == Mode.CODE)) {
            menus.setCodeModeSelected(mode == Mode.CODE);
            return;
        }
        if (on) {
            String text = mode == Mode.RICH ? rich.getAllText() : "";
            boolean wasDirty = mode == Mode.RICH && rich.isDirty();
            ensureCode();
            code.loadText(text.trim().isEmpty() ? language.template() : text);
            code.setSyntax(language);
            code.setDirty(wasDirty);
            runnableDoc = true;
            showMode(Mode.CODE);
        } else {
            String text = code.getAllText();
            boolean wasDirty = code.isDirty();
            ensureRich();
            rich.loadText(text);
            rich.setDirty(wasDirty);
            showMode(Mode.RICH);
        }
    }

    public void setLanguage(Language l) {
        language = l;
        menus.setLanguageSelected(l);
        if (code != null) {
            String cur = code.getAllText();
            for (Language x : Language.values()) {
                if (x != l && cur.equals(x.template())) { // untouched template: swap to the new language's template
                    boolean dirty = code.isDirty();
                    code.loadText(l.template());
                    code.setDirty(dirty);
                }
            }
            code.setSyntax(l);
        }
        if (mode == Mode.CODE) statusBar.setMode("Code (" + l.label() + ")");
        runnableDoc = true;
    }

    public void runCode() {
        if (mode != Mode.CODE) {
            ErrorHandler.toast("Turn on Code mode (Code menu) to run code.");
            return;
        }
        if (!runnableDoc) {
            ErrorHandler.toast("This file type cannot be run. Choose Python, C++ or Java in Code > Language.");
            return;
        }
        if (running) {
            ErrorHandler.toast("A program is already running. Use Stop first.");
            return;
        }
        ensureOutput();
        output.showRunning();
        running = true;
        String src = code.getAllText();
        String stdin = output.getStdin();
        Language l = language;
        AppExecutors.asyncChecked(() -> AppContext.get().codeRunner().run(l, src, stdin), r -> {
            running = false;
            if (r.isOk()) output.showResult(r.get());
            else output.showError(r.error());
        });
    }

    public void stopCode() {
        AppContext.get().codeRunner().cancel();
    }

    public void clearOutput() {
        if (output != null) output.clear();
    }

    // =============================================================================================
    // Voice typing and read aloud
    // =============================================================================================

    public void setVoiceLanguage(String code) {
        settings.get().setVoiceLanguage(code);
        AppExecutors.io().submit(settings::save);
        menus.setVoiceLanguageSelected(code);
    }

    /** Push-to-talk: first click starts recording, second click stops it and inserts the text. */
    public void toggleVoice() {
        if (recording) {
            AppContext.get().voice().stopRecording();
            return;
        }
        if (currentEditor() == null) {
            ErrorHandler.toast("Create or open a document first.");
            toolbar.setMicActive(false);
            return;
        }
        recording = true;
        toolbar.setMicActive(true);
        ErrorHandler.toast("Listening... click the microphone again to stop (max 60 seconds)");
        String lang = settings.get().getVoiceLanguage();
        AppExecutors.asyncChecked(() -> AppContext.get().voice().listen(lang), r -> {
            recording = false;
            toolbar.setMicActive(false);
            if (r.isOk()) {
                TextEditor e = currentEditor();
                if (r.get().isEmpty()) ErrorHandler.toast("No speech was heard.");
                else if (e != null) e.insertAtCaret(r.get().endsWith("\n") ? r.get() : r.get() + " ");
            } else {
                ErrorHandler.error(r.error());
            }
            refreshVoiceLabel(true);
        });
    }

    /** Updates "Voice minutes x/60" and warns once the user passes 50 minutes. */
    private void refreshVoiceLabel(boolean warn) {
        AppExecutors.asyncChecked(() -> {
            QuotaGuard g = AppContext.get().voice().quota();
            return new double[]{g.minutesUsed(), g.capMinutes(), g.shouldWarn() ? 1 : 0};
        }, r -> {
            if (!r.isOk()) return;
            statusBar.setVoiceMinutes(r.get()[0], (int) r.get()[1]);
            if (warn && r.get()[2] > 0) {
                ErrorHandler.toast(String.format("You have used %.0f of %d voice minutes this month.", r.get()[0], (int) r.get()[1]));
            }
        });
    }

    public void readAloud() {
        TextEditor e = currentEditor();
        if (e == null) {
            ErrorHandler.toast("Create or open a document first.");
            return;
        }
        String text = e.getSelectedText().isEmpty() ? e.getAllText() : e.getSelectedText();
        double speed = settings.get().getVoiceSpeed();
        AppExecutors.asyncChecked(() -> { AppContext.get().tts().speak(text, speed); return Boolean.TRUE; }, r -> {
            if (!r.isOk()) ErrorHandler.error(r.error());
        });
    }

    public void pauseReading() {
        AppContext.get().tts().pause();
    }

    public void stopReading() {
        AppContext.get().tts().stop();
    }

    // =============================================================================================
    // Settings, help
    // =============================================================================================

    public void showSettings() {
        if (new SettingsDialog(this).showAndApply()) applySettings();
    }

    /** Applies the saved settings (after Settings dialog, sign-in or startup). */
    public void applySettings() {
        UserSettings s = settings.get();
        try {
            Theme t = Theme.valueOf(s.getTheme());
            if (t != ThemeManager.getInstance().getChoice()) ThemeManager.getInstance().apply(t);
        } catch (IllegalArgumentException e) {
            // unknown theme name: keep the current one
        }
        applyLite(s.isLiteMode());
        menus.setVoiceLanguageSelected(s.getVoiceLanguage());
        if (rich != null) rich.setDefaults(s.getDefaultFont(), s.getDefaultFontSize());
    }

    public void showShortcuts() {
        String text = "Ctrl+N / O / S / Shift+S    New / Open / Save / Save As\n"
                + "Ctrl+Z / Y                  Undo / Redo\n"
                + "Ctrl+B / I / U              Bold / Italic / Underline\n"
                + "Ctrl+F / P                  Find and replace / Print\n"
                + "F5                          Run code\n"
                + "Ctrl+Alt+1 / 2 / 3          Heading 1 / 2 / 3\n"
                + "Ctrl+Shift+A                Side panel (AI / Study)\n"
                + "Ctrl+Alt+M                  Voice typing (start / stop)\n"
                + "Ctrl+Alt+R                  Read aloud\n"
                + "Ctrl+Alt+F                  Flashcards\n"
                + "Ctrl+Alt+D                  Switch light / dark theme";
        javax.swing.JTextArea area = new javax.swing.JTextArea(text);
        area.setEditable(false);
        area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, area, "Keyboard shortcuts", JOptionPane.PLAIN_MESSAGE);
    }

    public void showAbout() {
        JOptionPane.showMessageDialog(this,
                "TextCraft 1.0\nA lightweight Google Docs-style editor in Java Swing.\n"
                        + "Many file formats, Gemini AI, code runner, voice typing, study mode, cloud storage.\nUniversity project.",
                "About TextCraft", JOptionPane.INFORMATION_MESSAGE);
    }

    // =============================================================================================
    // startup, autosave, closing
    // =============================================================================================

    /** Background start-up work: database, silent Google login, draft recovery. The window is already visible. */
    public void startBackground() {
        applySettings();
        AppExecutors.asyncChecked(() -> {
            boolean ok = AppContext.get().initDatabase();
            if (ok) AppContext.get().auth().trySilentGoogle(); // listener updates the UI when it works
            return ok;
        }, r -> {
            if (!r.isOk()) ErrorHandler.toast("Offline mode: " + r.error());
            else if (!r.get()) ErrorHandler.toast("Offline mode: cloud features are unavailable. Local files still work.");
        });
        refreshVoiceLabel(false);
        Optional<AutoSaveService.Snapshot> draft = AutoSaveService.readDraft();
        if (draft.isPresent()) {
            int ok = JOptionPane.showConfirmDialog(this, "TextCraft found an unsaved draft of \"" + draft.get().title
                            + "\" from the last session.\nRestore it?", "Restore draft", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                loadContent(draft.get().code ? DocType.CODE : DocType.RICH, draft.get().content, null);
                currentFile = null;
                cloudDoc = null;
                setDocTitle(draft.get().title);
                TextEditor e = currentEditor();
                if (e != null) e.setDirty(true);
                refreshTitleAndLabel();
            } else {
                AutoSaveService.clearDraft();
            }
        }
    }

    private AutoSaveService.Snapshot draftSnapshot() {
        return new AutoSaveService.Snapshot(contentForCloud(), docTitle, mode == Mode.CODE);
    }

    private void refreshRecent() {
        List<File> files = recent.list();
        menus.setRecent(files);
        welcome.setRecent(files);
    }

    /** Unsaved-changes prompt: Save / Don't save / Cancel. Returns false when the user cancels. */
    private boolean confirmDiscard() {
        TextEditor e = currentEditor();
        if (e == null || !e.isDirty()) return true;
        int c = JOptionPane.showOptionDialog(this, "Save changes to \"" + docTitle + "\"?", "Unsaved changes",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[]{"Save", "Don't save", "Cancel"}, "Save");
        if (c == 0) return saveAndWait();
        return c == 1;
    }

    /** Save that the caller waits for (used by the unsaved-changes prompt). Work still runs off the EDT. */
    private boolean saveAndWait() {
        TextEditor ed = currentEditor();
        try {
            if (cloudDoc != null && session.canUseCloud()) {
                long uid = session.getUser().getId();
                cloudDoc.setTitle(docTitle);
                String content = contentForCloud();
                Document d = cloudDoc;
                cloudDoc = await(() -> AppContext.get().documents().saveToCloud(uid, d, content));
            } else {
                File target = currentFile != null ? currentFile : chooseTarget("Save As", null);
                if (target == null) return false;
                Object snap = snapshot();
                await(() -> { writeTo(target, snap); return Boolean.TRUE; });
                currentFile = target;
                recent.add(target);
            }
            ed.setDirty(false);
            AutoSaveService.clearDraft();
            return true;
        } catch (Exception e) {
            ErrorHandler.error("Could not save", e);
            return false;
        }
    }

    private static <T> T await(Callable<T> work) throws Exception {
        try {
            return AppExecutors.io().submit(work).get();
        } catch (java.util.concurrent.ExecutionException e) {
            throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        }
    }

    public void exit() {
        if (!confirmDiscard()) return;
        AutoSaveService.clearDraft();
        try {
            AppContext.get().tts().stop();
        } catch (RuntimeException ignored) {
            // nothing was speaking
        }
        dispose();
        System.exit(0);
    }

    // =============================================================================================
    // theme
    // =============================================================================================

    @Override
    public void onThemeChanged(Theme resolved, Palette p) {
        workspace.setBackground(p.workspace);
        richScroll.getViewport().setBackground(p.workspace);
        header.setBackground(p.surface);
        sideHost.setBackground(p.surface);
        sideHost.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, p.border));
        savedLabel.setForeground(p.text2);
        themeButton.setText(resolved == Theme.DARK ? "☀" : "☾"); // sun / moon
        invalidate();
        validate();
        repaint();
    }
}
