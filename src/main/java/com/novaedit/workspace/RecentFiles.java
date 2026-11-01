package com.novaedit.workspace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages recently opened file paths with persistence.
 */
public class RecentFiles {
    private static final Logger log = LoggerFactory.getLogger(RecentFiles.class);
    private static final int MAX_RECENT = 15;
    private static final Path RECENT_FILE = Path.of(System.getProperty("user.home"), ".novaedit", "recent.json");

    private final List<String> recentPaths = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public RecentFiles() {
        load();
    }

    public synchronized void add(Path path) {
        if (path == null) return;
        String p = path.toAbsolutePath().toString();
        recentPaths.remove(p);
        recentPaths.add(0, p);
        while (recentPaths.size() > MAX_RECENT) {
            recentPaths.remove(recentPaths.size() - 1);
        }
        save();
    }

    public synchronized List<String> getRecentFiles() {
        return Collections.unmodifiableList(new ArrayList<>(recentPaths));
    }

    public synchronized void clear() {
        recentPaths.clear();
        save();
    }

    private synchronized void load() {
        if (Files.exists(RECENT_FILE)) {
            try {
                List<String> list = mapper.readValue(RECENT_FILE.toFile(), new TypeReference<>() {});
                recentPaths.clear();
                recentPaths.addAll(list);
            } catch (IOException e) {
                log.warn("Failed to load recent files: {}", e.getMessage());
            }
        }
    }

    private synchronized void save() {
        try {
            if (!Files.exists(RECENT_FILE.getParent())) {
                Files.createDirectories(RECENT_FILE.getParent());
            }
            mapper.writeValue(RECENT_FILE.toFile(), recentPaths);
        } catch (IOException e) {
            log.error("Failed to save recent files: {}", e.getMessage());
        }
    }
}
