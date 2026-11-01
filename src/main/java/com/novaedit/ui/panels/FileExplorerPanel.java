package com.novaedit.ui.panels;

import com.novaedit.util.Icons;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * File Explorer side panel displaying workspace file hierarchy with file-type icons,
 * context menus, creation, renaming, and deletion.
 */
public class FileExplorerPanel extends BorderPane {
    private static final Logger log = LoggerFactory.getLogger(FileExplorerPanel.class);

    private final TreeView<Path> treeView = new TreeView<>();
    private final Label titleLabel = new Label("NO FOLDER OPEN");
    private Path rootPath;
    private Consumer<Path> onFileSelected;

    public FileExplorerPanel() {
        initUI();
    }

    private void initUI() {
        getStyleClass().add("side-panel");

        // Header
        HBox header = new HBox(8);
        header.getStyleClass().add("side-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);

        titleLabel.getStyleClass().add("side-panel-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newFileBtn = createHeaderAction("New File", "file-plus", e -> createNewItem(false));
        Button newFolderBtn = createHeaderAction("New Folder", "folder", e -> createNewItem(true));
        Button refreshBtn = createHeaderAction("Refresh", "refresh", e -> refresh());

        header.getChildren().addAll(titleLabel, spacer, newFileBtn, newFolderBtn, refreshBtn);
        setTop(header);

        // TreeView
        treeView.getStyleClass().add("tree-view");
        treeView.setShowRoot(true);
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String name = (item.equals(rootPath))
                            ? item.getFileName().toString().toUpperCase()
                            : item.getFileName().toString();
                    setText(name);

                    if (Files.isDirectory(item)) {
                        setGraphic(Icons.get("folder", 14));
                    } else {
                        setGraphic(Icons.get(getFileIconName(item), 14));
                    }
                }
            }
        });

        // Double click to open
        treeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<Path> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null && !Files.isDirectory(selected.getValue())) {
                    if (onFileSelected != null) {
                        onFileSelected.accept(selected.getValue());
                    }
                }
            }
        });

        // Context Menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem newFileItem = new MenuItem("New File...");
        newFileItem.setOnAction(e -> createNewItem(false));

        MenuItem newFolderItem = new MenuItem("New Folder...");
        newFolderItem.setOnAction(e -> createNewItem(true));

        MenuItem renameItem = new MenuItem("Rename...");
        renameItem.setOnAction(e -> renameSelected());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> deleteSelected());

        MenuItem revealItem = new MenuItem("Reveal in Explorer");
        revealItem.setOnAction(e -> revealSelected());

        contextMenu.getItems().addAll(newFileItem, newFolderItem, new SeparatorMenuItem(), renameItem, deleteItem, new SeparatorMenuItem(), revealItem);
        treeView.setContextMenu(contextMenu);

        setCenter(treeView);
    }

    public void openFolder(Path folder) {
        this.rootPath = folder;
        if (folder != null && Files.isDirectory(folder)) {
            titleLabel.setText(folder.getFileName().toString().toUpperCase());
            refresh();
        } else {
            titleLabel.setText("NO FOLDER OPEN");
            treeView.setRoot(null);
        }
    }

    public void refresh() {
        if (rootPath == null || !Files.isDirectory(rootPath)) return;
        TreeItem<Path> rootItem = buildTreeItem(rootPath);
        rootItem.setExpanded(true);
        treeView.setRoot(rootItem);
    }

    private TreeItem<Path> buildTreeItem(Path path) {
        TreeItem<Path> item = new TreeItem<>(path);
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.sorted(Comparator.comparing(p -> !Files.isDirectory(p)))
                      .filter(p -> !p.getFileName().toString().startsWith(".") && !p.getFileName().toString().equals("target"))
                      .forEach(child -> item.getChildren().add(buildTreeItem(child)));
            } catch (IOException e) {
                log.debug("Error listing directory {}: {}", path, e.getMessage());
            }
        }
        return item;
    }

    private void createNewItem(boolean isDir) {
        TreeItem<Path> selected = treeView.getSelectionModel().getSelectedItem();
        Path parent = (selected != null)
                ? (Files.isDirectory(selected.getValue()) ? selected.getValue() : selected.getValue().getParent())
                : rootPath;

        if (parent == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(isDir ? "New Folder" : "New File");
        dialog.setHeaderText("Create " + (isDir ? "folder" : "file") + " in " + parent.getFileName());
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                Path target = parent.resolve(name.trim());
                try {
                    if (isDir) {
                        Files.createDirectories(target);
                    } else {
                        Files.createFile(target);
                        if (onFileSelected != null) {
                            onFileSelected.accept(target);
                        }
                    }
                    refresh();
                } catch (IOException ex) {
                    log.error("Failed to create item", ex);
                }
            }
        });
    }

    private void renameSelected() {
        TreeItem<Path> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null || selected.getValue().equals(rootPath)) return;

        Path target = selected.getValue();
        TextInputDialog dialog = new TextInputDialog(target.getFileName().toString());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename " + target.getFileName());
        dialog.setContentText("New name:");

        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isBlank() && !newName.equals(target.getFileName().toString())) {
                try {
                    Files.move(target, target.resolveSibling(newName.trim()));
                    refresh();
                } catch (IOException ex) {
                    log.error("Failed to rename", ex);
                }
            }
        });
    }

    private void deleteSelected() {
        TreeItem<Path> selected = treeView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null || selected.getValue().equals(rootPath)) return;

        Path target = selected.getValue();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete " + target.getFileName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    if (Files.isDirectory(target)) {
                        deleteDirRecursively(target);
                    } else {
                        Files.delete(target);
                    }
                    refresh();
                } catch (IOException ex) {
                    log.error("Failed to delete", ex);
                }
            }
        });
    }

    private void deleteDirRecursively(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
        }
    }

    private void revealSelected() {
        TreeItem<Path> selected = treeView.getSelectionModel().getSelectedItem();
        Path target = (selected != null && selected.getValue() != null) ? selected.getValue() : rootPath;
        if (target != null && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(Files.isDirectory(target) ? target.toFile() : target.getParent().toFile());
            } catch (Exception ex) {
                log.warn("Failed to reveal in desktop", ex);
            }
        }
    }

    private String getFileIconName(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".java")) return "brackets";
        if (name.endsWith(".json")) return "brackets";
        if (name.endsWith(".xml") || name.endsWith(".html")) return "brackets";
        if (name.endsWith(".md")) return "file-plus";
        return "file-plus";
    }

    private Button createHeaderAction(String tooltip, String iconName, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button();
        btn.setGraphic(Icons.get(iconName, 12));
        btn.getStyleClass().add("tool-button");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnAction(handler);
        return btn;
    }

    public void setOnFileSelected(Consumer<Path> onFileSelected) {
        this.onFileSelected = onFileSelected;
    }

    public Path getRootPath() {
        return rootPath;
    }
}
