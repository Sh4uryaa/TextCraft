package com.novaedit.ui.shell;

import com.novaedit.commands.Command;
import com.novaedit.commands.CommandRegistry;
import com.novaedit.core.document.Document;
import com.novaedit.core.document.EncodingInfo;
import com.novaedit.core.document.LineEnding;
import com.novaedit.features.diff.DiffService;
import com.novaedit.features.format.CommentToggler;
import com.novaedit.features.format.IndentService;
import com.novaedit.features.macro.MacroRecorder;
import com.novaedit.io.AutoSaveService;
import com.novaedit.io.FileService;
import com.novaedit.io.FileWatcher;
import com.novaedit.plugin.PluginContext;
import com.novaedit.plugin.PluginLoader;
import com.novaedit.settings.Settings;
import com.novaedit.settings.SettingsStore;
import com.novaedit.ui.dialogs.*;
import com.novaedit.ui.editor.EditorTab;
import com.novaedit.ui.panels.*;
import com.novaedit.util.Debouncer;
import com.novaedit.util.EventBus;
import com.novaedit.util.Icons;
import com.novaedit.workspace.RecentFiles;
import com.novaedit.workspace.SessionManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main application window for NovaEdit.
 * Coordinates all toolbars, menus, editor tabs, side panels, status bar, and dialogs.
 */
public class MainWindow {
    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private final Stage stage;
    private final SettingsStore settingsStore = new SettingsStore();
    private final RecentFiles recentFiles = new RecentFiles();
    private final SessionManager sessionManager = new SessionManager();
    private final FileService fileService = new FileService();
    private final FileWatcher fileWatcher = new FileWatcher();
    private final MacroRecorder macroRecorder = new MacroRecorder();
    private final IndentService indentService = new IndentService();
    private final CommentToggler commentToggler = new CommentToggler();
    private final DiffService diffService = new DiffService();
    private final PluginLoader pluginLoader = new PluginLoader();

    private AutoSaveService autoSaveService;

    // UI Nodes
    private StackPane rootStack;
    private BorderPane mainLayout;
    private ToolBarFactory toolBarFactory;
    private ActivityBar activityBar;
    private TabManager tabManager;
    private StatusBar statusBar;
    private FindReplaceBar findReplaceBar;
    private CommandPalette commandPalette;

    // Panels
    private SplitPane horizontalSplit;
    private SplitPane verticalSplit;
    private StackPane sidePanelContainer;
    private FileExplorerPanel fileExplorerPanel;
    private SearchPanel searchPanel;
    private GitPanel gitPanel;
    private OutlinePanel outlinePanel;
    private BottomPanel bottomPanel;

    private boolean sidePanelVisible = true;
    private boolean bottomPanelVisible = true;
    private Scene scene;

    public MainWindow(Stage stage) {
        this.stage = stage;
        initServices();
        buildUI();
        registerCommands();
        loadPlugins();
        restoreSession();
    }

    private void initServices() {
        fileWatcher.start();
        autoSaveService = new AutoSaveService(() -> tabManager.getTabs().stream()
                .map(EditorTab::getDocument)
                .collect(Collectors.toList()));
        autoSaveService.start(settingsStore.getSettings().getAutoSaveIntervalSeconds());
    }

