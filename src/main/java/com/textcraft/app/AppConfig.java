package com.textcraft.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Loads config/app.properties once. Missing file is fine (defaults are used),
 * so the app still starts in offline mode with no secrets.
 * OOP: Singleton.
 */
public final class AppConfig {
    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());
    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties props = new Properties();

    private AppConfig() {
        Path file = Paths.get("config", "app.properties");
        if (Files.exists(file)) {
            // OOP: try-with-resources closes the stream even on error.
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                LOG.warning("Could not read " + file + ": " + e.getMessage());
            }
        } else {
            LOG.info("No config/app.properties found; using defaults (offline mode).");
        }
    }

    public static AppConfig get() {
        return INSTANCE;
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
