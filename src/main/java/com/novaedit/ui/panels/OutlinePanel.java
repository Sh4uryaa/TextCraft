package com.novaedit.ui.panels;

import com.novaedit.ui.editor.EditorTab;
import com.novaedit.util.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Outline panel extracting classes, methods, and markdown headings for instant jumping.
 */
public class OutlinePanel extends BorderPane {

    public record Symbol(String name, int line, String kind) {}

    private static final Pattern JAVA_SYMBOL = Pattern.compile(
            "^\\s*(public|protected|private|static|final|abstract|synchronized)*\\s*(class|interface|enum|record)\\s+([a-zA-Z0-9_]+)" +
            "|^\\s*(public|protected|private|static|final|native|synchronized)*\\s*([a-zA-Z0-9_<>\\[\\]]+)\\s+([a-zA-Z0-9_]+)\\s*\\([^\\)]*\\)\\s*(throws[^{]+)?\\{"
    );

    private static final Pattern MD_HEADING = Pattern.compile("^(#{1,6})\\s+([^\n]+)");

    private final ListView<Symbol> symbolList = new ListView<>();
    private EditorTab currentTab;

    public OutlinePanel() {
        initUI();
    }

    private void initUI() {
        getStyleClass().add("side-panel");

        HBox header = new HBox(8);
        header.getStyleClass().add("side-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("OUTLINE");
        title.getStyleClass().add("side-panel-title");
        header.getChildren().add(title);
        setTop(header);

        symbolList.getStyleClass().add("tree-view");
        symbolList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Symbol item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name());
                    String icon = switch (item.kind()) {
                        case "class" -> "brackets";
                        case "heading" -> "format";
                        default -> "snippets";
                    };
                    setGraphic(Icons.get(icon, 13));
                }
            }
        });

        symbolList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 || event.getClickCount() == 1) {
                Symbol selected = symbolList.getSelectionModel().getSelectedItem();
                if (selected != null && currentTab != null) {
                    int line = selected.line();
                    if (line >= 0 && line < currentTab.getCodeArea().getParagraphs().size()) {
                        currentTab.getCodeArea().showParagraphAtCenter(line);
                        currentTab.getCodeArea().moveTo(line, 0);
                        currentTab.getCodeArea().requestFocus();
                    }
                }
            }
        });

        setCenter(symbolList);
    }

    public void updateSymbols(EditorTab tab) {
        this.currentTab = tab;
        symbolList.getItems().clear();
        if (tab == null) return;

        String lang = tab.getTokenizer().getLanguageId();
        String text = tab.getCodeArea().getText();
        String[] lines = text.split("\n", -1);
        List<Symbol> symbols = new ArrayList<>();

        if ("markdown".equalsIgnoreCase(lang)) {
            for (int i = 0; i < lines.length; i++) {
                Matcher m = MD_HEADING.matcher(lines[i]);
                if (m.find()) {
                    String prefix = "  ".repeat(m.group(1).length() - 1);
                    symbols.add(new Symbol(prefix + m.group(2), i, "heading"));
                }
            }
        } else if ("java".equalsIgnoreCase(lang)) {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Matcher m = JAVA_SYMBOL.matcher(line);
                if (m.find()) {
                    String name;
                    String kind;
                    if (m.group(3) != null) {
                        name = m.group(2) + " " + m.group(3);
                        kind = "class";
                    } else if (m.group(6) != null) {
                        name = m.group(6) + "()";
                        kind = "method";
                    } else {
                        name = line.trim();
                        kind = "symbol";
                    }
                    symbols.add(new Symbol(name, i, kind));
                }
            }
        }

        symbolList.getItems().setAll(symbols);
    }
}