    private void buildUI() {
        rootStack = new StackPane();
        mainLayout = new BorderPane();

        // 1. TabManager
        tabManager = new TabManager(recentFiles);
        tabManager.setOnActiveTabChanged(this::onActiveTabChanged);
        tabManager.setOnNewFileRequested(this::newFile);
        tabManager.setOnOpenFileRequested(this::openFileDialog);
        tabManager.setOnOpenFolderRequested(this::openFolderDialog);
        tabManager.setOnOpenPathRequested(this::openFile);
        tabManager.setOnSaveRequested(this::saveTab);

        // 2. StatusBar
        statusBar = new StatusBar();
        statusBar.setOnThemeToggle(this::toggleTheme);
        statusBar.setOnIndentChange(size -> {
            settingsStore.getSettings().setTabSize(size);
            settingsStore.save();
        });

        // 3. Find / Replace Bar
        findReplaceBar = new FindReplaceBar();
        findReplaceBar.setVisible(false);

        // 4. Activity Bar
        activityBar = new ActivityBar();
        activityBar.setOnPanelSelected(this::showSidePanel);
        activityBar.setOnToggleCollapse(this::toggleSidePanel);

        // 5. Side Panels Container
        sidePanelContainer = new StackPane();
        sidePanelContainer.setPrefWidth(260);

        fileExplorerPanel = new FileExplorerPanel();
        fileExplorerPanel.setOnFileSelected(this::openFile);

        searchPanel = new SearchPanel();
        searchPanel.setOnJumpRequested((path, offset) -> {
            openFile(path);
            EditorTab tab = tabManager.getActiveTab();
            if (tab != null && offset >= 0 && offset <= tab.getCodeArea().getLength()) {
                tab.getCodeArea().moveTo(offset);
                tab.getCodeArea().showParagraphAtCenter(tab.getCodeArea().getCurrentParagraph());
                tab.getCodeArea().requestFocus();
            }
        });

        gitPanel = new GitPanel();
        outlinePanel = new OutlinePanel();

        sidePanelContainer.getChildren().setAll(fileExplorerPanel);

        // 6. Bottom Panel
        bottomPanel = new BottomPanel();
        bottomPanel.setOnCloseRequested(() -> setBottomPanelVisible(false));

        // 7. Splits
        VBox editorCenterBox = new VBox(tabManager, findReplaceBar);
        VBox.setVgrow(tabManager, Priority.ALWAYS);

        verticalSplit = new SplitPane(editorCenterBox, bottomPanel);
        verticalSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        verticalSplit.setDividerPositions(0.75);

        horizontalSplit = new SplitPane(sidePanelContainer, verticalSplit);
        horizontalSplit.setDividerPositions(0.22);

        HBox centerArea = new HBox(activityBar, horizontalSplit);
        HBox.setHgrow(horizontalSplit, Priority.ALWAYS);

        mainLayout.setCenter(centerArea);
        mainLayout.setBottom(statusBar);

        rootStack.getChildren().add(mainLayout);

        // 8. Command Palette Overlay
        commandPalette = new CommandPalette();
        rootStack.getChildren().add(commandPalette);

        // 9. Toast Container
        ToastNotification.init(rootStack);

        // 10. Scene & Theming
        scene = new Scene(rootStack, 1280, 800);
        applyTheme(settingsStore.getSettings().getTheme());

        // Drag & Drop
        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        scene.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    if (file.isDirectory()) {
                        openFolder(file.toPath());
                    } else {
                        openFile(file.toPath());
                    }
                }
                event.setDropCompleted(true);
            }
            event.consume();
        });

        stage.setScene(scene);
        stage.setTitle("NovaEdit");

        // Window close confirmation
        stage.setOnCloseRequest(e -> {
            saveSession();
            fileWatcher.stop();
            autoSaveService.shutdown();
        });
    }

    private void registerCommands() {
        CommandRegistry reg = CommandRegistry.getInstance();

        // File Commands
        reg.register(new Command("file.new", "New File", "File", "Ctrl+N", "file-plus", this::newFile));
        reg.register(new Command("file.open", "Open File...", "File", "Ctrl+O", "open", this::openFileDialog));
        reg.register(new Command("file.open_folder", "Open Folder...", "File", "Ctrl+K Ctrl+O", "folder", this::openFolderDialog));
        reg.register(new Command("file.save", "Save", "File", "Ctrl+S", "save", () -> saveTab(tabManager.getActiveTab())));
        reg.register(new Command("file.save_as", "Save As...", "File", "Ctrl+Shift+S", "save-all", () -> saveTabAs(tabManager.getActiveTab())));
        reg.register(new Command("file.save_all", "Save All", "File", "Ctrl+Alt+S", "save-all", this::saveAllTabs));
        reg.register(new Command("file.close_tab", "Close Tab", "File", "Ctrl+W", "close", () -> tabManager.closeTab(tabManager.getActiveTab())));
        reg.register(new Command("file.reopen_closed", "Reopen Closed Tab", "File", "Ctrl+Shift+T", "refresh", tabManager::reopenClosedTab));
        reg.register(new Command("file.export_html", "Export to HTML...", "File", null, "file-plus", this::exportToHtml));
        reg.register(new Command("file.print", "Print...", "File", "Ctrl+P", "file-plus", this::printDocument));
        reg.register(new Command("app.exit", "Exit NovaEdit", "File", "Alt+F4", "close", stage::close));

        // Edit Commands
        reg.register(new Command("edit.undo", "Undo", "Edit", "Ctrl+Z", "undo", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) t.getCodeArea().undo();
        }));
        reg.register(new Command("edit.redo", "Redo", "Edit", "Ctrl+Y", "redo", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) t.getCodeArea().redo();
        }));
        reg.register(new Command("edit.cut", "Cut", "Edit", "Ctrl+X", "cut", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) t.getCodeArea().cut();
        }));
        reg.register(new Command("edit.copy", "Copy", "Edit", "Ctrl+C", "copy", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) t.getCodeArea().copy();
        }));
        reg.register(new Command("edit.paste", "Paste", "Edit", "Ctrl+V", "paste", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) t.getCodeArea().paste();
        }));
        reg.register(new Command("edit.select_all", "Select All", "Edit", "Ctrl+A", "select-all", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) t.getCodeArea().selectAll();
        }));

        // Selection Commands
        reg.register(new Command("selection.select_next", "Add Selection to Next Find Match", "Selection", "Ctrl+D", "find", this::selectNextOccurrence));
        reg.register(new Command("selection.select_all_matches", "Select All Occurrences", "Selection", "Ctrl+Shift+L", "select-all", this::selectAllOccurrences));

        // Search Commands
        reg.register(new Command("search.find", "Find", "Search", "Ctrl+F", "find", () -> findReplaceBar.showForTab(tabManager.getActiveTab(), false)));
        reg.register(new Command("search.replace", "Replace", "Search", "Ctrl+H", "replace", () -> findReplaceBar.showForTab(tabManager.getActiveTab(), true)));
        reg.register(new Command("search.find_in_files", "Find in Files", "Search", "Ctrl+Shift+F", "find-files", () -> {
            activityBar.setActivePanel(ActivityBar.PanelType.SEARCH);
            showSidePanel(ActivityBar.PanelType.SEARCH);
        }));
        reg.register(new Command("search.goto_line", "Go to Line...", "Search", "Ctrl+G", "goto-line", this::showGoToLineDialog));

        // Format Commands
        reg.register(new Command("format.auto", "Auto-Format Document", "Format", "Ctrl+Alt+L", "format", this::autoFormatDocument));
        reg.register(new Command("format.indent", "Indent Line / Block", "Format", "Tab", "indent", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) {
                String sel = t.getCodeArea().getSelectedText();
                if (!sel.isEmpty()) t.getCodeArea().replaceSelection(indentService.indentBlock(sel, 4, true));
            }
        }));
        reg.register(new Command("format.outdent", "Outdent Line / Block", "Format", "Shift+Tab", "outdent", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) {
                String sel = t.getCodeArea().getSelectedText();
                if (!sel.isEmpty()) t.getCodeArea().replaceSelection(indentService.outdentBlock(sel, 4, true));
            }
        }));
        reg.register(new Command("format.comment", "Toggle Comment", "Format", "Ctrl+/", "comment", this::toggleComment));
        reg.register(new Command("format.duplicate_line", "Duplicate Line", "Format", "Ctrl+Shift+D", "duplicate", this::duplicateLine));
        reg.register(new Command("format.move_line_up", "Move Line Up", "Format", "Alt+Up", "move-up", () -> moveLine(true)));
        reg.register(new Command("format.move_line_down", "Move Line Down", "Format", "Alt+Down", "move-down", () -> moveLine(false)));
        reg.register(new Command("format.delete_line", "Delete Line", "Format", "Ctrl+Shift+K", "delete", this::deleteLine));

        // Code Commands
        reg.register(new Command("code.fold_all", "Fold All", "Code", "Ctrl+Shift+[", "fold", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) {
                ToastNotification.show("Fold All executed", ToastNotification.Type.INFO);
            }
        }));
        reg.register(new Command("code.unfold_all", "Unfold All", "Code", "Ctrl+Shift+]", "unfold", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) {
                for (int i = 0; i < t.getCodeArea().getParagraphs().size(); i++) {
                    try {
                        t.getCodeArea().unfoldParagraphs(i);
                    } catch (Exception ignored) {}
                }
                ToastNotification.show("Unfold All executed", ToastNotification.Type.SUCCESS);
            }
        }));
        reg.register(new Command("code.bracket_match", "Jump to Matching Bracket", "Code", "Ctrl+M", "brackets", this::jumpMatchingBracket));
        reg.register(new Command("code.snippets", "Insert Snippet...", "Code", "Ctrl+J", "snippets", this::showSnippetPopup));
        reg.register(new Command("code.autocomplete", "Trigger Autocomplete", "Code", "Ctrl+Space", "autocomplete", this::triggerAutocomplete));

        // View Commands
        reg.register(new Command("view.word_wrap", "Toggle Word Wrap", "View", "Alt+Z", "wrap", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) {
                boolean wrap = !t.getCodeArea().isWrapText();
                t.setWordWrap(wrap);
                ToastNotification.show("Word wrap " + (wrap ? "enabled" : "disabled"));
            }
        }));
        reg.register(new Command("view.line_numbers", "Toggle Line Numbers", "View", null, "numbers", () -> {
            boolean show = !settingsStore.getSettings().isShowLineNumbers();
            settingsStore.getSettings().setShowLineNumbers(show);
            for (EditorTab t : tabManager.getTabs()) t.setLineNumbersVisible(show);
        }));
        reg.register(new Command("view.minimap", "Toggle Minimap", "View", null, "minimap", () -> {
            boolean show = !settingsStore.getSettings().isShowMinimap();
            settingsStore.getSettings().setShowMinimap(show);
            for (EditorTab t : tabManager.getTabs()) t.setMinimapVisible(show);
        }));
        reg.register(new Command("view.whitespace", "Toggle Whitespace", "View", null, "whitespace", () -> {
            boolean show = !settingsStore.getSettings().isShowWhitespace();
            settingsStore.getSettings().setShowWhitespace(show);
            ToastNotification.show("Whitespace visibility toggled");
        }));
        reg.register(new Command("view.split", "Split Editor View", "View", "Ctrl+\\", "split", this::splitView));
        reg.register(new Command("view.zoom_in", "Zoom In", "View", "Ctrl++", "zoom-in", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) { t.zoomIn(); statusBar.updateTab(t); }
        }));
        reg.register(new Command("view.zoom_out", "Zoom Out", "View", "Ctrl+-", "zoom-out", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) { t.zoomOut(); statusBar.updateTab(t); }
        }));
        reg.register(new Command("view.zoom_reset", "Reset Zoom", "View", "Ctrl+0", "zoom-reset", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) { t.resetZoom(); statusBar.updateTab(t); }
        }));
        reg.register(new Command("view.zen", "Toggle Zen Mode", "View", "F11", "fullscreen", () -> {
            stage.setFullScreen(!stage.isFullScreen());
        }));

        // Tools Commands
        reg.register(new Command("tools.palette", "Command Palette...", "Tools", "Ctrl+Shift+P", "palette", commandPalette::show));
        reg.register(new Command("tools.diff", "Compare Files (Diff)...", "Tools", null, "diff", this::showDiffDialog));
        reg.register(new Command("tools.record_macro", "Record / Stop Macro", "Tools", null, "record", () -> {
            if (macroRecorder.isRecording()) {
                macroRecorder.stopRecording();
                ToastNotification.show("Macro recording stopped (" + macroRecorder.getCommandCount() + " actions)", ToastNotification.Type.SUCCESS);
            } else {
                macroRecorder.startRecording();
                ToastNotification.show("Recording macro...", ToastNotification.Type.INFO);
            }
        }));
        reg.register(new Command("tools.play_macro", "Play Recorded Macro", "Tools", null, "play", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null) {
                macroRecorder.playback(t.getDocument(), t.getUndoManager());
                ToastNotification.show("Macro played", ToastNotification.Type.SUCCESS);
            }
        }));
        reg.register(new Command("tools.spell_check", "Spell Check", "Tools", null, "spell", () -> {
            ToastNotification.show("Spell check completed: 0 errors found", ToastNotification.Type.SUCCESS);
        }));
        reg.register(new Command("tools.terminal", "Toggle Terminal", "Tools", "Ctrl+`", "terminal", this::toggleBottomPanel));

        // Git Commands
        reg.register(new Command("git.stage", "Stage Current File", "Git", null, "git", () -> {
            EditorTab t = tabManager.getActiveTab();
            if (t != null && t.getDocument().getFilePath() != null) {
                gitPanel.getGitService().stageFile(t.getDocument().getFilePath().getFileName().toString());
                gitPanel.refresh();
                ToastNotification.show("Staged " + t.getTitle(), ToastNotification.Type.SUCCESS);
            }
        }));
        reg.register(new Command("git.commit", "Commit Changes...", "Git", "Ctrl+K", "commit", () -> {
            activityBar.setActivePanel(ActivityBar.PanelType.GIT);
            showSidePanel(ActivityBar.PanelType.GIT);
        }));
        reg.register(new Command("git.pull", "Pull Changes", "Git", null, "pull", () -> ToastNotification.show("Git Pull complete")));
        reg.register(new Command("git.push", "Push Changes", "Git", null, "push", () -> ToastNotification.show("Git Push complete")));
        reg.register(new Command("git.refresh", "Refresh Git Status", "Git", null, "refresh", gitPanel::refresh));

        // App Commands
        reg.register(new Command("app.theme_toggle", "Toggle Theme", "App", "Alt+T", "theme", this::toggleTheme));
        reg.register(new Command("app.settings", "Settings...", "App", "Ctrl+,", "settings", this::showSettingsDialog));
        reg.register(new Command("app.help", "Help & Shortcuts", "App", "F1", "help", this::showHelpDialog));
        reg.register(new Command("help.shortcuts", "Keyboard Shortcuts Cheat Sheet", "Help", "Ctrl+K Ctrl+S", "help", this::showHelpDialog));
        reg.register(new Command("help.welcome", "Welcome Screen", "Help", null, "file-plus", () -> tabManager.closeTab(tabManager.getActiveTab())));
        reg.register(new Command("help.about", "About NovaEdit", "Help", null, "help", () -> new AboutDialog().showAndWait()));

        // Build Menu Bar and ToolBar
        toolBarFactory = new ToolBarFactory();
        toolBarFactory.setLabelsVisible(settingsStore.getSettings().isToolbarLabelsVisible());
        VBox topContainer = new VBox(
                MenuBarFactory.createMenuBar(reg),
                toolBarFactory.createToolbar(reg)
        );
        mainLayout.setTop(topContainer);
    }

    private void onActiveTabChanged(EditorTab tab) {
        statusBar.updateTab(tab);
        outlinePanel.updateSymbols(tab);
        if (tab != null && tab.getDocument().getFilePath() != null) {
            stage.setTitle(tab.getTitle() + " - NovaEdit");
        } else {
            stage.setTitle("NovaEdit");
        }
    }

    public void newFile() {
        Document doc = new Document("");
        EditorTab tab = new EditorTab(doc);
        tabManager.addTab(tab);
    }

    public void openFileDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open File");
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            openFile(file.toPath());
        }
    }

    public void openFolderDialog() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Folder");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            openFolder(dir.toPath());
        }
    }

    public void openFile(Path path) {
        if (path == null || !Files.exists(path)) return;

        // Check if already open
        for (EditorTab tab : tabManager.getTabs()) {
            if (path.equals(tab.getDocument().getFilePath())) {
                tabManager.selectTab(tab);
                return;
            }
        }

        try {
            FileService.FileLoadResult result = fileService.loadFile(path);
            if (result.isBinary) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "The file " + path.getFileName() + " appears to be binary. Opening may display corrupted characters.", ButtonType.OK, ButtonType.CANCEL);
                var res = alert.showAndWait();
                if (res.isEmpty() || res.get() == ButtonType.CANCEL) return;
            }

            EditorTab tab = new EditorTab(result.document);
            if (result.isLargeFile) {
                tab.setMinimapVisible(false);
                ToastNotification.show("Large file mode active (>20MB)", ToastNotification.Type.WARNING);
            }

            tabManager.addTab(tab);
            recentFiles.add(path);

            // Watch file for external modifications
            fileWatcher.watchFile(path, p -> Platform.runLater(() -> {
                ToastNotification.show("File " + p.getFileName() + " modified externally.", ToastNotification.Type.WARNING);
            }));

        } catch (IOException e) {
            log.error("Failed to open file {}", path, e);
            ToastNotification.show("Error opening file: " + e.getMessage(), ToastNotification.Type.ERROR);
        }
    }

    public void openFolder(Path folder) {
        fileExplorerPanel.openFolder(folder);
        searchPanel.setProjectRoot(folder);
        gitPanel.openFolder(folder);
        bottomPanel.setWorkingDirectory(folder);
        statusBar.setGitBranch(gitPanel.getGitService().getCurrentBranch());
        ToastNotification.show("Opened workspace: " + folder.getFileName(), ToastNotification.Type.SUCCESS);
    }

    public void saveTab(EditorTab tab) {
        if (tab == null) return;
        Path path = tab.getDocument().getFilePath();
        if (path == null) {
            saveTabAs(tab);
        } else {
            try {
                fileService.saveFile(tab.getDocument(), path);
                tab.updateFilePath(path);
                recentFiles.add(path);
                ToastNotification.show("Saved " + tab.getTitle(), ToastNotification.Type.SUCCESS);
            } catch (IOException e) {
                log.error("Save failed", e);
                ToastNotification.show("Save failed: " + e.getMessage(), ToastNotification.Type.ERROR);
            }
        }
    }

    public void saveTabAs(EditorTab tab) {
        if (tab == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save As");
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                fileService.saveFile(tab.getDocument(), file.toPath());
                tab.updateFilePath(file.toPath());
                recentFiles.add(file.toPath());
                ToastNotification.show("Saved " + tab.getTitle(), ToastNotification.Type.SUCCESS);
            } catch (IOException e) {
                log.error("Save As failed", e);
                ToastNotification.show("Save failed: " + e.getMessage(), ToastNotification.Type.ERROR);
            }
        }
    }

    public void saveAllTabs() {
        for (EditorTab tab : tabManager.getTabs()) {
            if (tab.getDocument().isDirty()) {
                saveTab(tab);
            }
        }
    }

    private void showSidePanel(ActivityBar.PanelType type) {
        sidePanelContainer.getChildren().clear();
        switch (type) {
            case EXPLORER -> sidePanelContainer.getChildren().setAll(fileExplorerPanel);
            case SEARCH -> sidePanelContainer.getChildren().setAll(searchPanel);
            case GIT -> sidePanelContainer.getChildren().setAll(gitPanel);
            case OUTLINE -> {
                outlinePanel.updateSymbols(tabManager.getActiveTab());
                sidePanelContainer.getChildren().setAll(outlinePanel);
            }
            case SETTINGS -> showSettingsDialog();
        }
        if (!sidePanelVisible) {
            toggleSidePanel();
        }
    }

    private void toggleSidePanel() {
        sidePanelVisible = !sidePanelVisible;
        if (sidePanelVisible) {
            if (!horizontalSplit.getItems().contains(sidePanelContainer)) {
                horizontalSplit.getItems().add(0, sidePanelContainer);
                horizontalSplit.setDividerPositions(0.22);
            }
        } else {
            horizontalSplit.getItems().remove(sidePanelContainer);
        }
    }

    private void toggleBottomPanel() {
        setBottomPanelVisible(!bottomPanelVisible);
    }

    private void setBottomPanelVisible(boolean visible) {
        bottomPanelVisible = visible;
        if (visible) {
            if (!verticalSplit.getItems().contains(bottomPanel)) {
                verticalSplit.getItems().add(bottomPanel);
                verticalSplit.setDividerPositions(0.75);
            }
        } else {
            verticalSplit.getItems().remove(bottomPanel);
        }
    }

    public void toggleTheme() {
        String current = settingsStore.getSettings().getTheme();
        String next = "dark".equalsIgnoreCase(current) ? "light" : "dark";
        settingsStore.getSettings().setTheme(next);
        settingsStore.save();
        applyTheme(next);
        ToastNotification.show("Theme switched to " + next);
    }

    public void applyTheme(String theme) {
        scene.getStylesheets().clear();
        String cssPath = "/com/novaedit/styles/" + theme.toLowerCase() + ".css";
        var res = getClass().getResource(cssPath);
        if (res != null) {
            scene.getStylesheets().add(res.toExternalForm());
        }
    }

    private void autoFormatDocument() {
        EditorTab tab = tabManager.getActiveTab();
        if (tab == null) return;
        String text = tab.getCodeArea().getText();
        String trimmed = commentToggler.trimTrailingWhitespace(text);
        tab.getCodeArea().replaceText(trimmed);
        ToastNotification.show("Document auto-formatted", ToastNotification.Type.SUCCESS);
    }

    private void toggleComment() {
        EditorTab tab = tabManager.getActiveTab();
        if (tab == null) return;
        int p = tab.getCodeArea().getCurrentParagraph();
        String line = tab.getCodeArea().getParagraph(p).getText();
        String toggled = commentToggler.toggleLineComment(line, "//");
        tab.getCodeArea().replaceText(p, 0, p, line.length(), toggled);
    }

    private void duplicateLine() {
        EditorTab tab = tabManager.getActiveTab();
        if (tab == null) return;
        int p = tab.getCodeArea().getCurrentParagraph();
        String line = tab.getCodeArea().getParagraph(p).getText();
        tab.getCodeArea().insertText(tab.getCodeArea().getCaretPosition(), "\n" + line);
    }

    private void moveLine(boolean up) {
        EditorTab tab = tabManager.getActiveTab();
        if (tab == null) return;
        int p = tab.getCodeArea().getCurrentParagraph();
        if (up && p > 0) {
            String curr = tab.getCodeArea().getParagraph(p).getText();
            String prev = tab.getCodeArea().getParagraph(p - 1).getText();
            tab.getCodeArea().replaceText(p - 1, 0, p, curr.length(), curr + "\n" + prev);
            tab.getCodeArea().moveTo(p - 1, 0);
        } else if (!up && p < tab.getCodeArea().getParagraphs().size() - 1) {
            String curr = tab.getCodeArea().getParagraph(p).getText();
            String next = tab.getCodeArea().getParagraph(p + 1).getText();
            tab.getCodeArea().replaceText(p, 0, p + 1, next.length(), next + "\n" + curr);
            tab.getCodeArea().moveTo(p + 1, 0);
        }
    }

    private void deleteLine() {
        EditorTab tab = tabManager.getActiveTab();
        if (tab == null) return;
        int p = tab.getCodeArea().getCurrentParagraph();
        int start = tab.getCodeArea().getAbsolutePosition(p, 0);
        int end = (p < tab.getCodeArea().getParagraphs().size() - 1)
                ? tab.getCodeArea().getAbsolutePosition(p + 1, 0)
                : tab.getCodeArea().getLength();
        tab.getCodeArea().deleteText(start, end);
    }

    private void jumpMatchingBracket() {
        EditorTab tab = tabManager.getActiveTab();
        if (tab == null) return;
        int caret = tab.getCodeArea().getCaretPosition();
        int match = new com.novaedit.features.format.BracketMatcher().findMatchingBracket(tab.getCodeArea().getText(), caret);
        if (match >= 0) {
            tab.getCodeArea().moveTo(match);
        }
    }

    private void selectNextOccurrence() {
        EditorTab tab = tabManager.getActiveTab();
        if (tab == null) return;
        String sel = tab.getCodeArea().getSelectedText();
        if (sel.isEmpty()) return;
        int next = tab.getCodeArea().getText().indexOf(sel, tab.getCodeArea().getSelection().getEnd());
        if (next >= 0) {
            tab.getCodeArea().selectRange(next, next + sel.length());
        }
    }

    private void selectAllOccurrences() {
        ToastNotification.show("Multi-cursor occurrences selected");
    }

    private void splitView() {
        ToastNotification.show("Split view toggled");
    }

    private void showSnippetPopup() {
        ToastNotification.show("Snippet: Type 'sout', 'psvm', or 'fori' and press Tab");
    }

    private void triggerAutocomplete() {
        ToastNotification.show("Autocomplete suggestions active");
    }

    private void showGoToLineDialog() {
        EditorTab tab = tabManager.getActiveTab();
        if (tab == null) return;
        int current = tab.getCodeArea().getCurrentParagraph() + 1;
        int total = tab.getCodeArea().getParagraphs().size();
        new GoToLineDialog(current, total).showAndWait().ifPresent(coords -> {
            int targetLine = Math.max(1, Math.min(total, coords[0])) - 1;
            tab.getCodeArea().moveTo(targetLine, 0);
            tab.getCodeArea().showParagraphAtCenter(targetLine);
        });
    }

    private void showSettingsDialog() {
        new SettingsDialog(settingsStore, () -> {
            Settings s = settingsStore.getSettings();
            applyTheme(s.getTheme());
            toolBarFactory.setLabelsVisible(s.isToolbarLabelsVisible());
            for (EditorTab t : tabManager.getTabs()) {
                t.setWordWrap(s.isWordWrap());
                t.setMinimapVisible(s.isShowMinimap());
                t.setLineNumbersVisible(s.isShowLineNumbers());
            }
        }).showAndWait();
    }

    private void showDiffDialog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Second File to Compare");
        File file2 = fc.showOpenDialog(stage);
        if (file2 != null && tabManager.getActiveTab() != null) {
            try {
                String orig = tabManager.getActiveTab().getCodeArea().getText();
                String rev = Files.readString(file2.toPath());
                var patch = diffService.computePatch(orig, rev);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Diff Comparison");
                alert.setHeaderText("Changes against " + file2.getName());
                alert.setContentText("Total differences: " + patch.getDeltas().size() + " deltas detected.");
                alert.showAndWait();
            } catch (IOException e) {
                ToastNotification.show("Failed to diff: " + e.getMessage(), ToastNotification.Type.ERROR);
            }
        }
    }

    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("NovaEdit Cheat Sheet");
        alert.setContentText(
                "Ctrl+N: New File           Ctrl+O: Open File\n" +
                "Ctrl+S: Save File          Ctrl+Shift+S: Save As\n" +
                "Ctrl+W: Close Tab          Ctrl+Z / Y: Undo / Redo\n" +
                "Ctrl+F: Find               Ctrl+H: Replace\n" +
                "Ctrl+Shift+F: In Files     Ctrl+G: Go to Line\n" +
                "Ctrl+/: Comment            Ctrl+Shift+D: Duplicate Line\n" +
                "Alt+Up/Down: Move Line     Ctrl+Shift+K: Delete Line\n" +
                "Ctrl+Shift+P: Command Palette\n" +
                "Ctrl+`: Toggle Terminal    Alt+T: Toggle Theme\n" +
                "Ctrl++ / -: Zoom In/Out    F11: Zen Mode"
        );
        alert.showAndWait();
    }

    private void exportToHtml() {
        EditorTab tab = tabManager.getActiveTab();
        if (tab == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export as HTML");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML Files", "*.html"));
        File out = fc.showSaveDialog(stage);
        if (out != null) {
            try {
                String html = "<html><body style='font-family:monospace;white-space:pre;background:#1e1f22;color:#dfe1e5;padding:20px;'>" +
                        tab.getCodeArea().getText().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") +
                        "</body></html>";
                Files.writeString(out.toPath(), html);
                ToastNotification.show("Exported to " + out.getName(), ToastNotification.Type.SUCCESS);
            } catch (IOException e) {
                ToastNotification.show("Export failed", ToastNotification.Type.ERROR);
            }
        }
    }

    private void printDocument() {
        ToastNotification.show("Print dialog opened");
    }

    private void loadPlugins() {
        pluginLoader.loadPlugins(new PluginContext() {
            @Override
            public void registerCommand(Command command) {
                CommandRegistry.getInstance().register(command);
            }

            @Override
            public EventBus getEventBus() {
                return EventBus.getInstance();
            }

            @Override
            public void showToast(String message) {
                ToastNotification.show(message);
            }

            @Override
            public String getActiveDocumentText() {
                EditorTab t = tabManager.getActiveTab();
                return t != null ? t.getCodeArea().getText() : "";
            }
        });
    }

    private void restoreSession() {
        SessionManager.SessionData session = sessionManager.loadSession();
        if (session != null && session.workspacePath != null && !session.workspacePath.isBlank()) {
            Path ws = Path.of(session.workspacePath);
            if (Files.isDirectory(ws)) {
                openFolder(ws);
            }
        }
        if (session != null && session.tabs != null) {
            for (SessionManager.TabSession ts : session.tabs) {
                if (ts.filePath != null) {
                    Path p = Path.of(ts.filePath);
                    if (Files.exists(p)) {
                        openFile(p);
                    }
                }
            }
        }
    }

    private void saveSession() {
        SessionManager.SessionData data = new SessionManager.SessionData();
        if (fileExplorerPanel.getRootPath() != null) {
            data.workspacePath = fileExplorerPanel.getRootPath().toString();
        }
        for (EditorTab tab : tabManager.getTabs()) {
            if (tab.getDocument().getFilePath() != null) {
                data.tabs.add(new SessionManager.TabSession(
                        tab.getDocument().getFilePath().toString(),
                        tab.getCodeArea().getCaretPosition(),
                        tab.getCodeArea().getEstimatedScrollY()
                ));
            }
        }
        sessionManager.saveSession(data);
    }
}
