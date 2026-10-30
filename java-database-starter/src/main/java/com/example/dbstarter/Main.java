package com.example.dbstarter;

import java.awt.Desktop;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try (Connection connection = Database.connect()) {
            SchemaInitializer.apply(connection);
            System.out.println("Connected to the SQLite game library.");
            runMenu(
                    new Scanner(System.in),
                    new GameCatalogDao(connection),
                    new GameDao(connection),
                    new UserDao(connection));
        } catch (Exception exception) {
            System.err.println("Could not start the game library: " + exception.getMessage());
        }
    }

    private static void runMenu(Scanner input, GameCatalogDao catalog,
                                GameDao games, UserDao users) {
        while (true) {
            printMenu();
            Integer choice = readInteger(input, "Choose an option: ");
            if (choice == null || choice == 0) {
                System.out.println("Goodbye.");
                return;
            }

            try {
                switch (choice) {
                    case 1 -> showGamePlayers(catalog.findGamePlayers());
                    case 2 -> searchGamePlayers(input, catalog);
                    case 3 -> filterGamePlayersByGenre(input, catalog);
                    case 4 -> addUser(input, users);
                    case 5 -> editUser(input, catalog, users);
                    case 6 -> deleteUser(input, catalog, users);
                    case 7 -> addGame(input, catalog, games);
                    case 8 -> editGame(input, catalog, games);
                    case 9 -> deleteGame(input, catalog, games);
                    case 10 -> addGameToUser(input, catalog, users);
                    case 11 -> openGame(input, catalog);
                    default -> System.out.println("Choose a number from 0 to 11.");
                }
            } catch (SQLException exception) {
                System.out.println("The database could not complete that action: "
                        + exception.getMessage());
            } catch (IllegalArgumentException exception) {
                System.out.println("Please check that information: " + exception.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== Game Library ===");
        System.out.println("1. Show games and the players who have them");
        System.out.println("2. Search the player/game list");
        System.out.println("3. Filter the player/game list by genre");
        System.out.println("4. Add a user");
        System.out.println("5. Edit a user");
        System.out.println("6. Delete a user");
        System.out.println("7. Add a game");
        System.out.println("8. Edit a game");
        System.out.println("9. Delete a game");
        System.out.println("10. Add a game to a user's library");
        System.out.println("11. Open a game's saved link");
        System.out.println("0. Exit");
    }

    private static void showGamePlayers(List<GamePlayerEntry> entries) {
        if (entries.isEmpty()) {
            System.out.println("No games have been added to a user's library yet.");
            return;
        }
        System.out.printf("%-42s %s%n", "Game", "Player");
        System.out.println("-".repeat(66));
        for (GamePlayerEntry entry : entries) {
            System.out.printf("%-42s %s%n",
                    shorten(entry.gameTitle(), 42), entry.username());
        }
    }

    private static void searchGamePlayers(Scanner input, GameCatalogDao catalog)
            throws SQLException {
        String search = readRequired(input, "Game title to search for: ", null);
        if (search != null) {
            showGamePlayers(catalog.searchGamePlayers(search));
        }
    }

    private static void filterGamePlayersByGenre(Scanner input, GameCatalogDao catalog)
            throws SQLException {
        Genre genre = chooseGenre(input, catalog.findAllGenres(), null);
        if (genre != null) {
            showGamePlayers(catalog.findGamePlayersByGenre(genre.id()));
        }
    }

    private static void addUser(Scanner input, UserDao users) throws SQLException {
        String username = readRequired(input, "Username (blank cancels): ", null);
        if (username == null) {
            System.out.println("User entry cancelled.");
            return;
        }
        String email = readEmail(input, "Email (blank cancels): ", null);
        if (email == null) {
            System.out.println("User entry cancelled.");
            return;
        }
        int id = users.create(new User(0, username, email));
        System.out.println("User added. Their ID is " + id + ".");
    }

    private static void editUser(Scanner input, GameCatalogDao catalog, UserDao users)
            throws SQLException {
        User current = chooseUser(input, catalog.findAllUsers(), "User ID to edit: ");
        if (current == null) {
            return;
        }
        String username = readRequired(input, "Username [" + current.username() + "]: ",
                current.username());
        String email = readEmail(input, "Email [" + current.email() + "]: ", current.email());
        if (username == null || email == null) {
            return;
        }
        if (users.update(new User(current.id(), username, email))) {
            System.out.println("User updated.");
        } else {
            System.out.println("That user no longer exists.");
        }
    }

    private static void deleteUser(Scanner input, GameCatalogDao catalog, UserDao users)
            throws SQLException {
        User user = chooseUser(input, catalog.findAllUsers(), "User ID to delete: ");
        if (user == null) {
            return;
        }
        if (!confirm(input, "Deleting this user also removes their game links. Type yes: ")) {
            System.out.println("User deletion cancelled.");
            return;
        }
        System.out.println(users.delete(user.id())
                ? "User deleted."
                : "That user no longer exists.");
    }

    private static void addGame(Scanner input, GameCatalogDao catalog, GameDao games)
            throws SQLException {
        Genre genre = chooseGenre(input, catalog.findAllGenres(), null);
        if (genre == null) {
            return;
        }
        String title = readRequired(input, "Game title (blank cancels): ", null);
        String url = readWebUrl(input, "Game link (http:// or https://): ", null);
        String date = readIsoDate(input, "Release date (YYYY-MM-DD): ", null);
        if (title == null || url == null || date == null) {
            System.out.println("Game entry cancelled.");
            return;
        }
        int id = games.create(new Game(0, genre.id(), title, url, date, genre.name()));
        System.out.println("Game added. Its ID is " + id + ".");
    }

    private static void editGame(Scanner input, GameCatalogDao catalog, GameDao games)
            throws SQLException {
        Game current = chooseGame(input, catalog.findAllGames(), "Game ID to edit: ");
        if (current == null) {
            return;
        }
        String title = readRequired(input, "Title [" + current.title() + "]: ",
                current.title());
        String url = readWebUrl(input, "Game link [" + current.url() + "]: ", current.url());
        String date = readIsoDate(input, "Release date [" + current.releaseDate() + "]: ",
                current.releaseDate());
        Genre genre = chooseGenre(input, catalog.findAllGenres(), current.genreId());
        if (title == null || url == null || date == null || genre == null) {
            System.out.println("Game edit cancelled.");
            return;
        }
        if (games.update(new Game(
                current.id(), genre.id(), title, url, date, genre.name()))) {
            System.out.println("Game updated.");
        } else {
            System.out.println("That game no longer exists.");
        }
    }

    private static void deleteGame(Scanner input, GameCatalogDao catalog, GameDao games)
            throws SQLException {
        Game game = chooseGame(input, catalog.findAllGames(), "Game ID to delete: ");
        if (game == null) {
            return;
        }
        if (!confirm(input, "Deleting this game also removes its player links. Type yes: ")) {
            System.out.println("Game deletion cancelled.");
            return;
        }
        System.out.println(games.delete(game.id())
                ? "Game deleted."
                : "That game no longer exists.");
    }

    private static void addGameToUser(Scanner input, GameCatalogDao catalog, UserDao users)
            throws SQLException {
        User user = chooseUser(input, catalog.findAllUsers(), "User ID (blank cancels): ");
        if (user == null) {
            return;
        }
        Game game = chooseGame(input, catalog.findAllGames(), "Game ID (blank cancels): ");
        if (game == null) {
            return;
        }

        Integer rating = readOptionalRating(input);
        Integer playCount = readOptionalCount(input);
        if (playCount == null) {
            return;
        }

        users.addGameToLibrary(new UserGame(user.id(), game.id(), rating, playCount));
        System.out.println("Saved " + game.title() + " to " + user.username() + "'s library.");
    }

    private static Integer readOptionalRating(Scanner input) {
        while (true) {
            String value = readLine(input, "Rating 1-5 (blank leaves it unrated): ");
            if (value == null) {
                return null;
            }
            if (value.isBlank()) {
                return null;
            }
            try {
                int rating = Integer.parseInt(value.trim());
                if (rating >= 1 && rating <= 5) {
                    return rating;
                }
            } catch (NumberFormatException ignored) {
                // Show the same helpful message as for a number outside the allowed range.
            }
            System.out.println("Enter a whole number from 1 to 5, or leave it blank.");
        }
    }

    private static Integer readOptionalCount(Scanner input) {
        while (true) {
            String value = readLine(input, "Play count (blank means 0, q cancels): ");
            if (value == null || value.trim().equalsIgnoreCase("q")) {
                return null;
            }
            if (value.isBlank()) {
                return 0;
            }
            try {
                int count = Integer.parseInt(value.trim());
                if (count >= 0) {
                    return count;
                }
            } catch (NumberFormatException ignored) {
                // Show the same helpful message as for a negative or non-numeric value.
            }
            System.out.println("Enter 0 or a positive whole number, or leave it blank.");
        }
    }

    private static void openGame(Scanner input, GameCatalogDao catalog) throws SQLException {
        Game game = chooseGame(input, catalog.findAllGames(), "Game ID to open: ");
        if (game == null) {
            return;
        }
        try {
            URI uri = URI.create(game.url());
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                System.out.println("Opened the saved link for " + game.title() + ".");
            } else {
                System.out.println("This environment cannot open a browser. Saved link: "
                        + game.url());
            }
        } catch (Exception exception) {
            System.out.println("Could not open the browser. Saved link: " + game.url());
        }
    }

    private static User chooseUser(Scanner input, List<User> users, String prompt) {
        if (users.isEmpty()) {
            System.out.println("No users are available. Add a user first.");
            return null;
        }
        System.out.printf("%-6s %s%n", "ID", "Username");
        for (User user : users) {
            System.out.printf("%-6d %s%n", user.id(), user.username());
        }
        Integer id = readListedId(input, prompt, users.stream().map(User::id).toList(), null);
        return id == null ? null : users.stream().filter(user -> user.id() == id)
                .findFirst().orElse(null);
    }

    private static Game chooseGame(Scanner input, List<Game> games, String prompt) {
        if (games.isEmpty()) {
            System.out.println("No games are available. Add a game first.");
            return null;
        }
        showGamesForSelection(games);
        Integer id = readListedId(input, prompt, games.stream().map(Game::id).toList(), null);
        return id == null ? null : games.stream().filter(game -> game.id() == id)
                .findFirst().orElse(null);
    }

    private static Genre chooseGenre(Scanner input, List<Genre> genres, Integer defaultId) {
        if (genres.isEmpty()) {
            System.out.println("No genres are available.");
            return null;
        }
        System.out.printf("%-6s %s%n", "ID", "Genre");
        for (Genre genre : genres) {
            System.out.printf("%-6d %s%n", genre.id(), genre.name());
        }
        String prompt = defaultId == null
                ? "Genre ID (blank cancels): "
                : "Genre ID [" + defaultId + "] (Enter keeps it): ";
        Integer id = readListedId(input, prompt, genres.stream().map(Genre::id).toList(),
                defaultId);
        return id == null ? null : genres.stream().filter(genre -> genre.id() == id)
                .findFirst().orElse(null);
    }

    private static Integer readListedId(Scanner input, String prompt,
                                        List<Integer> validIds, Integer defaultId) {
        Set<Integer> allowed = new HashSet<>(validIds);
        while (true) {
            String value = readLine(input, prompt);
            if (value == null) {
                return null;
            }
            if (value.isBlank()) {
                return defaultId;
            }
            try {
                int id = Integer.parseInt(value.trim());
                if (allowed.contains(id)) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // Use the common message below for all invalid IDs.
            }
            System.out.println("Choose an ID from the list.");
        }
    }

    private static void showGamesForSelection(List<Game> games) {
        System.out.printf("%-6s %s%n", "ID", "Game");
        for (Game game : games) {
            System.out.printf("%-6d %s%n", game.id(), game.title());
        }
    }

    private static String readRequired(Scanner input, String prompt, String defaultValue) {
        while (true) {
            String value = readLine(input, prompt);
            if (value == null) {
                return null;
            }
            if (value.isBlank()) {
                return defaultValue;
            }
            try {
                return CatalogValidation.requireText(value, "Value");
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }

    private static String readEmail(Scanner input, String prompt, String defaultValue) {
        while (true) {
            String value = readLine(input, prompt);
            if (value == null) {
                return null;
            }
            if (value.isBlank()) {
                return defaultValue;
            }
            try {
                return CatalogValidation.requireEmail(value);
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }

    private static String readWebUrl(Scanner input, String prompt, String defaultValue) {
        while (true) {
            String value = readLine(input, prompt);
            if (value == null) {
                return null;
            }
            if (value.isBlank()) {
                return defaultValue;
            }
            try {
                return CatalogValidation.requireWebUrl(value, "Game link");
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }

    private static String readIsoDate(Scanner input, String prompt, String defaultValue) {
        while (true) {
            String value = readLine(input, prompt);
            if (value == null) {
                return null;
            }
            if (value.isBlank()) {
                return defaultValue;
            }
            try {
                return CatalogValidation.requireIsoDate(value);
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
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
                System.out.println("Enter a whole number.");
            }
        }
    }

    private static boolean confirm(Scanner input, String prompt) {
        String response = readLine(input, prompt);
        return response != null && response.trim().equalsIgnoreCase("yes");
    }

    private static String shorten(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    private static String readLine(Scanner input, String prompt) {
        System.out.print(prompt);
        return input.hasNextLine() ? input.nextLine() : null;
    }
}
