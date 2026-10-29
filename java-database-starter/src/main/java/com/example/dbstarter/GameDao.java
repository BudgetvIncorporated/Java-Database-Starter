package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class GameDao {
    private final Connection connection;

    public GameDao(Connection connection) {
        this.connection = connection;
    }

    public int createGame(Game game) throws SQLException {
        String sql = """
                INSERT INTO games (genre_id, title, launch_url, release_date)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindGame(statement, game);
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
        throw new SQLException("SQLite did not return the new game ID.");
    }

    public boolean updateGame(Game game) throws SQLException {
        String sql = """
                UPDATE games
                SET genre_id = ?, title = ?, launch_url = ?, release_date = ?
                WHERE game_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindGame(statement, game);
            statement.setInt(5, game.id());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteGame(int gameId) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM games WHERE game_id = ?")) {
            statement.setInt(1, gameId);
            return statement.executeUpdate() == 1;
        }
    }

    private void bindGame(PreparedStatement statement, Game game) throws SQLException {
        statement.setInt(1, game.genreId());
        statement.setString(2, game.title());
        statement.setString(3, game.url());
        statement.setString(4, game.releaseDate());
    }
}
