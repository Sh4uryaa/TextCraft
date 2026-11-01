package com.novaedit.workspace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages restoring open tabs and cursor states across app restarts.
 */
public class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final Path SESSION_FILE = Path.of(System.getProperty("user.home"), ".novaedit", "session.json");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TabSession {
        public String filePath;
        public int caretOffset;
        public double scrollY;

        public TabSession() {}

        public TabSession(String filePath, int caretOffset, double scrollY) {
            this.filePath = filePath;
            this.caretOffset = caretOffset;
            this.scrollY = scrollY;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionData {
        public List<TabSession> tabs = new ArrayList<>();
        public int activeTabIndex = 0;
        public String workspacePath;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public synchronized SessionData loadSession() {
        if (Files.exists(SESSION_FILE)) {
            try {
                return mapper.readValue(SESSION_FILE.toFile(), SessionData.class);
            } catch (IOException e) {
                log.warn("Failed to load session from {}: {}", SESSION_FILE, e.getMessage());
            }
        }
        return new SessionData();
    }

    public synchronized void saveSession(SessionData data) {
        if (data == null) return;
        try {
            if (!Files.exists(SESSION_FILE.getParent())) {
                Files.createDirectories(SESSION_FILE.getParent());
            }
            mapper.writeValue(SESSION_FILE.toFile(), data);
            log.info("Session state saved successfully.");
        } catch (IOException e) {
            log.error("Failed to save session to {}", SESSION_FILE, e);
        }
    }

    public Path getSessionFile() {
        return SESSION_FILE;
    }
}
