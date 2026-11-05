package com.example.dbstarter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.junit.Test;

public class SavedGameFilterTest {
    @Test
    public void refreshRestoresTheSameRelationshipDespiteChangedValuesAndOrdering() {
        List<SavedGameEntry> original = savedGames();
        JTable oldTable = new JTable(tableModel(original));
        oldTable.setAutoCreateRowSorter(true);
        oldTable.getRowSorter().toggleSortOrder(1);
        int selectedViewRow = oldTable.convertRowIndexToView(2);
        oldTable.setRowSelectionInterval(selectedViewRow, selectedViewRow);
        SavedGameEntry captured = GameLibraryFrame.savedGameAtViewRow(
                oldTable, original, oldTable.getSelectedRow());
        SavedGameEntry updated = new SavedGameEntry(20, 201, "Ava renamed", "Portal remaster", 5, 99);
        List<SavedGameEntry> refreshed = List.of(
                new SavedGameEntry(30, 201, "Ava renamed", "Portal remaster", 5, 99),
                updated,
                new SavedGameEntry(20, 300, "Ava renamed", "Portal remaster", 5, 99),
                original.get(0));
        for (SortOrder direction : List.of(SortOrder.ASCENDING, SortOrder.DESCENDING)) {
            JTable table = new JTable(tableModel(refreshed));
            TableRowSorter<DefaultTableModel> sorter =
                    new TableRowSorter<>((DefaultTableModel) table.getModel());
            table.setRowSorter(sorter);
            sorter.setRowFilter(GameLibraryFrame.savedGameRowFilter("ava", "portal"));
            sorter.setSortKeys(List.of(new RowSorter.SortKey(1, direction)));

            GameLibraryFrame.restoreSavedGameSelection(table, refreshed, captured);

            assertEquals(updated, GameLibraryFrame.savedGameAtViewRow(
                    table, refreshed, table.getSelectedRow()));
            assertEquals(table.convertRowIndexToView(1), table.getSelectedRow());
        }
    }

    @Test
    public void deletedFilteredOrUnselectedRelationshipsLeaveNoSelection() {
        List<SavedGameEntry> entries = savedGames();
        JTable table = new JTable(tableModel(entries));
        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);
        table.setRowSelectionInterval(0, 0);
        GameLibraryFrame.restoreSavedGameSelection(table, entries,
                new SavedGameEntry(20, 100, "Ava", "Tetris", null, 0));
        assertEquals(-1, table.getSelectedRow());

        sorter.setRowFilter(GameLibraryFrame.savedGameRowFilter("zara", ""));
        table.setRowSelectionInterval(0, 0);
        GameLibraryFrame.restoreSavedGameSelection(table, entries, entries.get(2));
        assertEquals(-1, table.getSelectedRow());
        assertNull(GameLibraryFrame.savedGameAtViewRow(table, entries, table.getSelectedRow()));

        sorter.setRowFilter(null);
        table.setRowSelectionInterval(0, 0);
        GameLibraryFrame.restoreSavedGameSelection(table, entries, null);
        assertEquals(-1, table.getSelectedRow());

