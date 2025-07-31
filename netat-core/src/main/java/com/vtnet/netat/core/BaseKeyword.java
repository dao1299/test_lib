// src/main/java/com/vtnet/netat/core/BaseKeyword.java
package com.vtnet.netat.core;

import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.exceptions.NetatException;
import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.core.reporting.StepResult;
import com.vtnet.netat.core.utils.ScreenshotUtils;
import org.openqa.selenium.WebDriver;
import io.appium.java_client.AppiumDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Base class cho tất cả NETAT keyword classes
 * Cung cấp core functionality như logging, screenshot, error handling, retry logic
 */
public abstract class BaseKeyword {

    // Loại bỏ các trường static 'context' và 'netatLogger' khỏi đây
    // protected static final Logger logger = LoggerFactory.getLogger(BaseKeyword.class); // Giữ nguyên SLF4J logger nếu muốn
    protected final Logger logger = LoggerFactory.getLogger(this.getClass()); // Khởi tạo non-static logger cho mỗi instance

    public BaseKeyword() {
        // Loại bỏ khởi tạo context và netatLogger ở đây.
        // Chúng sẽ được lấy thông qua các getter static khi cần.
    }

    /**
     * Execute keyword với full error handling, logging, screenshot, retry
     */
    protected Object executeKeyword(String methodName, Object... params) {
        Method method = findKeywordMethod(methodName);
        NetatKeyword annotation = method.getAnnotation(NetatKeyword.class);

        StepResult stepResult = new StepResult();
        stepResult.setKeywordName(annotation.name());
        stepResult.setDescription(annotation.description());
        stepResult.setParameters(Arrays.toString(params));
        stepResult.setStartTime(LocalDateTime.now());

        int maxRetries = annotation.retryOnFailure() ? annotation.maxRetries() : 1;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                getNetatLogger().info("Executing keyword: {} (Attempt {}/{})", // Dùng getNetatLogger()
                        annotation.name(), attempt, maxRetries);

                if (params.length > 0) {
                    getNetatLogger().debug("Parameters: {}", Arrays.toString(params)); // Dùng getNetatLogger()
                }

                // Pre-execution hooks
                beforeKeywordExecution(annotation, params);

                // Set timeout if specified
                if (annotation.timeout() > 0) {
                    getContext().setTimeout(annotation.timeout(), TimeUnit.SECONDS); // Dùng getContext()
                }

                // Execute actual keyword logic
                Object result = performKeywordAction(methodName, params);

                // Post-execution hooks
                afterKeywordExecution(annotation, params, result);

                // Take screenshot if needed
                if (annotation.screenshot() && shouldTakeScreenshot()) {
                    String screenshotPath = captureScreenshot(annotation.name() + "_success");
                    stepResult.setScreenshotPath(screenshotPath);
                }

                stepResult.setStatus(StepResult.Status.PASSED);
                stepResult.setEndTime(LocalDateTime.now());
                stepResult.setResult(result);

                getContext().addStepResult(stepResult); // Dùng getContext()
                getNetatLogger().info("Keyword executed successfully: {}", annotation.name()); // Dùng getNetatLogger()

                return result;

            } catch (Exception e) {
                lastException = e;
                getNetatLogger().error("Keyword execution failed (Attempt {}/{}): {}", // Dùng getNetatLogger()
                        attempt, maxRetries, e.getMessage());

                // Take screenshot on failure
                if (annotation.screenshot() && shouldTakeScreenshot()) {
                    try {
                        String screenshotPath = captureScreenshot(
                                annotation.name() + "_failed_attempt_" + attempt);
                        stepResult.setScreenshotPath(screenshotPath);
                    } catch (Exception screenshotEx) {
                        getNetatLogger().warn("Failed to capture screenshot: {}", screenshotEx.getMessage()); // Dùng getNetatLogger()
                    }
                }

                // If not the last attempt, wait before retry
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // All attempts failed
        stepResult.setStatus(StepResult.Status.FAILED);
        stepResult.setEndTime(LocalDateTime.now());
        stepResult.setError(lastException);

        getContext().addStepResult(stepResult); // Dùng getContext()

        throw new NetatException(
                String.format("Keyword '%s' failed after %d attempts", annotation.name(), maxRetries),
                lastException
        );
    }

    /**
     * Abstract method để implement keyword logic
     */
    protected abstract Object performKeywordAction(String methodName, Object... params);

    /**
     * Hook method called before keyword execution
     */
    protected void beforeKeywordExecution(NetatKeyword annotation, Object... params) {
        // Override in child classes if needed
        getContext().setCurrentKeyword(annotation.name()); // Dùng getContext()
    }

    /**
     * Hook method called after successful keyword execution
     */
    protected void afterKeywordExecution(NetatKeyword annotation, Object[] params, Object result) {
        // Override in child classes if needed
    }

    /**
     * Find keyword method by name using reflection
     */
    private Method findKeywordMethod(String methodName) {
        Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(NetatKeyword.class)) {
                NetatKeyword annotation = method.getAnnotation(NetatKeyword.class);
                if (annotation.name().equals(methodName) || method.getName().equals(methodName)) {
                    return method;
                }
            }
        }
        throw new NetatException("Keyword method not found: " + methodName);
    }

    /**
     * Determine if screenshot should be taken
     */
    private boolean shouldTakeScreenshot() {
        return (getContext().getWebDriver() != null || getContext().getMobileDriver() != null) // Dùng getContext()
                && getContext().isScreenshotEnabled(); // Dùng getContext()
    }

    /**
     * Capture screenshot
     */
    private String captureScreenshot(String fileName) {
        try {
            WebDriver webDriver = getContext().getWebDriver(); // Dùng getContext()
            AppiumDriver mobileDriver = getContext().getMobileDriver(); // Dùng getContext()

            if (webDriver != null) {
                return ScreenshotUtils.captureWebScreenshot(webDriver, fileName);
            } else if (mobileDriver != null) {
                return ScreenshotUtils.captureMobileScreenshot(mobileDriver, fileName);
            }
        } catch (Exception e) {
            getNetatLogger().warn("Failed to capture screenshot: {}", e.getMessage()); // Dùng getNetatLogger()
        }
        return null;
    }

    /**
     * Utility method để log keyword execution.
     * Phương thức này cần dùng getNetatLogger() để đảm bảo thread-safety.
     */
    protected static void logKeywordExecution(String keywordName, Object... params) {
        getNetatLogger().info("Executing: {} with parameters: {}", keywordName, Arrays.toString(params));
    }

    /**
     * Utility method để validate parameters.
     * Phương thức này cần dùng getNetatLogger() để đảm bảo thread-safety.
     */
    protected static void validateParameters(Object... params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                throw new NetatException("Parameter at index " + i + " cannot be null");
            }
        }
    }

    /**
     * Get current execution context.
     * Phương thức này luôn gọi ExecutionContext.getInstance() để đảm bảo thread-safety.
     */
    protected static ExecutionContext getContext() {
        return ExecutionContext.getInstance();
    }

    /**
     * Get current NetatLogger.
     * Phương thức này luôn gọi NetatLogger.getInstance() để đảm bảo thread-safety.
     */
    protected static NetatLogger getNetatLogger() {
        return NetatLogger.getInstance();
    }

    /**
     * Get current web driver.
     * (Convenience method, gọi getContext().getWebDriver())
     */
    protected static WebDriver getWebDriver() {
        return getContext().getWebDriver();
    }

    /**
     * Get current mobile driver.
     * (Convenience method, gọi getContext().getMobileDriver())
     */
    protected static AppiumDriver getMobileDriver() {
        return getContext().getMobileDriver();
    }
}