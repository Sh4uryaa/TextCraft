package com.novaedit.features.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * Git integration service powered by JGit.
 * Manages repository status, staging, commits, branch tracking.
 */
public class GitService {
    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    public record RepoStatus(
            String branch,
            Set<String> modified,
            Set<String> untracked,
            Set<String> added,
            Set<String> missing
    ) {}

    private Git activeGit;

    public synchronized boolean openRepository(Path folder) {
        close();
        if (folder == null) return false;

        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.findGitDir(folder.toFile())
                    .readEnvironment()
                    .build();

            if (repository.getObjectDatabase().exists()) {
                activeGit = new Git(repository);
                return true;
            }
        } catch (Exception e) {
            log.debug("No valid Git repository found in {}: {}", folder, e.getMessage());
        }
        return false;
    }

    public synchronized boolean isRepoOpen() {
        return activeGit != null;
    }

    public synchronized String getCurrentBranch() {
        if (activeGit == null) return "";
        try {
            return activeGit.getRepository().getBranch();
        } catch (Exception e) {
            log.warn("Failed to get current Git branch", e);
            return "";
        }
    }

    public synchronized RepoStatus getStatus() {
        if (activeGit == null) {
            return new RepoStatus("", Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
        }
        try {
            Status status = activeGit.status().call();
            String branch = getCurrentBranch();
            return new RepoStatus(
                    branch,
                    status.getModified(),
                    status.getUntracked(),
                    status.getAdded(),
                    status.getMissing()
            );
        } catch (Exception e) {
            log.warn("Failed to query Git status", e);
            return new RepoStatus(getCurrentBranch(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
        }
    }

    public synchronized boolean stageFile(String filePattern) {
        if (activeGit == null) return false;
        try {
            activeGit.add().addFilepattern(filePattern).call();
            return true;
        } catch (Exception e) {
            log.error("Failed to stage file {}", filePattern, e);
            return false;
        }
    }

    public synchronized boolean commit(String message) {
        if (activeGit == null || message == null || message.isBlank()) return false;
        try {
            activeGit.commit().setMessage(message).call();
            return true;
        } catch (Exception e) {
            log.error("Failed to commit Git changes", e);
            return false;
        }
    }

    public synchronized void close() {
        if (activeGit != null) {
            activeGit.close();
            activeGit = null;
        }
    }
}
