package com.vtnet.netat.db.exceptions.query;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Exception thrown when query execution exceeds the configured timeout.
 * May be retryable depending on the query type and system load.
 *
 * <p>Common causes:
 * <ul>
 *   <li>Missing indexes causing full table scans</li>
 *   <li>Large result sets without pagination</li>
 *   <li>Lock contention with other queries</li>
 *   <li>Database server overload</li>
 *   <li>Network latency issues</li>
 * </ul>
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class QueryTimeoutException extends QueryExecutionException {

    private static final long serialVersionUID = 1L;

    private final long timeoutMillis;
    private final long executionTimeMillis;
    private final Long estimatedRows;

    protected QueryTimeoutException(Builder builder) {
        super(builder);
        this.timeoutMillis = builder.timeoutMillis;
        this.executionTimeMillis = builder.executionTimeMillis;
        this.estimatedRows = builder.estimatedRows;
    }

    /**
     * Gets the configured query timeout.
     *
     * @return timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Gets the actual execution time before timeout.
     *
     * @return execution time in milliseconds
     */
    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }

    /**
     * Gets the estimated number of rows being processed (if available).
     *
     * @return estimated rows, or null if unknown
     */
    public Long getEstimatedRows() {
        return estimatedRows;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append(String.format(
                "Query execution timed out after %d ms (timeout: %d ms)\n\n",
                executionTimeMillis, timeoutMillis));

        if (getQuery() != null) {
            suggestion.append("Query:\n");
            suggestion.append(getQuery()).append("\n\n");
        }

        if (estimatedRows != null) {
            suggestion.append(String.format("Estimated rows processed: %,d\n\n", estimatedRows));
        }

        suggestion.append("Performance Optimization Strategies:\n\n");

        suggestion.append("1. ADD INDEXES:\n");
        suggestion.append("   - Analyze query execution plan\n");
        suggestion.append("   - Add indexes on frequently filtered columns\n");
        suggestion.append("   - Consider composite indexes for multi-column filters\n\n");

        suggestion.append("2. OPTIMIZE QUERY:\n");
        suggestion.append("   - Avoid SELECT * (specify needed columns)\n");
        suggestion.append("   - Add WHERE clause to limit result set\n");
        suggestion.append("   - Use LIMIT/OFFSET for pagination\n");
        suggestion.append("   - Replace subqueries with JOINs where possible\n\n");

        suggestion.append("3. CHECK LOCKS:\n");
        suggestion.append("   - Query may be waiting for locks held by other transactions\n");
        suggestion.append("   - Review long-running transactions\n");
        suggestion.append("   - Consider reducing transaction scope\n\n");

        suggestion.append("4. INCREASE TIMEOUT (if query legitimately needs more time):\n");
        suggestion.append(String.format("   - Current: %d ms\n", timeoutMillis));
        suggestion.append(String.format("   - Suggested: %d ms\n\n", Math.min(timeoutMillis * 2, 300000)));

        suggestion.append("5. SYSTEM RESOURCES:\n");
        suggestion.append("   - Check database server CPU/Memory usage\n");
        suggestion.append("   - Review slow query logs\n");
        suggestion.append("   - Monitor concurrent connection count\n");

        return suggestion.toString();
    }

    /**
     * Creates a new builder for QueryTimeoutException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for QueryTimeoutException.
     */
    public static class Builder extends QueryExecutionException.Builder {
        private long timeoutMillis;
        private long executionTimeMillis;
        private Long estimatedRows;

        public Builder() {
            severity(ErrorSeverity.WARNING);
            retryable(true); // May succeed on retry with less load
        }

        public Builder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            addContext("timeoutMillis", timeoutMillis);
            return this;
        }

        public Builder executionTimeMillis(long executionTimeMillis) {
            this.executionTimeMillis = executionTimeMillis;
            addContext("executionTimeMillis", executionTimeMillis);
            return this;
        }

        public Builder estimatedRows(Long estimatedRows) {
            this.estimatedRows = estimatedRows;
            if (estimatedRows != null) {
                addContext("estimatedRows", estimatedRows);
            }
            return this;
        }

        @Override
        public QueryTimeoutException build() {
            return new QueryTimeoutException(this);
        }
    }
}