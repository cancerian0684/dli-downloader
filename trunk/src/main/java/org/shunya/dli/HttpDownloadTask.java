package org.shunya.dli;

import com.itextpdf.text.DocumentException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HttpDownloadTask implements InteractiveTask, ProgressListener {
    private final String parentUrl;
    private final String fileName;
    int progress = 0;
    private boolean cancel = false;
    private RunState state = RunState.Queued;
    private DownloadObserver observer;
    private Future<?> future;
    private final AppContext appContext;
    private LogWindow logWindow;
    private BufferedDownloader downloader = new BufferedDownloader();

    public HttpDownloadTask(String url, AppContext appContext, LogWindow logWindow) {
        this.appContext = appContext;
        this.logWindow = logWindow;
        this.parentUrl = url.substring(0, url.lastIndexOf("/")+1);
        this.fileName = url.substring(url.lastIndexOf("/")+1);
    }

    @Override
    public String getBarcode() {
        return fileName;
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public String getAttr(String attribute) {
        return "";
    }

    @Override
    public int getFailCount() {
        return 0;
    }

    @Override
    public void cancel() {
        cancel = true;
        state = RunState.Cancelled;
        notifyObserver();
    }

    @Override
    public void awaitTermination() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RunState getState() {
        return state;
    }

    @Override
    public void setState(RunState state) {
        this.state = state;
    }

    @Override
    public Future<?> getFuture() {
        return future;
    }

    @Override
    public int getPdfFailures() {
        return 0;
    }

    @Override
    public String getPdfName() {
        return "";
    }

    @Override
    public String getRootDirectory() {
        return "";
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void logs() {
        logWindow.setVisible(true);
    }

    @Override
    public void clean() {
        logWindow.dispose();
    }

    @Override
    public void addMetadata(String key, String value) {

    }

    @Override
    public void updateProgress(int progress) {
        this.progress = progress;
        notifyObserver();
    }

    @Override
    public void download() throws InterruptedException, IOException, CancelledExecutionException, ExecutionException, MetadataNotFound, DocumentException {
        try {
            state = RunState.Downloading;
            notifyObserver();
            downloader.setProgressListener(this);
            downloader.download(parentUrl, fileName, appContext.getRootDirectory(), appContext, false, logWindow, 1 << 30, false);
        } finally {
            state = RunState.Completed;
            notifyObserver();
        }
    }

    @Override
    public void beforeStart() {
        this.observer.started(this);
    }

    @Override
    public void afterComplete() {
        try {
            this.observer.completed(this);
        } catch (Exception e) {
            logWindow.log(Utils.getException(e));
        }
    }

    @Override
    public void withObserver(DownloadObserver observer) {
        this.observer = observer;
    }

    @Override
    public void notifyObserver() {
        observer.update(this);
    }

    @Override
    public void setFuture(Future<?> future) {
        this.future = future;
    }
}
