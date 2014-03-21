package org.shunya.dli;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TokenBucketTest {
    private TokenBucket tokenBucket;

    @Test
    public void testSpeedLimit() throws InterruptedException {
        int downloadSpeedLimit = 500;
        tokenBucket = TokenBuckets.newFixedIntervalRefill(5000, downloadSpeedLimit / 10, 100, TimeUnit.MILLISECONDS);
        int threads = 2;
        int downloadPerThread = 100;
        int eachDownloadSize = 6;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            MyConsumer myConsumer = new MyConsumer(tokenBucket, startLatch, stopLatch, downloadPerThread, eachDownloadSize);
            myConsumer.start();
        }
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        stopLatch.await();
        long stopTime = System.currentTimeMillis();
        System.out.println("downloadSpeedLimit = " + downloadSpeedLimit);
        System.out.println("downloadPerThread = " + downloadPerThread);
        System.out.println("threads = " + threads);
        int totalDownload = downloadPerThread * eachDownloadSize * threads;
        System.out.println("totalDownload = " + totalDownload);
        double totalTime = (double) (stopTime - startTime) / 1000;
        System.out.println("totalTime = " + totalTime);
        double speed = totalDownload / totalTime;
        System.out.println("Actual Speed = " + speed + " , Expected Speed : " + downloadSpeedLimit);
    }

    @Test
    public void testSpeedLimitEachDownloadAboveLimit() throws InterruptedException {
        int downloadSpeedLimit = 600;
        tokenBucket = TokenBuckets.newFixedIntervalRefill(5000, downloadSpeedLimit / 10, 100, TimeUnit.MILLISECONDS);
        int threads = 4;
        int downloadPerThread = 5;
        int eachDownloadSize = 65;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            MyConsumer myConsumer = new MyConsumer(tokenBucket, startLatch, stopLatch, downloadPerThread, eachDownloadSize);
            myConsumer.start();
        }
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        stopLatch.await();
        long stopTime = System.currentTimeMillis();
        System.out.println("downloadSpeedLimit = " + downloadSpeedLimit);
        System.out.println("downloadPerThread = " + downloadPerThread);
        System.out.println("threads = " + threads);
        int totalDownload = downloadPerThread * eachDownloadSize * threads;
        System.out.println("totalDownload = " + totalDownload);
        double totalTime = (double) (stopTime - startTime) / 1000;
        System.out.println("totalTime = " + totalTime);
        double speed = totalDownload / totalTime;
        System.out.println("Actual Speed = " + speed + " , Expected Speed : " + downloadSpeedLimit);
    }

    class MyConsumer extends Thread {
        private final TokenBucket tokenBucket;
        private final CountDownLatch startLatch;
        private final CountDownLatch stopLatch;
        private final int totalDownload;
        private final int eachDownloadSize;

        MyConsumer(TokenBucket tokenBucket, CountDownLatch startLatch, CountDownLatch stopLatch, int totalDownload, int eachDownloadSize) {
            this.tokenBucket = tokenBucket;
            this.startLatch = startLatch;
            this.stopLatch = stopLatch;
            this.totalDownload = totalDownload;
            this.eachDownloadSize = eachDownloadSize;
        }

        @Override
        public void run() {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < totalDownload; i++) {
                try {
                    tokenBucket.consume(eachDownloadSize);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stopLatch.countDown();
        }
    }
}
