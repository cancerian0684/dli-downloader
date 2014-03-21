package org.shunya.dli;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class SearchTableModel extends AbstractTableModel {
    public final int[] width = {25, 440};
    private final CachedDownloads cachedDownloads;
    private ArrayList<Publication> list = new ArrayList<>();
    private String[] columnNames = new String[]{"<html><b>#", "<html><b>e-Book Details"};
    private Class[] columnClasses = new Class[]{Integer.class, String.class};

    public SearchTableModel(CachedDownloads cachedDownloads) {
        this.cachedDownloads = cachedDownloads;
    }


    public void addPublication(Publication publication) {
        insertRow(publication);
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return list.size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
        Publication publication = list.get(row);
        switch (col) {
            case 0:
                return row + 1;
            case 1:
                if (publication.getLocalPath() != null && !publication.getLocalPath().isEmpty())
                    return "<html><font color=\"075D0B\">" + publication.getSearchText() + "<sup>local</sup></font></html>";
                else
                    return "<html>" + publication.getSearchText() + "</html>";
        }
        return "";
    }

    public Class<?> getColumnClass(int col) {
        return columnClasses[col];
    }

    public synchronized Publication insertRowAtBeginning(Publication task) {
        list.add(0, task);
        super.fireTableRowsInserted(0, 0);
        return list.get(0);
    }

    public synchronized Publication insertRow(Publication task) {
        list.add(task);
        super.fireTableRowsInserted(list.size() - 1, list.size() - 1);
        return list.get(list.size() - 1);
    }

    public synchronized void deleteRow(int row) {
        list.remove(row);
        super.fireTableDataChanged();
    }

    public synchronized void deleteRow(Publication task) {
        deleteRow(list.indexOf(task));
    }


    public synchronized void deleteRows(ArrayList<Object> rows) {
        list.removeAll(rows);
        super.fireTableDataChanged();
    }

    public void deleteAfterSelectedRow(int row) {
        int size = this.getRowCount();
        int n = size - (row + 1);
        for (int i = 1; i <= n; i++) {
            list.remove(row + 1);
        }
        super.fireTableDataChanged();
    }

    public Publication getRow(int row) {
        return list.get(row);
    }

    public void updateRow(Publication updatedRow, int row) {
        list.set(row, updatedRow);
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
        list.clear();
        super.fireTableDataChanged();
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public void update(Publication task) {
        final int index = list.indexOf(task);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireTableRowsUpdated(index, index);
            }
        });
    }
}
