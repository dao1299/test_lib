package com.vtnet.netat.db.exceptions.transaction;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Exception thrown when a transaction is forced to rollback.
 * This can happen due to various reasons including system errors,
 * constraint violations within transaction, or explicit rollback.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class TransactionRollbackException extends TransactionException {

    private static final long serialVersionUID = 1L;

    /**
     * Reason for transaction rollback.
     */
    public enum RollbackReason {
        CONSTRAINT_VIOLATION("Constraint Violation", "Database constraint was violated"),
        DEADLOCK("Deadlock Detected", "Transaction was victim of deadlock"),
        TIMEOUT("Timeout", "Transaction exceeded timeout limit"),
        SYSTEM_ERROR("System Error", "Database system error occurred"),
        EXPLICIT("Explicit Rollback", "Transaction was explicitly rolled back"),
        SERIALIZATION_FAILURE("Serialization Failure", "Concurrent modification detected"),
        UNKNOWN("Unknown", "Rollback reason unknown");

        private final String displayName;
        private final String description;

        RollbackReason(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    private final RollbackReason rollbackReason;
    private final boolean autoRollback;

    protected TransactionRollbackException(Builder builder) {
        super(builder);
        this.rollbackReason = builder.rollbackReason;
        this.autoRollback = builder.autoRollback;
    }

    /**
     * Gets the reason for rollback.
     *
     * @return rollback reason
     */
    public RollbackReason getRollbackReason() {
        return rollbackReason;
    }

    /**
     * Checks if this was an automatic rollback by the database.
     *
     * @return true if automatic rollback
     */
    public boolean isAutoRollback() {
        return autoRollback;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append(String.format("Transaction Rollback: %s\n\n",
                rollbackReason.getDisplayName()));
        suggestion.append(String.format("Description: %s\n",
                rollbackReason.getDescription()));
        suggestion.append(String.format("Auto-rollback: %s\n\n",
                autoRollback ? "Yes" : "No"));

        switch (rollbackReason) {
            case CONSTRAINT_VIOLATION:
                suggestion.append("The transaction was rolled back due to a constraint violation.\n");
                suggestion.append("Review the underlying constraint exception for details.\n");
                suggestion.append("Ensure data integrity before starting transaction.\n");
                break;

            case DEADLOCK:
                suggestion.append("The transaction was chosen as deadlock victim.\n");
                suggestion.append("This is retryable - consider implementing retry logic.\n");
                suggestion.append("Review lock ordering to prevent future deadlocks.\n");
                break;

            case TIMEOUT:
                suggestion.append("The transaction exceeded its timeout limit.\n");
                suggestion.append("Consider breaking into smaller transactions.\n");
                suggestion.append("Optimize queries within the transaction.\n");
                break;

            case SERIALIZATION_FAILURE:
                suggestion.append("Concurrent modification detected (isolation level conflict).\n");
                suggestion.append("This is retryable with optimistic locking.\n");
                suggestion.append("Consider using lower isolation level if acceptable.\n");
                break;

            case EXPLICIT:
                suggestion.append("The transaction was explicitly rolled back by code.\n");
                suggestion.append("Review the business logic that triggered rollback.\n");
                suggestion.append("Ensure proper error handling in transaction code.\n");
                break;

            case SYSTEM_ERROR:
                suggestion.append("A system-level error forced rollback.\n");
                suggestion.append("Check database logs for details.\n");
                suggestion.append("Verify database health and resources.\n");
                break;

            default:
                suggestion.append("Review the error message and database logs.\n");
                suggestion.append("Contact database administrator if issue persists.\n");
        }

        return suggestion.toString();
    }

    /**
     * Creates a new builder for TransactionRollbackException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TransactionRollbackException.
     */
    public static class Builder extends TransactionException.Builder {
        private RollbackReason rollbackReason = RollbackReason.UNKNOWN;
        private boolean autoRollback = true;

        public Builder() {
            severity(ErrorSeverity.ERROR);
        }

        public Builder rollbackReason(RollbackReason rollbackReason) {
            this.rollbackReason = rollbackReason;
            addContext("rollbackReason", rollbackReason.name());

            // Set retryable based on reason
            retryable(rollbackReason == RollbackReason.DEADLOCK ||
                    rollbackReason == RollbackReason.SERIALIZATION_FAILURE);

            return this;
        }

        public Builder autoRollback(boolean autoRollback) {
            this.autoRollback = autoRollback;
            addContext("autoRollback", autoRollback);
            return this;
        }

        @Override
        public TransactionRollbackException build() {
            return new TransactionRollbackException(this);
        }
    }
}