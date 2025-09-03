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