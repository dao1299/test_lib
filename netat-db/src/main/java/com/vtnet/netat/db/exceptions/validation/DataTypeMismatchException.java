package com.vtnet.netat.db.exceptions.validation;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Exception thrown when data type doesn't match expected type.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class DataTypeMismatchException extends DataValidationException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;
    private final String expectedType;
    private final String actualType;
    private final Object actualValue;

    protected DataTypeMismatchException(Builder builder) {
        super(builder);
        this.fieldName = builder.fieldName;
        this.expectedType = builder.expectedType;
        this.actualType = builder.actualType;
        this.actualValue = builder.actualValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public String getActualType() {
        return actualType;
    }

    public Object getActualValue() {
        return actualValue;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("Data type mismatch detected.\n\n");

        if (fieldName != null) {
            suggestion.append(String.format("Field: %s\n", fieldName));
        }

        suggestion.append(String.format("Expected Type: %s\n", expectedType));
        suggestion.append(String.format("Actual Type: %s\n", actualType));

        if (actualValue != null) {
            suggestion.append(String.format("Actual Value: %s\n\n", actualValue));
        } else {
            suggestion.append("\n");
        }

        suggestion.append("Common Type Conversions:\n");
        suggestion.append("  String → Integer: Integer.parseInt(str)\n");
        suggestion.append("  String → Double: Double.parseDouble(str)\n");
        suggestion.append("  String → Date: SimpleDateFormat.parse(str)\n");
        suggestion.append("  Integer → String: String.valueOf(int)\n\n");

        suggestion.append("Resolution:\n");
        suggestion.append("1. Convert data to expected type before operation\n");
        suggestion.append("2. Update database schema if type should be different\n");
        suggestion.append("3. Use appropriate data type in test data files\n");

        return suggestion.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends DataValidationException.Builder {
        private String fieldName;
        private String expectedType;
        private String actualType;
        private Object actualValue;

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

        public Builder expectedType(String expectedType) {
            this.expectedType = expectedType;
            addContext("expectedType", expectedType);
            return this;
        }

        public Builder actualType(String actualType) {
            this.actualType = actualType;
            addContext("actualType", actualType);
            return this;
        }

        public Builder actualValue(Object actualValue) {
            this.actualValue = actualValue;
            if (actualValue != null) {
                addContext("actualValue", actualValue.toString());
            }
            return this;
        }

        @Override
        public DataTypeMismatchException build() {
            if (fieldName != null) {
                message(String.format("Type mismatch for field '%s': expected %s but got %s",
                        fieldName, expectedType, actualType));
            }
            return new DataTypeMismatchException(this);
        }
    }
}