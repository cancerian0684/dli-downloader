package org.shunya.dli;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class AppConfigPanel extends JPanel {

    private final JTable configTable;

    public AppConfigPanel(AppContext appContext) {
        setLayout(new GridLayout(1, 0));

        final AppConfigTableModel configTableModel = new AppConfigTableModel(appContext);
        configTable = new JTable(configTableModel);
        configTable.setShowGrid(true);
        configTable.setShowVerticalLines(false);
        configTable.setFillsViewportHeight(true);
        configTable.setAutoCreateRowSorter(true);
        configTable.setRowHeight(35);
        configTable.setRowMargin(1);
        configTable.setShowGrid(false);
        configTable.setIntercellSpacing(new Dimension(0, 0));
        configTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        configTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        Utils.initializeTableColumns(configTable, AppConfigTableModel.size);

        TableCellRenderer dcr = configTable.getDefaultRenderer(String.class);
        if (dcr instanceof JLabel) {
            ((JLabel) dcr).setVerticalAlignment(SwingConstants.TOP);
            ((JLabel) dcr).setBorder(new EmptyBorder(1, 1, 1, 1));
        }

        JScrollPane editorScrollPane = new JScrollPane(configTable);
        editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(editorScrollPane);
    }
}
