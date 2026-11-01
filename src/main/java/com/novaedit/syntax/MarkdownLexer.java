package com.novaedit.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast regex lexer for Markdown documents.
 */
public class MarkdownLexer implements Tokenizer {

    private static final Pattern PATTERN = Pattern.compile(
            "(?<HEADING>^#{1,6}\\s+[^\n]+)"
                    + "|(?<CODEBLOCK>```[\\s\\S]*?```)"
                    + "|(?<INLINECODE>`[^`\n]+`)"
                    + "|(?<BOLD>(\\*\\*|__)[^\\*\n]+?(\\*\\*|__))"
                    + "|(?<ITALIC>(\\*|_)[^\\*\n]+?(\\*|_))"
                    + "|(?<LINK>\\[[^\\]]+\\]\\([^\\)]+\\))"
                    + "|(?<BLOCKQUOTE>^>\\s+[^\n]+)",
            Pattern.MULTILINE
    );

    @Override
    public String getLanguageId() {
        return "markdown";
    }

    @Override
    public String getDisplayName() {
        return "Markdown";
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
            if (matcher.group("HEADING") != null) {
                type = TokenType.HEADING;
            } else if (matcher.group("CODEBLOCK") != null || matcher.group("INLINECODE") != null) {
                type = TokenType.TYPE;
            } else if (matcher.group("BOLD") != null) {
                type = TokenType.BOLD;
            } else if (matcher.group("ITALIC") != null) {
                type = TokenType.ITALIC;
            } else if (matcher.group("LINK") != null) {
                type = TokenType.LINK;
            } else if (matcher.group("BLOCKQUOTE") != null) {
                type = TokenType.COMMENT;
            } else {
                type = TokenType.PLAIN;
            }
            tokens.add(new Token(matcher.start(), matcher.end(), type));
        }
        return tokens;
    }
}
