package com.textcraft.service;

import com.textcraft.auth.SessionManager;
import com.textcraft.dao.SettingsDAO;
import com.textcraft.db.DatabaseConnection;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.User;
import com.textcraft.model.UserSettings;
import com.textcraft.util.AppPaths;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Settings are always kept in ~/.textcraft/settings.properties (works offline, used at startup)
 * and, for a signed-in user with a reachable database, also in the user_settings table.
 * OOP: Singleton.
 */
public final class SettingsService {
    private static final Logger LOG = Logger.getLogger(SettingsService.class.getName());
    private static final SettingsService INSTANCE = new SettingsService();

    private UserSettings current = new UserSettings();

    private SettingsService() {
        loadLocal();
    }

    public static SettingsService getInstance() {
        return INSTANCE;
    }

    public UserSettings get() {
        return current;
    }

    /** Called after sign-in: the cloud copy wins over the local one. */
    public void loadForUser(User user) {
        if (user == null || !SessionManager.getInstance().isDatabaseAvailable()) return;
        try {
            Optional<UserSettings> s = new SettingsDAO(DatabaseConnection.getInstance()).findById(user.getId());
            s.ifPresent(v -> current = v);
        } catch (DatabaseException e) {
            LOG.warning("Could not load cloud settings: " + e.getMessage());
        }
    }

    /** Saves locally, and to the cloud when possible. */
    public void save() {
        saveLocal();
        User u = SessionManager.getInstance().getUser();
        if (u != null && SessionManager.getInstance().isDatabaseAvailable()) {
            current.setUserId(u.getId());
            try {
                new SettingsDAO(DatabaseConnection.getInstance()).save(current);
            } catch (DatabaseException e) {
                LOG.warning("Could not save cloud settings: " + e.getMessage());
            }
        }
    }

    private void loadLocal() {
        Path f = AppPaths.file("settings.properties");
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            p.load(in);
            current.setTheme(p.getProperty("theme", "LIGHT"));
            current.setDefaultFont(p.getProperty("font", "Arial"));
            current.setDefaultFontSize(Integer.parseInt(p.getProperty("fontSize", "12")));
            current.setGeminiModel(p.getProperty("geminiModel", current.getGeminiModel()));
            current.setVoiceLanguage(p.getProperty("voiceLanguage", "en-US"));
            current.setVoiceSpeed(Double.parseDouble(p.getProperty("voiceSpeed", "1.0")));
            current.setLiteMode(Boolean.parseBoolean(p.getProperty("lite", "false")));
        } catch (IOException | NumberFormatException e) {
            LOG.warning("Could not read local settings: " + e.getMessage());
        }
    }

    private void saveLocal() {
        Properties p = new Properties();
        p.setProperty("theme", current.getTheme());
        p.setProperty("font", current.getDefaultFont());
        p.setProperty("fontSize", String.valueOf(current.getDefaultFontSize()));
        p.setProperty("geminiModel", current.getGeminiModel() == null ? "" : current.getGeminiModel());
        p.setProperty("voiceLanguage", current.getVoiceLanguage());
        p.setProperty("voiceSpeed", String.valueOf(current.getVoiceSpeed()));
        p.setProperty("lite", String.valueOf(current.isLiteMode()));
        try (OutputStream out = Files.newOutputStream(AppPaths.file("settings.properties"))) {
            p.store(out, "TextCraft local settings");
        } catch (IOException e) {
            LOG.warning("Could not save local settings: " + e.getMessage());
        }
    }
}
