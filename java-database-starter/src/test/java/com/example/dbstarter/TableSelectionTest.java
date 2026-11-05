package com.example.dbstarter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.junit.Test;

public class TableSelectionTest {
    @Test
    public void catalogIdsSortNumericallyAndRetainSelectionAfterRefresh() {
        assertNumericIdSortAndRefresh(
                gameModel(List.of(game(10, "Alpha"), game(1, "Zulu"), game(2, "Bravo"))),
                gameModel(List.of(game(2, "Renamed"), game(10, "Alpha"), game(1, "Zulu"))));
    }

    @Test
    public void userIdsSortNumericallyAndRetainSelectionAfterRefresh() {
        assertNumericIdSortAndRefresh(
                userModel(List.of(new User(10, "Alpha", "a@example.com"),
                        new User(1, "Zulu", "z@example.com"),
                        new User(2, "Bravo", "b@example.com"))),
                userModel(List.of(new User(2, "Renamed", "new@example.com"),
                        new User(10, "Alpha", "a@example.com"),
                        new User(1, "Zulu", "z@example.com"))));
    }

    private void assertNumericIdSortAndRefresh(
            DefaultTableModel original, DefaultTableModel refreshed) {
        assertEquals(Integer.class, original.getColumnClass(0));
        for (int column = 1; column < original.getColumnCount(); column++) {
            assertEquals(Object.class, original.getColumnClass(column));
        }
        for (SortOrder direction : List.of(SortOrder.ASCENDING, SortOrder.DESCENDING)) {
            JTable table = sortedTable(original, direction);
            List<? extends RowSorter.SortKey> keys = List.of(new RowSorter.SortKey(0, direction));
            table.getRowSorter().setSortKeys(keys);
            assertIdOrder(table, direction);
            TableSelection.restoreId(table, 2);
            for (DefaultTableModel model : List.of(refreshed, original)) {
                Integer selected = TableSelection.selectedId(table);
                List<? extends RowSorter.SortKey> captured = TableSelection.sortKeys(table);
                table.setModel(model);
                TableSelection.restoreSort(table.getRowSorter(), captured);
                TableSelection.restoreId(table, selected);
                assertEquals(keys, table.getRowSorter().getSortKeys());
                assertIdOrder(table, direction);
                assertEquals(Integer.valueOf(2), TableSelection.selectedId(table));
            }
        }
    }

    private void assertIdOrder(JTable table, SortOrder direction) {
        assertEquals(direction == SortOrder.ASCENDING ? List.of(1, 2, 10) : List.of(10, 2, 1),
                List.of(table.getValueAt(0, 0), table.getValueAt(1, 0), table.getValueAt(2, 0)));
    }

    @Test
    public void catalogSortAndSelectionSurviveFilteredRefreshInBothDirections() {
        List<Game> original = List.of(game(1, "Zelda"), game(2, "Portal"), game(3, "Portal 2"));
        List<Game> filtered = List.of(game(3, "Portal 2"), game(2, "Portal"));
        for (SortOrder direction : List.of(SortOrder.ASCENDING, SortOrder.DESCENDING)) {
            JTable table = sortedTable(gameModel(original), direction);
            int selectedView = table.convertRowIndexToView(1);
            table.setRowSelectionInterval(selectedView, selectedView);
            Integer selected = TableSelection.selectedId(table);
            List<? extends RowSorter.SortKey> keys = TableSelection.sortKeys(table);

            table.setModel(gameModel(filtered));
            TableSelection.restoreSort(table.getRowSorter(), keys);
            TableSelection.restoreId(table, selected);

            assertEquals(keys, table.getRowSorter().getSortKeys());
            assertEquals(direction == SortOrder.ASCENDING ? "Portal" : "Portal 2",
                    table.getValueAt(0, 1));
            assertEquals(Integer.valueOf(2), TableSelection.selectedId(table));

            table.setModel(gameModel(List.of(game(1, "Zelda"))));
            TableSelection.restoreSort(table.getRowSorter(), keys);
            TableSelection.restoreId(table, selected);
            assertEquals(keys, table.getRowSorter().getSortKeys());
            assertNull(TableSelection.selectedId(table));
        }
    }

    @Test
    public void userSortAndSelectionSurviveRepeatedRefreshes() {
        List<User> original = List.of(new User(10, "Zara", "zara@example.com"),
                new User(20, "Ben", "ben@example.com"));
        for (SortOrder direction : List.of(SortOrder.ASCENDING, SortOrder.DESCENDING)) {
            JTable table = sortedTable(userModel(original), direction);
            int selectedView = table.convertRowIndexToView(0);
            table.setRowSelectionInterval(selectedView, selectedView);
            for (int refresh = 0; refresh < 2; refresh++) {
                Integer selected = TableSelection.selectedId(table);
                List<? extends RowSorter.SortKey> keys = TableSelection.sortKeys(table);
                table.setModel(userModel(List.of(original.get(1), original.get(0))));
                TableSelection.restoreSort(table.getRowSorter(), keys);
                TableSelection.restoreId(table, selected);
                assertEquals(keys, table.getRowSorter().getSortKeys());
                assertEquals(direction == SortOrder.ASCENDING ? "Ben" : "Zara",
                        table.getValueAt(0, 1));
                assertEquals(Integer.valueOf(10), TableSelection.selectedId(table));
            }
        }
    }

