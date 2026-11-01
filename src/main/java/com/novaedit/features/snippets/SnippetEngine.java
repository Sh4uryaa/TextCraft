package com.novaedit.features.snippets;

import java.util.*;

/**
 * Manages built-in and user snippets across languages.
 */
public class SnippetEngine {
    private final Map<String, List<Snippet>> snippetsByLanguage = new HashMap<>();

    public SnippetEngine() {
        registerDefaults();
    }

    private void registerDefaults() {
        // Java snippets
        register("java", new Snippet("sout", "sout", "System.out.println($0);", "Prints to standard output"));
        register("java", new Snippet("psvm", "psvm", "public static void main(String[] args) {\n    $0\n}", "Main method declaration"));
        register("java", new Snippet("fori", "fori", "for (int i = 0; i < $1; i++) {\n    $0\n}", "Indexed for loop"));
        register("java", new Snippet("iff", "iff", "if ($1) {\n    $0\n}", "If condition block"));
        register("java", new Snippet("tryc", "tryc", "try {\n    $0\n} catch (Exception e) {\n    e.printStackTrace();\n}", "Try-catch block"));

        // Markdown snippets
        register("markdown", new Snippet("link", "link", "[$1]($0)", "Markdown hyperlink"));
        register("markdown", new Snippet("img", "img", "![$1]($0)", "Markdown image"));
        register("markdown", new Snippet("code", "code", "```$1\n$0\n```", "Code block"));

        // HTML / XML snippets
        register("xml", new Snippet("html5", "html5", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <title>$1</title>\n</head>\n<body>\n    $0\n</body>\n</html>", "HTML5 starter document"));
    }

    public synchronized void register(String language, Snippet snippet) {
        snippetsByLanguage.computeIfAbsent(language.toLowerCase(), k -> new ArrayList<>()).add(snippet);
    }

    public synchronized List<Snippet> getSnippetsForLanguage(String language) {
        if (language == null) return Collections.emptyList();
        return snippetsByLanguage.getOrDefault(language.toLowerCase(), Collections.emptyList());
    }

    public synchronized Snippet findMatchingSnippet(String language, String prefix) {
        if (prefix == null || prefix.isEmpty()) return null;
        for (Snippet snippet : getSnippetsForLanguage(language)) {
            if (snippet.prefix().equalsIgnoreCase(prefix)) {
                return snippet;
            }
        }
        return null;
    }
}
