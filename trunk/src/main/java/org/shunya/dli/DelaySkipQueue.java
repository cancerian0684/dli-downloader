package org.shunya.dli;

import java.util.concurrent.TimeUnit;

public class DelaySkipQueue<T> {
    private final int maxDelay;
    private final Latch latch;
    private volatile T element;
    private volatile long expiryTime;


    public DelaySkipQueue(int maxDelay) {
        this.maxDelay = maxDelay;
        this.latch = new Latch();
        this.expiryTime = System.currentTimeMillis() + maxDelay;
    }

    public void put(T element) {
        this.element = element;
        if (element != null)
            latch.release();
    }

    public T take() throws InterruptedException {
        latch.await();
        long currentTime = System.currentTimeMillis();
        if (expiryTime > currentTime) {
            Thread.sleep(expiryTime - currentTime);
        }
        expiryTime = System.currentTimeMillis() + maxDelay;
        latch.lock();
        return element;
    }

    public static void main(String[] args) {
        final DelaySkipQueue<String> pdq = new DelaySkipQueue<String>(400);
        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (i < 5) {
                    pdq.put("munish" + i++);
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        producer.start();

        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        System.out.println(pdq.take());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        consumer.start();
    }

    static class Latch {
        boolean state = false;

        public synchronized void release() {
            state = true;
            notifyAll();
        }

        public synchronized void await() throws InterruptedException {
            while (!state)
                wait();
        }

        public synchronized void lock() {
            state = false;
        }
    }
}
