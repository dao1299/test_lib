package com.vtnet.netat.driver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.Arrays;
import java.util.Properties;

public class CapabilityFactory {

    public static MutableCapabilities getCapabilities(String platform) {
        MutableCapabilities capabilities;
        Properties properties = ConfigReader.getProperties();

        switch (platform.toLowerCase()) {
            case "android":
                capabilities = new UiAutomator2Options();
                break;
            case "ios":
                capabilities = new XCUITestOptions();
                break;
            case "firefox":
                capabilities = new FirefoxOptions();
                break;
            case "chrome":
            default:
                capabilities = new ChromeOptions();
                break;
        }

        // Đọc tất cả các capability có tiền tố "capability."
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("capability.")) {
                String capabilityName = key.substring("capability.".length());
                String value = properties.getProperty(key);

                // Xử lý các trường hợp đặc biệt...
                if ("goog:chromeOptions.args".equalsIgnoreCase(capabilityName)) {
                    ((ChromeOptions) capabilities).addArguments(Arrays.asList(value.split(",")));
                } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    capabilities.setCapability(capabilityName, Boolean.parseBoolean(value));
                } else {
                    String capabilityKey = capabilityName.replace("appium.","");
                    System.out.println("Capability: "+capabilityKey+"| value: "+value);

                    capabilities.setCapability(capabilityKey, value);
                }
            }
        }

        // Gợi ý: Xử lý đường dẫn ứng dụng
        String appName = ConfigReader.getProperty("app.name");
        if (appName != null && !appName.isEmpty()) {
            String appPath = System.getProperty("user.dir") + "/src/test/resources/apps/" + appName;
            capabilities.setCapability("appium:app", appPath);
        }

        return capabilities;
    }
}