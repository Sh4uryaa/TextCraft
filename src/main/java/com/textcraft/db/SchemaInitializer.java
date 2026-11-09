package com.textcraft.db;

import com.textcraft.exception.DatabaseException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/** Runs schema.sql when the users table does not exist yet. */
public final class SchemaInitializer {
    private static final Logger LOG = Logger.getLogger(SchemaInitializer.class.getName());

    private SchemaInitializer() { }

    public static void ensureSchema(ConnectionProvider provider) throws DatabaseException {
        try (Connection c = provider.getConnection()) {
            if (tableExists(c, "users")) return;
            LOG.info("Creating TextCraft tables...");
            String sql = loadSchema();
            try (Statement st = c.createStatement()) {
                for (String part : sql.split(";")) {
                    String stmt = stripComments(part).trim();
                    if (!stmt.isEmpty()) st.execute(stmt);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not create the database tables: " + e.getMessage()
                    + " (the DB user needs CREATE rights once, or run schema.sql by hand)", e);
        }
    }

    private static boolean tableExists(Connection c, String name) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getTables(c.getCatalog(), null, name, null)) {
            return rs.next();
        }
    }

    private static String loadSchema() throws DatabaseException {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new DatabaseException("schema.sql is missing from the application");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DatabaseException("Cannot read schema.sql", e);
        }
    }

    private static String stripComments(String s) {
        StringBuilder sb = new StringBuilder();
        for (String line : s.split("\n")) {
            if (!line.trim().startsWith("--")) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
