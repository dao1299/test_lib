package com.vtnet.netat.core;

import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.exceptions.NetatException;
import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.core.reporting.StepResult;
import com.vtnet.netat.core.utils.ScreenshotUtils;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

public abstract class BaseKeyword {

    protected final NetatLogger logger = NetatLogger.getInstance(this.getClass());

    /**
     * Functional interface để định nghĩa một hành động của keyword.
     * @param <T> Kiểu dữ liệu trả về của hành động (Void nếu không có).
     */
    @FunctionalInterface
    protected interface KeywordAction<T> {
        T execute() throws Exception;
    }

    /**
     * Cỗ máy thực thi keyword trung tâm.
     * @param action Logic thực thi của keyword, được truyền vào dưới dạng Lambda.
     * @param <T> Kiểu dữ liệu trả về của keyword.
     * @return Kết quả của keyword.
     */
    protected <T> T execute(KeywordAction<T> action, Object... params) {
        // Lấy thông tin về phương thức đã gọi (ví dụ: click, sendKeys)
        Method callingMethod = findCallingMethod();
        NetatKeyword annotation = callingMethod.getAnnotation(NetatKeyword.class);

        ExecutionContext context = ExecutionContext.getInstance();
        context.setCurrentKeyword(annotation.name());

        StepResult stepResult = new StepResult(annotation.name(), annotation.description());
        stepResult.setParameters(Arrays.toString(params));

        logger.logKeywordStart(annotation.name(), params);

        int maxRetries = annotation.retryOnFailure() ? annotation.maxRetries() : 1;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                T result = action.execute(); // Thực thi logic Selenium/Appium

                stepResult.setStatus(StepResult.Status.PASSED);
                stepResult.setResult(result);
                if (annotation.screenshot()) {
                    captureScreenshot(stepResult, annotation.name() + "_success");
                }

                return result; // Trả về kết quả nếu thành công

            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    logger.warn(String.format("Keyword '%s' thất bại ở lần thử %d/%d. Thử lại sau 1s.", annotation.name(), attempt, maxRetries));
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        }

        // Nếu tất cả các lần thử đều thất bại
        stepResult.setStatus(StepResult.Status.FAILED);
        stepResult.setError(lastException);
        captureScreenshot(stepResult, annotation.name() + "_failure");

        // Ghi lại kết quả và ném ra exception chuẩn hóa
        context.addStepResult(stepResult);
        logger.logKeywordEnd(annotation.name(), false, stepResult.getDurationMs());
        throw new NetatException("Keyword '" + annotation.name() + "' thất bại sau " + maxRetries + " lần thử.", lastException);
    }

    // Helper method để tìm ra phương thức đã gọi `execute`
    private Method findCallingMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // stackTrace[0] là getStackTrace
        // stackTrace[1] là findCallingMethod
        // stackTrace[2] là execute
        // stackTrace[3] là phương thức keyword (click, sendKeys, ...)
        String methodName = stackTrace[3].getMethodName();
        try {
            // Logic tìm method trong class hiện tại dựa vào tên
            // (Cần cải tiến nếu có method overloading)
            return Arrays.stream(this.getClass().getMethods())
                    .filter(m -> m.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("Method not found: " + methodName));
        } catch (Exception e) {
            throw new NetatException("Không thể tìm thấy định nghĩa keyword cho: " + methodName, e);
        }
    }

    // Helper method để chụp ảnh màn hình
    private void captureScreenshot(StepResult stepResult, String fileName) {
        WebDriver driver = ExecutionContext.getInstance().getWebDriver();
        if (driver != null) {
            try {
                String path = ScreenshotUtils.captureWebScreenshot(driver, fileName);
                stepResult.setScreenshotPath(path);
                logger.logScreenshot(path);
            } catch (Exception e) {
                logger.warn("Không thể chụp ảnh màn hình: " + e.getMessage());
            }
        }
    }
}