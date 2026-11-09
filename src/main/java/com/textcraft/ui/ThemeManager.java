package com.textcraft.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Applies a theme live (no restart) and tells listeners about it.
 * OOP: Singleton + Observer.
 */
public final class ThemeManager {
    private static final ThemeManager INSTANCE = new ThemeManager();

    private final List<ThemeChangeListener> listeners = new ArrayList<>();
    private Theme choice = Theme.LIGHT;      // what the user picked
    private Theme resolved = Theme.LIGHT;    // LIGHT or DARK actually shown

    private ThemeManager() { }

    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    public void addListener(ThemeChangeListener l) {
        listeners.add(l);
    }

    public Theme getChoice() {
        return choice;
    }

    public Theme getResolved() {
        return resolved;
    }

    public Palette palette() {
        return resolved.palette();
    }

    public boolean isDark() {
        return resolved == Theme.DARK;
    }

    /** Switch theme. Safe to call before any window exists (startup) or later (live switch). */
    public void apply(Theme theme) {
        this.choice = theme;
        this.resolved = (theme == Theme.SYSTEM) ? detectSystemTheme() : theme;
        Palette p = palette();

        // Accent colour and rounded corners come from the palette (spec section 3).
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor",
                String.format("#%06X", p.accent.getRGB() & 0xFFFFFF)));
        UIManager.put("defaultFont", new Font(uiFontFamily(), Font.PLAIN, 13));
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);

        if (resolved == Theme.DARK) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        FlatLaf.updateUI(); // refreshes every open window

        // Tell listeners on the EDT so they can recolour their custom components.
        Runnable notify = () -> listeners.forEach(l -> l.onThemeChanged(resolved, p));
        if (SwingUtilities.isEventDispatchThread()) {
            notify.run();
        } else {
            SwingUtilities.invokeLater(notify);
        }
    }

    public void toggleLightDark() {
        apply(isDark() ? Theme.LIGHT : Theme.DARK);
    }

    /**
     * Simple default: the JDK has no portable dark-mode API, so SYSTEM uses LIGHT unless
     * the JVM property "textcraft.systemDark=true" is set. (Noted in README.)
     */
    private Theme detectSystemTheme() {
        return Boolean.getBoolean("textcraft.systemDark") ? Theme.DARK : Theme.LIGHT;
    }

    /** UI font: Inter if installed, else Segoe UI, else the Swing default. */
    static String uiFontFamily() {
        List<String> families = java.util.Arrays.asList(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        if (families.contains("Inter")) return "Inter";
        if (families.contains("Segoe UI")) return "Segoe UI";
        return Font.SANS_SERIF;
    }
}
