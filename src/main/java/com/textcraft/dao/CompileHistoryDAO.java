package com.textcraft.dao;

import com.textcraft.db.ConnectionProvider;
import com.textcraft.exception.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** compile_history table: every code run. */
public class CompileHistoryDAO implements GenericDAO<CompileHistoryDAO.Entry, Long> {

    /** One logged run. */
    public static class Entry {
        public Long id;
        public long userId;
        public String language;
        public String source;
        public String stdin;
        public String output;
        public String status;
        public long runMs;

        public Entry(long userId, String language, String source, String stdin, String output, String status, long runMs) {
            this.userId = userId;
            this.language = language;
            this.source = source;
            this.stdin = stdin;
            this.output = output;
            this.status = status;
            this.runMs = runMs;
        }
    }

    private final ConnectionProvider db;

    public CompileHistoryDAO(ConnectionProvider db) {
        this.db = db;
    }

    @Override
    public Entry save(Entry e) throws DatabaseException {
        String sql = "INSERT INTO compile_history (user_id, language, source_code, stdin_text, output_text, status, run_ms) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, e.userId);
            ps.setString(2, e.language);
            ps.setString(3, e.source);
            ps.setString(4, e.stdin);
            ps.setString(5, e.output);
            ps.setString(6, e.status);
            ps.setLong(7, e.runMs);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.id = keys.getLong(1);
            }
            return e;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not log the run: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Entry> findById(Long id) throws DatabaseException {
        List<Entry> l = query("SELECT * FROM compile_history WHERE id=?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Entry> findAll() throws DatabaseException {
        return query("SELECT * FROM compile_history ORDER BY id DESC LIMIT 200", null);
    }

    private List<Entry> query(String sql, Long param) throws DatabaseException {
        List<Entry> out = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Entry e = new Entry(rs.getLong("user_id"), rs.getString("language"), rs.getString("source_code"),
                            rs.getString("stdin_text"), rs.getString("output_text"), rs.getString("status"),
                            rs.getLong("run_ms"));
                    e.id = rs.getLong("id");
                    out.add(e);
                }
            }
            return out;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not read the run history: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(Long id) throws DatabaseException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM compile_history WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Could not delete the run entry: " + ex.getMessage(), ex);
        }
    }
}
