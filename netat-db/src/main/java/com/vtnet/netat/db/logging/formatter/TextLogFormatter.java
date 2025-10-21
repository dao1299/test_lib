package com.vtnet.netat.db.logging.formatter;

import com.vtnet.netat.db.logging.model.QueryExecutionLog;
import com.vtnet.netat.db.logging.model.ConnectionLifecycleLog;
import com.vtnet.netat.db.logging.model.TransactionLog;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

/**
 * Human-readable text formatter for logs.
 */
public class TextLogFormatter implements LogFormatter {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String format(QueryExecutionLog log) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("[").append(log.getTimestamp().toString()).append("]");
        sb.append(" [").append(log.isSuccess() ? "SUCCESS" : "FAILED").append("]");
        sb.append(" [").append(log.getProfileName()).append("]");
        sb.append("\n");

        // Query
        sb.append("Query: ").append(log.getQuery()).append("\n");

        // Parameters
        if (log.getParameters() != null && log.getParameters().length > 0) {
            sb.append("Parameters: ").append(Arrays.toString(log.getParameters())).append("\n");
        }

        // Duration and rows
        sb.append("Duration: ").append(log.getDurationMs()).append("ms");
        if (log.isSuccess()) {
            sb.append(", Rows: ").append(log.getRowsAffected());
        }
        sb.append("\n");

        // Error (if any)
        if (!log.isSuccess() && log.getErrorMessage() != null) {
            sb.append("Error: ").append(log.getErrorMessage()).append("\n");
        }

        // Thread and context
        sb.append("Thread: ").append(log.getThreadName()).append("\n");

        return sb.toString();
    }

    @Override
    public String format(ConnectionLifecycleLog log) {
        // TODO: Implement in next step
        return "ConnectionLog: " + log;
    }

    @Override
    public String format(TransactionLog log) {
        // TODO: Implement in next step
        return "TransactionLog: " + log;
    }

    @Override
    public String formatMessage(String level, String message, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(level).append("] ");
        sb.append(message);

        if (context != null && !context.isEmpty()) {
            sb.append(" | Context: ").append(context);
        }

        return sb.toString();
    }
}