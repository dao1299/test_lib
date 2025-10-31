//package com.vtnet.netat.driver;
//
//import org.openqa.selenium.MutableCapabilities;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeDriverService;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.edge.EdgeDriver;
//import org.openqa.selenium.edge.EdgeDriverService;
//import org.openqa.selenium.edge.EdgeOptions;
//import org.openqa.selenium.firefox.FirefoxDriver;
//import org.openqa.selenium.firefox.GeckoDriverService;
//import org.openqa.selenium.firefox.FirefoxOptions;
//import org.openqa.selenium.safari.SafariDriver;
//import org.openqa.selenium.safari.SafariOptions;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//
//public class LocalDriverFactory implements IDriverFactory {
//    private static final Logger log = LoggerFactory.getLogger(LocalDriverFactory.class);
//
//    @Override
//    public WebDriver createDriver(String platform, MutableCapabilities capabilities) {
//        setupProxy();
//        log.info("Initializing local driver for platform: {}", platform);
//
//        switch (platform.toLowerCase()) {
//            case "edge":
//                EdgeDriverService edgeService = new EdgeDriverService.Builder()
//                        .usingDriverExecutable(new File(getDriverPath("webdriver.edge.driver")))
//                        .build();
//                return new EdgeDriver(edgeService, (EdgeOptions) capabilities);
//
//            case "firefox":
//                GeckoDriverService geckoService = new GeckoDriverService.Builder()
//                        .usingDriverExecutable(new File(getDriverPath("webdriver.gecko.driver")))
//                        .build();
//                return new FirefoxDriver(geckoService, (FirefoxOptions) capabilities);
//
//            case "safari":
//                // SafariDriver is managed by the OS and doesn't use a separate service
//                return new SafariDriver((SafariOptions) capabilities);
//
//            case "chrome":
//            default:
//                ChromeDriverService chromeService = new ChromeDriverService.Builder()
//                        .usingDriverExecutable(new File(getDriverPath("webdriver.chrome.driver")))
//                        .build();
//                return new ChromeDriver(chromeService, (ChromeOptions) capabilities);
//        }
//    }
//
//    /**
//     * Phương thức trợ giúp để lấy đường dẫn driver từ file config.
//     */
//    private String getDriverPath(String driverPropertyKey) {
//        String driverPath = ConfigReader.getProperty(driverPropertyKey);
//        if (driverPath != null && !driverPath.isEmpty()) {
//            log.info("Using manual driver at: {}", driverPath);
//            return driverPath;
//        } else {
//            // Xử lý UpdateChromeHelper hoặc các cơ chế khác nếu cần
//            if (driverPropertyKey.contains("chrome")) {
//                // Giả định bạn có cơ chế tự động cập nhật
//                String version = new UpdateChromeHelper().updateAutomaticallyChromeDriver();
//                return System.getProperty("user.dir") + "/driver/chromedriver" + version + ".exe";
//            }else if (driverPropertyKey.contains("edge")) {
//                log.info("Edge driver path not specified. Attempting to update automatically...");
//                String version = new UpdateEdgeHelper().updateAutomaticallyEdgeDriver();
//                return System.getProperty("user.dir") + "/driver/msedgedriver" + version + ".exe";
//            }
//            throw new RuntimeException("Cannot find driver path for: " + driverPropertyKey + " in configuration file.");
//        }
//    }
//
//    private void setupProxy() {
//        String proxyHost = ConfigReader.getProperty("proxy.host");
//        String proxyPort = ConfigReader.getProperty("proxy.port");
//
//        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
//            String proxyUrl = proxyHost + ":" + proxyPort;
//            log.info("Configure proxy for driver loading: {}", proxyUrl);
//
//            System.setProperty("https.proxyHost", proxyHost);
//            System.setProperty("https.proxyPort", proxyPort);
//            System.setProperty("http.proxyHost", proxyHost);
//            System.setProperty("http.proxyPort", proxyPort);
//        }
//    }
//}
package com.vtnet.netat.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Proxy;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class LocalDriverFactory implements IDriverFactory {
    private static final Logger log = LoggerFactory.getLogger(LocalDriverFactory.class);

    // --- NEW: cache Chrome service để tránh spawn lại chromedriver ---
    private static volatile ChromeDriverService CHROME_SERVICE;

    @Override
    public WebDriver createDriver(String platform, MutableCapabilities capabilities) {
        setupProxySystemPropsForDownloader(); // hỗ trợ Selenium Manager tải driver qua proxy
        log.info("Initializing local driver for platform: {}", platform);

        switch (platform.toLowerCase()) {
            case "edge": {
                EdgeDriverService edgeService = new EdgeDriverService.Builder()
                        .usingDriverExecutable(new File(getDriverPath("webdriver.edge.driver")))
                        .build();
                return new EdgeDriver(edgeService, (EdgeOptions) capabilities);
            }
            case "firefox": {
                GeckoDriverService geckoService = new GeckoDriverService.Builder()
                        .usingDriverExecutable(new File(getDriverPath("webdriver.gecko.driver")))
                        .build();
                return new FirefoxDriver(geckoService, (FirefoxOptions) capabilities);
            }
            case "safari": {
                return new SafariDriver((SafariOptions) capabilities);
            }
            case "chrome":
            default: {
                ChromeOptions opts = buildChromeOptions(capabilities);
                ChromeDriverService service = getOrCreateChromeService();
                return new ChromeDriver(service, opts);
            }
        }
    }

    // ================== CHROME OPTIMIZATIONS ==================

    /** Khởi tạo/lấy lại ChromeDriverService dùng chung (giảm thời gian spawn). */
    private static ChromeDriverService getOrCreateChromeService() {
        if (CHROME_SERVICE == null) {
            synchronized (LocalDriverFactory.class) {
                if (CHROME_SERVICE == null) {
                    // TỪ SELENIUM 4.6+: KHÔNG set usingDriverExecutable -> Selenium Manager tự tìm/tải
                    ChromeDriverService.Builder builder = new ChromeDriverService.Builder()
                            // không verbose, hạn chế log
                            .withSilent(true);
                    CHROME_SERVICE = builder.build();
                    log.info("ChromeDriverService initialized (Selenium Manager will resolve driver automatically).");
                }
            }
        }
        return CHROME_SERVICE;
    }

    /** Gộp capabilities từ bên ngoài, thêm các flag tăng tốc & proxy chuẩn W3C. */
    private ChromeOptions buildChromeOptions(MutableCapabilities baseCaps) {
        ChromeOptions options = (baseCaps instanceof ChromeOptions)
                ? (ChromeOptions) baseCaps
                : new ChromeOptions().merge(baseCaps);

        // PageLoadStrategy nhanh hơn nếu test không phụ thuộc full load
        String pls = Optional.ofNullable(ConfigReader.getProperty("webdriver.chrome.pageLoadStrategy"))
                .orElse("EAGER");
        try {
            options.setPageLoadStrategy(PageLoadStrategy.valueOf(pls.toUpperCase()));
        } catch (Exception ignored) {
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        }

        // Headless tùy config (new headless ổn định hơn)
        boolean headless = Boolean.parseBoolean(
                Optional.ofNullable(ConfigReader.getProperty("webdriver.chrome.headless")).orElse("false")
        );
        if (headless) {
            // Selenium 4.x tự chọn chế độ headless phù hợp Chrome
            options.addArguments("--headless=new");
        }

        // Tạo profile/cache tạm để khởi động nhanh & cô lập
        Path tempRoot = createTempChromeDirs();
        if (tempRoot != null) {
            options.addArguments("--user-data-dir=" + tempRoot.resolve("profile"));
            options.addArguments("--disk-cache-dir=" + tempRoot.resolve("cache"));
        }

        // Cờ giảm thời gian khởi chạy
        options.addArguments(
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-features=Translate,AutomationControlled,InterestFeed",
                "--disable-background-networking",
                "--disable-background-timer-throttling",
                "--disable-client-side-phishing-detection",
                "--disable-default-apps",
                "--disable-popup-blocking",
                "--disable-renderer-backgrounding",
                "--disable-extensions",
                "--metrics-recording-only",
                "--mute-audio"
        );

        // OS-specific
        if (isLinux()) {
            options.addArguments("--disable-dev-shm-usage", "--no-sandbox");
        }

        // Tùy chọn giảm tải tài nguyên nếu test cho phép:
        boolean blockImages = Boolean.parseBoolean(
                Optional.ofNullable(ConfigReader.getProperty("webdriver.chrome.blockImages")).orElse("false")
        );
        if (blockImages) {
            // Chặn tải ảnh bằng chrome prefs
            options.setExperimentalOption("prefs", java.util.Map.of(
                    "profile.managed_default_content_settings.images", 2
            ));
        }

        // Áp proxy W3C nếu cấu hình
        applyProxyToOptions(options);

        // Hỗ trợ insecure cert nếu cần
        boolean acceptInsecure = Boolean.parseBoolean(
                Optional.ofNullable(ConfigReader.getProperty("webdriver.acceptInsecureCerts")).orElse("true")
        );
        options.setAcceptInsecureCerts(acceptInsecure);

        return options;
    }

    private void applyProxyToOptions(ChromeOptions options) {
        String proxyHost = ConfigReader.getProperty("proxy.host");
        String proxyPort = ConfigReader.getProperty("proxy.port");
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            String http = proxyHost + ":" + proxyPort;
            Proxy proxy = new Proxy();
            proxy.setHttpProxy(http);
            proxy.setSslProxy(http);
            // Nếu cần loại trừ:
            String bypass = ConfigReader.getProperty("proxy.bypass"); // ví dụ: "localhost,127.0.0.1"
            if (bypass != null && !bypass.isEmpty()) proxy.setNoProxy(bypass);
            options.setCapability("proxy", proxy);
            log.info("Applied W3C proxy to ChromeOptions: {}", http);
        }
    }

    private static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nux") || os.contains("nix");
    }

    private Path createTempChromeDirs() {
        try {
            Path root = Files.createTempDirectory("netat-chrome-");
            Files.createDirectories(root.resolve("profile"));
            Files.createDirectories(root.resolve("cache"));
            root.toFile().deleteOnExit();
            return root;
        } catch (Exception e) {
            log.warn("Cannot create temp dirs for Chrome profile/cache: {}", e.getMessage());
            return null;
        }
    }

    private String getDriverPath(String driverPropertyKey) {
        String driverPath = ConfigReader.getProperty(driverPropertyKey);
        if (driverPath != null && !driverPath.isEmpty()) {
            log.info("Using manual driver at: {}", driverPath);
            return driverPath;
        } else {
            if (driverPropertyKey.contains("chrome")) {
                log.info("Chrome will use Selenium Manager (no manual path).");
                return ""; // không dùng.
            } else if (driverPropertyKey.contains("edge")) {
                log.info("Edge driver path not specified. Attempting to update automatically...");
                String version = new UpdateEdgeHelper().updateAutomaticallyEdgeDriver();
                return System.getProperty("user.dir") + "/driver/msedgedriver" + version + ".exe";
            }
            throw new RuntimeException("Cannot find driver path for: " + driverPropertyKey + " in configuration file.");
        }
    }

    /**
     * System props cho đường tải driver (Selenium Manager) đi qua proxy nếu cần.
     * Lưu ý: Proxy chạy web-test thì áp ở ChromeOptions (applyProxyToOptions).
     */
    private void setupProxySystemPropsForDownloader() {
        String proxyHost = ConfigReader.getProperty("proxy.host");
        String proxyPort = ConfigReader.getProperty("proxy.port");
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            String proxyUrl = proxyHost + ":" + proxyPort;
            log.info("Configure proxy for driver downloading: {}", proxyUrl);
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", proxyPort);
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
        }
    }
}
