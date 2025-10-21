package com.vtnet.netat.db.exceptions.validation;

import com.vtnet.netat.db.exceptions.DatabaseException;
import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Base exception for data validation errors.
 * Thrown when data doesn't meet expected format or constraints before/after database operations.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class DataValidationException extends DatabaseException {

    private static final long serialVersionUID = 1L;

    protected DataValidationException(Builder builder) {
        super(builder);
    }

    @Override
    public String getSuggestion() {
        return "Validate data format and type before database operations.";
    }

    /**
     * Creates a new builder for DataValidationException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DataValidationException.
     */
    public static class Builder extends DatabaseException.Builder<Builder> {

        public Builder() {
            severity(ErrorSeverity.ERROR);
            retryable(false);
        }

        @Override
        public DataValidationException build() {
            return new DataValidationException(this);
        }
    }
}