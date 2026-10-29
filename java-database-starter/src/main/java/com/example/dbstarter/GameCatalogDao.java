package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class GameCatalogDao {
    private static final String GAME_SELECT = """
            SELECT g.game_id, g.title, g.game_url, g.published_on, g.developer_id,
                   d.name AS developer_name,
                   COALESCE(GROUP_CONCAT(DISTINCT ge.genre_name), '') AS genre_names
            FROM games AS g
            JOIN developers AS d ON d.developer_id = g.developer_id
            LEFT JOIN game_genres AS gg ON gg.game_id = g.game_id
            LEFT JOIN genres AS ge ON ge.genre_id = gg.genre_id
            """;

    private final Connection connection;

    public GameCatalogDao(Connection connection) {
        this.connection = connection;
    }

    public List<Game> findAllGames() throws SQLException {
        return queryGames(GAME_SELECT + """
                GROUP BY g.game_id, d.name
                """, null);
    }

    public List<Game> searchGames(String titleText) throws SQLException {
        return queryGames(GAME_SELECT + """
                WHERE instr(lower(g.title), lower(?)) > 0
                GROUP BY g.game_id, d.name
                """, titleText);
    }

    public List<Game> findGamesByGenre(String genreName) throws SQLException {
        return queryGames(GAME_SELECT + """
                WHERE EXISTS (
                    SELECT 1
                    FROM game_genres AS filter_gg
                    JOIN genres AS filter_ge ON filter_ge.genre_id = filter_gg.genre_id
                    WHERE filter_gg.game_id = g.game_id
                      AND lower(filter_ge.genre_name) = lower(?)
                )
                GROUP BY g.game_id, d.name
                """, genreName);
    }

    public List<Genre> findAllGenres() throws SQLException {
        String sql = """
                SELECT genre_id, genre_name AS name
                FROM genres
                ORDER BY genre_name COLLATE NOCASE
                """;
        List<Genre> genres = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                genres.add(new Genre(
                        resultSet.getInt("genre_id"),
                        resultSet.getString("name")));
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

    public List<UserGameSummary> summarizeUsers() throws SQLException {
        String sql = """
                SELECT u.user_id, u.username,
                       COUNT(ug.game_id) AS game_count,
                       COALESCE(ROUND(AVG(ug.rating), 1), 0) AS average_rating,
                       COALESCE(SUM(ug.play_count), 0) AS total_play_count
                FROM users AS u
                LEFT JOIN user_games AS ug ON ug.user_id = u.user_id
                GROUP BY u.user_id, u.username
                ORDER BY total_play_count DESC, u.username COLLATE NOCASE
                """;
        List<UserGameSummary> summaries = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                summaries.add(new UserGameSummary(
                        resultSet.getInt("user_id"),
                        resultSet.getString("username"),
                        resultSet.getInt("game_count"),
                        resultSet.getDouble("average_rating"),
                        resultSet.getInt("total_play_count")));
            }
        }
        return summaries;
    }

    public List<DeveloperGameCount> countGamesPerDeveloper() throws SQLException {
        String sql = """
                SELECT d.name AS developer_name, COUNT(g.game_id) AS game_count
                FROM developers AS d
                LEFT JOIN games AS g ON g.developer_id = d.developer_id
                GROUP BY d.developer_id, d.name
                ORDER BY game_count DESC, d.name COLLATE NOCASE
                """;
        List<DeveloperGameCount> counts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                counts.add(new DeveloperGameCount(
                        resultSet.getString("developer_name"),
                        resultSet.getInt("game_count")));
            }
        }
        return counts;
    }

    public List<UserGame> findGamesForUser(String username) throws SQLException {
        String sql = """
                SELECT u.user_id, u.username, u.email,
                       g.game_id, g.title, g.game_url, g.published_on, g.developer_id,
                       d.name AS developer_name,
                       COALESCE(GROUP_CONCAT(DISTINCT ge.genre_name), '') AS genre_names,
                       ug.rating, ug.play_count
                FROM users AS u
                JOIN user_games AS ug ON ug.user_id = u.user_id
                JOIN games AS g ON g.game_id = ug.game_id
                JOIN developers AS d ON d.developer_id = g.developer_id
                LEFT JOIN game_genres AS gg ON gg.game_id = g.game_id
                LEFT JOIN genres AS ge ON ge.genre_id = gg.genre_id
                WHERE lower(u.username) = lower(?)
                GROUP BY u.user_id, u.username, u.email, g.game_id, d.name,
                         ug.rating, ug.play_count
                ORDER BY g.title COLLATE NOCASE
                """;
        List<UserGame> userGames = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int ratingValue = resultSet.getInt("rating");
                    Integer rating = resultSet.wasNull() ? null : ratingValue;
                    User user = new User(
                            resultSet.getInt("user_id"),
                            resultSet.getString("username"),
                            resultSet.getString("email"));
                    Game game = new Game(
                            resultSet.getInt("game_id"),
                            resultSet.getString("title"),
                            resultSet.getString("game_url"),
                            resultSet.getString("published_on"),
                            resultSet.getInt("developer_id"),
                            resultSet.getString("developer_name"),
                            resultSet.getString("genre_names"));
                    userGames.add(new UserGame(
                            user,
                            game,
                            rating,
                            resultSet.getInt("play_count")));
                }
            }
        }
        return userGames;
    }

    private List<Game> queryGames(String sql, String parameter) throws SQLException {
        List<Game> games = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setString(1, parameter);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    games.add(new Game(
                            resultSet.getInt("game_id"),
                            resultSet.getString("title"),
                            resultSet.getString("game_url"),
                            resultSet.getString("published_on"),
                            resultSet.getInt("developer_id"),
                            resultSet.getString("developer_name"),
                            resultSet.getString("genre_names")));
                }
            }
        }
        return games;
    }
}
