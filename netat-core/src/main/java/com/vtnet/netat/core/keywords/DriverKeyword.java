package com.vtnet.netat.core.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.driver.DriverManager;
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
            category = "DRIVER",
            parameters = {"Không có tham số."},
            example = "// Khởi tạo trình duyệt mặc định\n" +
                    "driverKeyword.startBrowser();"
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
                    "Nếu không có tham số, nó sẽ sử dụng 'platform.name' mặc định.",
            category = "DRIVER",
            parameters = {"String: platform (Tùy chọn) - Tên nền tảng cần khởi tạo (ví dụ: 'chrome', 'firefox')."},
            example = "// Chỉ định khởi tạo trình duyệt Firefox\n" +
                    "driverKeyword.startBrowser(\"firefox\");"
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
            description = "Đóng hoàn toàn phiên trình duyệt hoặc thiết bị hiện tại và giải phóng tài nguyên.",
            category = "DRIVER",
            parameters = {"Không có tham số."},
            example = "driverKeyword.closeBrowser();"
    )
    @Step("Đóng trình duyệt/thiết bị")
    public void closeBrowser() {
        execute(() -> {
            DriverManager.quitDriver();
            return null;
        });
    }
}