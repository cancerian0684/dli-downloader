package org.shunya.dli;

import com.itextpdf.text.DocumentException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface InteractiveTask {
    String getBarcode();

    default String getSummary() {return getBarcode();}

    default String getLanguage() {return "";}

    int getProgress();

    void cancel();

    void awaitTermination();

    RunState getState();

    void setState(RunState state);

    Future<?> getFuture();

    String getPdfName();

    String getRootDirectory();

    boolean isCancelled();

    void logs();

    void clean();

    void addMetadata(String key, String value);

    void download() throws InterruptedException, IOException, CancelledExecutionException, ExecutionException, MetadataNotFound, DocumentException;

    void beforeStart();

    void afterComplete();

    void withObserver(DownloadObserver observer);

    void notifyObserver();

    void setFuture(Future<?> future);
}
