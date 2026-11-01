package com.novaedit.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast regex lexer for JSON.
 */
public class JsonLexer implements Tokenizer {

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEY>\"([^\"\\\\]|\\\\.)*\"\\s*(?=:))"
                    + "|(?<STRING>\"([^\"\\\\]|\\\\.)*\")"
                    + "|(?<KEYWORD>\\b(true|false|null)\\b)"
                    + "|(?<NUMBER>-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?)"
                    + "|(?<PUNCTUATION>[{}\\[\\],:])"
    );

    @Override
    public String getLanguageId() {
        return "json";
    }

    @Override
    public String getDisplayName() {
        return "JSON";
    }

    @Override
    public List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            TokenType type;
            if (matcher.group("KEY") != null) {
                type = TokenType.ATTRIBUTE; // JSON keys styled distinctively
            } else if (matcher.group("STRING") != null) {
                type = TokenType.STRING;
            } else if (matcher.group("KEYWORD") != null) {
                type = TokenType.KEYWORD;
            } else if (matcher.group("NUMBER") != null) {
                type = TokenType.NUMBER;
            } else if (matcher.group("PUNCTUATION") != null) {
                type = TokenType.PUNCTUATION;
            } else {
                type = TokenType.PLAIN;
            }
            tokens.add(new Token(matcher.start(), matcher.end(), type));
        }
        return tokens;
    }
}
