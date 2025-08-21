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
            String executionType = ConfigReader.getProperty("execution.type");
            String platform = ConfigReader.getProperty("platform.name");
            log.info("Môi trường thực thi: {}", executionType.toUpperCase());
            log.info("Nền tảng thực thi: {}", platform.toUpperCase());

            IDriverFactory factory;
            switch (platform.toLowerCase()) {
                case "android":
                case "ios":
                    factory = new MobileDriverFactory();
                    break;
                case "chrome":
                case "firefox":
                default: // Web là mặc định
                    ConfigReader.getProperties().setProperty("browser.name", platform);
                    factory = new LocalDriverFactory();
                    break;
            }

            WebDriver driver = factory.createDriver();
            driver.manage().window().maximize();

            long defaultTimeout = Long.parseLong(ConfigReader.getProperty("timeout.default"));
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