package com.vtnet.netat.db.logging.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionLog {

    public enum Status {
        ACTIVE, COMMITTED, ROLLED_BACK
    }

    private final String transactionId;
    private final Instant startTime;
    private Instant endTime;
    private final String profileName;
    private final String isolationLevel;
    private Status status;
    private final List<String> operations;
    private String rollbackReason;

    private TransactionLog(Builder builder) {
        this.transactionId = builder.transactionId;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.profileName = builder.profileName;
        this.isolationLevel = builder.isolationLevel;
        this.status = builder.status;
        this.operations = new ArrayList<>(builder.operations);
        this.rollbackReason = builder.rollbackReason;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public String getProfileName() { return profileName; }
    public String getIsolationLevel() { return isolationLevel; }
    public Status getStatus() { return status; }
    public List<String> getOperations() { return new ArrayList<>(operations); }
    public String getRollbackReason() { return rollbackReason; }

    public long getDurationMs() {
        if (endTime == null) {
            return 0;
        }
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String transactionId = UUID.randomUUID().toString();
        private Instant startTime = Instant.now();
        private Instant endTime;
        private String profileName;
        private String isolationLevel;
        private Status status = Status.ACTIVE;
        private List<String> operations = new ArrayList<>();
        private String rollbackReason;

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public Builder isolationLevel(String isolationLevel) {
            this.isolationLevel = isolationLevel;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder addOperation(String operation) {
            this.operations.add(operation);
            return this;
        }

        public Builder rollbackReason(String rollbackReason) {
            this.rollbackReason = rollbackReason;
            return this;
        }

        public TransactionLog build() {
            return new TransactionLog(this);
        }
    }
}