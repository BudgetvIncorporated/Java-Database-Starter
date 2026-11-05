package com.example.dbstarter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class GameLibraryFrame extends JFrame {
    private static final Color INK = new Color(28, 38, 53);
    private static final Color MUTED = new Color(103, 116, 133);
    private static final Color ACCENT = new Color(49, 103, 166);

    private final Connection connection;
    private final GameCatalogDao catalog;
    private final GameDao games;
    private final UserDao users;
    private final JTable gameTable = table();
    private final JTable userTable = table();
    private final JTable savedGamesTable = table();
    private final JTable statsTable = table();
    private final JTextField searchField = new JTextField(22);
    private final JTextField savedGamePlayerFilter = new JTextField(12);
    private final JTextField savedGameTitleFilter = new JTextField(12);
    private final JComboBox<GenreChoice> genreFilter = new JComboBox<>();
    private final JLabel savedGameFilterCount = new JLabel("All saved games");
    private final JLabel savedGameNoMatches = new JLabel(
            "No saved games match these filters. Change the player or game title, or choose Clear filters.");
    private final JLabel status = new JLabel("Connected to your local SQLite game library.");
    private TableRowSorter<DefaultTableModel> savedGamesSorter;
    private List<Game> visibleGames = List.of();
    private List<User> visibleUsers = List.of();
    private List<SavedGameEntry> visibleSavedGames = List.of();
    private List<UserLibraryStats> visibleStats = List.of();

    GameLibraryFrame(Connection connection) {
        super("Game Library");
        this.connection = connection;
        catalog = new GameCatalogDao(connection);
        games = new GameDao(connection);
        users = new UserDao(connection);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(920, 610));
        setSize(1120, 740);
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                closeConnection();
                dispose();
            }
        });

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(BorderFactory.createEmptyBorder(22, 26, 14, 26));
        root.setBackground(new Color(246, 248, 251));
        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabs.addTab("Game catalog", buildCatalogTab());
        tabs.addTab("Users", buildUsersTab());
        tabs.addTab("Library & reports", buildLibraryTab());
        root.add(tabs, BorderLayout.CENTER);

        status.setForeground(MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
        reloadAll();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("GAME LIBRARY");
        title.setFont(new Font("SansSerif", Font.BOLD, 27));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Browse games, manage your collection, and review library stats.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel text = new JPanel(new GridBagLayout());
        text.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        text.add(title, c);
        c.gridy++;
        c.insets = new Insets(4, 0, 0, 0);
        text.add(subtitle, c);
        header.add(text, BorderLayout.WEST);
        JButton refresh = button("Refresh", false);
        refresh.addActionListener(event -> reloadAll());
        header.add(refresh, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCatalogTab() {
        JPanel panel = panel();
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setOpaque(false);
        controls.add(label("Search title"));
        controls.add(searchField);
        JButton search = button("Search", true);
        search.addActionListener(event -> refreshGames());
        searchField.addActionListener(event -> refreshGames());
        controls.add(search);
        controls.add(label("Genre"));
        genreFilter.setPreferredSize(new Dimension(160, 34));
        genreFilter.addActionListener(event -> refreshGames());
        controls.add(genreFilter);
        JButton clear = button("Clear", false);
        clear.addActionListener(event -> {
            searchField.setText("");
            genreFilter.setSelectedIndex(0);
            refreshGames();
        });
        controls.add(clear);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(gameTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 5));
        actions.setOpaque(false);
        JButton add = button("Add game", true);
        add.addActionListener(event -> addGame());
        JButton edit = button("Edit selected", false);
        edit.addActionListener(event -> editGame());
        JButton delete = button("Delete selected", false);
        delete.addActionListener(event -> deleteGame());
        JButton open = button("Open game link", false);
        open.addActionListener(event -> openGame());
        JButton sort = button("Sort by title (Java)", false);
        sort.addActionListener(event -> sortGames());
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(open);
        actions.add(sort);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildUsersTab() {
        JPanel panel = panel();
        JLabel intro = label("Create and manage the player accounts used by the library.");
        intro.setBorder(BorderFactory.createEmptyBorder(5, 4, 10, 0));
        panel.add(intro, BorderLayout.NORTH);
        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 5));
        actions.setOpaque(false);
        JButton add = button("Add user", true);
        add.addActionListener(event -> addUser());
        JButton edit = button("Edit selected", false);
        edit.addActionListener(event -> editUser());
        JButton delete = button("Delete selected", false);
        delete.addActionListener(event -> deleteUser());
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildLibraryTab() {
        JPanel panel = panel();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 5));
        top.setOpaque(false);
        JButton saveGame = button("Add saved game", true);
        saveGame.addActionListener(event -> addGameToUser());
        JButton editSavedGame = button("Edit selected", false);
        editSavedGame.addActionListener(event -> editSavedGame());
        JButton removeSavedGame = button("Remove selected", false);
        removeSavedGame.addActionListener(event -> removeSavedGame());
        JButton refresh = button("Refresh reports", false);
        refresh.addActionListener(event -> withDatabaseErrors(this::refreshReports));
        top.add(saveGame);
        top.add(editSavedGame);
        top.add(removeSavedGame);
        top.add(refresh);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.setOpaque(false);
        savedGamePlayerFilter.setToolTipText("Filter saved games by player name");
        savedGameTitleFilter.setToolTipText("Filter saved games by game title");
        filters.add(label("Player"));
        filters.add(savedGamePlayerFilter);
        filters.add(label("Game title"));
        filters.add(savedGameTitleFilter);
        JButton clearFilters = button("Clear filters", false);
        clearFilters.addActionListener(event -> {
            savedGamePlayerFilter.setText("");
            savedGameTitleFilter.setText("");
            applySavedGameFilters();
        });
        filters.add(clearFilters);
        filters.add(savedGameFilterCount);
        addSavedGameFilterListener(savedGamePlayerFilter);
        addSavedGameFilterListener(savedGameTitleFilter);

        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        controls.add(top, BorderLayout.NORTH);
        controls.add(filters, BorderLayout.SOUTH);
        panel.add(controls, BorderLayout.NORTH);

        JTabbedPane reportTabs = new JTabbedPane();
        JPanel savedGameResults = new JPanel(new BorderLayout(0, 8));
        savedGameResults.setOpaque(false);
        savedGameNoMatches.setForeground(INK);
        savedGameNoMatches.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        savedGameNoMatches.setVisible(false);
        savedGameResults.add(savedGameNoMatches, BorderLayout.NORTH);
        savedGameResults.add(new JScrollPane(savedGamesTable), BorderLayout.CENTER);
        reportTabs.addTab("Saved games (JOIN)", savedGameResults);
        reportTabs.addTab("User statistics (GROUP BY)", new JScrollPane(statsTable));
        panel.add(reportTabs, BorderLayout.CENTER);
        return panel;
    }

    private void reloadAll() {
        withDatabaseErrors(() -> {
            refreshGenres();
            refreshGames();
            refreshUsers();
            refreshReports();
            status.setText("Catalog, users, and reports are up to date.");
        });
    }

    private void refreshGenres() throws SQLException {
        Integer selectedId = selectedGenreId();
        genreFilter.removeAllItems();
        genreFilter.addItem(new GenreChoice(null, "All genres"));
        for (Genre genre : catalog.findAllGenres()) {
            genreFilter.addItem(new GenreChoice(genre.id(), genre.name()));
        }
        if (selectedId != null) {
            for (int index = 0; index < genreFilter.getItemCount(); index++) {
                GenreChoice choice = genreFilter.getItemAt(index);
                if (selectedId.equals(choice.id())) {
                    genreFilter.setSelectedIndex(index);
                    break;
                }
            }
        }
    }

    private void refreshGames() {
        withDatabaseErrors(() -> {
            Integer selectedId = TableSelection.selectedId(gameTable);
            List<? extends RowSorter.SortKey> sortKeys = TableSelection.sortKeys(gameTable);
            String query = searchField.getText().trim().toLowerCase();
            GenreChoice genre = (GenreChoice) genreFilter.getSelectedItem();
            visibleGames = catalog.findAllGames().stream()
                    .filter(game -> query.isEmpty()
                            || game.title().toLowerCase().contains(query))
                    .filter(game -> genre == null || genre.id() == null
                            || game.genreId() == genre.id())
                    .toList();
            fill(gameTable, new String[]{"ID", "Title", "Genre", "Published", "Game link"});
            for (Game game : visibleGames) {
                addRow(gameTable, game.id(), game.title(), game.genreName(),
                        game.releaseDate(), game.url());
            }
            TableSelection.restoreSort(gameTable.getRowSorter(), sortKeys);
            TableSelection.restoreId(gameTable, selectedId);
            status.setText(visibleGames.size() + " game(s) shown.");
        });
    }

    private void refreshUsers() throws SQLException {
        Integer selectedId = TableSelection.selectedId(userTable);
        List<? extends RowSorter.SortKey> sortKeys = TableSelection.sortKeys(userTable);
        visibleUsers = catalog.findAllUsers();
        fill(userTable, new String[]{"ID", "Username", "Email"});
        for (User user : visibleUsers) {
            addRow(userTable, user.id(), user.username(), user.email());
        }
        TableSelection.restoreSort(userTable.getRowSorter(), sortKeys);
        TableSelection.restoreId(userTable, selectedId);
    }

    private void refreshReports() throws SQLException {
        SavedGameEntry selected = savedGameAtViewRow(
                savedGamesTable, visibleSavedGames, savedGamesTable.getSelectedRow());
        List<? extends RowSorter.SortKey> sortKeys = savedGamesSorter == null
                ? List.of() : List.copyOf(savedGamesSorter.getSortKeys());
        visibleSavedGames = users.findSavedGames();
        savedGamesTable.setModel(savedGameTableModel());
        savedGamesSorter = new TableRowSorter<>(
                (DefaultTableModel) savedGamesTable.getModel());
        savedGamesTable.setRowSorter(savedGamesSorter);
        for (SavedGameEntry entry : visibleSavedGames) {
            addRow(savedGamesTable, entry.username(), entry.gameTitle(),
                    entry.rating() == null ? "Not rated" : entry.rating(), entry.playCount());
        }
        applySavedGameFilters();
        restoreSavedGameSort(savedGamesSorter, sortKeys);
        restoreSavedGameSelection(savedGamesTable, visibleSavedGames, selected);

        visibleStats = catalog.summarizeLibraryByUser();
        fill(statsTable, new String[]{"Username", "Games saved", "Total plays", "Average rating"});
        for (UserLibraryStats stats : visibleStats) {
            addRow(statsTable, stats.username(), stats.savedGames(), stats.totalPlayCount(),
                    stats.averageRating() == null ? "Not rated"
                            : String.format("%.2f", stats.averageRating()));
        }
    }

    private void addGame() {
        withDatabaseErrors(() -> {
            List<Genre> genres = catalog.findAllGenres();
            GameInput input = gameDialog("Add a game", null, genres);
            if (input == null) return;
            Genre genre = input.genre();
            games.create(new Game(0, genre.id(), input.title(), input.url(),
                    input.date(), genre.name()));
            reloadAll();
            status.setText("Game added.");
        });
    }

    private void editGame() {
        Game selected = selectedGame();
        if (selected == null) return;
        withDatabaseErrors(() -> {
            GameInput input = gameDialog("Edit game", selected, catalog.findAllGenres());
            if (input == null) return;
            Genre genre = input.genre();
            if (!games.update(new Game(selected.id(), genre.id(), input.title(),
                    input.url(), input.date(), genre.name()))) {
                showMessage("That game could not be found. Refresh and try again.", "Game not found");
                return;
            }
            reloadAll();
            status.setText("Game updated.");
        });
    }

    private void deleteGame() {
        Game selected = selectedGame();
        if (selected == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete \"" + selected.title() + "\"? Its saved library links will also be removed.",
                "Confirm game deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        withDatabaseErrors(() -> {
            games.delete(selected.id());
            reloadAll();
            status.setText("Game deleted.");
        });
    }

    private void addUser() {
        UserInput input = userDialog("Add a user", null);
        if (input == null) return;
        withDatabaseErrors(() -> {
            users.create(new User(0, input.username(), input.email()));
            reloadAll();
            status.setText("User added.");
        });
    }

    private void editUser() {
        User selected = selectedUser();
        if (selected == null) return;
        UserInput input = userDialog("Edit user", selected);
        if (input == null) return;
        withDatabaseErrors(() -> {
            if (!users.update(new User(selected.id(), input.username(), input.email()))) {
                showMessage("That user could not be found. Refresh and try again.", "User not found");
                return;
            }
            reloadAll();
            status.setText("User updated.");
        });
    }

    private void deleteUser() {
        User selected = selectedUser();
        if (selected == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete \"" + selected.username() + "\" and their saved game links?",
                "Confirm user deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        withDatabaseErrors(() -> {
            users.delete(selected.id());
            reloadAll();
            status.setText("User deleted.");
        });
    }

    private void addGameToUser() {
        withDatabaseErrors(() -> {
            List<User> allUsers = catalog.findAllUsers();
            List<Game> allGames = catalog.findAllGames();
            if (allUsers.isEmpty() || allGames.isEmpty()) {
                showMessage("Add at least one user and one game before creating a library entry.",
                        "Library is empty");
                return;
            }
            JComboBox<UserChoice> userChoice = new JComboBox<>(allUsers.stream()
                    .map(UserChoice::new).toArray(UserChoice[]::new));
            JComboBox<GameChoice> gameChoice = new JComboBox<>(allGames.stream()
                    .map(GameChoice::new).toArray(GameChoice[]::new));
            JTextField ratingField = new JTextField(5);
            JTextField playsField = new JTextField("0", 5);
            JPanel fields = formPanel();
            addField(fields, 0, "User", userChoice);
            addField(fields, 1, "Game", gameChoice);
            addField(fields, 2, "Rating (1–5, optional)", ratingField);
            addField(fields, 3, "Play count (0 or more)", playsField);
            if (JOptionPane.showConfirmDialog(this, fields, "Add to library",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) return;

            UserGameInput input = userGameInput(ratingField, playsField);
            User user = ((UserChoice) userChoice.getSelectedItem()).user();
            Game game = ((GameChoice) gameChoice.getSelectedItem()).game();
            try {
                users.addGameToLibrary(
                        new UserGame(user.id(), game.id(), input.rating(), input.playCount()));
            } catch (DuplicateSavedGameException exception) {
                showMessage("\"" + game.title() + "\" is already saved in "
                        + user.username() + "'s library.", "Game already saved");
                return;
            }
            refreshReports();
            status.setText(game.title() + " added to " + user.username() + "'s library.");
        });
    }

    private void editSavedGame() {
        SavedGameEntry selected = selectedSavedGame();
        if (selected == null) return;
        JTextField ratingField = new JTextField(
                selected.rating() == null ? "" : selected.rating().toString(), 5);
        JTextField playsField = new JTextField(Integer.toString(selected.playCount()), 5);
        JPanel fields = formPanel();
        addField(fields, 0, "User", new JLabel(selected.username()));
        addField(fields, 1, "Game", new JLabel(selected.gameTitle()));
        addField(fields, 2, "Rating (1–5, optional)", ratingField);
        addField(fields, 3, "Play count (0 or more)", playsField);

        while (JOptionPane.showConfirmDialog(this, fields, "Edit saved game",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                UserGameInput input = userGameInput(ratingField, playsField);
                if (!users.updateSavedGame(new UserGame(selected.userId(), selected.gameId(),
                        input.rating(), input.playCount()))) {
                    showMessage("That saved game could not be found. Refresh and try again.",
                            "Saved game not found");
                    return;
                }
                refreshReports();
                status.setText(selected.gameTitle() + " updated for "
                        + selected.username() + ".");
                return;
            } catch (IllegalArgumentException exception) {
                showMessage(exception.getMessage(), "Check saved-game details");
            } catch (SQLException exception) {
                showMessage("The database could not complete that action:\n"
                        + friendlyDatabaseMessage(exception), "Database error");
                return;
            }
        }
    }

    private void removeSavedGame() {
        SavedGameEntry selected = selectedSavedGame();
        if (selected == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Remove \"" + selected.gameTitle() + "\" from "
                        + selected.username() + "'s library? The game and user will remain.",
                "Confirm saved-game removal", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        withDatabaseErrors(() -> {
            if (!users.removeGameFromLibrary(selected.userId(), selected.gameId())) {
                showMessage("That saved game could not be found. Refresh and try again.",
                        "Saved game not found");
                return;
            }
            refreshReports();
            status.setText(selected.gameTitle() + " removed from "
                    + selected.username() + "'s library.");
        });
    }

    private UserGameInput userGameInput(JTextField ratingField, JTextField playsField) {
        String ratingText = ratingField.getText().trim();
        Integer rating = null;
        if (!ratingText.isEmpty()) {
            try {
                rating = Integer.valueOf(ratingText);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Rating must be a whole number from 1 to 5.");
            }
        }
        int playCount;
        try {
            playCount = Integer.parseInt(playsField.getText().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Play count must be a whole number of 0 or more.");
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be from 1 to 5.");
        }
        if (playCount < 0) {
            throw new IllegalArgumentException("Play count cannot be negative.");
        }
        return new UserGameInput(rating, playCount);
    }

    private void sortGames() {
        Integer selectedId = TableSelection.selectedId(gameTable);
        visibleGames = GameAlgorithms.sortByTitle(visibleGames);
        fill(gameTable, new String[]{"ID", "Title", "Genre", "Published", "Game link"});
        for (Game game : visibleGames) {
            addRow(gameTable, game.id(), game.title(), game.genreName(),
                    game.releaseDate(), game.url());
        }
        TableSelection.restoreId(gameTable, selectedId);
        status.setText("Games sorted by title using Java insertion sort.");
    }

    private void openGame() {
        Game selected = selectedGame();
        if (selected == null) return;
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(selected.url()));
            } else {
                JOptionPane.showMessageDialog(this,
                        "A browser cannot be opened here. Copy this saved link:\n" + selected.url(),
                        "Game link", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception exception) {
            showMessage("Could not open the link:\n" + exception.getMessage(), "Browser error");
        }
    }

    private GameInput gameDialog(String title, Game existing, List<Genre> genres) {
        if (genres.isEmpty()) {
            showMessage("There are no genres in the database yet.", "Cannot add game");
            return null;
        }
        JTextField titleField = new JTextField(existing == null ? "" : existing.title(), 24);
        JTextField urlField = new JTextField(existing == null ? "" : existing.url(), 24);
        JTextField dateField = new JTextField(existing == null ? "" : existing.releaseDate(), 24);
        JComboBox<Genre> genreField = new JComboBox<>(genres.toArray(Genre[]::new));
        if (existing != null) {
            genres.stream().filter(genre -> genre.id() == existing.genreId())
                    .findFirst().ifPresent(genreField::setSelectedItem);
        }
        JPanel fields = formPanel();
        addField(fields, 0, "Game title", titleField);
        addField(fields, 1, "Game link (http/https)", urlField);
        addField(fields, 2, "Published (YYYY-MM-DD)", dateField);
        addField(fields, 3, "Genre", genreField);

        while (JOptionPane.showConfirmDialog(this, fields, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                String gameTitle = CatalogValidation.requireText(titleField.getText(), "Title");
                String url = CatalogValidation.requireWebUrl(urlField.getText(), "Game link");
                String date = CatalogValidation.requireIsoDate(dateField.getText());
                Genre genre = (Genre) genreField.getSelectedItem();
                return genre == null ? null : new GameInput(gameTitle, url, date, genre);
            } catch (IllegalArgumentException exception) {
                showMessage(exception.getMessage(), "Check game details");
            }
        }
        return null;
    }

    private UserInput userDialog(String title, User existing) {
        JTextField username = new JTextField(existing == null ? "" : existing.username(), 24);
        JTextField email = new JTextField(existing == null ? "" : existing.email(), 24);
        JPanel fields = formPanel();
        addField(fields, 0, "Username", username);
        addField(fields, 1, "Email", email);
        while (JOptionPane.showConfirmDialog(this, fields, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                return new UserInput(CatalogValidation.requireText(username.getText(), "Username"),
                        CatalogValidation.requireEmail(email.getText()));
            } catch (IllegalArgumentException exception) {
                showMessage(exception.getMessage(), "Check user details");
            }
        }
        return null;
    }

    private JPanel formPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return form;
    }

    private void addField(JPanel panel, int row, String name, Component input) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(6, 5, 6, 12);
        panel.add(new JLabel(name), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(input, c);
    }

    private Game selectedGame() {
        int row = gameTable.getSelectedRow();
        if (row < 0 || row >= visibleGames.size()) {
            showMessage("Select a game in the table first.", "No game selected");
            return null;
        }
        return visibleGames.get(gameTable.convertRowIndexToModel(row));
    }

    private User selectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= visibleUsers.size()) {
            showMessage("Select a user in the table first.", "No user selected");
            return null;
        }
        return visibleUsers.get(userTable.convertRowIndexToModel(row));
    }

    private SavedGameEntry selectedSavedGame() {
        int row = savedGamesTable.getSelectedRow();
        SavedGameEntry selected = savedGameAtViewRow(savedGamesTable, visibleSavedGames, row);
        if (selected == null) {
            showMessage("Select a saved game in the table first.", "No saved game selected");
            return null;
        }
        return selected;
    }

    static RowFilter<DefaultTableModel, Integer> savedGameRowFilter(
            String playerQuery, String titleQuery) {
        String playerNeedle = playerQuery.trim().toLowerCase(Locale.ROOT);
        String titleNeedle = titleQuery.trim().toLowerCase(Locale.ROOT);
        return new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String player = entry.getStringValue(0).toLowerCase(Locale.ROOT);
                String title = entry.getStringValue(1).toLowerCase(Locale.ROOT);
                return player.contains(playerNeedle) && title.contains(titleNeedle);
            }
        };
    }

    static SavedGameEntry savedGameAtViewRow(
            JTable table, List<SavedGameEntry> savedGames, int viewRow) {
        if (viewRow < 0 || viewRow >= table.getRowCount()) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < savedGames.size() ? savedGames.get(modelRow) : null;
    }

    static void restoreSavedGameSort(
            TableRowSorter<DefaultTableModel> sorter, List<? extends RowSorter.SortKey> sortKeys) {
        TableSelection.restoreSort(sorter, sortKeys);
    }

    static void restoreSavedGameSelection(
            JTable table, List<SavedGameEntry> entries, SavedGameEntry selected) {
        table.clearSelection();
        if (selected == null) {
            return;
        }
        for (int modelRow = 0; modelRow < entries.size(); modelRow++) {
            SavedGameEntry entry = entries.get(modelRow);
            if (entry.userId() == selected.userId() && entry.gameId() == selected.gameId()) {
                int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                }
                return;
            }
        }
    }

    static DefaultTableModel savedGameTableModel() {
        return new DefaultTableModel(new String[]{"User", "Game", "Rating", "Play count"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 3 ? Integer.class : Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void addSavedGameFilterListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applySavedGameFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applySavedGameFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applySavedGameFilters();
            }
        });
    }

    private void applySavedGameFilters() {
        if (savedGamesSorter == null) {
            return;
        }
        savedGamesSorter.setRowFilter(savedGameRowFilter(
                savedGamePlayerFilter.getText(), savedGameTitleFilter.getText()));
        savedGameFilterCount.setText(savedGamesTable.getRowCount() + " of "
                + visibleSavedGames.size() + " saved games");
        savedGameNoMatches.setVisible(hasNoSavedGameMatches(
                savedGamePlayerFilter.getText(), savedGameTitleFilter.getText(),
                savedGamesTable.getRowCount(), visibleSavedGames.size()));
    }

    static boolean hasNoSavedGameMatches(
            String playerQuery, String titleQuery, int matchingCount, int totalCount) {
        boolean hasFilter = !playerQuery.trim().isEmpty() || !titleQuery.trim().isEmpty();
        return hasFilter && totalCount > 0 && matchingCount == 0;
    }

    private Integer selectedGenreId() {
        GenreChoice choice = (GenreChoice) genreFilter.getSelectedItem();
        return choice == null ? null : choice.id();
    }

    private void withDatabaseErrors(SqlAction action) {
        try {
            action.run();
        } catch (SQLException exception) {
            showMessage("The database could not complete that action:\n"
                    + friendlyDatabaseMessage(exception), "Database error");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage() == null
                    ? "Enter a valid value in each field." : exception.getMessage(),
                    "Check your input");
        }
    }

    private String friendlyDatabaseMessage(SQLException exception) {
        String message = exception.getMessage();
        if (message != null && message.toLowerCase().contains("unique")) {
            return "That value is already in use. Usernames/emails and game title/date pairs must be unique.";
        }
        if (message != null && message.toLowerCase().contains("foreign key")) {
            return "A related record was changed. Refresh the data and try again.";
        }
        return message == null ? "Unknown SQLite error." : message;
    }

    private void closeConnection() {
        try {
            connection.close();
        } catch (SQLException exception) {
            showMessage("The database connection could not close cleanly:\n" + exception.getMessage(),
                    "Close warning");
        }
    }

    private void showMessage(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel panel() {
        JPanel result = new JPanel(new BorderLayout(8, 12));
        result.setBorder(BorderFactory.createEmptyBorder(14, 12, 6, 12));
        result.setBackground(Color.WHITE);
        return result;
    }

    private JLabel label(String text) {
        JLabel result = new JLabel(text);
        result.setForeground(INK);
        return result;
    }

    private JButton button(String text, boolean primary) {
        JButton result = new JButton(text);
        result.setFocusPainted(false);
        result.setMargin(new Insets(8, 14, 8, 14));
        if (primary) {
            result.setBackground(ACCENT);
            result.setForeground(Color.WHITE);
        }
        return result;
    }

    private static JTable table() {
        JTable result = new JTable(new DefaultTableModel());
        result.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        result.setRowHeight(29);
        result.setFillsViewportHeight(true);
        result.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        result.setAutoCreateRowSorter(true);
        return result;
    }

    private static void fill(JTable table, String[] columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);
        table.setAutoCreateRowSorter(true);
    }

    private static void addRow(JTable table, Object... values) {
        ((DefaultTableModel) table.getModel()).addRow(values);
    }

    private record GenreChoice(Integer id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record UserChoice(User user) {
        @Override
        public String toString() {
            return user.username();
        }
    }

    private record GameChoice(Game game) {
        @Override
        public String toString() {
            return game.title() + " (" + game.genreName() + ")";
        }
    }

    private record GameInput(String title, String url, String date, Genre genre) {
    }

    private record UserInput(String username, String email) {
    }

    private record UserGameInput(Integer rating, int playCount) {
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
