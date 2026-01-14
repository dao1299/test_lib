package com.vtnet.netat.core;

import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.ui.Locator;
import com.vtnet.netat.core.ui.ObjectUI;
import com.vtnet.netat.core.utils.ScreenshotUtils;
import com.vtnet.netat.driver.ConfigReader;
import com.vtnet.netat.driver.DriverManager;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import com.vtnet.netat.core.secret.SecretDecryptor;
import com.vtnet.netat.core.secret.MasterKeyProvider;

public abstract class BaseUiKeyword extends BaseKeyword {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration PRIMARY_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SECONDARY_TIMEOUT = Duration.ofSeconds(15);
    private static final int DEFAULT_DOC_READY_TIMEOUT = 5;
    private static final int DEFAULT_AJAX_TIMEOUT = 5;
    protected static final Duration POLLING_INTERVAL = Duration.ofMillis(100);
    private static final Logger log = LoggerFactory.getLogger(BaseUiKeyword.class);
    private static final ThreadLocal<ObjectUI> CURRENT_ELEMENT =
            new ThreadLocal<>();

    private int getAjaxTimeout() {
        return getConfigInt("smart.wait.ajax.timeout", DEFAULT_AJAX_TIMEOUT);
    }

    private int getPollingInterval() {
        return getConfigInt("smart.wait.polling.interval", ((int) POLLING_INTERVAL.toMillis()));
    }

    private boolean isSmartWaitEnabled() {
        return getConfigBoolean("smart.wait.enabled", true);
    }

