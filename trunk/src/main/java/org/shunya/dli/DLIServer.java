package org.shunya.dli;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class DLIServer {
    private final String rootUrl;
    private AtomicInteger downloadCount = new AtomicInteger(0);
    private AtomicInteger metadataCount = new AtomicInteger(0);
    private Date lastAccessed = new Date();
    private boolean active = true;


    public DLIServer(String rootUrl) {this.rootUrl = rootUrl;}

    public AtomicInteger getDownloadCount() {
        return downloadCount;
    }

    public void incrementDownloadCount() {
        this.downloadCount.incrementAndGet();
    }

    public AtomicInteger getMetadataCount() {
        return metadataCount;
    }

    public void resetDownloadCount(){
        this.downloadCount.set(0);
    }

    public void incrementMetadataCount() {
        this.metadataCount.incrementAndGet();
    }

    public void resetMetadataCount(){
        this.metadataCount.set(0);
    }

    public Date getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(Date lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    @Override
    public String toString() {
        return rootUrl;
    }
}
