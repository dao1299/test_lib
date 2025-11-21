package com.vtnet.netat.core.utils;

import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.driver.SessionManager;
import io.qameta.allure.Allure;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HTMLSnapshotUtils {

    private static final String SNAPSHOT_DIR = System.getProperty("user.dir") + "/html-snapshots";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final NetatLogger logger = NetatLogger.getInstance(HTMLSnapshotUtils.class);

    private static final ThreadLocal<Boolean> IN_PROGRESS = ThreadLocal.withInitial(() -> false);

    static {
        try {
            Files.createDirectories(Path.of(SNAPSHOT_DIR));
        } catch (Exception e) {
            logger.warn("Cannot create HTML snapshot directory: {}", e.getMessage());
        }
    }

    public static String captureHTMLSnapshot(String fileName) {
        if (Boolean.TRUE.equals(IN_PROGRESS.get())) {
            return null;
        }

        IN_PROGRESS.set(true);
        try {
            String base = (fileName == null || fileName.isBlank()) ? "snapshot" : fileName.trim();
            String cleanBase = base.replaceAll("[^a-zA-Z0-9_-]", "_");
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String finalName = cleanBase + "_" + timestamp + ".html";

            WebDriver driver = SessionManager.getInstance().getCurrentDriver();
            if (driver == null) {
                logger.error("Cannot capture HTML: driver is null");
                return null;
            }


            if (driver instanceof io.appium.java_client.AppiumDriver) {
                logger.debug("Skipping HTML snapshot for mobile app context");
                return null;
            }

            if (!(driver instanceof JavascriptExecutor)) {
                logger.warn("Driver does not support JavascriptExecutor");
                return null;
            }

            String htmlContent = getEnhancedPageSource(driver);

            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                logger.warn("HTML content is empty");
                return null;
            }

            // Attach to Allure
            try (ByteArrayInputStream bais = new ByteArrayInputStream(
                    htmlContent.getBytes(StandardCharsets.UTF_8))) {
                Allure.addAttachment(cleanBase + " (HTML)", "text/html", bais, ".html");
            } catch (Exception e) {
                logger.warn("Failed to attach HTML to Allure: {}", e.getMessage());
            }

            try {
                Path dest = Path.of(SNAPSHOT_DIR, finalName);
                FileUtils.writeStringToFile(dest.toFile(), htmlContent, StandardCharsets.UTF_8);
                logger.info("HTML snapshot saved: {}", dest.toAbsolutePath());
                return dest.toAbsolutePath().toString();
            } catch (Exception e) {
                logger.warn("Failed to save HTML file: {}", e.getMessage());
                return null;
            }

        } catch (Exception e) {
            logger.error("Unexpected error while capturing HTML: {}", e.getMessage());
            return null;
        } finally {
            IN_PROGRESS.set(false);
        }
    }

    /**
     * Get enhanced page source with inline styles
     */
    private static String getEnhancedPageSource(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            String script =
                    "var html = document.documentElement.outerHTML;" +
                            "var style = '<style>';" +
                            "var sheets = document.styleSheets;" +
                            "for (var i = 0; i < sheets.length; i++) {" +
                            "  try {" +
                            "    var rules = sheets[i].cssRules || sheets[i].rules;" +
                            "    for (var j = 0; j < rules.length; j++) {" +
                            "      style += rules[j].cssText + '\\n';" +
                            "    }" +
                            "  } catch(e) {}" +
                            "}" +
                            "style += '</style>';" +
                            "return html.replace('</head>', style + '</head>');";

            Object result = js.executeScript(script);

            if (result instanceof String) {
                return (String) result;
            } else {
                return driver.getPageSource();
            }

        } catch (Exception e) {
            logger.warn("Failed to get enhanced HTML, using simple page source: {}", e.getMessage());
            return driver.getPageSource();
        }
    }

    public static void captureScreenshotAndHTML(String baseName) {
        ScreenshotUtils.takeScreenshot(baseName);
        captureHTMLSnapshot(baseName);
    }
}