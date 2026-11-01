package com.novaedit.features.search;

/**
 * Search options configuring case sensitivity, whole word, and regex matching.
 */
public record FindOptions(String query, boolean matchCase, boolean wholeWord, boolean useRegex) {

    public static FindOptions of(String query) {
        return new FindOptions(query, false, false, false);
    }
}
