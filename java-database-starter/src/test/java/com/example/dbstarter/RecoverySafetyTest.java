package com.example.dbstarter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.Test;

public class RecoverySafetyTest {
    @Test
    public void maintenanceGateBlocksJavaBeforeOpeningDatabase() throws Exception {
        Path directory = Files.createTempDirectory("java-recovery-test-");
        Path database = directory.resolve("library.db");
        Path gate = directory.resolve("library.db.recovery-lock");
        String previous = System.getProperty("game.catalog.db");
        try {
            Files.createDirectory(gate);
            System.setProperty("game.catalog.db", database.toString());
            SQLException error = assertThrows(SQLException.class, Database::connect);
            assertTrue(error.getMessage().contains("administrator recovery"));
            assertTrue(Files.notExists(database));
        } finally {
            if (previous == null) System.clearProperty("game.catalog.db");
            else System.setProperty("game.catalog.db", previous);
            Files.deleteIfExists(gate);
            Files.deleteIfExists(database);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void restartingRestoredEmptyLibraryDoesNotReinsertSamples() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SchemaInitializer.apply(connection);
            try (var statement = connection.createStatement()) {
                statement.execute("DELETE FROM user_games");
                statement.execute("DELETE FROM games");
                statement.execute("DELETE FROM users");
                statement.execute("DELETE FROM genres");
            }
            SchemaInitializer.apply(connection);
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT COUNT(*) FROM games")) {
                assertTrue(rows.next());
                assertEquals(0, rows.getInt(1));
            }
        }
    }
}
