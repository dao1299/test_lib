package com.vtnet.netat.driver;

import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocalDriverFactory implements IDriverFactory {
    private static final Logger log = LoggerFactory.getLogger(LocalDriverFactory.class);
    @Override
    public WebDriver createDriver() {
        setupProxy();
        String version,driverPath;
        // Giả sử đã có phương thức này, bạn cần hiện thực hóa nó
        // updateChromeDriver();
//        String version = new UpdateChromeHelper().updateAutomaticallyChromeDriver();
//        String driverPath = System.getProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "\\driver\\chromedriver" + version + ".exe");\
        String browser = ConfigReader.getProperty("platform.name");
        log.info("Khởi tạo driver local cho nền tảng: {}", browser);

        // --- PHẦN ĐƯỢC CẬP NHẬT VÀ TÁI CẤU TRÚC ---

        // 1. Xác định đúng key system property cho từng trình duyệt
        String driverPropertyKey;
        switch (browser.toLowerCase()) {
            case "firefox":
                driverPropertyKey = "webdriver.gecko.driver";
                break;
            case "chrome":
                driverPropertyKey = "webdriver.chrome.driver";
                break;
            case "edge":
                driverPropertyKey = "webdriver.edge.driver";
                break;
            default:
                // Giữ lại cho các trường hợp khác nếu có
                driverPropertyKey = "webdriver." + browser + ".driver";
                break;
        }

        // 2. Xử lý driver path thủ công hoặc gọi updateChromeDriver
        driverPath = ConfigReader.getProperty(driverPropertyKey);
        String company = ConfigReader.getProperty("company","");
        if (driverPath != null && !driverPath.isEmpty()) {
            System.setProperty(driverPropertyKey, driverPath);
            log.info("Sử dụng driver thủ công tại: {}", driverPath);
        } else {
            // Nếu là Chrome và không có driver path, gọi phương thức cập nhật
            if ("chrome".equalsIgnoreCase(browser) && company.equalsIgnoreCase("TTCDS")) {
                log.info("Không tìm thấy cấu hình cho '{}', gọi phương thức updateChromeDriver()...", driverPropertyKey);
                // Giả định bạn có một phương thức updateChromeDriver() ở đâu đó
                // updateChromeDriver();
                version = new UpdateChromeHelper().updateAutomaticallyChromeDriver();
                driverPath = System.getProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "\\driver\\chromedriver" + version + ".exe");
                System.setProperty(driverPropertyKey, driverPath);
                log.info("Sử dụng driver thủ công thông qua cài trực tiếp từ website tại: {}", driverPath);
            }else{
                log.info("Không có đường dẫn driver thủ công, Selenium Manager sẽ tự động xử lý.");
            }

        }

        // --- KẾT THÚC PHẦN CẬP NHẬT ---

        MutableCapabilities capabilities = CapabilityFactory.getCapabilities(browser);

        switch (browser.toLowerCase()) {
            case "edge":
                return new EdgeDriver((EdgeOptions) capabilities);
            case "firefox":
                return new FirefoxDriver((FirefoxOptions) capabilities);
            case "chrome":
            default:
                return new ChromeDriver((ChromeOptions) capabilities);
        }
    }

    private void setupProxy() {
        String proxyHost = ConfigReader.getProperty("proxy.host");
        String proxyPort = ConfigReader.getProperty("proxy.port");

        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            String proxyUrl = proxyHost + ":" + proxyPort;
            log.info("Cấu hình proxy cho việc tải driver: {}", proxyUrl);

            // Thiết lập các system property cho Java
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", proxyPort);
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
        }
    }
}