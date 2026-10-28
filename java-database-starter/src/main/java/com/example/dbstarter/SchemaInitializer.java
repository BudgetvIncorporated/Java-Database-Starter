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
        try (InputStream input = SchemaInitializer.class.getResourceAsStream("/schema.sql")) {
            if (input == null) {
                throw new IOException("Could not find schema.sql on the classpath.");
            }

            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String part : script.split(";")) {
                String sql = part.lines()
                        .filter(line -> !line.stripLeading().startsWith("--"))
                        .collect(Collectors.joining("\n"))
                        .trim();

                if (!sql.isEmpty()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
            }
        }
    }
}
