package com.vtnet.netat.web.utils;

import com.vtnet.netat.core.exceptions.NetatException;
import com.vtnet.netat.core.logging.NetatLogger;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating WebDriver instances
 * Supports Chrome, Firefox, Edge, Safari browsers
 */
public class WebDriverFactory {

    private static final NetatLogger logger = NetatLogger.getInstance(WebDriverFactory.class);
    private static final int DEFAULT_TIMEOUT = 30;

    public enum BrowserType {
        CHROME("chrome"),
        FIREFOX("firefox"),
        EDGE("edge"),
        SAFARI("safari");

        private final String name;

        BrowserType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static BrowserType fromString(String browserName) {
            if (browserName == null) {
                return CHROME; // Default
            }

            for (BrowserType type : values()) {
                if (type.name.equalsIgnoreCase(browserName.trim())) {
                    return type;
                }
            }

            throw new NetatException("Unsupported browser type: " + browserName);
        }
    }

    /**
     * Create WebDriver instance based on browser type
     */
    public static WebDriver createDriver(String browserType) {
        return createDriver(browserType, false);
    }

    /**
     * Create WebDriver instance with headless option
     */
    public static WebDriver createDriver(String browserType, boolean headless) {
        BrowserType type = BrowserType.fromString(browserType);
        logger.info("Creating {} driver (headless: {})", type.getName(), headless);

        WebDriver driver;

        try {
            switch (type) {
                case CHROME:
                    driver = createChromeDriver(headless);
                    break;
                case FIREFOX:
                    driver = createFirefoxDriver(headless);
                    break;
                case EDGE:
                    driver = createEdgeDriver(headless);
                    break;
                case SAFARI:
                    driver = createSafariDriver();
                    break;
                default:
                    throw new NetatException("Unsupported browser: " + type);
            }

            // Configure common settings
            configureDriver(driver);

            logger.info("Successfully created {} driver", type.getName());
            return driver;

        } catch (Exception e) {
            throw new NetatException("Failed to create " + type.getName() + " driver", e);
        }
    }

    /**
     * Create Chrome driver
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless");
        }

        // Common Chrome options
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--ignore-ssl-errors");
        options.addArguments("--ignore-certificate-errors-spki-list");

        // Performance options
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--disable-renderer-backgrounding");

        // Window size
        options.addArguments("--window-size=1920,1080");

        // Preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("profile.managed_default_content_settings.images", 2); // Block images for faster loading
        options.setExperimentalOption("prefs", prefs);

        return new ChromeDriver(options);
    }

    /**
     * Create Firefox driver
     */
    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();

        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("--headless");
        }

        // Common Firefox options
        options.addArguments("--width=1920");
        options.addArguments("--height=1080");

        // Preferences
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("dom.push.enabled", false);
        options.addPreference("media.volume_scale", "0.0");

        return new FirefoxDriver(options);
    }

    /**
     * Create Edge driver
     */
    private static WebDriver createEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();

        EdgeOptions options = new EdgeOptions();

        if (headless) {
            options.addArguments("--headless");
        }

        // Common Edge options (similar to Chrome)
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--window-size=1920,1080");

        return new EdgeDriver(options);
    }

    /**
     * Create Safari driver
     */
    private static WebDriver createSafariDriver() {
        // Safari doesn't support headless mode
        logger.warn("Safari does not support headless mode");
        return new SafariDriver();
    }

    /**
     * Configure common driver settings
     */
    private static void configureDriver(WebDriver driver) {
        // Set timeouts
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT));

        // Maximize window (if not headless)
        try {
            driver.manage().window().maximize();
        } catch (Exception e) {
            logger.warn("Could not maximize window: {}", e.getMessage());
        }
    }

    /**
     * Get available browser types
     */
    public static String[] getAvailableBrowsers() {
        return new String[]{"chrome", "firefox", "edge", "safari"};
    }

    /**
     * Check if browser type is supported
     */
    public static boolean isBrowserSupported(String browserType) {
        try {
            BrowserType.fromString(browserType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}