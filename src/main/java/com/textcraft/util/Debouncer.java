package com.textcraft.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Runs a task only after calls have stopped for a while (used for the live word count). */
public final class Debouncer {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "debouncer");
        t.setDaemon(true);
        return t;
    });
    private final long delayMs;
    private ScheduledFuture<?> pending;

    public Debouncer(long delayMs) {
        this.delayMs = delayMs;
    }

    public synchronized void call(Runnable task) {
        if (pending != null) pending.cancel(false);
        pending = scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }
}
