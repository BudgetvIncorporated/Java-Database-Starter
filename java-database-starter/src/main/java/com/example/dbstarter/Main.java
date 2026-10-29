package com.example.dbstarter;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Scanner;

public final class Main {
    private static final int EXIT = 0;

    private Main() {
    }

    public static void main(String[] args) {
        try (Connection connection = Database.connect()) {
            SchemaInitializer.apply(connection);
            System.out.println("Connected to the SQLite game catalog.");
            runMenu(new Scanner(System.in), new GameCatalogDao(connection),
                    new GameDao(connection), new DeveloperDao(connection));
        } catch (SQLException | IOException exception) {
            System.err.println("Could not start the game catalog: " + exception.getMessage());
        }
    }

    private static void runMenu(Scanner input, GameCatalogDao catalog,
                                GameDao games, DeveloperDao developers) {
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
                    case 7 -> showDeveloperReport(catalog);
                    case 8 -> addGame(input, catalog, games, developers);
                    case 9 -> editGame(input, catalog, games, developers);
                    case 10 -> deleteGame(input, catalog, games);
                    case 11 -> showDevelopers(developers.findAllDevelopers());
                    case 12 -> addDeveloper(input, developers);
                    case 13 -> editDeveloper(input, developers);
                    case 14 -> deleteDeveloper(input, developers);
                    default -> System.out.println("Please choose a number from 0 to 14.");
                }
            } catch (SQLException exception) {
                System.err.println("Database error. You can try another menu option: "
                        + exception.getMessage());
            } catch (IllegalArgumentException exception) {
                System.out.println("Invalid catalog value: " + exception.getMessage());
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
        System.out.println("7. Games per developer report");
        System.out.println("8. Add a game");
        System.out.println("9. Edit a game");
        System.out.println("10. Delete a game");
        System.out.println("11. List developers");
        System.out.println("12. Add a developer");
        System.out.println("13. Edit a developer");
        System.out.println("14. Delete a developer");
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

    private static void showDeveloperReport(GameCatalogDao catalog) throws SQLException {
        List<DeveloperGameCount> counts = catalog.countGamesPerDeveloper();
        if (counts.isEmpty()) {
            System.out.println("No developers are available.");
            return;
        }
        System.out.println("Games per developer:");
        System.out.printf("%-30s %s%n", "Developer", "Games");
        System.out.println("-".repeat(39));
        for (DeveloperGameCount count : counts) {
            System.out.printf("%-30s %d%n",
                    count.developerName(), count.gameCount());
        }
    }

    private static void addGame(Scanner input, GameCatalogDao catalog, GameDao games,
                                DeveloperDao developers) throws SQLException {
        List<Developer> availableDevelopers = developers.findAllDevelopers();
        Developer developer = chooseDeveloper(input, availableDevelopers, null);
        if (developer == null) {
            System.out.println("Game creation cancelled.");
            return;
        }

        String title = readRequired(input, "Game title (blank cancels): ", null);
        if (title == null) {
            System.out.println("Game creation cancelled.");
            return;
        }
        String url = readWebUrl(input, "Game URL (http:// or https://; blank cancels): ",
                null, false, false);
        if (url == null) {
            System.out.println("Game creation cancelled.");
            return;
        }
        String publishedOn = readIsoDate(input, "Publication date (YYYY-MM-DD; blank cancels): ",
                null);
        if (publishedOn == null) {
            System.out.println("Game creation cancelled.");
            return;
        }
        List<Integer> genreIds = chooseGenreIds(input, catalog.findAllGenres(), List.of(), false);
        if (genreIds == null) {
            System.out.println("Game creation cancelled.");
            return;
        }

        int gameId = games.createGame(
                new Game(0, title, url, publishedOn, developer.id(), "", ""), genreIds);
        System.out.println("Added game with ID " + gameId + ".");
    }

    private static void editGame(Scanner input, GameCatalogDao catalog, GameDao games,
                                 DeveloperDao developers) throws SQLException {
        List<Game> allGames = catalog.findAllGames();
        Game current = chooseGame(input, allGames, "Game ID to edit: ");
        if (current == null) {
            return;
        }

        String title = readRequired(input, "Title [" + current.title() + "]: ", current.title());
        String url = readWebUrl(input, "Game URL [" + current.url() + "]: ",
                current.url(), false, false);
        String publishedOn = readIsoDate(input,
                "Publication date [" + current.publishedOn() + "]: ", current.publishedOn());
        if (title == null || url == null || publishedOn == null) {
            return;
        }

        List<Developer> availableDevelopers = developers.findAllDevelopers();
        Developer developer = chooseDeveloper(input, availableDevelopers, current.developerId());
        if (developer == null) {
            return;
        }
        List<Integer> currentGenreIds = games.findGenreIdsForGame(current.id());
        List<Integer> genreIds = chooseGenreIds(
                input, catalog.findAllGenres(), currentGenreIds, true);
        if (genreIds == null) {
            return;
        }

        Game updated = new Game(current.id(), title, url, publishedOn,
                developer.id(), current.developerName(), current.genres());
        if (games.updateGame(updated, genreIds)) {
            System.out.println("Updated " + updated.title() + ".");
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
        if (!confirmDelete(input, "Delete \"" + game.title() + "\"? Type yes to confirm: ")) {
            System.out.println("Game deletion cancelled.");
            return;
        }
        System.out.println(games.deleteGame(game.id())
                ? "Deleted " + game.title() + "."
                : "That game no longer exists.");
    }

    private static void addDeveloper(Scanner input, DeveloperDao developers) throws SQLException {
        String name = readRequired(input, "Developer name (blank cancels): ", null);
        if (name == null) {
            System.out.println("Developer creation cancelled.");
            return;
        }
        String websiteUrl = readWebUrl(input,
                "Website URL (optional; http:// or https://): ", null, true, false);
        int developerId = developers.createDeveloper(new Developer(0, name, websiteUrl));
        System.out.println("Added developer with ID " + developerId + ".");
    }

    private static void editDeveloper(Scanner input, DeveloperDao developers) throws SQLException {
        Developer current = chooseDeveloper(input, developers.findAllDevelopers(), null);
        if (current == null) {
            return;
        }
        String name = readRequired(input, "Developer name [" + current.name() + "]: ",
                current.name());
        String websiteUrl = readWebUrl(input,
                "Website URL [" + displayWebsite(current.websiteUrl())
                        + "] (Enter keeps it, - clears it): ",
                current.websiteUrl(), true, true);
        if (name == null) {
            return;
        }
        Developer updated = new Developer(current.id(), name, websiteUrl);
        if (developers.updateDeveloper(updated)) {
            System.out.println("Updated " + updated.name() + ".");
        } else {
            System.out.println("That developer no longer exists.");
        }
    }

    private static void deleteDeveloper(Scanner input, DeveloperDao developers)
            throws SQLException {
        Developer developer = chooseDeveloper(input, developers.findAllDevelopers(), null);
        if (developer == null) {
            return;
        }
        if (!confirmDelete(input,
                "Delete \"" + developer.name() + "\"? Type yes to confirm: ")) {
            System.out.println("Developer deletion cancelled.");
            return;
        }
        System.out.println(developers.deleteDeveloper(developer.id())
                ? "Deleted " + developer.name() + "."
                : "That developer no longer exists.");
    }

    private static Game chooseGame(Scanner input, List<Game> games, String prompt) {
        if (games.isEmpty()) {
            System.out.println("There are no games available.");
            return null;
        }
        showGames(games);
        Set<Integer> validIds = new HashSet<>();
        for (Game game : games) {
            validIds.add(game.id());
        }
        Integer selectedId = readListedId(input, prompt, validIds, null);
        if (selectedId == null) {
            return null;
        }
        return games.stream().filter(game -> game.id() == selectedId).findFirst().orElse(null);
    }

    private static Developer chooseDeveloper(Scanner input, List<Developer> developers,
                                             Integer defaultId) {
        if (developers.isEmpty()) {
            System.out.println("Add a developer before creating a game.");
            return null;
        }
        showDevelopers(developers);
        Set<Integer> validIds = new HashSet<>();
        for (Developer developer : developers) {
            validIds.add(developer.id());
        }
        String prompt = defaultId == null
                ? "Developer ID (blank cancels): "
                : "Developer ID [" + defaultId + "] (Enter keeps it): ";
        Integer selectedId = readListedId(input, prompt, validIds, defaultId);
        if (selectedId == null) {
            return null;
        }
        return developers.stream()
                .filter(developer -> developer.id() == selectedId)
                .findFirst()
                .orElse(null);
    }

    private static List<Integer> chooseGenreIds(Scanner input, List<Genre> genres,
                                                List<Integer> defaults, boolean editing) {
        if (genres.isEmpty()) {
            System.out.println("No genres are available; the game will have no genres.");
            return List.of();
        }
        System.out.println("Available genres:");
        for (Genre genre : genres) {
            System.out.printf("%d. %s%n", genre.id(), genre.name());
        }
        String defaultText = defaults.isEmpty() ? "none"
                : defaults.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        String prompt = editing
                ? "Genre IDs [" + defaultText + "] (Enter keeps them, - clears, q cancels): "
                : "Genre IDs separated by commas (blank for none, q cancels): ";
        Set<Integer> validIds = new HashSet<>();
        for (Genre genre : genres) {
            validIds.add(genre.id());
        }

        while (true) {
            String value = readLine(input, prompt);
            if (value == null || value.trim().equalsIgnoreCase("q")) {
                return null;
            }
            if (value.isBlank()) {
                return defaults;
            }
            if (value.trim().equals("-")) {
                return List.of();
            }

            List<Integer> selected = new ArrayList<>();
            boolean valid = true;
            for (String part : value.split(",")) {
                try {
                    int genreId = Integer.parseInt(part.trim());
                    if (!validIds.contains(genreId) || selected.contains(genreId)) {
                        valid = false;
                        break;
                    }
                    selected.add(genreId);
                } catch (NumberFormatException exception) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                return selected;
            }
            System.out.println("Enter each available genre ID once, separated by commas.");
        }
    }

    private static Integer readListedId(Scanner input, String prompt, Set<Integer> validIds,
                                        Integer defaultId) {
        while (true) {
            String value = readLine(input, prompt);
            if (value == null) {
                return null;
            }
            if (value.isBlank()) {
                return defaultId;
            }
            try {
                int selectedId = Integer.parseInt(value.trim());
                if (validIds.contains(selectedId)) {
                    return selectedId;
                }
                System.out.println("Choose an ID shown in the list.");
            } catch (NumberFormatException exception) {
                System.out.println("Enter a whole-number ID.");
            }
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

    private static String readWebUrl(Scanner input, String prompt, String defaultValue,
                                     boolean optional, boolean allowClear) {
        while (true) {
            String value = readLine(input, prompt);
            if (value == null) {
                return null;
            }
            if (value.isBlank()) {
                return defaultValue;
            }
            if (allowClear && value.trim().equals("-")) {
                return null;
            }
            if (optional && value.trim().equals("-")) {
                return null;
            }
            try {
                return CatalogValidation.requireWebUrl(value, "Website URL");
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

    private static boolean confirmDelete(Scanner input, String prompt) {
        String response = readLine(input, prompt);
        return response != null && response.trim().equalsIgnoreCase("yes");
    }

    private static void showDevelopers(List<Developer> developers) {
        if (developers.isEmpty()) {
            System.out.println("No developers are available.");
            return;
        }
        System.out.printf("%-4s %-32s %s%n", "ID", "Developer", "Website");
        System.out.println("-".repeat(95));
        for (Developer developer : developers) {
            System.out.printf("%-4d %-32s %s%n",
                    developer.id(), shorten(developer.name(), 32),
                    displayWebsite(developer.websiteUrl()));
        }
    }

    private static String displayWebsite(String websiteUrl) {
        return websiteUrl == null ? "(none)" : websiteUrl;
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

        System.out.printf("%-4s %-34s %-12s %-22s %s%n",
                "ID", "Title", "Released", "Developer", "Genres");
        System.out.println("-".repeat(105));
        for (Game game : sortedGames) {
            System.out.printf("%-4d %-34s %-12s %-22s %s%n",
                    game.id(), shorten(game.title(), 34), game.releaseDate(),
                    shorten(game.developerName(), 22), game.genreName());
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
