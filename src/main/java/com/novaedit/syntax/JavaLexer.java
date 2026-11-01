package com.novaedit.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance regex-based lexer for Java 21 syntax.
 */
public class JavaLexer implements Tokenizer {

    private static final String[] KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "record", "yield", "sealed", "non-sealed",
            "permits", "var", "true", "false", "null"
    };

    private static final String[] COMMON_TYPES = {
            "String", "Integer", "Long", "Double", "Float", "Boolean", "Character",
            "Byte", "Short", "Void", "Object", "System", "List", "Map", "Set",
            "Optional", "StringBuilder", "StringBuffer", "Exception", "RuntimeException",
            "Override", "Deprecated", "SuppressWarnings"
    };

    private static final Pattern PATTERN;

    static {
        String keywordPattern = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
        String typePattern = "\\b(" + String.join("|", COMMON_TYPES) + ")\\b";
        String commentPattern = "(//[^\n]*|/\\*[\\s\\S]*?\\*/)";
        String stringPattern = "(\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*')";
        String numberPattern = "\\b(0[xX][0-9a-fA-F_]+|0[bB][01_]+|[0-9][0-9_]*(\\.[0-9_]+)?([eE][+-]?[0-9_]+)?[fFdDlL]?)\\b";
        String operatorPattern = "([+\\-*/%=&|!<>^~?:])";
        String punctuationPattern = "([;,.()\\[\\]{}])";

        PATTERN = Pattern.compile(
                "(?<COMMENT>" + commentPattern + ")"
                        + "|(?<STRING>" + stringPattern + ")"
                        + "|(?<KEYWORD>" + keywordPattern + ")"
                        + "|(?<TYPE>" + typePattern + ")"
                        + "|(?<NUMBER>" + numberPattern + ")"
                        + "|(?<OPERATOR>" + operatorPattern + ")"
                        + "|(?<PUNCTUATION>" + punctuationPattern + ")"
        );
    }

    @Override
    public String getLanguageId() {
        return "java";
    }

    @Override
    public String getDisplayName() {
        return "Java";
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
            } else if (matcher.group("STRING") != null) {
                type = TokenType.STRING;
            } else if (matcher.group("KEYWORD") != null) {
                type = TokenType.KEYWORD;
            } else if (matcher.group("TYPE") != null) {
                type = TokenType.TYPE;
            } else if (matcher.group("NUMBER") != null) {
                type = TokenType.NUMBER;
            } else if (matcher.group("OPERATOR") != null) {
                type = TokenType.OPERATOR;
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
