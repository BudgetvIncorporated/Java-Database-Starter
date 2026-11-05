package com.example.dbstarter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.io.IOException;

public final class Database {
    private Database() {
    }

    public static Connection connect() throws SQLException {
        String databasePath = System.getProperty("game.catalog.db", "game-library.db");
        try {
            databasePath = new File(databasePath).getCanonicalPath();
        } catch (IOException exception) {
            throw new SQLException("Could not resolve the library path.", exception);
        }
        File maintenance = new File(databasePath + ".recovery-lock");
        if (maintenance.exists()) {
            throw new SQLException("The library is offline for administrator recovery.");
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (Statement statement = connection.createStatement()) {
            if (maintenance.exists()) {
                throw new SQLException("The library is offline for administrator recovery.");
            }
            statement.execute("PRAGMA foreign_keys = ON");
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }
}
