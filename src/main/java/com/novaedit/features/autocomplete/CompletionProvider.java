package com.novaedit.features.autocomplete;

import com.novaedit.core.document.Document;
import com.novaedit.features.snippets.Snippet;
import com.novaedit.features.snippets.SnippetEngine;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides autocompletions dynamically combining language keywords,
 * active snippets, and identifiers extracted from the document.
 */
public class CompletionProvider {
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]{2,}\\b");

    private final SnippetEngine snippetEngine;

    public CompletionProvider(SnippetEngine snippetEngine) {
        this.snippetEngine = snippetEngine;
    }

    public List<CompletionItem> getCompletions(Document document, int caretOffset, String language) {
        if (document == null) return Collections.emptyList();

        String prefix = extractPrefix(document, caretOffset);
        if (prefix.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletionItem> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Snippets
        if (snippetEngine != null) {
            for (Snippet snippet : snippetEngine.getSnippetsForLanguage(language)) {
                if (snippet.prefix().toLowerCase().startsWith(prefix.toLowerCase())) {
                    results.add(new CompletionItem(
                            snippet.label(),
                            snippet.resolve(),
                            snippet.description(),
                            CompletionItem.Kind.SNIPPET
                    ));
                    seen.add(snippet.label());
                }
            }
        }

        // 2. Document words
        String fullText = document.getText();
        Matcher matcher = WORD_PATTERN.matcher(fullText);
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() > prefix.length() &&
                word.toLowerCase().startsWith(prefix.toLowerCase()) &&
                !seen.contains(word)) {
                results.add(new CompletionItem(
                        word,
                        word,
                        "Identifier in document",
                        CompletionItem.Kind.WORD
                ));
                seen.add(word);
                if (results.size() > 50) break; // limit to 50 results
            }
        }

        results.sort(Comparator.comparing(CompletionItem::label));
        return results;
    }

    public String extractPrefix(Document document, int caretOffset) {
        if (document == null || caretOffset <= 0 || caretOffset > document.length()) {
            return "";
        }
        int start = caretOffset;
        while (start > 0) {
            char c = document.charAt(start - 1);
            if (Character.isJavaIdentifierPart(c)) {
                start--;
            } else {
                break;
            }
        }
        return document.slice(start, caretOffset);
    }
}
