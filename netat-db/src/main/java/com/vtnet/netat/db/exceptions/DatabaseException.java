package com.vtnet.netat.db.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base exception class for all database-related errors in NETAT framework.
 * Provides rich context information for debugging and error handling.
 *
 * <p>Key features:
 * <ul>
 *   <li>SQL State and vendor error code tracking</li>
 *   <li>Query and parameter context (with automatic masking)</li>
 *   <li>Severity classification</li>
 *   <li>Retry eligibility determination</li>
 *   <li>JSON serialization for logging</li>
 *   <li>Actionable error suggestions</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     connection.execute(query);
 * } catch (SQLException e) {
 *     DatabaseException dbEx = SqlStateMapper.mapException(e, query, params, profileName);
 *     if (dbEx.isRetryable()) {
 *         // Retry logic
 *     } else {
 *         throw dbEx;
 *     }
 * }
 * }</pre>
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public abstract class DatabaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper JSON_MAPPER = createObjectMapper();

    // Pattern to detect sensitive parameter names
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            ".*(password|pwd|secret|token|key|ssn|credit|card).*",
            Pattern.CASE_INSENSITIVE
    );

    // Maximum query length in error messages
    private static final int MAX_QUERY_LENGTH = 500;

    // Core attributes
    private final String sqlState;
    private final int vendorErrorCode;
    private final String query;
    private final Object[] parameters;
    private final String profileName;
    private final String jdbcUrl;
    private final ErrorSeverity severity;
    private final Instant timestamp;
    private final String threadName;
    private final Map<String, Object> additionalContext;
    private final boolean retryable;

    /**
     * Protected constructor for subclasses using builder pattern.
     *
     * @param builder the builder containing exception details
     */
    protected DatabaseException(Builder<?> builder) {
        super(builder.message, builder.cause);
        this.sqlState = builder.sqlState;
        this.vendorErrorCode = builder.vendorErrorCode;
        this.query = builder.query;
        this.parameters = builder.parameters != null ? builder.parameters.clone() : null;
        this.profileName = builder.profileName;
        this.jdbcUrl = maskJdbcUrl(builder.jdbcUrl);
        this.severity = builder.severity;
        this.timestamp = builder.timestamp;
        this.threadName = builder.threadName;
        this.additionalContext = new HashMap<>(builder.additionalContext);
        this.retryable = builder.retryable;
    }

    // Getters

    /**
     * Gets the ANSI SQL State code (5 characters).
     *
     * @return SQL State code, or null if not available
     */
    public String getSqlState() {
        return sqlState;
    }

    /**
     * Gets the database vendor-specific error code.
     *
     * @return vendor error code, or 0 if not available
     */
    public int getVendorErrorCode() {
        return vendorErrorCode;
    }

    /**
     * Gets the SQL query that caused this exception.
     *
     * @return query string, or null if not applicable
     */
    public String getQuery() {
        return query;
    }

    /**
     * Gets the query parameters (masked for security).
     *
     * @return array of parameters, or null if not applicable
     */
    public Object[] getParameters() {
        return parameters != null ? parameters.clone() : null;
    }

    /**
     * Gets the database profile name.
     *
     * @return profile name
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Gets the JDBC URL (masked for security).
     *
     * @return masked JDBC URL
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Gets the severity level of this exception.
     *
     * @return severity level
     */
    public ErrorSeverity getSeverity() {
        return severity;
    }

    /**
     * Gets the timestamp when this exception occurred.
     *
     * @return timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the name of the thread where exception occurred.
     *
     * @return thread name
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Gets additional context information.
     *
     * @return unmodifiable map of context data
     */
    public Map<String, Object> getAdditionalContext() {
        return Collections.unmodifiableMap(additionalContext);
    }

    /**
     * Checks if this exception represents a transient error that can be retried.
     *
     * @return true if retry is recommended
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Gets a formatted, user-friendly error message with full context.
     *
     * @return formatted message
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();

        // Header with severity and timestamp
        sb.append(String.format("[%s] Database Error at %s\n",
                severity.getDisplayName(),
                timestamp));

        // Profile and connection info
        if (profileName != null) {
            sb.append(String.format("Profile: %s\n", profileName));
        }
        if (jdbcUrl != null) {
            sb.append(String.format("Database: %s\n", jdbcUrl));
        }

        // Error codes
        if (sqlState != null || vendorErrorCode != 0) {
            sb.append(String.format("Error Code: %d (SQL State: %s)\n",
                    vendorErrorCode,
                    sqlState != null ? sqlState : "N/A"));
        }

        // Main error message
        sb.append(String.format("Message: %s\n", getMessage()));

        // Query context
        if (query != null) {
            String displayQuery = query.length() > MAX_QUERY_LENGTH
                    ? query.substring(0, MAX_QUERY_LENGTH) + "..."
                    : query;
            sb.append(String.format("Query: %s\n", displayQuery));

            if (parameters != null && parameters.length > 0) {
                sb.append(String.format("Parameters: %s\n", formatParameters()));
            }
        }

        // Additional context
        if (!additionalContext.isEmpty()) {
            sb.append("Context:\n");
            additionalContext.forEach((key, value) ->
                    sb.append(String.format("  %s: %s\n", key, value)));
        }

        // Suggestion
        String suggestion = getSuggestion();
        if (suggestion != null && !suggestion.isEmpty()) {
            sb.append(String.format("Suggestion: %s\n", suggestion));
        }

        // Thread info
        sb.append(String.format("Thread: %s\n", threadName));

        return sb.toString();
    }

    /**
     * Gets an actionable suggestion for resolving this error.
     * Subclasses should override to provide specific guidance.
     *
     * @return suggestion text, or null if no specific suggestion available
     */
    public String getSuggestion() {
        return "Check the error details above and consult the documentation.";
    }

    /**
     * Converts this exception to a Map for serialization.
     *
     * @return map representation
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("exceptionType", this.getClass().getSimpleName());
        map.put("severity", severity.name());
        map.put("timestamp", timestamp.toString());
        map.put("message", getMessage());

        if (sqlState != null) map.put("sqlState", sqlState);
        if (vendorErrorCode != 0) map.put("vendorErrorCode", vendorErrorCode);
        if (profileName != null) map.put("profileName", profileName);
        if (jdbcUrl != null) map.put("jdbcUrl", jdbcUrl);
        if (query != null) map.put("query", truncateQuery(query));
        if (parameters != null) map.put("parameters", formatParameters());

        map.put("retryable", retryable);
        map.put("threadName", threadName);

        if (!additionalContext.isEmpty()) {
            map.put("additionalContext", additionalContext);
        }

        String suggestion = getSuggestion();
        if (suggestion != null) {
            map.put("suggestion", suggestion);
        }

        // Stack trace (first 5 elements)
        StackTraceElement[] stackTrace = getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            List<String> traces = Arrays.stream(stackTrace)
                    .limit(5)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.toList());
            map.put("stackTrace", traces);
        }

        return map;
    }

    /**
     * Converts this exception to JSON format.
     *
     * @return JSON string representation
     */
    public String toJson() {
        try {
            return JSON_MAPPER.writeValueAsString(toMap());
        } catch (JsonProcessingException e) {
            return String.format("{\"error\": \"Failed to serialize exception: %s\"}",
                    e.getMessage());
        }
    }

    /**
     * Converts this exception to pretty-printed JSON format.
     *
     * @return formatted JSON string
     */
    public String toJsonPretty() {
        try {
            return JSON_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(toMap());
        } catch (JsonProcessingException e) {
            return toJson();
        }
    }

    // Utility methods

    /**
     * Masks sensitive information in JDBC URL.
     *
     * @param url the JDBC URL to mask
     * @return masked URL
     */
    private static String maskJdbcUrl(String url) {
        if (url == null) return null;

        // Extract and mask password if present in URL
        return url.replaceAll("password=[^&;]+", "password=***")
                .replaceAll("pwd=[^&;]+", "pwd=***")
                .replaceAll("://[^:]+:[^@]+@", "://***:***@");
    }

    /**
     * Formats query parameters for display, masking sensitive values.
     *
     * @return formatted parameter string
     */
    private String formatParameters() {
        if (parameters == null || parameters.length == 0) {
            return "[]";
        }

        List<String> formatted = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            String value;

            // Check if this looks like a sensitive parameter
            boolean sensitive = false;
            if (query != null) {
                // Try to determine if this parameter position is sensitive
                // This is a simplistic approach; real implementation might be more sophisticated
                sensitive = SENSITIVE_PATTERN.matcher(query.toLowerCase()).find();
            }

            if (sensitive) {
                value = "***";
            } else if (param == null) {
                value = "NULL";
            } else if (param instanceof String) {
                String str = (String) param;
                value = str.length() > 50
                        ? "'" + str.substring(0, 47) + "...'"
                        : "'" + str + "'";
            } else {
                value = param.toString();
            }

            formatted.add(String.format("$%d=%s", i + 1, value));
        }

        return "[" + String.join(", ", formatted) + "]";
    }

    /**
     * Truncates query to maximum length.
     *
     * @param query the query to truncate
     * @return truncated query
     */
    private String truncateQuery(String query) {
        if (query == null) return null;
        if (query.length() <= MAX_QUERY_LENGTH) return query;
        return query.substring(0, MAX_QUERY_LENGTH) + "... (truncated)";
    }

    /**
     * Creates the ObjectMapper for JSON serialization.
     *
     * @return configured ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    // Builder Pattern

    /**
     * Abstract builder for creating DatabaseException instances.
     *
     * @param <T> the builder type for method chaining
     */
    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {
        private String message;
        private Throwable cause;
        private String sqlState;
        private int vendorErrorCode;
        private String query;
        private Object[] parameters;
        private String profileName;
        private String jdbcUrl;
        private ErrorSeverity severity = ErrorSeverity.ERROR;
        private Instant timestamp = Instant.now();
        private String threadName = Thread.currentThread().getName();
        private Map<String, Object> additionalContext = new HashMap<>();
        private boolean retryable = false;

        protected Builder() {}

        public T message(String message) {
            this.message = message;
            return (T) this;
        }

        public T cause(Throwable cause) {
            this.cause = cause;
            return (T) this;
        }

        public T sqlState(String sqlState) {
            this.sqlState = sqlState;
            return (T) this;
        }

        public T vendorErrorCode(int vendorErrorCode) {
            this.vendorErrorCode = vendorErrorCode;
            return (T) this;
        }

        public T query(String query) {
            this.query = query;
            return (T) this;
        }

        public T parameters(Object... parameters) {
            this.parameters = parameters;
            return (T) this;
        }

        public T profileName(String profileName) {
            this.profileName = profileName;
            return (T) this;
        }

        public T jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return (T) this;
        }

        public T severity(ErrorSeverity severity) {
            this.severity = severity;
            return (T) this;
        }

        public T timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return (T) this;
        }

        public T threadName(String threadName) {
            this.threadName = threadName;
            return (T) this;
        }

        public T retryable(boolean retryable) {
            this.retryable = retryable;
            return (T) this;
        }

        public T addContext(String key, Object value) {
            this.additionalContext.put(key, value);
            return (T) this;
        }

        public T addContext(Map<String, Object> context) {
            this.additionalContext.putAll(context);
            return (T) this;
        }

        /**
         * Populates builder from SQLException.
         *
         * @param e the SQLException
         * @return this builder
         */
        public T fromSQLException(SQLException e) {
            this.message = e.getMessage();
            this.cause = e;
            this.sqlState = e.getSQLState();
            this.vendorErrorCode = e.getErrorCode();
            return (T) this;
        }

        public abstract DatabaseException build();
    }
}