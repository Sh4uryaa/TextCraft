package com.novaedit.ui.shell;

import com.novaedit.core.document.Document;
import com.novaedit.ui.editor.EditorTab;
import com.novaedit.util.Icons;
import com.novaedit.workspace.RecentFiles;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages editor tabs with custom rounded tab headers, dirty-dot indicators,
 * close confirmations, and the modern Welcome Screen when empty.
 */
public class TabManager extends BorderPane {
    private final List<EditorTab> tabs = new ArrayList<>();
    private final List<EditorTab> closedHistory = new ArrayList<>();
    private EditorTab activeTab = null;

    private final HBox tabBar = new HBox(2);
    private final StackPane contentArea = new StackPane();
    private final RecentFiles recentFiles;

    private Consumer<EditorTab> onActiveTabChanged;
    private Runnable onNewFileRequested;
    private Runnable onOpenFileRequested;
    private Runnable onOpenFolderRequested;
    private Consumer<Path> onOpenPathRequested;
    private Consumer<EditorTab> onSaveRequested;

    public TabManager(RecentFiles recentFiles) {
        this.recentFiles = recentFiles;
        initUI();
    }

    private void initUI() {
        // Tab Header Area
        HBox headerArea = new HBox(tabBar);
        headerArea.getStyleClass().add("tab-header-area");
        tabBar.getStyleClass().add("tab-bar");
        HBox.setHgrow(tabBar, Priority.ALWAYS);

        // [+] Add Tab button
        Button addTabBtn = new Button();
        addTabBtn.setGraphic(Icons.get("file-plus", 14));
        addTabBtn.getStyleClass().add("tool-button");
        addTabBtn.setTooltip(new Tooltip("New File (Ctrl+N)"));
        addTabBtn.setOnAction(e -> {
            if (onNewFileRequested != null) onNewFileRequested.run();
        });
        headerArea.getChildren().add(addTabBtn);

        setTop(headerArea);
        setCenter(contentArea);

        showWelcomeScreen();
    }

    public void addTab(EditorTab tab) {
        tabs.add(tab);
        rebuildTabBar();
        selectTab(tab);
    }

    public void selectTab(EditorTab tab) {
        if (tab == null || !tabs.contains(tab)) return;
        activeTab = tab;
        contentArea.getChildren().setAll(tab);
        rebuildTabBar();
        if (onActiveTabChanged != null) {
            onActiveTabChanged.accept(tab);
        }
    }

