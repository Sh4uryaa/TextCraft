package com.novaedit.ui.shell;

import com.novaedit.core.document.Document;
import com.novaedit.core.document.EncodingInfo;
import com.novaedit.core.document.LineEnding;
import com.novaedit.syntax.TokenizerRegistry;
import com.novaedit.ui.editor.EditorTab;
import com.novaedit.util.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * Status bar presenting line/col, selection length, indentation, encoding,
 * line ending, language mode, Git branch, zoom, and live theme toggle.
 */
public class StatusBar extends HBox {
    private final Label positionLabel = new Label("Ln 1, Col 1");
    private final Label selectionLabel = new Label("Sel 0");
    private final Label indentLabel = new Label("Spaces: 4");
    private final Label encodingLabel = new Label("UTF-8");
    private final Label lineEndingLabel = new Label("LF");
    private final Label languageLabel = new Label("Plain Text");
    private final Label gitLabel = new Label("Git: -");
    private final Label zoomLabel = new Label("100%");
    private final Button themeToggleBtn = new Button();

    private EditorTab currentTab;
    private Runnable onThemeToggle;
    private Consumer<Integer> onIndentChange;

    public StatusBar() {
        initUI();
    }

    private void initUI() {
        getStyleClass().add("status-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);

        positionLabel.getStyleClass().add("status-item");
        selectionLabel.getStyleClass().add("status-item");
        indentLabel.getStyleClass().add("status-item");
        encodingLabel.getStyleClass().add("status-item");
        lineEndingLabel.getStyleClass().add("status-item");
        languageLabel.getStyleClass().add("status-item");
        gitLabel.getStyleClass().add("status-item");
        zoomLabel.getStyleClass().add("status-item");

        gitLabel.setGraphic(Icons.get("git", 12));

        // Theme toggle button
        themeToggleBtn.setGraphic(Icons.get("theme", 13));
        themeToggleBtn.getStyleClass().add("tool-button");
        themeToggleBtn.setTooltip(new Tooltip("Toggle Theme (Dark / Light)"));
        themeToggleBtn.setOnAction(e -> {
            if (onThemeToggle != null) onThemeToggle.run();
        });

        // Clickable encoding menu
        ContextMenu encodingMenu = new ContextMenu();
        for (EncodingInfo enc : List.of(EncodingInfo.UTF_8, EncodingInfo.UTF_16LE, EncodingInfo.ISO_8859_1, EncodingInfo.WINDOWS_1252)) {
            MenuItem item = new MenuItem(enc.name());
            item.setOnAction(e -> {
                if (currentTab != null) {
                    currentTab.getDocument().setEncodingInfo(enc);
                    updateTab(currentTab);
                    ToastNotification.show("Encoding set to " + enc.name());
                }
            });
            encodingMenu.getItems().add(item);
        }
        encodingLabel.setOnMouseClicked(e -> encodingMenu.show(encodingLabel, e.getScreenX(), e.getScreenY()));

        // Clickable line ending menu
        ContextMenu lineEndingMenu = new ContextMenu();
        for (LineEnding ending : LineEnding.values()) {
            MenuItem item = new MenuItem(ending.getDisplayName());
            item.setOnAction(e -> {
                if (currentTab != null) {
                    currentTab.getDocument().setLineEnding(ending);
                    updateTab(currentTab);
                    ToastNotification.show("Line endings set to " + ending.getDisplayName());
                }
            });
            lineEndingMenu.getItems().add(item);
        }
        lineEndingLabel.setOnMouseClicked(e -> lineEndingMenu.show(lineEndingLabel, e.getScreenX(), e.getScreenY()));

        // Clickable language menu
        ContextMenu langMenu = new ContextMenu();
        TokenizerRegistry.getAll().forEach((id, tokenizer) -> {
            MenuItem item = new MenuItem(tokenizer.getDisplayName());
            item.setOnAction(e -> {
                if (currentTab != null) {
                    currentTab.setTokenizer(tokenizer);
                    updateTab(currentTab);
                    ToastNotification.show("Language set to " + tokenizer.getDisplayName());
                }
            });
            langMenu.getItems().add(item);
        });
        languageLabel.setOnMouseClicked(e -> langMenu.show(languageLabel, e.getScreenX(), e.getScreenY()));

        // Indent change
        indentLabel.setOnMouseClicked(e -> {
            TextInputDialog dialog = new TextInputDialog("4");
            dialog.setTitle("Indentation");
            dialog.setHeaderText("Set Tab Size (Spaces)");
            dialog.setContentText("Spaces per tab:");
            dialog.showAndWait().ifPresent(val -> {
                try {
                    int size = Integer.parseInt(val.trim());
                    indentLabel.setText("Spaces: " + size);
                    if (onIndentChange != null) onIndentChange.accept(size);
                } catch (NumberFormatException ignored) {}
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                positionLabel, selectionLabel,
                spacer,
                gitLabel, indentLabel, encodingLabel, lineEndingLabel, languageLabel, zoomLabel, themeToggleBtn
        );
    }

    public void updateTab(EditorTab tab) {
        this.currentTab = tab;
        if (tab == null) {
            positionLabel.setText("Ln 1, Col 1");
            selectionLabel.setText("Sel 0");
            encodingLabel.setText("UTF-8");
            lineEndingLabel.setText("LF");
            languageLabel.setText("Plain Text");
            zoomLabel.setText("100%");
            return;
        }

        Document doc = tab.getDocument();
        encodingLabel.setText(doc.getEncodingInfo().name());
        lineEndingLabel.setText(doc.getLineEnding().getDisplayName());
        languageLabel.setText(tab.getTokenizer().getDisplayName());

        int zoomPct = (int) Math.round((tab.getFontSize() / 14.0) * 100);
        zoomLabel.setText(zoomPct + "%");

        // Cursor position listener
        tab.getCodeArea().caretPositionProperty().addListener((obs, oldVal, newVal) -> {
            int pos = newVal.intValue();
            int line = tab.getCodeArea().getCurrentParagraph() + 1;
            int col = tab.getCodeArea().getCaretColumn() + 1;
            positionLabel.setText("Ln " + line + ", Col " + col);

            int selLen = tab.getCodeArea().getSelection().getLength();
            selectionLabel.setText("Sel " + selLen);
        });
    }

    public void setGitBranch(String branch) {
        gitLabel.setText(branch != null && !branch.isBlank() ? "Git: " + branch : "Git: -");
    }

    public void setOnThemeToggle(Runnable onThemeToggle) {
        this.onThemeToggle = onThemeToggle;
    }

    public void setOnIndentChange(Consumer<Integer> onIndentChange) {
        this.onIndentChange = onIndentChange;
    }
}
