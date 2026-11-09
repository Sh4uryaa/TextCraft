package com.textcraft.service;

import com.textcraft.dao.DocumentDAO;
import com.textcraft.dao.VersionDAO;
import com.textcraft.db.ConnectionProvider;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.Document;
import com.textcraft.model.DocumentVersion;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OOP: Facade - cloud documents: save (with a version row, in ONE transaction), list, open,
 * versions, rename, delete. The UI never sees SQL.
 */
public class DocumentService {
    private final ConnectionProvider db;
    private final DocumentDAO documents;
    private final VersionDAO versions;

    public DocumentService(ConnectionProvider db, DocumentDAO documents, VersionDAO versions) {
        this.db = db;
        this.documents = documents;
        this.versions = versions;
    }

    /**
     * Saves the document and adds a version row. Both succeed or neither does (JDBC transaction).
     */
    public Document saveToCloud(long userId, Document doc, String content) throws DatabaseException {
        doc.setUserId(userId);
        doc.setContent(content);
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                documents.save(c, doc);
                DocumentVersion v = new DocumentVersion();
                v.setDocumentId(doc.getId());
                v.setContent(content);
                versions.insert(c, v);
                c.commit();
            } catch (DatabaseException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
            return doc;
        } catch (SQLException e) {
            throw new DatabaseException("Could not save to the cloud: " + e.getMessage(), e);
        }
    }

    /** Lists a user's documents. Search and sort are done in SQL; the stream only drops blank titles. */
    public List<Document> list(long userId, String titleSearch, boolean newestFirst) throws DatabaseException {
        return documents.findByUser(userId, titleSearch, newestFirst).stream()
                .filter(d -> d.getTitle() != null)
                .collect(Collectors.toList());
    }

    public Document open(long userId, long id) throws DatabaseException {
        return documents.findOwned(userId, id)
                .orElseThrow(() -> new DatabaseException("That document no longer exists in the cloud."));
    }

    public List<DocumentVersion> versionsOf(long userId, long documentId) throws DatabaseException {
        open(userId, documentId); // ownership check
        return versions.findByDocument(documentId);
    }

    public void rename(long userId, long id, String title) throws DatabaseException {
        documents.rename(userId, id, title);
    }

    public void delete(long userId, long id) throws DatabaseException {
        documents.deleteOwned(userId, id);
    }
}
