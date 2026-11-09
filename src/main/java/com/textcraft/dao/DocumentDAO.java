package com.textcraft.dao;

import com.textcraft.db.ConnectionProvider;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.DocType;
import com.textcraft.model.Document;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Documents table. PreparedStatement only. Every query that takes a user id only touches that
 * user's rows (per-user data only).
 */
public class DocumentDAO implements GenericDAO<Document, Long> {
    private final ConnectionProvider db;

    public DocumentDAO(ConnectionProvider db) {
        this.db = db;
    }

    @Override
    public Document save(Document d) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            return save(c, d);
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the document: " + e.getMessage(), e);
        }
    }

    /** Insert or update using the caller's connection, so DocumentService can wrap it in a transaction. */
    public Document save(Connection c, Document d) throws DatabaseException {
        try {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            if (d.getId() == null) {
                String sql = "INSERT INTO documents (user_id, title, doc_type, language, content, created_at, updated_at) "
                        + "VALUES (?,?,?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, d.getUserId());
                    ps.setString(2, d.getTitle());
                    ps.setString(3, d.getDocType().name());
                    ps.setString(4, d.getLanguage());
                    ps.setString(5, d.getContent());
                    ps.setTimestamp(6, now);
                    ps.setTimestamp(7, now);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) d.setId(keys.getLong(1));
                    }
                }
            } else {
                String sql = "UPDATE documents SET title=?, doc_type=?, language=?, content=?, updated_at=? "
                        + "WHERE id=? AND user_id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, d.getTitle());
                    ps.setString(2, d.getDocType().name());
                    ps.setString(3, d.getLanguage());
                    ps.setString(4, d.getContent());
                    ps.setTimestamp(5, now);
                    ps.setLong(6, d.getId());
                    ps.setLong(7, d.getUserId());
                    if (ps.executeUpdate() == 0) throw new DatabaseException("Document not found for this user.");
                }
            }
            d.setUpdatedAt(now.toLocalDateTime());
            return d;
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the document: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Document> findById(Long id) throws DatabaseException {
        return queryOne("SELECT * FROM documents WHERE id=?", id, null);
    }

    /** Document with content, only if it belongs to the user. */
    public Optional<Document> findOwned(long userId, long id) throws DatabaseException {
        return queryOne("SELECT * FROM documents WHERE id=? AND user_id=?", id, userId);
    }

    private Optional<Document> queryOne(String sql, Object p1, Object p2) throws DatabaseException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, p1);
            if (p2 != null) ps.setObject(2, p2);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs, true)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the document: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> findAll() throws DatabaseException {
        return list("SELECT id, user_id, title, doc_type, language, created_at, updated_at FROM documents "
                + "ORDER BY updated_at DESC", null, null);
    }

    /** List a user's documents without their content: optional title search, newest or oldest first. */
    public List<Document> findByUser(long userId, String titleSearch, boolean newestFirst) throws DatabaseException {
        String sql = "SELECT id, user_id, title, doc_type, language, created_at, updated_at FROM documents "
                + "WHERE user_id=? AND title LIKE ? ORDER BY updated_at " + (newestFirst ? "DESC" : "ASC");
        String like = "%" + (titleSearch == null ? "" : titleSearch.trim()) + "%";
        return list(sql, userId, like);
    }

    private List<Document> list(String sql, Object p1, Object p2) throws DatabaseException {
        List<Document> out = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (p1 != null) ps.setObject(1, p1);
            if (p2 != null) ps.setObject(2, p2);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs, false));
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("Could not list documents: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long id) throws DatabaseException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM documents WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete the document: " + e.getMessage(), e);
        }
    }

    public void deleteOwned(long userId, long id) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM documents WHERE id=? AND user_id=?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete the document: " + e.getMessage(), e);
        }
    }

    public void rename(long userId, long id, String title) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE documents SET title=? WHERE id=? AND user_id=?")) {
            ps.setString(1, title);
            ps.setLong(2, id);
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not rename the document: " + e.getMessage(), e);
        }
    }

    private static Document map(ResultSet rs, boolean withContent) throws SQLException {
        Document d = new Document();
        d.setId(rs.getLong("id"));
        d.setUserId(rs.getLong("user_id"));
        d.setTitle(rs.getString("title"));
        d.setDocType(DocType.valueOf(rs.getString("doc_type")));
        d.setLanguage(rs.getString("language"));
        if (withContent) d.setContent(rs.getString("content"));
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        if (created != null) d.setCreatedAt(created.toLocalDateTime());
        if (updated != null) d.setUpdatedAt(updated.toLocalDateTime());
        return d;
    }
}
