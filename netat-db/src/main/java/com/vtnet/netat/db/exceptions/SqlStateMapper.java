package com.vtnet.netat.db.exceptions;

import com.vtnet.netat.db.exceptions.connection.*;
import com.vtnet.netat.db.exceptions.query.*;
import com.vtnet.netat.db.exceptions.transaction.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to map SQLException to specific DatabaseException subtypes
 * based on SQL State codes and vendor error codes.
 *
 * <p>Supports major databases:
 * <ul>
 *   <li>MySQL / MariaDB</li>
 *   <li>PostgreSQL</li>
 *   <li>Oracle</li>
 *   <li>SQL Server</li>
 *   <li>H2</li>
 * </ul>
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public final class SqlStateMapper {

    // SQL State prefixes (ANSI standard)
    private static final String CONNECTION_ERROR_PREFIX = "08";
    private static final String SYNTAX_ERROR_PREFIX = "42";
    private static final String CONSTRAINT_VIOLATION_PREFIX = "23";
    private static final String TRANSACTION_ERROR_PREFIX = "40";

    // Specific SQL States
    private static final Map<String, Class<? extends DatabaseException>> SQL_STATE_MAP = new HashMap<>();

    static {
        // Connection errors (08xxx)
        SQL_STATE_MAP.put("08000", ConnectionException.class);
        SQL_STATE_MAP.put("08001", ConnectionException.class);
        SQL_STATE_MAP.put("08003", ConnectionRefusedException.class);
        SQL_STATE_MAP.put("08004", ConnectionRefusedException.class);
        SQL_STATE_MAP.put("08006", ConnectionException.class);
        SQL_STATE_MAP.put("08007", ConnectionException.class);
        SQL_STATE_MAP.put("08S01", ConnectionTimeoutException.class);

        // Syntax errors (42xxx)
        SQL_STATE_MAP.put("42000", QuerySyntaxException.class);
        SQL_STATE_MAP.put("42S01", QuerySyntaxException.class); // Table already exists
        SQL_STATE_MAP.put("42S02", QuerySyntaxException.class); // Table not found
        SQL_STATE_MAP.put("42S21", QuerySyntaxException.class); // Column already exists
        SQL_STATE_MAP.put("42S22", QuerySyntaxException.class); // Column not found

        // Constraint violations (23xxx)
        SQL_STATE_MAP.put("23000", ConstraintViolationException.class);
        SQL_STATE_MAP.put("23001", ConstraintViolationException.class);
        SQL_STATE_MAP.put("23502", ConstraintViolationException.class); // NOT NULL violation
        SQL_STATE_MAP.put("23503", ConstraintViolationException.class); // Foreign key violation
        SQL_STATE_MAP.put("23505", ConstraintViolationException.class); // Unique violation
        SQL_STATE_MAP.put("23514", ConstraintViolationException.class); // Check constraint

        // Transaction errors (40xxx)
        SQL_STATE_MAP.put("40001", DeadlockException.class);
        SQL_STATE_MAP.put("40P01", DeadlockException.class); // PostgreSQL deadlock
        SQL_STATE_MAP.put("40002", TransactionRollbackException.class);
        SQL_STATE_MAP.put("40003", TransactionRollbackException.class);
    }

    // Vendor-specific error codes
    private static final Map<Integer, Class<? extends DatabaseException>> MYSQL_ERROR_MAP = new HashMap<>();
    private static final Map<Integer, Class<? extends DatabaseException>> POSTGRES_ERROR_MAP = new HashMap<>();

    static {
        // MySQL / MariaDB error codes
        MYSQL_ERROR_MAP.put(1040, ConnectionPoolExhaustedException.class); // Too many connections
        MYSQL_ERROR_MAP.put(1042, ConnectionRefusedException.class); // Can't get hostname
        MYSQL_ERROR_MAP.put(1043, ConnectionRefusedException.class); // Bad handshake
        MYSQL_ERROR_MAP.put(1045, ConnectionRefusedException.class); // Access denied
        MYSQL_ERROR_MAP.put(1046, QuerySyntaxException.class); // No database selected
        MYSQL_ERROR_MAP.put(1054, QuerySyntaxException.class); // Unknown column
        MYSQL_ERROR_MAP.put(1062, ConstraintViolationException.class); // Duplicate entry
        MYSQL_ERROR_MAP.put(1064, QuerySyntaxException.class); // SQL syntax error
        MYSQL_ERROR_MAP.put(1146, QuerySyntaxException.class); // Table doesn't exist
        MYSQL_ERROR_MAP.put(1205, QueryTimeoutException.class); // Lock wait timeout
        MYSQL_ERROR_MAP.put(1213, DeadlockException.class); // Deadlock found
        MYSQL_ERROR_MAP.put(1216, ConstraintViolationException.class); // Foreign key constraint
        MYSQL_ERROR_MAP.put(1217, ConstraintViolationException.class); // Cannot delete parent row
        MYSQL_ERROR_MAP.put(1364, ConstraintViolationException.class); // Field doesn't have default
        MYSQL_ERROR_MAP.put(1451, ConstraintViolationException.class); // Cannot delete/update parent
        MYSQL_ERROR_MAP.put(1452, ConstraintViolationException.class); // Cannot add/update child
        MYSQL_ERROR_MAP.put(2003, ConnectionRefusedException.class); // Can't connect to server
        MYSQL_ERROR_MAP.put(2006, ConnectionException.class); // Server has gone away
        MYSQL_ERROR_MAP.put(2013, ConnectionException.class); // Lost connection during query

        // PostgreSQL (using SQL State mostly, but some specific ones)
        // PostgreSQL typically relies more on SQL State than error codes
    }

    private SqlStateMapper() {
        // Utility class
    }

    /**
     * Maps a SQLException to the most specific DatabaseException type.
     *
     * @param sqlException the SQLException to map
     * @param query the SQL query that caused the exception (can be null)
     * @param parameters the query parameters (can be null)
     * @param profileName the database profile name
     * @return mapped DatabaseException
     */
    public static DatabaseException mapException(
            SQLException sqlException,
            String query,
            Object[] parameters,
            String profileName) {

        if (sqlException == null) {
            return GenericDatabaseException.builder()
                    .message("Unknown database error")
                    .profileName(profileName)
                    .query(query)
                    .parameters(parameters)
                    .build();
        }

        String sqlState = sqlException.getSQLState();
        int errorCode = sqlException.getErrorCode();
        String message = sqlException.getMessage();

        // Try exact SQL State match first
        Class<? extends DatabaseException> exceptionClass = SQL_STATE_MAP.get(sqlState);

        // Try vendor-specific error code
        if (exceptionClass == null && errorCode != 0) {
            exceptionClass = getVendorSpecificException(errorCode, message);
        }

        // Try SQL State prefix matching
        if (exceptionClass == null && sqlState != null && sqlState.length() >= 2) {
            String prefix = sqlState.substring(0, 2);
            exceptionClass = getExceptionByPrefix(prefix);
        }

        // Fallback to generic DatabaseException
        if (exceptionClass == null) {
            return createGenericException(sqlException, query, parameters, profileName);
        }

        // Create specific exception instance
        return createException(exceptionClass, sqlException, query, parameters, profileName);
    }

    /**
     * Gets exception class based on vendor-specific error code.
     */
    /**
     * Gets exception class based on vendor-specific error code.
     */
    private static Class<? extends DatabaseException> getVendorSpecificException(
            int errorCode, String message) {

        // Try MySQL/MariaDB codes
        Class<? extends DatabaseException> exceptionClass = MYSQL_ERROR_MAP.get(errorCode);
        if (exceptionClass != null) {
            return exceptionClass;
        }

        // Try PostgreSQL codes
        exceptionClass = POSTGRES_ERROR_MAP.get(errorCode);
        if (exceptionClass != null) {
            return exceptionClass;
        }

        // Try to infer from message content (last resort)
        if (message != null) {
            String lowerMsg = message.toLowerCase();

            if (lowerMsg.contains("timeout")) {
                if (lowerMsg.contains("connection")) {
                    return ConnectionTimeoutException.class;
                } else {
                    return QueryTimeoutException.class;
                }
            }

            if (lowerMsg.contains("deadlock")) {
                return DeadlockException.class;
            }

            if (lowerMsg.contains("duplicate") || lowerMsg.contains("unique")) {
                return ConstraintViolationException.class;
            }

            if (lowerMsg.contains("foreign key") || lowerMsg.contains("referential")) {
                return ConstraintViolationException.class;
            }

            if (lowerMsg.contains("syntax")) {
                return QuerySyntaxException.class;
            }
        }

        return null;
    }

    /**
     * Gets exception class based on SQL State prefix.
     */
    private static Class<? extends DatabaseException> getExceptionByPrefix(String prefix) {
        switch (prefix) {
            case CONNECTION_ERROR_PREFIX:
                return ConnectionException.class;
            case SYNTAX_ERROR_PREFIX:
                return QuerySyntaxException.class;
            case CONSTRAINT_VIOLATION_PREFIX:
                return ConstraintViolationException.class;
            case TRANSACTION_ERROR_PREFIX:
                return TransactionException.class;
            default:
                return null;
        }
    }

    /**
     * Creates a specific exception instance using reflection.
     */
    private static DatabaseException createException(
            Class<? extends DatabaseException> exceptionClass,
            SQLException sqlException,
            String query,
            Object[] parameters,
            String profileName) {

        try {
            // All exception classes should have a builder() static method
            Object builder = exceptionClass.getMethod("builder").invoke(null);

            // Get the builder class
            Class<?> builderClass = builder.getClass();

            // Populate common fields
            builderClass.getMethod("fromSQLException", SQLException.class).invoke(builder, sqlException);
            builderClass.getMethod("query", String.class).invoke(builder, query);
            builderClass.getMethod("parameters", Object[].class).invoke(builder, (Object) parameters);
            builderClass.getMethod("profileName", String.class).invoke(builder, profileName);

            // Special handling for specific exception types
            if (exceptionClass == QuerySyntaxException.class) {
                // Try to extract error position from message
                builderClass.getMethod("extractPositionFromMessage").invoke(builder);
            } else if (exceptionClass == ConstraintViolationException.class) {
                // Try to detect constraint type from message
                builderClass.getMethod("detectConstraintType").invoke(builder);
            }

            // Build and return the exception
            return (DatabaseException) builderClass.getMethod("build").invoke(builder);

        } catch (Exception e) {
            // If reflection fails, fall back to generic exception
            return createGenericException(sqlException, query, parameters, profileName);
        }
    }

    /**
     * Creates a generic DatabaseException when specific type cannot be determined.
     */
    private static DatabaseException createGenericException(
            SQLException sqlException,
            String query,
            Object[] parameters,
            String profileName) {

        return GenericDatabaseException.builder()
                .fromSQLException(sqlException)
                .query(query)
                .parameters(parameters)
                .profileName(profileName)
                .severity(ErrorSeverity.ERROR)
                .retryable(false)
                .build();
    }

    /**
     * Checks if an exception is retryable based on its type and characteristics.
     *
     * @param exception the database exception
     * @return true if retry is recommended
     */
    public static boolean isRetryable(DatabaseException exception) {
        if (exception == null) {
            return false;
        }

        // Use exception's own retryable flag
        if (exception.isRetryable()) {
            return true;
        }

        // Additional heuristics
        if (exception instanceof DeadlockException) {
            return true;
        }

        if (exception instanceof ConnectionTimeoutException) {
            return true;
        }

        if (exception instanceof ConnectionPoolExhaustedException) {
            return true;
        }

        if (exception instanceof QueryTimeoutException) {
            // Only retry if it's a SELECT query
            String query = exception.getQuery();
            if (query != null) {
                String trimmed = query.trim().toUpperCase();
                return trimmed.startsWith("SELECT");
            }
        }

        return false;
    }

    /**
     * Gets recommended retry delay in milliseconds based on exception type.
     *
     * @param exception the database exception
     * @param attemptNumber the retry attempt number (1-based)
     * @return recommended delay in milliseconds
     */
    public static long getRetryDelay(DatabaseException exception, int attemptNumber) {
        if (exception == null || attemptNumber < 1) {
            return 0;
        }

        // Base delays (in milliseconds)
        long baseDelay;

        if (exception instanceof DeadlockException) {
            baseDelay = 50; // Quick retry for deadlocks
        } else if (exception instanceof ConnectionTimeoutException) {
            baseDelay = 1000; // Wait longer for connection issues
        } else if (exception instanceof ConnectionPoolExhaustedException) {
            baseDelay = 500; // Medium wait for pool exhaustion
        } else if (exception instanceof QueryTimeoutException) {
            baseDelay = 2000; // Long wait for query timeouts
        } else {
            baseDelay = 100; // Default
        }

        // Exponential backoff: delay = baseDelay * (2 ^ (attemptNumber - 1))
        // With jitter to avoid thundering herd
        long delay = baseDelay * (long) Math.pow(2, attemptNumber - 1);

        // Add jitter (±25%)
        double jitter = 0.75 + (Math.random() * 0.5);
        delay = (long) (delay * jitter);

        // Cap at 30 seconds
        return Math.min(delay, 30000);
    }

    /**
     * Determines if an exception should be logged at ERROR level.
     *
     * @param exception the database exception
     * @return true if should log at ERROR level
     */
    public static boolean shouldLogAsError(DatabaseException exception) {
        if (exception == null) {
            return true;
        }

        ErrorSeverity severity = exception.getSeverity();

        // CRITICAL and ERROR should be logged at ERROR level
        if (severity == ErrorSeverity.CRITICAL || severity == ErrorSeverity.ERROR) {
            return true;
        }

        // WARNING should be logged at WARN level (not ERROR)
        if (severity == ErrorSeverity.WARNING) {
            return false;
        }

        // INFO should be logged at INFO level
        return false;
    }

    /**
     * Creates a summary message for logging purposes.
     *
     * @param exception the database exception
     * @return summary message
     */
    public static String getSummaryMessage(DatabaseException exception) {
        if (exception == null) {
            return "Unknown database error";
        }

        StringBuilder summary = new StringBuilder();

        summary.append(exception.getClass().getSimpleName());
        summary.append(": ");
        summary.append(exception.getMessage());

        if (exception.getSqlState() != null) {
            summary.append(" [SQLState: ").append(exception.getSqlState()).append("]");
        }

        if (exception.getVendorErrorCode() != 0) {
            summary.append(" [Code: ").append(exception.getVendorErrorCode()).append("]");
        }

        if (exception.getProfileName() != null) {
            summary.append(" [Profile: ").append(exception.getProfileName()).append("]");
        }

        return summary.toString();
    }
}