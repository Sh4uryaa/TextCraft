package com.novaedit.ui.dialogs;

import com.novaedit.features.search.FindOptions;
import com.novaedit.features.search.FindReplaceService;
import com.novaedit.features.search.SearchResult;
import com.novaedit.ui.editor.EditorTab;
import com.novaedit.util.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Slide-in Find and Replace bar with case, word, and regex options,
 * match counter, navigation, and replace operations.
 */
public class FindReplaceBar extends VBox {

    private final TextField findField = new TextField();
    private final TextField replaceField = new TextField();

    private final ToggleButton matchCaseBtn = new ToggleButton("Aa");
    private final ToggleButton wholeWordBtn = new ToggleButton("\\b");
    private final ToggleButton regexBtn = new ToggleButton(".*");

    private final Button prevBtn = new Button();
    private final Button nextBtn = new Button();
    private final Button replaceBtn = new Button("Replace");
    private final Button replaceAllBtn = new Button("Replace All");
    private final Button closeBtn = new Button();

    private final Label matchCounterLabel = new Label("No matches");

    private final FindReplaceService searchService = new FindReplaceService();
    private EditorTab activeTab;
    private Runnable onClose;

    public FindReplaceBar() {
        initUI();
    }

    private void initUI() {
        getStyleClass().add("find-replace-bar");
        setSpacing(6);

        // --- Find Row ---
        HBox findRow = new HBox(6);
        findRow.setAlignment(Pos.CENTER_LEFT);

        findField.setPromptText("Find");
        HBox.setHgrow(findField, Priority.ALWAYS);

        matchCaseBtn.getStyleClass().add("tool-button");
        matchCaseBtn.setTooltip(new Tooltip("Match Case"));
        matchCaseBtn.setOnAction(e -> updateMatches());

        wholeWordBtn.getStyleClass().add("tool-button");
        wholeWordBtn.setTooltip(new Tooltip("Match Whole Word"));
        wholeWordBtn.setOnAction(e -> updateMatches());

        regexBtn.getStyleClass().add("tool-button");
        regexBtn.setTooltip(new Tooltip("Use Regular Expression"));
        regexBtn.setOnAction(e -> updateMatches());

        prevBtn.setGraphic(Icons.get("move-up", 13));
        prevBtn.getStyleClass().add("tool-button");
        prevBtn.setTooltip(new Tooltip("Previous Match (Shift+Enter)"));
        prevBtn.setOnAction(e -> findPrevious());

        nextBtn.setGraphic(Icons.get("move-down", 13));
        nextBtn.getStyleClass().add("tool-button");
        nextBtn.setTooltip(new Tooltip("Next Match (Enter)"));
        nextBtn.setOnAction(e -> findNext());

        closeBtn.setGraphic(Icons.get("close", 12));
        closeBtn.getStyleClass().add("tool-button");
        closeBtn.setTooltip(new Tooltip("Close (Esc)"));
        closeBtn.setOnAction(e -> {
            setVisible(false);
            if (onClose != null) onClose.run();
        });

        matchCounterLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-min-width: 80px;");

        findRow.getChildren().addAll(findField, matchCaseBtn, wholeWordBtn, regexBtn, matchCounterLabel, prevBtn, nextBtn, closeBtn);

        // --- Replace Row ---
        HBox replaceRow = new HBox(6);
        replaceRow.setAlignment(Pos.CENTER_LEFT);

        replaceField.setPromptText("Replace");
        HBox.setHgrow(replaceField, Priority.ALWAYS);

        replaceBtn.getStyleClass().add("tool-button");
        replaceBtn.setOnAction(e -> replaceCurrent());

        replaceAllBtn.getStyleClass().add("tool-button");
        replaceAllBtn.setOnAction(e -> replaceAll());

        replaceRow.getChildren().addAll(replaceField, replaceBtn, replaceAllBtn);

        getChildren().addAll(findRow, replaceRow);

        // Keystroke listeners
        findField.textProperty().addListener((obs, oldVal, newVal) -> updateMatches());
        findField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (e.isShiftDown()) findPrevious();
                else findNext();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                setVisible(false);
            }
        });
    }

    public void showForTab(EditorTab tab, boolean showReplace) {
        this.activeTab = tab;
        setVisible(true);
        findField.requestFocus();
        if (tab != null && !tab.getCodeArea().getSelectedText().isEmpty()) {
            findField.setText(tab.getCodeArea().getSelectedText());
        }
        updateMatches();
    }

    private FindOptions getOptions() {
        return new FindOptions(
                findField.getText(),
                matchCaseBtn.isSelected(),
                wholeWordBtn.isSelected(),
                regexBtn.isSelected()
        );
    }

    private void updateMatches() {
        if (activeTab == null || findField.getText().isEmpty()) {
            matchCounterLabel.setText("No matches");
            return;
        }

        String text = activeTab.getCodeArea().getText();
        List<SearchResult> results = searchService.findAll(text, getOptions());
        if (results.isEmpty()) {
            matchCounterLabel.setText("No matches");
        } else {
            int caret = activeTab.getCodeArea().getCaretPosition();
            int currentMatchIndex = 0;
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).start() >= caret) {
                    currentMatchIndex = i + 1;
                    break;
                }
            }
            if (currentMatchIndex == 0) currentMatchIndex = 1;
            matchCounterLabel.setText(currentMatchIndex + " of " + results.size());
        }
    }

    public void findNext() {
        if (activeTab == null) return;
        String text = activeTab.getCodeArea().getText();
        SearchResult res = searchService.findNext(text, activeTab.getCodeArea().getCaretPosition(), getOptions());
        if (res != null) {
            activeTab.getCodeArea().selectRange(res.start(), res.end());
            activeTab.getCodeArea().showParagraphAtCenter(activeTab.getCodeArea().getCurrentParagraph());
            updateMatches();
        }
    }

    public void findPrevious() {
        if (activeTab == null) return;
        String text = activeTab.getCodeArea().getText();
        SearchResult res = searchService.findPrevious(text, activeTab.getCodeArea().getSelection().getStart(), getOptions());
        if (res != null) {
            activeTab.getCodeArea().selectRange(res.start(), res.end());
            activeTab.getCodeArea().showParagraphAtCenter(activeTab.getCodeArea().getCurrentParagraph());
            updateMatches();
        }
    }

    public void replaceCurrent() {
        if (activeTab == null) return;
        String selected = activeTab.getCodeArea().getSelectedText();
        if (!selected.isEmpty()) {
            activeTab.getCodeArea().replaceSelection(replaceField.getText());
            findNext();
        } else {
            findNext();
        }
    }

    public void replaceAll() {
        if (activeTab == null) return;
        String text = activeTab.getCodeArea().getText();
        String replaced = searchService.replaceAll(text, getOptions(), replaceField.getText());
        activeTab.getCodeArea().replaceText(replaced);
        updateMatches();
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }
}
