package com.textcraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Where TextCraft keeps local data: ~/.textcraft (refresh token, drafts, local settings, recent files). */
public final class AppPaths {
    private AppPaths() { }

    public static Path home() {
        Path p = Paths.get(System.getProperty("user.home"), ".textcraft");
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            // fall through: callers handle failures when they write
        }
        return p;
    }

    public static Path file(String name) {
        return home().resolve(name);
    }
}
