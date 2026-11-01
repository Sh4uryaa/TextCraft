package com.novaedit.io;

import com.novaedit.core.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Periodically backs up dirty open documents to ~/.novaedit/backup/ for crash recovery.
 */
public class AutoSaveService {
    private static final Logger log = LoggerFactory.getLogger(AutoSaveService.class);
    private static final Path BACKUP_DIR = Path.of(System.getProperty("user.home"), ".novaedit", "backup");

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "NovaEdit-AutoSave");
        t.setDaemon(true);
        return t;
    });

    private final Supplier<List<Document>> documentsSupplier;
    private ScheduledFuture<?> scheduledTask;

    public AutoSaveService(Supplier<List<Document>> documentsSupplier) {
        this.documentsSupplier = documentsSupplier;
    }

    public synchronized void start(int intervalSeconds) {
        stop();
        scheduledTask = scheduler.scheduleWithFixedDelay(this::performBackup,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
    }

    public synchronized void shutdown() {
        stop();
        scheduler.shutdown();
    }

    private void performBackup() {
        if (documentsSupplier == null) return;
        List<Document> docs = documentsSupplier.get();
        if (docs == null || docs.isEmpty()) return;

        try {
            if (!Files.exists(BACKUP_DIR)) {
                Files.createDirectories(BACKUP_DIR);
            }

            for (Document doc : docs) {
                if (doc.isDirty()) {
                    String filename = (doc.getFilePath() != null)
                            ? doc.getFilePath().getFileName().toString() + ".bak"
                            : "untitled_" + System.identityHashCode(doc) + ".bak";
                    Path backupPath = BACKUP_DIR.resolve(filename);
                    Files.writeString(backupPath, doc.getText());
                    log.debug("Auto-saved backup for {}", filename);
                }
            }
        } catch (IOException e) {
            log.warn("Auto-save backup failed: {}", e.getMessage());
        }
    }

    public Path getBackupDir() {
        return BACKUP_DIR;
    }
}
