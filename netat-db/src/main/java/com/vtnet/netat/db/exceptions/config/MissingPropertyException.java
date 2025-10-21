package com.vtnet.netat.db.exceptions.config;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Exception thrown when a required configuration property is missing.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class MissingPropertyException extends ConfigurationException {

    private static final long serialVersionUID = 1L;

    private final String propertyName;
    private final String expectedFormat;
    private final String exampleValue;

    protected MissingPropertyException(Builder builder) {
        super(builder);
        this.propertyName = builder.propertyName;
        this.expectedFormat = builder.expectedFormat;
        this.exampleValue = builder.exampleValue;
    }

    /**
     * Gets the name of the missing property.
     *
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Gets the expected format of the property.
     *
     * @return expected format description, or null if not provided
     */
    public String getExpectedFormat() {
        return expectedFormat;
    }

    /**
     * Gets an example value for the property.
     *
     * @return example value, or null if not provided
     */
    public String getExampleValue() {
        return exampleValue;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append(String.format("Required property '%s' is missing from configuration.\n\n",
                propertyName));

        if (getProfileName() != null) {
            suggestion.append(String.format("Profile: %s\n\n", getProfileName()));
        }

        suggestion.append("Resolution:\n");
        suggestion.append(String.format("1. Add '%s' to your database profile JSON file\n", propertyName));

        if (expectedFormat != null) {
            suggestion.append(String.format("2. Expected format: %s\n", expectedFormat));
        }

        if (exampleValue != null) {
            suggestion.append(String.format("3. Example value: %s\n", exampleValue));
        }

        suggestion.append("\nExample profile structure:\n");
        suggestion.append("{\n");
        suggestion.append("  \"profileName\": \"my-database\",\n");
        suggestion.append("  \"jdbcUrl\": \"jdbc:mysql://localhost:3306/mydb\",\n");
        suggestion.append("  \"username\": \"user\",\n");
        suggestion.append("  \"password\": \"encrypted_password\",\n");
        suggestion.append(String.format("  \"%s\": ", propertyName));

        if (exampleValue != null) {
            if (exampleValue.matches("\\d+")) {
                suggestion.append(exampleValue);
            } else {
                suggestion.append("\"").append(exampleValue).append("\"");
            }
        } else {
            suggestion.append("\"your_value_here\"");
        }

        suggestion.append("\n}\n");

        return suggestion.toString();
    }

    /**
     * Creates a new builder for MissingPropertyException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for MissingPropertyException.
     */
    public static class Builder extends ConfigurationException.Builder {
        private String propertyName;
        private String expectedFormat;
        private String exampleValue;

        public Builder() {
            severity(ErrorSeverity.ERROR);
        }

        public Builder propertyName(String propertyName) {
            this.propertyName = propertyName;
            addContext("propertyName", propertyName);
            return this;
        }

        public Builder expectedFormat(String expectedFormat) {
            this.expectedFormat = expectedFormat;
            if (expectedFormat != null) {
                addContext("expectedFormat", expectedFormat);
            }
            return this;
        }

        public Builder exampleValue(String exampleValue) {
            this.exampleValue = exampleValue;
            if (exampleValue != null) {
                addContext("exampleValue", exampleValue);
            }
            return this;
        }

        @Override
        public MissingPropertyException build() {
            if (propertyName != null) {
                message(String.format("Required property '%s' is missing", propertyName));
            }
            return new MissingPropertyException(this);
        }
    }
}