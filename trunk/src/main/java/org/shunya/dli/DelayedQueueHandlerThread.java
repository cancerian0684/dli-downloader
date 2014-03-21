package org.shunya.dli;

public class DelayedQueueHandlerThread<T> extends Thread {
    public interface CallBackHandler<T> {
        void process(T element);
    }

    private DelaySkipQueue<T> delaySkipQueue;
    private CallBackHandler callBackHandler;
    private final AppContext appContext;

    DelayedQueueHandlerThread(CallBackHandler callBackHandler, AppContext appContext) {
        this.callBackHandler = callBackHandler;
        this.appContext = appContext;
        this.delaySkipQueue = new DelaySkipQueue<>(appContext.getMaxKeyStrokeDelay());
        setPriority(Thread.MIN_PRIORITY);
        setDaemon(true);
        System.err.println("Delayed Queue Thread started.");
    }

    public void put(T element) {
        delaySkipQueue.put(element);
    }

    @Override
    public void run() {
        while (true) {
            T element;
            try {
                element = delaySkipQueue.take();
                callBackHandler.process(element);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException("DelayedQueueHandlerThread Interrupted!");
            }
        }
    }
}
