package com.example.dbstarter;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public final class Main {
    private static final int EXIT = 0;

    private Main() {
    }

    public static void main(String[] args) {
        try (Connection connection = Database.connect()) {
            SchemaInitializer.apply(connection);
            System.out.println("Connected to the SQLite game catalog.");
            runMenu(new Scanner(System.in), new GameCatalogDao(connection));
        } catch (SQLException | IOException exception) {
            System.err.println("Could not start the game catalog: " + exception.getMessage());
        }
    }

    private static void runMenu(Scanner input, GameCatalogDao catalog) {
        while (true) {
            printMenu();
            Integer choice = readInteger(input, "Choose an option: ");
            if (choice == null || choice == EXIT) {
                System.out.println("Goodbye.");
                return;
            }

            try {
                switch (choice) {
                    case 1 -> showGames(catalog.findAllGames());
                    case 2 -> searchGames(input, catalog);
                    case 3 -> filterByGenre(input, catalog);
                    case 4 -> openGame(input, catalog);
                    case 5 -> showUserReport(catalog);
                    case 6 -> showUserLibrary(input, catalog);
                    default -> System.out.println("Please choose a number from 0 to 6.");
                }
            } catch (SQLException exception) {
                System.err.println("Database error. You can try another menu option: "
                        + exception.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== Game Library ===");
        System.out.println("1. Browse all games (sorted by title)");
        System.out.println("2. Search games by title");
        System.out.println("3. Filter games by genre");
        System.out.println("4. Open a game's saved page");
        System.out.println("5. User game activity report");
        System.out.println("6. View a user's game library");
        System.out.println("0. Exit");
    }

    private static void searchGames(Scanner input, GameCatalogDao catalog) throws SQLException {
        String searchTerm = readLine(input, "Enter a title or part of a title: ");
        if (searchTerm == null) {
            return;
        }
        if (searchTerm.isBlank()) {
            System.out.println("Search text cannot be empty.");
            return;
        }
        showGames(catalog.searchGames(searchTerm.trim()));
    }

    private static void filterByGenre(Scanner input, GameCatalogDao catalog) throws SQLException {
        List<Genre> genres = catalog.findAllGenres();
        if (genres.isEmpty()) {
            System.out.println("No genres are available.");
            return;
        }

        System.out.println("Genres:");
        for (int i = 0; i < genres.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, genres.get(i).name());
        }
        Integer selection = readInteger(input, "Choose a genre number: ");
        if (selection == null) {
            return;
        }
        if (selection < 1 || selection > genres.size()) {
            System.out.println("That genre number is not in the list.");
            return;
        }

        Genre genre = genres.get(selection - 1);
        System.out.println("Games in " + genre.name() + ":");
        showGames(catalog.findGamesByGenre(genre.name()));
    }

    private static void openGame(Scanner input, GameCatalogDao catalog) throws SQLException {
        List<Game> games = catalog.findAllGames();
        if (games.isEmpty()) {
            System.out.println("There are no games to open.");
            return;
        }

        System.out.println("Enter the game ID shown in the list:");
        showGames(games);
        Game selected = readGame(input, games);
        if (selected == null) {
            return;
        }

        try {
            URI gameUri = URI.create(selected.url());
            if (!Desktop.isDesktopSupported()) {
                System.out.println("This environment cannot open a browser. Saved page: " + gameUri);
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                System.out.println("This environment cannot open a browser. Saved page: " + gameUri);
                return;
            }
            desktop.browse(gameUri);
            System.out.println("Opened " + selected.title() + " in your browser.");
        } catch (Exception exception) {
            System.out.println("Could not open the browser. Saved page: " + selected.url());
        }
    }

    private static void showUserReport(GameCatalogDao catalog) throws SQLException {
        List<UserGameSummary> summaries = catalog.summarizeUsers();
        if (summaries.isEmpty()) {
            System.out.println("No users are available.");
            return;
        }
        System.out.println("Game activity per user:");
        System.out.printf("%-18s %-8s %-12s %s%n",
                "Username", "Games", "Avg rating", "Play count");
        System.out.println("-".repeat(54));
        for (UserGameSummary summary : summaries) {
            System.out.printf("%-18s %-8d %-12.1f %d%n",
                    summary.username(), summary.gameCount(),
                    summary.averageRating(), summary.totalPlayCount());
        }
    }

    private static void showUserLibrary(Scanner input, GameCatalogDao catalog)
            throws SQLException {
        List<User> users = catalog.findAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users are available.");
            return;
        }

        System.out.println("Sample users:");
        for (User user : users) {
            System.out.println("- " + user.username());
        }
        String username = readLine(input, "Enter a username: ");
        if (username == null || username.isBlank()) {
            return;
        }

        List<UserGame> userGames = catalog.findGamesForUser(username.trim());
        if (userGames.isEmpty()) {
            System.out.println("No saved games found for that username.");
            return;
        }

        System.out.printf("%-34s %-18s %-12s %-8s %s%n",
                "Title", "Genre", "Published", "Rating", "Plays");
        System.out.println("-".repeat(82));
        for (UserGame userGame : userGames) {
            String rating = userGame.rating() == null ? "—" : userGame.rating().toString();
            Game game = userGame.game();
            System.out.printf("%-34s %-18s %-12s %-8s %d%n",
                    shorten(game.title(), 34), shorten(game.genreName(), 18),
                    game.releaseDate(), rating, userGame.playCount());
        }
    }

    private static void showGames(List<Game> games) {
        if (games.isEmpty()) {
            System.out.println("No games matched.");
            return;
        }

        // Sorting is performed in Java after the rows are loaded from SQLite.
        List<Game> sortedGames = new ArrayList<>(games);
        sortedGames.sort(Comparator.comparing(Game::title, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Game::title));

        System.out.printf("%-4s %-42s %-12s %s%n",
                "ID", "Title", "Released", "Genre");
        System.out.println("-".repeat(82));
        for (Game game : sortedGames) {
            System.out.printf("%-4d %-42s %-12s %s%n",
                    game.id(), shorten(game.title(), 42), game.releaseDate(),
                    game.genreName());
        }
    }

    private static String shorten(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private static Game readGame(Scanner input, List<Game> games) {
        while (true) {
            String value = readLine(input, "Game ID (or press Enter to cancel): ");
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                int gameId = Integer.parseInt(value.trim());
                for (Game game : games) {
                    if (game.id() == gameId) {
                        return game;
                    }
                }
                System.out.println("No game has that ID. Choose an ID from the list.");
            } catch (NumberFormatException exception) {
                System.out.println("Enter a whole-number game ID.");
            }
        }
    }

    private static Integer readInteger(Scanner input, String prompt) {
        while (true) {
            String value = readLine(input, prompt);
            if (value == null) {
                return null;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static String readLine(Scanner input, String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) {
            return null;
        }
        return input.nextLine();
    }
}
