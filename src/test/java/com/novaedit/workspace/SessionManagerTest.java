package com.novaedit.workspace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionManagerTest {

    @Test
    @DisplayName("Session serialization and restoration")
    void testSessionSerialization(@TempDir Path tempDir) {
        SessionManager manager = new SessionManager();

        SessionManager.SessionData data = new SessionManager.SessionData();
        data.activeTabIndex = 1;
        data.workspacePath = tempDir.toString();
        data.tabs = List.of(
                new SessionManager.TabSession("file1.java", 15, 0.0),
                new SessionManager.TabSession("file2.md", 42, 100.5)
        );

        manager.saveSession(data);
        SessionManager.SessionData loaded = manager.loadSession();

        assertThat(loaded).isNotNull();
        assertThat(loaded.activeTabIndex).isEqualTo(1);
        assertThat(loaded.workspacePath).isEqualTo(tempDir.toString());
        assertThat(loaded.tabs).hasSize(2);
        assertThat(loaded.tabs.get(1).filePath).isEqualTo("file2.md");
        assertThat(loaded.tabs.get(1).caretOffset).isEqualTo(42);
        assertThat(loaded.tabs.get(1).scrollY).isEqualTo(100.5);
    }
}
