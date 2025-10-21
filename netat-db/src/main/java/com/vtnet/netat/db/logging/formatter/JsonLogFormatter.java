package com.vtnet.netat.db.logging.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vtnet.netat.db.logging.model.QueryExecutionLog;
import com.vtnet.netat.db.logging.model.ConnectionLifecycleLog;
import com.vtnet.netat.db.logging.model.TransactionLog;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON formatter for machine-parseable logs.
 * Suitable for ELK stack, Splunk, etc.
 */
public class JsonLogFormatter implements LogFormatter {

    private final ObjectMapper objectMapper;
    private final boolean prettyPrint;

    public JsonLogFormatter() {
        this(false);
    }

    public JsonLogFormatter(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        this.objectMapper = createObjectMapper();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        if (prettyPrint) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return mapper;
    }

    @Override
    public String format(QueryExecutionLog log) {
        Map<String, Object> json = new LinkedHashMap<>();

        json.put("timestamp", log.getTimestamp());
        json.put("type", "QUERY_EXECUTION");
        json.put("logId", log.getLogId());
        json.put("profileName", log.getProfileName());
        json.put("query", log.getQuery());

        if (log.getParameters() != null) {
            json.put("parameters", Arrays.asList(log.getParameters()));
        }

        json.put("durationMs", log.getDurationMs());
        json.put("rowsAffected", log.getRowsAffected());
        json.put("success", log.isSuccess());

        if (!log.isSuccess()) {
            json.put("errorMessage", log.getErrorMessage());
        }

        json.put("threadName", log.getThreadName());

        // Metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("slowQuery", log.isSlowQuery(1000));
        json.put("metadata", metadata);

        return toJson(json);
    }

    @Override
    public String format(ConnectionLifecycleLog log) {
        // TODO: Implement
        return toJson(Map.of("type", "CONNECTION_LIFECYCLE"));
    }

    @Override
    public String format(TransactionLog log) {
        // TODO: Implement
        return toJson(Map.of("type", "TRANSACTION"));
    }

    @Override
    public String formatMessage(String level, String message, Map<String, Object> context) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("level", level);
        json.put("message", message);
        if (context != null && !context.isEmpty()) {
            json.put("context", context);
        }
        return toJson(json);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize: " + e.getMessage() + "\"}";
        }
    }
}