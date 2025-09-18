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
            description = "Khởi tạo và mở một phiên trình duyệt (web) mới dựa trên cấu hình trong file properties. " +
                    "Nếu không có tham số, nó sẽ sử dụng 'platform.name' mặc định.",
            category = "DriverKeyword",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Khởi tạo trình duyệt mặc định\n" +
                    "driverKeyword.startBrowser();",
            note = "Áp dụng cho web"
    )
    @Step("Initialize browser/device")
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
            category = "DriverKeyword",
            parameters = {
                    "platform: String - Tên nền tảng cần khởi tạo (ví dụ: 'chrome', 'firefox', 'android', 'ios')"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chỉ định khởi tạo trình duyệt Firefox\n" +
                    "driverKeyword.startBrowser(\"firefox\");",
            note = "File cấu hình properties phải tồn tại và chứa các thiết lập cần thiết cho nền tảng được chỉ định. " +
                    "Có thể throw WebDriverException nếu không thể khởi tạo driver, ConfigurationException nếu thiếu thông tin cấu hình, " +
                    "hoặc IllegalArgumentException nếu nền tảng không được hỗ trợ."
    )
    @Step("Initialize browser/device: {0}")
    public void startBrowser(String platform) {
        execute(() -> {
            DriverManager.initDriver(platform);
            return null;
        }, platform);
    }

    @NetatKeyword(
            name = "closeBrowser",
            description = "Đóng hoàn toàn phiên trình duyệt hoặc thiết bị hiện tại và giải phóng tài nguyên. " +
                    "Phương thức này nên được gọi ở cuối mỗi test case để tránh rò rỉ tài nguyên.",
            category = "DriverKeyword",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Đóng trình duyệt sau khi hoàn thành test case\n" +
                    "driverKeyword.closeBrowser();",
            note = "Đã khởi tạo trình duyệt hoặc thiết bị trước đó. Có thể throw WebDriverException nếu có lỗi khi đóng trình duyệt."
    )
    @Step("Close browser/device")
    public void closeBrowser() {
        execute(() -> {
            DriverManager.quitDriver();
            return null;
        });
    }

    @NetatKeyword(
            name = "startApplication",
            description = "Khởi chạy một phiên làm việc mới hoặc kích hoạt lại ứng dụng nếu nó đang chạy nền. " +
                    "Luôn đảm bảo ứng dụng đang ở foreground. Nếu phiên làm việc đã tồn tại, phương thức sẽ kích hoạt lại ứng dụng " +
                    "bằng cách sử dụng appPackage hoặc bundleId từ file cấu hình.",
            category = "DriverKeyword",
            parameters = {
                    "platformName: String (optional) - Tên nền tảng di động ('android', 'ios')"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Khởi chạy ứng dụng trên Android\n" +
                    "driverKeyword.startApplication(\"android\");\n\n" +
                    "// Khởi chạy ứng dụng với cấu hình mặc định\n" +
                    "driverKeyword.startApplication();",
            note = "Áp dụng cho Android và iOS. File cấu hình properties phải chứa 'capability.appium.appPackage' (Android) " +
                    "hoặc 'capability.appium.bundleId' (iOS). Appium server phải đang chạy. Có thể throw WebDriverException " +
                    "nếu không thể khởi tạo driver hoặc kích hoạt ứng dụng, ConfigurationException nếu thiếu thông tin cấu hình."
    )
    @Step("Start or activate application: {0}")
    public void startApplication(String... platformName) {
        execute(() -> {
            // 1. Kiểm tra xem phiên làm việc đã tồn tại hay chưa
            if (DriverManager.getDriver() != null) {
                // Nếu đã tồn tại, kích hoạt lại ứng dụng
                logger.info("Session already exists. Reactivating application to bring to foreground.");

                // Tự động đọc appPackage hoặc bundleId từ file cấu hình
                String appId = ConfigReader.getProperty("capability.appium.appPackage");
                if (appId == null || appId.isEmpty()) {
                    appId = ConfigReader.getProperty("capability.appium.bundleId");
                }

                if (appId != null && !appId.isEmpty()) {
                    ((InteractsWithApps) DriverManager.getDriver()).activateApp(appId);
                } else {
                    logger.warn("Cannot reactivate application because 'appPackage' or 'bundleId' is not defined in configuration file.");
                }

            } else {
                // Nếu chưa tồn tại, khởi tạo một phiên làm việc mới
                logger.info("No existing session. Initializing new session.");
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
            category = "DriverKeyword",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Đóng phiên làm việc sau khi hoàn thành test case\n" +
                    "driverKeyword.closeSession();",
            note = "Áp dụng cho tất cả nền tảng (web và mobile). Đã khởi tạo phiên làm việc trước đó bằng startBrowser hoặc startApplication. " +
                    "Có thể throw WebDriverException nếu có lỗi khi đóng phiên làm việc."
    )
    @Step("Close session (browser/device)")
    public void closeSession() {
        execute(() -> {
            DriverManager.quitDriver();
            return null;
        });
    }
}
