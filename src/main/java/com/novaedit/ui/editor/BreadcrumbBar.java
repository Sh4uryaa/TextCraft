package com.novaedit.ui.editor;

import com.novaedit.util.Icons;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.nio.file.Path;

/**
 * Clickable breadcrumb bar above the editor showing file hierarchy and current scope.
 */
public class BreadcrumbBar extends HBox {

    public BreadcrumbBar() {
        getStyleClass().add("breadcrumb-bar");
        setSpacing(6);
        updatePath(null);
    }

    public void updatePath(Path filePath) {
        getChildren().clear();
        if (filePath == null) {
            Label untitled = new Label("untitled");
            untitled.getStyleClass().add("breadcrumb-item");
            getChildren().add(untitled);
            return;
        }

        Path current = filePath.toAbsolutePath().normalize();
        Path root = current.getRoot();
        int count = current.getNameCount();

        // Show up to the last 4 path components
        int startIndex = Math.max(0, count - 4);
        for (int i = startIndex; i < count; i++) {
            if (i > startIndex) {
                Label separator = new Label(">");
                separator.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 10px;");
                getChildren().add(separator);
            }
            String part = current.getName(i).toString();
            Label item = new Label(part);
            item.getStyleClass().add("breadcrumb-item");
            if (i == count - 1) {
                item.setGraphic(Icons.get("file-plus", 12));
            } else {
                item.setGraphic(Icons.get("folder", 12));
            }
            getChildren().add(item);
        }
    }
}
