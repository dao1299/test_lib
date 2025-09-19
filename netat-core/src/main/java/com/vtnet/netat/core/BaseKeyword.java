package com.vtnet.netat.core;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.exceptions.StepFailException;
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

    protected <T> T execute(Callable<T> logic, Object... params) {
        Method callingKeywordMethod = findCallingKeywordMethod();
        if (callingKeywordMethod == null) {
            throw new IllegalStateException("Could not find a method with @NetatKeyword annotation in the call stack.");
        }
        NetatKeyword annotation = callingKeywordMethod.getAnnotation(NetatKeyword.class);

        String keywordName = annotation.name();
        String paramsString = Arrays.stream(params)
                .map(p -> Objects.toString(p, "null"))
                .collect(Collectors.joining(", "));

        logger.info("KEYWORD START: {} | Parameters: [{}]", keywordName, paramsString);
        long startTime = System.currentTimeMillis();

        try {
            T result = logic.call();
            long endTime = System.currentTimeMillis();
            logger.info("KEYWORD SUCCESS: {} | Duration: {}ms", keywordName, (endTime - startTime));
            Allure.step(keywordName, Status.PASSED);
            return result;
        } catch (Throwable e) {
            long endTime = System.currentTimeMillis();
            logger.error("KEYWORD FAILURE: {} | Duration: {}ms | Error: {}", keywordName, (endTime - startTime), e.getMessage());
            Allure.step(keywordName, Status.FAILED);

            String screenshotPath = ScreenshotUtils.takeScreenshot(keywordName + "_failure");

            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert != null) {
                softAssert.fail("Keyword '" + keywordName + "' failed.", e);
            }

            String errorMessage = "Keyword '" + keywordName + "' failed with params: [" + paramsString + "]";
            StepFailException stepFailException = new StepFailException(errorMessage, e, keywordName);
            stepFailException.setScreenshotPath(screenshotPath);

            throw stepFailException;
        }
    }

    private Method findCallingKeywordMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 3; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            try {
                Class<?> clazz = Class.forName(element.getClassName());
                if (BaseKeyword.class.isAssignableFrom(clazz)) {
                    for (Method method : clazz.getMethods()) {
                        if (method.getName().equals(element.getMethodName()) && method.isAnnotationPresent(NetatKeyword.class)) {
                            return method;
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }
}