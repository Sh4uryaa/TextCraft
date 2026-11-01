package com.novaedit.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast regex lexer for XML and HTML.
 */
public class XmlLexer implements Tokenizer {

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT><!--[\\s\\S]*?-->)"
                    + "|(?<TAG><\\/?[a-zA-Z0-9_:-]+|\\/?>)"
                    + "|(?<ATTR>[a-zA-Z0-9_:-]+(?=\\s*=))"
                    + "|(?<STRING>\"[^\"]*\"|'[^']*')"
                    + "|(?<PUNCTUATION>[=])"
    );

    @Override
    public String getLanguageId() {
        return "xml";
    }

    @Override
    public String getDisplayName() {
        return "XML / HTML";
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
            if (matcher.group("COMMENT") != null) {
                type = TokenType.COMMENT;
            } else if (matcher.group("TAG") != null) {
                type = TokenType.TAG;
            } else if (matcher.group("ATTR") != null) {
                type = TokenType.ATTRIBUTE;
            } else if (matcher.group("STRING") != null) {
                type = TokenType.STRING;
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
