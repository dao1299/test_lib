package com.vtnet.netat.core.context;

import com.vtnet.netat.core.reporting.StepResult;
import org.openqa.selenium.WebDriver;
import io.appium.java_client.AppiumDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Singleton class để manage execution context
 * Lưu trữ thông tin về current test execution, drivers, settings, etc.
 */
public class ExecutionContext {

    private static final ThreadLocal<ExecutionContext> INSTANCE = new ThreadLocal<ExecutionContext>() {
        @Override
        protected ExecutionContext initialValue() {
            return new ExecutionContext();
        }
    };

    // Driver instances
    private WebDriver webDriver;
    private AppiumDriver mobileDriver;

    // Test information
    private String currentTestCase;
    private String currentTestSuite;
    private String currentKeyword;

    // Execution settings
    private int defaultTimeout = 30;
    private TimeUnit timeoutUnit = TimeUnit.SECONDS;
    private boolean screenshotEnabled = true;
    private boolean videoRecordingEnabled = false;

    // Test data and variables
    private Map<String, Object> testData = new HashMap<>();
    private Map<String, Object> globalVariables = new HashMap<>();

    // Execution results
    private List<StepResult> stepResults = new ArrayList<>();

    // Environment configuration
    private String environment = "default";
    private String baseUrl;
    private Map<String, String> environmentConfig = new HashMap<>();

    private ExecutionContext() {
        // Private constructor for singleton
    }

    public static ExecutionContext getInstance() {
        return INSTANCE.get();
    }

    public static void reset() {
        INSTANCE.remove();
    }

    // Driver management
    public WebDriver getWebDriver() {
        return webDriver;
    }

    public void setWebDriver(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public AppiumDriver getMobileDriver() {
        return mobileDriver;
    }

    public void setMobileDriver(AppiumDriver mobileDriver) {
        this.mobileDriver = mobileDriver;
    }

    // Test information
    public String getCurrentTestCase() {
        return currentTestCase;
    }

    public void setCurrentTestCase(String currentTestCase) {
        this.currentTestCase = currentTestCase;
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

    // Timeout management
    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setTimeout(int timeout, TimeUnit unit) {
        this.defaultTimeout = timeout;
        this.timeoutUnit = unit;
    }

    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    // Screenshot settings
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

    // Test data management
    public Object getTestData(String key) {
        return testData.get(key);
    }

    public void setTestData(String key, Object value) {
        testData.put(key, value);
    }

    public void setTestData(Map<String, Object> data) {
        testData.putAll(data);
    }

    public Map<String, Object> getAllTestData() {
        return new HashMap<>(testData);
    }

    // Global variables
    public Object getGlobalVariable(String key) {
        return globalVariables.get(key);
    }

    public void setGlobalVariable(String key, Object value) {
        globalVariables.put(key, value);
    }

    public Map<String, Object> getAllGlobalVariables() {
        return new HashMap<>(globalVariables);
    }

    // Step results
    public void addStepResult(StepResult stepResult) {
        stepResults.add(stepResult);
    }

    public List<StepResult> getStepResults() {
        return new ArrayList<>(stepResults);
    }

    public void clearStepResults() {
        stepResults.clear();
    }

    // Environment configuration
    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEnvironmentConfig(String key) {
        return environmentConfig.get(key);
    }

    public void setEnvironmentConfig(String key, String value) {
        environmentConfig.put(key, value);
    }

    public void setEnvironmentConfig(Map<String, String> config) {
        environmentConfig.putAll(config);
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (webDriver != null) {
            try {
                webDriver.quit();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            webDriver = null;
        }

        if (mobileDriver != null) {
            try {
                mobileDriver.quit();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            mobileDriver = null;
        }

        stepResults.clear();
        testData.clear();
        // Keep global variables for next test
    }
}