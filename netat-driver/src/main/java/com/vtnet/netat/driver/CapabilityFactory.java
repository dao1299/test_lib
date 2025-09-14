package com.vtnet.netat.driver;

import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.safari.SafariOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CapabilityFactory {

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
        // Create a map to handle complex options like Firefox preferences
        Map<String, Object> firefoxPrefs = new HashMap<>();

        // 1. Handle browser-specific options
        String optionPrefix = platform + ".option.";
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(optionPrefix)) {
                String optionType = key.substring(optionPrefix.length());
                String value = properties.getProperty(key);

                if (capabilities instanceof ChromeOptions) {
                    if (optionType.equalsIgnoreCase("binary")) {
                        ((ChromeOptions) capabilities).setBinary(value);
                    } else if (optionType.equalsIgnoreCase("args")) {
                        ((ChromeOptions) capabilities).addArguments(value.split(","));
                    }
                } else if (capabilities instanceof FirefoxOptions) {
                    if (optionType.equalsIgnoreCase("binary")) {
                        ((FirefoxOptions) capabilities).setBinary(value);
                    } else if (optionType.equalsIgnoreCase("args")) {
                        ((FirefoxOptions) capabilities).addArguments(value.split(","));
                    } else if (optionType.startsWith("prefs.")) {
                        // Collect all Firefox preferences into a map
                        String prefKey = optionType.substring("prefs.".length());
                        firefoxPrefs.put(prefKey, convertPrefValue(value));
                    }
                } else if (capabilities instanceof EdgeOptions) {
                    if (optionType.equalsIgnoreCase("binary")) {
                        ((EdgeOptions) capabilities).setBinary(value);
                    } else if (optionType.equalsIgnoreCase("args")) {
                        ((EdgeOptions) capabilities).addArguments(value.split(","));
                    }
                }
            }
        }

        // Apply Firefox preferences if they exist
        if (capabilities instanceof FirefoxOptions && !firefoxPrefs.isEmpty()) {
            // **Correction**: Create a FirefoxProfile, set preferences on it,
            // and then set the entire profile to the options.
            FirefoxProfile profile = new FirefoxProfile();
            for (Map.Entry<String, Object> entry : firefoxPrefs.entrySet()) {
                profile.setPreference(entry.getKey(), entry.getValue());
            }
            ((FirefoxOptions) capabilities).setProfile(profile);
        }

        // 2. Handle general capabilities (starting with "capability.")
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("capability.")) {
                String capabilityName = key.substring("capability.".length());
                String value = properties.getProperty(key);
                capabilities.setCapability(capabilityName, convertPrefValue(value));
            }
        }

        // 3. Handle Appium app path
        String appName = ConfigReader.getProperty("app.name");
        if (appName != null && !appName.isEmpty()) {
            String appPath = System.getProperty("user.dir") + "/src/test/resources/apps/" + appName;
            capabilities.setCapability("appium:app", appPath);
        }

        return capabilities;
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
            // Not a number, return the original string
            return value;
        }
    }
}