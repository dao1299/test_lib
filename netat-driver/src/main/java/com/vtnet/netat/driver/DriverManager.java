package com.vtnet.netat.driver;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;

public final class DriverManager {
    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();

    private DriverManager() {}

    public static WebDriver getDriver() {
        return threadLocalDriver.get();
    }

    public static void initDriver() {
        if (threadLocalDriver.get() == null) {
            ConfigReader.loadProperties(); // Đảm bảo cấu hình đã được tải

            // Sử dụng "platform.name" làm key chính để quyết định
            String platform = ConfigReader.getProperty("platform.name");
            log.info("Nền tảng thực thi được yêu cầu: {}", platform.toUpperCase());

            IDriverFactory factory;

            // Phân loại factory dựa trên nền tảng
            switch (platform.toLowerCase()) {
                case "android":
                case "ios":
                    factory = new MobileDriverFactory();
                    break;
                case "chrome":
                case "firefox":
                case "edge":
                    // Nếu là web, đặt lại thuộc tính browser.name để Local/Remote factory có thể dùng
                    ConfigReader.getProperties().setProperty("browser.name", platform);
                    String executionType = ConfigReader.getProperty("execution.type", "local");
                    if ("remote".equalsIgnoreCase(executionType)) {
                        factory = new RemoteDriverFactory();
                    } else {
                        factory = new LocalDriverFactory();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Nền tảng không được hỗ trợ: " + platform);
            }

            WebDriver driver = factory.createDriver();

            // Không maximize cho mobile
            if (!(platform.equalsIgnoreCase("android") || platform.equalsIgnoreCase("ios"))) {
                driver.manage().window().maximize();
            }

            long defaultTimeout = Long.parseLong(ConfigReader.getProperty("timeout.default", "30"));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(defaultTimeout));

            threadLocalDriver.set(driver);
        }
    }

    public static void quitDriver() {
        WebDriver driver = threadLocalDriver.get();
        if (driver != null) {
            log.info("Đóng driver cho luồng: {}", Thread.currentThread().getId());
            driver.quit();
            threadLocalDriver.remove();
        }
    }
}