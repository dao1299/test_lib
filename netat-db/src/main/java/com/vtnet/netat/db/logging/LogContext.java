package com.vtnet.netat.db.logging;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local context for database logging.
 * Stores contextual information that will be automatically included in all logs.
 */
public final class LogContext {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    // Standard context keys
    public static final String PROFILE_NAME = "profileName";
    public static final String TEST_CASE = "testCase";
    public static final String KEYWORD = "keyword";
    public static final String THREAD_NAME = "threadName";
    public static final String START_TIME = "startTime";

    private LogContext() {
        // Utility class
    }

    /**
     * Sets a context value.
     */
    public static void set(String key, Object value) {
        CONTEXT.get().put(key, value);
    }

    /**
     * Gets a context value.
     */
    public static Object get(String key) {
        return CONTEXT.get().get(key);
    }

    /**
     * Gets a context value as String.
     */
    public static String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Checks if context has a key.
     */
    public static boolean has(String key) {
        return CONTEXT.get().containsKey(key);
    }

    /**
     * Gets all context as a map.
     */
    public static Map<String, Object> getAll() {
        return new HashMap<>(CONTEXT.get());
    }

    /**
     * Removes a specific key.
     */
    public static void remove(String key) {
        CONTEXT.get().remove(key);
    }

    /**
     * Clears all context for current thread.
     */
    public static void clear() {
        CONTEXT.get().clear();
    }

    /**
     * Sets standard context for a database operation.
     */
    public static void setDatabaseContext(String profileName, String testCase, String keyword) {
        set(PROFILE_NAME, profileName);
        set(TEST_CASE, testCase);
        set(KEYWORD, keyword);
        set(THREAD_NAME, Thread.currentThread().getName());
        set(START_TIME, Instant.now());
    }

    /**
     * Gets the profile name from context.
     */
    public static String getProfileName() {
        return getString(PROFILE_NAME);
    }

    /**
     * Gets the test case name from context.
     */
    public static String getTestCase() {
        return getString(TEST_CASE);
    }

    /**
     * Gets the keyword name from context.
     */
    public static String getKeyword() {
        return getString(KEYWORD);
    }
}