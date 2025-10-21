package com.vtnet.netat.db.exceptions.connection;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Exception thrown when connection attempt exceeds the configured timeout period.
 * This is typically a transient issue caused by network latency or server load.
 *
 * <p>Common causes:
 * <ul>
 *   <li>High network latency</li>
 *   <li>Database server is overloaded</li>
 *   <li>Connection timeout configured too low</li>
 *   <li>Firewall is silently dropping packets</li>
 * </ul>
 *
 * <p>This exception is marked as retryable since timeout issues are often temporary.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class ConnectionTimeoutException extends ConnectionException {

    private static final long serialVersionUID = 1L;

    private final long timeoutMillis;
    private final long attemptedDuration;

    protected ConnectionTimeoutException(Builder builder) {
        super(builder);
        this.timeoutMillis = builder.timeoutMillis;
        this.attemptedDuration = builder.attemptedDuration;
    }

    /**
     * Gets the configured connection timeout in milliseconds.
     *
     * @return timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Gets the actual duration of the failed connection attempt.
     *
     * @return duration in milliseconds
     */
    public long getAttemptedDuration() {
        return attemptedDuration;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append(String.format(
                "Connection attempt timed out after %d ms (configured timeout: %d ms).\n\n",
                attemptedDuration, timeoutMillis));

        suggestion.append("Recommended actions:\n");
        suggestion.append("1. Check network connectivity and latency\n");
        suggestion.append("2. Verify database server is responsive\n");
        suggestion.append("3. Consider increasing connection timeout\n");
        suggestion.append(String.format("   - Current: %d ms\n", timeoutMillis));
        suggestion.append(String.format("   - Suggested: %d ms\n", timeoutMillis * 2));
        suggestion.append("4. Check for firewall or proxy issues\n");
        suggestion.append("5. Verify database server is not overloaded\n");

        return suggestion.toString();
    }

    /**
     * Creates a new builder for ConnectionTimeoutException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ConnectionTimeoutException.
     */
    public static class Builder extends ConnectionException.Builder {
        private long timeoutMillis;
        private long attemptedDuration;

        public Builder() {
            severity(ErrorSeverity.WARNING);
            retryable(true);
        }

        public Builder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            addContext("timeoutMillis", timeoutMillis);
            return this;
        }

        public Builder attemptedDuration(long attemptedDuration) {
            this.attemptedDuration = attemptedDuration;
            addContext("attemptedDuration", attemptedDuration);
            return this;
        }

        @Override
        public ConnectionTimeoutException build() {
            return new ConnectionTimeoutException(this);
        }
    }
}