    public void closeTab(EditorTab tab) {
        if (tab == null || !tabs.contains(tab)) return;

        if (tab.getDocument().isDirty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("Do you want to save changes to " + tab.getTitle() + "?");
            alert.setContentText("Your changes will be lost if you don't save them.");

            ButtonType saveBtn = new ButtonType("Save");
            ButtonType dontSaveBtn = new ButtonType("Don't Save");
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(saveBtn, dontSaveBtn, cancelBtn);

            var result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == saveBtn) {
                    if (onSaveRequested != null) onSaveRequested.accept(tab);
                } else if (result.get() == cancelBtn) {
                    return; // Cancel close
                }
            }
        }

        int index = tabs.indexOf(tab);
        tabs.remove(tab);
        closedHistory.add(tab);

        if (tabs.isEmpty()) {
            activeTab = null;
            showWelcomeScreen();
            if (onActiveTabChanged != null) onActiveTabChanged.accept(null);
        } else {
            int newIndex = Math.min(index, tabs.size() - 1);
            selectTab(tabs.get(newIndex));
        }
        rebuildTabBar();
    }

    public void reopenClosedTab() {
        if (!closedHistory.isEmpty()) {
            EditorTab tab = closedHistory.remove(closedHistory.size() - 1);
            addTab(tab);
        }
    }

    private void rebuildTabBar() {
        tabBar.getChildren().clear();
        for (EditorTab tab : tabs) {
            HBox tabHeader = createTabHeader(tab);
            tabBar.getChildren().add(tabHeader);
        }
    }

    private HBox createTabHeader(EditorTab tab) {
        HBox header = new HBox(8);
        header.getStyleClass().add("custom-tab");
        if (tab == activeTab) {
            header.getStyleClass().add("active");
        }

        // Icon
        Node icon = Icons.get("file-plus", 14);

        // Title
        Label titleLabel = new Label(tab.getTitle());
        titleLabel.getStyleClass().add("tab-title");

        // Dirty indicator
        Circle dirtyDot = new Circle(4);
        dirtyDot.getStyleClass().add("dirty-indicator");
        dirtyDot.setVisible(tab.getDocument().isDirty());

        tab.getDocument().addListener(new com.novaedit.core.document.DocumentListener() {
            @Override
            public void onDirtyStateChanged(boolean isDirty) {
                Platform.runLater(() -> dirtyDot.setVisible(isDirty));
            }
        });

        // Close button
        Button closeBtn = new Button();
        closeBtn.setGraphic(Icons.get("close", 12));
        closeBtn.getStyleClass().add("tab-close-btn");
        closeBtn.setOnAction(e -> {
            e.consume();
            closeTab(tab);
        });

        header.getChildren().addAll(icon, titleLabel, dirtyDot, closeBtn);
        header.setOnMouseClicked(e -> selectTab(tab));

        return header;
    }

    private void showWelcomeScreen() {
        VBox welcome = new VBox(20);
        welcome.setAlignment(Pos.CENTER);
        welcome.setStyle("-fx-background-color: -color-bg; -fx-padding: 40;");

        // Logo & Title
        Node logo = Icons.get("brackets", 48);
        Label title = new Label("NovaEdit");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
        Label subtitle = new Label("Professional Text & Code Editor");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-text-muted;");

        // Action Buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);

        Button newFileBtn = createWelcomeButton("New File", "file-plus", e -> {
            if (onNewFileRequested != null) onNewFileRequested.run();
        });
        Button openFileBtn = createWelcomeButton("Open File", "folder", e -> {
            if (onOpenFileRequested != null) onOpenFileRequested.run();
        });
        Button openFolderBtn = createWelcomeButton("Open Folder", "folder", e -> {
            if (onOpenFolderRequested != null) onOpenFolderRequested.run();
        });

        actions.getChildren().addAll(newFileBtn, openFileBtn, openFolderBtn);

        // Recent Files Box
        VBox recentBox = new VBox(8);
        recentBox.setAlignment(Pos.CENTER_LEFT);
        recentBox.setMaxWidth(400);

        Label recentHeader = new Label("Recent Files");
        recentHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-font-size: 12px;");
        recentBox.getChildren().add(recentHeader);

        List<String> recents = (recentFiles != null) ? recentFiles.getRecentFiles() : Collections.emptyList();
        if (recents.isEmpty()) {
            Label noRecents = new Label("No recent files");
            noRecents.setStyle("-fx-text-fill: -color-text-muted; -fx-font-style: italic;");
            recentBox.getChildren().add(noRecents);
        } else {
            for (String pathStr : recents) {
                Path p = Path.of(pathStr);
                Hyperlink link = new Hyperlink(p.getFileName().toString() + " (" + p.getParent() + ")");
                link.setStyle("-fx-text-fill: -color-accent; -fx-font-size: 12px;");
                link.setOnAction(e -> {
                    if (onOpenPathRequested != null) onOpenPathRequested.accept(p);
                });
                recentBox.getChildren().add(link);
            }
        }

        // Shortcuts hint
        Label tips = new Label("Quick tips: Ctrl+Shift+P for Command Palette  •  Ctrl+N for New File  •  Ctrl+O for Open");
        tips.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");

        welcome.getChildren().addAll(logo, title, subtitle, actions, new Separator(), recentBox, tips);
        contentArea.getChildren().setAll(welcome);
    }

    private Button createWelcomeButton(String text, String iconName, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text, Icons.get(iconName, 16));
        btn.setStyle("-fx-background-color: -color-panel; -fx-text-fill: -color-text; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-border-color: -color-border; -fx-border-radius: 6px; -fx-cursor: hand;");
        btn.setOnAction(handler);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: -color-hover; -fx-text-fill: -color-text; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-border-color: -color-accent; -fx-border-radius: 6px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: -color-panel; -fx-text-fill: -color-text; -fx-padding: 8 16; -fx-background-radius: 6px; -fx-border-color: -color-border; -fx-border-radius: 6px; -fx-cursor: hand;"));
        return btn;
    }

    public EditorTab getActiveTab() {
        return activeTab;
    }

    public List<EditorTab> getTabs() {
        return Collections.unmodifiableList(tabs);
    }

    public void setOnActiveTabChanged(Consumer<EditorTab> onActiveTabChanged) {
        this.onActiveTabChanged = onActiveTabChanged;
    }

    public void setOnNewFileRequested(Runnable onNewFileRequested) {
        this.onNewFileRequested = onNewFileRequested;
    }

    public void setOnOpenFileRequested(Runnable onOpenFileRequested) {
        this.onOpenFileRequested = onOpenFileRequested;
    }

    public void setOnOpenFolderRequested(Runnable onOpenFolderRequested) {
        this.onOpenFolderRequested = onOpenFolderRequested;
    }

    public void setOnOpenPathRequested(Consumer<Path> onOpenPathRequested) {
        this.onOpenPathRequested = onOpenPathRequested;
    }

    public void setOnSaveRequested(Consumer<EditorTab> onSaveRequested) {
        this.onSaveRequested = onSaveRequested;
    }
}
