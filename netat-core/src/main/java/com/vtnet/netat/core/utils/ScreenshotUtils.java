package com.vtnet.netat.core.utils;

import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.driver.DriverManager;
import io.qameta.allure.Allure;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotUtils {

    private static final String SCREENSHOT_DIR = System.getProperty("user.dir") + "/screenshots";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final NetatLogger logger = NetatLogger.getInstance(ScreenshotUtils.class);

    static {
        // Tạo thư mục screenshots nếu chưa tồn tại
        new File(SCREENSHOT_DIR).mkdirs();
    }

    /**
     * Phương thức chung để chụp ảnh màn hình, tự động phát hiện nền tảng
     * (Web/Mobile).
     * Đây là phương thức mà BaseKeyword sẽ gọi.
     */
    public static String takeScreenshot(String fileName) {
        // Input validation
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.warn("Invalid filename provided, using default name");
            fileName = "screenshot_" + System.currentTimeMillis();
        }

        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            logger.error("Cannot take screenshot: Driver is null");
            return null;
        }

        if (!(driver instanceof TakesScreenshot)) {
            logger.error("Cannot take screenshot: Driver does not support screenshots");
            return null;
        }

        try {
            File sourceFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String fullFileName = generateFileName(fileName);
            File destFile = new File(SCREENSHOT_DIR, fullFileName);

            // Ensure directory exists
            if (!destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }

            FileUtils.copyFile(sourceFile, destFile);
            logger.info("Screenshot saved: {}", destFile.getAbsolutePath());

            // Attach to Allure report
            if (destFile.exists()) {
                Path content = Paths.get(destFile.getAbsolutePath());
                try (InputStream is = Files.newInputStream(content)) {
                    Allure.addAttachment(fileName, is);
                }
            }

            return destFile.getAbsolutePath();

        } catch (WebDriverException e) {
            logger.error("WebDriver error while taking screenshot: {}", e.getMessage());
        } catch (IOException e) {
            logger.error("File I/O error while saving screenshot: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while taking screenshot: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Tạo tên file duy nhất cho ảnh chụp màn hình
     */
    private static String generateFileName(String baseName) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        // Dọn dẹp tên cơ sở
        String cleanBaseName = baseName != null ? baseName.replaceAll("[^a-zA-Z0-9_-]", "_") : "screenshot";
        return String.format("%s_%s.png", cleanBaseName, timestamp);
    }

    // Các phương thức cũ hơn (captureWebScreenshot, captureMobileScreenshot, ...)
    // có thể được giữ lại
    // hoặc xóa đi nếu bạn thấy phương thức takeScreenshot mới đã đủ dùng.
}