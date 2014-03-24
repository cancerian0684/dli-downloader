package org.shunya.dli;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DownloadTableModel extends AbstractTableModel {
    public final int[] width = {25, 310, 80, 70};
    private CopyOnWriteArrayList<InteractiveTask> downloadList = new CopyOnWriteArrayList<>();
    private String[] columnNames = new String[]{"<html><b>#","<html><b>e-Book Details", "<html><b>Progress", "<html><b>Status"};
    private Class[] columnClasses = new Class[]{Integer.class, String.class, JProgressBar.class, RunState.class};

    public List<InteractiveTask> getModelData() {
        return downloadList;
    }

    public boolean addDownload(InteractiveTask task) {
        if (!downloadList.contains(task)) {
            insertRow(task);
            return true;
        }
        return false;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return downloadList.size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
        InteractiveTask task = downloadList.get(row);
        switch (col) {
            case 0:
                return row+1;
            case 1:
                return "<html>"+task.getSummary();
            case 2:
                return task.getProgress();
            case 3:
                return task.getState();
        }
        return "";
    }

    public Class<?> getColumnClass(int col) {
        return columnClasses[col];
    }

    public synchronized InteractiveTask insertRowAtBeginning(InteractiveTask task) {
        downloadList.add(0, task);
        super.fireTableRowsInserted(0, 0);
        return downloadList.get(0);
    }

    public synchronized InteractiveTask insertRow(InteractiveTask task) {
        downloadList.add(task);
        super.fireTableRowsInserted(downloadList.size() - 1, downloadList.size() - 1);
        return downloadList.get(downloadList.size() - 1);
    }

    public synchronized void deleteRow(int row) {
        downloadList.remove(row);
        super.fireTableDataChanged();
    }

    public synchronized void deleteRow(InteractiveTask task){
        deleteRow(downloadList.indexOf(task));
    }


    public synchronized void deleteRows(ArrayList<Object> rows) {
        downloadList.removeAll(rows);
        super.fireTableDataChanged();
    }

    public void deleteAfterSelectedRow(int row) {
        int size = this.getRowCount();
        int n = size - (row + 1);
        for (int i = 1; i <= n; i++) {
            downloadList.remove(row + 1);
        }
        super.fireTableDataChanged();
    }

    public InteractiveTask getRow(int row) {
        return downloadList.get(row);
    }

    public void updateRow(InteractiveTask updatedRow, int row) {
        downloadList.set(row, updatedRow);
        super.fireTableDataChanged();
    }

    public synchronized void refreshTable() {
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
                super.fireTableCellUpdated(i, j);
            }
        }
    }

    public void clearTable() {
        downloadList.clear();
        super.fireTableDataChanged();
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public void update(final InteractiveTask task) {
        SwingUtilities.invokeLater(() -> {
            final int index = downloadList.indexOf(task);
            fireTableRowsUpdated(index, index);
        });
    }
}
