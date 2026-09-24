package com.example.urlshortener.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * In-process token-bucket rate limiter (per client key).
 * State is per-JVM; multi-instance deployments need Redis — same limitation as the
 * original Python implementation.
 */
public class TokenBucketLimiter {

    private final double capacity;
    private final double refillPerSecond;
    private final LongSupplier nanoClock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    private record Bucket(double tokens, double lastSeconds) {}

    public TokenBucketLimiter(int ratePerMinute) {
        this(ratePerMinute, null, System::nanoTime);
    }

    public TokenBucketLimiter(int ratePerMinute, Integer burst, LongSupplier nanoClock) {
        if (ratePerMinute <= 0) throw new IllegalArgumentException("ratePerMinute must be positive");
        this.capacity = burst != null ? burst : ratePerMinute;
        this.refillPerSecond = ratePerMinute / 60.0;
        this.nanoClock = nanoClock;
    }

    private double nowSeconds() { return nanoClock.getAsLong() / 1_000_000_000.0; }

    private double refilled(String key, double now) {
        Bucket b = buckets.getOrDefault(key, new Bucket(capacity, now));
        return Math.min(capacity, b.tokens() + (now - b.lastSeconds()) * refillPerSecond);
    }

    public boolean allow(String key) {
        double now = nowSeconds();
        synchronized (lock) {
            double tokens = refilled(key, now);
            if (tokens >= 1.0) { buckets.put(key, new Bucket(tokens - 1.0, now)); return true; }
            buckets.put(key, new Bucket(tokens, now));
            return false;
        }
    }

    public int retryAfter(String key) {
        double now = nowSeconds();
        double tokens;
        synchronized (lock) { tokens = refilled(key, now); }
        return Math.max(1, (int) Math.ceil((1.0 - tokens) / refillPerSecond));
    }
}
