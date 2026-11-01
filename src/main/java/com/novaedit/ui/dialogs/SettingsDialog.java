package com.novaedit.ui.dialogs;

import com.novaedit.commands.Command;
import com.novaedit.commands.CommandRegistry;
import com.novaedit.settings.Settings;
import com.novaedit.settings.SettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Preferences and Settings dialog with Keymap editor.
 */
public class SettingsDialog extends Dialog<Boolean> {

    private final SettingsStore settingsStore;
    private final Runnable onSettingsChanged;

    public SettingsDialog(SettingsStore settingsStore, Runnable onSettingsChanged) {
        this.settingsStore = settingsStore;
        this.onSettingsChanged = onSettingsChanged;

        setTitle("NovaEdit Settings");
        initModality(Modality.APPLICATION_MODAL);

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveBtn, cancelBtn);

        TabPane tabPane = new TabPane();
        tabPane.setPrefWidth(550);
        tabPane.setPrefHeight(400);

        Settings settings = settingsStore.getSettings();

        // 1. Editor Tab
        Tab editorTab = new Tab("Editor");
        editorTab.setClosable(false);
        GridPane editorGrid = new GridPane();
        editorGrid.setHgap(12);
        editorGrid.setVgap(12);
        editorGrid.setPadding(new Insets(16));

        Spinner<Integer> tabSizeSpinner = new Spinner<>(1, 8, settings.getTabSize());
        CheckBox useSpacesCheck = new CheckBox("Insert spaces instead of tabs");
        useSpacesCheck.setSelected(settings.isUseSpaces());

        CheckBox wordWrapCheck = new CheckBox("Enable word wrap");
        wordWrapCheck.setSelected(settings.isWordWrap());

        CheckBox lineNumbersCheck = new CheckBox("Show line numbers gutter");
        lineNumbersCheck.setSelected(settings.isShowLineNumbers());

        CheckBox minimapCheck = new CheckBox("Show minimap code preview");
        minimapCheck.setSelected(settings.isShowMinimap());

        CheckBox whitespaceCheck = new CheckBox("Render whitespace symbols");
        whitespaceCheck.setSelected(settings.isShowWhitespace());

        editorGrid.addRow(0, new Label("Tab Size:"), tabSizeSpinner);
        editorGrid.add(useSpacesCheck, 0, 1, 2, 1);
        editorGrid.add(wordWrapCheck, 0, 2, 2, 1);
        editorGrid.add(lineNumbersCheck, 0, 3, 2, 1);
        editorGrid.add(minimapCheck, 0, 4, 2, 1);
        editorGrid.add(whitespaceCheck, 0, 5, 2, 1);
        editorTab.setContent(editorGrid);

        // 2. Appearance Tab
        Tab appearanceTab = new Tab("Appearance");
        appearanceTab.setClosable(false);
        GridPane appGrid = new GridPane();
        appGrid.setHgap(12);
        appGrid.setVgap(12);
        appGrid.setPadding(new Insets(16));

        ComboBox<String> themeBox = new ComboBox<>();
        themeBox.getItems().addAll("dark", "light");
        themeBox.setValue(settings.getTheme());

        Spinner<Integer> fontSizeSpinner = new Spinner<>(8, 36, settings.getFontSize());
        ComboBox<String> fontBox = new ComboBox<>();
        fontBox.getItems().addAll("JetBrains Mono", "Consolas", "Courier New");
        fontBox.setValue(settings.getFontFamily());

        CheckBox toolbarLabelsCheck = new CheckBox("Show text labels alongside toolbar icons");
        toolbarLabelsCheck.setSelected(settings.isToolbarLabelsVisible());

        appGrid.addRow(0, new Label("Theme:"), themeBox);
        appGrid.addRow(1, new Label("Editor Font Family:"), fontBox);
        appGrid.addRow(2, new Label("Font Size (px):"), fontSizeSpinner);
        appGrid.add(toolbarLabelsCheck, 0, 3, 2, 1);
        appearanceTab.setContent(appGrid);

        // 3. Keymap Editor Tab
        Tab keymapTab = new Tab("Keymap");
        keymapTab.setClosable(false);
        VBox keymapBox = new VBox(8);
        keymapBox.setPadding(new Insets(12));

        TableView<Command> keymapTable = new TableView<>();
        TableColumn<Command, String> titleCol = new TableColumn<>("Command");
        titleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().title()));
        titleCol.setPrefWidth(220);

        TableColumn<Command, String> shortcutCol = new TableColumn<>("Shortcut");
        shortcutCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                CommandRegistry.getInstance().getEffectiveShortcut(data.getValue().id())
        ));
        shortcutCol.setPrefWidth(180);

        keymapTable.getColumns().addAll(titleCol, shortcutCol);
        keymapTable.getItems().addAll(CommandRegistry.getInstance().getAll());

        Button rebindBtn = new Button("Rebind Shortcut...");
        rebindBtn.setOnAction(e -> {
            Command selected = keymapTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                TextInputDialog rebindDialog = new TextInputDialog(CommandRegistry.getInstance().getEffectiveShortcut(selected.id()));
                rebindDialog.setTitle("Rebind Shortcut");
                rebindDialog.setHeaderText("Set shortcut for: " + selected.title());
                rebindDialog.setContentText("Key combination (e.g. Ctrl+Alt+K):");
                rebindDialog.showAndWait().ifPresent(newKey -> {
                    CommandRegistry.getInstance().setCustomShortcut(selected.id(), newKey.trim());
                    keymapTable.refresh();
                });
            }
        });

        keymapBox.getChildren().addAll(keymapTable, rebindBtn);
        keymapTab.setContent(keymapBox);

        tabPane.getTabs().addAll(editorTab, appearanceTab, keymapTab);
        getDialogPane().setContent(tabPane);

        setResultConverter(btn -> {
            if (btn == saveBtn) {
                settings.setTabSize(tabSizeSpinner.getValue());
                settings.setUseSpaces(useSpacesCheck.isSelected());
                settings.setWordWrap(wordWrapCheck.isSelected());
                settings.setShowLineNumbers(lineNumbersCheck.isSelected());
                settings.setShowMinimap(minimapCheck.isSelected());
                settings.setShowWhitespace(whitespaceCheck.isSelected());

                settings.setTheme(themeBox.getValue());
                settings.setFontFamily(fontBox.getValue());
                settings.setFontSize(fontSizeSpinner.getValue());
                settings.setToolbarLabelsVisible(toolbarLabelsCheck.isSelected());

                settingsStore.save();
                if (onSettingsChanged != null) onSettingsChanged.run();
                return true;
            }
            return false;
        });
    }
}
