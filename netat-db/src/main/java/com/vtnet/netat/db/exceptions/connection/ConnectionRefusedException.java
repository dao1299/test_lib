package com.vtnet.netat.db.exceptions.connection;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

public class ConnectionRefusedException extends ConnectionException {

    private static final long serialVersionUID = 1L;

    private final String host;
    private final int port;
    private final String refusalReason;

    protected ConnectionRefusedException(Builder builder) {
        super(builder);
        this.host = builder.host;
        this.port = builder.port;
        this.refusalReason = builder.refusalReason;
    }

    /**
     * Gets the database host that refused connection.
     *
     * @return host address
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port that refused connection.
     *
     * @return port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the reason for refusal if available.
     *
     * @return refusal reason, or null if unknown
     */
    public String getRefusalReason() {
        return refusalReason;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append(String.format(
                "Connection to %s:%d was refused by the server.\n\n",
                host != null ? host : "unknown", port));

        if (refusalReason != null) {
            suggestion.append(String.format("Reason: %s\n\n", refusalReason));
        }

        suggestion.append("Immediate checks:\n");
        suggestion.append("1. Verify database service is running:\n");
        suggestion.append(String.format("   - Host: %s\n", host));
        suggestion.append(String.format("   - Port: %d\n", port));
        suggestion.append("2. Check connection string in database profile\n");
        suggestion.append("3. Verify firewall allows connections from this IP\n");
        suggestion.append("4. Test connection with database client tools\n");

        if (refusalReason != null && refusalReason.toLowerCase().contains("auth")) {
            suggestion.append("\n Authentication-related refusal detected:\n");
            suggestion.append("5. Verify username and password are correct\n");
            suggestion.append("6. Check if user has permission to access database\n");
            suggestion.append("7. Verify password hasn't expired\n");
        }

        if (refusalReason != null && refusalReason.toLowerCase().contains("max")) {
            suggestion.append("\n Max connections issue detected:\n");
            suggestion.append("5. Database may have reached max connections limit\n");
            suggestion.append("6. Check and close idle connections\n");
            suggestion.append("7. Consider increasing max_connections setting\n");
        }

        return suggestion.toString();
    }

    /**
     * Creates a new builder for ConnectionRefusedException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ConnectionRefusedException.
     */
    public static class Builder extends ConnectionException.Builder {
        private String host;
        private int port;
        private String refusalReason;

        public Builder() {
            severity(ErrorSeverity.ERROR);
            retryable(false); // Configuration issues don't auto-resolve
        }

        public Builder host(String host) {
            this.host = host;
            addContext("host", host);
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            addContext("port", port);
            return this;
        }

        public Builder refusalReason(String refusalReason) {
            this.refusalReason = refusalReason;
            addContext("refusalReason", refusalReason);
            return this;
        }

        @Override
        public ConnectionRefusedException build() {
            return new ConnectionRefusedException(this);
        }
    }
}