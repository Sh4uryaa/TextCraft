package com.novaedit.syntax;

/**
 * Standard syntax token categories for syntax highlighting and themes.
 */
public enum TokenType {
    KEYWORD("token-keyword"),
    TYPE("token-type"),
    STRING("token-string"),
    NUMBER("token-number"),
    COMMENT("token-comment"),
    OPERATOR("token-operator"),
    PUNCTUATION("token-punctuation"),
    IDENTIFIER("token-identifier"),
    TAG("token-tag"),
    ATTRIBUTE("token-attribute"),
    HEADING("token-heading"),
    BOLD("token-bold"),
    ITALIC("token-italic"),
    LINK("token-link"),
    PLAIN("token-plain");

    private final String cssClass;

    TokenType(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCssClass() {
        return cssClass;
    }
}
