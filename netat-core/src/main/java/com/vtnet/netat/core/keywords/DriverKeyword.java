package com.vtnet.netat.core.keywords;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.driver.*;
import io.qameta.allure.Step;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keywords for managing Driver and Session.
 * <p>
 * Improvements:
 * - Unified usage of MobileDriverFactory for all mobile sessions
 * - Input validation with clear error messages
 * - Ensures consistent capabilities across methods
 */
public class DriverKeyword extends BaseKeyword {

    // Supported platforms for validation
    private static final List<String> WEB_PLATFORMS = Arrays.asList("chrome", "firefox", "edge", "safari");
    private static final List<String> MOBILE_PLATFORMS = Arrays.asList("android", "ios");

    // ==================== WEB BROWSER METHODS ====================

    @NetatKeyword(
            name = "startBrowser",
            description = "Khởi tạo và mở một phiên trình duyệt WEB mới. Nếu không có tham số, sẽ sử dụng trình duyệt mặc định trong file cấu hình.",
            category = "Session",
            subCategory = "Lifecycle",
            example = "driverKeyword.startBrowser();"
    )
    @Step("Start default browser")
    public void startBrowser() {
        execute(() -> {
            DriverManager.initDriver();
            return null;
        });
    }

    @NetatKeyword(
            name = "startBrowser",
            description = "Khởi tạo và mở một phiên trình duyệt WEB cụ thể.",
            category = "Session",
            subCategory = "Lifecycle",
            parameters = {
                    "platform: String - Tên nền tảng WEB cần khởi tạo (ví dụ: 'chrome', 'firefox')."
            },
            example = "driverKeyword.startBrowser(\"firefox\");",
            note = "Keyword này chỉ dành cho các nền tảng Web. Để khởi động ứng dụng di động, vui lòng sử dụng 'startApplicationByPath' hoặc 'startApplicationByPackage'."
    )
    @Step("Start browser: {0}")
    public void startBrowser(String platform) {
        execute(() -> {
            if (platform == null || platform.trim().isEmpty()) {
                throw new IllegalArgumentException("Platform name cannot be null or empty for startBrowser.");
            }
            String lowerCasePlatform = platform.toLowerCase();
            if (MOBILE_PLATFORMS.contains(lowerCasePlatform)) {
                throw new IllegalArgumentException("Invalid platform for startBrowser. '" + platform + "' is a mobile platform. Please use startApplicationByPath or startApplicationByPackage instead.");
            }
            DriverManager.initDriver(platform, null);
            return null;
        }, platform);
    }

    // ==================== MOBILE APPLICATION METHODS ====================

    @NetatKeyword(
            name = "startApplicationByPath",
            description = "Cài đặt (nếu cần) và khởi chạy ứng dụng từ một đường dẫn file .apk hoặc .ipa.",
            category = "Session",
            subCategory = "Lifecycle",
            parameters = {
                    "platformName: String - Tên nền tảng di động ('android', 'ios').",
                    "appPath: String - Đường dẫn tuyệt đối đến file ứng dụng."
            },
            example = "driverKeyword.startApplicationByPath(\"android\", \"C:/builds/app-debug.apk\");",
            note = "Keyword này chỉ dành cho các nền tảng Mobile. Để khởi động trình duyệt web, vui lòng sử dụng 'startBrowser'."
    )
    @Step("Start application on {0} from path: {1}")
    public void startApplicationByPath(String platformName, String appPath) {
        execute(() -> {
            if (platformName == null || platformName.trim().isEmpty()) {
                throw new IllegalArgumentException("Platform name cannot be null or empty for startApplicationByPath.");
            }
            String lowerCasePlatform = platformName.toLowerCase();
            if (WEB_PLATFORMS.contains(lowerCasePlatform)) {
                throw new IllegalArgumentException("Invalid platform for startApplicationByPath. '" + platformName + "' is a web platform. Please use startBrowser instead.");
            }
            Map<String, Object> options = new HashMap<>();
            options.put("app", appPath);
            DriverManager.initDriver(platformName, options);
            return null;
        }, platformName, appPath);
    }

