package com.vtnet.netat.db.logging;

import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.db.exceptions.DatabaseException;
import com.vtnet.netat.db.logging.formatter.JsonLogFormatter;
import com.vtnet.netat.db.logging.formatter.LogFormatter;
import com.vtnet.netat.db.logging.formatter.TextLogFormatter;
import com.vtnet.netat.db.logging.masking.SensitiveDataMasker;
import com.vtnet.netat.db.logging.model.PoolStats;
import com.vtnet.netat.db.logging.model.QueryExecutionLog;
import com.vtnet.netat.db.logging.performance.SlowQueryDetector;

import java.util.Arrays;

/**
 * Main facade for database logging.
 * Provides high-level logging methods for all database operations.
 */
public class DatabaseLogger {

    private static final NetatLogger logger = NetatLogger.getInstance(DatabaseLogger.class);

    // Configuration
    private LogLevel logLevel = LogLevel.INFO;
    private LogFormatter formatter = new TextLogFormatter();
    private final SlowQueryDetector slowQueryDetector = new SlowQueryDetector();
    private boolean maskSensitiveData = true;

    // Singleton
    private static final DatabaseLogger INSTANCE = new DatabaseLogger();

    private DatabaseLogger() {
        // Private constructor
        DatabaseLoggingConfig.applyConfiguration(this);
    }

    public static DatabaseLogger getInstance() {
        return INSTANCE;
    }

    // ========================================================================
    // QUERY LOGGING
    // ========================================================================

    /**
     * Logs the start of a query execution.
     */
    public void logQueryStart(String profileName, String query, Object... parameters) {
        if (!LogLevel.DEBUG.isEnabled(logLevel)) {
            return;
        }

        // Mask parameters
        Object[] maskedParams = maskSensitiveData
                ? SensitiveDataMasker.maskParameters(query, parameters)
                : parameters;

        logger.debug("[{}] QUERY_START: {}", profileName, query);
        if (maskedParams != null && maskedParams.length > 0) {
            logger.debug("[{}] Parameters: {}", profileName, Arrays.toString(maskedParams));
        }
    }

    /**
     * Logs successful query execution.
     */
    public void logQuerySuccess(String profileName, String query, Object[] parameters,
                                long durationMs, int rowsAffected) {

        // Record for slow query detection
        slowQueryDetector.recordQuery(query, durationMs);

        if (!LogLevel.INFO.isEnabled(logLevel)) {
            return;
        }

        // Build log entry
        QueryExecutionLog.Builder logBuilder = QueryExecutionLog.builder()
                .profileName(profileName)
                .query(query)
                .durationMs(durationMs)
                .rowsAffected(rowsAffected)
                .success(true);

        // Mask parameters
        if (parameters != null && parameters.length > 0) {
            Object[] maskedParams = maskSensitiveData
                    ? SensitiveDataMasker.maskParameters(query, parameters)
                    : parameters;
            logBuilder.parameters(maskedParams);
        }

        QueryExecutionLog log = logBuilder.build();

        // Format and log
        String formatted = formatter.format(log);

        if (log.isSlowQuery(slowQueryDetector.getWarningThreshold())) {
            logger.warn(formatted);
        } else {
            logger.info(formatted);
        }
    }

    /**
     * Logs failed query execution.
     */
    public void logQueryFailure(String profileName, String query, Object[] parameters,
                                long durationMs, DatabaseException exception) {

        if (!LogLevel.ERROR.isEnabled(logLevel)) {
            return;
        }

        // Build log entry
        QueryExecutionLog.Builder logBuilder = QueryExecutionLog.builder()
                .profileName(profileName)
                .query(query)
                .durationMs(durationMs)
                .success(false)
                .errorMessage(exception.getMessage());

        // Mask parameters
        if (parameters != null && parameters.length > 0) {
            Object[] maskedParams = maskSensitiveData
                    ? SensitiveDataMasker.maskParameters(query, parameters)
                    : parameters;
            logBuilder.parameters(maskedParams);
        }

        QueryExecutionLog log = logBuilder.build();

        // Format and log
        String formatted = formatter.format(log);
        logger.error(formatted);
    }

