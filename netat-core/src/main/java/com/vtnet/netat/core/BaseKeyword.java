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

//    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//    public BaseKeyword() {
//        // Loại bỏ khởi tạo context và netatLogger ở đây.
//        // Chúng sẽ được lấy thông qua các getter static khi cần.
//    }
//
//    /**
//     * Execute keyword với full error handling, logging, screenshot, retry
//     */
//    protected Object executeKeyword(String methodName, Object... params) {
//        Method method = findKeywordMethod(methodName);
//        NetatKeyword annotation = method.getAnnotation(NetatKeyword.class);
//
//        StepResult stepResult = new StepResult();
//        stepResult.setKeywordName(annotation.name());
//        stepResult.setDescription(annotation.description());
//        stepResult.setParameters(Arrays.toString(params));
//        stepResult.setStartTime(LocalDateTime.now());
//
//        int maxRetries = annotation.retryOnFailure() ? annotation.maxRetries() : 1;
//        Exception lastException = null;
//
//        for (int attempt = 1; attempt <= maxRetries; attempt++) {
//            try {
//                getNetatLogger().info("Executing keyword: {} (Attempt {}/{})",
//                        annotation.name(), attempt, maxRetries);
//
//                if (params.length > 0) {
//                    getNetatLogger().debug("Parameters: {}", Arrays.toString(params));
//                }
//
//                beforeKeywordExecution(annotation, params);
//
//                if (annotation.timeout() > 0) {
//                    getContext().setTimeout(annotation.timeout(), TimeUnit.SECONDS);
//                }
//
//                Object result = performKeywordAction(methodName, params);
//
//                afterKeywordExecution(annotation, params, result);
//
//                if (annotation.screenshot() && shouldTakeScreenshot()) {
//                    String screenshotPath = captureScreenshot(annotation.name() + "_success");
//                    stepResult.setScreenshotPath(screenshotPath);
//                }
//
//                stepResult.setStatus(StepResult.Status.PASSED);
//                stepResult.setEndTime(LocalDateTime.now());
//                stepResult.setResult(result);
//
//                getContext().addStepResult(stepResult);
//                getNetatLogger().info("Keyword executed successfully: {}", annotation.name());
//
//                return result;
//
//            } catch (Exception e) {
//                lastException = e;
//                getNetatLogger().error("Keyword execution failed (Attempt {}/{}): {}",
//                        attempt, maxRetries, e.getMessage());
//
//                if (annotation.screenshot() && shouldTakeScreenshot()) {
//                    try {
//                        String screenshotPath = captureScreenshot(
//                                annotation.name() + "_failed_attempt_" + attempt);
//                        stepResult.setScreenshotPath(screenshotPath);
//                    } catch (Exception screenshotEx) {
//                        getNetatLogger().warn("Failed to capture screenshot: {}", screenshotEx.getMessage());
//                    }
//                }
//
//                if (attempt < maxRetries) {
//                    try {
//                        Thread.sleep(1000 * attempt);
//                    } catch (InterruptedException ie) {
//                        Thread.currentThread().interrupt();
//                        break;
//                    }
//                }
//            }
//        }
//
//        stepResult.setStatus(StepResult.Status.FAILED);
//        stepResult.setEndTime(LocalDateTime.now());
//        stepResult.setError(lastException);
//
//        getContext().addStepResult(stepResult);
//
//        throw new NetatException(
//                String.format("Keyword '%s' failed after %d attempts", annotation.name(), maxRetries),
//                lastException
//        );
//    }
//
//    protected abstract Object performKeywordAction(String methodName, Object... params);
//
//    protected void beforeKeywordExecution(NetatKeyword annotation, Object... params) {
//        getContext().setCurrentKeyword(annotation.name());
//    }
//
//    protected void afterKeywordExecution(NetatKeyword annotation, Object[] params, Object result) {
//    }
//
//    private Method findKeywordMethod(String methodName) {
//        Method[] methods = this.getClass().getDeclaredMethods();
//        for (Method method : methods) {
//            if (method.isAnnotationPresent(NetatKeyword.class)) {
//                NetatKeyword annotation = method.getAnnotation(NetatKeyword.class);
//                if (annotation.name().equals(methodName) || method.getName().equals(methodName)) {
//                    return method;
//                }
//            }
//        }
//        throw new NetatException("Keyword method not found: " + methodName);
//    }
//
//    private boolean shouldTakeScreenshot() {
//        return (getContext().getWebDriver() != null || getContext().getMobileDriver() != null)
//                && getContext().isScreenshotEnabled();
//    }
//
//    private String captureScreenshot(String fileName) {
//        try {
//            WebDriver webDriver = getContext().getWebDriver();
//            AppiumDriver mobileDriver = getContext().getMobileDriver();
//
//            if (webDriver != null) {
//                return ScreenshotUtils.captureWebScreenshot(webDriver, fileName);
//            } else if (mobileDriver != null) {
//                return ScreenshotUtils.captureMobileScreenshot(mobileDriver, fileName);
//            }
//        } catch (Exception e) {
//            getNetatLogger().warn("Failed to capture screenshot: {}", e.getMessage());
//        }
//        return null;
//    }
//
//    protected static void logKeywordExecution(String keywordName, Object... params) {
//        getNetatLogger().info("Executing: {} with parameters: {}", keywordName, Arrays.toString(params));
//    }
//
//    protected static void validateParameters(Object... params) {
//        for (int i = 0; i < params.length; i++) {
//            if (params[i] == null) {
//                throw new NetatException("Parameter at index " + i + " cannot be null");
//            }
//        }
//    }
//
//    protected static ExecutionContext getContext() {
//        return ExecutionContext.getInstance();
//    }
//
//    protected static NetatLogger getNetatLogger() {
//        return NetatLogger.getInstance();
//    }
//
//    protected static WebDriver getWebDriver() {
//        return getContext().getWebDriver();
//    }
//
//    protected static AppiumDriver getMobileDriver() {
//        return getContext().getMobileDriver();
//    }

    // Phương thức tĩnh mới để truy cập UIObjectRepository, làm cho việc tìm kiếm UIObject
    // trở nên dễ dàng và nhất quán hơn từ mọi keyword hoặc test script.
//    protected static NetatUIObject getUIObjectByPath(String objectPath) {
//        try {
//            return UIObjectRepository.getInstance().getUIObjectByPath(objectPath);
//        } catch (NetatException e) {
//            // Bao bọc lại exception để cung cấp ngữ cảnh tốt hơn
//            throw new NetatException("Failed to retrieve UIObject for path: " + objectPath, e);
//        }
//    }
}