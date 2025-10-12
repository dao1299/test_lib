package com.vtnet.netat.core;

import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.ui.Locator;
import com.vtnet.netat.core.ui.ObjectUI;
import com.vtnet.netat.driver.DriverManager;
import org.openqa.selenium.*;
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

/**
 * Base chung cho keyword UI (Web/Mobile).
 * Lưu ý: các method protected/public ở đây nên gọi qua execute(...) ở lớp con/public.
 */
public abstract class BaseUiKeyword extends BaseKeyword {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration PRIMARY_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration SECONDARY_TIMEOUT = Duration.ofSeconds(5);
    protected static final Duration POLLING_INTERVAL = Duration.ofMillis(100);
    private static final Logger log = LoggerFactory.getLogger(BaseUiKeyword.class);

    protected WebElement findElement(ObjectUI uiObject) {
        // Gọi đến phương thức findElement mới với thời gian chờ mặc định (PRIMARY_TIMEOUT)
        return findElement(uiObject, PRIMARY_TIMEOUT);
    }


    protected WebElement findElement(ObjectUI uiObject, Duration timeout) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            throw new IllegalStateException("Driver is null while finding element: " + (uiObject != null ? uiObject.getName() : "null"));
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

        logger.error("COULD NOT FIND element '{}' using any defined locators within {}s.", uiObject.getName(), timeout.getSeconds());
        throw new NoSuchElementException("Cannot find element '" + uiObject.getName() + "' using any of the defined locators within the timeout period.");
    }

    public List<WebElement> findElements(ObjectUI uiObject) {
        return execute(() -> {
            WebDriver driver = DriverManager.getDriver();
            if (driver == null) throw new IllegalStateException("Driver is null in findElements");
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return driver.findElements(by);
        }, uiObject != null ? uiObject.getName() : "null");
    }

    // =================================================================================
    // --- ACTION KEYWORDS (PUBLIC/PROTECTED) ---
    // =================================================================================

    protected void click(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            WebDriver driver = DriverManager.getDriver();
            new WebDriverWait(driver, PRIMARY_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(element))
                    .click();
            return null;
        }, uiObject != null ? uiObject.getName() : "null");
    }

    protected void clear(ObjectUI uiObject) {
        execute(() -> {
            findElement(uiObject).clear();
            return null;
        }, uiObject != null ? uiObject.getName() : "null");
    }

    protected void sendKeys(ObjectUI uiObject, String text) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            WebDriver driver = DriverManager.getDriver();
            new WebDriverWait(driver, PRIMARY_TIMEOUT).until(ExpectedConditions.visibilityOf(element));
            element.clear();
            element.sendKeys(text);
            return null;
        }, uiObject != null ? uiObject.getName() : "null", text);
    }

    protected String getText(ObjectUI uiObject) {
        return execute(() -> {
            WebElement element = findElement(uiObject);
            new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT).until(ExpectedConditions.visibilityOf(element));

            String text = element.getText();
            boolean isMobile = DriverManager.getDriver() instanceof io.appium.java_client.AppiumDriver;

            if (!isMobile) {
                String tagName = element.getTagName().toLowerCase();
                if ("input".equals(tagName) || "textarea".equals(tagName) || "select".equals(tagName)) {
                    String valueAttr = element.getAttribute("value");
                    if (valueAttr != null && !valueAttr.isEmpty()) {
                        text = valueAttr;
                    }
                }

                if (text == null || text.trim().isEmpty()) {
                    try {
                        JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
                        text = (String) js.executeScript("return arguments[0].textContent || arguments[0].innerText;", element);
                    } catch (Exception e) {
                        logger.warn("Unable to retrieve text via JavaScript. Returning empty string.");
                        text = "";
                    }
                }
            }

            return text != null ? text : "";
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