    @Test
    public void missingSortColumnFallsBackToNormalOrderAndEmptyStateIsSafe() {
        JTable table = sortedTable(gameModel(List.of(game(1, "Zelda"), game(2, "Portal"))),
                SortOrder.ASCENDING);
        table.getRowSorter().setSortKeys(List.of(
                new RowSorter.SortKey(4, SortOrder.DESCENDING)));
        List<? extends RowSorter.SortKey> keys = TableSelection.sortKeys(table);
        table.setModel(userModel(List.of(new User(10, "Zara", "zara@example.com"),
                new User(20, "Ben", "ben@example.com"))));
        TableSelection.restoreSort(table.getRowSorter(), keys);
        assertTrue(table.getRowSorter().getSortKeys().isEmpty());
        assertEquals("Zara", table.getValueAt(0, 1));
        TableSelection.restoreSort(table.getRowSorter(), List.of());
        assertTrue(table.getRowSorter().getSortKeys().isEmpty());
        JTable empty = new JTable();
        assertTrue(TableSelection.sortKeys(empty).isEmpty());
        TableSelection.restoreSort(empty.getRowSorter(), keys);
    }

    @Test
    public void gameSelectionSurvivesRenamingReorderingAndBothSortDirections() {
        List<Game> original = List.of(game(1, "Zelda"), game(2, "Portal"));
        List<Game> refreshed = List.of(game(3, "A new title"), game(2, "Portal"),
                game(1, "A new title"));
        for (SortOrder direction : List.of(SortOrder.ASCENDING, SortOrder.DESCENDING)) {
            JTable table = sortedTable(gameModel(original), direction);
            int selectedView = table.convertRowIndexToView(0);
            table.setRowSelectionInterval(selectedView, selectedView);
            Integer captured = TableSelection.selectedId(table);
            assertEquals(Integer.valueOf(1), captured);

            table.setModel(gameModel(refreshed));
            table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, direction)));
            TableSelection.restoreId(table, captured);

            assertEquals(captured, TableSelection.selectedId(table));
            assertEquals(refreshed.get(2),
                    refreshed.get(table.convertRowIndexToModel(table.getSelectedRow())));
        }
    }

    @Test
    public void userSelectionSurvivesUsernameEmailAndRowChanges() {
        List<User> original = List.of(new User(10, "Zara", "old@example.com"),
                new User(20, "Ben", "ben@example.com"));
        List<User> refreshed = List.of(new User(20, "Ben", "ben@example.com"),
                new User(30, "Ava", "other@example.com"),
                new User(10, "Ava", "new@example.com"));
        JTable table = sortedTable(userModel(original), SortOrder.ASCENDING);
        int selectedView = table.convertRowIndexToView(0);
        table.setRowSelectionInterval(selectedView, selectedView);
        Integer captured = TableSelection.selectedId(table);

        table.setModel(userModel(refreshed));
        table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.DESCENDING)));
        TableSelection.restoreId(table, captured);

        assertEquals(Integer.valueOf(10), TableSelection.selectedId(table));
        assertEquals(refreshed.get(2),
                refreshed.get(table.convertRowIndexToModel(table.getSelectedRow())));
    }

    @Test
    public void deletedOrFilteredItemsClearSelectionRatherThanSelectingANeighbor() {
        JTable table = sortedTable(userModel(List.of(
                new User(20, "Ben", "ben@example.com"),
                new User(30, "Ava", "ava@example.com"))), SortOrder.ASCENDING);
        table.setRowSelectionInterval(0, 0);
        TableSelection.restoreId(table, 10);
        assertNull(TableSelection.selectedId(table));

        TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
        sorter.setRowFilter(RowFilter.regexFilter("Ben", 1));
        table.setRowSelectionInterval(0, 0);
        TableSelection.restoreId(table, 30);
        assertNull(TableSelection.selectedId(table));

        table.setModel(gameModel(List.of(game(2, "Portal"))));
        table.setRowSelectionInterval(0, 0);
        TableSelection.restoreId(table, 1);
        assertNull(TableSelection.selectedId(table));
    }

    @Test
    public void emptyAndInitiallyUnselectedTablesStayUnselected() {
        JTable table = new JTable(new DefaultTableModel());
        assertNull(TableSelection.selectedId(table));
        TableSelection.restoreId(table, null);
        TableSelection.restoreId(table, 1);
        assertNull(TableSelection.selectedId(table));

        table.setModel(gameModel(List.of(game(1, "Portal"))));
        table.setRowSelectionInterval(0, 0);
        TableSelection.restoreId(table, null);
        assertNull(TableSelection.selectedId(table));
    }

    private JTable sortedTable(DefaultTableModel model, SortOrder direction) {
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, direction)));
        return table;
    }

    private Game game(int id, String title) {
        return new Game(id, 1, title, "https://example.com/game", "2020-01-01", "Puzzle");
    }

    private DefaultTableModel gameModel(List<Game> entries) {
        DefaultTableModel model = GameLibraryFrame.tableModel(
                new String[]{"ID", "Title", "Genre", "Published", "Game link"});
        for (Game game : entries) {
            model.addRow(new Object[]{game.id(), game.title(), game.genreName(),
                    game.releaseDate(), game.url()});
        }
        return model;
    }

    private DefaultTableModel userModel(List<User> entries) {
        DefaultTableModel model = GameLibraryFrame.tableModel(
                new String[]{"ID", "Username", "Email"});
        for (User user : entries) {
            model.addRow(new Object[]{user.id(), user.username(), user.email()});
        }
        return model;
    }
}
