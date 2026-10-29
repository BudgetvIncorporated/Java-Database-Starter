package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public final class UserDao {
    private final Connection connection;

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    public int createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, email) VALUES (?, ?)";
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.username());
            statement.setString(2, user.email());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT last_insert_rowid()");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        throw new SQLException("SQLite did not return the new user ID.");
    }

    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.username());
            statement.setString(2, user.email());
            statement.setInt(3, user.id());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public void addGameToLibrary(int userId, int gameId, Integer rating, int playCount)
            throws SQLException {
        String sql = """
                INSERT INTO user_games (user_id, game_id, rating, play_count)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, gameId);
            if (rating == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, rating);
            }
            statement.setInt(4, playCount);
            statement.executeUpdate();
        }
    }
}
