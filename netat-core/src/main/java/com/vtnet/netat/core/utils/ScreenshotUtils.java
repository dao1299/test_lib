package com.vtnet.netat.core.utils;

import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.driver.SessionManager;
import io.qameta.allure.Allure;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotUtils {

    private static final String SCREENSHOT_DIR = System.getProperty("user.dir") + "/screenshots";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final NetatLogger logger = NetatLogger.getInstance(ScreenshotUtils.class);

    private static final ThreadLocal<Boolean> IN_PROGRESS = ThreadLocal.withInitial(() -> false);

    private static final String HIGHLIGHT_STYLE = "border: 3px solid red; background: yellow; display: block;";

    static {
        try {
            Files.createDirectories(Path.of(SCREENSHOT_DIR));
        } catch (Exception e) {
            logger.warn("Cannot create screenshot directory: {}", e.getMessage());
            File dir = new File(SCREENSHOT_DIR);
            if (!dir.exists()) {
                logger.warn("Cannot create screenshot directory: {}", e.getMessage());
            } else {
                logger.warn("Cannot create screenshot directory: {}", e.getMessage());
            }
        }
    }

    public static String takeScreenshot(String fileName) {
        if (Boolean.TRUE.equals(IN_PROGRESS.get())) {
            return null;
        }
        IN_PROGRESS.set(true);
        try {
            String base = (fileName == null || fileName.isBlank()) ? "screenshot" : fileName.trim();
            String cleanBase = base.replaceAll("[^a-zA-Z0-9_-]", "_");
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String finalName = cleanBase + "_" + timestamp + ".png";

            WebDriver driver = ExecutionContext.getInstance().getWebDriver();
            if (driver == null) {
                driver = SessionManager.getInstance().getCurrentDriver();
            }
            if (driver == null) {
                logger.error("Cannot take screenshot: driver is null");
                return null;
            }
            if (!(driver instanceof TakesScreenshot)) {
                logger.error("Cannot take screenshot: driver does not implement TakesScreenshot");
                return null;
            }

            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(png)) {
                Allure.addAttachment(cleanBase, "image/png", bais, ".png");
            } catch (Exception e) {
                logger.warn("Attach screenshot to Allure failed: {}", e.getMessage());
            }

            try {
                Path dest = Path.of(SCREENSHOT_DIR, finalName);
                FileUtils.writeByteArrayToFile(dest.toFile(), png);
                logger.info("Screenshot saved: {}", dest.toAbsolutePath());
                return dest.toAbsolutePath().toString();
            } catch (Exception e) {
                logger.warn("Save screenshot file failed: {}", e.getMessage());
                return null;
            }

        } catch (Exception e) {
            logger.error("Unexpected error while taking screenshot: {}", e.getMessage());
            return null;
        } finally {
            IN_PROGRESS.set(false);
        }
    }

    public static void highlightAndTakeScreenshot(WebDriver driver, WebElement element, String stepName) {
        if (Boolean.TRUE.equals(IN_PROGRESS.get())) {
            logger.warn("Screenshot in progress, skipping highlight screenshot.");
            return;
        }
        IN_PROGRESS.set(true);

        String originalStyle = "";
        try {
            if (driver == null || element == null) {
                logger.warn("Driver or Element is null. Cannot highlight and take screenshot.");
                return;
            }
            if (!(driver instanceof TakesScreenshot) || !(driver instanceof JavascriptExecutor)) {
                logger.warn("Driver does not support TakesScreenshot or JavascriptExecutor.");
                return;
            }

            originalStyle = highlight(driver, element);

            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            String cleanBase = stepName.replaceAll("[^a-zA-Z0-9_-]", "_");
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String finalName = cleanBase + "_" + timestamp + ".png";

            try (ByteArrayInputStream bais = new ByteArrayInputStream(png)) {
                Allure.addAttachment(cleanBase, "image/png", bais, ".png");
            } catch (Exception e) {
                logger.warn("Attach highlight screenshot to Allure failed: {}", e.getMessage());
            }

            try {
                Path dest = Path.of(SCREENSHOT_DIR, finalName);
                FileUtils.writeByteArrayToFile(dest.toFile(), png);
                logger.info("Highlight screenshot saved: {}", dest.toAbsolutePath());
            } catch (Exception e) {
                logger.warn("Save highlight screenshot file failed: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Unexpected error while taking highlight screenshot: {}", e.getMessage());
        } finally {
            unHighlight(driver, element, originalStyle);
            IN_PROGRESS.set(false);
        }
    }


    private static String highlight(WebDriver driver, WebElement element) {
        if (driver == null || element == null) return "";
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            String originalStyle = (String) js.executeScript("return arguments[0].getAttribute('style');", element);
            js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, HIGHLIGHT_STYLE);

            Thread.sleep(200);

            return (originalStyle == null) ? "" : originalStyle;
        } catch (Exception e) {
            logger.warn("Failed to highlight element: " + e.getMessage());
            return "";
        }
    }


    private static void unHighlight(WebDriver driver, WebElement element, String originalStyle) {
        if (driver == null || element == null) return;
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, originalStyle);
        } catch (Exception e) {
            logger.warn("Failed to un-highlight element: " + e.getMessage());
        }
    }
}