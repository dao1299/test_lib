package com.vtnet.netat.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Custom logger wrapper cho NETAT platform
 * Provides additional context và formatting cho test execution logs
 */
public class NetatLogger {

    private static final Map<String, NetatLogger> LOGGERS = new ConcurrentHashMap<>();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final Logger logger;
    private final String name;

    private NetatLogger(String name) {
        this.name = name;
        this.logger = LoggerFactory.getLogger(name);
    }

    public static NetatLogger getInstance() {
        return getInstance(NetatLogger.class);
    }

    public static NetatLogger getInstance(Class<?> clazz) {
        return getInstance(clazz.getName());
    }

    public static NetatLogger getInstance(String name) {
        return LOGGERS.computeIfAbsent(name, NetatLogger::new);
    }

    // Info level logging
    public void info(String message) {
        setContext();
        logger.info(formatMessage(message));
        clearContext();
    }

    public void info(String message, Object... args) {
        setContext();
        logger.info(formatMessage(message), args);
        clearContext();
    }

    // Debug level logging
    public void debug(String message) {
        setContext();
        logger.debug(formatMessage(message));
        clearContext();
    }

    public void debug(String message, Object... args) {
        setContext();
        logger.debug(formatMessage(message), args);
        clearContext();
    }

    // Warn level logging
    public void warn(String message) {
        setContext();
        logger.warn(formatMessage(message));
        clearContext();
    }

    public void warn(String message, Object... args) {
        setContext();
        logger.warn(formatMessage(message), args);
        clearContext();
    }

    public void warn(String message, Throwable throwable) {
        setContext();
        logger.warn(formatMessage(message), throwable);
        clearContext();
    }

    // Error level logging
    public void error(String message) {
        setContext();
        logger.error(formatMessage(message));
        clearContext();
    }

    public void error(String message, Object... args) {
        setContext();
        logger.error(formatMessage(message), args);
        clearContext();
    }

    public void error(String message, Throwable throwable) {
        setContext();
        logger.error(formatMessage(message), throwable);
        clearContext();
    }

    // Special logging methods for test execution
    public void logKeywordStart(String keywordName, Object... parameters) {
        setContext();
        logger.info("🚀 KEYWORD START: {} | Parameters: {}", keywordName, formatParameters(parameters));
        clearContext();
    }

    public void logKeywordEnd(String keywordName, boolean success, long durationMs) {
        setContext();
        String status = success ? "✅ PASSED" : "❌ FAILED";
        logger.info("🏁 KEYWORD END: {} | Status: {} | Duration: {}ms", keywordName, status, durationMs);
        clearContext();
    }

    public void logTestCaseStart(String testCaseName) {
        setContext();
        logger.info("📋 TEST CASE START: {}", testCaseName);
        clearContext();
    }

    public void logTestCaseEnd(String testCaseName, boolean success, long durationMs) {
        setContext();
        String status = success ? "✅ PASSED" : "❌ FAILED";
        logger.info("📊 TEST CASE END: {} | Status: {} | Duration: {}ms", testCaseName, status, durationMs);
        clearContext();
    }

    public void logScreenshot(String screenshotPath) {
        setContext();
        logger.info("📸 Screenshot captured: {}", screenshotPath);
        clearContext();
    }

    public void logAssertion(String assertion, boolean result) {
        setContext();
        String status = result ? "✅ PASSED" : "❌ FAILED";
        logger.info("🔍 ASSERTION: {} | Result: {}", assertion, status);
        clearContext();
    }

    // Private helper methods
    private void setContext() {
        try {
            // Get current execution context if available
            com.vtnet.netat.core.context.ExecutionContext context =
                    com.vtnet.netat.core.context.ExecutionContext.getInstance();

            if (context.getCurrentTestCase() != null) {
                MDC.put("testCase", context.getCurrentTestCase());
            }

            if (context.getCurrentTestSuite() != null) {
                MDC.put("testSuite", context.getCurrentTestSuite());
            }

            if (context.getCurrentKeyword() != null) {
                MDC.put("keyword", context.getCurrentKeyword());
            }

            MDC.put("thread", Thread.currentThread().getName());
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));

        } catch (Exception e) {
            // Ignore context setting errors
        }
    }

    private void clearContext() {
        MDC.clear();
    }

    private String formatMessage(String message) {
        return message;
    }

    private String formatParameters(Object... parameters) {
        if (parameters == null || parameters.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters[i] != null ? parameters[i].toString() : "null");
        }
        sb.append("]");
        return sb.toString();
    }

    // Getter for underlying SLF4J logger
    public Logger getLogger() {
        return logger;
    }

    public String getName() {
        return name;
    }
}