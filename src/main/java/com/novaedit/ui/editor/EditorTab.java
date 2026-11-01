package com.novaedit.ui.editor;

import com.novaedit.core.document.Document;
import com.novaedit.core.history.UndoManager;
import com.novaedit.features.format.BracketMatcher;
import com.novaedit.features.format.IndentService;
import com.novaedit.syntax.Token;
import com.novaedit.syntax.Tokenizer;
import com.novaedit.syntax.TokenizerRegistry;
import com.novaedit.util.Debouncer;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Encapsulates an open document tab with virtualized CodeArea, line numbers gutter,
 * syntax highlighting, minimap, breadcrumbs, and formatting support.
 */
public class EditorTab extends BorderPane {
    private final Document document;
    private final UndoManager undoManager;
    private final CodeArea codeArea;
    private final BreadcrumbBar breadcrumbBar;
    private final MinimapView minimapView;
    private final Debouncer syntaxDebouncer = new Debouncer();
    private final IndentService indentService = new IndentService();
    private final BracketMatcher bracketMatcher = new BracketMatcher();

    private Tokenizer tokenizer;
    private int fontSize = 14;
    private boolean updatingFromDocument = false;
    private boolean updatingFromEditor = false;

    public EditorTab(Document document) {
        this.document = document;
        this.undoManager = new UndoManager(document);
        this.codeArea = new CodeArea();
        this.breadcrumbBar = new BreadcrumbBar();
        this.minimapView = new MinimapView(codeArea);

        // Detect tokenizer
        this.tokenizer = TokenizerRegistry.detect(document.getFilePath());

        initUI();
        initListeners();
        applySyntaxHighlighting();
    }

    private void initUI() {
        getStyleClass().add("editor-tab-pane");

        // Breadcrumb
        breadcrumbBar.updatePath(document.getFilePath());
        setTop(breadcrumbBar);

        // CodeArea setup
        codeArea.getStyleClass().add("code-area");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.replaceText(document.getText());
        codeArea.setStyle("-fx-font-size: " + fontSize + "px;");

        // Center layout with CodeArea and Minimap
        HBox editorCenter = new HBox(codeArea, minimapView);
        HBox.setHgrow(codeArea, Priority.ALWAYS);
        setCenter(editorCenter);

        // Key bindings for indentation and auto-closing
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);

        // Zoom with Ctrl+Scroll
        codeArea.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                if (event.getDeltaY() > 0) {
                    zoomIn();
                } else if (event.getDeltaY() < 0) {
                    zoomOut();
                }
                event.consume();
            }
        });
    }

    private void initListeners() {
        // Sync CodeArea changes to Document & Debounced Syntax Highlighting
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!updatingFromDocument) {
                updatingFromEditor = true;
                try {
                    // Update document buffer
                    document.getBuffer().delete(0, document.getBuffer().length());
                    document.getBuffer().insert(0, newText);
                    document.setDirty(true);
                } finally {
                    updatingFromEditor = false;
                }
            }

            syntaxDebouncer.debounce(this::applySyntaxHighlighting, 150);
            minimapView.render();
        });
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            // Smart auto-indent on Enter
            int caret = codeArea.getCaretPosition();
            int currentParagraph = codeArea.getCurrentParagraph();
            String currentLine = codeArea.getParagraph(currentParagraph).getText();

            String indent = indentService.computeNewLineIndent(currentLine, 4, true);

            // Check if between braces: { | }
            boolean betweenBraces = false;
            if (caret > 0 && caret < codeArea.getLength()) {
                char before = codeArea.getText(caret - 1, caret).charAt(0);
                char after = codeArea.getText(caret, caret + 1).charAt(0);
                betweenBraces = indentService.isBetweenBraces(before, after);
            }

            if (betweenBraces) {
                String baseIndent = indentService.getLineIndentation(currentLine);
                String insertText = "\n" + indent + "\n" + baseIndent;
                codeArea.insertText(caret, insertText);
                codeArea.moveTo(caret + 1 + indent.length());
                event.consume();
            } else if (!indent.isEmpty()) {
                codeArea.insertText(caret, "\n" + indent);
                event.consume();
            }
        } else if (event.getCode() == KeyCode.TAB) {
            if (event.isShiftDown()) {
                // Outdent block
                int start = codeArea.getSelection().getStart();
                int end = codeArea.getSelection().getEnd();
                if (start != end) {
                    String selected = codeArea.getSelectedText();
                    String outdented = indentService.outdentBlock(selected, 4, true);
                    codeArea.replaceSelection(outdented);
                    event.consume();
                }
            } else {
                int start = codeArea.getSelection().getStart();
                int end = codeArea.getSelection().getEnd();
                if (start != end) {
                    // Indent block
                    String selected = codeArea.getSelectedText();
                    String indented = indentService.indentBlock(selected, 4, true);
                    codeArea.replaceSelection(indented);
                    event.consume();
                } else {
                    // Insert 4 spaces
                    codeArea.insertText(codeArea.getCaretPosition(), "    ");
                    event.consume();
                }
            }
        }
    }

    public void applySyntaxHighlighting() {
        if (tokenizer == null) return;
        String text = codeArea.getText();
        List<Token> tokens = tokenizer.tokenize(text);

        StyleSpans<Collection<String>> spans = computeStyleSpans(tokens, text.length());
        Platform.runLater(() -> {
            try {
                if (codeArea.getLength() == text.length()) {
                    codeArea.setStyleSpans(0, spans);
                }
            } catch (Exception ignored) {}
        });
    }

    private StyleSpans<Collection<String>> computeStyleSpans(List<Token> tokens, int textLength) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastTokenEnd = 0;

        for (Token token : tokens) {
            if (token.start() > lastTokenEnd) {
                spansBuilder.add(Collections.emptyList(), token.start() - lastTokenEnd);
            }
            spansBuilder.add(Collections.singleton(token.getCssClass()), token.length());
            lastTokenEnd = token.end();
        }

        if (textLength > lastTokenEnd) {
            spansBuilder.add(Collections.emptyList(), textLength - lastTokenEnd);
        }

        return spansBuilder.create();
    }

    public void zoomIn() {
        fontSize = Math.min(48, fontSize + 1);
        codeArea.setStyle("-fx-font-size: " + fontSize + "px;");
    }

    public void zoomOut() {
        fontSize = Math.max(8, fontSize - 1);
        codeArea.setStyle("-fx-font-size: " + fontSize + "px;");
    }

    public void resetZoom() {
        fontSize = 14;
        codeArea.setStyle("-fx-font-size: " + fontSize + "px;");
    }

    public void setWordWrap(boolean wrap) {
        codeArea.setWrapText(wrap);
    }

    public void setMinimapVisible(boolean visible) {
        minimapView.setVisible(visible);
        minimapView.setManaged(visible);
    }

    public void setLineNumbersVisible(boolean visible) {
        codeArea.setParagraphGraphicFactory(visible ? LineNumberFactory.get(codeArea) : null);
    }

    public Document getDocument() {
        return document;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    public void setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        applySyntaxHighlighting();
    }

    public String getTitle() {
        Path p = document.getFilePath();
        return p != null ? p.getFileName().toString() : "untitled";
    }

    public void updateFilePath(Path path) {
        document.setFilePath(path);
        breadcrumbBar.updatePath(path);
        this.tokenizer = TokenizerRegistry.detect(path);
        applySyntaxHighlighting();
    }

    public int getFontSize() {
        return fontSize;
    }
}
