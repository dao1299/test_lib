package com.vtnet.netat.db.exceptions.query;

import com.vtnet.netat.db.exceptions.ErrorSeverity;
import java.sql.SQLException;

public class ConstraintViolationException extends QueryExecutionException {

    private static final long serialVersionUID = 1L;

    public enum ConstraintType {
        PRIMARY_KEY("Primary Key", "Duplicate primary key value"),
        FOREIGN_KEY("Foreign Key", "Referenced row doesn't exist or deletion restricted"),
        UNIQUE("Unique Constraint", "Duplicate value in unique column"),
        NOT_NULL("Not Null Constraint", "NULL value not allowed"),
        CHECK("Check Constraint", "Value doesn't meet check condition"),
        UNKNOWN("Unknown Constraint", "Constraint violation detected");

        private final String displayName;
        private final String description;

        ConstraintType(String displayName, String description) {
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

    private final ConstraintType constraintType;
    private final String constraintName;
    private final String tableName;
    private final String columnName;
    private final Object violatingValue;

    protected ConstraintViolationException(Builder builder) {
        super(builder);
        this.constraintType = builder.constraintType;
        this.constraintName = builder.constraintName;
        this.tableName = builder.tableName;
        this.columnName = builder.columnName;
        this.violatingValue = builder.violatingValue;
    }

    public ConstraintType getConstraintType() {
        return constraintType;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public Object getViolatingValue() {
        return violatingValue;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append(String.format("Database Constraint Violation: %s\n\n",
                constraintType.getDisplayName()));

        suggestion.append("Constraint Details:\n");
        if (constraintName != null) {
            suggestion.append(String.format("  - Constraint: %s\n", constraintName));
        }
        if (tableName != null) {
            suggestion.append(String.format("  - Table: %s\n", tableName));
        }
        if (columnName != null) {
            suggestion.append(String.format("  - Column: %s\n", columnName));
        }
        if (violatingValue != null) {
            suggestion.append(String.format("  - Value: %s\n", violatingValue));
        }
        suggestion.append("\n");

        suggestion.append(String.format("Description: %s\n\n", constraintType.getDescription()));

        switch (constraintType) {
            case PRIMARY_KEY:
            case UNIQUE:
                suggestion.append("Resolution for Duplicate Value:\n");
                suggestion.append("1. Check if record already exists before inserting\n");
                suggestion.append("2. Use INSERT ... ON DUPLICATE KEY UPDATE (MySQL)\n");
                suggestion.append("3. Use INSERT ... ON CONFLICT DO UPDATE (PostgreSQL)\n");
                suggestion.append("4. Generate unique values (UUID, sequence, timestamp)\n");
                suggestion.append("5. Review test data for duplicates\n");
                break;

            case FOREIGN_KEY:
                suggestion.append("Resolution for Foreign Key Violation:\n");
                suggestion.append("1. Ensure parent record exists before inserting child\n");
                suggestion.append("2. Check for typos in foreign key values\n");
                suggestion.append("3. Review deletion/update cascade rules\n");
                suggestion.append("4. Insert data in correct order (parents before children)\n");
                suggestion.append("5. Use transactions to ensure referential integrity\n");
                break;

            case NOT_NULL:
                suggestion.append("Resolution for NULL Value:\n");
                suggestion.append("1. Provide a value for required column\n");
                suggestion.append("2. Use default value in table definition\n");
                suggestion.append("3. Make column nullable if business logic allows\n");
                suggestion.append("4. Review data source for missing values\n");
                break;

            case CHECK:
                suggestion.append("Resolution for Check Constraint:\n");
                suggestion.append("1. Review check constraint condition\n");
                suggestion.append("2. Ensure value meets business rules\n");
                suggestion.append("3. Validate data before database operation\n");
                suggestion.append("4. Update constraint if rules changed\n");
                break;

            default:
                suggestion.append("General Resolution Steps:\n");
                suggestion.append("1. Review database schema and constraints\n");
                suggestion.append("2. Validate data before insertion/update\n");
                suggestion.append("3. Check for data integrity issues\n");
                suggestion.append("4. Review error message for specific guidance\n");
        }

        if (getQuery() != null) {
            suggestion.append("\nFailed Query:\n");
            suggestion.append(getQuery()).append("\n");
        }

        return suggestion.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends QueryExecutionException.Builder {
        private ConstraintType constraintType = ConstraintType.UNKNOWN;
        private String constraintName;
        private String tableName;
        private String columnName;
        private Object violatingValue;
        private String cachedMessage; // ✅ Cache message locally

        public Builder() {
            severity(ErrorSeverity.ERROR);
            retryable(false);
        }

        // ✅ Override to cache message
        @Override
        public Builder message(String message) {
            this.cachedMessage = message;
            super.message(message);
            return this;
        }

        // ✅ Override to cache message from SQLException
        @Override
        public Builder fromSQLException(SQLException e) {
            super.fromSQLException(e);
            if (e != null) {
                this.cachedMessage = e.getMessage();
            }
            return this;
        }

        public Builder constraintType(ConstraintType constraintType) {
            this.constraintType = constraintType;
            addContext("constraintType", constraintType.name());
            return this;
        }

        public Builder constraintName(String constraintName) {
            this.constraintName = constraintName;
            if (constraintName != null) {
                addContext("constraintName", constraintName);
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

        public Builder columnName(String columnName) {
            this.columnName = columnName;
            if (columnName != null) {
                addContext("columnName", columnName);
            }
            return this;
        }

        public Builder violatingValue(Object violatingValue) {
            this.violatingValue = violatingValue;
            if (violatingValue != null) {
                addContext("violatingValue", violatingValue.toString());
            }
            return this;
        }

        /**
         * Attempts to detect constraint type from error message.
         *
         * @return this builder
         */
        public Builder detectConstraintType() {
            // ✅ FIX: Use cached message
            String msg = cachedMessage != null ? cachedMessage.toLowerCase() : "";

            if (msg.contains("primary key") || msg.contains("duplicate entry")) {
                constraintType(ConstraintType.PRIMARY_KEY);
            } else if (msg.contains("foreign key") || msg.contains("cannot add or update child row")) {
                constraintType(ConstraintType.FOREIGN_KEY);
            } else if (msg.contains("unique") || msg.contains("duplicate key")) {
                constraintType(ConstraintType.UNIQUE);
            } else if (msg.contains("not null") || msg.contains("cannot be null")) {
                constraintType(ConstraintType.NOT_NULL);
            } else if (msg.contains("check constraint")) {
                constraintType(ConstraintType.CHECK);
            }

            return this;
        }

        @Override
        public ConstraintViolationException build() {
            return new ConstraintViolationException(this);
        }
    }
}