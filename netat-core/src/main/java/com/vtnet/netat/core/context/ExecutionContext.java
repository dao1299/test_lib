package com.vtnet.netat.core.context;

import com.vtnet.netat.core.reporting.StepResult;
import com.vtnet.netat.core.assertion.AllureSoftAssert;
import com.vtnet.netat.driver.SessionManager;
import org.openqa.selenium.WebDriver;
import io.appium.java_client.AppiumDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe Execution Context cho NETAT framework.
 *
 * <p>Mỗi thread có instance riêng (ThreadLocal pattern) để hỗ trợ parallel testing.</p>
 *
 * <p><b>Driver Management:</b> WebDriver được delegate cho {@link SessionManager}
 * để đảm bảo single source of truth.</p>
 */
public class ExecutionContext {

    private static final Logger log = LoggerFactory.getLogger(ExecutionContext.class);

    private static final ThreadLocal<ExecutionContext> INSTANCE = ThreadLocal.withInitial(() -> {
        log.debug("Creating new ExecutionContext for thread: {}",
                Thread.currentThread().getName());
        return new ExecutionContext();
    });

    private ExecutionContext() {
        // Private constructor
    }

    public static ExecutionContext getInstance() {
        return INSTANCE.get();
    }

    public WebDriver getWebDriver() {
        WebDriver driver = SessionManager.getInstance().getCurrentDriver();

        if (driver == null) {
            log.warn("No WebDriver found for thread [{}]. " +
                            "Ensure DriverManager.initDriver() was called.",
                    Thread.currentThread().getName());
        }

        return driver;
    }

    public AppiumDriver getMobileDriver() {
        WebDriver driver = SessionManager.getInstance().getCurrentDriver();

        if (driver == null) {
            log.warn("No driver found for getMobileDriver()");
            return null;
        }

        if (driver instanceof AppiumDriver) {
            return (AppiumDriver) driver;
        }

        log.debug("Current driver is not AppiumDriver. Type: {}",
                driver.getClass().getSimpleName());
        return null;
    }

    private String currentTestCase;
    private String currentTestSuite;
    private String currentKeyword;

    public String getCurrentTestCase() {
        return currentTestCase;
    }

    public void setCurrentTestCase(String currentTestCase) {
        this.currentTestCase = currentTestCase;
        log.trace("Current test case: {}", currentTestCase);
    }

    public String getCurrentTestSuite() {
        return currentTestSuite;
    }

    public void setCurrentTestSuite(String currentTestSuite) {
        this.currentTestSuite = currentTestSuite;
    }

    public String getCurrentKeyword() {
        return currentKeyword;
    }

    public void setCurrentKeyword(String currentKeyword) {
        this.currentKeyword = currentKeyword;
    }

    public String getCurrentPlatform() {
        return com.vtnet.netat.driver.DriverManager.getCurrentPlatform();
    }

