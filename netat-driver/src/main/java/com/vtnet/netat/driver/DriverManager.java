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
            ConfigReader.loadProperties();

            String platform = ConfigReader.getProperty("platform.name");

            if (platform == null || platform.trim().isEmpty()) {
                throw new IllegalArgumentException("Thuộc tính 'platform.name' không được định nghĩa trong file cấu hình.");
            }

            log.info("Nền tảng thực thi được yêu cầu: {}", platform.toUpperCase());

            IDriverFactory factory;

            switch (platform.toLowerCase()) {
                case "android":
                case "ios":
                    factory = new MobileDriverFactory();
                    break;
                case "chrome":
                case "firefox":
                case "edge":
                    // [TINH CHỈNH 2] Không cần set 'browser.name' nữa, các factory sẽ đọc trực tiếp 'platform.name'
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

            if (!(platform.equalsIgnoreCase("android") || platform.equalsIgnoreCase("ios"))) {
                driver.manage().window().maximize();
            }

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            threadLocalDriver.set(driver);
        }
    }

    public static void initDriver(String platform) {
        if (threadLocalDriver.get() == null) {
            ConfigReader.loadProperties();

            if (platform == null || platform.trim().isEmpty()) {
                throw new IllegalArgumentException("Tên platform không được để trống khi khởi tạo driver.");
            }

            log.info("Nền tảng thực thi được yêu cầu cho luồng này: {}", platform.toUpperCase());

            // Ghi đè hoặc thiết lập thuộc tính platform.name cho luồng này
            ConfigReader.getProperties().setProperty("platform.name", platform);

            IDriverFactory factory;

            switch (platform.toLowerCase()) {
                case "android":
                case "ios":
                    factory = new MobileDriverFactory();
                    break;
                case "chrome":
                case "firefox":
                case "edge":
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

            if (!(platform.equalsIgnoreCase("android") || platform.equalsIgnoreCase("ios"))) {
                driver.manage().window().maximize();
            }

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

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