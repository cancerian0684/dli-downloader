package org.shunya.dli;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A token bucket implementation used for rate limiting access to a portion of code.  This implementation is that of a
 * leaky bucket in the sense that it has a finite capacity and any added tokens that would exceed this capacity will
 * "overflow" out of the bucket and are lost forever.
 * <p/>
 * In this implementation the rules for refilling the bucket are encapsulated in a provided {@code RefillStrategy}
 * instance.  Prior to attempting to consume any tokens the refill strategy will be consulted to see how many tokens
 * should be added to the bucket.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Token_bucket">Token Bucket on Wikipedia</a>
 * @see <a href="http://en.wikipedia.org/wiki/Leaky_bucket">Leaky Bucket on Wikipedia</a>
 */
public class TokenBucket {
    private final RefillStrategy refillStrategy;
    private final long capacity;
    private AtomicLong size;

    public TokenBucket(long capacity, RefillStrategy refillStrategy) {
        this.refillStrategy = refillStrategy;
        this.capacity = capacity;
        this.size = new AtomicLong(0L);
    }

    /**
     * consumes a specified number of tokens from the bucket.  If the tokens are not available then sufficient time is spent to refill the tokens.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     */
    public void consume(long numTokens) throws InterruptedException {
        if (numTokens < 0)
            throw new RuntimeException("Number of tokens to consume must be positive");
        if (numTokens >= capacity)
            throw new RuntimeException("Number of tokens to consume must be less than the capacity of the bucket");
        long newTokens = Math.max(0, refillStrategy.refill());
        while (!Thread.currentThread().isInterrupted()) {
            long existingSize = size.get();
            long newValue = Math.max(0, Math.min(existingSize + newTokens, capacity));
            if (numTokens <= newValue) {
                newValue -= numTokens;
                if (size.compareAndSet(existingSize, newValue))
                    break;
            } else {
                if (existingSize + newTokens <= capacity)
                    size.addAndGet(newTokens);
                else
                    size.addAndGet(capacity - existingSize);
                Thread.sleep(refillStrategy.getIntervalInMillis());
                newTokens = Math.max(0, refillStrategy.refill());
            }
        }
    }

    public static interface RefillStrategy {
        long refill();

        long getIntervalInMillis();
    }

    @Override
    public String toString() {
        return "Capacity : " + capacity + ", Size : " + size;
    }
}
