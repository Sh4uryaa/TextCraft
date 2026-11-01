package com.novaedit.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Debounces frequent actions (e.g. typing, re-highlighting, autosave)
 * by delaying execution until a quiet period has passed.
 */
public class Debouncer {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "NovaEdit-Debouncer");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> scheduledFuture;

    public synchronized void debounce(Runnable task, long delayMs) {
        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void cancel() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    public synchronized void shutdown() {
        cancel();
        scheduler.shutdown();
    }
}
