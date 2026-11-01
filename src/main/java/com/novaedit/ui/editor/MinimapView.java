package com.novaedit.ui.editor;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.CodeArea;

/**
 * Minimap overview canvas rendering a bird's-eye representation of the document.
 */
public class MinimapView extends Pane {
    private final CodeArea codeArea;
    private final Canvas canvas;
    private static final double MINIMAP_WIDTH = 90;
    private static final double LINE_HEIGHT = 2.0;

    public MinimapView(CodeArea codeArea) {
        this.codeArea = codeArea;
        this.canvas = new Canvas(MINIMAP_WIDTH, 600);
        getChildren().add(canvas);
        setPrefWidth(MINIMAP_WIDTH);
        setMaxWidth(MINIMAP_WIDTH);
        setStyle("-fx-background-color: -color-surface; -fx-border-color: transparent transparent transparent -color-border-subtle; -fx-border-width: 0 0 0 1;");

        widthProperty().addListener((obs, oldVal, newVal) -> canvas.setWidth(newVal.doubleValue()));
        heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            render();
        });

        // Click to jump/scroll
        canvas.setOnMouseClicked(event -> {
            double y = event.getY();
            int targetLine = (int) (y / LINE_HEIGHT);
            if (targetLine >= 0 && targetLine < codeArea.getParagraphs().size()) {
                codeArea.showParagraphAtTop(targetLine);
            }
        });
    }

    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);

        int totalParagraphs = codeArea.getParagraphs().size();
        if (totalParagraphs == 0) return;

        gc.setFill(Color.web("#868a91", 0.4));

        int maxLines = (int) (h / LINE_HEIGHT);
        int step = Math.max(1, totalParagraphs / Math.max(1, maxLines));

        for (int i = 0; i < totalParagraphs; i += step) {
            String lineText = codeArea.getParagraph(i).getText();
            int len = Math.min(lineText.length(), 60);
            if (len > 0) {
                double y = (double) i / totalParagraphs * h;
                double lineW = (len / 60.0) * (w - 12);
                gc.fillRect(6, y, Math.max(2, lineW), 1.5);
            }
        }
    }
}
