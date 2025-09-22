package com.vtnet.netat.core.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.driver.DriverManager;
import io.qameta.allure.Step;

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
}