package com.vtnet.netat.db.exceptions.transaction;

import com.vtnet.netat.db.exceptions.DatabaseException;
import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Base exception for transaction-related errors.
 * Thrown during transaction lifecycle operations (begin, commit, rollback).
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class TransactionException extends DatabaseException {

    private static final long serialVersionUID = 1L;

    protected TransactionException(Builder builder) {
        super(builder);
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("Transaction operation failed.\n\n");
        suggestion.append("General Transaction Best Practices:\n");
        suggestion.append("1. Keep transactions as short as possible\n");
        suggestion.append("2. Avoid user interaction within transactions\n");
        suggestion.append("3. Always handle commit/rollback in try-finally\n");
        suggestion.append("4. Set appropriate isolation levels\n");
        suggestion.append("5. Monitor transaction duration\n");

        return suggestion.toString();
    }

    /**
     * Creates a new builder for TransactionException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TransactionException.
     */
    public static class Builder extends DatabaseException.Builder<Builder> {

        public Builder() {
            severity(ErrorSeverity.ERROR);
            retryable(false);
        }

        @Override
        public TransactionException build() {
            return new TransactionException(this);
        }
    }
}