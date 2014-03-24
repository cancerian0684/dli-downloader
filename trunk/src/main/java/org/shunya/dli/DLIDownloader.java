package org.shunya.dli;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.shunya.dli.AppConstants.*;
import static org.shunya.dli.Utils.checkNotNull;

public class DLIDownloader implements InteractiveTask {
    private final Logger logger = LoggerFactory.getLogger(DLIDownloader.class);

    private AppContext appContext;
    private String rootUrl;
    private String outputDir;
    private String barcode;
    private String filename;
    private PageSpread pageSpreadQueue;
    private Downloader downloader;
    private TiffToPDFConverter pdfConverter = new TiffToPDFConverter();
    private List<PageSpread.Page> failedDownloadBasket = Collections.synchronizedList(new ArrayList<PageSpread.Page>());
    private ExecutorService executorService;
    private DownloadObserver observer;
    private volatile RunState runState = RunState.Queued;
    private final Map<String, String> adminData = new HashMap<>();
    private Future<?> future;
    private volatile boolean cancelled = false;
    private int pdfFailures = 0;
    private final List<Future> futures;
    private LogWindow logWindow;
    private final String cleanupChar;
    private final String cleanupRegex;
    private final BarCodeInterpreter interpreter;
    private List<String> validHosts;
    private String postUrl;
    private String summary;
    private volatile int progress = 0;
    private String language;

    public DLIDownloader(String barcode, ExecutorService service, AppContext appContext, LogWindow logWindow, ServerQueue serverQueue) {
        this.barcode = barcode;
        this.executorService = service;
        this.appContext = appContext;
        this.logWindow = logWindow;
        this.cleanupChar = appContext.getCleanupChar();
        this.cleanupRegex = appContext.getCleanupRegex();
        this.futures = new ArrayList<>();
        this.interpreter = new BarCodeInterpreter(serverQueue, appContext);
    }

    @Override
    public void addMetadata(String key, String value) {
        adminData.put(key, value);
    }

    public void fetchMetaData() throws IOException, MetadataNotFound, CancelledExecutionException {
        adminData.putAll(interpreter.collect("/cgi-bin/DBscripts/allmetainfo.cgi?barcode=", barcode, appContext, logWindow, this));
        postUrl = adminData.get(URL);
        language = adminData.get(Language);
        validHosts = interpreter.getValidHost(postUrl, logWindow, this);
        URI url = URI.create(validHosts.get(0) + postUrl);
        int startPage = 1;
        int endPage = Integer.parseInt(adminData.get(AppConstants.TotalPages));
        if (appContext.isSearchForExtraPages()) {
            endPage = checkForExtraPages(url, endPage);
        }
        outputDir = FileSystems.getDefault().getPath(appContext.getRootDirectory(), barcode).toString();
        Files.createDirectories(Paths.get(outputDir));
        logger.info(url.toURL().toString() + " End Page = " + endPage);
        logWindow.log(url.toURL().toString() + " End Page = " + endPage);
        logger.info("Output Directory = " + outputDir);
        logWindow.log("Output Directory = " + outputDir);
        this.rootUrl = url.toURL().toString();
        this.downloader = Downloader.NIO_DOWNLOADER;
        this.pageSpreadQueue = new PageSpread(startPage, endPage);
        this.summary = refreshSummary();
    }

    private int checkForExtraPages(URI url, int endPage) throws CancelledExecutionException {
        logWindow.log("Checking existence of Extra pages in the End.");
        final char[] filename = {'0', '0', '0', '0', '0', '0', '0', '0', '.', 't', 'i', 'f'};
        int currentPage = endPage;
        int failures = 0;
        int maxConsecutiveFailures = 3;
        int newEndPage = currentPage;
        while (true) {
            assertNotCancelled();
            char[] seq = String.valueOf(++currentPage).toCharArray();
            int length = seq.length;
            int i = 0;
            for (int j = 8 - length; j < 8; j++) {
                filename[j] = seq[i++];
            }
            if (!Utils.pingUrl(url.toASCIIString() + new String(filename), logWindow))
                failures++;
            else {
                newEndPage = currentPage;
                failures = 0;
            }
            if (failures >= maxConsecutiveFailures)
                break;
        }
        logWindow.log("end page according to Metadata : " + endPage + ", Corrected end page : " + newEndPage);
        return newEndPage;
    }

