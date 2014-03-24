package org.shunya.dli;

import java.awt.*;

public class Tap {
    private TapListener listener;
    private boolean block = false;
    private boolean checkingForAvailability = false;
    private final InternetConnectionTester connectionTester;

    public Tap(AppContext appContext) {
        connectionTester = new InternetConnectionTester(appContext.getRetryIntervalInSeconds(), appContext.getUrlForInternetConnectionCheck());
    }

    public synchronized void await() throws InterruptedException {
        while (block) {
            wait();
        }
    }

    public synchronized void on() {
        block = false;
        if (listener != null) {
            listener.resume();
        }
        notifyAll();
    }

    public synchronized void off() {
        block = true;
        if (listener != null) {
            listener.pause();
        }
        notifyAll();
    }

    public boolean isOff(){
        return block;
    }

    public synchronized void offIfDisconnected() {
        if (!connectionTester.isConnected() && !isOff()) {
            listener.displayMsg("There is some problem with Internet Connection", TrayIcon.MessageType.WARNING);
            off();
            keepCheckingForInternetAvailability();
        }
    }

    public synchronized void pauseIfDisconnected() throws CancelledExecutionException {
        try {
            offIfDisconnected();
            await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new CancelledExecutionException("NIO Download Might have been Cancelled");
        }
    }

    public boolean checkConnected() {
        return connectionTester.isConnected();
    }

    private synchronized void keepCheckingForInternetAvailability() {
        if (!checkingForAvailability) {
            checkingForAvailability = true;
            Thread t = new Thread("checkForInternetAvailability") {
                @Override
                public void run() {
                    try {
                        connectionTester.awaitAvailability();
                        on();
                        listener.displayMsg("Internet Connection Restored", TrayIcon.MessageType.INFO);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        checkingForAvailability = false;
                    }
                }
            };
            t.start();
        }
    }

    public void setListener(TapListener listener) {
        this.listener = listener;
    }

    @Override
    public String toString() {
        return block ? "OFF" : "ON";
    }
}
