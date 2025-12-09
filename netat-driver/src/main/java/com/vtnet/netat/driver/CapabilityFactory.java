package com.vtnet.netat.driver;

import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Factory for creating Capabilities for different platforms.
 * Improvements:
 * - Added default mobile capabilities (noReset, autoGrantPermissions)
 * - App path validation
 * - Support override capabilities via Map
 */
public class CapabilityFactory {

    private static final Logger log = LoggerFactory.getLogger(CapabilityFactory.class);

    // Default mobile capabilities - applied to all mobile sessions
    private static final Map<String, Object> DEFAULT_MOBILE_CAPABILITIES = new HashMap<>();

    static {
        DEFAULT_MOBILE_CAPABILITIES.put("appium:noReset", true);
        DEFAULT_MOBILE_CAPABILITIES.put("appium:autoGrantPermissions", true);
        DEFAULT_MOBILE_CAPABILITIES.put("appium:newCommandTimeout", 300);
        DEFAULT_MOBILE_CAPABILITIES.put("df:recordVideo", true);
        DEFAULT_MOBILE_CAPABILITIES.put("df:liveVideo", true);
    }

    public static MutableCapabilities getCapabilities(String platform) {
        Properties properties = ConfigReader.getProperties();

        switch (platform.toLowerCase()) {
            case "android":
                return buildCapabilities(new UiAutomator2Options(), "android", properties);
            case "ios":
                return buildCapabilities(new XCUITestOptions(), "ios", properties);
            case "firefox":
                return buildCapabilities(new FirefoxOptions(), "firefox", properties);
            case "edge":
                return buildCapabilities(new EdgeOptions(), "edge", properties);
            case "safari":
                return buildCapabilities(new SafariOptions(), "safari", properties);
            case "chrome":
            default:
                return buildCapabilities(new ChromeOptions(), "chrome", properties);
        }
    }

