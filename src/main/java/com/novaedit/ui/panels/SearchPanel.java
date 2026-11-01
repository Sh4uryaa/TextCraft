package com.novaedit.ui.panels;

import com.novaedit.features.search.FindOptions;
import com.novaedit.features.search.ProjectSearchService;
import com.novaedit.features.search.SearchResult;
import com.novaedit.util.Icons;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Project-wide search panel with regex, glob filters, and grouped file results.
 */
public class SearchPanel extends BorderPane {

    private final TextField searchField = new TextField();
    private final TextField replaceField = new TextField();
    private final TextField includeField = new TextField();
    private final TextField excludeField = new TextField();

    private final ToggleButton matchCaseBtn = new ToggleButton("Aa");
    private final ToggleButton wholeWordBtn = new ToggleButton("\\b");
    private final ToggleButton regexBtn = new ToggleButton(".*");

    private final Button searchBtn = new Button("Search");
    private final Label statusLabel = new Label("Enter search query");
    private final TreeView<Object> resultsTree = new TreeView<>();

    private final ProjectSearchService projectSearchService = new ProjectSearchService();
    private Path projectRoot;
    private BiConsumer<Path, Integer> onJumpRequested;

    public SearchPanel() {
        initUI();
    }

    private void initUI() {
        getStyleClass().add("side-panel");

        VBox topBox = new VBox(8);
        topBox.setStyle("-fx-padding: 10;");

        Label header = new Label("SEARCH IN FILES");
        header.getStyleClass().add("side-panel-title");

        searchField.setPromptText("Search");
        replaceField.setPromptText("Replace (optional)");
        includeField.setPromptText("files to include (e.g. *.java)");
        excludeField.setPromptText("files to exclude");

        HBox optionsBox = new HBox(4);
        optionsBox.setAlignment(Pos.CENTER_LEFT);

        matchCaseBtn.getStyleClass().add("tool-button");
        matchCaseBtn.setTooltip(new Tooltip("Match Case"));
        wholeWordBtn.getStyleClass().add("tool-button");
        wholeWordBtn.setTooltip(new Tooltip("Match Whole Word"));
        regexBtn.getStyleClass().add("tool-button");
        regexBtn.setTooltip(new Tooltip("Use Regular Expression"));

        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox searchRow = new HBox(4, searchField, matchCaseBtn, wholeWordBtn, regexBtn);

        searchBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-background-radius: 4px; -fx-cursor: hand;");
        searchBtn.setOnAction(e -> executeSearch());
        searchField.setOnAction(e -> executeSearch());

        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        topBox.getChildren().addAll(header, searchRow, replaceField, includeField, excludeField, searchBtn, statusLabel);
        setTop(topBox);

        // Results Tree
        resultsTree.getStyleClass().add("tree-view");
        resultsTree.setShowRoot(false);
        resultsTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof ProjectSearchService.FileMatches fm) {
                    setText(fm.path().getFileName().toString() + " (" + fm.matches().size() + ")");
                    setGraphic(Icons.get("file-plus", 13));
                } else if (item instanceof SearchResult sr) {
                    setText("Line " + (sr.line() + 1) + ": " + sr.text());
                    setGraphic(Icons.get("search", 11));
                }
            }
        });

        resultsTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<Object> selected = resultsTree.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() instanceof SearchResult sr) {
                    TreeItem<Object> parent = selected.getParent();
                    if (parent != null && parent.getValue() instanceof ProjectSearchService.FileMatches fm) {
                        if (onJumpRequested != null) {
                            onJumpRequested.accept(fm.path(), sr.start());
                        }
                    }
                }
            }
        });

        setCenter(resultsTree);
    }

    public void setProjectRoot(Path root) {
        this.projectRoot = root;
    }

    public void executeSearch() {
        String query = searchField.getText();
        if (query == null || query.isBlank() || projectRoot == null) {
            statusLabel.setText("No search term or open folder");
            return;
        }

        statusLabel.setText("Searching...");
        FindOptions options = new FindOptions(
                query,
                matchCaseBtn.isSelected(),
                wholeWordBtn.isSelected(),
                regexBtn.isSelected()
        );

        String inc = includeField.getText();
        String exc = excludeField.getText();

        projectSearchService.searchProject(projectRoot, options, inc, exc).thenAccept(fileMatches -> {
            Platform.runLater(() -> {
                TreeItem<Object> rootItem = new TreeItem<>("root");
                int totalMatches = 0;
                for (ProjectSearchService.FileMatches fm : fileMatches) {
                    TreeItem<Object> fileItem = new TreeItem<>(fm);
                    for (SearchResult match : fm.matches()) {
                        fileItem.getChildren().add(new TreeItem<>(match));
                        totalMatches++;
                    }
                    fileItem.setExpanded(true);
                    rootItem.getChildren().add(fileItem);
                }
                resultsTree.setRoot(rootItem);
                statusLabel.setText(totalMatches + " matches in " + fileMatches.size() + " files");
            });
        });
    }

    public void setOnJumpRequested(BiConsumer<Path, Integer> onJumpRequested) {
        this.onJumpRequested = onJumpRequested;
    }
}
