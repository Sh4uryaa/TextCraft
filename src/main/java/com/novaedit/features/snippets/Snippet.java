package com.novaedit.features.snippets;

/**
 * Code snippet template with trigger prefix and cursor insertion template.
 */
public record Snippet(String prefix, String label, String template, String description) {

    /**
     * Resolves the template into the inserted text, stripping placeholder markers like $0 and $1.
     */
    public String resolve() {
        return template.replace("$0", "").replace("$1", "");
    }
}
