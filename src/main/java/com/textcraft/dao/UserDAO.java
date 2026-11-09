package com.textcraft.dao;

import com.textcraft.db.ConnectionProvider;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Users table. PreparedStatement only. */
public class UserDAO implements GenericDAO<User, Long> {
    private final ConnectionProvider db;

    public UserDAO(ConnectionProvider db) {
        this.db = db;
    }

    @Override
    public User save(User u) throws DatabaseException {
        try (Connection c = db.getConnection()) {
            if (u.getId() == null) {
                String sql = "INSERT INTO users (google_id, display_name, avatar_url, username, email, password_hash, salt) "
                        + "VALUES (?,?,?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, u);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) u.setId(keys.getLong(1));
                    }
                }
            } else {
                String sql = "UPDATE users SET google_id=?, display_name=?, avatar_url=?, username=?, email=?, "
                        + "password_hash=?, salt=? WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, u);
                    ps.setLong(8, u.getId());
                    ps.executeUpdate();
                }
            }
            return u;
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the user: " + e.getMessage(), e);
        }
    }

    private static void bind(PreparedStatement ps, User u) throws SQLException {
        ps.setString(1, u.getGoogleId());
        ps.setString(2, u.getDisplayName());
        ps.setString(3, u.getAvatarUrl());
        ps.setString(4, u.getUsername());
        ps.setString(5, u.getEmail());
        ps.setString(6, u.getPasswordHash());
        ps.setString(7, u.getSalt());
    }

    @Override
    public Optional<User> findById(Long id) throws DatabaseException {
        return queryOne("SELECT * FROM users WHERE id=?", id);
    }

    public Optional<User> findByEmail(String email) throws DatabaseException {
        return queryOne("SELECT * FROM users WHERE email=?", email);
    }

    public Optional<User> findByGoogleId(String googleId) throws DatabaseException {
        return queryOne("SELECT * FROM users WHERE google_id=?", googleId);
    }

    private Optional<User> queryOne(String sql, Object param) throws DatabaseException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the user: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findAll() throws DatabaseException {
        List<User> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("Could not list users: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long id) throws DatabaseException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete the user: " + e.getMessage(), e);
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setGoogleId(rs.getString("google_id"));
        u.setDisplayName(rs.getString("display_name"));
        u.setAvatarUrl(rs.getString("avatar_url"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) u.setCreatedAt(t.toLocalDateTime());
        return u;
    }
}
