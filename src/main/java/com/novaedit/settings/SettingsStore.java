package com.novaedit.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists and loads Settings from ~/.novaedit/settings.json.
 */
public class SettingsStore {
    private static final Logger log = LoggerFactory.getLogger(SettingsStore.class);
    private static final Path SETTINGS_DIR = Path.of(System.getProperty("user.home"), ".novaedit");
    private static final Path SETTINGS_FILE = SETTINGS_DIR.resolve("settings.json");

    private final ObjectMapper mapper;
    private Settings currentSettings;

    public SettingsStore() {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.currentSettings = load();
    }

    public synchronized Settings getSettings() {
        return currentSettings;
    }

    public synchronized Settings load() {
        if (Files.exists(SETTINGS_FILE)) {
            try {
                currentSettings = mapper.readValue(SETTINGS_FILE.toFile(), Settings.class);
                return currentSettings;
            } catch (IOException e) {
                log.warn("Failed to read settings from {}: {}", SETTINGS_FILE, e.getMessage());
            }
        }
        currentSettings = new Settings();
        return currentSettings;
    }

    public synchronized void save() {
        try {
            if (!Files.exists(SETTINGS_DIR)) {
                Files.createDirectories(SETTINGS_DIR);
            }
            mapper.writeValue(SETTINGS_FILE.toFile(), currentSettings);
            log.info("Settings saved to {}", SETTINGS_FILE);
        } catch (IOException e) {
            log.error("Failed to save settings to {}", SETTINGS_FILE, e);
        }
    }

    public Path getSettingsFile() {
        return SETTINGS_FILE;
    }
}
