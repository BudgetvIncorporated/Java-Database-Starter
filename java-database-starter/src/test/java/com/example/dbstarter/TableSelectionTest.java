package com.example.dbstarter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Title", "Genre", "Published", "Game link"}, 0);
        for (Game game : entries) {
            model.addRow(new Object[]{game.id(), game.title(), game.genreName(),
                    game.releaseDate(), game.url()});
        }
        return model;
    }

    private DefaultTableModel userModel(List<User> entries) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Username", "Email"}, 0);
        for (User user : entries) {
            model.addRow(new Object[]{user.id(), user.username(), user.email()});
        }
        return model;
    }
}
