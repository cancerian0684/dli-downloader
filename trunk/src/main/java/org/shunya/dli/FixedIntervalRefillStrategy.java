package org.shunya.dli;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A token bucket refill strategy that will provide N tokens for a token bucket to consume every T units of time.
 * The tokens are refilled in bursts rather than at a fixed rate.  This refill strategy will never allow more than
 * N tokens to be consumed during a window of time T.
 */
public class FixedIntervalRefillStrategy implements TokenBucket.RefillStrategy {
    private final long numTokens;
    private final long intervalInMillis;
    private AtomicLong nextRefillTime;

    /**
     * Create a FixedIntervalRefillStrategy.
     *
     * @param numTokens The number of tokens to add to the bucket every interval.
     * @param interval  How often to refill the bucket.
     * @param unit      Unit for interval.
     */
    public FixedIntervalRefillStrategy(long numTokens, long interval, TimeUnit unit) {
        this.numTokens = numTokens;
        this.intervalInMillis = unit.toMillis(interval);
        this.nextRefillTime = new AtomicLong(-1L);
    }

    public long refill() {
        final long now = System.currentTimeMillis();
        final long refillTime = nextRefillTime.get();
        if (now < refillTime) {
            return 0;
        }
        return nextRefillTime.compareAndSet(refillTime, now + intervalInMillis) ? numTokens : 0;
    }

    @Override
    public long getIntervalInMillis() {
        return intervalInMillis;
    }
}

