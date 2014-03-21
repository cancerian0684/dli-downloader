package org.shunya.dli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.shunya.dli.AppConstants.DLI_SETTINGS_XML;
import static org.shunya.dli.Utils.writeToFile;

public class Main implements TapListener {
    final Logger logger = LoggerFactory.getLogger(Main.class);
    private JFrame jFrame;
    private SystemTray tray;
    private TrayIcon trayIcon;
    private AppContext appContext;
    private BufferedImage idleImage, busyImage, pauseImage, currentImage, lastImage;
    private final WindowListener exitListener = new ExitListener();
    private final WindowListener resetConfigListner = new ResetConfigListener();
    final Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            synchronized (this) {
                reportException(e);
            }
        }
    };

    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = Main.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public void reportException(Throwable e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter();
        sw.write(Utils.getException(e));
        sw.write("\n\n");
        Utils.listSystemProperties(new PrintWriter(sw));
        try {
            Path path = FileSystems.getDefault().getPath(System.getProperty("user.home"));
            sw.write("\n\nSettings\n");
            sw.write(Utils.readFromFile(path.resolve(DLI_SETTINGS_XML)));
        } catch (Exception ex) {
        }
        final ImageIcon icon = createImageIcon("/images/dli-red.png");
        String userEmail = (String) JOptionPane.showInputDialog(
                jFrame,
                "Unknown failure has occurred !\r\nPlease enter your email address for followup",
                "Report Unknown Failure to Developer ?",
                JOptionPane.ERROR_MESSAGE,
                icon,
                null,
                appContext.getUserEmail());
        if ((userEmail != null) && (userEmail.length() > 0)) {
            appContext.setUserEmail(userEmail);
            DevEmailService.getInstance().sendEmail("DLIDownloader Exception for user : [" + appContext.getUsername() + "] ", "cancerian0684@gmail.com", userEmail, "PFA", Arrays.<byte[]>asList(sw.toString().getBytes()), asList("Exception-Stack-Trace.txt"));
        }
        writeToFile("Uncaught exception : " + e.getMessage() + sw.toString(), Paths.get(appContext.getRootDirectory(), "" + System.currentTimeMillis() + ".log"));
    }

    private SearchPanel searchPanel;
    private LuceneSearcher searcher;
    private LuceneIndexer indexer;
    private AppConfigPanel configPanel;
    private DownloadPanel downloadPanel;
    private final BarcodeExtractor extractor = new BarcodeExtractor();
    private CachedDownloads downloadCache;
    private volatile boolean cancelled = false;
    private boolean currentlyBusy = false;
    private SingleInstanceFileLock singleInstanceFileLock;
    private final Properties properties = new Properties();

    public Main() {
        try {
            init();
        } catch (Exception e) {
            reportException(e);
        }
    }

    public void init() throws Exception {
        properties.load(Main.class.getClassLoader().getResourceAsStream("dev.properties"));
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36");
        jFrame = new JFrame(properties.getProperty(AppConstants.JFRAME_TITLE));
        singleInstanceFileLock = new SingleInstanceFileLock(AppConstants.Lock_File, jFrame);
        if (singleInstanceFileLock.checkIfAlreadyRunning())
            System.exit(1);
        appContext = Utils.load(AppContext.class, DLI_SETTINGS_XML);
        try {
            UIManager.setLookAndFeel(getSystemLookAndFeelClassName(appContext.getLookAndFeelNumber()));
        } catch (Exception e) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
        downloadCache = new CachedDownloads(appContext.getRootDirectory() + ";" + appContext.getDownloadDirectories(), extractor, appContext);
        idleImage = ImageIO.read(DownloadPanel.class.getResourceAsStream("/images/dli-blue.png"));
        busyImage = ImageIO.read(DownloadPanel.class.getResourceAsStream("/images/dli-red.png"));
        pauseImage = ImageIO.read(DownloadPanel.class.getResourceAsStream("/images/dli-yellow.png"));
        currentImage = idleImage;

        PopupMenu popup = new PopupMenu();
        final MenuItem openPunterMenuItem = new MenuItem("Window");
        openPunterMenuItem.setFont(new Font("Tahoma", Font.BOLD, 12));
        openPunterMenuItem.addActionListener(e -> {
            jFrame.setExtendedState(Frame.NORMAL);
            jFrame.setVisible(true);
        });
        popup.add(openPunterMenuItem);
        final MenuItem pauseAllDownloads = new MenuItem("Pause All");
        pauseAllDownloads.setFont(new Font("Tahoma", Font.BOLD, 12));
        pauseAllDownloads.setActionCommand("pause");
        pauseAllDownloads.addActionListener(e -> {
            if (pauseAllDownloads.getActionCommand().equalsIgnoreCase("pause")) {
                appContext.getTap().off();
                pauseAllDownloads.setActionCommand("resume");
                pauseAllDownloads.setLabel("Resume All");
            } else {
                appContext.getTap().on();
                pauseAllDownloads.setLabel("Pause All");
                pauseAllDownloads.setActionCommand("pause");
            }
        });
        popup.add(pauseAllDownloads);

        final MenuItem aboutMenuItem = new MenuItem("About");
        aboutMenuItem.setFont(new Font("Tahoma", Font.BOLD, 12));
        aboutMenuItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(jFrame, "Developer : Munish Chandel [cancerian0684@gmail.com] \nCo-Developer : Arun Sharma [arunsharma.nith@gmail.com]\n\n" +
                    "Donate for this work (atleast Rs.100)\n" +
                    "Bank Details for NEFT Transfer\n" +
                    "MUNISH CHANDEL\n" +
                    "a/c 5277618224\n" +
                    "IFSC : CITI0000002\n" +
                    "Citi Bank, Branch NA. Delhi", "Developers", JOptionPane.INFORMATION_MESSAGE);
        });
        popup.add(aboutMenuItem);

        popup.addSeparator();

        MenuItem resetConfigItem = new MenuItem("Reset Config");
        resetConfigItem.addActionListener((ActionListener) resetConfigListner);
        popup.add(resetConfigItem);

        MenuItem scanDownloadItem = new MenuItem("Index Downloads");
        scanDownloadItem.addActionListener(e -> {
            int confirm = JOptionPane.showOptionDialog(jFrame,
                    "Are You Sure You want to scan local downloads ? \n\nThis might takes minutes of time",
                    "Scan all local downloads ?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (confirm == JOptionPane.YES_OPTION) {
                indexLocalDirectories();
            }
        });
        popup.add(scanDownloadItem);

        MenuItem defaultItem = new MenuItem("Shutdown");
        defaultItem.addActionListener((ActionListener) exitListener);
        popup.add(defaultItem);

        if (SystemTray.isSupported()) {
            tray = SystemTray.getSystemTray();
            jFrame.setIconImage(currentImage);
            trayIcon = new TrayIcon(currentImage, "DLI Downloader Tool", popup);
            trayIcon.setToolTip("DLI Downloader Tool");
            trayIcon.setImageAutoSize(true);
            ActionListener actionListener = e -> {
                jFrame.setExtendedState(Frame.NORMAL);
                jFrame.setVisible(true);
            };

            MouseListener mouseListener = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        jFrame.setExtendedState(Frame.NORMAL);
                        jFrame.setVisible(true);
                    }
                }
            };

            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(actionListener);
            trayIcon.addMouseListener(mouseListener);
            try {
                tray.add(trayIcon);
                trayIcon.displayMessage("DLI Downloader", "Click here to launch DLI Downloader", TrayIcon.MessageType.INFO);
            } catch (AWTException e) {
                logger.error("SystemTray Icon could not be loaded. ", e);
            }
        }

        appContext.getTap().setListener(this);
        configPanel = new AppConfigPanel(appContext);
        indexer = new LuceneIndexer(appContext);
        searcher = new LuceneSearcher(indexer.getWriter());
        appContext.setSearcher(searcher);
        appContext.setIndexer(indexer);
        downloadPanel = new DownloadPanel(appContext, extractor, downloadCache, this);
        searchPanel = new SearchPanel(downloadCache, searcher, downloadPanel, appContext, this);

        addShutdownHook();
        createAndShowGUI();

        askUserToIndexLanguage();
        askUserToScanDirectories();

        singleInstanceFileLock.unlockFile();
    }

    private void askUserToScanDirectories() {
        if (!appContext.isIndexedLocalDirectories()) {
            int confirm = JOptionPane.showOptionDialog(jFrame,
                    "You can index your local download directories for enabling more features ? \n\nThis might takes minutes of time",
                    "Scan all local downloads ?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (confirm == JOptionPane.YES_OPTION) {
                indexLocalDirectories();
                appContext.setIndexedLocalDirectories(true);
            }
        }
    }

    private void askUserToIndexLanguage() {
        int totalDocs = 0;
        try {
            totalDocs = searcher.countDocs();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (totalDocs == 0) {
            int confirm = JOptionPane.showOptionDialog(jFrame, "Please Create an Index for Books to enable local search ? \n\nThis might takes minutes of time",
                    "Create Local Book Index", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (confirm == JOptionPane.YES_OPTION) {
                appContext.setIndexedLocalDirectories(true);
                downloadPanel.indexLanguage(this, appContext);
            }
        }
    }

    public void indexLocalDirectories() {
        appContext.getThreadExecutorService().submit(() -> {
            logger.info("Building Local Index");
            downloadCache.buildLocalIndex();
            appContext.setIndexedLocalDirectories(true);
            logger.info("Local Index built successfully");
        });
    }

    private String getSystemLookAndFeelClassName(int lookAndFeelNumber) {
        switch (lookAndFeelNumber) {
            case 0:
                return UIManager.getSystemLookAndFeelClassName();
            case 1:
                return "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
            case 2:
                return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
            case 3:
                return "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
            case 4:
                return "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
            default:
                return UIManager.getSystemLookAndFeelClassName();
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (appContext.isResetConfig()) {
                    logger.warn("Resetting DLI config now.");
                    Utils.save(AppContext.class, new AppContext(), DLI_SETTINGS_XML);
                } else
                    Utils.save(AppContext.class, appContext, DLI_SETTINGS_XML);
                if (!cancelled)
                    downloadPanel.cancelAll();
                cancelled = true;
                try {
                    if (searcher != null)
                        searcher.close();
                    if (indexer != null)
                        indexer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    logger.warn("DLI Shutdown Complete");
                }
            }
        });
    }

    class ResetConfigListener extends WindowAdapter implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int confirm = JOptionPane.showOptionDialog(jFrame,
                    "Are You Sure to reset DLI Downloader Config ? \n\nAll Settings will be reset to default upon next startup",
                    "Reset DLI Downloader Config ?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (confirm == JOptionPane.YES_OPTION) {
                appContext.setResetConfig(true);
            }
        }
    }

    class ExitListener extends WindowAdapter implements ActionListener {
        @Override
        public void windowClosing(WindowEvent e) {
            exit();
        }

        private void exit() {
            int confirm = JOptionPane.showOptionDialog(jFrame,
                    "Are You Sure to quit this DLI Downloader ? \n\nAll Queued/Running Jobs will be Saved",
                    "Exit DLI Downloader ?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (confirm == JOptionPane.YES_OPTION) {
                logger.warn("Shutting DLI Downloader");
                if (jFrame.isVisible()) {
                    disposeWindow();
                }
                tray.remove(trayIcon);
                System.exit(0);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            exit();
        }
    }

    private void disposeWindow() {
        appContext.setLocation(jFrame.getLocation());
        appContext.setSize(jFrame.getSize());
        jFrame.setVisible(false);
        jFrame.dispose();
    }

    public void createAndShowGUI() throws Exception {
        JFrame.setDefaultLookAndFeelDecorated(true);
        jFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowIconified(WindowEvent e) {
                disposeWindow();
            }

            public void windowClosing(WindowEvent e) {
                disposeWindow();
            }
        });
        jFrame.setLocationRelativeTo(null);
        if (appContext.getLocation() != null)
            jFrame.setLocation(appContext.getLocation());
        if (appContext.getSize() != null)
            jFrame.setPreferredSize(appContext.getSize());
        JTabbedPane tabbedPane = new JTabbedPane();
        if (searchPanel != null)
            tabbedPane.addTab("Search", null, searchPanel, "Search Books Catalogue");
        tabbedPane.addTab("Downloads", null, downloadPanel, "Download Books From DLI");
        tabbedPane.addTab("Settings", null, configPanel, "Customize Settings");
        tabbedPane.addTab("Help & FAQ", null, new HtmlContentPanel("help.txt"), "Help and FAQ");
        tabbedPane.addTab("About Us", null, new HtmlContentPanel("about.txt"), "About Team");
        jFrame.setContentPane(tabbedPane);
        jFrame.setIgnoreRepaint(false);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    public void setIdle() {
        currentlyBusy = false;
        currentImage = idleImage;
        updateIconImage();
    }

    public JFrame getFrame() {
        return jFrame;
    }

    public void setBusy() {
        currentlyBusy = true;
        currentImage = busyImage;
        updateIconImage();
    }

    public void updateIconImage() {
        SwingUtilities.invokeLater(() -> {
            jFrame.setIconImage(currentImage);
            trayIcon.setImage(currentImage);
        });
    }

    public void displayMsg(String msg, TrayIcon.MessageType msgType) {
        if (trayIcon != null) {
            trayIcon.displayMessage("DLI Downloader", msg, msgType);
        }
    }

    @Override
    public void pause() {
        lastImage = currentImage;
        currentImage = pauseImage;
        updateIconImage();
    }

    @Override
    public void resume() {
        if (currentlyBusy)
            currentImage = busyImage;
        else
            currentImage = idleImage;
        updateIconImage();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main();
        });
    }
}
