package org.shunya.dli;

import com.itextpdf.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import static org.shunya.dli.AppConstants.*;
import static org.shunya.dli.Utils.getException;
import static org.shunya.dli.Utils.writeToFile;

public class DownloadPanel extends JPanel implements DownloadObserver {
    final Logger logger = LoggerFactory.getLogger(DownloadPanel.class);
    private JTable jTable;
    private JTextField jTextField;
    private JTextField jTextFieldDir;
    private JButton jButton;
    private final DownloadTableModel tableModel = new DownloadTableModel();
    private final AppContext appContext;
    private final BarcodeExtractor extractor;
    private final CachedDownloads downloads;
    private final Main window;
    private volatile int queueSize = 0;
    private final List<String> categories;


    private final JFileChooser fc = new JFileChooser();
    private DownloadJobs lastSavedJobs = new DownloadJobs();

    private final NewTaskInterpreter newTaskInterpreter = new NewTaskInterpreter();
    private final ServerQueue serverQueue;

    public void cancelAll() {
        appContext.setShutdown(true);
        saveAllJobs(getTableModel().getModelData());
        stopAllTasksGracefully();
        logger.info("Shutting down the CachedThreadPool Executor forcefully.");
        appContext.getThreadExecutorService().shutdownNow();
        logger.info("Shutting down the FixedThreadPool Executor forcefully.");
        appContext.getJobExecutorService().shutdownNow();
        try {
            appContext.getThreadExecutorService().awaitTermination(1, TimeUnit.MINUTES);
            logger.info("CachedThreadPool Terminated");
            appContext.getJobExecutorService().awaitTermination(1, TimeUnit.MINUTES);
            logger.info("FixedThreadPool Terminated");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopAllTasksGracefully() {
        List<InteractiveTask> modelData = getTableModel().getModelData();
        logger.info("Cancelling All running/queued Downloads");
        for (InteractiveTask task : modelData) {
            if (task.getState() == RunState.Downloading || task.getState() == RunState.Converting)
                task.cancel();
        }
        for (InteractiveTask task : modelData) {
            if (task.getState() == RunState.Downloading || task.getState() == RunState.Converting)
                task.awaitTermination();
        }
    }

    private void saveAllJobs(java.util.List<InteractiveTask> modelData) {
        DownloadJobs jobs = new DownloadJobs();
        for (InteractiveTask task : modelData) {
            if (task.getState() != RunState.Completed) {
                jobs.getBarCodes().add(task.getBarcode());
            }
        }
        if (!lastSavedJobs.getBarCodes().equals(jobs.getBarCodes())) {
            logger.info("Saving un completed jobs for the next startup.");
            Utils.save(DownloadJobs.class, jobs, DLI_JOBS_XML);
            lastSavedJobs = jobs;
        }
    }

    public void loadSavedJobs() {
        try {
            DownloadJobs jobs = Utils.load(DownloadJobs.class, DLI_JOBS_XML);
            for (String barcode : jobs.getBarCodes()) {
                addTask(extractor.extract(barcode));
                lastSavedJobs.getBarCodes().add(barcode);
            }
        } catch (IllegalAccessException | InstantiationException e) {
            logger.warn("Could not load saved Jobs, ", e);
        } catch (RejectedExecutionException e) {
            logger.warn("Excess downloads has been removed.", e);
        } catch (Exception e) {
            logger.error("Problem reloading Jobs", e);
        }
    }

    public DownloadPanel(final AppContext appContext, BarcodeExtractor extractor, final CachedDownloads downloads, final Main window) throws Exception {
        this.appContext = appContext;
        this.extractor = extractor;
        this.downloads = downloads;
        this.window = window;
        this.categories = Arrays.asList("Arabic", "Assamese", "Bengali", "English", "French", "German", "Greek", "Gujarati", "Hindi", "Irish", "Italian", "Kannada", "Marathi", "Norwegian", "Persian", "Punjabi", "Portuguese", "Russian", "Spanish", "Sanskrit", "Swedish", "Tamil", "Telugu", "Tibetan", "Urdu", "Multi", "Others", "Unknown");
        this.serverQueue = new ServerQueue(appContext.getDliServers());
        setLayout(new GridBagLayout());
        initializeTextField();
        initializeSaveToDirTextField();
        initializedSaveToDirBtn();
        initializeTable();

        final JMenuItem stopTaskMenu;
        final JMenuItem startTaskMenu;
        final JMenuItem openPDFMenu;
        final JMenuItem OpenFolder;
        final JMenuItem removeDownload;
        final JMenuItem removeAllDownload;
        final JMenuItem removeCompleted;
        final JMenuItem viewLogs;
        final JMenuItem indexBooks;
        final JPopupMenu popupTask = new JPopupMenu();
        stopTaskMenu = new JMenuItem("Stop Task");
        stopTaskMenu.addActionListener(e -> {
            if (jTable.getSelectedRow() != -1) {
                final InteractiveTask td = getTableModel().getRow(jTable.convertRowIndexToModel(jTable.getSelectedRow()));
                if (td.getState() != RunState.Completed && td.getState() != RunState.Failed && td.getState() != RunState.Cancelled) {
                    appContext.getThreadExecutorService().submit(new Runnable() {
                        @Override
                        public String toString() {
                            return "Stop : " + td.getBarcode();
                        }

                        @Override
                        public void run() {
                            try {
                                td.cancel();
                                logger.info("Stopping Task - " + td.getBarcode());
                            } catch (Exception ee) {
                                ee.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
        popupTask.add(stopTaskMenu);
        startTaskMenu = new JMenuItem("Start Task");
        startTaskMenu.addActionListener(e -> {
            if (jTable.getSelectedRow() != -1) {
                try {
                    InteractiveTask td = getTableModel().getRow(jTable.convertRowIndexToModel(jTable.getSelectedRow()));
                    if (removeTask(td)) {
                        addTask(td.getBarcode());
                    }
                    logger.info("Submitted Task - " + td.getBarcode());
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });
        popupTask.add(startTaskMenu);
        openPDFMenu = new JMenuItem("Open PDF");
        openPDFMenu.addActionListener(e -> {
            if (jTable.getSelectedRow() != -1) {
                try {
                    InteractiveTask td = getTableModel().getRow(jTable.convertRowIndexToModel(jTable.getSelectedRow()));
                    Path path = Paths.get(appContext.getRootDirectory(), td.getPdfName());
                    if (Files.exists(path)) {
                        Desktop.getDesktop().open(path.toFile());
                    } else {
                        logger.info("File not found : " + td.getPdfName());
                        JOptionPane.showMessageDialog(window.getFrame(), "PDF file does not exists : " + td.getPdfName(), "File Not Found", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });
        popupTask.add(openPDFMenu);

        OpenFolder = new JMenuItem("Explore");
        OpenFolder.addActionListener(e -> {
            if (jTable.getSelectedRow() != -1) {
                try {
                    InteractiveTask td = getTableModel().getRow(jTable.convertRowIndexToModel(jTable.getSelectedRow()));
                    Path path = Paths.get(appContext.getRootDirectory(), td.getBarcode());
                    if (Files.exists(path)) {
                        Desktop.getDesktop().open(path.toFile());
                    } else {
                        logger.info("Folder does not exists anymore - " + path.toString());
                        JOptionPane.showMessageDialog(window.getFrame(), "TIFF Folder does not exists, might have been deleted ! " + path.toString(), "TIFF Folder Not Found", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });
        popupTask.add(OpenFolder);

        removeDownload = new JMenuItem("Remove");
        removeDownload.addActionListener(e -> {
            if (jTable.getSelectedRow() != -1) {
                removeTask(getTableModel().getRow(jTable.convertRowIndexToModel(jTable.getSelectedRow())));
            }
        });
        popupTask.add(removeDownload);

        removeAllDownload = new JMenuItem("Remove All");
        removeAllDownload.addActionListener(e -> {
            int i = JOptionPane.showConfirmDialog(window.getFrame(), "This will remove all tasks (queued and completed, except running) \nDo you want to remove ?", "Remove All", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            if (i == JOptionPane.OK_OPTION) {
                List<InteractiveTask> modelData = getTableModel().getModelData();
                for (InteractiveTask task : modelData) {
                    removeTask(task);
                }
            }
        });
        popupTask.add(removeAllDownload);

        removeCompleted = new JMenuItem("Remove Completed");
        removeCompleted.addActionListener(e -> {
            int i = JOptionPane.showConfirmDialog(window.getFrame(), "This will remove Completed & Not_Found Tasks \nDo you want to remove ?", "Remove Completed", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            if (i == JOptionPane.OK_OPTION) {
                List<InteractiveTask> modelData = getTableModel().getModelData();
                for (InteractiveTask task : modelData) {
                    removeCompleted(task);
                }
            }

        });
        popupTask.add(removeCompleted);

        popupTask.addSeparator();

        viewLogs = new JMenuItem("View Logs");
        viewLogs.addActionListener(e -> {
            if (jTable.getSelectedRow() != -1) {
                InteractiveTask row = getTableModel().getRow(jTable.convertRowIndexToModel(jTable.getSelectedRow()));
                row.logs();
            }
        });
        popupTask.add(viewLogs);

        indexBooks = new JMenuItem("Index Language");
        indexBooks.addActionListener(e -> {
            indexLanguage(window, appContext);
        });
        popupTask.add(indexBooks);

        jTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = jTable.rowAtPoint(e.getPoint());
                if (selRow != -1) {
                    jTable.setRowSelectionInterval(selRow, selRow);
                    int selRowInModel = jTable.convertRowIndexToModel(selRow);
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (jTable.getSelectedRow() == -1 || selRow == -1) {
                        stopTaskMenu.setEnabled(false);
                    } else {
                        stopTaskMenu.setEnabled(true);
                    }
                    popupTask.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });


        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.weightx = 0.0;
        c.weighty = 0.025;
        c.gridx = 0;
        c.gridy = 0;
        add(jTextField, c);

        c.fill = GridBagConstraints.BOTH;
        c.ipady = 0;      //make this component tall
        c.weightx = 0.0;
        c.weighty = 0.97;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 1;
        add(new JScrollPane(jTable), c);


        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.weightx = 0.8;
        c.weighty = 0.015;
        c.gridx = 0;
        c.gridy = 2;
        add(jTextFieldDir, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
//        c.weightx = 0.3;
        c.weighty = 0.015;
        c.gridx = 1;
        c.gridy = 2;
        add(jButton, c);

        loadSavedJobs();
        setOpaque(true);
    }

    public void indexLanguage(final Main window, final AppContext appContext) {
        final String language = (String) JOptionPane.showInputDialog(
                window.getFrame(),
                "Select the Language:",
                "Choose Language",
                JOptionPane.PLAIN_MESSAGE,
                null,
                categories.toArray(),
                categories.get(0));
        indexLanguage(window, appContext, language);
    }

    public void indexLanguage(Main window, AppContext appContext, String language) {
        if (language == null || language.isEmpty())
            return;
        final LogWindow logWindow = new LogWindow(appContext.getLogBufferSize(), "Logs - " + language);
        final InteractiveTask task = new IndexDLITask(language, appContext, logWindow);
        if (!task.getBarcode().isEmpty() && getTableModel().addDownload(task)) {
            task.withObserver(this);
            Future<?> future = appContext.getJobExecutorService().submit(() -> {
                if (appContext.isShutdown() || task.isCancelled()) {
                    System.err.println("Cancelling Task -- " + task.getBarcode());
                    task.setState(RunState.Cancelled);
                    task.notifyObserver();
                    return;
                }
                try {
                    task.beforeStart();
                    downloadWithRetry(task);
                    task.setState(RunState.Completed);
                    window.indexLocalDirectories();
                    appContext.getIndexedLanguages().add(language.toLowerCase());
                    appContext.setLastIndexUpdate(new Date());
                    logWindow.log("Index Language Task Completed successfully.");
                } catch (CancelledExecutionException | CancellationException | InterruptedException e) {
                    task.setState(RunState.Cancelled);
                    logger.warn("Task Cancelled for this barcode : " + language, e);
                    logWindow.log(Utils.getException(e));
                    logWindow.log("Task Cancelled for this barcode : " + language);
                } catch (MetadataNotFound e) {
                    task.setState(RunState.Not_Found);
                    logger.warn(e.getMessage(), e);
                    logWindow.log(Utils.getException(e));
                    logWindow.log("Task Failed for this barcode : " + language);
                } catch (Exception e) {
                    task.setState(RunState.Failed);
                    logWindow.log(Utils.getException(e));
                    logWindow.log("Download Failed for this barcode : " + language);
                    logger.error("Error Occurred in downloading the barcode : " + task.getBarcode(), e);
                    writeToFile("Error Occurred in downloading the barcode : " + task.getBarcode() + "\r\n" + getException(e), Paths.get(task.getRootDirectory(), task.getBarcode() + ".log"));
                } finally {
                    task.notifyObserver();
                    task.afterComplete();
                    task.notifyObserver();
                }
            });
            task.setFuture(future);
        }
    }


    private boolean removeCompleted(InteractiveTask td) {
        try {
            if (td.getState() == RunState.Completed || td.getState() == RunState.Not_Found) {
                return removeTaskFromTable(td);
            }
        } catch (Exception ee) {
            logger.warn("Error occurred : ", ee);
        }
        return false;
    }

    private boolean removeTask(InteractiveTask td) {
        try {
            if (td.getState() == RunState.Completed || td.getState() == RunState.Failed || td.getState() == RunState.Cancelled || td.getState() == RunState.Not_Found) {
                return removeTaskFromTable(td);
            } else if (td.getState() == RunState.Queued) {
                if (appContext.getWorkQueue().remove(td.getFuture())) {
                    return removeTaskFromTable(td);
                }
            } else {
                logger.info("Can not remove task as the download is currently " + td.getState() + " - " + td.getBarcode());
            }
        } catch (Exception ee) {
            logger.warn("Error occurred : ", ee);
        }
        return false;
    }

    private void initializeSaveToDirTextField() {
        jTextFieldDir = new JTextField();
        jTextFieldDir.setPreferredSize(new Dimension(300, 30));
        jTextFieldDir.setText(appContext.getRootDirectory());
        jTextFieldDir.setEnabled(false);
    }

    private void initializedSaveToDirBtn() {
        jButton = new JButton("Download To");
        jButton.setPreferredSize(new Dimension(70, 30));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == jButton) {
                    fc.setCurrentDirectory(new File(appContext.getRootDirectory()));
                    int returnVal = fc.showOpenDialog(DownloadPanel.this);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        jTextFieldDir.setText(file.getAbsolutePath());
                        appContext.setRootDirectory(file.getAbsolutePath());
                        logger.info("Changing Download Directory To : " + file.getAbsolutePath());
                    } else {
                        logger.info("Action cancelled by the user.");
                    }
                }
            }
        });
    }

    private boolean removeTaskFromTable(InteractiveTask td) {
        getTableModel().deleteRow(td);
        td.clean();
        logger.info("Removing Download with Barcode : " + td.getBarcode());
        saveAllJobs(getTableModel().getModelData());
        return true;
    }

    private void initializeTable() {
        jTable = new JTable(tableModel) {
            private final Color green = new Color(0x075D0B);
            private final Color progress = new Color(210, 96, 10);
            private final Color selection = new Color(210, 206, 47);

            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    int modelRow = convertRowIndexToModel(row);
                    InteractiveTask task = getTableModel().getRow(modelRow);
                    if (task.getState() == RunState.Completed) {
                        c.setForeground(green);
                    } else if (task.getState() == RunState.Failed || task.getState() == RunState.Not_Found || task.getState() == RunState.Cancelled) {
                        c.setForeground(Color.RED);
                    } else if (task.getState() == RunState.Converting || task.getState() == RunState.Downloading || task.getState() == RunState.Deleting) {
                        c.setForeground(progress);
                    } else {
                        c.setForeground(getForeground());
                    }
                } else {
                    c.setForeground(selection);
//                    c.setBackground(getSelectionBackground());
                }
                return c;
            }
        };
        jTable.setShowGrid(false);
//        jTable.setPreferredScrollableViewportSize(new Dimension(490, 450));
        jTable.setFillsViewportHeight(true);
//        jTable.setAutoCreateRowSorter(true);
        jTable.setRowHeight(45);
        jTable.setRowMargin(0);
        jTable.setDragEnabled(false);
        jTable.setIntercellSpacing(new Dimension(1, 1));
        jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utils.initializeTableColumns(jTable, tableModel.width);
        jTable.getColumn("<html><b>Progress").setCellRenderer(new ProgressRenderer());
        jTable.setFont(new Font("Arial Unicode MS", Font.TRUETYPE_FONT, 12));
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

    private void interpretNewTaskAndSubmit(String input) {
        if (input == null || input.trim().isEmpty()) {} else if (input.startsWith("http://") || input.startsWith("https://")) {
            addHttpDownloadTask(input.trim());
        } else {
            List<String> barcodes = newTaskInterpreter.getBarcodes(input.trim(), extractor);
            for (String barcode : barcodes) {
                addTask(barcode);
            }
            saveAllJobs(getTableModel().getModelData());
        }
    }

    private void initializeTextField() {
        jTextField = new HintTextField("Enter Barcode/Dir Path/File Path... ");
        jTextField.setFont(new Font("Arial Unicode MS", Font.TRUETYPE_FONT, 16));
        jTextField.setForeground(new Color(13, 72, 163));
        jTextField.setPreferredSize(new Dimension(350, 35));
        jTextField.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                interpretNewTaskAndSubmit(DownloadPanel.this.jTextField.getText());
                jTextField.setText("");
            }
        });
    }

    public void addHttpDownloadTask(final String url) {
        final LogWindow logWindow = new LogWindow(appContext.getLogBufferSize(), "Logs - " + url);
        final InteractiveTask task = new HttpDownloadTask(url, appContext, logWindow);
        if (!task.getBarcode().isEmpty() && getTableModel().addDownload(task)) {
            task.withObserver(this);
            Future<?> future = appContext.getJobExecutorService().submit(() -> {
                if (appContext.isShutdown() || task.isCancelled()) {
                    System.err.println("Cancelling Task -- " + task.getBarcode());
                    return;
                }
                try {
                    task.beforeStart();
                    downloadWithRetry(task);
                    task.setState(RunState.Completed);
                } catch (CancelledExecutionException | CancellationException | InterruptedException e) {
                    task.setState(RunState.Cancelled);
                    logger.warn("Task Cancelled for this barcode : " + url, e);
                    logWindow.log(Utils.getException(e));
                    logWindow.log("Task Cancelled for this barcode : " + url);
                } catch (MetadataNotFound e) {
                    task.setState(RunState.Not_Found);
                    logger.warn(e.getMessage(), e);
                    logWindow.log(Utils.getException(e));
                    logWindow.log("Task Failed for this barcode : " + url);
                } catch (Exception e) {
                    task.setState(RunState.Failed);
                    logWindow.log(Utils.getException(e));
                    logWindow.log("Download Failed for this barcode : " + url);
                    logger.error("Error Occurred in downloading the barcode : " + task.getBarcode(), e);
                    writeToFile("Error Occurred in downloading the barcode : " + task.getBarcode() + "\r\n" + getException(e), Paths.get(task.getRootDirectory(), task.getBarcode() + ".log"));
                } finally {
                    task.notifyObserver();
                    task.afterComplete();
                    task.notifyObserver();
                }
            });
            task.setFuture(future);
        }
    }

    public void addTask(final String barcode) {
        try {
            final Publication publication = appContext.getSearcher().searchBook(barcode);
            if (publication != null && publication.getLocalPath() != null && !publication.getLocalPath().isEmpty()) {
                int i = JOptionPane.showConfirmDialog(window.getFrame(), "PDF already exists for this barcode, download again - " + barcode + "\n", "PDF Exists", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
                if (i == JOptionPane.CANCEL_OPTION || i == JOptionPane.CLOSED_OPTION) {
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        final LogWindow logWindow = new LogWindow(appContext.getLogBufferSize(), "Logs - " + barcode);

        final InteractiveTask task = new DLIDownloader(barcode, appContext.getThreadExecutorService(), appContext, logWindow, serverQueue);
        try {
            final Publication publication = appContext.getSearcher().searchBook(barcode);
            task.addMetadata(Title, publication.getTitle());
            task.addMetadata(Author, publication.getAuthor());
            task.addMetadata(Subject, publication.getSubject());
            task.addMetadata(Language, publication.getLanguage());
            task.addMetadata(TotalPages, publication.getPages());
            task.addMetadata(Year, publication.getYear());
            task.addMetadata(URL, publication.getUrl());
        } catch (Exception ignored) {}
        if (!task.getBarcode().isEmpty() && getTableModel().addDownload(task)) {
            task.withObserver(DownloadPanel.this);
            Future<?> future = appContext.getJobExecutorService().submit(new Runnable() {
                @Override
                public String toString() {
                    return "Task : " + task.getBarcode();
                }

                @Override
                public void run() {
                    if (appContext.isShutdown() || task.isCancelled()) {
                        System.err.println("Cancelling Task -- " + task.getBarcode());
                        return;
                    }
                    try {
                        task.beforeStart();
                        downloadWithRetry(task);
                        task.setState(RunState.Completed);
                        if (appContext.isAutomaticallyIndexNewLanguages()) {
                            if (!appContext.getIndexedLanguages().contains(task.getLanguage().toLowerCase())) {
                                indexLanguage(window, appContext, task.getLanguage());
                            }
                        }
                        logWindow.log("Download Task Completed successfully.");
                        Path path = Paths.get(appContext.getRootDirectory(), task.getPdfName());
                        downloads.add(task.getBarcode(), path);
                        downloads.commitChanges();
                    } catch (CancelledExecutionException | CancellationException | InterruptedException e) {
                        task.setState(RunState.Cancelled);
                        logger.warn("Download Cancelled for this barcode : " + barcode, e);
                        logWindow.log(Utils.getException(e));
                        logWindow.log("Download Cancelled for this barcode : " + barcode);
                    } catch (MetadataNotFound e) {
                        task.setState(RunState.Not_Found);
                        logger.warn(e.getMessage(), e);
                        logWindow.log(Utils.getException(e));
                        logWindow.log("Download Failed for this barcode : " + barcode);
                    } catch (Throwable e) {
                        task.setState(RunState.Failed);
                        logWindow.log(Utils.getException(e));
                        logWindow.log("Download Failed for this barcode : " + barcode);
                        logger.error("Error Occurred in downloading the barcode : " + task.getBarcode(), e);
                        writeToFile("Error Occurred in downloading the barcode : " + task.getBarcode() + "\r\n" + getException(e), Paths.get(task.getRootDirectory(), task.getBarcode() + ".log"));
                    } finally {
                        task.notifyObserver();
                        task.afterComplete();
                        task.notifyObserver();
                    }
                }
            });
            task.setFuture(future);
        } else if (task.getBarcode().isEmpty()) {
//                JOptionPane.showMessageDialog(jFrame, "Barcode can't be empty !", "Barcode Empty", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(window.getFrame(), "Barcode already added for download : " + jTextField.getText(), "Duplicate Job", JOptionPane.ERROR_MESSAGE);
        }

    }

    private void downloadWithRetry(InteractiveTask task) throws CancelledExecutionException, InterruptedException, IOException, ExecutionException, MetadataNotFound, DocumentException {
        do {
            try {
                appContext.getTap().pauseIfDisconnected();
                task.download();
            } catch (CancelledExecutionException ce) {
                throw ce;
            } catch (Exception ie) {
                if (appContext.getTap().checkConnected()) {
                    throw ie;
                }
            }
        } while (!appContext.getTap().checkConnected());
    }

    public DownloadTableModel getTableModel() {
        return tableModel;
    }

    @Override
    public void started(InteractiveTask task) {
        if (queueSize == 0) {
            window.setBusy();
        }
        ++queueSize;
    }

    @Override
    public void update(InteractiveTask task) {
        getTableModel().update(task);
    }

    @Override
    public void completed(InteractiveTask task) {
        saveAllJobs(getTableModel().getModelData());
        switch (task.getState()) {
            case Cancelled:
                window.displayMsg(task.getBarcode() + " " + task.getState().toString(), TrayIcon.MessageType.WARNING);
                break;
            case Completed:
                appContext.incrementBooks();
                window.displayMsg(task.getBarcode() + " " + task.getState().toString(), TrayIcon.MessageType.INFO);
                break;
            case Failed:
                window.displayMsg(task.getBarcode() + " " + task.getState().toString(), TrayIcon.MessageType.ERROR);
                break;
        }
        --queueSize;
        if (queueSize == 0) {
            window.setIdle();
        }
    }

    static class ProgressRenderer extends JProgressBar implements TableCellRenderer {
        public ProgressRenderer() {
            super();
            this.setStringPainted(true);
            //  this.setIndeterminate(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.setValue(Integer.parseInt(value.toString()));
            return this;
        }

        public boolean isDisplayable() {
            return true;
        }

        /*public void repaint() {
            try {
                //   theTable.repaint();
            } catch (Exception e) {
                System.out.println("some unknown error");
            }
        }*/
    }

    static class NewTaskInterpreter {
        public java.util.List<String> getBarcodes(final String input, final BarcodeExtractor extractor) {
            java.util.List<String> result = new ArrayList<>();
            try {
                Path inputPath = Paths.get(input);
                if (Files.isDirectory(inputPath)) {
                    try (DirectoryStream<Path> ds = Files.newDirectoryStream(inputPath)) {
                        for (Path path : ds) {
                            if (Files.isDirectory(path)) {
                                String item = extractor.extract(path.getFileName().toString());
                                if (item != null && !item.trim().isEmpty())
                                    result.add(item);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (Files.exists(inputPath)) {
                    String contents = Utils.readFromFile(inputPath);
                    String[] barcodes = Utils.extractBarcodes(contents);
                    for (String barcode : barcodes) {
                        if (barcode != null && !barcode.isEmpty()) {
                            String item = extractor.extract(barcode);
                            if (item != null && !item.trim().isEmpty())
                                result.add(item);
                        }
                    }
                } else {
                    result.add(extractor.extract(input));
                }
            } catch (Exception e) {
                result.add(extractor.extract(input));
            }
            return result;
        }
    }
}