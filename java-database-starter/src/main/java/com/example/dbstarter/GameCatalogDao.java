package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class GameCatalogDao {
    private final Connection connection;

    public GameCatalogDao(Connection connection) {
        this.connection = connection;
    }

    public List<Game> findAllGames() throws SQLException {
        String sql = """
                SELECT g.game_id, g.genre_id, g.title, g.launch_url, g.release_date,
                       ge.genre_name
                FROM games AS g
                JOIN genres AS ge ON ge.genre_id = g.genre_id
                """;
        List<Game> games = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                games.add(readGame(resultSet));
            }
        }
        return games;
    }

    public List<GamePlayerEntry> findGamePlayers() throws SQLException {
        return queryGamePlayers("""
                SELECT g.title AS game_title, u.username
                FROM user_games AS ug
                JOIN games AS g ON g.game_id = ug.game_id
                JOIN users AS u ON u.user_id = ug.user_id
                ORDER BY g.title COLLATE NOCASE, u.username COLLATE NOCASE
                """, null);
    }

    public List<GamePlayerEntry> searchGamePlayers(String titlePart) throws SQLException {
        return queryGamePlayers("""
                SELECT g.title AS game_title, u.username
                FROM user_games AS ug
                JOIN games AS g ON g.game_id = ug.game_id
                JOIN users AS u ON u.user_id = ug.user_id
                WHERE instr(lower(g.title), lower(?)) > 0
                ORDER BY g.title COLLATE NOCASE, u.username COLLATE NOCASE
                """, titlePart);
    }

    public List<GamePlayerEntry> findGamePlayersByGenre(int genreId) throws SQLException {
        String sql = """
                SELECT g.title AS game_title, u.username
                FROM user_games AS ug
                JOIN games AS g ON g.game_id = ug.game_id
                JOIN users AS u ON u.user_id = ug.user_id
                JOIN genres AS ge ON ge.genre_id = g.genre_id
                WHERE ge.genre_id = ?
                ORDER BY g.title COLLATE NOCASE, u.username COLLATE NOCASE
                """;
        List<GamePlayerEntry> entries = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, genreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(readGamePlayerEntry(resultSet));
                }
            }
        }
        return entries;
    }

    public List<Genre> findAllGenres() throws SQLException {
        String sql = """
                SELECT genre_id, genre_name
                FROM genres
                ORDER BY genre_name COLLATE NOCASE
                """;
        List<Genre> genres = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                genres.add(new Genre(
                        resultSet.getInt("genre_id"),
                        resultSet.getString("genre_name")));
            }
        }
        return genres;
    }

    public List<User> findAllUsers() throws SQLException {
        String sql = """
                SELECT user_id, username, email
                FROM users
                ORDER BY username COLLATE NOCASE
                """;
        List<User> users = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(new User(
                        resultSet.getInt("user_id"),
                        resultSet.getString("username"),
                        resultSet.getString("email")));
            }
        }
        return users;
    }

    public List<UserGameCount> countSavedGamesByUser() throws SQLException {
        String sql = """
                SELECT u.username, COUNT(ug.game_id) AS saved_games
                FROM users AS u
                LEFT JOIN user_games AS ug ON ug.user_id = u.user_id
                GROUP BY u.user_id, u.username
                ORDER BY saved_games DESC, u.username COLLATE NOCASE
                """;
        List<UserGameCount> counts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                counts.add(new UserGameCount(
                        resultSet.getString("username"),
                        resultSet.getInt("saved_games")));
            }
        }
        return counts;
    }

    private List<GamePlayerEntry> queryGamePlayers(String sql, String searchTerm)
            throws SQLException {
        List<GamePlayerEntry> entries = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (searchTerm != null) {
                statement.setString(1, searchTerm);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(readGamePlayerEntry(resultSet));
                }
            }
        }
        return entries;
    }

    private Game readGame(ResultSet resultSet) throws SQLException {
        return new Game(
                resultSet.getInt("game_id"),
                resultSet.getInt("genre_id"),
                resultSet.getString("title"),
                resultSet.getString("launch_url"),
                resultSet.getString("release_date"),
                resultSet.getString("genre_name"));
    }

    private GamePlayerEntry readGamePlayerEntry(ResultSet resultSet) throws SQLException {
        return new GamePlayerEntry(
                resultSet.getString("game_title"),
                resultSet.getString("username"));
    }
}
