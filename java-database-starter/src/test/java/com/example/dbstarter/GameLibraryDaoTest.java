package com.example.dbstarter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GameLibraryDaoTest {
    private Connection connection;
    private GameCatalogDao catalog;
    private GameDao games;
    private UserDao users;

    @Before
    public void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        SchemaInitializer.apply(connection);
        catalog = new GameCatalogDao(connection);
        games = new GameDao(connection);
        users = new UserDao(connection);
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void gameDaoCreatesReadsUpdatesAndDeletesGames() throws Exception {
        Game draft = new Game(
                0, 7, "DAO Test Puzzle", "https://example.com/puzzle",
                "2025-01-02", "Puzzle");
        int gameId = games.create(draft);

        Game stored = games.findById(gameId).orElseThrow();
        assertEquals("DAO Test Puzzle", stored.title());
        assertEquals(7, stored.genreId());
        assertEquals("2025-01-02", stored.releaseDate());

        Game updated = new Game(
                gameId, 7, "Updated DAO Puzzle", "https://example.com/updated",
                "2025-01-03", "Puzzle");
        assertTrue(games.update(updated));
        assertEquals("Updated DAO Puzzle", games.findById(gameId).orElseThrow().title());

        users.addGameToLibrary(new UserGame(1, gameId, 5, 2));
        assertTrue(catalog.findGamePlayers()
                .contains(new GamePlayerEntry("Updated DAO Puzzle", "player01")));

        assertTrue(games.delete(gameId));
        assertFalse(games.findById(gameId).isPresent());
        assertFalse(catalog.findGamePlayers()
                .contains(new GamePlayerEntry("Updated DAO Puzzle", "player01")));
    }

    @Test
    public void userDaoCreatesReadsUpdatesAndDeletesUsers() throws Exception {
        int userId = users.create(new User(0, "dao_player", "dao@example.com"));

        User stored = users.findById(userId).orElseThrow();
        assertEquals("dao_player", stored.username());

        User updated = new User(userId, "updated_player", "updated@example.com");
        assertTrue(users.update(updated));
        assertEquals("updated_player", users.findById(userId).orElseThrow().username());

        users.addGameToLibrary(new UserGame(userId, 1, 4, 9));
        assertTrue(catalog.findGamePlayers()
                .contains(new GamePlayerEntry("Minecraft", "updated_player")));

        assertTrue(users.delete(userId));
        assertFalse(users.findById(userId).isPresent());
        assertFalse(catalog.findGamePlayers()
                .contains(new GamePlayerEntry("Minecraft", "updated_player")));
    }

    @Test
    public void gamePlayerListShowsOnlyGameAndUsername() throws Exception {
        List<GamePlayerEntry> entries = catalog.findGamePlayers();
        assertTrue(entries.contains(new GamePlayerEntry("Minecraft", "player01")));
        GamePlayerEntry minecraft = entries.stream()
                .filter(entry -> entry.gameTitle().equals("Minecraft"))
                .findFirst()
                .orElseThrow();
        assertEquals("player01", minecraft.username());
    }

    @Test
    public void userGameRejectsInvalidHiddenValues() {
        assertThrows(IllegalArgumentException.class, () -> new UserGame(1, 1, 6, 0));
        assertThrows(IllegalArgumentException.class, () -> new UserGame(1, 1, null, -1));
    }

    @Test
    public void databaseRejectsDuplicateGameForOneUser() throws Exception {
        int userId = users.create(new User(0, "duplicate_test_player", "duplicate_test@example.com"));
        int gameId = games.create(new Game(
                0, 7, "Duplicate Test Puzzle", "https://example.com/duplicate-test",
                "2025-01-04", "Puzzle"));
        UserGame saved = new UserGame(userId, gameId, null, 0);
        users.addGameToLibrary(saved);
        assertThrows(SQLException.class, () -> users.addGameToLibrary(saved));
    }

    @Test
    public void savedGameCountsIncludeUsersWithNoGames() throws Exception {
        users.create(new User(0, "empty_library_user", "empty_library@example.com"));

        List<UserGameCount> counts = catalog.countSavedGamesByUser();

        assertTrue(counts.contains(new UserGameCount("player01", 2)));
        assertTrue(counts.contains(new UserGameCount("empty_library_user", 0)));
    }

    @Test
    public void titleSortUsesJavaInsertionSortWithoutChangingInput() {
        Game zulu = new Game(
                3, 7, "zulu", "https://example.com/zulu", "2025-01-03", "Puzzle");
        Game alpha = new Game(
                1, 7, "Alpha", "https://example.com/alpha", "2025-01-01", "Puzzle");
        Game bravo = new Game(
                2, 7, "BRAVO", "https://example.com/bravo", "2025-01-02", "Puzzle");
        List<Game> original = List.of(zulu, alpha, bravo);

        List<Game> sorted = GameAlgorithms.sortByTitle(original);

        assertEquals(List.of("Alpha", "BRAVO", "zulu"),
                sorted.stream().map(Game::title).toList());
        assertEquals("zulu", original.get(0).title());
    }
}
