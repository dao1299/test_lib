package com.vtnet.netat.db.exceptions.query;

import com.vtnet.netat.db.exceptions.DatabaseException;
import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Base exception for errors that occur during SQL query execution.
 * This includes syntax errors, timeout issues, and constraint violations.
 *
 * <p>Subclasses provide specific handling for different query execution failures.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class QueryExecutionException extends DatabaseException {

    private static final long serialVersionUID = 1L;

    protected QueryExecutionException(Builder builder) {
        super(builder);
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("Query execution failed. Review the query and error details above.\n\n");

        if (getQuery() != null) {
            suggestion.append("Failed Query:\n");
            suggestion.append(getQuery()).append("\n\n");
        }

        suggestion.append("General Troubleshooting:\n");
        suggestion.append("1. Verify SQL syntax is correct\n");
        suggestion.append("2. Check that all referenced tables/columns exist\n");
        suggestion.append("3. Ensure you have necessary permissions\n");
        suggestion.append("4. Review query parameters for correct data types\n");

        return suggestion.toString();
    }

    /**
     * Creates a new builder for QueryExecutionException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for QueryExecutionException.
     */
    public static class Builder extends DatabaseException.Builder<Builder> {

        public Builder() {
            severity(ErrorSeverity.ERROR);
            retryable(false); // Query errors usually require code fixes
        }

        @Override
        public QueryExecutionException build() {
            return new QueryExecutionException(this);
        }
    }
}