package com.textcraft.voice;

import com.textcraft.exception.DatabaseException;
import com.textcraft.util.AppPaths;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Offline fallback: seconds per month in ~/.textcraft/voice_usage.properties. */
public class FileUsageStore implements UsageStore {
    private final Path file = AppPaths.file("voice_usage.properties");

    @Override
    public synchronized int getSeconds(String month) throws DatabaseException {
        try {
            return Integer.parseInt(load().getProperty(month, "0").trim());
        } catch (NumberFormatException e) {
            return 0; // damaged file: start the month again
        }
    }

    @Override
    public synchronized void addSeconds(String month, int seconds) throws DatabaseException {
        Properties p = load();
        p.setProperty(month, String.valueOf(getSeconds(month) + seconds));
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "voice seconds per month");
        } catch (IOException e) {
            throw new DatabaseException("Could not save voice usage: " + e.getMessage(), e);
        }
    }

    private Properties load() throws DatabaseException {
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                throw new DatabaseException("Could not read voice usage: " + e.getMessage(), e);
            }
        }
        return p;
    }
}