    @NetatKeyword(
            name = "startApplicationByPackage",
            description = "Khởi chạy một ứng dụng đã được cài đặt sẵn trên thiết bị bằng appPackage và appActivity.",
            category = "Session",
            subCategory = "Lifecycle",
            parameters = {
                    "platformName: String - Tên nền tảng di động ('android', 'ios').",
                    "appPackage: String - Tên package của ứng dụng.",
                    "appActivity: String - Tên activity chính để khởi chạy."
            },
            example = "driverKeyword.startApplicationByPackage(\"android\", \"com.android.settings\", \".Settings\");",
            note = "Keyword này chỉ dành cho các nền tảng Mobile. Để khởi động trình duyệt web, vui lòng sử dụng 'startBrowser'."
    )
    @Step("Start application on {0} with package '{1}'")
    public void startApplicationByPackage(String platformName, String appPackage, String appActivity) {
        execute(() -> {
            if (platformName == null || platformName.trim().isEmpty()) {
                throw new IllegalArgumentException("Platform name cannot be null or empty for startApplicationByPackage.");
            }
            String lowerCasePlatform = platformName.toLowerCase();
            if (WEB_PLATFORMS.contains(lowerCasePlatform)) {
                throw new IllegalArgumentException("Invalid platform for startApplicationByPackage. '" + platformName + "' is a web platform. Please use startBrowser instead.");
            }
            Map<String, Object> options = new HashMap<>();
            options.put("appium.appPackage", appPackage);
            options.put("appium.appActivity", appActivity);
            DriverManager.initDriver(platformName, options);
            return null;
        }, platformName, appPackage, appActivity);
    }

    @NetatKeyword(
            name = "startApplication",
            description = "Khởi chạy một ứng dụng di động dựa trên các thông số đã được định nghĩa trong file cấu hình (profile).",
            category = "Session",
            subCategory = "Lifecycle",
            parameters = {
                    "platformName: String - Tên nền tảng di động ('android', 'ios'). Framework sẽ đọc file config tương ứng."
            },
            example = "driverKeyword.startApplication(\"android\");",
            note = "Đây là keyword tiêu chuẩn để bắt đầu một phiên làm việc mobile với cấu hình đã định sẵn (cấu hình trong default.properties)"
    )
    @Step("Start application with default configuration for {0}")
    public void startApplication(String platformName) {
        execute(() -> {
            if (platformName == null || platformName.trim().isEmpty()) {
                throw new IllegalArgumentException("Platform name cannot be null or empty for startApplication.");
            }
            String lowerCasePlatform = platformName.toLowerCase();
            if (WEB_PLATFORMS.contains(lowerCasePlatform)) {
                throw new IllegalArgumentException("Invalid platform for startApplication. '" + platformName + "' is a web platform. Please use startBrowser instead.");
            }
            DriverManager.initDriver(platformName, new HashMap<>());
            return null;
        }, platformName);
    }

    // ==================== SESSION MANAGEMENT - WEB ====================

    @NetatKeyword(
            name = "startSession",
            description = "Khởi tạo nhanh một phiên làm việc trên trình duyệt web bằng cách cung cấp thông tin trực tiếp.",
            category = "Driver",
            subCategory = "Session",
            parameters = {
                    "sessionName: String - Tên định danh duy nhất cho phiên làm việc này (ví dụ: 'user_A')",
                    "browserName: String - Tên trình duyệt cần khởi động (ví dụ: 'chrome', 'firefox', 'edge')"
            },
            example = "// Bắt đầu nhanh một phiên trên Firefox cho User B\n" +
                    "driver.startSession(\"user_B_ff\", \"firefox\");",
            note = "Hữu ích cho các kịch bản test nhanh hoặc khi không muốn tạo file profile. Sử dụng 'switchSession' để điều khiển phiên này."
    )
    @Step("Start Browser Session: {0} ({1})")
    public void startSession(String sessionName, String browserName) {
        execute(() -> {
            // Validation
            validateSessionName(sessionName);
            validateWebPlatform(browserName, "startSession");

            String executionType = ConfigReader.getProperty("execution.type", "local");
            logger.info("execution type: " + executionType);

            IDriverFactory factory = "remote".equalsIgnoreCase(executionType)
                    ? new RemoteDriverFactory()
                    : new LocalDriverFactory();

            MutableCapabilities capabilities = CapabilityFactory.getCapabilities(browserName, null);
            WebDriver driver = factory.createDriver(browserName, capabilities);
            SessionManager.getInstance().addSession(sessionName, driver);
            return null;
        }, sessionName, browserName);
    }

    // ==================== SESSION MANAGEMENT - MOBILE (APP PATH) ====================

