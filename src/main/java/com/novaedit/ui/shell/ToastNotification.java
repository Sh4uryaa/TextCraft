package com.novaedit.ui.shell;

import com.novaedit.util.Icons;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Non-intrusive floating toast notifications in the bottom-right corner.
 */
public class ToastNotification {
    private static VBox toastContainer;

    public enum Type {
        INFO, SUCCESS, WARNING, ERROR
    }

    public static void init(StackPane rootPane) {
        toastContainer = new VBox(8);
        toastContainer.getStyleClass().add("toast-container");
        toastContainer.setAlignment(Pos.BOTTOM_RIGHT);
        toastContainer.setMouseTransparent(true);
        toastContainer.setPickOnBounds(false);
        rootPane.getChildren().add(toastContainer);
        StackPane.setAlignment(toastContainer, Pos.BOTTOM_RIGHT);
    }

    public static void show(String message, Type type) {
        if (toastContainer == null || message == null || message.isBlank()) return;

        HBox toast = new HBox(8);
        toast.getStyleClass().add("toast");
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setMouseTransparent(false);

        String iconName = switch (type) {
            case SUCCESS -> "spell";
            case WARNING -> "help";
            case ERROR -> "close";
            default -> "file-plus";
        };

        Node iconNode = Icons.get(iconName, 16);
        Label label = new Label(message);
        label.getStyleClass().add("toast-message");

        toast.getChildren().addAll(iconNode, label);
        toast.setOpacity(0);
        toastContainer.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.millis(2500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> toastContainer.getChildren().remove(toast));

        SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
        seq.play();
    }

    public static void show(String message) {
        show(message, Type.INFO);
    }
}
