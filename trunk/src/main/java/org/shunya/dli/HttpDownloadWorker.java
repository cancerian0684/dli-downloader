package org.shunya.dli;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.shunya.dli.PageSpread.Page;

public class HttpDownloadWorker implements Callable {
    private final PageSpread queue;
    private final String serverHost;
    private final String rootUrl;
    private final String outputDir;
    private final Downloader downloader;
    private final List<Page> failedDownloads;
    private final DownloadObserver observer;
    private final DLIDownloader task;
    private final AppContext appContext;
    private final LogWindow simpleLogger;
    private AtomicInteger downloads = new AtomicInteger(0);

    public HttpDownloadWorker(PageSpread queue, String serverHost, String rootUrl, String outputDir, Downloader downloader, List<Page> failedDownloads, DownloadObserver observer, DLIDownloader dliDownloader, AppContext appContext, LogWindow simpleLogger) {
        this.queue = queue;
        this.serverHost = serverHost;
        this.rootUrl = rootUrl;
        this.outputDir = outputDir;
        this.downloader = downloader;
        this.failedDownloads = failedDownloads;
        this.observer = observer;
        this.task = dliDownloader;
        this.appContext = appContext;
        this.simpleLogger = simpleLogger;
    }

    @Override
    public Void call() throws InterruptedException, CancelledExecutionException {
        Page page = queue.poll();
        int updateInterval = queue.getTotalPages() / 100;
        if (updateInterval < 1) updateInterval = 1;
        while (page != null && !Thread.currentThread().isInterrupted()) {
            appContext.getTap().await();
            if (page.canDownload(appContext.getMaxRetryCount())) {
                boolean status = downloader.download(rootUrl, page.getAndIncrement(), outputDir, appContext, false, simpleLogger, 1 << 22, true);
                if (!status) {
                    queue.offer(page);
                }
                downloads.incrementAndGet();
                appContext.incrementPages();
            } else {
                failedDownloads.add(page);
            }
            if (queue.getCounter() % updateInterval == 0) {
                updateProgress();
            }
            page = queue.poll();
        }
        updateProgress();
        return null;
    }

    public void updateProgress() {
        task.updateProgress(100 * (queue.getCounter()) / queue.getTotalPages() - 2 * queue.getTotalPages() / 100);
        observer.update(task);
    }

    public int getTotalDownloads() {
        return downloads.get();
    }

    public String getRootUrl() {
        return rootUrl;
    }

    @Override
    public String toString() {
        return "{" + serverHost + ", " + getTotalDownloads() + "}";
    }
}