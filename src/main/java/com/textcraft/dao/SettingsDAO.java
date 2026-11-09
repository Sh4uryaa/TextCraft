package com.textcraft.dao;

import com.textcraft.db.ConnectionProvider;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.UserSettings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** user_settings table (theme, Lite mode, fonts, voice language...). */
public class SettingsDAO implements GenericDAO<UserSettings, Long> {
    private final ConnectionProvider db;

    public SettingsDAO(ConnectionProvider db) {
        this.db = db;
    }

    /** Insert or update by user id (portable: update first, insert when nothing was updated). */
    @Override
    public UserSettings save(UserSettings s) throws DatabaseException {
        String upd = "UPDATE user_settings SET theme=?, default_font=?, default_font_size=?, gemini_model=?, "
                + "voice_language=?, voice_speed=?, lite_mode=? WHERE user_id=?";
        String ins = "INSERT INTO user_settings (theme, default_font, default_font_size, gemini_model, "
                + "voice_language, voice_speed, lite_mode, user_id) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = db.getConnection()) {
            int n;
            try (PreparedStatement ps = c.prepareStatement(upd)) {
                bind(ps, s);
                n = ps.executeUpdate();
            }
            if (n == 0) {
                try (PreparedStatement ps = c.prepareStatement(ins)) {
                    bind(ps, s);
                    ps.executeUpdate();
                }
            }
            return s;
        } catch (SQLException e) {
            throw new DatabaseException("Could not save settings: " + e.getMessage(), e);
        }
    }

    private static void bind(PreparedStatement ps, UserSettings s) throws SQLException {
        ps.setString(1, s.getTheme());
        ps.setString(2, s.getDefaultFont());
        ps.setInt(3, s.getDefaultFontSize());
        ps.setString(4, s.getGeminiModel());
        ps.setString(5, s.getVoiceLanguage());
        ps.setDouble(6, s.getVoiceSpeed());
        ps.setBoolean(7, s.isLiteMode());
        ps.setLong(8, s.getUserId());
    }

    @Override
    public Optional<UserSettings> findById(Long userId) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM user_settings WHERE user_id=?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read settings: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UserSettings> findAll() throws DatabaseException {
        List<UserSettings> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM user_settings");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("Could not list settings: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long userId) throws DatabaseException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM user_settings WHERE user_id=?")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete settings: " + e.getMessage(), e);
        }
    }

    private static UserSettings map(ResultSet rs) throws SQLException {
        UserSettings s = new UserSettings();
        s.setUserId(rs.getLong("user_id"));
        s.setTheme(rs.getString("theme"));
        s.setDefaultFont(rs.getString("default_font"));
        s.setDefaultFontSize(rs.getInt("default_font_size"));
        s.setGeminiModel(rs.getString("gemini_model"));
        s.setVoiceLanguage(rs.getString("voice_language"));
        s.setVoiceSpeed(rs.getDouble("voice_speed"));
        s.setLiteMode(rs.getBoolean("lite_mode"));
        return s;
    }
}
