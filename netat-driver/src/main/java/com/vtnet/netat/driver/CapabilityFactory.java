package com.vtnet.netat.driver;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.Arrays;
import java.util.Properties;

public class CapabilityFactory {
    public static MutableCapabilities getCapabilities(String browser) {
        MutableCapabilities capabilities;
        switch (browser.toLowerCase()) {
            case "firefox":
                capabilities = new FirefoxOptions();
                break;
            case "chrome":
            default:
                capabilities = new ChromeOptions();
                break;
        }

        Properties properties = ConfigReader.getProperties();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("capability.")) {
                String capabilityName = key.substring("capability.".length());
                String value = properties.getProperty(key);

                // Xử lý các trường hợp đặc biệt, ví dụ: list các arguments cho Chrome
                if (capabilityName.equalsIgnoreCase("goog:chromeOptions.args")) {
                    ((ChromeOptions) capabilities).addArguments(Arrays.asList(value.split(",")));
                } else {
                    // Xử lý giá trị boolean
                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        capabilities.setCapability(capabilityName, Boolean.parseBoolean(value));
                    } else {
                        capabilities.setCapability(capabilityName, value);
                    }
                }
            }
        }
        return capabilities;
    }
}