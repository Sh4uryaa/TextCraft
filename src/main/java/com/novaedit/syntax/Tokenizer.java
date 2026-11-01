package com.novaedit.syntax;

import java.util.List;

/**
 * Strategy interface for language-specific tokenizers.
 * Core domain interface; must not import JavaFX.
 */
public interface Tokenizer {
    /**
     * Unique language identifier (e.g. "java", "json", "xml", "markdown", "plain").
     */
    String getLanguageId();

    /**
     * Display name for the language (e.g. "Java", "JSON", "XML / HTML", "Markdown", "Plain Text").
     */
    String getDisplayName();

    /**
     * Tokenize the full document text.
     */
    List<Token> tokenize(String text);
}
