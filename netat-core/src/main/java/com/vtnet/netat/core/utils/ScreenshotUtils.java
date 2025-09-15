package com.vtnet.netat.core.utils;

import com.vtnet.netat.driver.DriverManager;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ScreenshotUtils {

    private static final String SCREENSHOT_DIR = System.getProperty("user.dir") + "/screenshots";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    static {
        // Tạo thư mục screenshots nếu chưa tồn tại
        new File(SCREENSHOT_DIR).mkdirs();
    }

    /**
     * Phương thức chung để chụp ảnh màn hình, tự động phát hiện nền tảng (Web/Mobile).
     * Đây là phương thức mà BaseKeyword sẽ gọi.
     */
    public static void takeScreenshot(String fileName) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null || !(driver instanceof TakesScreenshot)) {
            System.err.println("Không thể chụp ảnh màn hình: Driver không hợp lệ hoặc không hỗ trợ.");
            return;
        }

        try {
            File sourceFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String fullFileName = generateFileName(fileName);
            File destFile = new File(SCREENSHOT_DIR, fullFileName);
            FileUtils.copyFile(sourceFile, destFile);

            // Tự động đính kèm ảnh chụp màn hình vào báo cáo Allure
            Path content = Paths.get(destFile.getAbsolutePath());
            try (InputStream is = Files.newInputStream(content)) {
                Allure.addAttachment(fileName, is);
            }

        } catch (IOException e) {
            System.err.println("Lỗi khi chụp hoặc lưu ảnh màn hình: " + e.getMessage());
        }
    }

    /**
     * Tạo tên file duy nhất cho ảnh chụp màn hình
     */
    private static String generateFileName(String baseName) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        // Dọn dẹp tên cơ sở
        String cleanBaseName = baseName != null ?
                baseName.replaceAll("[^a-zA-Z0-9_-]", "_") : "screenshot";
        return String.format("%s_%s.png", cleanBaseName, timestamp);
    }

    // Các phương thức cũ hơn (captureWebScreenshot, captureMobileScreenshot, ...) có thể được giữ lại
    // hoặc xóa đi nếu bạn thấy phương thức takeScreenshot mới đã đủ dùng.
}