package com.vtnet.netat.core.utils;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Utility class cho screenshot operations
 */
public class ScreenshotUtils {

    private static final String SCREENSHOT_DIR = System.getProperty("user.dir") + "/screenshots";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    static {
        // Create screenshots directory if not exists
        File screenshotDir = new File(SCREENSHOT_DIR);
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }
    }

    /**
     * Capture screenshot from WebDriver
     */
    public static String captureWebScreenshot(WebDriver driver, String fileName) throws IOException {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver cannot be null");
        }

        if (!(driver instanceof TakesScreenshot)) {
            throw new IllegalArgumentException("WebDriver does not support screenshots");
        }

        TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
        File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);

        String fullFileName = generateFileName(fileName);
        File destFile = new File(SCREENSHOT_DIR, fullFileName);

        FileUtils.copyFile(sourceFile, destFile);

        return destFile.getAbsolutePath();
    }

    /**
     * Capture screenshot from Mobile Driver
     */
    public static String captureMobileScreenshot(AppiumDriver driver, String fileName) throws IOException {
        if (driver == null) {
            throw new IllegalArgumentException("AppiumDriver cannot be null");
        }

        File sourceFile = driver.getScreenshotAs(OutputType.FILE);

        String fullFileName = generateFileName(fileName);
        File destFile = new File(SCREENSHOT_DIR, fullFileName);

        FileUtils.copyFile(sourceFile, destFile);

        return destFile.getAbsolutePath();
    }

    /**
     * Capture screenshot as byte array
     */
    public static byte[] captureScreenshotAsBytes(WebDriver driver) {
        if (driver == null || !(driver instanceof TakesScreenshot)) {
            return new byte[0];
        }

        TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
        return takesScreenshot.getScreenshotAs(OutputType.BYTES);
    }

    /**
     * Generate unique filename for screenshot
     */
    private static String generateFileName(String baseName) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        // Clean base name
        String cleanBaseName = baseName != null ?
                baseName.replaceAll("[^a-zA-Z0-9_-]", "_") : "screenshot";

        return String.format("%s_%s_%s.png", cleanBaseName, timestamp, uuid);
    }

    /**
     * Get screenshots directory path
     */
    public static String getScreenshotDirectory() {
        return SCREENSHOT_DIR;
    }

    /**
     * Clean up old screenshots (older than specified days)
     */
    public static void cleanupOldScreenshots(int daysOld) {
        File screenshotDir = new File(SCREENSHOT_DIR);
        if (!screenshotDir.exists()) {
            return;
        }

        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);

        File[] files = screenshotDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    file.delete();
                }
            }
        }
    }
}