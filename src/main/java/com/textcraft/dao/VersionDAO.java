package com.textcraft.dao;

import com.textcraft.db.ConnectionProvider;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.DocumentVersion;
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

/** document_versions table: one row per cloud save, used by Version history. */
public class VersionDAO implements GenericDAO<DocumentVersion, Long> {
    private final ConnectionProvider db;

    public VersionDAO(ConnectionProvider db) {
        this.db = db;
    }

    @Override
    public DocumentVersion save(DocumentVersion v) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            return insert(c, v);
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the version: " + e.getMessage(), e);
        }
    }

    /** Insert with the caller's connection (part of the save transaction). */
    public DocumentVersion insert(Connection c, DocumentVersion v) throws DatabaseException {
        String sql = "INSERT INTO document_versions (document_id, content, saved_at) VALUES (?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, v.getDocumentId());
            ps.setString(2, v.getContent());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) v.setId(keys.getLong(1));
            }
            return v;
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the version: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<DocumentVersion> findById(Long id) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM document_versions WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the version: " + e.getMessage(), e);
        }
    }

    /** Newest first. */
    public List<DocumentVersion> findByDocument(long documentId) throws DatabaseException {
        List<DocumentVersion> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM document_versions WHERE document_id=? ORDER BY saved_at DESC, id DESC")) {
            ps.setLong(1, documentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("Could not list versions: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DocumentVersion> findAll() throws DatabaseException {
        List<DocumentVersion> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM document_versions ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("Could not list versions: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long id) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM document_versions WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete the version: " + e.getMessage(), e);
        }
    }

    private static DocumentVersion map(ResultSet rs) throws SQLException {
        DocumentVersion v = new DocumentVersion();
        v.setId(rs.getLong("id"));
        v.setDocumentId(rs.getLong("document_id"));
        v.setContent(rs.getString("content"));
        Timestamp t = rs.getTimestamp("saved_at");
        if (t != null) v.setSavedAt(t.toLocalDateTime());
        return v;
    }
}
