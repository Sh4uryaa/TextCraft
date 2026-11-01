package com.novaedit.features.format;

import java.util.Map;

/**
 * Finds matching pairs of parenthesis, brackets, and braces.
 */
public class BracketMatcher {

    private static final Map<Character, Character> OPEN_TO_CLOSE = Map.of(
            '(', ')',
            '{', '}',
            '[', ']',
            '<', '>'
    );

    private static final Map<Character, Character> CLOSE_TO_OPEN = Map.of(
            ')', '(',
            '}', '{',
            ']', '[',
            '>', '<'
    );

    /**
     * Given an offset in text, finds the index of the matching bracket if the char at or before offset is a bracket.
     * Returns -1 if no matching bracket is found.
     */
    public int findMatchingBracket(String text, int offset) {
        if (text == null || text.isEmpty()) return -1;

        // Check character right before offset or at offset
        int pos = -1;
        if (offset > 0 && isBracket(text.charAt(offset - 1))) {
            pos = offset - 1;
        } else if (offset >= 0 && offset < text.length() && isBracket(text.charAt(offset))) {
            pos = offset;
        }

        if (pos == -1) return -1;

        char c = text.charAt(pos);
        if (OPEN_TO_CLOSE.containsKey(c)) {
            char target = OPEN_TO_CLOSE.get(c);
            int depth = 1;
            for (int i = pos + 1; i < text.length(); i++) {
                char curr = text.charAt(i);
                if (curr == c) {
                    depth++;
                } else if (curr == target) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        } else if (CLOSE_TO_OPEN.containsKey(c)) {
            char target = CLOSE_TO_OPEN.get(c);
            int depth = 1;
            for (int i = pos - 1; i >= 0; i--) {
                char curr = text.charAt(i);
                if (curr == c) {
                    depth++;
                } else if (curr == target) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    public boolean isBracket(char c) {
        return OPEN_TO_CLOSE.containsKey(c) || CLOSE_TO_OPEN.containsKey(c);
    }
}
