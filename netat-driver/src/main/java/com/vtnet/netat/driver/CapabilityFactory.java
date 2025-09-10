package com.vtnet.netat.driver;

import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.util.Arrays;
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
            case "chrome":
            default:
                return buildCapabilities(new ChromeOptions(), "chrome", properties);
        }
    }

    private static MutableCapabilities buildCapabilities(MutableCapabilities capabilities, String platform, Properties properties) {
        // Tạo một map để xử lý các tùy chọn phức tạp như Firefox preferences
        Map<String, Object> firefoxPrefs = new HashMap<>();

        // 1. Xử lý các tùy chọn (options) đặc thù cho từng trình duyệt
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
                        // Gom tất cả các preferences của Firefox vào một map
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

        // Áp dụng các preferences cho Firefox nếu có
        if (capabilities instanceof FirefoxOptions && !firefoxPrefs.isEmpty()) {
            ((FirefoxOptions) capabilities).addPreference("profile", new FirefoxProfile()); // Khởi tạo profile
            for (Map.Entry<String, Object> entry : firefoxPrefs.entrySet()) {
                ((FirefoxOptions) capabilities).addPreference(entry.getKey(), entry.getValue());
            }
        }

        // 2. Xử lý các capabilities chung (bắt đầu bằng "capability.")
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("capability.")) {
                String capabilityName = key.substring("capability.".length());
                String value = properties.getProperty(key);
                capabilities.setCapability(capabilityName, convertPrefValue(value));
            }
        }

        // 3. Xử lý đường dẫn ứng dụng Appium
        String appName = ConfigReader.getProperty("app.name");
        if (appName != null && !appName.isEmpty()) {
            String appPath = System.getProperty("user.dir") + "/src/test/resources/apps/" + appName;
            capabilities.setCapability("appium:app", appPath);
        }

        return capabilities;
    }

    /**
     * Chuyển đổi giá trị của preference từ String sang kiểu dữ liệu phù hợp (Boolean, Integer,...)
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
            // Không phải là số, trả về chuỗi gốc
            return value;
        }
    }
}