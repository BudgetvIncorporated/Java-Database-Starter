package com.example.dbstarter;

import java.sql.Connection;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = Database.connect()) {
            SchemaInitializer.apply(connection);
            System.out.println("Connected to the SQLite database.");
        }
    }
}
