package com.vtnet.netat.db.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration loader for database logging.
 */
public class DatabaseLoggingConfig {

    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        if (loaded) {
            return;
        }

        try (InputStream input = DatabaseLoggingConfig.class
                .getClassLoader()
                .getResourceAsStream("database-logging.properties")) {

            if (input != null) {
                properties.load(input);
                loaded = true;
            }
        } catch (IOException e) {
            // Use defaults if config file not found
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Return default
            }
        }
        return defaultValue;
    }

    public static long getLongProperty(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                // Return default
            }
        }
        return defaultValue;
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * Applies configuration to DatabaseLogger.
     */
    public static void applyConfiguration(DatabaseLogger logger) {
        // Log level
        String levelStr = getProperty("db.logging.level", "INFO");
        try {
            LogLevel level = LogLevel.valueOf(levelStr);
            logger.setLogLevel(level);
        } catch (IllegalArgumentException e) {
            // Invalid level, use default
        }

        // Format
        String format = getProperty("db.logging.format", "TEXT");
        if ("JSON".equalsIgnoreCase(format)) {
            boolean prettyPrint = getBooleanProperty("db.logging.json.prettyPrint", false);
            logger.useJsonFormat(prettyPrint);
        } else {
            logger.useTextFormat();
        }

        // Masking
        boolean maskingEnabled = getBooleanProperty("db.logging.masking.enabled", true);
        logger.setMaskSensitiveData(maskingEnabled);

        // Slow query thresholds
        long warningThreshold = getLongProperty("db.logging.slowquery.warning.threshold", 1000);
        long criticalThreshold = getLongProperty("db.logging.slowquery.critical.threshold", 5000);
        logger.configureSlowQueryDetection(warningThreshold, criticalThreshold);
    }
}