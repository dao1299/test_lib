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
    private static final Logger log = LoggerFactory.getLogger(BaseUiKeyword.class);

    /**
     * Tìm WebElement theo danh sách locator (ưu tiên default, fallback các locator còn lại)
     */
    protected WebElement findElement(ObjectUI uiObject) {
        WebDriver driver = DriverManager.getDriver();
        if (driver == null) {
            throw new IllegalStateException("Driver is null while finding element: " + (uiObject != null ? uiObject.getName() : "null"));
        }

        List<Locator> originalLocators = uiObject.getActiveLocators();
        if (originalLocators == null || originalLocators.isEmpty()) {
            throw new IllegalArgumentException("No active locator is defined for object: " + uiObject.getName());
        }

        List<Locator> searchOrder = new ArrayList<>();
        Locator defaultLocator = null;

        for (Locator loc : originalLocators) {
            if (loc.isDefault()) {
                defaultLocator = loc;
                break;
            }
        }
        if (defaultLocator != null) searchOrder.add(defaultLocator);
        for (Locator loc : originalLocators) {
            if (!loc.equals(defaultLocator)) searchOrder.add(loc);
        }

        for (int i = 0; i < searchOrder.size(); i++) {
            Locator locator = searchOrder.get(i);
            Duration timeout = (i == 0) ? PRIMARY_TIMEOUT : SECONDARY_TIMEOUT;
            WebDriverWait wait = new WebDriverWait(driver, timeout);
            try {
                By by = locator.convertToBy();
                logger.info("Searching element '{}' using locator: {} ({}; {}s)",
                        uiObject.getName(), locator, (i == 0 && defaultLocator != null) ? "DEFAULT" : "FALLBACK", timeout.getSeconds());
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
                if (element != null) {
                    logger.info("Found element '{}' with locator: {}", uiObject.getName(), locator);
                    return element;
                }
            } catch (Exception e) {
                logger.debug("Element '{}' not found with locator {}. Try next.", uiObject.getName(), locator);
            }
        }

        logger.error("COULD NOT FIND element '{}' using any defined locators.", uiObject.getName());
        throw new NoSuchElementException("Cannot find element '" + uiObject.getName() + "' using defined locators.");
    }

    /**
     * Tìm nhiều phần tử (gọi qua execute để Allure có params)
     */
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

            String text;
            String tagName = element.getTagName().toLowerCase();
            if ("input".equals(tagName) || "textarea".equals(tagName) || "select".equals(tagName)) {
                text = element.getAttribute("value");
            } else {
                text = element.getText();
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
            return text != null ? text.trim() : "";
        }, uiObject != null ? uiObject.getName() : "null");
    }

    protected void waitForElementVisible(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.visibilityOf(findElement(uiObject)));
            return null;
        }, uiObject != null ? uiObject.getName() : "null", timeoutInSeconds);
    }

    protected void waitForElementNotVisible(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.invisibilityOf(findElement(uiObject)));
            return null;
        }, uiObject != null ? uiObject.getName() : "null", timeoutInSeconds);
    }

    protected void waitForElementClickable(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.elementToBeClickable(findElement(uiObject)));
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

        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
        String actualText;
        try {
            wait.until(d -> {
                String t = safeGetText(uiObject);
                return Objects.equals(t, expectedText);
            });
            logger.info("Text of '{}' matched '{}' within {}s.", uiObject.getName(), expectedText, DEFAULT_TIMEOUT.getSeconds());
        } catch (TimeoutException e) {
            logger.warn("Timeout {}s waiting text of '{}' to be '{}'.", DEFAULT_TIMEOUT.getSeconds(), uiObject.getName(), expectedText);
        }
        actualText = safeGetText(uiObject);

        logger.info("Final text assertion for '{}': expected='{}', actual='{}'", uiObject.getName(), expectedText, actualText);

        String baseMessage = "Text of '" + uiObject.getName() + "' does not match within " + DEFAULT_TIMEOUT.getSeconds() + "s.";
        String finalMessage = appendCustom(baseMessage, customMessage);

        if (isSoft) {
            sa().assertEquals(actualText, expectedText, finalMessage);
        } else {
            Assert.assertEquals(actualText, expectedText, finalMessage);
        }
    }

    protected void performTextContainsAssertion(ObjectUI uiObject, String partialText, boolean isSoft, String... customMessage) {
        String actualText = safeGetText(uiObject);
        boolean contains = actualText != null && actualText.contains(partialText);

        String baseMessage = "Text of '" + uiObject.getName() + "' ('" + actualText + "') does not contain '" + partialText + "'.";
        String finalMessage = appendCustom(baseMessage, customMessage);

        logger.info("Checking '{}' contains '{}': {}", uiObject.getName(), partialText, contains);

        if (isSoft) {
            sa().assertTrue(contains, finalMessage);
        } else {
            Assert.assertTrue(contains, finalMessage);
        }
    }

    protected void performAttributeAssertion(ObjectUI uiObject, String attributeName, String expectedValue, boolean isSoft, String... customMessage) {
        WebElement element = findElement(uiObject);
        String actualValue = element.getAttribute(attributeName);

        String baseMessage = "Attribute '" + attributeName + "' of '" + uiObject.getName() + "' does not match.";
        String finalMessage = appendCustom(baseMessage, customMessage);

        logger.info("Checking attribute '{}' of '{}': expected='{}', actual='{}'",
                attributeName, uiObject.getName(), expectedValue, actualValue);

        if (isSoft) {
            sa().assertEquals(actualValue, expectedValue, finalMessage);
        } else {
            Assert.assertEquals(actualValue, expectedValue, finalMessage);
        }
    }

    protected void performStateAssertion(ObjectUI uiObject, boolean expectedState, boolean isSoft, String... customMessage) {
        boolean actualState = safeIsEnabled(findElement(uiObject));

        String baseMessage = String.format("'enabled' state of '%s' expected %b but actual is %b.",
                uiObject.getName(), expectedState, actualState);
        String finalMessage = appendCustom(baseMessage, customMessage);

        logger.info("Checking 'enabled' of '{}': expected={}, actual={}", uiObject.getName(), expectedState, actualState);

        if (isSoft) {
            sa().assertEquals(actualState, expectedState, finalMessage);
        } else {
            Assert.assertEquals(actualState, expectedState, finalMessage);
        }
    }

    protected void performSelectionAssertion(ObjectUI uiObject, boolean expectedSelection, boolean isSoft, String... customMessage) {
        boolean actualSelection = "true".equalsIgnoreCase(String.valueOf(findElement(uiObject).getDomProperty("checked")));

        String baseMessage = String.format("Selection state of '%s' expected %b but actual is %b.",
                uiObject.getName(), expectedSelection, actualSelection);
        String finalMessage = appendCustom(baseMessage, customMessage);

        logger.info("Checking selection of '{}': expected={}, actual={}", uiObject.getName(), expectedSelection, actualSelection);

        if (isSoft) {
            sa().assertEquals(actualSelection, expectedSelection, finalMessage);
        } else {
            Assert.assertEquals(actualSelection, expectedSelection, finalMessage);
        }
    }

    protected void performRegexAssertion(ObjectUI uiObject, String pattern, boolean isSoft, String... customMessage) {
        String actualText = safeGetText(uiObject);
        boolean matches = actualText != null && actualText.matches(pattern);

        String baseMessage = String.format("Text '%s' of '%s' does not match regex '%s'.",
                actualText, uiObject.getName(), pattern);
        String finalMessage = appendCustom(baseMessage, customMessage);

        logger.info("Checking regex on '{}': pattern='{}', matches={}", uiObject.getName(), pattern, matches);

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
            Duration timeout = Duration.ofSeconds(3);
            WebDriverWait wait = new WebDriverWait(driver, timeout);

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
        String actualValue = findElement(uiObject).getCssValue(cssName);

        String baseMessage = String.format("CSS value '%s' of '%s' expected '%s' but actual '%s'",
                cssName, uiObject.getName(), expectedValue, actualValue);
        String finalMessage = appendCustom(baseMessage, customMessage);

        logger.info("Checking CSS '{}' of '{}': expected='{}', actual='{}'",
                cssName, uiObject.getName(), expectedValue, actualValue);

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
        // Lấy element & đọc giá trị attribute
        WebElement element = findElement(uiObject);
        String actual = (element != null) ? element.getAttribute(attributeName) : null;
        String elemName = (uiObject != null ? uiObject.getName() : "null");

        boolean contains = (actual != null) && actual.contains(expectedSubstring);

        // Message gọn + có thể thêm customMessage
        String baseMessage = String.format(
                "Attribute '%s' of '%s' ('%s') does not contain '%s'.",
                attributeName, elemName, String.valueOf(actual), expectedSubstring
        );
        String finalMessage = appendCustom(baseMessage, customMessage);

        logger.info("Checking attribute contains: element='{}', attr='{}', expected-sub='{}', actual='{}', result={}",
                elemName, attributeName, expectedSubstring, actual, contains);

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
                WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
                wait.until(d -> findElement(uiObject) != null);
            } catch (Exception e) {
                throw new AssertionError("Element '" + uiObject.getName() + "' does not exist within " + timeoutInSeconds + "s.");
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

    private String appendCustom(String base, String... customMessage) {
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

    /**
     * AllureSoftAssert lấy từ ExecutionContext (Cách A)
     */
    protected SoftAssert sa() {
        return ExecutionContext.getInstance().getSoftAssert();
    }

    /**
     * Legacy getter (giữ để tương thích nếu nơi khác đang dùng)
     */
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
