package com.vtnet.netat.db.logging.formatter;

import com.vtnet.netat.db.logging.model.QueryExecutionLog;
import com.vtnet.netat.db.logging.model.ConnectionLifecycleLog;
import com.vtnet.netat.db.logging.model.TransactionLog;

import java.util.Map;

/**
 * Interface for formatting log entries.
 * Implementations provide different output formats (text, JSON, etc.)
 */
public interface LogFormatter {

    /**
     * Formats a query execution log.
     */
    String format(QueryExecutionLog log);

    /**
     * Formats a connection lifecycle log.
     */
    String format(ConnectionLifecycleLog log);

    /**
     * Formats a transaction log.
     */
    String format(TransactionLog log);

    /**
     * Formats a generic message with context.
     */
    String formatMessage(String level, String message, Map<String, Object> context);
}