    // ========================================================================
    // CONNECTION LOGGING
    // ========================================================================

    /**
     * Logs connection open event.
     */
    public void logConnectionOpen(String profileName, String jdbcUrl) {
        if (!LogLevel.DEBUG.isEnabled(logLevel)) {
            return;
        }

        logger.debug("[{}] CONNECTION_OPEN: {}", profileName, jdbcUrl);
    }

    /**
     * Logs connection close event.
     */
    public void logConnectionClose(String profileName, long lifetimeMs) {
        if (!LogLevel.DEBUG.isEnabled(logLevel)) {
            return;
        }

        logger.debug("[{}] CONNECTION_CLOSE: lifetime={}ms", profileName, lifetimeMs);
    }

    /**
     * Logs connection timeout.
     */
    public void logConnectionTimeout(String profileName, long timeoutMs, long attemptedMs) {
        if (!LogLevel.WARN.isEnabled(logLevel)) {
            return;
        }

        logger.warn("[{}] CONNECTION_TIMEOUT: timeout={}ms, attempted={}ms",
                profileName, timeoutMs, attemptedMs);
    }

    /**
     * Logs connection pool statistics.
     */
    public void logConnectionPoolStats(String profileName, PoolStats stats) {
        if (!LogLevel.INFO.isEnabled(logLevel)) {
            return;
        }

        logger.info("[{}] POOL_STATS: {}", profileName, stats);

        // Warn if utilization is high
        if (stats.getUtilizationPercent() > 80) {
            logger.warn("[{}] High pool utilization: {:.1f}% (consider increasing pool size)",
                    profileName, stats.getUtilizationPercent());
        }
    }

    // ========================================================================
    // TRANSACTION LOGGING
    // ========================================================================

    /**
     * Logs transaction begin.
     */
    public void logTransactionBegin(String profileName, String isolationLevel) {
        if (!LogLevel.DEBUG.isEnabled(logLevel)) {
            return;
        }

        logger.debug("[{}] TRANSACTION_BEGIN: isolation={}", profileName, isolationLevel);
    }

    /**
     * Logs transaction commit.
     */
    public void logTransactionCommit(String profileName, long durationMs, int operationCount) {
        if (!LogLevel.INFO.isEnabled(logLevel)) {
            return;
        }

        logger.info("[{}] TRANSACTION_COMMIT: duration={}ms, operations={}",
                profileName, durationMs, operationCount);
    }

    /**
     * Logs transaction rollback.
     */
    public void logTransactionRollback(String profileName, String reason) {
        if (!LogLevel.WARN.isEnabled(logLevel)) {
            return;
        }

        logger.warn("[{}] TRANSACTION_ROLLBACK: reason={}", profileName, reason);
    }

    // ========================================================================
    // CONFIGURATION
    // ========================================================================

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        logger.info("Database log level set to: {}", logLevel);
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setFormatter(LogFormatter formatter) {
        this.formatter = formatter;
    }

    public void useTextFormat() {
        setFormatter(new TextLogFormatter());
    }

    public void useJsonFormat(boolean prettyPrint) {
        setFormatter(new JsonLogFormatter(prettyPrint));
    }

    public void setMaskSensitiveData(boolean maskSensitiveData) {
        this.maskSensitiveData = maskSensitiveData;
    }

    public boolean isMaskSensitiveData() {
        return maskSensitiveData;
    }

    public SlowQueryDetector getSlowQueryDetector() {
        return slowQueryDetector;
    }

    /**
     * Configures slow query thresholds.
     */
    public void configureSlowQueryDetection(long warningThresholdMs, long criticalThresholdMs) {
        slowQueryDetector.setWarningThreshold(warningThresholdMs);
        slowQueryDetector.setCriticalThreshold(criticalThresholdMs);
        logger.info("Slow query detection configured: warning={}ms, critical={}ms",
                warningThresholdMs, criticalThresholdMs);
    }
}