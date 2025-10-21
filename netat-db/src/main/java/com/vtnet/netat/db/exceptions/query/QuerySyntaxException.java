package com.vtnet.netat.db.exceptions.query;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exception thrown when SQL query has syntax errors.
 * This is never retryable as it requires code correction.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class QuerySyntaxException extends QueryExecutionException {

    private static final long serialVersionUID = 1L;

    // Pattern to extract error position from common database error messages
    private static final Pattern POSITION_PATTERN = Pattern.compile(
            "(?:at position|near|column|line)\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    private final Integer errorPosition;
    private final Integer errorLine;
    private final Integer errorColumn;
    private final String nearText;

    protected QuerySyntaxException(Builder builder) {
        super(builder);
        this.errorPosition = builder.errorPosition;
        this.errorLine = builder.errorLine;
        this.errorColumn = builder.errorColumn;
        this.nearText = builder.nearText;
    }

    public Integer getErrorPosition() {
        return errorPosition;
    }

    public Integer getErrorLine() {
        return errorLine;
    }

    public Integer getErrorColumn() {
        return errorColumn;
    }

    public String getNearText() {
        return nearText;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("SQL Syntax Error Detected\n\n");

        if (getQuery() != null) {
            suggestion.append("Query:\n");
            suggestion.append(highlightError(getQuery()));
            suggestion.append("\n\n");
        }

        if (nearText != null) {
            suggestion.append(String.format("Error near: '%s'\n\n", nearText));
        }

        if (errorLine != null && errorColumn != null) {
            suggestion.append(String.format("Error at Line %d, Column %d\n\n", errorLine, errorColumn));
        } else if (errorPosition != null) {
            suggestion.append(String.format("Error at Position %d\n\n", errorPosition));
        }

        suggestion.append("Common Syntax Issues:\n");
        suggestion.append("1. Missing or extra commas in SELECT/INSERT lists\n");
        suggestion.append("2. Unclosed quotes or parentheses\n");
        suggestion.append("3. Reserved keyword used as identifier (wrap in quotes)\n");
        suggestion.append("4. Wrong SQL dialect (e.g., MySQL vs PostgreSQL syntax)\n");
        suggestion.append("5. Typo in SQL keyword (e.g., 'SELET' instead of 'SELECT')\n");
        suggestion.append("6. Missing JOIN condition in multi-table query\n\n");

        suggestion.append("Debugging Steps:\n");
        suggestion.append("1. Copy query to database IDE (e.g., DBeaver, SQL Developer)\n");
        suggestion.append("2. Use syntax highlighting to spot issues\n");
        suggestion.append("3. Simplify query to isolate the problem\n");
        suggestion.append("4. Check database documentation for correct syntax\n");

        return suggestion.toString();
    }

    /**
     * Attempts to highlight the error location in the query.
     */
    private String highlightError(String query) {
        if (errorPosition == null || errorPosition >= query.length()) {
            return query;
        }

        try {
            int start = Math.max(0, errorPosition - 20);
            int end = Math.min(query.length(), errorPosition + 20);

            String before = query.substring(start, errorPosition);
            String after = query.substring(errorPosition, end);

            return (start > 0 ? "..." : "") + before + " >>> ERROR HERE <<< " + after +
                    (end < query.length() ? "..." : "");
        } catch (Exception e) {
            return query;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for QuerySyntaxException.
     */
    public static class Builder extends QueryExecutionException.Builder {
        private Integer errorPosition;
        private Integer errorLine;
        private Integer errorColumn;
        private String nearText;
        private String cachedMessage;

        public Builder() {
            severity(ErrorSeverity.ERROR);
            retryable(false);
        }

        @Override
        public Builder message(String message) {
            this.cachedMessage = message;
            super.message(message);
            return this;
        }

        @Override
        public Builder fromSQLException(SQLException e) {
            super.fromSQLException(e);
            if (e != null) {
                this.cachedMessage = e.getMessage();
            }
            return this;
        }

        public Builder errorPosition(Integer errorPosition) {
            this.errorPosition = errorPosition;
            if (errorPosition != null) {
                addContext("errorPosition", errorPosition);
            }
            return this;
        }

        public Builder errorLine(Integer errorLine) {
            this.errorLine = errorLine;
            if (errorLine != null) {
                addContext("errorLine", errorLine);
            }
            return this;
        }

        public Builder errorColumn(Integer errorColumn) {
            this.errorColumn = errorColumn;
            if (errorColumn != null) {
                addContext("errorColumn", errorColumn);
            }
            return this;
        }

        public Builder nearText(String nearText) {
            this.nearText = nearText;
            if (nearText != null) {
                addContext("nearText", nearText);
            }
            return this;
        }

        /**
         * Attempts to extract error position from the error message.
         *
         * @return this builder
         */
        public Builder extractPositionFromMessage() {

            String msg = cachedMessage != null ? cachedMessage.toLowerCase() : "";

            Matcher matcher = POSITION_PATTERN.matcher(msg);
            if (matcher.find()) {
                try {
                    errorPosition(Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException ignored) {
                }
            }

            return this;
        }

        @Override
        public QuerySyntaxException build() {
            return new QuerySyntaxException(this);
        }
    }
}