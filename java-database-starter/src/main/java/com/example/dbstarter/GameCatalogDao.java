package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class GameCatalogDao {
    private static final String GAME_SELECT = """
            SELECT g.game_id, g.title, g.launch_url, g.release_date, ge.genre_name
            FROM games AS g
            JOIN genres AS ge ON ge.genre_id = g.genre_id
            """;

    private final Connection connection;

    public GameCatalogDao(Connection connection) {
        this.connection = connection;
    }

    public List<Game> findAllGames() throws SQLException {
        return queryGames(GAME_SELECT, null);
    }

    public List<Game> searchGames(String titleText) throws SQLException {
        return queryGames(GAME_SELECT + """
                WHERE instr(lower(g.title), lower(?)) > 0
                """, titleText);
    }

    public List<Game> findGamesByGenre(String genreName) throws SQLException {
        return queryGames(GAME_SELECT + """
                WHERE lower(ge.genre_name) = lower(?)
                """, genreName);
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

    public List<UserGame> findGamesForUser(String username) throws SQLException {
        String sql = """
                SELECT u.user_id, u.username, u.email,
                       g.game_id, g.title, g.launch_url, g.release_date, ge.genre_name,
                       ug.rating, ug.play_count
                FROM users AS u
                JOIN user_games AS ug ON ug.user_id = u.user_id
                JOIN games AS g ON g.game_id = ug.game_id
                JOIN genres AS ge ON ge.genre_id = g.genre_id
                WHERE lower(u.username) = lower(?)
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
                            resultSet.getString("launch_url"),
                            resultSet.getString("release_date"),
                            resultSet.getString("genre_name"));
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
                            resultSet.getString("launch_url"),
                            resultSet.getString("release_date"),
                            resultSet.getString("genre_name")));
                }
            }
        }
        return games;
    }
}
