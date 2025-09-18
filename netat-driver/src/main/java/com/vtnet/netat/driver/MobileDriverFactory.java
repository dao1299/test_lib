package com.vtnet.netat.driver;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class MobileDriverFactory implements IDriverFactory {
    private static final Logger log = LoggerFactory.getLogger(MobileDriverFactory.class);

    @Override
    public WebDriver createDriver(String platform) { // Sửa 1: Thêm tham số 'platform'
        String appiumServerUrl = ConfigReader.getProperty("appium.server.url", "http://127.0.0.1:4723/");

        // Sửa 2: Sử dụng tham số 'platform'
        log.info("Initializing mobile driver for platform {} at Appium Server: {}", platform, appiumServerUrl);

        try {
            // Sửa 3: Truyền 'platform' vào CapabilityFactory
            MutableCapabilities capabilities = CapabilityFactory.getCapabilities(platform);
            URL url = new URL(appiumServerUrl);

            if ("android".equalsIgnoreCase(platform)) {
                return new AndroidDriver(url, capabilities);
            } else if ("ios".equalsIgnoreCase(platform)) {
                return new IOSDriver(url, capabilities);
            } else {
                throw new IllegalArgumentException("Unsupported mobile platform: " + platform);
            }
        } catch (Exception e) {
            log.error("Unable to initialize Appium driver.", e);
            throw new RuntimeException("Unable to initialize Appium driver.", e);
        }
    }
}