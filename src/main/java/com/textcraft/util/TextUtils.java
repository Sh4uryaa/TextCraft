package com.textcraft.util;

/** Small text helpers (word count etc.). */
public final class TextUtils {
    private TextUtils() { }

    /** Counts words without regex/allocation so it is fast on old PCs. */
    public static int wordCount(String text) {
        if (text == null) return 0;
        int count = 0;
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            boolean space = Character.isWhitespace(text.charAt(i));
            if (!space && !inWord) count++;
            inWord = !space;
        }
        return count;
    }

    public static int paragraphCount(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        int n = 0;
        for (String p : text.split("\n")) {
            if (!p.trim().isEmpty()) n++;
        }
        return n;
    }

    public static int charsWithoutSpaces(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) n++;
        }
        return n;
    }

    /** Lower-case file extension without dot, "" if none. */
    public static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    public static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? fileName : fileName.substring(0, dot);
    }

    public static String escapeHtml(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /** CSV cell: wrap in quotes and double inner quotes. */
    public static String csv(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }
}
