package com.vtnet.netat.db.exceptions.connection;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Exception thrown when the connection pool has no available connections
 * and cannot create new ones (pool is at maximum size).
 *
 * <p>Common causes:
 * <ul>
 *   <li>Pool size configured too small for workload</li>
 *   <li>Connection leaks (not properly closing connections)</li>
 *   <li>Long-running queries holding connections</li>
 *   <li>Unexpectedly high concurrent load</li>
 * </ul>
 *
 * <p>This exception is marked as retryable since waiting briefly may allow
 * connections to be returned to the pool.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class ConnectionPoolExhaustedException extends ConnectionException {

    private static final long serialVersionUID = 1L;

    private final int poolSize;
    private final int activeConnections;
    private final int idleConnections;
    private final int waitingThreads;
    private final long waitTimeoutMillis;

    protected ConnectionPoolExhaustedException(Builder builder) {
        super(builder);
        this.poolSize = builder.poolSize;
        this.activeConnections = builder.activeConnections;
        this.idleConnections = builder.idleConnections;
        this.waitingThreads = builder.waitingThreads;
        this.waitTimeoutMillis = builder.waitTimeoutMillis;
    }

    /**
     * Gets the maximum pool size.
     *
     * @return pool size
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Gets the number of active (in-use) connections.
     *
     * @return active connection count
     */
    public int getActiveConnections() {
        return activeConnections;
    }

    /**
     * Gets the number of idle (available) connections.
     *
     * @return idle connection count
     */
    public int getIdleConnections() {
        return idleConnections;
    }

    /**
     * Gets the number of threads waiting for connections.
     *
     * @return waiting thread count
     */
    public int getWaitingThreads() {
        return waitingThreads;
    }

    /**
     * Gets the configured wait timeout in milliseconds.
     *
     * @return timeout in milliseconds
     */
    public long getWaitTimeoutMillis() {
        return waitTimeoutMillis;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("Connection pool exhausted. No connections available.\n\n");

        suggestion.append("Current Pool Statistics:\n");
        suggestion.append(String.format("  - Pool Size: %d\n", poolSize));
        suggestion.append(String.format("  - Active: %d\n", activeConnections));
        suggestion.append(String.format("  - Idle: %d\n", idleConnections));
        suggestion.append(String.format("  - Waiting Threads: %d\n", waitingThreads));
        suggestion.append(String.format("  - Wait Timeout: %d ms\n\n", waitTimeoutMillis));

        suggestion.append("Recommended Actions:\n");

        // Check for potential connection leaks
        if (activeConnections >= poolSize && idleConnections == 0) {
            suggestion.append("⚠️  All connections are active - possible connection leak!\n");
            suggestion.append("1. CHECK FOR CONNECTION LEAKS:\n");
            suggestion.append("   - Ensure all keywords properly close connections\n");
            suggestion.append("   - Use try-with-resources or finally blocks\n");
            suggestion.append("   - Review recent code changes\n\n");
        }

        // Check for pool size issues
        if (waitingThreads > poolSize) {
            suggestion.append("⚠️  High contention detected (waiting threads > pool size)\n");
            suggestion.append("2. INCREASE POOL SIZE:\n");
            suggestion.append(String.format("   - Current: %d\n", poolSize));
            suggestion.append(String.format("   - Suggested: %d (based on waiting threads)\n\n",
                    Math.max(poolSize * 2, waitingThreads)));
        }

        suggestion.append("3. OPTIMIZE QUERY PERFORMANCE:\n");
        suggestion.append("   - Review slow queries holding connections\n");
        suggestion.append("   - Add appropriate indexes\n");
        suggestion.append("   - Consider query timeout settings\n\n");

        suggestion.append("4. REDUCE CONCURRENT LOAD:\n");
        suggestion.append("   - Reduce parallel test execution\n");
        suggestion.append("   - Implement throttling or rate limiting\n");
        suggestion.append("   - Consider batch processing\n");

        return suggestion.toString();
    }

    /**
     * Creates a new builder for ConnectionPoolExhaustedException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ConnectionPoolExhaustedException.
     */
    public static class Builder extends ConnectionException.Builder {
        private int poolSize;
        private int activeConnections;
        private int idleConnections;
        private int waitingThreads;
        private long waitTimeoutMillis;

        public Builder() {
            severity(ErrorSeverity.WARNING);
            retryable(true); // Can retry after brief wait
        }

        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            addContext("poolSize", poolSize);
            return this;
        }

        public Builder activeConnections(int activeConnections) {
            this.activeConnections = activeConnections;
            addContext("activeConnections", activeConnections);
            return this;
        }

        public Builder idleConnections(int idleConnections) {
            this.idleConnections = idleConnections;
            addContext("idleConnections", idleConnections);
            return this;
        }

        public Builder waitingThreads(int waitingThreads) {
            this.waitingThreads = waitingThreads;
            addContext("waitingThreads", waitingThreads);
            return this;
        }

        public Builder waitTimeoutMillis(long waitTimeoutMillis) {
            this.waitTimeoutMillis = waitTimeoutMillis;
            addContext("waitTimeoutMillis", waitTimeoutMillis);
            return this;
        }

        @Override
        public ConnectionPoolExhaustedException build() {
            return new ConnectionPoolExhaustedException(this);
        }
    }
}