        JTable empty = new JTable(tableModel(List.of()));
        GameLibraryFrame.restoreSavedGameSelection(empty, List.of(), entries.get(0));
        assertEquals(-1, empty.getSelectedRow());
    }

    @Test
    public void playCountsSortNumericallyAndKeepFilteringAcrossRefresh() {
        for (SortOrder direction : List.of(SortOrder.ASCENDING, SortOrder.DESCENDING)) {
            List<RowSorter.SortKey> keys = List.of(new RowSorter.SortKey(3, direction));
            TableRowSorter<DefaultTableModel> previousSorter = null;
            for (int refresh = 0; refresh < 2; refresh++) {
                DefaultTableModel model = GameLibraryFrame.savedGameTableModel();
                model.addRow(new Object[]{"Ava", "Portal", "Not rated", 10});
                model.addRow(new Object[]{"Ava", "Portal 2", 5, 0});
                model.addRow(new Object[]{"Ava", "Portal 3", 4, 2});
                model.addRow(new Object[]{"Ben", "Tetris", "Not rated", 1});
                JTable table = new JTable(model);
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                table.setRowSorter(sorter);
                sorter.setRowFilter(GameLibraryFrame.savedGameRowFilter("ava", "portal"));
                GameLibraryFrame.restoreSavedGameSort(sorter, previousSorter == null
                        ? keys : List.copyOf(previousSorter.getSortKeys()));

                List<Integer> expected = direction == SortOrder.ASCENDING
                        ? List.of(0, 2, 10) : List.of(10, 2, 0);
                assertEquals(3, table.getRowCount());
                for (int row = 0; row < expected.size(); row++) {
                    assertEquals(expected.get(row), table.getValueAt(row, 3));
                }
                assertEquals("Not rated", model.getValueAt(0, 2));
                assertEquals(5, model.getValueAt(1, 2));
                sorter.setSortKeys(List.of(new RowSorter.SortKey(2, direction)));
                assertEquals(3, table.getRowCount());
                sorter.setSortKeys(keys);
                previousSorter = sorter;
            }
        }
    }

    @Test
    public void refreshingPreservesSortDirectionAndFilteredSelection() {
        List<SavedGameEntry> original = savedGames();
        List<SavedGameEntry> refreshed = List.of(
                original.get(3), original.get(2), original.get(0), original.get(1));
        for (SortOrder direction : List.of(SortOrder.ASCENDING, SortOrder.DESCENDING)) {
            TableRowSorter<DefaultTableModel> oldSorter = new TableRowSorter<>(tableModel(original));
            oldSorter.setSortKeys(List.of(new RowSorter.SortKey(1, direction)));
            List<? extends RowSorter.SortKey> capturedKeys = List.copyOf(oldSorter.getSortKeys());

            JTable table = new JTable(tableModel(refreshed));
            TableRowSorter<DefaultTableModel> newSorter =
                    new TableRowSorter<>((DefaultTableModel) table.getModel());
            table.setRowSorter(newSorter);
            newSorter.setRowFilter(GameLibraryFrame.savedGameRowFilter("ava", "portal"));
            GameLibraryFrame.restoreSavedGameSort(newSorter, capturedKeys);

            assertEquals(capturedKeys, newSorter.getSortKeys());
            assertEquals(2, table.getRowCount());
            SavedGameEntry expected = direction == SortOrder.ASCENDING
                    ? original.get(2) : original.get(1);
            assertEquals(expected, GameLibraryFrame.savedGameAtViewRow(table, refreshed, 0));

            newSorter.setRowFilter(GameLibraryFrame.savedGameRowFilter("", ""));
            assertEquals(4, table.getRowCount());
            assertEquals(capturedKeys, newSorter.getSortKeys());
        }
    }

    @Test
    public void missingSortColumnFallsBackToNormalOrdering() {
        List<SavedGameEntry> entries = savedGames();
        JTable table = new JTable(tableModel(entries));
        TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING)));

        GameLibraryFrame.restoreSavedGameSort(sorter, List.of(
                new RowSorter.SortKey(1, SortOrder.ASCENDING),
                new RowSorter.SortKey(3, SortOrder.DESCENDING)));

        assertTrue(sorter.getSortKeys().isEmpty());
        assertEquals(entries.get(0), GameLibraryFrame.savedGameAtViewRow(table, entries, 0));
    }

    @Test
    public void refreshingAnEmptyTableKeepsValidSortKeysAndAcceptsUnsortedState() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel(List.of()));
        List<RowSorter.SortKey> keys = List.of(
                new RowSorter.SortKey(0, SortOrder.ASCENDING),
                new RowSorter.SortKey(1, SortOrder.DESCENDING));
        GameLibraryFrame.restoreSavedGameSort(sorter, keys);
        assertEquals(keys, sorter.getSortKeys());
        GameLibraryFrame.restoreSavedGameSort(sorter, List.of());
        assertTrue(sorter.getSortKeys().isEmpty());
    }

    @Test
    public void noMatchMessageOnlyAppearsWhenFiltersExcludeSavedGames() {
        assertTrue(GameLibraryFrame.hasNoSavedGameMatches("unknown", "", 0, 4));
        assertTrue(GameLibraryFrame.hasNoSavedGameMatches("", "unknown", 0, 4));
        assertTrue(GameLibraryFrame.hasNoSavedGameMatches("Ava", "Tetris", 0, 4));
        assertFalse(GameLibraryFrame.hasNoSavedGameMatches("Ava", "", 2, 4));
        assertFalse(GameLibraryFrame.hasNoSavedGameMatches("", "", 4, 4));
        assertFalse(GameLibraryFrame.hasNoSavedGameMatches(" ", "\t", 0, 4));
        assertFalse(GameLibraryFrame.hasNoSavedGameMatches("unknown", "", 0, 0));
        assertFalse(GameLibraryFrame.hasNoSavedGameMatches("", "", 0, 0));
    }

    @Test
    public void filtersByPlayerAndTitleAndClearingShowsAllRows() {
        List<SavedGameEntry> savedGames = savedGames();
        DefaultTableModel model = tableModel(savedGames);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

        sorter.setRowFilter(GameLibraryFrame.savedGameRowFilter(" ava ", ""));
        assertEquals(2, sorter.getViewRowCount());

        sorter.setRowFilter(GameLibraryFrame.savedGameRowFilter("", "PORTAL"));
        assertEquals(3, sorter.getViewRowCount());

        sorter.setRowFilter(GameLibraryFrame.savedGameRowFilter("ava", "portal"));
        assertEquals(2, sorter.getViewRowCount());

        sorter.setRowFilter(GameLibraryFrame.savedGameRowFilter("", ""));
        assertEquals(savedGames.size(), sorter.getViewRowCount());
    }

    @Test
    public void sortedFilteredViewStillResolvesTheSelectedRelationship() {
        List<SavedGameEntry> savedGames = savedGames();
        DefaultTableModel model = tableModel(savedGames);
        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.setRowFilter(GameLibraryFrame.savedGameRowFilter("ava", "portal"));
        sorter.toggleSortOrder(1);

        assertEquals(2, table.getRowCount());
        assertEquals(savedGames.get(2),
                GameLibraryFrame.savedGameAtViewRow(table, savedGames, 0));
        assertNull(GameLibraryFrame.savedGameAtViewRow(table, savedGames, -1));
    }

    private List<SavedGameEntry> savedGames() {
        return List.of(
                new SavedGameEntry(10, 100, "Zara", "Tetris", null, 2),
                new SavedGameEntry(20, 200, "Ava", "Portal 2", 5, 8),
                new SavedGameEntry(20, 201, "Ava", "Portal", 4, 3),
                new SavedGameEntry(30, 202, "Ben", "Portal", null, 0));
    }

    private DefaultTableModel tableModel(List<SavedGameEntry> savedGames) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"User", "Game"}, 0);
        for (SavedGameEntry entry : savedGames) {
            model.addRow(new Object[]{entry.username(), entry.gameTitle()});
        }
        return model;
    }
}
