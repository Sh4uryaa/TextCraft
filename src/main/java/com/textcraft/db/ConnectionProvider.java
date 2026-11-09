package com.textcraft.db;

import com.textcraft.exception.DatabaseException;
import java.sql.Connection;

/** Gives DAOs a connection. Real app: DatabaseConnection (MySQL). Tests: any JDBC URL. */
public interface ConnectionProvider {
    Connection getConnection() throws DatabaseException;
}
