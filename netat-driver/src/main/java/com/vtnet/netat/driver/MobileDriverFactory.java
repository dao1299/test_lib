package com.vtnet.netat.driver;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Factory for creating Mobile Driver (Android/iOS) with improvements:
 * - Input validation
 * - Appium server health check
 * - Retry logic on connection failure
 */
public class MobileDriverFactory implements IDriverFactory {
    private static final Logger log = LoggerFactory.getLogger(MobileDriverFactory.class);

    // Default configuration - can be overridden via properties
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DEFAULT_RETRY_DELAY_MS = 2000;
    private static final int DEFAULT_HEALTH_CHECK_TIMEOUT_MS = 5000;

    @Override
    public WebDriver createDriver(String platform, MutableCapabilities capabilities) {
        String appiumServerUrl = ConfigReader.getProperty("appium.server.url", "http://127.0.0.1:4723/");
        return createDriver(platform, capabilities, appiumServerUrl);
    }

    /**
     * Create driver with custom Appium server URL
     */
    public WebDriver createDriver(String platform, MutableCapabilities capabilities, String appiumServerUrl) {
        // 1. Validation
        validateInputs(platform, capabilities, appiumServerUrl);

        log.info("Initializing mobile driver for platform '{}' at Appium Server: {}", platform, appiumServerUrl);

        // 2. Health check Appium server
        checkAppiumServerReady(appiumServerUrl);

        // 3. Create driver with retry
        return createDriverWithRetry(platform, capabilities, appiumServerUrl);
    }

    // ==================== VALIDATION ====================

    private void validateInputs(String platform, MutableCapabilities capabilities, String appiumServerUrl) {
        // Validate platform
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException(
                    "Platform cannot be empty. Please specify 'android' or 'ios'.");
        }

        String normalizedPlatform = platform.toLowerCase().trim();
        if (!normalizedPlatform.equals("android") && !normalizedPlatform.equals("ios")) {
            throw new IllegalArgumentException(
                    "Invalid platform: '" + platform + "'. Only 'android' and 'ios' are supported.");
        }

        // Validate appiumServerUrl
        if (appiumServerUrl == null || appiumServerUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Appium Server URL cannot be empty. " +
                            "Please configure 'appium.server.url' in properties file or pass it directly.");
        }

        try {
            new URL(appiumServerUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    "Invalid Appium Server URL: '" + appiumServerUrl + "'. " +
                            "URL must be in format 'http://host:port/' (e.g., http://127.0.0.1:4723/)");
        }

        // Validate capabilities
        if (capabilities == null) {
            throw new IllegalArgumentException(
                    "Capabilities cannot be null. Please provide device configuration.");
        }

        // Validate UDID if present in capabilities
        Object udid = capabilities.getCapability("appium:udid");
        if (udid == null) {
            udid = capabilities.getCapability("udid");
        }
        if (udid != null && udid.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "UDID cannot be empty if declared. " +
                            "Please check 'capability.appium.udid' configuration or udid parameter.");
        }

        // Validate app path or appPackage
        Object app = capabilities.getCapability("appium:app");
        if (app == null) app = capabilities.getCapability("app");
        Object appPackage = capabilities.getCapability("appium:appPackage");
        if (appPackage == null) appPackage = capabilities.getCapability("appPackage");

        if ((app == null || app.toString().isBlank()) &&
                (appPackage == null || appPackage.toString().isBlank())) {
            log.warn("No 'app' or 'appPackage' found in capabilities. " +
                    "Make sure you have configured the application to test correctly.");
        }

        log.debug("Validation passed for platform: {}, serverUrl: {}", platform, appiumServerUrl);
    }

    // ==================== HEALTH CHECK ====================

    private void checkAppiumServerReady(String appiumServerUrl) {
        int timeout = getConfigInt("appium.healthcheck.timeout.ms", DEFAULT_HEALTH_CHECK_TIMEOUT_MS);
        String statusUrl = normalizeServerUrl(appiumServerUrl) + "status";

        log.debug("Checking Appium server health at: {}", statusUrl);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(statusUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                log.info("Appium server is ready at {}", appiumServerUrl);
            } else {
                log.warn("Appium server responded with status code: {}. Proceeding anyway...", responseCode);
            }
        } catch (IOException e) {
            String errorMsg = String.format(
                    "Cannot connect to Appium server at '%s'. " +
                            "Please check:\n" +
                            "  1. Is Appium server started?\n" +
                            "  2. Is the URL correct? (default: http://127.0.0.1:4723/)\n" +
                            "  3. Is firewall blocking the connection?\n" +
                            "Error details: %s",
                    appiumServerUrl, e.getMessage()
            );
            log.error(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ==================== RETRY LOGIC ====================

    private WebDriver createDriverWithRetry(String platform, MutableCapabilities capabilities, String appiumServerUrl) {
        int maxRetries = getConfigInt("appium.connection.retries", DEFAULT_RETRY_COUNT);
        int retryDelayMs = getConfigInt("appium.connection.retry.delay.ms", DEFAULT_RETRY_DELAY_MS);

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Attempt {}/{} to create {} driver...", attempt, maxRetries, platform);
                return doCreateDriver(platform, capabilities, appiumServerUrl);
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    log.info("Waiting {}ms before retry...", retryDelayMs);
                    sleep(retryDelayMs);
                }
            }
        }

        // All attempts failed
        String errorMsg = String.format(
                "Failed to create driver after %d attempts. Last error: %s\n" +
                        "Please check:\n" +
                        "  1. Device is connected and visible in 'adb devices' (Android) or 'xcrun xctrace list devices' (iOS)\n" +
                        "  2. Appium server is running and healthy\n" +
                        "  3. Capabilities are configured correctly\n" +
                        "  4. Application file (.apk/.ipa) exists at the specified path",
                maxRetries, lastException != null ? lastException.getMessage() : "Unknown error"
        );
        log.error(errorMsg);
        throw new RuntimeException(errorMsg, lastException);
    }

    private WebDriver doCreateDriver(String platform, MutableCapabilities capabilities, String appiumServerUrl) {
        try {
            URL url = new URL(appiumServerUrl);
            String normalizedPlatform = platform.toLowerCase().trim();

            WebDriver driver;
            if ("android".equals(normalizedPlatform)) {
                driver = new AndroidDriver(url, capabilities);
                log.info("AndroidDriver created successfully");
            } else if ("ios".equals(normalizedPlatform)) {
                driver = new IOSDriver(url, capabilities);
                log.info("IOSDriver created successfully");
            } else {
                throw new IllegalArgumentException("Unsupported mobile platform: " + platform);
            }

            return driver;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium server URL: " + appiumServerUrl, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create driver: " + e.getMessage(), e);
        }
    }

    // ==================== UTILITY METHODS ====================

    private String normalizeServerUrl(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url : url + "/";
    }

    private int getConfigInt(String key, int defaultValue) {
        try {
            String value = ConfigReader.getProperty(key);
            if (value != null && !value.isBlank()) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for config '{}', using default: {}", key, defaultValue);
        }
        return defaultValue;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted");
        }
    }
}