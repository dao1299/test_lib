package com.vtnet.netat.db.exceptions.validation;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Exception thrown when encountering NULL where non-null value is expected.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class NullValueException extends DataValidationException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;
    private final String tableName;
    private final String context;

    protected NullValueException(Builder builder) {
        super(builder);
        this.fieldName = builder.fieldName;
        this.tableName = builder.tableName;
        this.context = builder.context;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getContext() {
        return context;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("Unexpected NULL value encountered.\n\n");

        if (fieldName != null) {
            suggestion.append(String.format("Field: %s\n", fieldName));
        }

        if (tableName != null) {
            suggestion.append(String.format("Table: %s\n", tableName));
        }

        if (context != null) {
            suggestion.append(String.format("Context: %s\n\n", context));
        } else {
            suggestion.append("\n");
        }

        suggestion.append("Possible Causes:\n");
        suggestion.append("1. Database returned NULL for expected non-null field\n");
        suggestion.append("2. Test data contains NULL values\n");
        suggestion.append("3. Optional field treated as required\n");
        suggestion.append("4. Data not properly initialized\n\n");

        suggestion.append("Resolution:\n");
        suggestion.append("1. Add NULL check before using value\n");
        suggestion.append("2. Provide default value if NULL\n");
        suggestion.append("3. Update database schema to enforce NOT NULL\n");
        suggestion.append("4. Verify test data completeness\n");
        suggestion.append("5. Make field optional if business logic allows\n");

        return suggestion.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends DataValidationException.Builder {
        private String fieldName;
        private String tableName;
        private String context;

        public Builder() {
            severity(ErrorSeverity.ERROR);
        }

        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            if (fieldName != null) {
                addContext("fieldName", fieldName);
            }
            return this;
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            if (tableName != null) {
                addContext("tableName", tableName);
            }
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        @Override
        public NullValueException build() {

                if (fieldName != null) {
                    message(String.format("NULL value encountered for field '%s'", fieldName));
                } else {
                    message("Unexpected NULL value encountered");
                }

            return new NullValueException(this);
        }
    }
}