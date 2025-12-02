package com.vtnet.netat.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class DriverManager {

    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);

    private static final ThreadLocal<String> currentPlatform = new ThreadLocal<>();

    private DriverManager() {}

    public static WebDriver getDriver() {
        return SessionManager.getInstance().getCurrentDriver();
    }

    public static boolean isDriverInitialized() {
        return SessionManager.getInstance().getCurrentDriver() != null;
    }


    public static String getCurrentPlatform() {
        return currentPlatform.get();
    }

    public static void initDriver() {
        ConfigReader.loadProperties();
        String platform = ConfigReader.getProperty("platform.name");

        if (platform == null || platform.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Property 'platform.name' is not defined in config. " +
                            "Please set it in properties file or via -Dplatform.name=xxx"
            );
        }

        initDriver(platform.trim(), null);
    }

    public static void initDriver(String platform, Map<String, Object> overrideCapabilities) {
        if (platform == null || platform.trim().isEmpty()) {
            throw new IllegalArgumentException("Platform cannot be null or empty");
        }

        String normalizedPlatform = platform.trim().toLowerCase();
        SessionManager sessionManager = SessionManager.getInstance();

        if (sessionManager.getSession(SessionManager.DEFAULT_SESSION) != null) {
            log.debug("Default session already exists for thread [{}]. Reusing existing driver.",
                    Thread.currentThread().getName());
            return;
        }

        ConfigReader.loadProperties();

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Initializing WebDriver");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Platform: {}", normalizedPlatform);
        log.info("Thread: {} (ID: {})",
                Thread.currentThread().getName(),
                Thread.currentThread().getId());

        try {
            IDriverFactory factory = createDriverFactory(normalizedPlatform);

            MutableCapabilities caps = CapabilityFactory.getCapabilities(
                    normalizedPlatform,
                    overrideCapabilities
            );

            WebDriver driver = factory.createDriver(normalizedPlatform, caps);

            sessionManager.addSession(SessionManager.DEFAULT_SESSION, driver);
            sessionManager.switchSession(SessionManager.DEFAULT_SESSION);

            currentPlatform.set(normalizedPlatform);

            log.info("WebDriver initialized successfully");
            log.info("Driver type: {}", driver.getClass().getSimpleName());
            log.info("═══════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("Failed to initialize WebDriver for platform: {}", normalizedPlatform, e);
            throw new RuntimeException(
                    "Failed to initialize WebDriver for platform: " + normalizedPlatform,
                    e
            );
        }
    }

    private static IDriverFactory createDriverFactory(String platform) {
        switch (platform) {
            case "android":
            case "ios":
                log.debug("Using MobileDriverFactory for platform: {}", platform);
                return new MobileDriverFactory();

            case "chrome":
            case "firefox":
            case "edge":
            case "safari":
                String executionType = ConfigReader.getProperty("execution.type", "local");

                if ("remote".equalsIgnoreCase(executionType)) {
                    String gridUrl = ConfigReader.getProperty("grid.url");
                    log.debug("Using RemoteDriverFactory. Grid URL: {}", gridUrl);
                    return new RemoteDriverFactory();
                } else {
                    log.debug("Using LocalDriverFactory for local execution");
                    return new LocalDriverFactory();
                }

            default:
                throw new IllegalArgumentException(
                        "Platform not supported: " + platform + ". " +
                                "Supported platforms: chrome, firefox, edge, safari, android, ios"
                );
        }
    }

    public static void quit() {
        log.info("Closing all driver sessions for thread [{}] (ID: {})",
                Thread.currentThread().getName(),
                Thread.currentThread().getId());

        try {
            SessionManager.getInstance().stopAllSessions();

            currentPlatform.remove();

            log.info("All driver sessions closed successfully");

        } catch (Exception e) {
            log.warn("Error while closing driver sessions: {}", e.getMessage());
        }
    }

    @Deprecated
    public static void quitDriver() {
        quit();
    }
}