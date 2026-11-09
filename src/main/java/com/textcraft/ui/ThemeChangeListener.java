package com.textcraft.ui;

/** OOP: Observer - UI parts register to be told when the theme changes. */
public interface ThemeChangeListener {
    void onThemeChanged(Theme resolvedTheme, Palette palette);
}
