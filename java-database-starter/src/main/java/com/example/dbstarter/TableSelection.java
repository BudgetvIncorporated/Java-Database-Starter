package com.example.dbstarter;

import java.util.List;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.TableModel;

/** Selection by stable integer ID for tables whose first model column contains the ID. */
final class TableSelection {
    private TableSelection() {
    }

    static List<? extends RowSorter.SortKey> sortKeys(JTable table) {
        return table.getRowSorter() == null
                ? List.of() : List.copyOf(table.getRowSorter().getSortKeys());
    }

    static void restoreSort(
            RowSorter<? extends TableModel> sorter, List<? extends RowSorter.SortKey> keys) {
        if (sorter == null) {
            return;
        }
        int columnCount = sorter.getModel().getColumnCount();
        boolean valid = keys.stream()
                .allMatch(key -> key.getColumn() >= 0 && key.getColumn() < columnCount);
        sorter.setSortKeys(valid ? keys : List.of());
    }

    static Integer selectedId(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0 || viewRow >= table.getRowCount()) {
            return null;
        }
        Object value = table.getModel().getValueAt(table.convertRowIndexToModel(viewRow), 0);
        return value instanceof Integer id ? id : null;
    }

    static void restoreId(JTable table, Integer selectedId) {
        table.clearSelection();
        if (selectedId == null) {
            return;
        }
        TableModel model = table.getModel();
        for (int modelRow = 0; modelRow < model.getRowCount(); modelRow++) {
            if (selectedId.equals(model.getValueAt(modelRow, 0))) {
                int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                }
                return;
            }
        }
    }
}
