package com.novaedit.ui.shell;

import com.novaedit.commands.CommandRegistry;
import com.novaedit.util.Icons;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;

/**
 * Creates the main application menu bar wired to the central CommandRegistry.
 */
public class MenuBarFactory {

    public static MenuBar createMenuBar(CommandRegistry registry) {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("menu-bar");

        // --- File Menu ---
        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(
                item(registry, "file.new", "New File", "Ctrl+N", "new"),
                item(registry, "file.open", "Open File...", "Ctrl+O", "open"),
                item(registry, "file.open_folder", "Open Folder...", "Ctrl+K Ctrl+O", "folder"),
                new SeparatorMenuItem(),
                item(registry, "file.save", "Save", "Ctrl+S", "save"),
                item(registry, "file.save_as", "Save As...", "Ctrl+Shift+S", "save-all"),
                item(registry, "file.save_all", "Save All", "Ctrl+Alt+S", "save-all"),
                new SeparatorMenuItem(),
                item(registry, "file.reopen_closed", "Reopen Closed Tab", "Ctrl+Shift+T", "refresh"),
                item(registry, "file.close_tab", "Close Tab", "Ctrl+W", "close"),
                new SeparatorMenuItem(),
                item(registry, "file.export_html", "Export to HTML...", "", "file-plus"),
                item(registry, "file.print", "Print...", "Ctrl+P", "file-plus"),
                new SeparatorMenuItem(),
                item(registry, "app.exit", "Exit", "Alt+F4", "close")
        );

        // --- Edit Menu ---
        Menu editMenu = new Menu("Edit");
        editMenu.getItems().addAll(
                item(registry, "edit.undo", "Undo", "Ctrl+Z", "undo"),
                item(registry, "edit.redo", "Redo", "Ctrl+Y", "redo"),
                new SeparatorMenuItem(),
                item(registry, "edit.cut", "Cut", "Ctrl+X", "cut"),
                item(registry, "edit.copy", "Copy", "Ctrl+C", "copy"),
                item(registry, "edit.paste", "Paste", "Ctrl+V", "paste"),
                item(registry, "edit.select_all", "Select All", "Ctrl+A", "select-all")
        );

        // --- Selection Menu ---
        Menu selectionMenu = new Menu("Selection");
        selectionMenu.getItems().addAll(
                item(registry, "selection.select_next", "Add Selection to Next Find Match", "Ctrl+D", "find"),
                item(registry, "selection.select_all_matches", "Select All Occurrences", "Ctrl+Shift+L", "select-all")
        );

        // --- View Menu ---
        Menu viewMenu = new Menu("View");
        viewMenu.getItems().addAll(
                item(registry, "view.word_wrap", "Toggle Word Wrap", "Alt+Z", "wrap"),
                item(registry, "view.line_numbers", "Toggle Line Numbers", "", "numbers"),
                item(registry, "view.minimap", "Toggle Minimap", "", "minimap"),
                item(registry, "view.whitespace", "Render Whitespace", "", "whitespace"),
                item(registry, "view.split", "Split Editor View", "Ctrl+\\", "split"),
                new SeparatorMenuItem(),
                item(registry, "view.zoom_in", "Zoom In", "Ctrl++", "zoom-in"),
                item(registry, "view.zoom_out", "Zoom Out", "Ctrl+-", "zoom-out"),
                item(registry, "view.zoom_reset", "Reset Zoom", "Ctrl+0", "zoom-reset"),
                new SeparatorMenuItem(),
                item(registry, "view.zen", "Toggle Zen Mode", "F11", "fullscreen")
        );

        // --- Go Menu ---
        Menu goMenu = new Menu("Go");
        goMenu.getItems().addAll(
                item(registry, "search.goto_line", "Go to Line...", "Ctrl+G", "goto-line"),
                item(registry, "code.bracket_match", "Go to Matching Bracket", "Ctrl+M", "brackets"),
                new SeparatorMenuItem(),
                item(registry, "search.find", "Find...", "Ctrl+F", "find"),
                item(registry, "search.replace", "Replace...", "Ctrl+H", "replace"),
                item(registry, "search.find_in_files", "Find in Files...", "Ctrl+Shift+F", "find-files")
        );

        // --- Tools Menu ---
        Menu toolsMenu = new Menu("Tools");
        toolsMenu.getItems().addAll(
                item(registry, "tools.palette", "Command Palette...", "Ctrl+Shift+P", "palette"),
                item(registry, "tools.diff", "Compare Files (Diff)...", "", "diff"),
                new SeparatorMenuItem(),
                item(registry, "tools.record_macro", "Record / Stop Macro", "", "record"),
                item(registry, "tools.play_macro", "Play Recorded Macro", "", "play"),
                new SeparatorMenuItem(),
                item(registry, "tools.spell_check", "Spell Check", "", "spell"),
                item(registry, "tools.terminal", "Toggle Integrated Terminal", "Ctrl+`", "terminal"),
                new SeparatorMenuItem(),
                item(registry, "app.settings", "Settings...", "Ctrl+,", "settings")
        );

        // --- Git Menu ---
        Menu gitMenu = new Menu("Git");
        gitMenu.getItems().addAll(
                item(registry, "git.stage", "Stage Current File", "", "git"),
                item(registry, "git.commit", "Commit...", "Ctrl+K", "commit"),
                item(registry, "git.pull", "Pull", "", "pull"),
                item(registry, "git.push", "Push", "", "push"),
                new SeparatorMenuItem(),
                item(registry, "git.refresh", "Refresh Git Status", "", "refresh")
        );

        // --- Help Menu ---
        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().addAll(
                item(registry, "help.welcome", "Welcome Screen", "", "file-plus"),
                item(registry, "help.shortcuts", "Keyboard Shortcuts", "Ctrl+K Ctrl+S", "help"),
                new SeparatorMenuItem(),
                item(registry, "help.about", "About NovaEdit", "", "help")
        );

        menuBar.getMenus().addAll(fileMenu, editMenu, selectionMenu, viewMenu, goMenu, toolsMenu, gitMenu, helpMenu);
        return menuBar;
    }

    private static MenuItem item(CommandRegistry registry, String commandId, String title, String shortcut, String iconName) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setGraphic(Icons.get(iconName, 13));
        if (shortcut != null && !shortcut.isBlank()) {
            try {
                menuItem.setAccelerator(KeyCombination.keyCombination(shortcut));
            } catch (Exception ignored) {}
        }
        menuItem.setOnAction(e -> registry.execute(commandId));
        return menuItem;
    }
}
