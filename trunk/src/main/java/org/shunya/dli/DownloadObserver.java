package org.shunya.dli;

public interface DownloadObserver {
    void started(InteractiveTask task);
    void update(InteractiveTask task);
    void completed(InteractiveTask task);
}