    private static MutableCapabilities buildCapabilities(MutableCapabilities capabilities, String platform, Properties properties) {
        Map<String, Object> firefoxPrefs = new HashMap<>();
        Map<String, Object> chromePrefs = new HashMap<>();
        Map<String, Object> edgePrefs = new HashMap<>();

        if (isMobilePlatform(platform)) {
            applyDefaultMobileCapabilities(capabilities);
        }

        String optionPrefix = platform + ".option.";
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(optionPrefix)) {
                String optionType = key.substring(optionPrefix.length());
                String value = properties.getProperty(key);

                if (capabilities instanceof ChromeOptions) {
                    if (optionType.equalsIgnoreCase("binary")) {
                        ((ChromeOptions) capabilities).setBinary(value);
                    } else if (optionType.equalsIgnoreCase("args")) {
                        ((ChromeOptions) capabilities).addArguments(value.split(";"));
                    } else if (optionType.startsWith("prefs.")) {
                        String prefKey = optionType.substring("prefs.".length());
                        chromePrefs.put(prefKey, convertPrefValue(value));
                    }
                } else if (capabilities instanceof FirefoxOptions) {
                    if (optionType.equalsIgnoreCase("binary")) {
                        ((FirefoxOptions) capabilities).setBinary(value);
                    } else if (optionType.equalsIgnoreCase("args")) {
                        ((FirefoxOptions) capabilities).addArguments(value.split(";"));
                    } else if (optionType.startsWith("prefs.")) {
                        String prefKey = optionType.substring("prefs.".length());
                        firefoxPrefs.put(prefKey, convertPrefValue(value));
                    }
                } else if (capabilities instanceof EdgeOptions) {
                    if (optionType.equalsIgnoreCase("binary")) {
                        ((EdgeOptions) capabilities).setBinary(value);
                    } else if (optionType.equalsIgnoreCase("args")) {
                        ((EdgeOptions) capabilities).addArguments(value.split(";"));
                    } else if (optionType.startsWith("prefs.")) {
                        String prefKey = optionType.substring("prefs.".length());
                        edgePrefs.put(prefKey, convertPrefValue(value));
                    }
                }
            }
        }

        // Apply Chrome prefs
        if (capabilities instanceof ChromeOptions && !chromePrefs.isEmpty()) {
            ((ChromeOptions) capabilities).setExperimentalOption("prefs", chromePrefs);
        }

        // Apply Firefox prefs
        if (capabilities instanceof FirefoxOptions && !firefoxPrefs.isEmpty()) {
            FirefoxProfile profile = new FirefoxProfile();
            for (Map.Entry<String, Object> entry : firefoxPrefs.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Boolean) {
                    profile.setPreference(entry.getKey(), (Boolean) val);
                } else if (val instanceof Integer) {
                    profile.setPreference(entry.getKey(), (Integer) val);
                } else {
                    profile.setPreference(entry.getKey(), String.valueOf(val));
                }
            }
            ((FirefoxOptions) capabilities).setProfile(profile);
        }

        // Apply Edge prefs
        if (capabilities instanceof EdgeOptions && !edgePrefs.isEmpty()) {
            ((EdgeOptions) capabilities).setExperimentalOption("prefs", edgePrefs);
        }

        boolean hasAppPackage = false;
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("capability.")) {
                String capabilityName = key.substring("capability.".length());
                String value = properties.getProperty(key);

                if (capabilityName.equals("appium.appPackage")) {
                    hasAppPackage = true;
                }

                if (capabilityName.startsWith("appium.")) {
                    capabilityName = capabilityName.replaceFirst("\\.", ":");
                }

                capabilities.setCapability(capabilityName, convertPrefValue(value));
            }
        }

        String appName = ConfigReader.getProperty("app.name");
        if (appName != null && !appName.isEmpty() && !hasAppPackage) {
            String appPath = System.getProperty("user.dir") + "/src/test/resources/apps/" + appName;

            // Validate app path
            validateAppPath(appPath, appName);

            capabilities.setCapability("appium:app", appPath);
        }

        System.out.println("CAP: " + capabilities);

        return capabilities;
    }

    /**
     * Apply default capabilities for mobile
     */
    private static void applyDefaultMobileCapabilities(MutableCapabilities capabilities) {
        for (Map.Entry<String, Object> entry : DEFAULT_MOBILE_CAPABILITIES.entrySet()) {
            // Only set if not already defined
            if (capabilities.getCapability(entry.getKey()) == null) {
                capabilities.setCapability(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Validate that app path exists
     */
    private static void validateAppPath(String appPath, String appName) {
        File appFile = new File(appPath);
        if (!appFile.exists()) {
            log.warn("WARNING: Application file not found: {}", appPath);
            log.warn("  Please check:");
            log.warn("  1. Does file '{}' exist in src/test/resources/apps/ directory?", appName);
            log.warn("  2. Is the file name in config 'app.name' correct?");
            // Don't throw exception here for backward compatibility
            // Appium will report more detailed error if file doesn't exist
        }
    }

    private static boolean isMobilePlatform(String platform) {
        return "android".equalsIgnoreCase(platform) || "ios".equalsIgnoreCase(platform);
    }

    /**
     * Converts a preference value from a String to a suitable type (Boolean, Integer, etc.)
     */
    private static Object convertPrefValue(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * Create capabilities with override from Map
     */
    public static MutableCapabilities getCapabilities(String platform, Map<String, Object> overrideCapabilities) {
        MutableCapabilities capabilities = getCapabilities(platform);

        if (overrideCapabilities != null) {
            for (Map.Entry<String, Object> entry : overrideCapabilities.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (key.startsWith("appium.")) {
                    key = key.replaceFirst("\\.", ":");
                }
                capabilities.setCapability(key, value);
            }
        }
        return capabilities;
    }

    /**
     * Create mobile capabilities for startSession methods.
     * This method ensures consistent capabilities across different session initialization methods.
     *
     * @param platformName Android or iOS
     * @param udid Device UDID
     * @param automationName UiAutomator2 or XCUITest
     * @return Configured MutableCapabilities
     */
    public static MutableCapabilities buildMobileCapabilities(
            String platformName,
            String udid,
            String automationName) {

        MutableCapabilities caps = new MutableCapabilities();

        // Platform
        caps.setCapability("platformName", platformName);

        // Device
        if (udid != null && !udid.isBlank()) {
            caps.setCapability("appium:udid", udid);
        }

        // Automation
        if (automationName != null && !automationName.isBlank()) {
            caps.setCapability("appium:automationName", automationName);
        } else {
            // Default automation name based on platform
            if ("android".equalsIgnoreCase(platformName)) {
                caps.setCapability("appium:automationName", "UiAutomator2");
            } else if ("ios".equalsIgnoreCase(platformName)) {
                caps.setCapability("appium:automationName", "XCUITest");
            }
        }

        // Apply default mobile capabilities
        applyDefaultMobileCapabilities(caps);

        return caps;
    }

    /**
     * Add app path to capabilities
     */
    public static void setAppPath(MutableCapabilities caps, String appPath) {
        if (appPath != null && !appPath.isBlank()) {
            // Validate app path
            File appFile = new File(appPath);
            if (!appFile.exists()) {
                log.warn("WARNING: Application file not found: {}", appPath);
            }
            caps.setCapability("appium:app", appPath);
        }
    }

    /**
     * Add app package and activity to capabilities (Android)
     */
    public static void setAppPackage(MutableCapabilities caps, String appPackage, String appActivity) {
        if (appPackage != null && !appPackage.isBlank()) {
            caps.setCapability("appium:appPackage", appPackage);
        }
        if (appActivity != null && !appActivity.isBlank()) {
            caps.setCapability("appium:appActivity", appActivity);
        }
    }

    /**
     * Add bundle ID to capabilities (iOS)
     */
    public static void setBundleId(MutableCapabilities caps, String bundleId) {
        if (bundleId != null && !bundleId.isBlank()) {
            caps.setCapability("appium:bundleId", bundleId);
        }
    }
}