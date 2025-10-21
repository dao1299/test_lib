package com.vtnet.netat.db.exceptions.validation;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Exception thrown when data format doesn't match expected pattern.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class DataFormatException extends DataValidationException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;
    private final String expectedFormat;
    private final String actualValue;
    private final String formatExample;

    protected DataFormatException(Builder builder) {
        super(builder);
        this.fieldName = builder.fieldName;
        this.expectedFormat = builder.expectedFormat;
        this.actualValue = builder.actualValue;
        this.formatExample = builder.formatExample;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getExpectedFormat() {
        return expectedFormat;
    }

    public String getActualValue() {
        return actualValue;
    }

    public String getFormatExample() {
        return formatExample;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("Data format validation failed.\n\n");

        if (fieldName != null) {
            suggestion.append(String.format("Field: %s\n", fieldName));
        }

        if (expectedFormat != null) {
            suggestion.append(String.format("Expected Format: %s\n", expectedFormat));
        }

        if (formatExample != null) {
            suggestion.append(String.format("Example: %s\n", formatExample));
        }

        if (actualValue != null) {
            suggestion.append(String.format("Actual Value: %s\n\n", actualValue));
        } else {
            suggestion.append("\n");
        }

        suggestion.append("Common Format Issues:\n");
        suggestion.append("1. DATE FORMAT:\n");
        suggestion.append("   - Expected: yyyy-MM-dd (e.g., 2025-01-15)\n");
        suggestion.append("   - Or: dd/MM/yyyy (e.g., 15/01/2025)\n\n");

        suggestion.append("2. TIME FORMAT:\n");
        suggestion.append("   - Expected: HH:mm:ss (e.g., 14:30:00)\n");
        suggestion.append("   - Or: HH:mm (e.g., 14:30)\n\n");

        suggestion.append("3. DATETIME FORMAT:\n");
        suggestion.append("   - Expected: yyyy-MM-dd HH:mm:ss\n");
        suggestion.append("   - Example: 2025-01-15 14:30:00\n\n");

        suggestion.append("4. NUMBER FORMAT:\n");
        suggestion.append("   - Integer: no decimal point\n");
        suggestion.append("   - Decimal: use dot (.) not comma\n");
        suggestion.append("   - No thousand separators\n\n");

        suggestion.append("Resolution:\n");
        suggestion.append("1. Validate data format before database operation\n");
        suggestion.append("2. Use appropriate date/number formatters\n");
        suggestion.append("3. Standardize data format in test data files\n");
        suggestion.append("4. Add format validation in data preparation step\n");

        return suggestion.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends DataValidationException.Builder {
        private String fieldName;
        private String expectedFormat;
        private String actualValue;
        private String formatExample;

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

        public Builder expectedFormat(String expectedFormat) {
            this.expectedFormat = expectedFormat;
            if (expectedFormat != null) {
                addContext("expectedFormat", expectedFormat);
            }
            return this;
        }

        public Builder actualValue(String actualValue) {
            this.actualValue = actualValue;
            if (actualValue != null) {
                addContext("actualValue", actualValue);
            }
            return this;
        }

        public Builder formatExample(String formatExample) {
            this.formatExample = formatExample;
            return this;
        }

        @Override
        public DataFormatException build() {

                if (fieldName != null && expectedFormat != null) {
                    message(String.format("Invalid format for field '%s': expected %s",
                            fieldName, expectedFormat));
                } else {
                    message("Data format validation failed");
                }

            return new DataFormatException(this);
        }
    }
}