package org.shunya.dli;

import org.apache.lucene.queryparser.classic.ParseException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.shunya.dli.Utils.clean;
import static org.shunya.dli.Utils.removeDuplicateWords;

public class SearchPanel extends JPanel {
    private JTable jTable;
    private final SearchTableModel tableModel;
    private JTextField jTextField;
    private final LuceneSearcher searcher;
    private final AppContext appContext;
    private final Main window;
    private DelayedQueueHandlerThread<String> delayedQueueHandlerThread;

    public SearchPanel(CachedDownloads cachedDownloads, LuceneSearcher searcher, final DownloadPanel downloadPanel, AppContext appContext, final Main window) {
        this.searcher = searcher;
        this.appContext = appContext;
        this.window = window;
        tableModel = new SearchTableModel(cachedDownloads);
        setLayout(new GridBagLayout());
        initializeTextField();
        initializeTable();

        delayedQueueHandlerThread = new DelayedQueueHandlerThread<>(new DelayedQueueHandlerThread.CallBackHandler<String>() {
            @Override
            public void process(final String query) {
                SwingUtilities.invokeLater(() -> {
                    populateSearchResults(query);
                });
            }
        }, appContext);
        delayedQueueHandlerThread.start();
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.025;
        c.gridx = 0;
        c.gridy = 0;
        add(jTextField, c);

        c.fill = GridBagConstraints.BOTH;
        c.ipady = 0;      //make this component tall
        c.weightx = 1.0;
        c.weighty = 0.975;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        add(new JScrollPane(jTable), c);

        final JPopupMenu popupTask = new JPopupMenu();
        final JMenuItem downloadMenu;
        final JMenuItem openPDFMenu;
        final JMenuItem findSimilarMenu;
        openPDFMenu = new JMenuItem("Open PDF");
        openPDFMenu.addActionListener(e -> {
            if (jTable.getSelectedRow() != -1) {
                Publication td = tableModel.getRow(jTable.convertRowIndexToModel(jTable.getSelectedRow()));
                final Path path = Paths.get(td.getLocalPath());
                if (Files.exists(path)) {
                    try {
                        Desktop.getDesktop().open(path.toFile());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(window.getFrame(), "PDF file does not exists : " + td.getBarcode()+", \n"+path.toFile().getAbsolutePath(), "File Not Found in Local Directory", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        popupTask.add(openPDFMenu);

        downloadMenu = new JMenuItem("Download");
        downloadMenu.addActionListener(e -> {
            if (jTable.getSelectedRow() != -1) {
                int[] userSelectedRows = jTable.getSelectedRows();
                for (int row : userSelectedRows) {
                    int rowModelIndex = jTable.convertRowIndexToModel(row);
                    Publication td = tableModel.getRow(rowModelIndex);
                    downloadPanel.addTask(td.getBarcode());
                }
            }
        });
        popupTask.add(downloadMenu);

        findSimilarMenu = new JMenuItem("Find Similar");
        findSimilarMenu.addActionListener(e -> {
            if (jTable.getSelectedRow() != -1) {
                int[] userSelectedRows = jTable.getSelectedRows();
                StringBuilder query = new StringBuilder();
                for (int row : userSelectedRows) {
                    int rowModelIndex = jTable.convertRowIndexToModel(row);
                    Publication td = tableModel.getRow(rowModelIndex);
                    query.append(td.getAuthor() + " " + td.getTitle() + " " + td.getSubject()+" ");
                }
                jTextField.setText(removeDuplicateWords(clean(query.toString())).trim());
//                    updateSearchResult(query);
            }
        });
        popupTask.add(findSimilarMenu);
        jTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = jTable.rowAtPoint(e.getPoint());
                if (selRow != -1) {
                    jTable.requestFocus();
//                    jTable.setRowSelectionInterval(selRow, selRow);
                }
                if (jTable.getSelectedRow() != -1 && SwingUtilities.isRightMouseButton(e)) {
                    Publication td = tableModel.getRow(jTable.convertRowIndexToModel(jTable.getSelectedRow()));
                    if (jTable.getSelectedRow() == -1 || selRow == -1) {
                        downloadMenu.setEnabled(false);
                    } else {
                        downloadMenu.setEnabled(true);
                    }
                    if (td.getLocalPath() == null || td.getLocalPath().isEmpty())
                        openPDFMenu.setEnabled(false);
                    else
                        openPDFMenu.setEnabled(true);
                    popupTask.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        setOpaque(true);

    }

    private void initializeTextField() {
        jTextField = new HintTextField("Type to Search... ");
        jTextField.setFont(new Font("Arial Unicode MS", Font.TRUETYPE_FONT, 17));
        jTextField.setForeground(new Color(13, 72, 163));
        jTextField.setPreferredSize(new Dimension(350, 35));
        jTextField.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        updateSearchResult(SearchPanel.this.jTextField.getText());
                    }

                    public void insertUpdate(DocumentEvent e) {
                        updateSearchResult(SearchPanel.this.jTextField.getText());
                    }

                    public void removeUpdate(DocumentEvent e) {
                        updateSearchResult(SearchPanel.this.jTextField.getText());
                    }
                });
    }

    private void updateSearchResult(String query) {
        delayedQueueHandlerThread.put(query);
    }

    private void populateSearchResults(String query) {
        try {
            tableModel.clearTable();
            if (query != null) {
                if (query.isEmpty() || query.equals("*") || query.equals("**")) {
                    final List<Publication> publications = searcher.searchExisting(appContext.getMaxSearchResults());
                    publish(publications);
                } else {
                    final List<Publication> publications = searcher.search(query.trim() + "*", appContext.getMaxSearchResults());
                    publish(publications);
                }
            }
        } catch (ParseException e) {
            System.err.println("SearchPanel : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("SearchPanel : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void publish(List<Publication> publications) {
        for (Publication publication : publications) {
            tableModel.addPublication(publication);
        }
    }

    private void initializeTable() {
        jTable = new JTable(tableModel);
        jTable.setShowGrid(false);
//        jTable.setPreferredScrollableViewportSize(new Dimension(490, 450));
        jTable.setFillsViewportHeight(true);
//        jTable.setAutoCreateRowSorter(true);
        jTable.setRowHeight(48);
        jTable.setRowMargin(1);
        jTable.setDragEnabled(false);
        jTable.setIntercellSpacing(new Dimension(0, 0));
        jTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Utils.initializeTableColumns(jTable, tableModel.width);
        jTable.setFont(new Font("Arial", Font.TRUETYPE_FONT, 12));
        jTable.setForeground(new Color(13, 72, 163));
        TableCellRenderer dcr = jTable.getDefaultRenderer(String.class);
        if (dcr instanceof JLabel) {
            ((JLabel) dcr).setVerticalAlignment(SwingConstants.TOP);
            ((JLabel) dcr).setBorder(new EmptyBorder(1, 1, 1, 1));
        }
        JTableHeader header = jTable.getTableHeader();
        TableCellRenderer headerRenderer = header.getDefaultRenderer();
        if (headerRenderer instanceof JLabel) {
            ((JLabel) headerRenderer).setHorizontalAlignment(JLabel.CENTER);
        }
        header.setPreferredSize(new Dimension(30, 20));
    }
}
