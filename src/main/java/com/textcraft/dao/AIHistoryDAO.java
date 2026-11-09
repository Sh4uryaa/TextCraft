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

/** ai_history table: every AI action with its input and output. */
public class AIHistoryDAO implements GenericDAO<AIHistoryDAO.Entry, Long> {

    /** One logged AI call. */
    public static class Entry {
        public Long id;
        public long userId;
        public String action;
        public String input;
        public String output;
        public String model;

        public Entry(long userId, String action, String input, String output, String model) {
            this.userId = userId;
            this.action = action;
            this.input = input;
            this.output = output;
            this.model = model;
        }
    }

    private final ConnectionProvider db;

    public AIHistoryDAO(ConnectionProvider db) {
        this.db = db;
    }

    @Override
    public Entry save(Entry e) throws DatabaseException {
        String sql = "INSERT INTO ai_history (user_id, action, input_text, output_text, model) VALUES (?,?,?,?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, e.userId);
            ps.setString(2, e.action);
            ps.setString(3, e.input);
            ps.setString(4, e.output);
            ps.setString(5, e.model);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.id = keys.getLong(1);
            }
            return e;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not log the AI action: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Entry> findById(Long id) throws DatabaseException {
        List<Entry> l = query("SELECT * FROM ai_history WHERE id=?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Entry> findAll() throws DatabaseException {
        return query("SELECT * FROM ai_history ORDER BY id DESC LIMIT 200", null);
    }

    private List<Entry> query(String sql, Long param) throws DatabaseException {
        List<Entry> out = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Entry e = new Entry(rs.getLong("user_id"), rs.getString("action"), rs.getString("input_text"),
                            rs.getString("output_text"), rs.getString("model"));
                    e.id = rs.getLong("id");
                    out.add(e);
                }
            }
            return out;
        } catch (SQLException ex) {
            throw new DatabaseException("Could not read the AI history: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(Long id) throws DatabaseException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM ai_history WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Could not delete the AI history entry: " + ex.getMessage(), ex);
        }
    }
}