    @Override
    public void download() throws InterruptedException, IOException, CancelledExecutionException, ExecutionException, MetadataNotFound, DocumentException {
        assertNotCancelled();
        runState = RunState.Downloading;
        notifyObserver();
        fetchMetadataWithRetry();
        notifyObserver();
        assertNotCancelled();
        List<HttpDownloadWorker> downloadWorkerList = new ArrayList<>(10);
        for (String validHost : validHosts) {
            final HttpDownloadWorker downloadTask = new HttpDownloadWorker(pageSpreadQueue, validHost, validHost + postUrl, outputDir, downloader, failedDownloadBasket, observer, this, appContext, logWindow);
            futures.add(executorService.submit(downloadTask));
            downloadWorkerList.add(downloadTask);
        }
        for (Future tmp : futures) {
            tmp.get();
        }
        assertNotCancelled();
        logger.info("Failed Downloads = " + failedDownloadBasket);
        logWindow.log("Failed Downloads = " + failedDownloadBasket);
        logger.info("Download Completed - " + barcode);
        logWindow.log("Download Completed - " + barcode);
        Collections.sort(downloadWorkerList, (o1, o2) -> {
            if (o1.getTotalDownloads() < o2.getTotalDownloads())
                return 1;
            else if (o1.getTotalDownloads() > o2.getTotalDownloads())
                return -1;
            else
                return 0;
        });
        logger.info("Servers sorted by speed : {}", downloadWorkerList);
        logWindow.log("Servers sorted by speed : " + downloadWorkerList);
        this.rootUrl = downloadWorkerList.get(0).getRootUrl();
        logger.info("Most efficient server was = {}", rootUrl);
        logWindow.log("Most efficient server was = " + rootUrl);
        convertToPdf();
        updateProgress(100 - pageSpreadQueue.getTotalPages() / 100);
        deleteTiffDirectory();
        updateProgress(100);
    }

    private void fetchMetadataWithRetry() throws CancelledExecutionException, IOException, MetadataNotFound {
        do {
            try {
                appContext.getTap().pauseIfDisconnected();
                fetchMetaData();
            } catch (Exception ie) {
                if (appContext.getTap().checkConnected()) {
                    throw ie;
                }
            }
        } while (!appContext.getTap().checkConnected());
    }

    public void assertNotCancelled() throws CancelledExecutionException {
        if (cancelled) {
            logWindow.log("Execution Cancelled for this task : " + getBarcode());
            throw new CancelledExecutionException("Execution Cancelled for task : " + getBarcode());
        }
    }

    public DLIDownloader convertToPdf() throws IOException, DocumentException, CancelledExecutionException {
        assertNotCancelled();
        runState = RunState.Converting;
        notifyObserver();
        pdfFailures = pdfConverter.convert(appContext.getRootDirectory(), barcode, getPdfName(), appContext, downloader, rootUrl, this, getPageSize(appContext.getQuality()), logWindow);
        assertNotCancelled();
        return this;
    }

    private Rectangle getPageSize(String quality) {
        if (quality == null) {
            quality = "a2";
        }
        logger.info("Setting PDF Quality to : " + quality);
        logWindow.log("Setting PDF Quality to : " + quality);
        switch (quality.toLowerCase()) {
            case "a2":
                return PageSize.A2;
            case "a3":
                return PageSize.A3;
            case "a4":
                return PageSize.A4;
            case "a5":
                return PageSize.A5;
            case "a6":
                return PageSize.A6;
            case "a7":
                return PageSize.A7;
            default:
                return PageSize.A3;
        }
    }


    public DLIDownloader deleteTiffDirectory() throws CancelledExecutionException {
        assertNotCancelled();
        notifyObserver();
        String path = Paths.get(appContext.getRootDirectory(), getBarcode()).toString();
        if (getFailCount() <= appContext.getMaxFailureForTIFFDeletion() && appContext.isDeleteTifIfSuccessful() && getPdfFailures() == 0) {
            runState = RunState.Deleting;
            Utils.deleteDir(path, logWindow);
            logger.info("Deleted TIFF Directory for Barcode : " + path);
            logWindow.log("Deleted TIFF Directory for Barcode : " + path);
        } else {
            logger.info("Not deleting TIFF directory. " + path);
            logWindow.log("Not deleting TIFF directory. " + path);
        }
        assertNotCancelled();
        return this;
    }

