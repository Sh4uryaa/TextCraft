package com.textcraft.ui;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * One place that shows errors: a toast for small things, a dialog for things the user must read.
 * Also catches any exception nobody else handled, so the app never just dies.
 */
public final class ErrorHandler {
    private static final Logger LOG = Logger.getLogger(ErrorHandler.class.getName());
    private static JFrame frame;

    private ErrorHandler() { }

    public static void install(JFrame mainFrame) {
        frame = mainFrame;
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            LOG.log(Level.SEVERE, "Unhandled error in " + t.getName(), e);
            toastOrDialog("Something unexpected went wrong: " + friendly(e));
        });
    }

    /** Short message, disappears by itself. */
    public static void toast(String message) {
        run(() -> {
            if (frame != null && frame.isShowing()) Toast.show(frame, message);
            else LOG.info(message);
        });
    }

    /** Message the user must read. */
    public static void error(String message) {
        run(() -> JOptionPane.showMessageDialog(frame, message, "TextCraft", JOptionPane.ERROR_MESSAGE));
    }

    public static void error(String context, Throwable t) {
        LOG.log(Level.WARNING, context, t);
        error(context + "\n" + friendly(t));
    }

    public static void info(String message) {
        run(() -> JOptionPane.showMessageDialog(frame, message, "TextCraft", JOptionPane.INFORMATION_MESSAGE));
    }

    private static void toastOrDialog(String m) {
        run(() -> JOptionPane.showMessageDialog(frame, m, "TextCraft", JOptionPane.WARNING_MESSAGE));
    }

    /** Our own exceptions already carry user-friendly text. */
    public static String friendly(Throwable t) {
        String m = t.getMessage();
        return m == null || m.isEmpty() ? t.getClass().getSimpleName() : m;
    }

    private static void run(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
