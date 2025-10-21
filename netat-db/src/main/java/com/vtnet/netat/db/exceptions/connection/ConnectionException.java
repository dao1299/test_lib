package com.vtnet.netat.db.exceptions.connection;


import com.vtnet.netat.db.exceptions.DatabaseException;
import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Base exception for all connection-related database errors.
 * Thrown when issues occur during connection establishment or maintenance.
 *
 * <p>Common scenarios:
 * <ul>
 *   <li>Database server is unavailable</li>
 *   <li>Network connectivity issues</li>
 *   <li>Authentication failures</li>
 *   <li>Connection pool problems</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * try {
 *     Connection conn = connectionManager.getConnection("profile");
 * } catch (ConnectionException e) {
 *     if (e.isRetryable()) {
 *         // Retry with exponential backoff
 *     } else {
 *         // Log and alert - configuration issue
 *     }
 * }
 * }</pre>
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class ConnectionException extends DatabaseException {

    private static final long serialVersionUID = 1L;

    protected ConnectionException(Builder builder) {
        super(builder);
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("Connection to database failed. Please check:\n");
        suggestion.append("1. Database service is running\n");
        suggestion.append("2. Network connectivity to database host\n");
        suggestion.append("3. Firewall rules allow connection\n");
        suggestion.append("4. Connection string (host, port) is correct\n");

        if (getJdbcUrl() != null) {
            suggestion.append(String.format("5. Verify connection details: %s\n", getJdbcUrl()));
        }

        if (isRetryable()) {
            suggestion.append("\nThis error may be transient. Retry is recommended.");
        }

        return suggestion.toString();
    }

    /**
     * Creates a new builder for ConnectionException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ConnectionException.
     */
    public static class Builder extends DatabaseException.Builder<Builder> {

        public Builder() {
            severity(ErrorSeverity.ERROR);
            retryable(true); // Connection errors are often transient
        }

        @Override
        public ConnectionException build() {
            return new ConnectionException(this);
        }
    }
}
