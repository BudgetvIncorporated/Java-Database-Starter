package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class GameDao implements CrudDao<Game> {
    private final Connection connection;

    public GameDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int create(Game game) throws SQLException {
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

    @Override
    public Optional<Game> findById(int gameId) throws SQLException {
        String sql = """
                SELECT g.game_id, g.genre_id, g.title, g.launch_url, g.release_date,
                       ge.genre_name
                FROM games AS g
                JOIN genres AS ge ON ge.genre_id = g.genre_id
                WHERE g.game_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gameId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new Game(
                            resultSet.getInt("game_id"),
                            resultSet.getInt("genre_id"),
                            resultSet.getString("title"),
                            resultSet.getString("launch_url"),
                            resultSet.getString("release_date"),
                            resultSet.getString("genre_name")));
                }
            }
        }
        return Optional.empty();
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

    @Override
    public boolean update(Game game) throws SQLException {
        return updateGame(game);
    }

    @Override
    public boolean delete(int gameId) throws SQLException {
        return deleteGame(gameId);
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
