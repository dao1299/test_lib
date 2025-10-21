package com.vtnet.netat.db.logging.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

public class QueryExecutionLog {
    private final String logId;
    private final Instant timestamp;
    private final String profileName;
    private final String query;
    private final Object[] parameters;
    private final long durationMs;
    private final int rowsAffected;
    private final boolean success;
    private final String errorMessage;
    private final String threadName;

    // Constructor
    private QueryExecutionLog(Builder builder) {
        this.logId = builder.logId;
        this.timestamp = builder.timestamp;
        this.profileName = builder.profileName;
        this.query = builder.query;
        this.parameters = builder.parameters;
        this.durationMs = builder.durationMs;
        this.rowsAffected = builder.rowsAffected;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
        this.threadName = builder.threadName;
    }

    // Getters
    public String getLogId() { return logId; }
    public Instant getTimestamp() { return timestamp; }
    public String getProfileName() { return profileName; }
    public String getQuery() { return query; }
    public Object[] getParameters() { return parameters; }
    public long getDurationMs() { return durationMs; }
    public int getRowsAffected() { return rowsAffected; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public String getThreadName() { return threadName; }

    public boolean isSlowQuery(long thresholdMs) {
        return durationMs > thresholdMs;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String logId = UUID.randomUUID().toString();
        private Instant timestamp = Instant.now();
        private String profileName;
        private String query;
        private Object[] parameters;
        private long durationMs;
        private int rowsAffected;
        private boolean success = true;
        private String errorMessage;
        private String threadName = Thread.currentThread().getName();

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder parameters(Object... parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder rowsAffected(int rowsAffected) {
            this.rowsAffected = rowsAffected;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            this.success = false;
            return this;
        }

        public QueryExecutionLog build() {
            return new QueryExecutionLog(this);
        }
    }

    @Override
    public String toString() {
        return String.format("QueryExecutionLog{id=%s, profile=%s, duration=%dms, success=%s}",
                logId, profileName, durationMs, success);
    }
}