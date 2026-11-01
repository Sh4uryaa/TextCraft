package com.novaedit.ui.dialogs;

import com.novaedit.util.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * About dialog showing NovaEdit version, author, and technology stack.
 */
public class AboutDialog extends Alert {

    public AboutDialog() {
        super(AlertType.INFORMATION);
        setTitle("About NovaEdit");
        setHeaderText(null);

        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 16;");

        Label title = new Label("NovaEdit 1.0.0");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        Label desc = new Label("Advanced Professional Desktop Text & Code Editor in Java 21");
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted;");

        Label tech = new Label(
                "• Java 21 LTS\n" +
                "• JavaFX 21 & RichTextFX\n" +
                "• PieceTable & Virtualized TextBuffer\n" +
                "• Ikonli Iconography & Jackson JSON\n" +
                "• JGit Source Control & java-diff-utils\n" +
                "• Juniversalchardet Encoding Detection"
        );
        tech.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text;");

        content.getChildren().addAll(Icons.get("brackets", 36), title, desc, tech);
        getDialogPane().setContent(content);
    }
}
