package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class GameCatalogDao {
    private static final String GAME_SELECT = """
            SELECT g.game_id, g.title, g.game_url, g.published_on, d.name AS developer_name,
                   COALESCE(GROUP_CONCAT(DISTINCT ge.name), '') AS genre_names
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
                      AND lower(filter_ge.name) = lower(?)
                )
                GROUP BY g.game_id, d.name
                """, genreName);
    }

    public List<Genre> findAllGenres() throws SQLException {
        String sql = """
                SELECT genre_id, name
                FROM genres
                ORDER BY name COLLATE NOCASE
                """;
        List<Genre> genres = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                genres.add(new Genre(resultSet.getInt("genre_id"), resultSet.getString("name")));
            }
        }
        return genres;
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
                            resultSet.getString("developer_name"),
                            resultSet.getString("genre_names")));
                }
            }
        }
        return games;
    }
}
