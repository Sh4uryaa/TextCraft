package com.novaedit.features.search;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

/**
 * Searches across an entire project directory on virtual threads.
 */
public class ProjectSearchService {

    public record FileMatches(Path path, List<SearchResult> matches) {}

    private final FindReplaceService findReplaceService = new FindReplaceService();

    public CompletableFuture<List<FileMatches>> searchProject(Path projectRoot, FindOptions options, String includeGlob, String excludeGlob) {
        return CompletableFuture.supplyAsync(() -> {
            List<FileMatches> results = new CopyOnWriteArrayList<>();
            if (projectRoot == null || !Files.isDirectory(projectRoot)) {
                return Collections.emptyList();
            }

            PathMatcher includeMatcher = (includeGlob != null && !includeGlob.isBlank())
                    ? FileSystems.getDefault().getPathMatcher("glob:" + includeGlob)
                    : null;
            PathMatcher excludeMatcher = (excludeGlob != null && !excludeGlob.isBlank())
                    ? FileSystems.getDefault().getPathMatcher("glob:" + excludeGlob)
                    : null;

            try {
                Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        String name = dir.getFileName().toString();
                        if (name.startsWith(".") || name.equals("target") || name.equals("node_modules") || name.equals("build")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (attrs.isRegularFile() && attrs.size() < 10_000_000) { // skip massive files
                            if (includeMatcher != null && !includeMatcher.matches(file.getFileName())) {
                                return FileVisitResult.CONTINUE;
                            }
                            if (excludeMatcher != null && excludeMatcher.matches(file.getFileName())) {
                                return FileVisitResult.CONTINUE;
                            }

                            try {
                                String content = Files.readString(file);
                                List<SearchResult> matches = findReplaceService.findAll(content, options);
                                if (!matches.isEmpty()) {
                                    results.add(new FileMatches(file, matches));
                                }
                            } catch (Exception ignored) {
                                // Skip binary or unreadable files
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger(ProjectSearchService.class)
                        .warn("Error walking project files during search", e);
            }

            results.sort(Comparator.comparing(fm -> fm.path().toString()));
            return results;
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
}
