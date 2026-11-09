package com.textcraft.app;

import com.textcraft.service.SettingsService;
import com.textcraft.ui.MainFrame;
import com.textcraft.ui.Theme;
import com.textcraft.ui.ThemeManager;
import javax.swing.SwingUtilities;

/**
 * Startup: load config, apply the saved theme, show the window. Database, Google login and
 * every heavy panel are started afterwards in the background, so the window appears quickly.
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        AppConfig.get(); // load config once
        SwingUtilities.invokeLater(() -> {
            Theme theme;
            try {
                // saved choice first (local settings file), then the config default
                String saved = SettingsService.getInstance().get().getTheme();
                theme = Theme.valueOf(AppConfig.get().get("ui.theme", saved).toUpperCase());
            } catch (IllegalArgumentException e) {
                theme = Theme.LIGHT;
            }
            ThemeManager.getInstance().apply(theme);
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            frame.startBackground();
        });
    }
}
