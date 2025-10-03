package com.vtnet.netat.core.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.driver.*;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverKeyword extends BaseKeyword {

    // Danh sách các nền tảng được hỗ trợ để kiểm tra
    private static final List<String> WEB_PLATFORMS = Arrays.asList("chrome", "firefox", "edge", "safari");
    private static final List<String> MOBILE_PLATFORMS = Arrays.asList("android", "ios");

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

    @NetatKeyword(
            name = "startSessionFromProfile",
            description = "Khởi tạo một phiên làm việc (trình duyệt hoặc thiết bị di động) từ một file profile đã được định nghĩa sẵn.",
            category = "Driver",
            subCategory = "Session",
            parameters = {
                    "profileName: String - Tên của file profile (không có đuôi .json) nằm trong thư mục 'browser_profiles' hoặc 'device_profiles'"
            },
            example = "// Bắt đầu phiên làm việc cho User A trên Chrome từ file profile\n" +
                    "driver.startSessionFromProfile(\"user_A_chrome\");",
            note = "Đây là phương pháp được khuyên dùng để quản lý các môi trường kiểm thử một cách nhất quán và có thể tái sử dụng."
    )
    @Step("Start session from profile: {0}")
    public void startSession(String profileName) {
        execute(() -> {
            // Logic để đọc file profile và khởi tạo driver tương ứng
            // (Phần này cần được cài đặt trong các lớp Factory và SessionManager)
            // Ví dụ:
            // WebDriver driver = DriverFactory.createFromProfile(profileName);
            // SessionManager.getInstance().addSession(profileName, driver);
            return null;
        }, profileName);
    }

    @NetatKeyword(
            name = "startBrowserSession",
            description = "Khởi tạo nhanh một phiên làm việc trên trình duyệt web bằng cách cung cấp thông tin trực tiếp.",
            category = "Driver",
            subCategory = "Session",
            parameters = {
                    "sessionName: String - Tên định danh duy nhất cho phiên làm việc này (ví dụ: 'user_A')",
                    "browserName: String - Tên trình duyệt cần khởi động (ví dụ: 'chrome', 'firefox', 'edge')"
            },
            example = "// Bắt đầu nhanh một phiên trên Firefox cho User B\n" +
                    "driver.startBrowserSession(\"user_B_ff\", \"firefox\");",
            note = "Hữu ích cho các kịch bản test nhanh hoặc khi không muốn tạo file profile. Sử dụng 'switchSession' để điều khiển phiên này."
    )
    @Step("Start Browser Session: {0} ({1})")
    public void startSession(String sessionName, String browserName) {
        execute(() -> {
            IDriverFactory factory = new LocalDriverFactory(); // Hoặc RemoteDriverFactory tùy cấu hình
            MutableCapabilities capabilities = CapabilityFactory.getCapabilities(browserName, null);
            WebDriver driver = factory.createDriver(browserName, capabilities);
            SessionManager.getInstance().addSession(sessionName, driver);
            return null;
        }, sessionName, browserName);
    }

    @NetatKeyword(
            name = "startMobileSession",
            description = "Khởi tạo nhanh một phiên làm việc trên thiết bị di động bằng cách cung cấp các thông tin cơ bản.",
            category = "Driver",
            subCategory = "Session",
            parameters = {
                    "sessionName: String - Tên định danh duy nhất cho phiên làm việc",
                    "platformName: String - Nền tảng 'Android' hoặc 'iOS'",
                    "udid: String - UDID của thiết bị (thật hoặc ảo)",
                    "appPath: String - Đường dẫn tuyệt đối đến file .apk hoặc .ipa",
                    "appiumServerUrl: String - URL của Appium Server (ví dụ: 'http://127.0.0.1:4723/wd/hub')",
                    "automationName: String - Tên của driver sử dụng, với Android thì UiAutomator2, iOS thì XCUITest"
            },
            example = "// Bắt đầu phiên trên thiết bị Android của User A\n" +
                    "driver.startMobileSession(\"user_A_android\", \"Android\", \"emulator-5554\", \"/path/to/app.apk\", \"http://127.0.0.1:4723/wd/hub\");",
            note = "Đây là cách đơn giản nhất để khởi động một thiết bị di động mà không cần tạo file profile phức tạp."
    )
    @Step("Start Mobile Session: {0} on {1} device {2}")
    public void startSession(String sessionName, String platformName, String udid, String appPath, String appiumServerUrl,String automationName) {
        execute(() -> {
            DesiredCapabilities caps = new DesiredCapabilities();
            caps.setCapability("platformName", platformName);
            caps.setCapability("udid", udid);
            caps.setCapability("app", appPath);
            // Một số capabilities quan trọng khác có thể thêm vào đây
            caps.setCapability("automationName", automationName);

            WebDriver driver = new AppiumDriver(new URL(appiumServerUrl), caps);
            SessionManager.getInstance().addSession(sessionName, driver);
            return null;
        }, sessionName, platformName, udid, appPath, appiumServerUrl);
    }

    @NetatKeyword(
            name = "startMobileSession",
            description = "Khởi tạo nhanh một phiên làm việc trên thiết bị di động bằng cách cung cấp các thông tin cơ bản.",
            category = "Driver",
            subCategory = "Session",
            parameters = {
                    "sessionName: String - Tên định danh duy nhất cho phiên làm việc",
                    "platformName: String - Nền tảng 'Android' hoặc 'iOS'",
                    "udid: String - UDID của thiết bị (thật hoặc ảo)",
                    "appPath: String - Đường dẫn tuyệt đối đến file .apk hoặc .ipa",
                    "appiumServerUrl: String - URL của Appium Server (ví dụ: 'http://127.0.0.1:4723/wd/hub')",
                    "automationName: String - Tên của driver sử dụng, với Android thì UiAutomator2, iOS thì XCUITest"
            },
            example = "// Bắt đầu phiên trên thiết bị Android của User A\n" +
                    "driver.startMobileSession(\"user_A_android\", \"Android\", \"emulator-5554\", \"/path/to/app.apk\", \"http://127.0.0.1:4723/wd/hub\");",
            note = "Đây là cách đơn giản nhất để khởi động một thiết bị di động mà không cần tạo file profile phức tạp."
    )
    @Step("Start Mobile Session: {0} on {1} device {2}")
    public void startSession(String sessionName, String platformName, String udid, String appPackage,String appActivity, String appiumServerUrl,String automationName) {
        execute(() -> {
            DesiredCapabilities caps = new DesiredCapabilities();
            caps.setCapability("platformName", platformName);
            caps.setCapability("udid", udid);
            caps.setCapability("appPackage", appActivity);
            caps.setCapability("appActivity", appActivity);
            // Một số capabilities quan trọng khác có thể thêm vào đây
            caps.setCapability("automationName", automationName);

            WebDriver driver = new AppiumDriver(new URL(appiumServerUrl), caps);
            SessionManager.getInstance().addSession(sessionName, driver);
            return null;
        }, sessionName, platformName, udid, appPackage,appActivity, appiumServerUrl);
    }

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
}