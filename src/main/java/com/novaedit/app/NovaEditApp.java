package com.novaedit.app;

import com.novaedit.ui.shell.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NovaEdit Application Entrypoint.
 */
public class NovaEditApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(NovaEditApp.class);

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting NovaEdit application...");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            log.error("Uncaught exception in thread {}", t.getName(), e);
        });

        MainWindow mainWindow = new MainWindow(primaryStage);
        primaryStage.show();
        log.info("NovaEdit launched successfully.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
