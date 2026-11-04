package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UserDao implements CrudDao<User> {
    private final Connection connection;

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int create(User user) throws SQLException {
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

    @Override
    public Optional<User> findById(int userId) throws SQLException {
        String sql = "SELECT user_id, username, email FROM users WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new User(
                            resultSet.getInt("user_id"),
                            resultSet.getString("username"),
                            resultSet.getString("email")));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.username());
            statement.setString(2, user.email());
            statement.setInt(3, user.id());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(int userId) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public void addGameToLibrary(UserGame userGame)
            throws SQLException {
        String sql = """
                INSERT INTO user_games (user_id, game_id, rating, play_count)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userGame.userId());
            statement.setInt(2, userGame.gameId());
            if (userGame.rating() == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, userGame.rating());
            }
            statement.setInt(4, userGame.playCount());
            statement.executeUpdate();
        }
    }

    public List<SavedGameEntry> findSavedGames() throws SQLException {
        String sql = """
                SELECT ug.user_id, ug.game_id, u.username, g.title,
                       ug.rating, ug.play_count
                FROM user_games AS ug
                JOIN users AS u ON u.user_id = ug.user_id
                JOIN games AS g ON g.game_id = ug.game_id
                ORDER BY u.username COLLATE NOCASE, g.title COLLATE NOCASE
                """;
        List<SavedGameEntry> entries = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int ratingValue = resultSet.getInt("rating");
                Integer rating = resultSet.wasNull() ? null : ratingValue;
                entries.add(new SavedGameEntry(
                        resultSet.getInt("user_id"),
                        resultSet.getInt("game_id"),
                        resultSet.getString("username"),
                        resultSet.getString("title"),
                        rating,
                        resultSet.getInt("play_count")));
            }
        }
        return entries;
    }

    public boolean updateSavedGame(UserGame userGame) throws SQLException {
        String sql = """
                UPDATE user_games
                SET rating = ?, play_count = ?
                WHERE user_id = ? AND game_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userGame.rating() == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, userGame.rating());
            }
            statement.setInt(2, userGame.playCount());
            statement.setInt(3, userGame.userId());
            statement.setInt(4, userGame.gameId());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean removeGameFromLibrary(int userId, int gameId) throws SQLException {
        String sql = "DELETE FROM user_games WHERE user_id = ? AND game_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, gameId);
            return statement.executeUpdate() == 1;
        }
    }
}
