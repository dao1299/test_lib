package com.vtnet.netat.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Facade cho khởi tạo/đóng driver; CHỈ gọi một chiều sang SessionManager.
 */
public final class DriverManager {
    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);

    private DriverManager() {}

    /** Lấy driver của phiên hiện tại (có thể null nếu chưa init) */
    public static WebDriver getDriver() {
        return SessionManager.getInstance().getCurrentDriver();
    }

    /** Khởi tạo phiên mặc định theo properties */
    public static void initDriver() {
        ConfigReader.loadProperties();
        String platform = ConfigReader.getProperty("platform.name");
        if (platform == null || platform.trim().isEmpty()) {
            throw new IllegalArgumentException("Property 'platform.name' is not defined.");
        }
        initDriver(platform, null);
    }

    /**
     * Khởi tạo phiên mặc định theo platform & capabilities.
     * Nếu phiên "default" chưa tồn tại, tạo mới và switch về "default".
     */
    public static void initDriver(String platform, Map<String, Object> overrideCapabilities) {
        if (SessionManager.getInstance().getSession(SessionManager.DEFAULT_SESSION) == null) {
            ConfigReader.loadProperties();
            log.info("Initializing default session for platform: {}", platform);

            IDriverFactory factory;
            switch (platform.toLowerCase()) {
                case "android":
                case "ios":
                    factory = new MobileDriverFactory();
                    break;
                case "chrome":
                case "firefox":
                case "edge":
                case "safari": {
                    String executionType = ConfigReader.getProperty("execution.type", "local");
                    factory = "remote".equalsIgnoreCase(executionType)
                            ? new RemoteDriverFactory()
                            : new LocalDriverFactory();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Platform not supported: " + platform);
            }

            MutableCapabilities caps = CapabilityFactory.getCapabilities(platform, overrideCapabilities);
            WebDriver driver = factory.createDriver(platform, caps);

            SessionManager sm = SessionManager.getInstance();
            sm.addSession(SessionManager.DEFAULT_SESSION, driver);
            sm.switchSession(SessionManager.DEFAULT_SESSION);
        }
    }

    /** Đóng TẤT CẢ phiên của thread hiện tại (uỷ quyền 1 chiều) */
    public static void quit() {
        log.info("Closing all driver sessions for thread: {}", Thread.currentThread().getId());
        SessionManager.getInstance().stopAllSessions();
    }

    /** Tương thích ngược */
    public static void quitDriver() {
        quit();
    }
}
