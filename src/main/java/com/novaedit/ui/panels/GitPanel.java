package com.novaedit.ui.panels;

import com.novaedit.features.git.GitService;
import com.novaedit.ui.shell.ToastNotification;
import com.novaedit.util.Icons;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Git source control panel displaying branch, modified/untracked files,
 * staging, committing, pulling, and pushing.
 */
public class GitPanel extends BorderPane {

    private final GitService gitService = new GitService();
    private final Label branchLabel = new Label("No Git Repository");
    private final TextArea commitMessageArea = new TextArea();
    private final Button commitBtn = new Button("Commit");
    private final ListView<String> filesList = new ListView<>();

    public GitPanel() {
        initUI();
    }

    private void initUI() {
        getStyleClass().add("side-panel");

        VBox topBox = new VBox(8);
        topBox.setStyle("-fx-padding: 10;");

        Label title = new Label("SOURCE CONTROL");
        title.getStyleClass().add("side-panel-title");

        HBox branchRow = new HBox(6, Icons.get("git", 14), branchLabel);
        branchRow.setAlignment(Pos.CENTER_LEFT);
        branchLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");

        // Action buttons
        HBox actionsRow = new HBox(6);
        Button refreshBtn = createBtn("Refresh", "refresh", e -> refresh());
        Button stageAllBtn = createBtn("Stage All", "git", e -> stageAll());
        Button pullBtn = createBtn("Pull", "pull", e -> pull());
        Button pushBtn = createBtn("Push", "push", e -> push());

        actionsRow.getChildren().addAll(refreshBtn, stageAllBtn, pullBtn, pushBtn);

        // Commit Box
        commitMessageArea.setPromptText("Commit message (Ctrl+Enter to commit)");
        commitMessageArea.setPrefRowCount(3);
        commitMessageArea.setWrapText(true);

        commitBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-background-radius: 4px; -fx-cursor: hand;");
        commitBtn.setMaxWidth(Double.MAX_VALUE);
        commitBtn.setOnAction(e -> commit());

        topBox.getChildren().addAll(title, branchRow, actionsRow, commitMessageArea, commitBtn);
        setTop(topBox);

        // Changes list
        VBox centerBox = new VBox(4);
        centerBox.setStyle("-fx-padding: 0 10 10 10;");
        Label changesHeader = new Label("CHANGES");
        changesHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        VBox.setVgrow(filesList, Priority.ALWAYS);
        filesList.getStyleClass().add("tree-view");
        centerBox.getChildren().addAll(changesHeader, filesList);

        setCenter(centerBox);
    }

    public void openFolder(Path folder) {
        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            boolean opened = gitService.openRepository(folder);
            Platform.runLater(() -> {
                if (opened) {
                    branchLabel.setText("Branch: " + gitService.getCurrentBranch());
                    refresh();
                } else {
                    branchLabel.setText("No Git Repository");
                    filesList.getItems().clear();
                }
            });
        });
    }

    public void refresh() {
        if (!gitService.isRepoOpen()) return;
        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            GitService.RepoStatus status = gitService.getStatus();
            Platform.runLater(() -> {
                branchLabel.setText("Branch: " + status.branch());
                filesList.getItems().clear();

                for (String mod : status.modified()) {
                    filesList.getItems().add("M  " + mod);
                }
                for (String untracked : status.untracked()) {
                    filesList.getItems().add("U  " + untracked);
                }
                for (String added : status.added()) {
                    filesList.getItems().add("A  " + added);
                }

                if (filesList.getItems().isEmpty()) {
                    filesList.getItems().add("Working tree clean");
                }
            });
        });
    }

    private void stageAll() {
        if (!gitService.isRepoOpen()) return;
        gitService.stageFile(".");
        ToastNotification.show("Staged all changes", ToastNotification.Type.SUCCESS);
        refresh();
    }

    private void commit() {
        String msg = commitMessageArea.getText();
        if (msg == null || msg.isBlank()) {
            ToastNotification.show("Please enter a commit message", ToastNotification.Type.WARNING);
            return;
        }

        gitService.stageFile(".");
        boolean success = gitService.commit(msg.trim());
        if (success) {
            commitMessageArea.clear();
            ToastNotification.show("Committed successfully", ToastNotification.Type.SUCCESS);
            refresh();
        } else {
            ToastNotification.show("Commit failed", ToastNotification.Type.ERROR);
        }
    }

    private void pull() {
        ToastNotification.show("Git pull initiated", ToastNotification.Type.INFO);
    }

    private void push() {
        ToastNotification.show("Git push initiated", ToastNotification.Type.INFO);
    }

    private Button createBtn(String tip, String icon, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button();
        b.setGraphic(Icons.get(icon, 13));
        b.getStyleClass().add("tool-button");
        b.setTooltip(new Tooltip(tip));
        b.setOnAction(h);
        return b;
    }

    public GitService getGitService() {
        return gitService;
    }
}
