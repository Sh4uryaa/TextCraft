package com.novaedit.syntax;

import java.util.Collections;
import java.util.List;

/**
 * Plain text fallback lexer (no syntax styling).
 */
public class PlainLexer implements Tokenizer {

    @Override
    public String getLanguageId() {
        return "plain";
    }

    @Override
    public String getDisplayName() {
        return "Plain Text";
    }

    @Override
    public List<Token> tokenize(String text) {
        return Collections.emptyList();
    }
}
