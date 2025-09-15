package com.vtnet.netat.core.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.driver.ConfigReader;
import com.vtnet.netat.driver.DriverManager;
import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Step;

/**
 * Cung cấp các keyword để quản lý vòng đời của trình duyệt hoặc thiết bị.
 * Các keyword này thường được sử dụng ở đầu và cuối của một kịch bản kiểm thử.
 */
public class DriverKeyword extends BaseKeyword {

    @NetatKeyword(
            name = "startBrowser",
            description = "Khởi tạo và mở một phiên trình duyệt (web) hoặc thiết bị (mobile) mới dựa trên cấu hình trong file properties. " +
                    "Nếu không có tham số, nó sẽ sử dụng 'platform.name' mặc định.",
            category = "Session/Lifecycle",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Khởi tạo trình duyệt mặc định\n" +
                    "driverKeyword.startBrowser();",
            prerequisites = {"File cấu hình properties phải tồn tại và chứa các thiết lập cần thiết"},
            exceptions = {"WebDriverException: Nếu không thể khởi tạo driver",
                    "ConfigurationException: Nếu thiếu thông tin cấu hình cần thiết"},
            platform = "ALL",
            systemImpact = "MODIFY",
            tags = {"browser", "session", "initialization"}
    )
    @Step("Khởi tạo trình duyệt/thiết bị")
    public void startBrowser() {
        execute(() -> {
            DriverManager.initDriver();
            return null;
        });
    }

    // Nạp chồng (Overloading) phương thức để nhận tham số platform
    @NetatKeyword(
            name = "startBrowser",
            description = "Khởi tạo và mở một phiên trình duyệt (web) hoặc thiết bị (mobile) mới dựa trên cấu hình trong file properties. " +
                    "Tham số platform cho phép chỉ định loại trình duyệt hoặc thiết bị cụ thể cần khởi tạo.",
            category = "Session/Lifecycle",
            parameters = {"String: platform - Tên nền tảng cần khởi tạo (ví dụ: 'chrome', 'firefox', 'android', 'ios')."},
            returnValue = "void: Không trả về giá trị",
            example = "// Chỉ định khởi tạo trình duyệt Firefox\n" +
                    "driverKeyword.startBrowser(\"firefox\");",
            prerequisites = {"File cấu hình properties phải tồn tại và chứa các thiết lập cần thiết cho nền tảng được chỉ định"},
            exceptions = {"WebDriverException: Nếu không thể khởi tạo driver",
                    "ConfigurationException: Nếu thiếu thông tin cấu hình cần thiết",
                    "IllegalArgumentException: Nếu nền tảng được chỉ định không được hỗ trợ"},
            platform = "ALL",
            systemImpact = "MODIFY",
            tags = {"browser", "session", "initialization", "custom-platform"}
    )
    @Step("Khởi tạo trình duyệt/thiết bị: {0}")
    public void startBrowser(String platform) {
        execute(() -> {
            DriverManager.initDriver(platform);
            return null;
        }, platform);
    }

    @NetatKeyword(
            name = "closeBrowser",
            description = "Đóng hoàn toàn phiên trình duyệt hoặc thiết bị hiện tại và giải phóng tài nguyên. Phương thức này nên được gọi ở cuối mỗi test case để tránh rò rỉ tài nguyên.",
            category = "Session/Lifecycle",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Đóng trình duyệt sau khi hoàn thành test case\ndriverKeyword.closeBrowser();",
            prerequisites = {"Đã khởi tạo trình duyệt hoặc thiết bị trước đó"},
            exceptions = {"WebDriverException: Nếu có lỗi khi đóng trình duyệt"},
            platform = "ALL",
            systemImpact = "MODIFY",
            tags = {"browser", "session", "cleanup"}
    )
    @Step("Đóng trình duyệt/thiết bị")
    public void closeBrowser() {
        execute(() -> {
            DriverManager.quitDriver();
            return null;
        });
    }

