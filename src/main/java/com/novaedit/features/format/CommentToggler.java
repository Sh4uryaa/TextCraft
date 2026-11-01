package com.novaedit.features.format;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Service for toggling comments and transforming lines (duplicate, move, delete, sort, trim, case).
 */
public class CommentToggler {

    /**
     * Toggles line comment (e.g. "//" or "#") on the given lines.
     */
    public String toggleLineComment(String text, String prefix) {
        if (text == null || text.isEmpty()) return text;
        String p = prefix != null ? prefix.trim() : "//";
        String[] lines = text.split("\n", -1);

        // Check if all non-empty lines are already commented
        boolean allCommented = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith(p)) {
                allCommented = false;
                break;
            }
        }

        if (allCommented) {
            // Uncomment
            return Arrays.stream(lines).map(line -> {
                int idx = line.indexOf(p);
                if (idx >= 0) {
                    int end = idx + p.length();
                    if (end < line.length() && line.charAt(end) == ' ') {
                        end++;
                    }
                    return line.substring(0, idx) + line.substring(end);
                }
                return line;
            }).collect(Collectors.joining("\n"));
        } else {
            // Comment
            return Arrays.stream(lines).map(line -> {
                if (line.trim().isEmpty()) return line;
                return p + " " + line;
            }).collect(Collectors.joining("\n"));
        }
    }

    /**
     * Toggles block comment (e.g. "/*" and "*\/").
     */
    public String toggleBlockComment(String text, String startMarker, String endMarker) {
        if (text == null || text.isEmpty()) return text;
        String s = startMarker != null ? startMarker : "/*";
        String e = endMarker != null ? endMarker : "*/";
        String trimmed = text.trim();

        if (trimmed.startsWith(s) && trimmed.endsWith(e)) {
            // Remove block comment
            int startIdx = text.indexOf(s);
            int endIdx = text.lastIndexOf(e);
            return text.substring(0, startIdx) + text.substring(startIdx + s.length(), endIdx).trim() + text.substring(endIdx + e.length());
        } else {
            return s + " " + text + " " + e;
        }
    }

    public String sortLines(String text, boolean ascending) {
        if (text == null || text.isEmpty()) return text;
        String[] lines = text.split("\n", -1);
        Comparator<String> comp = String.CASE_INSENSITIVE_ORDER;
        if (!ascending) comp = comp.reversed();
        Arrays.sort(lines, comp);
        return String.join("\n", lines);
    }

    public String trimTrailingWhitespace(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] lines = text.split("\n", -1);
        return Arrays.stream(lines)
                .map(line -> line.replaceAll("\\s+$", ""))
                .collect(Collectors.joining("\n"));
    }
}
