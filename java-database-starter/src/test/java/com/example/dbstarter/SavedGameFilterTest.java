package com.example.dbstarter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.junit.Test;

public class SavedGameFilterTest {
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
