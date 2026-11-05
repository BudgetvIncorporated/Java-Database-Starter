package com.example.dbstarter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class SchemaInitializer {
    private SchemaInitializer() {
    }

    public static void apply(Connection connection) throws IOException, SQLException {
        // Existing libraries (especially restored backups) must not be reseeded
        // or have their schema silently changed on a desktop restart.
        try (Statement statement = connection.createStatement();
             var tables = statement.executeQuery(
                     "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
            if (tables.next() && tables.getInt(1) > 0) {
                return;
            }
        }
        applyResource(connection, "/schema.sql");
        applyResource(connection, "/seed.sql");
    }

    private static void applyResource(Connection connection, String resourceName)
            throws IOException, SQLException {
        try (InputStream input = SchemaInitializer.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Could not find " + resourceName + " on the classpath.");
            }

            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String uncommentedScript = script.lines()
                    .filter(line -> !line.stripLeading().startsWith("--"))
                    .collect(Collectors.joining("\n"));

            for (String part : uncommentedScript.split(";")) {
                String sql = part.trim();
                if (!sql.isEmpty()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
            }
        }
    }
}
