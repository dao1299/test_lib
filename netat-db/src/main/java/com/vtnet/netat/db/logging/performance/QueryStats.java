package com.vtnet.netat.db.logging.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for a specific query pattern.
 * Thread-safe accumulation of execution metrics.
 */
public class QueryStats {

    private final String normalizedQuery;
    private final AtomicInteger executionCount;
    private final AtomicLong totalDuration;
    private final AtomicLong minDuration;
    private final AtomicLong maxDuration;
    private final List<Long> recentDurations;  // Last 10 executions
    private final int maxRecentSize = 10;

    public QueryStats(String normalizedQuery) {
        this.normalizedQuery = normalizedQuery;
        this.executionCount = new AtomicInteger(0);
        this.totalDuration = new AtomicLong(0);
        this.minDuration = new AtomicLong(Long.MAX_VALUE);
        this.maxDuration = new AtomicLong(0);
        this.recentDurations = new ArrayList<>();
    }

    /**
     * Records a new execution.
     */
    public synchronized void addExecution(long durationMs) {
        executionCount.incrementAndGet();
        totalDuration.addAndGet(durationMs);

        // Update min/max
        updateMin(durationMs);
        updateMax(durationMs);

        // Update recent durations (keep last 10)
        recentDurations.add(durationMs);
        if (recentDurations.size() > maxRecentSize) {
            recentDurations.remove(0);
        }
    }

    private void updateMin(long duration) {
        long currentMin;
        do {
            currentMin = minDuration.get();
            if (duration >= currentMin) {
                break;
            }
        } while (!minDuration.compareAndSet(currentMin, duration));
    }

    private void updateMax(long duration) {
        long currentMax;
        do {
            currentMax = maxDuration.get();
            if (duration <= currentMax) {
                break;
            }
        } while (!maxDuration.compareAndSet(currentMax, duration));
    }

    // Getters

    public String getNormalizedQuery() {
        return normalizedQuery;
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public long getTotalDuration() {
        return totalDuration.get();
    }

    public long getMinDuration() {
        long min = minDuration.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxDuration() {
        return maxDuration.get();
    }

    public double getAverageDuration() {
        int count = executionCount.get();
        if (count == 0) {
            return 0;
        }
        return totalDuration.get() / (double) count;
    }

    public synchronized List<Long> getRecentDurations() {
        return new ArrayList<>(recentDurations);
    }

    /**
     * Checks if this query is consistently slow.
     */
    public synchronized boolean isConsistentlySlow(long thresholdMs) {
        if (recentDurations.isEmpty()) {
            return false;
        }

        // At least 80% of recent executions exceed threshold
        long slowCount = recentDurations.stream()
                .filter(d -> d > thresholdMs)
                .count();

        return (slowCount / (double) recentDurations.size()) >= 0.8;
    }

    @Override
    public String toString() {
        return String.format(
                "QueryStats{executions=%d, avg=%.1fms, min=%dms, max=%dms, query='%s'}",
                getExecutionCount(),
                getAverageDuration(),
                getMinDuration(),
                getMaxDuration(),
                normalizedQuery.length() > 50 ? normalizedQuery.substring(0, 50) + "..." : normalizedQuery
        );
    }
}