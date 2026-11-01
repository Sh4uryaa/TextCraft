package com.novaedit.ui.shell;

import com.novaedit.util.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Vertical icon strip on the far left for switching side panels (Explorer, Search, Git, Outline, Settings).
 */
public class ActivityBar extends VBox {

    public enum PanelType {
        EXPLORER, SEARCH, GIT, OUTLINE, SETTINGS
    }

    private final Map<PanelType, Button> buttons = new HashMap<>();
    private PanelType activePanel = PanelType.EXPLORER;
    private Consumer<PanelType> onPanelSelected;
    private Runnable onToggleCollapse;

    public ActivityBar() {
        initUI();
    }

    private void initUI() {
        getStyleClass().add("activity-bar");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(4);

        addActivityButton(PanelType.EXPLORER, "Explorer", "explorer", "Ctrl+Shift+E");
        addActivityButton(PanelType.SEARCH, "Search in Files", "search", "Ctrl+Shift+F");
        addActivityButton(PanelType.GIT, "Source Control", "git", "Ctrl+Shift+G");
        addActivityButton(PanelType.OUTLINE, "Outline Symbols", "outline", "");

        // Spacer to push Settings to bottom
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        getChildren().add(spacer);

        addActivityButton(PanelType.SETTINGS, "Settings", "settings", "Ctrl+,");

        setActivePanel(PanelType.EXPLORER);
    }

    private void addActivityButton(PanelType type, String title, String iconName, String shortcut) {
        Button btn = new Button();
        btn.setGraphic(Icons.get(iconName, 18));
        btn.getStyleClass().add("activity-button");

        String tip = title + (shortcut.isEmpty() ? "" : " (" + shortcut + ")");
        btn.setTooltip(new Tooltip(tip));

        btn.setOnAction(e -> {
            if (activePanel == type) {
                // Toggle collapse
                if (onToggleCollapse != null) onToggleCollapse.run();
            } else {
                setActivePanel(type);
                if (onPanelSelected != null) onPanelSelected.accept(type);
            }
        });

        buttons.put(type, btn);
        getChildren().add(btn);
    }

    public void setActivePanel(PanelType type) {
        this.activePanel = type;
        buttons.forEach((pType, btn) -> {
            if (pType == type) {
                if (!btn.getStyleClass().contains("active")) {
                    btn.getStyleClass().add("active");
                }
            } else {
                btn.getStyleClass().remove("active");
            }
        });
    }

    public PanelType getActivePanel() {
        return activePanel;
    }

    public void setOnPanelSelected(Consumer<PanelType> onPanelSelected) {
        this.onPanelSelected = onPanelSelected;
    }

    public void setOnToggleCollapse(Runnable onToggleCollapse) {
        this.onToggleCollapse = onToggleCollapse;
    }
}