    @NetatKeyword(
            name = "startSession",
            description = "Khởi tạo nhanh một phiên làm việc trên thiết bị di động bằng cách cung cấp các thông tin cơ bản.",
            category = "Driver",
            subCategory = "Session",
            parameters = {
                    "sessionName: String - Tên định danh duy nhất cho phiên làm việc",
                    "platformName: String - Nền tảng 'Android' hoặc 'iOS'",
                    "udid: String - UDID của thiết bị (thật hoặc ảo)",
                    "appPath: String - Đường dẫn tuyệt đối đến file .apk hoặc .ipa",
                    "appiumServerUrl: String - URL của Appium Server (ví dụ: 'http://127.0.0.1:4723/')",
                    "automationName: String - Tên của driver sử dụng, với Android thì UiAutomator2, iOS thì XCUITest"
            },
            example = "// Bắt đầu phiên trên thiết bị Android của User A\n" +
                    "driver.startSession(\"user_A_android\", \"Android\", \"emulator-5554\", \"/path/to/app.apk\", \"http://127.0.0.1:4723/\", \"UiAutomator2\");",
            note = "Đây là cách đơn giản nhất để khởi động một thiết bị di động mà không cần tạo file profile phức tạp."
    )
    @Step("Start Mobile Session: {0} on {1} device {2}")
    public void startSession(String sessionName, String platformName, String udid, String appPath, String appiumServerUrl, String automationName) {
        execute(() -> {
            // Validation
            validateSessionName(sessionName);
            validateMobilePlatform(platformName, "startSession");
            validateUdid(udid);
            validateAppPath(appPath);
            validateAppiumUrl(appiumServerUrl);

            MutableCapabilities caps = CapabilityFactory.buildMobileCapabilities(platformName, udid, automationName);
            CapabilityFactory.setAppPath(caps, appPath);

            MobileDriverFactory factory = new MobileDriverFactory();
            WebDriver driver = factory.createDriver(platformName, caps, appiumServerUrl);

            SessionManager.getInstance().addSession(sessionName, driver);
            return null;
        }, sessionName, platformName, udid, appPath, appiumServerUrl);
    }

    // ==================== SESSION MANAGEMENT - MOBILE (APP PACKAGE) ====================

    @NetatKeyword(
            name = "startSession",
            description = "Khởi tạo nhanh một phiên làm việc trên thiết bị di động bằng cách cung cấp appPackage và appActivity.",
            category = "Driver",
            subCategory = "Session",
            parameters = {
                    "sessionName: String - Tên định danh duy nhất cho phiên làm việc",
                    "platformName: String - Nền tảng 'Android' hoặc 'iOS'",
                    "udid: String - UDID của thiết bị (thật hoặc ảo)",
                    "appPackage: String - Tên package của ứng dụng (ví dụ: 'com.android.settings')",
                    "appActivity: String - Tên activity chính (ví dụ: '.Settings')",
                    "appiumServerUrl: String - URL của Appium Server (ví dụ: 'http://127.0.0.1:4723/')",
                    "automationName: String - Tên của driver sử dụng, với Android thì UiAutomator2, iOS thì XCUITest"
            },
            example = "// Bắt đầu phiên trên thiết bị Android của User A với app đã cài sẵn\n" +
                    "driver.startSession(\"user_A_android\", \"Android\", \"emulator-5554\", \"com.android.settings\", \".Settings\", \"http://127.0.0.1:4723/\", \"UiAutomator2\");",
            note = "Sử dụng khi ứng dụng đã được cài đặt sẵn trên thiết bị."
    )
    @Step("Start Mobile Session: {0} on {1} device {2}")
    public void startSession(String sessionName, String platformName, String udid, String appPackage, String appActivity, String appiumServerUrl, String automationName) {
        execute(() -> {
            // Validation
            validateSessionName(sessionName);
            validateMobilePlatform(platformName, "startSession");
            validateUdid(udid);
            validateAppPackage(appPackage);
            validateAppiumUrl(appiumServerUrl);

            // Sử dụng CapabilityFactory để tạo capabilities nhất quán
            MutableCapabilities caps = CapabilityFactory.buildMobileCapabilities(platformName, udid, automationName);
            CapabilityFactory.setAppPackage(caps, appPackage, appActivity);

            // Sử dụng MobileDriverFactory với health check và retry
            MobileDriverFactory factory = new MobileDriverFactory();
            WebDriver driver = factory.createDriver(platformName, caps, appiumServerUrl);

            SessionManager.getInstance().addSession(sessionName, driver);
            return null;
        }, sessionName, platformName, udid, appPackage, appActivity, appiumServerUrl);
    }

    // ==================== SESSION CONTROL ====================

