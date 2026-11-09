package com.textcraft.db;

import com.textcraft.app.AppConfig;
import com.textcraft.exception.DatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * OOP: Singleton - one place that knows the JDBC URL/user/password (from config).
 * Works unchanged for a local MySQL, Cloud SQL or a VM: only the URL in config differs.
 * A new connection per call keeps it simple; callers close it with try-with-resources.
 */
public final class DatabaseConnection implements ConnectionProvider {
    private static final DatabaseConnection INSTANCE = new DatabaseConnection();

    private DatabaseConnection() { }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    public boolean isConfigured() {
        return !AppConfig.get().get("db.url", "").trim().isEmpty();
    }

    @Override
    public Connection getConnection() throws DatabaseException {
        AppConfig cfg = AppConfig.get();
        String url = cfg.get("db.url", "").trim();
        if (url.isEmpty()) {
            throw new DatabaseException("No database is configured (db.url in config/app.properties). Working offline.");
        }
        try {
            DriverManager.setLoginTimeout(5);
            return DriverManager.getConnection(url, cfg.get("db.user", ""), cfg.get("db.password", ""));
        } catch (SQLException e) {
            throw new DatabaseException("Cannot connect to the database: " + e.getMessage(), e);
        }
    }
}
