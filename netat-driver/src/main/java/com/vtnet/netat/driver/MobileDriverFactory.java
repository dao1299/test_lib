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
    public WebDriver createDriver() {
        String platform = ConfigReader.getProperty("platform.name");
        String appiumServerUrl = ConfigReader.getProperty("appium.server.url", "http://127.0.0.1:4723/");
        log.info("Khởi tạo mobile driver cho nền tảng {} tại Appium Server: {}", platform, appiumServerUrl);

        try {
            MutableCapabilities capabilities = CapabilityFactory.getCapabilities(platform);
            URL url = new URL(appiumServerUrl);

            if ("android".equalsIgnoreCase(platform)) {
                return new AndroidDriver(url, capabilities);
            } else if ("ios".equalsIgnoreCase(platform)) {
                return new IOSDriver(url, capabilities);
            } else {
                throw new IllegalArgumentException("Nền tảng di động không được hỗ trợ: " + platform);
            }
        } catch (Exception e) {
            log.error("Không thể khởi tạo Appium driver.", e);
            throw new RuntimeException("Không thể khởi tạo Appium driver.", e);
        }
    }
}