    @NetatKeyword(
            name = "switchSession",
            description = "Chuyển quyền điều khiển sang một phiên làm việc đã được khởi tạo. Tất cả các keyword UI sau lệnh này sẽ được thực thi trên phiên được chỉ định.",
            category = "Driver",
            subCategory = "Session",
            parameters = {"sessionName: String - Tên của phiên làm việc cần chuyển đến"},
            example = "// Sau khi User A gửi tin nhắn, chuyển sang User B để kiểm tra\n" +
                    "driver.switchSession(\"user_B_ff\");\n" +
                    "web.waitForElementVisible(\"notificationPopup\", 15);",
            note = "Đây là keyword then chốt để điều khiển nhiều người dùng trong cùng một kịch bản."
    )
    @Step("Switch active session to: {0}")
    public void switchSession(String sessionName) {
        execute(() -> {
            validateSessionName(sessionName);
            WebDriver driver = SessionManager.getInstance().getSession(sessionName);
            if (driver == null) {
                throw new IllegalStateException(
                        "Session '" + sessionName + "' not found. " +
                                "Make sure you have created it with startSession() first.");
            }

            if (driver instanceof org.openqa.selenium.remote.RemoteWebDriver) {
                org.openqa.selenium.remote.RemoteWebDriver remoteDriver =
                        (org.openqa.selenium.remote.RemoteWebDriver) driver;
                if (remoteDriver.getSessionId() == null) {
                    // Remove dead session from manager
                    logger.error("Session '{}' has been terminated by the server", sessionName);
                    throw new IllegalStateException(
                            "Session '" + sessionName + "' has been terminated by the server. " +
                                    "This may be due to session timeout or server disconnection. " +
                                    "Please create a new session.");
                }
            }
            SessionManager.getInstance().switchSession(sessionName);
            return null;
        }, sessionName);
    }

    @NetatKeyword(
            name = "stopAllSessions",
            description = "Đóng tất cả các trình duyệt và phiên kết nối thiết bị đã được mở bởi các keyword 'startSession'.",
            category = "Driver",
            subCategory = "Session",
            example = "// Đặt ở cuối kịch bản hoặc trong khối @AfterClass để dọn dẹp\n" +
                    "driver.stopAllSessions();",
            note = "Luôn luôn gọi keyword này để đảm bảo không có tiến trình nào bị treo sau khi kiểm thử kết thúc."
    )
    @Step("Stop all active sessions")
    public void stopAllSessions() {
        execute(() -> {
            SessionManager.getInstance().stopAllSessions();
            return null;
        });
    }

    @NetatKeyword(
            name = "closeSession",
            description = "Đóng hoàn toàn phiên làm việc hiện tại (cả trình duyệt web và ứng dụng di động) và giải phóng tài nguyên.",
            category = "Session",
            subCategory = "Lifecycle"
    )
    @Step("Close session (browser/device)")
    public void closeSession() {
        execute(() -> {
            DriverManager.quitDriver();
            return null;
        });
    }

    // ==================== VALIDATION HELPER METHODS ====================

