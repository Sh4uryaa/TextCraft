package com.novaedit.ui.shell;

import com.novaedit.commands.CommandRegistry;
import com.novaedit.util.Icons;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates the comprehensive two-row toolbar where EVERY single feature is reachable
 * via a visible button with icon, label, and shortcut tooltip.
 */
public class ToolBarFactory {

    private final List<Button> allButtons = new ArrayList<>();
    private boolean showLabels = true;

    public VBox createToolbar(CommandRegistry registry) {
        allButtons.clear();
        VBox container = new VBox(4);
        container.getStyleClass().add("toolbar-container");

        // Row 1: File, Edit, Search, Format, App
        HBox row1 = new HBox(4);
        row1.getStyleClass().add("nova-toolbar");
        row1.setAlignment(Pos.CENTER_LEFT);

        // File Group
        row1.getChildren().addAll(
                createGroup(
                        btn(registry, "file.new", "New", "new", "Ctrl+N"),
                        btn(registry, "file.open", "Open", "open", "Ctrl+O"),
                        btn(registry, "file.open_folder", "Folder", "folder", "Ctrl+K Ctrl+O"),
                        btn(registry, "file.save", "Save", "save", "Ctrl+S"),
                        btn(registry, "file.save_as", "Save As", "save-all", "Ctrl+Shift+S"),
                        btn(registry, "file.save_all", "Save All", "save-all", "Ctrl+Alt+S")
                ),
                separator(),
                // Edit Group
                createGroup(
                        btn(registry, "edit.undo", "Undo", "undo", "Ctrl+Z"),
                        btn(registry, "edit.redo", "Redo", "redo", "Ctrl+Y"),
                        btn(registry, "edit.cut", "Cut", "cut", "Ctrl+X"),
                        btn(registry, "edit.copy", "Copy", "copy", "Ctrl+C"),
                        btn(registry, "edit.paste", "Paste", "paste", "Ctrl+V"),
                        btn(registry, "edit.select_all", "Select All", "select-all", "Ctrl+A")
                ),
                separator(),
                // Search Group
                createGroup(
                        btn(registry, "search.find", "Find", "find", "Ctrl+F"),
                        btn(registry, "search.replace", "Replace", "replace", "Ctrl+H"),
                        btn(registry, "search.find_in_files", "In Files", "find-files", "Ctrl+Shift+F"),
                        btn(registry, "search.goto_line", "Go to Line", "goto-line", "Ctrl+G")
                ),
                separator(),
                // Format Group
                createGroup(
                        btn(registry, "format.auto", "Format", "format", "Ctrl+Alt+L"),
                        btn(registry, "format.indent", "Indent", "indent", "Tab"),
                        btn(registry, "format.outdent", "Outdent", "outdent", "Shift+Tab"),
                        btn(registry, "format.comment", "Comment", "comment", "Ctrl+/"),
                        btn(registry, "format.duplicate_line", "Duplicate", "duplicate", "Ctrl+Shift+D"),
                        btn(registry, "format.move_line_up", "Move Up", "move-up", "Alt+Up"),
                        btn(registry, "format.move_line_down", "Move Down", "move-down", "Alt+Down"),
                        btn(registry, "format.delete_line", "Delete Line", "delete", "Ctrl+Shift+K")
                ),
                separator(),
                // App Group
                createGroup(
                        btn(registry, "app.theme_toggle", "Theme", "theme", "Alt+T"),
                        btn(registry, "app.settings", "Settings", "settings", "Ctrl+,"),
                        btn(registry, "app.help", "Help", "help", "F1")
                )
        );

        // Row 2: Code, View, Tools, Git
        HBox row2 = new HBox(4);
        row2.getStyleClass().add("nova-toolbar");
        row2.setAlignment(Pos.CENTER_LEFT);

        row2.getChildren().addAll(
                // Code Group
                createGroup(
                        btn(registry, "code.fold_all", "Fold All", "fold", "Ctrl+Shift+["),
                        btn(registry, "code.unfold_all", "Unfold All", "unfold", "Ctrl+Shift+]"),
                        btn(registry, "code.bracket_match", "Match Bracket", "brackets", "Ctrl+M"),
                        btn(registry, "code.snippets", "Snippets", "snippets", "Ctrl+J"),
                        btn(registry, "code.autocomplete", "Autocomplete", "autocomplete", "Ctrl+Space")
                ),
                separator(),
                // View Group
                createGroup(
                        btn(registry, "view.word_wrap", "Wrap", "wrap", "Alt+Z"),
                        btn(registry, "view.whitespace", "Whitespace", "whitespace", ""),
                        btn(registry, "view.line_numbers", "Line Nos", "numbers", ""),
                        btn(registry, "view.minimap", "Minimap", "minimap", ""),
                        btn(registry, "view.split", "Split View", "split", "Ctrl+\\"),
                        btn(registry, "view.zoom_in", "Zoom In", "zoom-in", "Ctrl++"),
                        btn(registry, "view.zoom_out", "Zoom Out", "zoom-out", "Ctrl+-"),
                        btn(registry, "view.zoom_reset", "Reset Zoom", "zoom-reset", "Ctrl+0"),
                        btn(registry, "view.zen", "Zen Mode", "fullscreen", "F11")
                ),
                separator(),
                // Tools Group
                createGroup(
                        btn(registry, "tools.palette", "Palette", "palette", "Ctrl+Shift+P"),
                        btn(registry, "tools.diff", "Diff Files", "diff", ""),
                        btn(registry, "tools.record_macro", "Record", "record", ""),
                        btn(registry, "tools.play_macro", "Play Macro", "play", ""),
                        btn(registry, "tools.spell_check", "Spell Check", "spell", ""),
                        btn(registry, "tools.terminal", "Terminal", "terminal", "Ctrl+`")
                ),
                separator(),
                // Git Group
                createGroup(
                        btn(registry, "git.stage", "Stage", "git", ""),
                        btn(registry, "git.commit", "Commit", "commit", "Ctrl+K"),
                        btn(registry, "git.pull", "Pull", "pull", ""),
                        btn(registry, "git.push", "Push", "push", ""),
                        btn(registry, "git.refresh", "Refresh", "refresh", "")
                )
        );

        container.getChildren().addAll(row1, row2);
        return container;
    }

    private Button btn(CommandRegistry registry, String commandId, String label, String iconName, String shortcut) {
        Button button = new Button(showLabels ? label : "");
        button.setGraphic(Icons.get(iconName, 14));
        button.getStyleClass().add("tool-button");

        String tooltipText = label + (shortcut.isEmpty() ? "" : " (" + shortcut + ")");
        button.setTooltip(new Tooltip(tooltipText));
        button.setOnAction(e -> registry.execute(commandId));

        allButtons.add(button);
        return button;
    }

    private HBox createGroup(Node... nodes) {
        HBox group = new HBox(2, nodes);
        group.getStyleClass().add("toolbar-group");
        group.setAlignment(Pos.CENTER_LEFT);
        return group;
    }

    private Node separator() {
        Separator sep = new Separator();
        sep.setOrientation(javafx.geometry.Orientation.VERTICAL);
        sep.getStyleClass().add("toolbar-separator");
        return sep;
    }

    public void setLabelsVisible(boolean visible) {
        this.showLabels = visible;
        for (Button btn : allButtons) {
            Tooltip tip = btn.getTooltip();
            if (tip != null) {
                String tipText = tip.getText();
                String label = tipText.contains(" (") ? tipText.substring(0, tipText.indexOf(" (")) : tipText;
                btn.setText(visible ? label : "");
            }
        }
    }
}