    private int getConfigInt(String key, int defaultValue) {
        try {
            String value = ConfigReader.getProperty(key);
            return (value != null && !value.isEmpty()) ? Integer.parseInt(value.trim()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean getConfigBoolean(String key, boolean defaultValue) {
        try {
            String value = ConfigReader.getProperty(key);
            return (value != null && !value.isEmpty()) ? Boolean.parseBoolean(value.trim()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    protected Duration getPrimaryTimeout() {
        int seconds = getConfigInt("element.timeout.primary",(int) DEFAULT_TIMEOUT.toSeconds());
        return Duration.ofSeconds(seconds);
    }

    protected Duration getSecondaryTimeout() {
        int seconds = getConfigInt("element.timeout.secondary", (int) SECONDARY_TIMEOUT.toSeconds());
        return Duration.ofSeconds(seconds);
    }

    protected WebElement findElement(ObjectUI uiObject) {
        return findElement(uiObject, PRIMARY_TIMEOUT);
    }

    @Override
    protected <T> T execute(Callable<T> logic, Object... params) {
        if (params != null && params.length > 0 &&
                params[0] instanceof ObjectUI) {
            CURRENT_ELEMENT.set((ObjectUI) params[0]);
        }
        try {
            return super.execute(logic, params);
        } finally {
            CURRENT_ELEMENT.remove();
        }
    }

    protected void performActionWithRetry(ObjectUI uiObject,
                                          Function<WebElement, ExpectedCondition<WebElement>> waitCondition,
                                          Consumer<WebElement> action) {
        WebDriver driver = DriverManager.getDriver();
        int attempts = 3;

        while (attempts > 0) {
            try {
                attempts--;
                WebElement element = findElement(uiObject);

                new WebDriverWait(driver, PRIMARY_TIMEOUT)
                        .ignoring(StaleElementReferenceException.class)
                        .until(waitCondition.apply(element));

                action.accept(element);
                return;
            } catch (StaleElementReferenceException e) {
                if (attempts == 0) {
                    logger.error("Failed to perform action on '{}' after retries due to StaleElementReferenceException.", uiObject.getName());
                    throw e;
                }
                logger.warn("Stale reference for element '{}'. Retrying action... (Remaining attempts: {})", uiObject.getName(), attempts);

                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }
    private int getDocReadyTimeout() {
        return getConfigInt("smart.wait.document.ready.timeout", DEFAULT_DOC_READY_TIMEOUT);
    }

    private boolean isWebDriver(WebDriver driver) {
        return !(driver instanceof AppiumDriver);
    }

    private void waitForDocumentReady(WebDriver driver) {
        try {
            logger.info("Wait for DOM ready ...");
            new WebDriverWait(driver, Duration.ofSeconds(getDocReadyTimeout()))
                    .until(d -> ((JavascriptExecutor) d)
                            .executeScript("return document.readyState").equals("complete"));
        } catch (Exception ignored) {}
    }

    private void waitForAjaxComplete(WebDriver driver) {
        try {
            logger.info("Wait for Ajax complete ...");
            new WebDriverWait(driver, Duration.ofSeconds(getAjaxTimeout()))
                    .until(d -> (Boolean) ((JavascriptExecutor) d)
                            .executeScript("return (typeof jQuery === 'undefined') || jQuery.active === 0"));
        } catch (Exception ignored) {}
    }

    public static ObjectUI getCurrentElement() {
        return CURRENT_ELEMENT.get();
    }


//    protected WebElement findElement(ObjectUI uiObject, Duration timeout) {
//        WebDriver driver = DriverManager.getDriver();
//        if (driver == null) {
//            throw new IllegalStateException("Driver is null while finding element: " + (uiObject != null ? uiObject.getName() : "null"));
//        }
//        List<Locator> locators = uiObject.getActiveLocators();
//        if (locators == null || locators.isEmpty()) {
//            throw new IllegalArgumentException("No active locator is defined for object: " + uiObject.getName());
//        }
//
//        for (Locator locator : locators) {
//            WebDriverWait wait = new WebDriverWait(driver, timeout, POLLING_INTERVAL);
//            try {
//                By by = locator.convertToBy();
//                logger.info("Searching for element '{}' using locator: {} (Timeout: {}s)",
//                        uiObject.getName(), locator, timeout.getSeconds());
//                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
//                if (element != null) {
//                    logger.info("Found element '{}' with locator: {}", uiObject.getName(), locator);
//                    return element;
//                }
//            } catch (Exception e) {
//                logger.debug("Element '{}' not found with locator {}. Trying next.", uiObject.getName(), locator);
//            }
//        }
//
//        logger.error("COULD NOT FIND element '{}' using any defined locators within {}s.", uiObject.getName(), timeout.getSeconds());
//        throw new NoSuchElementException("Cannot find element '" + uiObject.getName() + "' using any of the defined locators within the timeout period.");
//    }

    protected WebElement findElement(ObjectUI uiObject, Duration timeout) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            throw new IllegalStateException("Driver is null while finding element: " +
                    (uiObject != null ? uiObject.getName() : "null"));
        }

        if (isSmartWaitEnabled() && isWebDriver(driver)) {
            waitForDocumentReady(driver);
            waitForAjaxComplete(driver);
        }

        List<Locator> locators = uiObject.getActiveLocators();
        if (locators == null || locators.isEmpty()) {
            throw new IllegalArgumentException("No active locator is defined for object: " + uiObject.getName());
        }

        for (Locator locator : locators) {
            WebDriverWait wait = new WebDriverWait(driver, timeout, POLLING_INTERVAL);
            try {
                By by = locator.convertToBy();
                logger.info("Searching for element '{}' using locator: {} (Timeout: {}s)",
                        uiObject.getName(), locator, timeout.getSeconds());
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
                if (element != null) {
                    logger.info("Found element '{}' with locator: {}", uiObject.getName(), locator);
                    return element;
                }
            } catch (Exception e) {
                logger.debug("Element '{}' not found with locator {}. Trying next.", uiObject.getName(), locator);
            }
        }

        logger.error("COULD NOT FIND element '{}' using any defined locators within {}s.",
                uiObject.getName(), timeout.getSeconds());
        throw new NoSuchElementException("Cannot find element '" + uiObject.getName() +
                "' using any of the defined locators within the timeout period.");
    }

    public List<WebElement> findElements(ObjectUI uiObject) {
        return execute(() -> {
            WebDriver driver = DriverManager.getDriver();
            if (driver == null) throw new IllegalStateException("Driver is null in findElements");
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return driver.findElements(by);
        }, uiObject != null ? uiObject.getName() : "null");
    }

    protected void highlightAndScreenshot(WebElement element, String stepName) {
        if (element == null) {
            logger.warn("Element is null, cannot highlight or screenshot.");
            return;
        }

        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            logger.warn("Driver is null, cannot highlight or screenshot.");
            return;
        }
        try {
            ScreenshotUtils.highlightAndTakeScreenshot(driver, element, stepName);

        } catch (Exception e) {
            logger.warn("Could not highlight or attach screenshot: " + e.getMessage());
        }
    }

    // =================================================================================
    // --- ACTION KEYWORDS (PUBLIC/PROTECTED) ---
    // =================================================================================

//    protected void click(ObjectUI uiObject) {
//        execute(() -> {
//            WebDriver driver = DriverManager.getDriver();
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//
//            List<Locator> locators = uiObject.getActiveLocators();
//            WebElement element = null;
//            By usedBy = null;
//
//            waitForDocumentReady(driver);
//
//            for (Locator locator : locators) {
//                try {
//                    By by = locator.convertToBy();
//
//                    element = new WebDriverWait(driver, getPrimaryTimeout())
//                            .pollingEvery(Duration.ofMillis(getPollingInterval()))
//                            .ignoring(StaleElementReferenceException.class)
//                            .until(ExpectedConditions.presenceOfElementLocated(by));
//
//                    usedBy = by;
//                    break;
//
//                } catch (Exception ignore) {}
//            }
//
//            if (element == null) {
//                throw new NoSuchElementException(
//                        "Cannot find element '" + uiObject.getName() + "'");
//            }
//
//            try {
//                js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
//                js.executeScript("arguments[0].focus();", element);
//            } catch (Exception ignore) {}
//
//            try {
//                element.click();
//                return null;
//            } catch (Exception firstFail) {
//                logger.debug("Normal click failed, fallback to JS click: {}", firstFail.getClass().getSimpleName());
//            }
//
//            try {
//                element = driver.findElement(usedBy);
//            } catch (Exception ignore) {}
//
//            js.executeScript("arguments[0].click();", element);
//
//            return null;
//        }, uiObject.getName());
//    }

    private static final int MAX_STALE_RETRY = 3;
    private static final Duration CLICK_READY_TIMEOUT = Duration.ofSeconds(5); // Giảm từ 15s xuống 5s

    // ==================== MAIN CLICK METHOD ====================
    protected void click(ObjectUI uiObject) {
        execute(() -> {
            WebDriver driver = DriverManager.getDriver();
            JavascriptExecutor js = (JavascriptExecutor) driver;
            List<Locator> locators = uiObject.getActiveLocators();
            WebElement element = null;
            By usedBy = null;

            if (isWebDriver(driver)) {
                waitForDocumentReady(driver);
                waitForAjaxComplete(driver);
            }

            // BƯỚC 1: Tìm element trong DOM
            for (Locator locator : locators) {
                try {
                    By by = locator.convertToBy();
                    element = new WebDriverWait(driver, getPrimaryTimeout())
                            .pollingEvery(Duration.ofMillis(getPollingInterval()))
                            .ignoring(StaleElementReferenceException.class)
                            .until(ExpectedConditions.presenceOfElementLocated(by));

                    if (element != null) {
                        usedBy = by;
                        logger.debug("Found element '{}' in DOM with locator: {}", uiObject.getName(), locator);
                        break;
                    }
                } catch (TimeoutException e) {
                    logger.debug("Element not found with locator {}: timeout", locator);
                } catch (Exception e) {
                    logger.debug("Element not found with locator {}: {}", locator, e.getMessage());
                }
            }

            if (element == null || usedBy == null) {
                throw new NoSuchElementException("Cannot find element '" + uiObject.getName() + "' in DOM");
            }

            final By finalBy = usedBy;
            try {
                element = new WebDriverWait(driver, CLICK_READY_TIMEOUT)
                        .pollingEvery(Duration.ofMillis(getPollingInterval()))
                        .ignoring(StaleElementReferenceException.class)
                        .until(ExpectedConditions.elementToBeClickable(finalBy));
            } catch (TimeoutException e) {
                logger.debug("Element '{}' not clickable after {}s, proceeding anyway",
                        uiObject.getName(), CLICK_READY_TIMEOUT.getSeconds());
                element = driver.findElement(finalBy);
            }

            scrollIntoView(js, element);

            performClick(driver, js, element, finalBy, uiObject.getName());

            return null;
        }, uiObject != null ? uiObject.getName() : "null");
    }

    private void scrollIntoView(JavascriptExecutor js, WebElement element) {
        try {
            js.executeScript(
                    "arguments[0].scrollIntoView({behavior:'instant', block:'center', inline:'nearest'});",
                    element);
        } catch (Exception e) {
            logger.debug("Scroll into view failed: {}", e.getMessage());
        }
    }

    private void performClick(WebDriver driver, JavascriptExecutor js,
                              WebElement element, By by, String elementName) {
        Exception lastException = null;
        int staleRetry = 0;

        while (staleRetry < MAX_STALE_RETRY) {
            // Strategy 1: Normal click (thử ngay, không check trước)
            try {
                element.click();
                logger.info("Clicked '{}' successfully (normal)", elementName);
                return;
            } catch (StaleElementReferenceException e) {
                staleRetry++;
                element = driver.findElement(by);
                logger.debug("Element stale, re-finding... (attempt {})", staleRetry);
                continue;
            } catch (ElementNotInteractableException e) {
                lastException = e;
                logger.debug("Normal click failed: {}", e.getMessage());
            }

            // Click failed → CHỈ BÂY GIỜ mới check animation (lazy check)
            if (!waitForAnimationOnce(driver, element, elementName)) {
                // Animation đang chạy, chờ xong rồi thử lại normal click
                try {
                    element = driver.findElement(by);
                    element.click();
                    logger.info("Clicked '{}' successfully (after animation)", elementName);
                    return;
                } catch (Exception e) {
                    lastException = e;
                    logger.debug("Click after animation failed: {}", e.getMessage());
                }
            }

            // Strategy 2: Actions click
            try {
                new Actions(driver)
                        .moveToElement(element)
                        .pause(Duration.ofMillis(50))
                        .click()
                        .perform();
                logger.info("Clicked '{}' successfully (Actions)", elementName);
                return;
            } catch (StaleElementReferenceException e) {
                staleRetry++;
                element = driver.findElement(by);
                continue;
            } catch (Exception e) {
                lastException = e;
                logger.debug("Actions click failed: {}", e.getMessage());
            }

            // Strategy 3: JavaScript click
            try {
                element = driver.findElement(by);
                js.executeScript("arguments[0].click();", element);
                logger.info("Clicked '{}' successfully (JavaScript)", elementName);
                return;
            } catch (StaleElementReferenceException e) {
                staleRetry++;
                element = driver.findElement(by);
                continue;
            } catch (Exception e) {
                lastException = e;
                logger.debug("JavaScript click failed: {}", e.getMessage());
            }

            break;
        }

        throw new ElementClickInterceptedException(
                String.format("Cannot click element '%s' after all attempts. Last error: %s",
                        elementName, lastException != null ? lastException.getMessage() : "unknown"));
    }

    // ==================== WAIT FOR ANIMATION (CHỈ GỌI KHI CẦN) ====================
    private boolean waitForAnimationOnce(WebDriver driver, WebElement element, String elementName) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Check nhanh: có animation không?
            Boolean hasAnimation = (Boolean) js.executeScript(
                    "var el = arguments[0];" +
                            "var style = window.getComputedStyle(el);" +
                            "var cls = el.className || '';" +
                            // Quick check animation indicators
                            "return style.animationPlayState === 'running' || " +
                            "       parseFloat(style.opacity) < 0.99 || " +
                            "       /v-enter|v-leave|ng-animate|collapsing|animating/.test(cls);",
                    element);

            if (hasAnimation == null || !hasAnimation) {
                return true; // Không có animation
            }

            // Có animation → chờ tối đa 2 giây
            logger.debug("Animation detected on '{}', waiting...", elementName);

            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .pollingEvery(Duration.ofMillis(200)) // Poll chậm hơn: 200ms thay vì 100ms
                    .until(d -> {
                        Boolean done = (Boolean) ((JavascriptExecutor) d).executeScript(
                                "var el = arguments[0];" +
                                        "var style = window.getComputedStyle(el);" +
                                        "var cls = el.className || '';" +
                                        "return style.animationPlayState !== 'running' && " +
                                        "       parseFloat(style.opacity) >= 0.99 && " +
                                        "       !/v-enter|v-leave|ng-animate|collapsing|animating/.test(cls);",
                                element);
                        return done != null && done;
                    });

            return true;
        } catch (TimeoutException e) {
            logger.debug("Animation wait timeout for '{}'", elementName);
            return true; // Timeout thì cứ thử click
        } catch (Exception e) {
            logger.debug("Animation check error: {}", e.getMessage());
            return true;
        }
    }

    protected void clear(ObjectUI uiObject) {
        execute(() -> {
            performActionWithRetry(
                    uiObject,
                    ExpectedConditions::visibilityOf,
                    WebElement::clear
            );
            return null;
        }, uiObject);
    }

    protected void sendKeys(ObjectUI uiObject, String text) {
        execute(() -> {
            WebDriver driver = DriverManager.getDriver();
            List<Locator> locators = uiObject.getActiveLocators();

            if (isSmartWaitEnabled() && isWebDriver(driver)) {
                waitForDocumentReady(driver);
                waitForAjaxComplete(driver);
            }

            for (Locator locator : locators) {
                try {
                    By by = locator.convertToBy();

                    WebElement element = new WebDriverWait(driver, getPrimaryTimeout())
                            .pollingEvery(Duration.ofMillis(getPollingInterval()))
                            .ignoring(StaleElementReferenceException.class)
                            .until(ExpectedConditions.visibilityOfElementLocated(by));

                    element.clear();
                    element.sendKeys(text);

                    logger.info("Successfully sent keys to '{}' with locator: {}",
                            uiObject.getName(), locator);
                    return null;

                } catch (Exception e) {
                    logger.debug("Failed with locator {}: {}", locator, e.getMessage());
                }
            }

            throw new RuntimeException("Cannot send keys to '" + uiObject.getName() + "'");
        }, uiObject != null ? uiObject.getName() : "null");
    }

    protected void sendKeysSensitive(ObjectUI uiObject, String encryptedText) {
        execute(() -> {
            // 1. Lấy master key
            String masterKey = MasterKeyProvider.getMasterKey();

            // 2. Decrypt
            String plainText = SecretDecryptor.decrypt(encryptedText, masterKey);

            // 3. Mask cho logging
            String maskedText = maskSensitiveValue(plainText);

            // 4. Thực hiện sendKeys
            WebDriver driver = DriverManager.getDriver();
            List<Locator> locators = uiObject.getActiveLocators();

            for (Locator locator : locators) {
                try {
                    By by = locator.convertToBy();

                    WebElement element = new WebDriverWait(driver, PRIMARY_TIMEOUT)
                            .pollingEvery(POLLING_INTERVAL)
                            .ignoring(StaleElementReferenceException.class)
                            .until(ExpectedConditions.visibilityOfElementLocated(by));

                    element.clear();
                    element.sendKeys(plainText);

                    logger.info("Successfully sent sensitive keys '{}' to '{}'",
                            maskedText, uiObject.getName());
                    return null;

                } catch (Exception e) {
                    logger.debug("Failed with locator {}, trying next", locator);
                }
            }

            throw new RuntimeException("Cannot send keys to '" + uiObject.getName() + "'");
        }, uiObject != null ? uiObject.getName() : "null");
    }

    /**
     * Mask giá trị: "Secret123" → "S*****3"
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.length() == 1) return "*";
        if (value.length() == 2) return value.charAt(0) + "*";
        return value.charAt(0) + "*****" + value.charAt(value.length() - 1);
    }

    protected String getText(ObjectUI uiObject) {
        return execute(() -> {
            if (uiObject == null) {
                logger.warn("getText called with null uiObject");
                return "";
            }

            WebDriver driver = DriverManager.getDriver();
            boolean isWeb = isWebDriver(driver);

            if (isSmartWaitEnabled() && isWeb) {
                waitForDocumentReady(driver);
                waitForAjaxComplete(driver);
            }

            int attempts = 3;
            while (attempts-- > 0) {
                try {
                    WebElement element = new WebDriverWait(driver, getPrimaryTimeout())
                            .pollingEvery(Duration.ofMillis(getPollingInterval()))
                            .ignoring(StaleElementReferenceException.class)
                            .until(d -> {
                                try {
                                    WebElement el = findElement(uiObject);
                                    return (el != null && el.isDisplayed()) ? el : null;
                                } catch (StaleElementReferenceException e) {
                                    return null;
                                }
                            });

                    if (element == null) {
                        throw new StaleElementReferenceException(
                                "Cannot locate visible element for: " + uiObject.getName());
                    }

                    String text = element.getText();

                    if (isWeb) {
                        String tagName = element.getTagName().toLowerCase();

                        if ("input".equals(tagName) || "textarea".equals(tagName) || "select".equals(tagName)) {
                            String valueAttr = element.getAttribute("value");
                            if (valueAttr != null && !valueAttr.isEmpty()) {
                                text = valueAttr;
                            }
                        }

                        if (text == null || text.trim().isEmpty()) {
                            try {
                                text = (String) ((JavascriptExecutor) driver).executeScript(
                                        "return arguments[0].textContent || arguments[0].innerText;", element);
                            } catch (Exception e) {
                                logger.warn("Unable to retrieve text via JavaScript for {}", uiObject.getName());
                                text = "";
                            }
                        }
                    }

                    return (text != null) ? text : "";

                } catch (StaleElementReferenceException e) {
                    logger.warn("StaleElementReferenceException for {}. Retrying... ({} attempts left)",
                            uiObject.getName(), attempts);
                }
            }

            throw new StaleElementReferenceException(
                    "Failed to get text for '" + uiObject.getName() + "' after multiple retries.");
        }, uiObject != null ? uiObject.getName() : "null");
    }


    protected void waitForElementVisible(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            Duration totalTimeout = Duration.ofSeconds(timeoutInSeconds);
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), totalTimeout, POLLING_INTERVAL);
            wait.until(ExpectedConditions.visibilityOfElementLocated(uiObject.getActiveLocators().get(0).convertToBy()));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    protected void waitForElementNotVisible(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            Duration totalTimeout = Duration.ofSeconds(timeoutInSeconds);
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), totalTimeout, POLLING_INTERVAL);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(uiObject.getActiveLocators().get(0).convertToBy()));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    protected void waitForElementClickable(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            Duration totalTimeout = Duration.ofSeconds(timeoutInSeconds);
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), totalTimeout, POLLING_INTERVAL);
            By locator = uiObject.getActiveLocators().get(0).convertToBy();
            wait.until(ExpectedConditions.elementToBeClickable(locator));

            return null;
        }, uiObject != null ? uiObject.getName() : "null", timeoutInSeconds);
    }

    // =================================================================================
    // --- ASSERTION LOGIC (PROTECTED) dùng bởi lớp con/public keyword ---
    // =================================================================================

    protected void performTextAssertion(ObjectUI uiObject, String expectedText, boolean isSoft, String... customMessage) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            String msg = "Driver is null when checking text of '" + (uiObject != null ? uiObject.getName() : "null") + "'.";
            if (isSoft) {
                sa().fail(msg);
                return;
            } else {
                Assert.fail(msg);
            }
        }

        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT, POLLING_INTERVAL);
        String actualText = "";

        try {
            wait.until(d -> safeGetText(uiObject).equals(expectedText));
            actualText = expectedText;
            logger.info("Text of '{}' matched '{}' within {}s.", uiObject.getName(), expectedText, DEFAULT_TIMEOUT.getSeconds());

        } catch (TimeoutException e) {
            actualText = safeGetText(uiObject);
            logger.warn("Timeout after {}s waiting for text of '{}' to be '{}'. Final text was: '{}'",
                    DEFAULT_TIMEOUT.getSeconds(), uiObject.getName(), expectedText, actualText);
        }

        logger.info("Final text assertion for '{}': expected='{}', actual='{}'", uiObject.getName(), expectedText, actualText);

        String baseMessage = String.format("Text of '%s' does not match. Expected '%s' but found '%s'.",
                uiObject.getName(), expectedText, actualText);
        String finalMessage = appendCustom(baseMessage, customMessage);

        if (isSoft) {
            sa().assertEquals(actualText, expectedText, finalMessage);
        } else {
            Assert.assertEquals(actualText, expectedText, finalMessage);
        }
    }

    protected void performTextContainsAssertion(ObjectUI uiObject, String partialText, boolean isSoft, String... customMessage) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            String msg = "Driver is null when checking text of '" + (uiObject != null ? uiObject.getName() : "null") + "'.";
            if (isSoft) {
                sa().fail(msg);
                return;
            } else {
                Assert.fail(msg);
            }
        }

        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT, POLLING_INTERVAL);
        boolean contains = false;
        String actualText = "";

        try {

            wait.until(d -> {
                String currentText = safeGetText(uiObject);
                return currentText != null && currentText.contains(partialText);
            });

            contains = true;
            logger.info("Text of '{}' contains '{}' within {}s.", uiObject.getName(), partialText, DEFAULT_TIMEOUT.getSeconds());

        } catch (TimeoutException e) {
            // 3. Nếu hết thời gian chờ, điều kiện không được đáp ứng
            contains = false;
            logger.warn("Timeout after {}s waiting for text of '{}' to contain '{}'.",
                    DEFAULT_TIMEOUT.getSeconds(), uiObject.getName(), partialText);
        }

        actualText = safeGetText(uiObject);
        logger.info("Final text contains assertion for '{}': check if '{}' contains '{}'", uiObject.getName(), actualText, partialText);

        String baseMessage = String.format("Text of '%s' was expected to contain '%s', but the actual text was '%s'.",
                uiObject.getName(), partialText, actualText);
        String finalMessage = appendCustom(baseMessage, customMessage);

        if (isSoft) {
            sa().assertTrue(contains, finalMessage);
        } else {
            Assert.assertTrue(contains, finalMessage);
        }
    }

    protected void performAttributeAssertion(ObjectUI uiObject, String attributeName, String expectedValue, boolean isSoft, String... customMessage) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            String msg = "Driver is null when checking text of '" + (uiObject != null ? uiObject.getName() : "null") + "'.";
            if (isSoft) {
                sa().fail(msg);
                return;
            } else {
                Assert.fail(msg);
            }
        }

        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT, POLLING_INTERVAL);
        String actualValue = null;
        boolean attributeMatched = false;

        try {
            wait.until(d -> {
                WebElement el = findElement(uiObject);
                String currentValue = el.getAttribute(attributeName);
                return expectedValue.equals(currentValue);
            });

            attributeMatched = true;
            actualValue = expectedValue; // Gán giá trị mong đợi để báo cáo thành công
            logger.info("Attribute '{}' of '{}' matched '{}' within {}s.", attributeName, uiObject.getName(), expectedValue, DEFAULT_TIMEOUT.getSeconds());

        } catch (TimeoutException e) {
            attributeMatched = false;
            try {
                actualValue = findElement(uiObject).getAttribute(attributeName);
            } catch (Exception findEx) {
                actualValue = "UNABLE_TO_RETRIEVE";
            }
            logger.warn("Timeout after {}s waiting for attribute '{}' of '{}' to be '{}'. Final value was: '{}'",
                    DEFAULT_TIMEOUT.getSeconds(), attributeName, uiObject.getName(), expectedValue, actualValue);
        }

        String baseMessage = String.format("Attribute '%s' of '%s' does not match. Expected '%s' but found '%s'.",
                attributeName, uiObject.getName(), expectedValue, actualValue);
        String finalMessage = appendCustom(baseMessage, customMessage);

        if (isSoft) {
            sa().assertEquals(actualValue, expectedValue, finalMessage);
        } else {
            Assert.assertEquals(actualValue, expectedValue, finalMessage);
        }
    }

    protected void performStateAssertion(ObjectUI uiObject, boolean expectedState, boolean isSoft, String... customMessage) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            String msg = "Driver is null when checking text of '" + (uiObject != null ? uiObject.getName() : "null") + "'.";
            if (isSoft) {
                sa().fail(msg);
                return;
            } else {
                Assert.fail(msg);
            }
        }

        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT, POLLING_INTERVAL);
        boolean actualState = !expectedState;

        try {
            WebElement element = findElement(uiObject);

            if (expectedState) {

                wait.until(ExpectedConditions.elementToBeClickable(element));
            } else {
                wait.until(ExpectedConditions.not(ExpectedConditions.elementToBeClickable(element)));
            }

            actualState = expectedState;
            logger.info("'enabled' state of '{}' matched '{}' within {}s.", uiObject.getName(), expectedState, DEFAULT_TIMEOUT.getSeconds());

        } catch (TimeoutException e) {
            try {
                actualState = safeIsEnabled(findElement(uiObject));
            } catch (Exception findEx) {
            }
            logger.warn("Timeout after {}s waiting for 'enabled' state of '{}' to be '{}'. Final state was: '{}'",
                    DEFAULT_TIMEOUT.getSeconds(), uiObject.getName(), expectedState, actualState);
        }

        String baseMessage = String.format("'enabled' state of '%s' does not match. Expected '%b' but found '%b'.",
                uiObject.getName(), expectedState, actualState);
        String finalMessage = appendCustom(baseMessage, customMessage);

        if (isSoft) {
            sa().assertEquals(actualState, expectedState, finalMessage);
        } else {
            Assert.assertEquals(actualState, expectedState, finalMessage);
        }
    }

    protected void performSelectionAssertion(ObjectUI uiObject, boolean expectedSelection, boolean isSoft, String... customMessage) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            String msg = "Driver is null when checking text of '" + (uiObject != null ? uiObject.getName() : "null") + "'.";
            if (isSoft) {
                sa().fail(msg);
                return;
            } else {
                Assert.fail(msg);
            }
        }

        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT, POLLING_INTERVAL);
        boolean actualSelection = !expectedSelection; // Khởi tạo giá trị ngược

        try {
            wait.until(ExpectedConditions.elementSelectionStateToBe(findElement(uiObject), expectedSelection));

            actualSelection = expectedSelection;
            logger.info("Selection state of '{}' matched '{}' within {}s.", uiObject.getName(), expectedSelection, DEFAULT_TIMEOUT.getSeconds());

        } catch (TimeoutException e) {
            try {
                actualSelection = "true".equalsIgnoreCase(String.valueOf(findElement(uiObject).getDomProperty("checked")));
            } catch (Exception findEx) {
            }
            logger.warn("Timeout after {}s waiting for selection state of '{}' to be '{}'. Final state was: '{}'",
                    DEFAULT_TIMEOUT.getSeconds(), uiObject.getName(), expectedSelection, actualSelection);
        }

        String baseMessage = String.format("Selection state of '%s' does not match. Expected '%b' but found '%b'.",
                uiObject.getName(), expectedSelection, actualSelection);
        String finalMessage = appendCustom(baseMessage, customMessage);

        if (isSoft) {
            sa().assertEquals(actualSelection, expectedSelection, finalMessage);
        } else {
            Assert.assertEquals(actualSelection, expectedSelection, finalMessage);
        }
    }

    protected void performRegexAssertion(ObjectUI uiObject, String pattern, boolean isSoft, String... customMessage) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            String msg = "Driver is null when checking text of '" + (uiObject != null ? uiObject.getName() : "null") + "'.";
            if (isSoft) {
                sa().fail(msg);
                return;
            } else {
                Assert.fail(msg);
            }
        }

        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT, POLLING_INTERVAL);
        boolean matches = false;
        String actualText = "";

        try {
            wait.until(d -> {
                String currentText = safeGetText(uiObject);
                return currentText != null && currentText.matches(pattern);
            });

            matches = true;
            logger.info("Text of '{}' matched regex '{}' within {}s.", uiObject.getName(), pattern, DEFAULT_TIMEOUT.getSeconds());

        } catch (TimeoutException e) {
            matches = false;
            logger.warn("Timeout after {}s waiting for text of '{}' to match regex '{}'.",
                    DEFAULT_TIMEOUT.getSeconds(), uiObject.getName(), pattern);
        }

        actualText = safeGetText(uiObject);

        String baseMessage = String.format("Text of '%s' was expected to match regex '%s', but the actual text was '%s'.",
                uiObject.getName(), pattern, actualText);
        String finalMessage = appendCustom(baseMessage, customMessage);

        if (isSoft) {
            sa().assertTrue(matches, finalMessage);
        } else {
            Assert.assertTrue(matches, finalMessage);
        }
    }

    protected void performVisibilityAssertion(ObjectUI uiObject,
                                              boolean expectedVisibility,
                                              boolean isSoft,
                                              String... customMessage) {
        boolean actualVisibility = false;
        String elementName = uiObject != null ? uiObject.getName() : "null";

        try {
            WebDriver driver = DriverManager.getDriver();
            if (driver == null) {
                String msg = "Driver is null when checking visibility of '" + elementName + "'.";
                if (isSoft) {
                    sa().fail(msg);
                    return;
                } else {
                    Assert.fail(msg);
                }
            }

            WebElement el = findElement(uiObject);
            Duration timeout = Duration.ofSeconds(20);
            WebDriverWait wait = new WebDriverWait(driver, timeout, POLLING_INTERVAL);

            if (expectedVisibility) {
                wait.until(ExpectedConditions.visibilityOf(el));
                actualVisibility = true;
            } else {
                try {
                    wait.until(ExpectedConditions.invisibilityOf(el));
                    actualVisibility = false;
                } catch (Exception ignore) {
                    actualVisibility = safeIsDisplayed(el);
                }
            }
        } catch (Exception e) {
            actualVisibility = false;
        }

        String baseMessage = String.format("Element '%s' expected visibility is %b but actual is %b.",
                elementName, expectedVisibility, actualVisibility);
        String finalMessage = appendCustom(baseMessage, customMessage);

        logger.info("Checking visibility of '{}': expected={}, actual={}", elementName, expectedVisibility, actualVisibility);

        if (isSoft) {
            sa().assertEquals(actualVisibility, expectedVisibility, finalMessage);
        } else {
            Assert.assertEquals(actualVisibility, expectedVisibility, finalMessage);
        }
    }

    protected void performCssValueAssertion(ObjectUI uiObject, String cssName, String expectedValue, boolean isSoft, String... customMessage) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            String msg = "Driver is null when checking text of '" + (uiObject != null ? uiObject.getName() : "null") + "'.";
            if (isSoft) {
                sa().fail(msg);
                return;
            } else {
                Assert.fail(msg);
            }
        }
        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT, POLLING_INTERVAL);
        String actualValue;
        try {
            wait.until(d -> {
                WebElement el = findElement(uiObject);
                String currentValue = el.getCssValue(cssName);
                return expectedValue.equalsIgnoreCase(currentValue);
            });

            actualValue = expectedValue;
            logger.info("CSS value '{}' of '{}' matched '{}' within {}s.", cssName, uiObject.getName(), expectedValue, DEFAULT_TIMEOUT.getSeconds());

        } catch (TimeoutException e) {
            try {
                actualValue = findElement(uiObject).getCssValue(cssName);
            } catch (Exception findEx) {
                actualValue = "UNABLE_TO_RETRIEVE";
            }
            logger.warn("Timeout after {}s waiting for CSS value '{}' of '{}' to be '{}'. Final value was: '{}'",
                    DEFAULT_TIMEOUT.getSeconds(), cssName, uiObject.getName(), expectedValue, actualValue);
        }

        String baseMessage = String.format("CSS value '%s' of '%s' does not match. Expected '%s' but found '%s'.",
                cssName, uiObject.getName(), expectedValue, actualValue);
        String finalMessage = appendCustom(baseMessage, customMessage);

        if (isSoft) {
            sa().assertEquals(actualValue, expectedValue, finalMessage);
        } else {
            Assert.assertEquals(actualValue, expectedValue, finalMessage);
        }
    }

    protected void performAttributeContainsAssertion(ObjectUI uiObject,
                                                     String attributeName,
                                                     String expectedSubstring,
                                                     boolean isSoft,
                                                     String... customMessage) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            String msg = "Driver is null when checking text of '" + (uiObject != null ? uiObject.getName() : "null") + "'.";
            if (isSoft) {
                sa().fail(msg);
                return;
            } else {
                Assert.fail(msg);
            }
        }

        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT, POLLING_INTERVAL);
        boolean contains = false;
        String actualValue = null;

        try {
            wait.until(d -> {
                WebElement el = findElement(uiObject);
                String currentValue = el.getAttribute(attributeName);
                return currentValue != null && currentValue.contains(expectedSubstring);
            });
            contains = true;
            logger.info("Attribute '{}' of '{}' contains '{}' within {}s.",
                    attributeName, uiObject.getName(), expectedSubstring, DEFAULT_TIMEOUT.getSeconds());

        } catch (TimeoutException e) {

            contains = false;
            logger.warn("Timeout after {}s waiting for attribute '{}' of '{}' to contain '{}'.",
                    DEFAULT_TIMEOUT.getSeconds(), attributeName, uiObject.getName(), expectedSubstring);
        }

        try {
            actualValue = findElement(uiObject).getAttribute(attributeName);
        } catch (Exception findEx) {
            actualValue = "UNABLE_TO_RETRIEVE";
        }

        String baseMessage = String.format(
                "Attribute '%s' of '%s' was expected to contain '%s', but the actual value was '%s'.",
                attributeName, uiObject.getName(), expectedSubstring, actualValue
        );
        String finalMessage = appendCustom(baseMessage, customMessage);

        if (isSoft) {
            sa().assertTrue(contains, finalMessage);
        } else {
            Assert.assertTrue(contains, finalMessage);
        }
    }

    // =================================================================================
    // --- HARD checks tiện dụng ---
    // =================================================================================

    public void verifyElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            try {
                Duration totalTimeout = Duration.ofSeconds(timeoutInSeconds);
                WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), totalTimeout, POLLING_INTERVAL);
                By locator = uiObject.getActiveLocators().get(0).convertToBy();
                wait.until(ExpectedConditions.presenceOfElementLocated(locator));

            } catch (TimeoutException e) {
                throw new AssertionError("Element '" + uiObject.getName() + "' does not exist in the DOM within " + timeoutInSeconds + "s.");
            }
            return null;
        }, uiObject != null ? uiObject.getName() : "null", timeoutInSeconds);
    }

    public void verifyElementVisibleHard(ObjectUI uiObject, boolean isVisible) {
        performVisibilityAssertion(uiObject, isVisible, false);
    }

    public void verifyTextHard(ObjectUI uiObject, String expectedText) {
        performTextAssertion(uiObject, expectedText, false);
    }

    // =================================================================================
    // --- Helpers ---
    // =================================================================================

    protected String appendCustom(String base, String... customMessage) {
        if (customMessage != null && customMessage.length > 0
                && customMessage[0] != null && !customMessage[0].trim().isEmpty()) {
            return base + " | " + customMessage[0].trim();
        }
        return base;
    }

    private String safeGetText(ObjectUI uiObject) {
        try {
            return getText(uiObject);
        } catch (Throwable t) {
            return "";
        }
    }

    private boolean safeIsDisplayed(WebElement el) {
        try {
            return el != null && el.isDisplayed();
        } catch (Throwable ignore) {
            return false;
        }
    }

    private boolean safeIsEnabled(WebElement el) {
        try {
            return el != null && el.isEnabled();
        } catch (Throwable ignore) {
            return false;
        }
    }

    protected SoftAssert sa() {
        return ExecutionContext.getInstance().getSoftAssert();
    }

    @Deprecated
    protected SoftAssert getSoftAssert() {
        return sa();
    }

    protected String getAttribute(ObjectUI uiObject, String attributeName) {
        return execute(() -> findElement(uiObject).getAttribute(attributeName),
                uiObject != null ? uiObject.getName() : "null", attributeName);
    }

    protected boolean _isElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        WebDriver driver = DriverManager.getDriver();
        By by = uiObject.getActiveLocators().get(0).convertToBy();
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
            wait.until(d -> !d.findElements(by).isEmpty());
            return true;
        } catch (TimeoutException e) {
            return false;
        } finally { // Khôi phục lại implicit wait về giá trị mặc định của framework (là 0)
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        }
    }
}
