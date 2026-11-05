package com.example.dbstarter;

import javax.swing.JTable;
import javax.swing.table.TableModel;

/** Selection by stable integer ID for tables whose first model column contains the ID. */
final class TableSelection {
    private TableSelection() {
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
