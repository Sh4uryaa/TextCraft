package com.textcraft.ui;

import java.awt.Color;

/**
 * Immutable set of colours for one theme.
 * OOP: Encapsulation - final fields, no setters.
 */
public final class Palette {
    public final Color workspace, surface, page, border, text, text2, accent, selection, ok, warn, error;

    public Palette(String workspace, String surface, String page, String border, String text, String text2,
                   String accent, String selection, String ok, String warn, String error) {
        this.workspace = Color.decode(workspace);
        this.surface = Color.decode(surface);
        this.page = Color.decode(page);
        this.border = Color.decode(border);
        this.text = Color.decode(text);
        this.text2 = Color.decode(text2);
        this.accent = Color.decode(accent);
        this.selection = Color.decode(selection);
        this.ok = Color.decode(ok);
        this.warn = Color.decode(warn);
        this.error = Color.decode(error);
    }
}
