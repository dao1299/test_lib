package com.vtnet.netat.driver;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class ConfigReader {

    private static final String CONFIG_DIR = "config/";
    private static final String DEFAULT_CONFIG_FILE = "default.properties";
    private static final String ENV_CONFIG_PREFIX = "config.";
    private static final String ENV_CONFIG_SUFFIX = ".properties";

    private static final Properties properties = new Properties();
    private static volatile boolean isLoaded = false;
    private static volatile boolean isLoading = false;
    private static final Object LOCK = new Object();
    private static volatile String currentEnvironment;

    private ConfigReader() {}

    public static void loadProperties() {
        if (isLoaded) return;

        synchronized (LOCK) {
            if (isLoaded) return;
            if (isLoading) return;  // Prevent recursive call

            isLoading = true;
            try {
                System.out.println("[ConfigReader] Loading configuration...");

                // Load default.properties
                boolean loaded = loadFile(CONFIG_DIR + DEFAULT_CONFIG_FILE);
                if (!loaded) {
                    loadFile(DEFAULT_CONFIG_FILE);
                }

                // Load environment config if specified
                String env = System.getProperty("env");
                if (env != null && !env.trim().isEmpty()) {
                    currentEnvironment = env.trim();
                    loadFile(CONFIG_DIR + ENV_CONFIG_PREFIX + currentEnvironment + ENV_CONFIG_SUFFIX);
                }

                System.out.println("[ConfigReader] Loaded " + properties.size() + " properties");
                isLoaded = true;

            } finally {
                isLoading = false;
            }
        }
    }

    private static boolean loadFile(String path) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (stream == null) return false;

            Properties temp = new Properties();
            temp.load(stream);
            properties.putAll(temp);
            System.out.println("[ConfigReader] Loaded from: " + path);
            return true;
        } catch (IOException e) {
            System.err.println("[ConfigReader] Error loading: " + path + " - " + e.getMessage());
            return false;
        }
    }

    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    public static String getProperty(String key) {

        if (!isLoaded) {
            loadProperties();
        }

        while (isLoading) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        String value = System.getProperty(key);
        if (value != null) return value;

        return properties.getProperty(key);
    }


    public static Properties getProperties() {
        if (!isLoaded && !isLoading) loadProperties();

        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = getProperty(key);
        return value != null ? Boolean.parseBoolean(value.trim()) : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        String value = getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getEnvironment() {
        if (!isLoaded && !isLoading) loadProperties();
        return currentEnvironment;
    }

    public static boolean hasProperty(String key) {
        return getProperty(key) != null;
    }

    public static synchronized void reload() {
        properties.clear();
        isLoaded = false;
        isLoading = false;
        currentEnvironment = null;
        loadProperties();
    }

    public static synchronized void clear() {
        properties.clear();
        isLoaded = false;
        isLoading = false;
        currentEnvironment = null;
    }
}