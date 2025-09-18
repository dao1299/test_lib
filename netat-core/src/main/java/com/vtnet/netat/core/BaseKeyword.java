package com.vtnet.netat.core;

import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.core.utils.ScreenshotUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.testng.asserts.SoftAssert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public abstract class BaseKeyword {

    protected final NetatLogger logger = NetatLogger.getInstance(this.getClass());

    /**
     * Cỗ máy thực thi keyword trung tâm.
     *
     * @param logic thực thi của keyword, được truyền vào dưới dạng Lambda.
     * @param <T>   Kiểu dữ liệu trả về của keyword.
     * @return Kết quả của keyword.
     */
    protected <T> T execute(Callable<T> logic, Object... params) {
        // === LOGIC MỚI: TÌM KIẾM KEYWORD GỐC ===
        Method callingKeywordMethod = findCallingKeywordMethod();
        if (callingKeywordMethod == null) {
            // Nếu không tìm thấy, ném ra lỗi rõ ràng thay vì NullPointerException
            throw new IllegalStateException("Cannot find a method annotated with @NetatKeyword in the call stack.");
        }
        NetatKeyword annotation = callingKeywordMethod.getAnnotation(NetatKeyword.class);
        // =====================================

        String keywordName = annotation.name();
        String paramsString = Arrays.stream(params)
                .map(p -> Objects.toString(p, "null"))
                .collect(Collectors.joining(", "));

        logger.info("KEYWORD START: {} | Parameters: [{}]", keywordName, paramsString);
        long startTime = System.currentTimeMillis();

        try {
            T result = logic.call();
            long endTime = System.currentTimeMillis();
            logger.info(" KEYWORD SUCCESS: {} | Duration: {}ms", keywordName, (endTime - startTime));
            Allure.step(keywordName, Status.PASSED);
            return result;
        } catch (Throwable e) {
            long endTime = System.currentTimeMillis();
            logger.error(" KEYWORD FAILURE: {} | Duration: {}ms | Error: {}", keywordName, (endTime - startTime), e.getMessage());
            Allure.step(keywordName, Status.FAILED);
            ScreenshotUtils.takeScreenshot(keywordName + "_failure");

            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert != null) {
                softAssert.fail("Keyword '" + keywordName + "' failed.", e);
            }

            if (e instanceof Error) {
                throw (Error) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Quét ngược chuỗi lời gọi (stack trace) để tìm phương thức public đầu tiên
     * được đánh dấu bằng @NetatKeyword.
     */
    private Method findCallingKeywordMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Bắt đầu quét từ vị trí thứ 3 để bỏ qua getStackTrace, findCallingKeywordMethod, và execute
        for (int i = 3; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            try {
                Class<?> clazz = Class.forName(element.getClassName());

                // Chỉ xem xét các lớp kế thừa từ BaseKeyword
                if (BaseKeyword.class.isAssignableFrom(clazz)) {
                    for (Method method : clazz.getMethods()) {
                        if (method.getName().equals(element.getMethodName()) && method.isAnnotationPresent(NetatKeyword.class)) {
                            // Tìm thấy rồi!
                            return method;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                // Bỏ qua và tiếp tục
            }
        }
        return null; // Không tìm thấy
    }

    /**
     * Functional interface để định nghĩa một hành động của keyword.
     *
     * @param <T> Kiểu dữ liệu trả về của hành động (Void nếu không có).
     */
    @FunctionalInterface
    protected interface KeywordAction<T> {
        T execute() throws Exception;
    }

    // Helper method để chụp ảnh màn hình
//    private void captureScreenshot(StepResult stepResult, String fileName) {
//        WebDriver driver = ExecutionContext.getInstance().getWebDriver();
//        if (driver != null) {
//            try {
//                String path = ScreenshotUtils.captureWebScreenshot(driver, fileName);
//                stepResult.setScreenshotPath(path);
//                logger.logScreenshot(path);
//            } catch (Exception e) {
//                logger.warn("Không thể chụp ảnh màn hình: " + e.getMessage());
//            }
//        }
//    }
}