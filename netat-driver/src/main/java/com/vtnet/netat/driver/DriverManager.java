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
            log.info("Môi trường thực thi: {}", executionType.toUpperCase());

            IDriverFactory factory;
            if ("remote".equalsIgnoreCase(executionType)) {
                factory = new RemoteDriverFactory();
            } else {
                factory = new LocalDriverFactory();
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