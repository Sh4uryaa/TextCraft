package com.textcraft.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

/**
 * One shared pool of 4 daemon threads for all network / DB / file / audio work.
 * The Swing EDT only draws the UI; results come back with invokeLater.
 * OOP: Multithreading.
 */
public final class AppExecutors {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "textcraft-worker-" + COUNTER.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private AppExecutors() { }

    public static ExecutorService io() {
        return POOL;
    }

    /** Run work off the EDT; deliver the Result on the EDT. Exceptions become error Results. */
    public static <T> void async(Supplier<T> work, Consumer<Result<T>> done) {
        POOL.submit(() -> {
            Result<T> r;
            try {
                r = Result.ok(work.get());
            } catch (RuntimeException e) {
                r = Result.fail(e.getMessage() == null ? e.toString() : e.getMessage());
            }
            Result<T> fin = r;
            SwingUtilities.invokeLater(() -> done.accept(fin));
        });
    }

    /** Checked-exception friendly version of the work to run. */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static <T> void asyncChecked(ThrowingSupplier<T> work, Consumer<Result<T>> done) {
        POOL.submit(() -> {
            Result<T> r;
            try {
                r = Result.ok(work.get());
            } catch (Exception e) {
                r = Result.fail(e.getMessage() == null ? e.toString() : e.getMessage());
            }
            Result<T> fin = r;
            SwingUtilities.invokeLater(() -> done.accept(fin));
        });
    }
}
