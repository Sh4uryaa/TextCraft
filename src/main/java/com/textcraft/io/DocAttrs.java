package com.textcraft.io;

import javax.swing.text.AttributeSet;

/**
 * Names of the paragraph styles and the custom attribute that stores them.
 * Character formatting uses the standard javax.swing.text.StyleConstants keys.
 */
public final class DocAttrs {
    /** Paragraph attribute key holding one of the style names below. */
    public static final String STYLE_KEY = "tcStyle";

    public static final String NORMAL = "Normal";
    public static final String TITLE = "Title";
    public static final String H1 = "Heading 1";
    public static final String H2 = "Heading 2";
    public static final String H3 = "Heading 3";

    public static final int DEFAULT_SIZE = 12;
    public static final String DEFAULT_FONT = "Arial";

    private DocAttrs() { }

    public static String styleOf(AttributeSet paragraphAttrs) {
        Object v = paragraphAttrs.getAttribute(STYLE_KEY);
        return v == null ? NORMAL : v.toString();
    }

    public static int sizeFor(String style) {
        switch (style) {
            case TITLE: return 26;
            case H1: return 20;
            case H2: return 16;
            case H3: return 14;
            default: return DEFAULT_SIZE;
        }
    }

    public static boolean boldFor(String style) {
        return H1.equals(style) || H2.equals(style) || H3.equals(style);
    }
}