    /**
     * Validate session name is not null or empty
     */
    private void validateSessionName(String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Session name cannot be empty. " +
                            "Please provide an identifier for the session (e.g., 'user_A', 'device_1').");
        }
    }

    /**
     * Validate platform is a web platform
     */
    private void validateWebPlatform(String platform, String methodName) {
        if (platform == null || platform.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Platform cannot be empty for " + methodName + ". " +
                            "Please specify one of: " + WEB_PLATFORMS);
        }
        if (MOBILE_PLATFORMS.contains(platform.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Platform '" + platform + "' is a mobile platform, not suitable for " + methodName + ". " +
                            "Please use startSession with full mobile parameters or startApplicationByPath/startApplicationByPackage.");
        }
    }

    /**
     * Validate platform is a mobile platform
     */
    private void validateMobilePlatform(String platform, String methodName) {
        if (platform == null || platform.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Platform cannot be empty for " + methodName + ". " +
                            "Please specify 'android' or 'ios'.");
        }
        String lowerPlatform = platform.toLowerCase().trim();
        if (!MOBILE_PLATFORMS.contains(lowerPlatform)) {
            throw new IllegalArgumentException(
                    "Platform '" + platform + "' is not valid for mobile. " +
                            "Only 'android' and 'ios' are supported. " +
                            "If you want to test web, please use startSession(sessionName, browserName).");
        }
    }

    /**
     * Validate device UDID
     */
    private void validateUdid(String udid) {
        if (udid == null || udid.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Device UDID cannot be empty. " +
                            "To get UDID:\n" +
                            "  - Android: run 'adb devices'\n" +
                            "  - iOS: run 'xcrun xctrace list devices' or check in Xcode");
        }
    }

    /**
     * Validate app path
     */
    private void validateAppPath(String appPath) {
        if (appPath == null || appPath.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Application path (appPath) cannot be empty. " +
                            "Please provide absolute path to .apk (Android) or .ipa/.app (iOS) file.");
        }

        // Only warn if file doesn't exist (could be remote path)
        java.io.File appFile = new java.io.File(appPath);
        if (!appFile.exists() && !appPath.startsWith("http")) {
            logger.warn("Warning: Application file not found at: {}. " +
                    "Make sure the path is correct or the file has been copied to that location.", appPath);
        }
    }

    /**
     * Validate app package
     */
    private void validateAppPackage(String appPackage) {
        if (appPackage == null || appPackage.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "App package cannot be empty. " +
                            "Example: 'com.android.settings', 'com.example.myapp'");
        }
    }

    /**
     * Validate Appium server URL
     */
    private void validateAppiumUrl(String appiumServerUrl) {
        if (appiumServerUrl == null || appiumServerUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Appium server URL cannot be empty. " +
                            "Default: 'http://127.0.0.1:4723/' or 'http://localhost:4723/'");
        }

        try {
            new java.net.URL(appiumServerUrl);
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException(
                    "Invalid Appium server URL: '" + appiumServerUrl + "'. " +
                            "URL must be in format 'http://host:port/' (e.g., http://127.0.0.1:4723/)");
        }
    }

    @NetatKeyword(
            name = "startMobileSession",
            description = "Khởi tạo phiên kiểm thử trên thiết bị di động (Android/iOS).\n" +
                    "Hỗ trợ cả Local Appium và Device Farm.\n\n" +
                    "capsJson hỗ trợ:\n" +
                    "- Appium capabilities: appium:noReset, appium:app, ...\n" +
                    "- Appium settings: appium:settings:{waitForIdleTimeout:0,...}\n" +
                    "- Device Farm: df:app, df:tags, df:recordVideo, ...",
            category = "Driver",
            subCategory = "Session",
            parameters = {
                    "sessionName: String - Tên định danh duy nhất cho phiên",
                    "platform: String - 'android' hoặc 'ios'",
                    "udid: String - UDID thiết bị (để trống/'auto' để Device Farm tự chọn, hoặc ${ENV_VAR})",
                    "appiumUrl: String - URL của Appium Server hoặc Device Farm",
                    "capsJson: String - JSON capabilities (có thể chứa appium:settings và df:*)"
            },
            returnValue = "void",
            example = "// === Local Appium với app path ===\n" +
                    "startMobileSession | session1 | android | emulator-5554 | http://127.0.0.1:4723/ | {\"appium:app\":\"/path/app.apk\"} |\n\n" +
                    "// === Với Appium Settings (tối ưu performance) ===\n" +
                    "startMobileSession | session1 | android | emulator-5554 | http://127.0.0.1:4723/ | " +
                    "{\"appium:app\":\"/path/app.apk\",\"appium:settings\":{\"waitForIdleTimeout\":0,\"waitForSelectorTimeout\":1000}} |\n\n" +
                    "// === Device Farm ===\n" +
                    "startMobileSession | df_session | android | auto | https://df.company.com/wd/hub | " +
                    "{\"df:app\":\"app-123\",\"df:tags\":[\"smoke\"],\"df:recordVideo\":true} |",
            note = "UDID có thể: 'auto' (Device Farm tự chọn), ${ENV_VAR} (đọc từ biến môi trường), hoặc giá trị cụ thể.\n" +
                    "appium:settings dùng để tối ưu tốc độ test (waitForIdleTimeout, waitForSelectorTimeout, ...)."
    )
    @Step("Start Mobile Session: {0} on {1}")
    public void startMobileSession(String sessionName, String platform, String udid,
                                   String appiumUrl, String capsJson) {
        execute(() -> {
            // Validation
            validateSessionName(sessionName);
            validateMobilePlatform(platform, "startMobileSession");
            validateAppiumUrl(appiumUrl);

            // Resolve UDID (hỗ trợ auto, ${ENV_VAR}, hoặc giá trị cụ thể)
            String resolvedUdid = resolveUdid(udid);

            // Build capabilities với xử lý appium:settings
            MutableCapabilities caps = buildMobileCapabilities(platform, resolvedUdid, capsJson);

            logger.info("Starting mobile session '{}' | Platform: {} | UDID: {} | URL: {}",
                    sessionName, platform, resolvedUdid != null ? resolvedUdid : "AUTO", appiumUrl);

            // Create driver
            MobileDriverFactory factory = new MobileDriverFactory();
            WebDriver driver = factory.createDriver(platform, caps, appiumUrl);

            // Register session
            SessionManager.getInstance().addSession(sessionName, driver);
            logger.info("Mobile session '{}' created successfully", sessionName);

            return null;
        }, sessionName, platform, udid, appiumUrl, capsJson);
    }

    @NetatKeyword(
            name = "startMobileSession",
            description = "Khởi tạo phiên mobile với cấu hình mặc định (không có capabilities bổ sung).",
            category = "Driver",
            subCategory = "Session",
            parameters = {
                    "sessionName: String - Tên định danh cho phiên",
                    "platform: String - 'android' hoặc 'ios'",
                    "udid: String - UDID của thiết bị",
                    "appiumUrl: String - URL của Appium Server"
            },
            example = "startMobileSession | session1 | android | emulator-5554 | http://127.0.0.1:4723/ |"
    )
    @Step("Start Mobile Session: {0} on {1} device {2}")
    public void startMobileSession(String sessionName, String platform, String udid, String appiumUrl) {
        startMobileSession(sessionName, platform, udid, appiumUrl, "{}");
    }

    private MutableCapabilities buildMobileCapabilities(String platform, String udid, String capsJson) {
        String automationName = "android".equalsIgnoreCase(platform) ? "UiAutomator2" : "XCUITest";

        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("platformName", platform);
        caps.setCapability("appium:automationName", automationName);

        if (udid != null && !udid.isEmpty()) {
            caps.setCapability("appium:udid", udid);
        }

        caps.setCapability("appium:noReset", true);
        caps.setCapability("appium:autoGrantPermissions", true);
        caps.setCapability("appium:newCommandTimeout", 300);

        if (capsJson != null && !capsJson.trim().isEmpty() && !capsJson.trim().equals("{}")) {
            Map<String, Object> customCaps = parseCapabilitiesJson(capsJson);
            Map<String, Object> settingsMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : customCaps.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();


                if (key.startsWith("appium:settings[") && key.endsWith("]")) {
                    String settingName = key.substring("appium:settings[".length(), key.length() - 1);
                    settingsMap.put(settingName, value);
                    logger.debug("Add setting: {} = {}", settingName, value);
                } else if ("appium:settings".equals(key) && value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> settingsFromJson = (Map<String, Object>) value;
                    settingsMap.putAll(settingsFromJson);
                    logger.debug("Merge settings map: {}", settingsFromJson);
                } else {
                    caps.setCapability(key, value);
                    logger.debug("Set capability: {} = {}", key, value);
                }
            }

            if (!settingsMap.isEmpty()) {
                caps.setCapability("appium:settings", settingsMap);
                logger.info("Applied appium:settings: {}", settingsMap);
            }
        }

        return caps;
    }

    private String resolveUdid(String udid) {
        if (udid == null || udid.trim().isEmpty()) {
            return null;
        }

        String trimmed = udid.trim();

        if ("auto".equalsIgnoreCase(trimmed)) {
            logger.info("UDID set to 'auto' - Device Farm will allocate available device");
            return null;
        }

        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            String varName = trimmed.substring(2, trimmed.length() - 1);

            String value = System.getProperty(varName);

            if (value == null || value.isEmpty()) {
                value = System.getenv(varName);
            }

            if (value == null || value.isEmpty()) {
                logger.warn("Environment variable '{}' not found. UDID will be null (auto-allocate)", varName);
                return null;
            }

            logger.info("Resolved UDID from environment: {} = {}", varName, value);
            return value;
        }

        return trimmed;
    }

    private Map<String, Object> parseCapabilitiesJson(String capsJson) {
        if (capsJson == null || capsJson.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(capsJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            logger.error("Failed to parse capabilities JSON: {}", capsJson, e);
            throw new IllegalArgumentException(
                    "Invalid capabilities JSON format: " + e.getMessage() + "\n" +
                            "Expected format: {\"appium:noReset\":true,\"appium:settings\":{\"waitForIdleTimeout\":0}}\n" +
                            "Received: " + capsJson
            );
        }
    }

    public void closeSession(String sessionName) {
        execute(() -> {
            SessionManager.getInstance().stopSession(sessionName);
            return null;
        }, sessionName);
    }
}