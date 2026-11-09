package com.textcraft.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.textcraft.db.ConnectionProvider;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.DocType;
import com.textcraft.model.Document;
import com.textcraft.model.User;
import com.textcraft.service.DocumentService;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** CRUD on a temporary SQLite database (same PreparedStatement code as MySQL). */
class DocumentDaoTest {
    @TempDir
    Path dir;

    private ConnectionProvider db;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("test.db");
        db = () -> {
            try {
                return DriverManager.getConnection(url);
            } catch (SQLException e) {
                throw new DatabaseException(e.getMessage(), e);
            }
        };
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, google_id TEXT UNIQUE, display_name TEXT,"
                    + " avatar_url TEXT, username TEXT UNIQUE, email TEXT NOT NULL UNIQUE, password_hash TEXT, salt TEXT,"
                    + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE documents (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL, title TEXT NOT NULL,"
                    + " doc_type TEXT NOT NULL DEFAULT 'RICH', language TEXT, content TEXT, created_at TIMESTAMP, updated_at TIMESTAMP)");
            st.execute("CREATE TABLE document_versions (id INTEGER PRIMARY KEY AUTOINCREMENT, document_id INTEGER NOT NULL,"
                    + " content TEXT, saved_at TIMESTAMP)");
        }
        User u = new User();
        u.setEmail("a@b.com");
        u.setDisplayName("A");
        userId = new UserDAO(db).save(u).getId();
    }

    private Document doc(String title, String content) {
        Document d = new Document();
        d.setUserId(userId);
        d.setTitle(title);
        d.setContent(content);
        return d;
    }

    @Test
    void createReadUpdateDelete() throws Exception {
        DocumentDAO dao = new DocumentDAO(db);
        Document d = dao.save(doc("First", "<p>hi</p>"));
        assertNotNull(d.getId());
        assertEquals("First", dao.findOwned(userId, d.getId()).get().getTitle());

        d.setTitle("Renamed");
        d.setContent("<p>changed</p>");
        dao.save(d);
        assertEquals("<p>changed</p>", dao.findById(d.getId()).get().getContent());

        dao.deleteOwned(userId, d.getId());
        assertFalse(dao.findById(d.getId()).isPresent());
    }

    @Test
    void listSearchesByTitleAndIsPerUser() throws Exception {
        DocumentDAO dao = new DocumentDAO(db);
        dao.save(doc("Physics notes", "x"));
        dao.save(doc("Maths notes", "x"));
        dao.save(doc("Shopping", "x"));
        User other = new User();
        other.setEmail("c@d.com");
        long otherId = new UserDAO(db).save(other).getId();
        Document foreign = doc("Physics secret", "x");
        foreign.setUserId(otherId);
        dao.save(foreign);

        List<Document> found = dao.findByUser(userId, "notes", true);
        assertEquals(2, found.size());
        assertTrue(found.get(0).getContent() == null || found.get(0).getContent().isEmpty()); // list does not load content
        assertEquals(3, dao.findByUser(userId, "", true).size());
        assertTrue(dao.findOwned(userId, foreign.getId()).isEmpty()); // other user's document is invisible
    }

    @Test
    void cloudSaveAddsAVersionEachTime() throws Exception {
        DocumentService svc = new DocumentService(db, new DocumentDAO(db), new VersionDAO(db));
        Document d = doc("Versioned", "");
        svc.saveToCloud(userId, d, "<p>one</p>");
        svc.saveToCloud(userId, d, "<p>two</p>");
        assertEquals(2, svc.versionsOf(userId, d.getId()).size());
        assertEquals("<p>two</p>", svc.open(userId, d.getId()).getContent());
        assertEquals(DocType.RICH, svc.open(userId, d.getId()).getDocType());
    }

    @Test
    void failedVersionInsertRollsTheWholeSaveBack() throws Exception {
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.execute("DROP TABLE document_versions"); // makes the second step of the transaction fail
        }
        DocumentService svc = new DocumentService(db, new DocumentDAO(db), new VersionDAO(db));
        assertThrows(DatabaseException.class, () -> svc.saveToCloud(userId, doc("Broken", ""), "x"));
        assertTrue(new DocumentDAO(db).findByUser(userId, "", true).isEmpty()); // document row was rolled back
    }

    @Test
    void anotherUserCannotOpenMyDocument() throws Exception {
        DocumentService svc = new DocumentService(db, new DocumentDAO(db), new VersionDAO(db));
        Document d = doc("Mine", "");
        svc.saveToCloud(userId, d, "x");
        assertThrows(DatabaseException.class, () -> svc.open(userId + 99, d.getId()));
    }
}
