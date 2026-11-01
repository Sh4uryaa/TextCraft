package com.novaedit.features.format;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Service for auto-indentation, smart Enter between braces, and block indent/outdent.
 */
public class IndentService {

    public String getLineIndentation(String line) {
        if (line == null) return "";
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return line.substring(0, i);
    }

    public String createIndentString(int tabSize, boolean useSpaces) {
        if (useSpaces) {
            return " ".repeat(Math.max(1, tabSize));
        } else {
            return "\t";
        }
    }

    /**
     * Computes the indentation for a new line following previousLine.
     * Increases indentation if the previous line opens a block with '{', '(', or '['.
     */
    public String computeNewLineIndent(String previousLine, int tabSize, boolean useSpaces) {
        if (previousLine == null) return "";
        String baseIndent = getLineIndentation(previousLine);
        String trimmed = previousLine.trim();

        if (trimmed.endsWith("{") || trimmed.endsWith("(") || trimmed.endsWith("[")) {
            return baseIndent + createIndentString(tabSize, useSpaces);
        }
        return baseIndent;
    }

    /**
     * Checks if pressing Enter between open and close braces (e.g. { | })
     * should expand to two lines with nested indentation.
     */
    public boolean isBetweenBraces(char before, char after) {
        return (before == '{' && after == '}') ||
               (before == '(' && after == ')') ||
               (before == '[' && after == ']');
    }

    /**
     * Indents a block of lines.
     */
    public String indentBlock(String text, int tabSize, boolean useSpaces) {
        if (text == null || text.isEmpty()) return text;
        String indent = createIndentString(tabSize, useSpaces);
        String[] lines = text.split("\n", -1);
        return Arrays.stream(lines)
                .map(line -> line.isEmpty() ? line : indent + line)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Outdents a block of lines.
     */
    public String outdentBlock(String text, int tabSize, boolean useSpaces) {
        if (text == null || text.isEmpty()) return text;
        String[] lines = text.split("\n", -1);
        return Arrays.stream(lines)
                .map(line -> {
                    if (line.startsWith("\t")) {
                        return line.substring(1);
                    }
                    int spacesToRemove = 0;
                    while (spacesToRemove < tabSize && spacesToRemove < line.length() && line.charAt(spacesToRemove) == ' ') {
                        spacesToRemove++;
                    }
                    return line.substring(spacesToRemove);
                })
                .collect(Collectors.joining("\n"));
    }
}
