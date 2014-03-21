package org.shunya.dli;

public interface ProgressListener {
    boolean isCancelled();

    void updateProgress(int progress);
}