    @NetatKeyword(
            name = "startApplication",
            description = "Khởi chạy một phiên làm việc mới hoặc kích hoạt lại ứng dụng nếu nó đang chạy nền. Luôn đảm bảo ứng dụng đang ở foreground. " +
                    "Nếu phiên làm việc đã tồn tại, phương thức sẽ kích hoạt lại ứng dụng bằng cách sử dụng appPackage hoặc bundleId từ file cấu hình.",
            category = "Session/Lifecycle",
            parameters = {"String: platformName (Tùy chọn) - Tên nền tảng di động ('android', 'ios')."},
            returnValue = "void: Không trả về giá trị",
            example = "// Khởi chạy ứng dụng trên Android\ndriverKeyword.startApplication(\"android\");",
            prerequisites = {"File cấu hình properties phải chứa 'capability.appium.appPackage' (Android) hoặc 'capability.appium.bundleId' (iOS)",
                    "Appium server phải đang chạy"},
            exceptions = {"WebDriverException: Nếu không thể khởi tạo driver hoặc kích hoạt ứng dụng",
                    "ConfigurationException: Nếu thiếu thông tin cấu hình cần thiết"},
            platform = "ANDROID, IOS",
            systemImpact = "MODIFY",
            tags = {"mobile", "application", "session", "initialization"}
    )
    @Step("Khởi chạy hoặc kích hoạt ứng dụng: {0}")
    public void startApplication(String... platformName) {
        execute(() -> {
            // 1. Kiểm tra xem phiên làm việc đã tồn tại hay chưa
            if (DriverManager.getDriver() != null) {
                // Nếu đã tồn tại, kích hoạt lại ứng dụng
                logger.info("Phiên làm việc đã tồn tại. Kích hoạt lại ứng dụng để đưa ra foreground.");

                // Tự động đọc appPackage hoặc bundleId từ file cấu hình
                String appId = ConfigReader.getProperty("capability.appium.appPackage");
                if (appId == null || appId.isEmpty()) {
                    appId = ConfigReader.getProperty("capability.appium.bundleId");
                }

                if (appId != null && !appId.isEmpty()) {
                    ((InteractsWithApps) DriverManager.getDriver()).activateApp(appId);
                } else {
                    logger.warn("Không thể kích hoạt lại ứng dụng vì 'appPackage' hoặc 'bundleId' không được định nghĩa trong file cấu hình.");
                }

            } else {
                // Nếu chưa tồn tại, khởi tạo một phiên làm việc mới
                logger.info("Chưa có phiên làm việc. Khởi tạo phiên mới.");
                if (platformName != null && platformName.length > 0 && !platformName[0].isEmpty()) {
                    DriverManager.initDriver(platformName[0]);
                } else {
                    DriverManager.initDriver();
                }
            }
            return null;
        }, (Object[]) platformName);
    }

    @NetatKeyword(
            name = "closeSession",
            description = "Đóng hoàn toàn phiên làm việc hiện tại (cả trình duyệt web và ứng dụng di động) và giải phóng tài nguyên. " +
                    "Phương thức này nên được gọi ở cuối mỗi test case để đảm bảo tất cả tài nguyên được giải phóng đúng cách.",
            category = "Session/Lifecycle",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Đóng phiên làm việc sau khi hoàn thành test case\ndriverKeyword.closeSession();",
            prerequisites = {"Đã khởi tạo phiên làm việc trước đó bằng startBrowser hoặc startApplication"},
            exceptions = {"WebDriverException: Nếu có lỗi khi đóng phiên làm việc"},
            platform = "ALL",
            systemImpact = "MODIFY",
            tags = {"browser", "mobile", "session", "cleanup"}
    )
    @Step("Đóng phiên làm việc (trình duyệt/thiết bị)")
    public void closeSession() {
        execute(() -> {
            DriverManager.quitDriver();
            return null;
        });
    }
}