    @Override
    public String getBarcode() {
        return barcode;
    }

    @Override
    public String getSummary() {
        if (summary == null) {
            summary = refreshSummary();
        }
        return summary;
    }

    public String refreshSummary() {
        StringBuilder sbf = new StringBuilder();
        sbf.append(getBarcode());
        if (checkNotNull(getAttr(Title))) {
            sbf.append(" - " + getAttr(Title));
        }
        if (checkNotNull(getAttr(Author))) {
            sbf.append(", " + getAttr(Author));
        }
        if (checkNotNull(getAttr(TotalPages))) {
            sbf.append(", " + getAttr(TotalPages) + "p");
        }
        if (checkNotNull(getAttr(Subject))) {
            sbf.append(", " + getAttr(Subject));
        }
        if (checkNotNull(getAttr(Language))) {
            sbf.append(", " + getAttr(Language));
        }
        if (checkNotNull(getAttr(Year))) {
            sbf.append(" (" + getAttr(Year) + ")");
        }
        return sbf.toString();
    }

    public void updateProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public int getProgress() {
        return progress;
    }

    public String getAttr(String attribute) {
        return adminData.get(attribute) == null ? "" : adminData.get(attribute);
    }

    public int getFailCount() {
        return failedDownloadBasket.size();
    }

    @Override
    public void cancel() {
        if (runState == RunState.Completed)
            return;
        cancelled = true;
        runState = RunState.Cancelling;
        if (futures != null && futures.size() > 0) {
            for (final Future tmp : futures) {
                if (!tmp.isDone()) {
                    Thread t = new Thread("Shutdown-" + future) {
                        @Override
                        public void run() {
                            logger.info("Cancelling task " + barcode + ", " + System.currentTimeMillis());
                            tmp.cancel(true);
                            logger.info("Cancelled task " + barcode + ", " + System.currentTimeMillis());
                            runState = RunState.Cancelled;
                        }
                    };
                    t.start();
                } else {
                    runState = RunState.Cancelled;
                }
            }
        } else {
            runState = RunState.Cancelled;
        }
        notifyObserver();
    }

    @Override
    public void notifyObserver() {
        observer.update(this);
    }

    @Override
    public void awaitTermination() {
        try {
            for (Future tmp : futures) {
                tmp.get();
            }
            runState = RunState.Cancelled;
            notifyObserver();
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("exception waiting for cancellation. ", e);
        }
    }

    @Override
    public RunState getState() {
        return runState;
    }

    @Override
    public void setState(RunState state) {
        runState = state;
    }

    @Override
    public void withObserver(DownloadObserver observer) {
        this.observer = observer;
    }

    @Override
    public void beforeStart() {
        this.observer.started(this);
    }

    @Override
    public void afterComplete() {
        this.observer.completed(this);
    }

    @Override
    public void setFuture(Future<?> future) {
        this.future = future;
    }

    @Override
    public Future<?> getFuture() {
        return future;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DLIDownloader that = (DLIDownloader) o;
        return barcode.equals(that.barcode);
    }

    @Override
    public String toString() {
        return barcode.toString();
    }

    @Override
    public String getPdfName() {
        if (filename == null || filename.trim().isEmpty()) {
//            filename = barcode + " - " + getAttr(AppConstants.Title).replaceAll("[^a-zA-Z0-9-_\\.\\s,]", cleanupString) + ".pdf";
            filename = refreshSummary().replaceAll(cleanupRegex, cleanupChar) + ".pdf";
        }
        return filename;
    }

    @Override
    public String getRootDirectory() {
        return appContext.getRootDirectory();
    }

    public int getPdfFailures() {
        return pdfFailures;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void logs() {
        logWindow.setVisible(true);
    }

    @Override
    public void clean() {
        logWindow.dispose();
        pdfConverter = null;
    }

    @Override
    public String getLanguage() {
        return language;
    }
}
