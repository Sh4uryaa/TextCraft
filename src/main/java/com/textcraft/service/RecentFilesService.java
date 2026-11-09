package com.textcraft.service;

import com.textcraft.util.AppPaths;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/** Last opened/saved files (newest first), kept in ~/.textcraft/recent.txt. OOP: Deque collection. */
public class RecentFilesService {
    private static final int MAX = 8;

    private final Deque<File> recent = new ArrayDeque<>();
    private final Path store;

    public RecentFilesService() {
        this(AppPaths.file("recent.txt"));
    }

    public RecentFilesService(Path store) {
        this.store = store;
        load();
    }

    public synchronized void add(File f) {
        recent.removeIf(x -> x.equals(f));
        recent.addFirst(f);
        while (recent.size() > MAX) recent.removeLast();
        save();
    }

    /** Only files that still exist. */
    public synchronized List<File> list() {
        return recent.stream().filter(File::exists).collect(Collectors.toList());
    }

    private void load() {
        try {
            if (!Files.exists(store)) return;
            List<String> lines = Files.readAllLines(store, StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0; i--) { // file stores newest first; rebuild in the same order
                if (!lines.get(i).trim().isEmpty()) recent.addFirst(new File(lines.get(i).trim()));
            }
        } catch (IOException e) {
            // no recent list; not important
        }
    }

    private void save() {
        try {
            List<String> lines = new ArrayList<>();
            recent.forEach(f -> lines.add(f.getAbsolutePath()));
            Files.write(store, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // ignore: the list is a convenience
        }
    }
}
