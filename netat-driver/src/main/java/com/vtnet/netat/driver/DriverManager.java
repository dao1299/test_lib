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

    /**
     * Khởi tạo driver với nền tảng mặc định từ file cấu hình.
     */
    public static void initDriver() {
        if (threadLocalDriver.get() == null) {
            ConfigReader.loadProperties();
            String platform = ConfigReader.getProperty("platform.name");
            if (platform == null || platform.trim().isEmpty()) {
                throw new IllegalArgumentException("Property 'platform.name' is not defined in configuration file.");
            }
            // Gọi lại phương thức initDriver có tham số để tái sử dụng logic
            initDriver(platform);
        }
    }

    /**
     * Khởi tạo driver với một nền tảng cụ thể (dùng cho parallel/cross-browser).
     */
    public static void initDriver(String platform) {
        if (threadLocalDriver.get() == null) {
            ConfigReader.loadProperties();

            if (platform == null || platform.trim().isEmpty()) {
                throw new IllegalArgumentException("Platform name cannot be empty when initializing driver.");
            }

            log.info("Platform requested for this thread: {}", platform.toUpperCase());

            IDriverFactory factory;

            switch (platform.toLowerCase()) {
                case "android":
                case "ios":
                    factory = new MobileDriverFactory();
                    break;
                case "chrome":
                case "firefox":
                case "edge":
                case "safari":
                    String executionType = ConfigReader.getProperty("execution.type", "local");
                    if ("remote".equalsIgnoreCase(executionType)) {
                        factory = new RemoteDriverFactory();
                    } else {
                        factory = new LocalDriverFactory();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Platform not supported: " + platform);
            }

            // Truyền platform trực tiếp vào factory để tạo driver chính xác
            WebDriver driver = factory.createDriver(platform);

            if (!(platform.equalsIgnoreCase("android") || platform.equalsIgnoreCase("ios"))) {
                driver.manage().window().maximize();
            }

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            threadLocalDriver.set(driver);
        }
    }

    /**
     * Đóng driver của luồng hiện tại.
     */
    public static void quitDriver() {
        WebDriver driver = threadLocalDriver.get();
        if (driver != null) {
            log.info("Closing driver for thread: {}", Thread.currentThread().getId());
            driver.quit();
            threadLocalDriver.remove();
        }
    }
}