package com.vtnet.netat.db.logging.model;

import java.time.Instant;

public class ConnectionLifecycleLog {

    public enum Event {
        OPEN, CLOSE, TIMEOUT, REFUSED, POOL_EXHAUSTED
    }

    private final Instant timestamp;
    private final Event event;
    private final String profileName;
    private final String jdbcUrl;
    private final long durationMs;
    private final PoolStats poolStats;
    private final String errorMessage;

    private ConnectionLifecycleLog(Builder builder) {
        this.timestamp = builder.timestamp;
        this.event = builder.event;
        this.profileName = builder.profileName;
        this.jdbcUrl = builder.jdbcUrl;
        this.durationMs = builder.durationMs;
        this.poolStats = builder.poolStats;
        this.errorMessage = builder.errorMessage;
    }

    // Getters
    public Instant getTimestamp() { return timestamp; }
    public Event getEvent() { return event; }
    public String getProfileName() { return profileName; }
    public String getJdbcUrl() { return jdbcUrl; }
    public long getDurationMs() { return durationMs; }
    public PoolStats getPoolStats() { return poolStats; }
    public String getErrorMessage() { return errorMessage; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Instant timestamp = Instant.now();
        private Event event;
        private String profileName;
        private String jdbcUrl;
        private long durationMs;
        private PoolStats poolStats;
        private String errorMessage;

        public Builder event(Event event) {
            this.event = event;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder poolStats(PoolStats poolStats) {
            this.poolStats = poolStats;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ConnectionLifecycleLog build() {
            return new ConnectionLifecycleLog(this);
        }
    }
}