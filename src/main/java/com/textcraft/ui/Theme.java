package com.textcraft.ui;

/** OOP: Enum - the three theme choices stored per user. */
public enum Theme {
    LIGHT(new Palette("#F1F3F4", "#FFFFFF", "#FFFFFF", "#DADCE0", "#202124", "#5F6368",
            "#1A73E8", "#C2DBFF", "#188038", "#F9AB00", "#D93025")),
    DARK(new Palette("#121316", "#1E1F22", "#1F2023", "#3C4043", "#E8EAED", "#9AA0A6",
            "#8AB4F8", "#2F4F7F", "#81C995", "#FDD663", "#F28B82")),
    /** Follows the operating system; resolved to LIGHT or DARK by ThemeManager. */
    SYSTEM(null);

    private final Palette palette;

    Theme(Palette palette) {
        this.palette = palette;
    }

    /** Palette for LIGHT/DARK; null for SYSTEM (use ThemeManager.palette()). */
    public Palette palette() {
        return palette;
    }
}
