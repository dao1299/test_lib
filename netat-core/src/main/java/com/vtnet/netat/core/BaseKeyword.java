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

    protected static final Logger logger = LoggerFactory.getLogger(BaseKeyword.class);
    protected static NetatLogger netatLogger;
    protected static ExecutionContext context;

    public BaseKeyword() {
        this.netatLogger = NetatLogger.getInstance();
        this.context = ExecutionContext.getInstance();
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
                netatLogger.info("Executing keyword: {} (Attempt {}/{})",
                        annotation.name(), attempt, maxRetries);

                if (params.length > 0) {
                    netatLogger.debug("Parameters: {}", Arrays.toString(params));
                }

                // Pre-execution hooks
                beforeKeywordExecution(annotation, params);

                // Set timeout if specified
                if (annotation.timeout() > 0) {
                    context.setTimeout(annotation.timeout(), TimeUnit.SECONDS);
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

                context.addStepResult(stepResult);
                netatLogger.info("Keyword executed successfully: {}", annotation.name());

                return result;

            } catch (Exception e) {
                lastException = e;
                netatLogger.error("Keyword execution failed (Attempt {}/{}): {}",
                        attempt, maxRetries, e.getMessage());

                // Take screenshot on failure
                if (annotation.screenshot() && shouldTakeScreenshot()) {
                    try {
                        String screenshotPath = captureScreenshot(
                                annotation.name() + "_failed_attempt_" + attempt);
                        stepResult.setScreenshotPath(screenshotPath);
                    } catch (Exception screenshotEx) {
                        netatLogger.warn("Failed to capture screenshot: {}", screenshotEx.getMessage());
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

        context.addStepResult(stepResult);

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
        context.setCurrentKeyword(annotation.name());
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
        return (context.getWebDriver() != null || context.getMobileDriver() != null)
                && context.isScreenshotEnabled();
    }

    /**
     * Capture screenshot
     */
    private String captureScreenshot(String fileName) {
        try {
            WebDriver webDriver = context.getWebDriver();
            AppiumDriver mobileDriver = context.getMobileDriver();

            if (webDriver != null) {
                return ScreenshotUtils.captureWebScreenshot(webDriver, fileName);
            } else if (mobileDriver != null) {
                return ScreenshotUtils.captureMobileScreenshot(mobileDriver, fileName);
            }
        } catch (Exception e) {
            netatLogger.warn("Failed to capture screenshot: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Utility method để log keyword execution
     */
    protected static void logKeywordExecution(String keywordName, Object... params) {
        netatLogger.info("Executing: {} with parameters: {}", keywordName, Arrays.toString(params));
    }

    /**
     * Utility method để validate parameters
     */
    protected static void validateParameters(Object... params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                throw new NetatException("Parameter at index " + i + " cannot be null");
            }
        }
    }

    /**
     * Get current execution context
     */
    protected static ExecutionContext getContext() {
        return context;
    }

    /**
     * Get current web driver
     */
    protected static WebDriver getWebDriver() {
        return context.getWebDriver();
    }

    /**
     * Get current mobile driver
     */
    protected AppiumDriver getMobileDriver() {
        return context.getMobileDriver();
    }
}