package com.textcraft.service;

import com.textcraft.util.AppExecutors;
import com.textcraft.util.AppPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Writes a local draft copy every 30 seconds while the document has unsaved changes.
 * A draft is NOT a save: it only protects against a crash, and is offered at the next start.
 * OOP: Multithreading (ScheduledExecutorService).
 */
public class AutoSaveService {
    private static final Logger LOG = Logger.getLogger(AutoSaveService.class.getName());
    private static final long PERIOD_SECONDS = 30;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "autosave");
        t.setDaemon(true);
        return t;
    });

    /** Text to store (taken on the EDT), plus a short description to show when restoring. */
    public static class Snapshot {
        public final String content;
        public final String title;
        public final boolean code;

        public Snapshot(String content, String title, boolean code) {
            this.content = content;
            this.title = title;
            this.code = code;
        }
    }

    /**
     * @param dirty    asks the UI (on the EDT) whether there are unsaved changes
     * @param snapshot builds the draft content (on the EDT)
     * @param onSaved  called on the EDT with the time of day, for the status bar
     */
    public void start(Supplier<Boolean> dirty, Supplier<Snapshot> snapshot, Consumer<String> onSaved) {
        scheduler.scheduleWithFixedDelay(() -> SwingUtilities.invokeLater(() -> {
            if (!dirty.get()) return;
            Snapshot s = snapshot.get();
            AppExecutors.io().submit(() -> {
                try {
                    write(s);
                    String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    SwingUtilities.invokeLater(() -> onSaved.accept(time));
                } catch (IOException e) {
                    LOG.warning("Autosave failed: " + e.getMessage());
                }
            });
        }), PERIOD_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private static Path contentFile() {
        return AppPaths.file("draft.txt");
    }

    private static Path metaFile() {
        return AppPaths.file("draft.meta");
    }

    private static void write(Snapshot s) throws IOException {
        Files.write(contentFile(), s.content.getBytes(StandardCharsets.UTF_8));
        Files.write(metaFile(), (s.title.replace('\n', ' ') + "\n" + s.code).getBytes(StandardCharsets.UTF_8));
    }

    public static Optional<Snapshot> readDraft() {
        try {
            if (!Files.exists(contentFile()) || !Files.exists(metaFile())) return Optional.empty();
            String content = new String(Files.readAllBytes(contentFile()), StandardCharsets.UTF_8);
            String[] meta = new String(Files.readAllBytes(metaFile()), StandardCharsets.UTF_8).split("\n", 2);
            if (content.trim().isEmpty()) return Optional.empty();
            return Optional.of(new Snapshot(content, meta[0], meta.length > 1 && Boolean.parseBoolean(meta[1].trim())));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /** Remove the draft after a normal save or a clean exit. */
    public static void clearDraft() {
        try {
            Files.deleteIfExists(contentFile());
            Files.deleteIfExists(metaFile());
        } catch (IOException e) {
            LOG.warning("Could not delete the draft: " + e.getMessage());
        }
    }
}
