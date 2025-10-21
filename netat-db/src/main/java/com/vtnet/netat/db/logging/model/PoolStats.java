package com.vtnet.netat.db.logging.model;

public class PoolStats {
    private final int poolSize;
    private final int activeConnections;
    private final int idleConnections;
    private final int waitingThreads;

    public PoolStats(int poolSize, int activeConnections, int idleConnections, int waitingThreads) {
        this.poolSize = poolSize;
        this.activeConnections = activeConnections;
        this.idleConnections = idleConnections;
        this.waitingThreads = waitingThreads;
    }

    // Getters
    public int getPoolSize() { return poolSize; }
    public int getActiveConnections() { return activeConnections; }
    public int getIdleConnections() { return idleConnections; }
    public int getWaitingThreads() { return waitingThreads; }

    public double getUtilizationPercent() {
        return (activeConnections / (double) poolSize) * 100;
    }

    @Override
    public String toString() {
        return String.format("PoolStats{size=%d, active=%d, idle=%d, waiting=%d, utilization=%.1f%%}",
                poolSize, activeConnections, idleConnections, waitingThreads, getUtilizationPercent());
    }
}