    private int defaultTimeout = 30;
    private TimeUnit timeoutUnit = TimeUnit.SECONDS;

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setTimeout(int timeout, TimeUnit unit) {
        this.defaultTimeout = timeout;
        this.timeoutUnit = unit;
        log.debug("Timeout set: {} {}", timeout, unit);
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    public long getTimeoutInMillis() {
        return timeoutUnit.toMillis(defaultTimeout);
    }

    private boolean screenshotEnabled = true;
    private boolean videoRecordingEnabled = false;

    public boolean isScreenshotEnabled() {
        return screenshotEnabled;
    }

    public void setScreenshotEnabled(boolean screenshotEnabled) {
        this.screenshotEnabled = screenshotEnabled;
    }

    public boolean isVideoRecordingEnabled() {
        return videoRecordingEnabled;
    }

    public void setVideoRecordingEnabled(boolean videoRecordingEnabled) {
        this.videoRecordingEnabled = videoRecordingEnabled;
    }

    private final Map<String, Object> testData = new ConcurrentHashMap<>();

    public Object getTestData(String key) {
        return testData.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getTestData(String key, Class<T> type) {
        Object value = testData.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        log.warn("TestData '{}' is not of type {}. Actual: {}",
                key, type.getSimpleName(), value.getClass().getSimpleName());
        return null;
    }


    @SuppressWarnings("unchecked")
    public <T> T getTestDataOrDefault(String key, T defaultValue) {
        Object value = testData.get(key);
        return value != null ? (T) value : defaultValue;
    }

    public void setTestData(String key, Object value) {
        if (key == null) {
            log.warn("Cannot set testData with null key");
            return;
        }
        if (value == null) {
            testData.remove(key);
        } else {
            testData.put(key, value);
        }
        log.trace("TestData set: {} = {}", key, value);
    }

    public void setTestData(Map<String, Object> data) {
        if (data != null) {
            testData.putAll(data);
        }
    }

    public Map<String, Object> getAllTestData() {
        return new ConcurrentHashMap<>(testData);
    }

    public boolean hasTestData(String key) {
        return testData.containsKey(key);
    }

    public void removeTestData(String key) {
        testData.remove(key);
    }


    private final Map<String, Object> globalVariables = new ConcurrentHashMap<>();

    public Object getGlobalVariable(String key) {
        return globalVariables.get(key);
    }

    public void setGlobalVariable(String key, Object value) {
        if (key == null) return;
        if (value == null) {
            globalVariables.remove(key);
        } else {
            globalVariables.put(key, value);
        }
    }

    public Map<String, Object> getAllGlobalVariables() {
        return new ConcurrentHashMap<>(globalVariables);
    }


    private final List<StepResult> stepResults = new CopyOnWriteArrayList<>();

    public void addStepResult(StepResult stepResult) {
        if (stepResult != null) {
            stepResults.add(stepResult);
        }
    }

    public List<StepResult> getStepResults() {
        return new ArrayList<>(stepResults);
    }

    public void clearStepResults() {
        stepResults.clear();
    }

    public StepResult getLastStepResult() {
        if (stepResults.isEmpty()) {
            return null;
        }
        return stepResults.get(stepResults.size() - 1);
    }

    public long countFailedSteps() {
        return stepResults.stream()
                .filter(step -> step != null && "FAILED".equalsIgnoreCase(step.getStatus().toString()))
                .count();
    }

    private volatile String environment = "default";
    private volatile String baseUrl;

    private final Map<String, String> environmentConfig = new ConcurrentHashMap<>();

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
        log.debug("Environment set: {}", environment);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        log.debug("Base URL set: {}", baseUrl);
    }

    public String getEnvironmentConfig(String key) {
        return environmentConfig.get(key);
    }

    public String getEnvironmentConfig(String key, String defaultValue) {
        return environmentConfig.getOrDefault(key, defaultValue);
    }

    public void setEnvironmentConfig(String key, String value) {
        if (key == null) return;
        if (value == null) {
            environmentConfig.remove(key);
        } else {
            environmentConfig.put(key, value);
        }
    }

    public void setEnvironmentConfig(Map<String, String> config) {
        if (config != null) {
            environmentConfig.putAll(config);
        }
    }

    private SoftAssert softAssert;

    @Deprecated
    private boolean isSoftAssert = false;

    public SoftAssert getSoftAssert() {
        if (softAssert == null) {
            softAssert = new AllureSoftAssert();
            log.trace("Created new AllureSoftAssert");
        }
        return softAssert;
    }

    public void setSoftAssert(SoftAssert softAssert) {
        this.softAssert = softAssert;
    }

    public void resetSoftAssert() {
        this.softAssert = null;
    }

    public void assertAllSoftAndReset() {
        if (softAssert != null) {
            try {
                log.debug("Executing assertAll() for soft assertions");
                softAssert.assertAll();
            } finally {
                softAssert = null;
            }
        }
    }


    public boolean isSoftAssertFlag() {
        return isSoftAssert;
    }

    @Deprecated
    public void setSoftAssertFlag(boolean softAssertFlag) {
        isSoftAssert = softAssertFlag;
    }

    @Deprecated
    public boolean isSoftAssert() {
        return isSoftAssertFlag();
    }

    @Deprecated
    public void setSoftAssert(boolean softAssert) {
        setSoftAssertFlag(softAssert);
    }

    public void cleanup() {
        log.debug("Cleaning up ExecutionContext for thread: {}",
                Thread.currentThread().getName());

        try {
            SessionManager.getInstance().stopAllSessions();
        } catch (Exception e) {
            log.warn("Error stopping sessions: {}", e.getMessage());
        }

        softAssert = null;
        isSoftAssert = false;

        stepResults.clear();
        testData.clear();
        globalVariables.clear();
        environmentConfig.clear();

        // Reset test info
        currentTestCase = null;
        currentTestSuite = null;
        currentKeyword = null;

        log.debug("ExecutionContext cleanup completed");
    }

    public static void reset() {
        log.debug("Resetting ExecutionContext for thread: {}",
                Thread.currentThread().getName());
        try {
            getInstance().cleanup();
        } catch (Exception e) {
            log.warn("Error during cleanup: {}", e.getMessage());
        } finally {
            INSTANCE.remove();
        }
    }

    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("ExecutionContext Debug Info:\n");
        sb.append("  Thread: ").append(Thread.currentThread().getName()).append("\n");
        sb.append("  TestSuite: ").append(currentTestSuite).append("\n");
        sb.append("  TestCase: ").append(currentTestCase).append("\n");
        sb.append("  Platform: ").append(getCurrentPlatform()).append("\n");
        sb.append("  Environment: ").append(environment).append("\n");
        sb.append("  BaseUrl: ").append(baseUrl).append("\n");
        sb.append("  HasDriver: ").append(getWebDriver() != null).append("\n");
        sb.append("  TestData keys: ").append(testData.keySet()).append("\n");
        sb.append("  StepResults count: ").append(stepResults.size()).append("\n");
        sb.append("  FailedSteps: ").append(countFailedSteps()).append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("ExecutionContext[thread=%s, test=%s, platform=%s, hasDriver=%s]",
                Thread.currentThread().getName(),
                currentTestCase,
                getCurrentPlatform(),
                getWebDriver() != null);
    }
}