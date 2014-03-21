package org.shunya.dli;

import java.util.concurrent.TimeUnit;

public class InternetConnectionTester {
    private final int retryIntervalInSeconds;

    private boolean connected = false;
    private final String address;

    public InternetConnectionTester(int retryIntervalInSeconds, String address) {
        this.retryIntervalInSeconds = retryIntervalInSeconds;
        this.address = address;
    }

    public void awaitAvailability() throws InterruptedException {
        isConnected();
        while (!connected) {
            TimeUnit.SECONDS.sleep(retryIntervalInSeconds);
            isConnected();
        }
    }

    public boolean isConnected() {
        connected = Utils.pingUrl(address, null);
        return connected;
    }
}
