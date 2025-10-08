package com.vtnet.netat.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LocalDriverFactory implements IDriverFactory {
    private static final Logger log = LoggerFactory.getLogger(LocalDriverFactory.class);

    @Override
    public WebDriver createDriver(String platform, MutableCapabilities capabilities) {
        setupProxy();
        log.info("Initializing local driver for platform: {}", platform);

        switch (platform.toLowerCase()) {
            case "edge":
                EdgeDriverService edgeService = new EdgeDriverService.Builder()
                        .usingDriverExecutable(new File(getDriverPath("webdriver.edge.driver")))
                        .build();
                return new EdgeDriver(edgeService, (EdgeOptions) capabilities);

            case "firefox":
                GeckoDriverService geckoService = new GeckoDriverService.Builder()
                        .usingDriverExecutable(new File(getDriverPath("webdriver.gecko.driver")))
                        .build();
                return new FirefoxDriver(geckoService, (FirefoxOptions) capabilities);

            case "safari":
                // SafariDriver is managed by the OS and doesn't use a separate service
                return new SafariDriver((SafariOptions) capabilities);

            case "chrome":
            default:
                ChromeDriverService chromeService = new ChromeDriverService.Builder()
                        .usingDriverExecutable(new File(getDriverPath("webdriver.chrome.driver")))
                        .build();
                return new ChromeDriver(chromeService, (ChromeOptions) capabilities);
        }
    }

    /**
     * Phương thức trợ giúp để lấy đường dẫn driver từ file config.
     */
    private String getDriverPath(String driverPropertyKey) {
        String driverPath = ConfigReader.getProperty(driverPropertyKey);
        if (driverPath != null && !driverPath.isEmpty()) {
            log.info("Using manual driver at: {}", driverPath);
            return driverPath;
        } else {
            // Xử lý UpdateChromeHelper hoặc các cơ chế khác nếu cần
            if (driverPropertyKey.contains("chrome")) {
                // Giả định bạn có cơ chế tự động cập nhật
                String version = new UpdateChromeHelper().updateAutomaticallyChromeDriver();
                return System.getProperty("user.dir") + "/driver/chromedriver" + version + ".exe";
            }else if (driverPropertyKey.contains("edge")) {
                log.info("Edge driver path not specified. Attempting to update automatically...");
                String version = new UpdateEdgeHelper().updateAutomaticallyEdgeDriver();
                return System.getProperty("user.dir") + "/driver/msedgedriver" + version + ".exe";
            }
            throw new RuntimeException("Cannot find driver path for: " + driverPropertyKey + " in configuration file.");
        }
    }

    private void setupProxy() {
        String proxyHost = ConfigReader.getProperty("proxy.host");
        String proxyPort = ConfigReader.getProperty("proxy.port");

        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            String proxyUrl = proxyHost + ":" + proxyPort;
            log.info("Configure proxy for driver loading: {}", proxyUrl);

            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", proxyPort);
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
        }
    }
}