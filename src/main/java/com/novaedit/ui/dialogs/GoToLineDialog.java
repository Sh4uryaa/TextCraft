package com.novaedit.ui.dialogs;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Dialog for jumping to a specific line and column.
 */
public class GoToLineDialog extends Dialog<int[]> {

    public GoToLineDialog(int currentLine, int maxLines) {
        setTitle("Go to Line");
        setHeaderText("Current line: " + currentLine + " of " + maxLines);

        ButtonType jumpBtn = new ButtonType("Go", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(jumpBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField lineField = new TextField(String.valueOf(currentLine));
        TextField colField = new TextField("1");

        grid.add(new Label("Line:"), 0, 0);
        grid.add(lineField, 1, 0);
        grid.add(new Label("Column:"), 0, 1);
        grid.add(colField, 1, 1);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == jumpBtn) {
                try {
                    int l = Integer.parseInt(lineField.getText().trim());
                    int c = Integer.parseInt(colField.getText().trim());
                    return new int[]{l, c};
                } catch (NumberFormatException ignored) {}
            }
            return null;
        });
    }
}
