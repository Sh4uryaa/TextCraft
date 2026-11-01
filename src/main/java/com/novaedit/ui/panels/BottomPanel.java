package com.novaedit.ui.panels;

import com.novaedit.util.Icons;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Bottom panel hosting Output logs, an Integrated Terminal, and Problems view.
 */
public class BottomPanel extends BorderPane {

    private final TabPane tabPane = new TabPane();
    private final TextArea outputArea = new TextArea();
    private final TextArea terminalOutputArea = new TextArea();
    private final TextField terminalInput = new TextField();
    private final ListView<String> problemsList = new ListView<>();

    private Path workingDirectory = Path.of(System.getProperty("user.dir"));
    private Runnable onCloseRequested;

    public BottomPanel() {
        initUI();
    }

    private void initUI() {
        getStyleClass().add("bottom-panel");
        setPrefHeight(180);

        // Header with close button
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button();
        closeBtn.setGraphic(Icons.get("close", 12));
        closeBtn.getStyleClass().add("tool-button");
        closeBtn.setTooltip(new Tooltip("Hide Panel (Ctrl+`)"));
        closeBtn.setOnAction(e -> {
            if (onCloseRequested != null) onCloseRequested.run();
        });

        // Tab 1: Output
        Tab outputTab = new Tab("Output");
        outputTab.setClosable(false);
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
        outputArea.setText("NovaEdit Console Output initialized.\nReady.\n");
        outputTab.setContent(outputArea);

        // Tab 2: Terminal
        Tab terminalTab = new Tab("Terminal");
        terminalTab.setClosable(false);
        VBox terminalBox = new VBox(4);
        terminalOutputArea.setEditable(false);
        terminalOutputArea.setStyle("-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
        VBox.setVgrow(terminalOutputArea, Priority.ALWAYS);

        terminalInput.setPromptText("Enter command (e.g. dir, git status, mvn --version)...");
        terminalInput.setStyle("-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 12px;");
        terminalInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                runTerminalCommand(terminalInput.getText());
                terminalInput.clear();
            }
        });

        terminalBox.getChildren().addAll(terminalOutputArea, terminalInput);
        terminalTab.setContent(terminalBox);

        // Tab 3: Problems
        Tab problemsTab = new Tab("Problems");
        problemsTab.setClosable(false);
        problemsList.getItems().add("No problems detected in the current workspace.");
        problemsTab.setContent(problemsList);

        tabPane.getTabs().addAll(outputTab, terminalTab, problemsTab);

        setCenter(tabPane);
        setRight(closeBtn);
    }

    public void appendOutput(String line) {
        Platform.runLater(() -> {
            outputArea.appendText(line + "\n");
        });
    }

    public void runTerminalCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) return;
        terminalOutputArea.appendText("> " + cmd + "\n");

        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            try {
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
                ProcessBuilder pb = isWindows
                        ? new ProcessBuilder("cmd.exe", "/c", cmd)
                        : new ProcessBuilder("sh", "-c", cmd);

                pb.directory(workingDirectory.toFile());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String finalLine = line;
                        Platform.runLater(() -> terminalOutputArea.appendText(finalLine + "\n"));
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                Platform.runLater(() -> terminalOutputArea.appendText("Error: " + e.getMessage() + "\n"));
            }
        });
    }

    public void setWorkingDirectory(Path workingDirectory) {
        if (workingDirectory != null) {
            this.workingDirectory = workingDirectory;
        }
    }

    public void setOnCloseRequested(Runnable onCloseRequested) {
        this.onCloseRequested = onCloseRequested;
    }
}
