package com.textcraft.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecentFilesServiceTest {
    @TempDir
    Path dir;

    @Test
    void newestFirstNoDuplicatesCappedAndPersisted() throws Exception {
        Path store = dir.resolve("recent.txt");
        RecentFilesService r = new RecentFilesService(store);
        File[] files = new File[10];
        for (int i = 0; i < 10; i++) {
            files[i] = Files.createFile(dir.resolve("f" + i + ".txt")).toFile();
            r.add(files[i]);
        }
        r.add(files[5]); // moves to the front
        assertEquals(8, r.list().size());
        assertEquals(files[5], r.list().get(0));
        assertEquals(files[9], r.list().get(1));
        assertEquals(r.list(), new RecentFilesService(store).list()); // survives a restart
    }

    @Test
    void deletedFilesAreHidden() throws Exception {
        RecentFilesService r = new RecentFilesService(dir.resolve("r.txt"));
        File f = Files.createFile(dir.resolve("gone.txt")).toFile();
        r.add(f);
        assertTrue(f.delete());
        assertTrue(r.list().isEmpty());
    }
}
