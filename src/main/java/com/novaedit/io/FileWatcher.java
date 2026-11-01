package com.novaedit.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Monitors active files for external disk modifications using WatchService on a virtual thread.
 */
public class FileWatcher {
    private static final Logger log = LoggerFactory.getLogger(FileWatcher.class);

    private WatchService watchService;
    private final Map<WatchKey, Path> watchKeyToDir = new ConcurrentHashMap<>();
    private final Map<Path, Consumer<Path>> fileCallbacks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean running = false;

    public synchronized void start() {
        if (running) return;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            running = true;
            executor.submit(this::watchLoop);
        } catch (IOException e) {
            log.warn("Failed to initialize WatchService: {}", e.getMessage());
        }
    }

    public synchronized void watchFile(Path file, Consumer<Path> onExternalChange) {
        if (file == null || !Files.exists(file) || onExternalChange == null) return;
        Path abs = file.toAbsolutePath();
        Path dir = abs.getParent();
        if (dir == null) return;

        fileCallbacks.put(abs, onExternalChange);

        if (watchService != null) {
            try {
                WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                watchKeyToDir.put(key, dir);
            } catch (IOException e) {
                log.debug("Failed to register watch for directory {}: {}", dir, e.getMessage());
            }
        }
    }

    public synchronized void unwatchFile(Path file) {
        if (file != null) {
            fileCallbacks.remove(file.toAbsolutePath());
        }
    }

    private void watchLoop() {
        while (running && watchService != null) {
            try {
                WatchKey key = watchService.take();
                Path dir = watchKeyToDir.get(key);
                if (dir != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Path filename = (Path) event.context();
                            Path modifiedFile = dir.resolve(filename);
                            Consumer<Path> callback = fileCallbacks.get(modifiedFile);
                            if (callback != null) {
                                callback.accept(modifiedFile);
                            }
                        }
                    }
                }
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }
        }
    }

    public synchronized void stop() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
        executor.shutdown();
    }
}
