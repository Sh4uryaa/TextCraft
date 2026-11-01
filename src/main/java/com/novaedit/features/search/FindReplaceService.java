package com.novaedit.features.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High performance search and replace service supporting literal, case-sensitive,
 * whole-word, and regex pattern matching.
 */
public class FindReplaceService {

    public List<SearchResult> findAll(String text, FindOptions options) {
        if (text == null || text.isEmpty() || options == null || options.query() == null || options.query().isEmpty()) {
            return Collections.emptyList();
        }

        Pattern pattern = buildPattern(options);
        if (pattern == null) {
            return Collections.emptyList();
        }

        List<SearchResult> results = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);

        // Track lines efficiently
        int currentLine = 0;
        int lastLineOffset = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // Count lines up to start
            for (int i = lastLineOffset; i < start; i++) {
                if (text.charAt(i) == '\n') {
                    currentLine++;
                }
            }
            lastLineOffset = start;

            results.add(new SearchResult(start, end, currentLine, matcher.group()));
        }

        return results;
    }

    public SearchResult findNext(String text, int fromOffset, FindOptions options) {
        List<SearchResult> all = findAll(text, options);
        if (all.isEmpty()) {
            return null;
        }

        for (SearchResult res : all) {
            if (res.start() >= fromOffset) {
                return res;
            }
        }
        // Wrap around to first match
        return all.get(0);
    }

    public SearchResult findPrevious(String text, int fromOffset, FindOptions options) {
        List<SearchResult> all = findAll(text, options);
        if (all.isEmpty()) {
            return null;
        }

        for (int i = all.size() - 1; i >= 0; i--) {
            SearchResult res = all.get(i);
            if (res.start() < fromOffset) {
                return res;
            }
        }
        // Wrap around to last match
        return all.get(all.size() - 1);
    }

    public String replaceAll(String text, FindOptions options, String replacement) {
        if (text == null || options == null || options.query() == null || options.query().isEmpty()) {
            return text;
        }
        Pattern pattern = buildPattern(options);
        if (pattern == null) {
            return text;
        }
        String rep = replacement != null ? replacement : "";
        if (!options.useRegex()) {
            rep = Matcher.quoteReplacement(rep);
        }
        return pattern.matcher(text).replaceAll(rep);
    }

    public Pattern buildPattern(FindOptions options) {
        try {
            int flags = 0;
            if (!options.matchCase()) {
                flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            }

            String regex = options.useRegex() ? options.query() : Pattern.quote(options.query());
            if (options.wholeWord()) {
                regex = "\\b" + regex + "\\b";
            }
            return Pattern.compile(regex, flags);
        } catch (Exception e) {
            return null;
        }
    }
}
