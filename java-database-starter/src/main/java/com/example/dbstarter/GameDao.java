package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class GameDao {
    private final Connection connection;

    public GameDao(Connection connection) {
        this.connection = connection;
    }

    public int createGame(Game game, List<Integer> genreIds) throws SQLException {
        return inTransaction(() -> {
            String sql = """
                    INSERT INTO games (title, game_url, published_on, developer_id)
                    VALUES (?, ?, ?, ?)
                    """;
            int gameId;
            try (PreparedStatement statement =
                         connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindGame(statement, game);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        gameId = keys.getInt(1);
                    } else {
                        gameId = lastInsertedId();
                    }
                }
            }
            replaceGameGenres(gameId, genreIds);
            return gameId;
        });
    }

    public boolean updateGame(Game game, List<Integer> genreIds) throws SQLException {
        return inTransaction(() -> {
            String sql = """
                    UPDATE games
                    SET title = ?, game_url = ?, published_on = ?, developer_id = ?
                    WHERE game_id = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindGame(statement, game);
                statement.setInt(5, game.id());
                if (statement.executeUpdate() != 1) {
                    return false;
                }
            }
            replaceGameGenres(game.id(), genreIds);
            return true;
        });
    }

    public boolean deleteGame(int gameId) throws SQLException {
        String sql = "DELETE FROM games WHERE game_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gameId);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Integer> findGenreIdsForGame(int gameId) throws SQLException {
        String sql = """
                SELECT genre_id
                FROM game_genres
                WHERE game_id = ?
                ORDER BY genre_id
                """;
        List<Integer> genreIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gameId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    genreIds.add(resultSet.getInt("genre_id"));
                }
            }
        }
        return genreIds;
    }

    private void replaceGameGenres(int gameId, List<Integer> genreIds) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM game_genres WHERE game_id = ?")) {
            delete.setInt(1, gameId);
            delete.executeUpdate();
        }

        LinkedHashSet<Integer> uniqueIds = new LinkedHashSet<>(genreIds);
        String sql = "INSERT INTO game_genres (game_id, genre_id) VALUES (?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            for (Integer genreId : uniqueIds) {
                if (genreId == null || genreId < 1) {
                    throw new SQLException("Choose valid genre IDs.");
                }
                insert.setInt(1, gameId);
                insert.setInt(2, genreId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private int lastInsertedId() throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT last_insert_rowid()");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        throw new SQLException("The database did not return the new game ID.");
    }

    private void bindGame(PreparedStatement statement, Game game) throws SQLException {
        statement.setString(1, game.title());
        statement.setString(2, game.url());
        statement.setString(3, game.publishedOn());
        statement.setInt(4, game.developerId());
    }

    private <T> T inTransaction(SqlWork<T> work) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.run();
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T run() throws SQLException;
    }
}
