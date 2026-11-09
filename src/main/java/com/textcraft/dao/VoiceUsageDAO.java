package com.textcraft.dao;

import com.textcraft.db.ConnectionProvider;
import com.textcraft.exception.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** voice_usage table: seconds of voice typing used per user and month ("YYYY-MM"). */
public class VoiceUsageDAO implements GenericDAO<VoiceUsageDAO.Usage, String> {

    /** Row of voice_usage. The DAO id is "userId:month". */
    public static class Usage {
        public final long userId;
        public final String month;
        public int seconds;

        public Usage(long userId, String month, int seconds) {
            this.userId = userId;
            this.month = month;
            this.seconds = seconds;
        }

        public String key() {
            return userId + ":" + month;
        }
    }

    private final ConnectionProvider db;

    public VoiceUsageDAO(ConnectionProvider db) {
        this.db = db;
    }

    public int getSeconds(long userId, String month) throws DatabaseException {
        return findById(userId + ":" + month).map(u -> u.seconds).orElse(0);
    }

    /** Add seconds to the month's total (creates the row if needed). */
    public void addSeconds(long userId, String month, int seconds) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            int n;
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE voice_usage SET seconds_used = seconds_used + ? WHERE user_id=? AND month=?")) {
                ps.setInt(1, seconds);
                ps.setLong(2, userId);
                ps.setString(3, month);
                n = ps.executeUpdate();
            }
            if (n == 0) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO voice_usage (user_id, month, seconds_used) VALUES (?,?,?)")) {
                    ps.setLong(1, userId);
                    ps.setString(2, month);
                    ps.setInt(3, seconds);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save voice usage: " + e.getMessage(), e);
        }
    }

    @Override
    public Usage save(Usage u) throws DatabaseException {
        int current = getSeconds(u.userId, u.month);
        addSeconds(u.userId, u.month, u.seconds - current);
        return u;
    }

    @Override
    public Optional<Usage> findById(String key) throws DatabaseException {
        String[] parts = key.split(":");
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT seconds_used FROM voice_usage WHERE user_id=? AND month=?")) {
            ps.setLong(1, Long.parseLong(parts[0]));
            ps.setString(2, parts[1]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Usage(Long.parseLong(parts[0]), parts[1], rs.getInt(1))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read voice usage: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Usage> findAll() throws DatabaseException {
        List<Usage> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT user_id, month, seconds_used FROM voice_usage");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new Usage(rs.getLong(1), rs.getString(2), rs.getInt(3)));
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("Could not list voice usage: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) throws DatabaseException {
        String[] parts = key.split(":");
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM voice_usage WHERE user_id=? AND month=?")) {
            ps.setLong(1, Long.parseLong(parts[0]));
            ps.setString(2, parts[1]);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete voice usage: " + e.getMessage(), e);
        }
    }
}
