package com.vtnet.netat.db.exceptions;

/**
 * Generic database exception used when specific exception type cannot be determined.
 * This is a concrete implementation of DatabaseException for cases where
 * SQL State or error codes don't map to any specific exception type.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class GenericDatabaseException extends DatabaseException {

    private static final long serialVersionUID = 1L;

    protected GenericDatabaseException(Builder builder) {
        super(builder);
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("A database error occurred that could not be classified into a specific category.\n\n");

        if (getSqlState() != null) {
            suggestion.append(String.format("SQL State: %s\n", getSqlState()));
        }

        if (getVendorErrorCode() != 0) {
            suggestion.append(String.format("Vendor Error Code: %d\n", getVendorErrorCode()));
        }

        suggestion.append("\nTroubleshooting Steps:\n");
        suggestion.append("1. Review the error message and SQL State code\n");
        suggestion.append("2. Check database vendor documentation for error code details\n");
        suggestion.append("3. Review the query that caused the error\n");
        suggestion.append("4. Check database logs for additional context\n");
        suggestion.append("5. Verify database connectivity and permissions\n\n");

        suggestion.append("If this error persists, please report it to the framework maintainers\n");
        suggestion.append("so that proper exception mapping can be added.\n");

        return suggestion.toString();
    }

    /**
     * Creates a new builder for GenericDatabaseException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for GenericDatabaseException.
     */
    public static class Builder extends DatabaseException.Builder<Builder> {

        public Builder() {
            severity(ErrorSeverity.ERROR);
            retryable(false);
        }

        @Override
        public GenericDatabaseException build() {
            return new GenericDatabaseException(this);
        }
    }
}