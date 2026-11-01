package com.novaedit.ui.dialogs;

import com.novaedit.commands.Command;
import com.novaedit.commands.CommandRegistry;
import com.novaedit.util.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Centered fuzzy-search Command Palette overlay listing all editor commands and shortcuts.
 */
public class CommandPalette extends StackPane {

    private final TextField inputField = new TextField();
    private final ListView<Command> commandListView = new ListView<>();
    private final VBox box = new VBox(8);

    public CommandPalette() {
        initUI();
    }

    private void initUI() {
        getStyleClass().add("palette-overlay");
        setAlignment(Pos.TOP_CENTER);
        setVisible(false);

        box.getStyleClass().add("palette-box");
        box.setMaxWidth(600);
        box.setStyle("-fx-margin-top: 50;");

        inputField.getStyleClass().add("palette-input");
        inputField.setPromptText("Type a command or search...");

        commandListView.getStyleClass().add("tree-view");
        commandListView.setPrefHeight(300);
        commandListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Command item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = new HBox(8);
                    cell.setAlignment(Pos.CENTER_LEFT);

                    Label title = new Label(item.category() + ": " + item.title());
                    title.setStyle("-fx-text-fill: -color-text; -fx-font-size: 13px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    String shortcut = CommandRegistry.getInstance().getEffectiveShortcut(item.id());
                    Label scLabel = new Label(shortcut != null ? shortcut : "");
                    scLabel.getStyleClass().add("palette-shortcut");

                    cell.getChildren().addAll(Icons.get(item.iconLiteral() != null ? item.iconLiteral() : "palette", 14), title, spacer, scLabel);
                    setGraphic(cell);
                    setText(null);
                }
            }
        });

        // Search filtering
        inputField.textProperty().addListener((obs, oldVal, newVal) -> filterCommands(newVal));

        // Keyboard navigation
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                commandListView.getSelectionModel().selectNext();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                commandListView.getSelectionModel().selectPrevious();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                executeSelected();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            }
        });

        commandListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                executeSelected();
            }
        });

        box.getChildren().addAll(inputField, commandListView);
        getChildren().add(box);

        // Click outside box to dismiss
        setOnMouseClicked(e -> {
            if (e.getTarget() == this) {
                hide();
            }
        });
    }

    public void show() {
        setVisible(true);
        inputField.clear();
        filterCommands("");
        inputField.requestFocus();
    }

    public void hide() {
        setVisible(false);
    }

    private void filterCommands(String query) {
        List<Command> matches = CommandRegistry.getInstance().search(query);
        commandListView.getItems().setAll(matches);
        if (!matches.isEmpty()) {
            commandListView.getSelectionModel().selectFirst();
        }
    }

    private void executeSelected() {
        Command selected = commandListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            hide();
            selected.execute();
        }
    }
}
