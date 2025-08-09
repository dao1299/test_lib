package com.vtnet.netat.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocalDriverFactory implements IDriverFactory {
    private static final Logger log = LoggerFactory.getLogger(LocalDriverFactory.class);
    @Override
    public WebDriver createDriver() {
        String browser = ConfigReader.getProperty("browser.name");
        log.info("Khởi tạo driver local cho trình duyệt: {}", browser);

        // Xử lý driver path thủ công cho môi trường offline
        String driverPath = ConfigReader.getProperty("webdriver." + browser + ".driver");
        if (driverPath != null && !driverPath.isEmpty()) {
            System.setProperty("webdriver." + browser + ".driver", driverPath);
            log.info("Sử dụng driver thủ công tại: {}", driverPath);
        } else {
            log.info("Không có đường dẫn driver thủ công, Selenium Manager sẽ tự động xử lý.");
        }

        MutableCapabilities capabilities = CapabilityFactory.getCapabilities(browser);

        switch (browser.toLowerCase()) {
            case "firefox":
                return new FirefoxDriver((FirefoxOptions) capabilities);
            case "chrome":
            default:
                return new ChromeDriver((ChromeOptions) capabilities);
        }
    }
}