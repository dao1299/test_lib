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
import java.util.List;
import java.util.Objects;

/**
 * Lớp trừu tượng cơ sở cho tất cả các keyword liên quan đến UI (Web, Mobile).
 * Chứa logic chung để tìm kiếm phần tử và các hành động, kiểm chứng cơ bản.
 * Lưu ý: Các phương thức trong lớp này không gọi cỗ máy 'execute()'. Việc gọi 'execute()' là trách nhiệm của các keyword public ở lớp con.
 */
public abstract class BaseUiKeyword extends BaseKeyword {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration PRIMARY_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration SECONDARY_TIMEOUT = Duration.ofSeconds(5);
    private static final Logger log = LoggerFactory.getLogger(BaseUiKeyword.class);

    /**
     * Phương thức tìm kiếm WebElement chung cho cả Web và Mobile.
     */
    protected WebElement findElement(ObjectUI uiObject) {
        WebDriver driver = DriverManager.getDriver();
        List<Locator> locators = uiObject.getActiveLocators();

        if (locators.isEmpty()) {
            throw new IllegalArgumentException("No active locator is defined for object: " + uiObject.getName());
        }

        for (int i = 0; i < locators.size(); i++) {
            Locator locator = locators.get(i);
            Duration timeout = (i == 0) ? PRIMARY_TIMEOUT : SECONDARY_TIMEOUT;
            WebDriverWait wait = new WebDriverWait(driver, timeout);

            try {
                By by = locator.convertToBy();
                logger.info("Searching for element '{}' using locator: {} (Timeout: {}s)", uiObject.getName(), locator, timeout.getSeconds());
                WebElement element =  wait.until(ExpectedConditions.presenceOfElementLocated(by));
                System.out.println(element);
                return element;
            } catch (Exception e) {
                logger.warn("Element '{}' not found with locator {}.", uiObject.getName(), locator);
            }
        }

        throw new NoSuchElementException("Cannot find element '" + uiObject.getName() + "' using the defined locators.");
    }

    public List<WebElement> findElements(ObjectUI uiObject) {
        return execute(() -> {
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return DriverManager.getDriver().findElements(by);
        }, uiObject);
    }

    // =================================================================================
    // --- CÁC KEYWORD HÀNH ĐỘNG CHUNG (PUBLIC) ---
    // =================================================================================

    protected void click(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            try {
                new WebDriverWait(DriverManager.getDriver(), PRIMARY_TIMEOUT)
                        .until(ExpectedConditions.elementToBeClickable(element)).click();
            } catch (ElementClickInterceptedException e) {
                logger.warn("Normal click blocked. Try again with JavaScript for object: " + uiObject.getName());
                JavascriptExecutor executor = (JavascriptExecutor) DriverManager.getDriver();
                executor.executeScript("arguments[0].click();", element);
            }
            return null;
        }, uiObject);
    }

    protected void clear(ObjectUI uiObject) {
        execute(() -> {
            findElement(uiObject).clear();
            return null;
        }, uiObject);
    }

    protected void sendKeys(ObjectUI uiObject, String text) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new WebDriverWait(DriverManager.getDriver(), PRIMARY_TIMEOUT)
                    .until(ExpectedConditions.visibilityOf(element));
            element.clear();
            element.sendKeys(text);
            return null;
        }, uiObject, text);
    }

    protected String getText(ObjectUI uiObject) {
        return execute(() -> {
            // Tìm và chờ cho đến khi phần tử được hiển thị để đảm bảo nó đã sẵn sàng
            WebElement element = findElement(uiObject);
            new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOf(element));

            String text;
            String tagName = element.getTagName().toLowerCase();

            // **Ưu tiên 1: Lấy thuộc tính 'value' cho các thẻ input, textarea, select**
            // Đây là trường hợp phổ biến nhất mà .getText() của Selenium không xử lý được.
            if ("input".equals(tagName) || "textarea".equals(tagName) || "select".equals(tagName)) {
                text = element.getAttribute("value");
            } else {
                // **Ưu tiên 2: Lấy văn bản hiển thị thông thường**
                text = element.getText();
            }

            // **Ưu tiên 3 (Dự phòng): Nếu các cách trên không trả về giá trị, thử lấy qua JavaScript**
            // 'textContent' và 'innerText' có thể lấy được văn bản ẩn hoặc các nội dung phức tạp hơn.
            if (text == null || text.trim().isEmpty()) {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
                    text = (String) js.executeScript("return arguments[0].textContent || arguments[0].innerText;", element);
                } catch (Exception e) {
                    logger.warn("Unable to retrieve text via JavaScript. Returning empty string.");
                    text = ""; // Trả về rỗng nếu có lỗi xảy ra
                }
            }

            // Trả về kết quả, đảm bảo không bao giờ là null và đã được cắt khoảng trắng thừa
            return text != null ? text.trim() : "";
        }, uiObject);
    }

    protected void waitForElementVisible(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.visibilityOf(findElement(uiObject)));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    protected void waitForElementNotVisible(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.invisibilityOf(findElement(uiObject)));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    protected void waitForElementClickable(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.elementToBeClickable(findElement(uiObject)));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    // =================================================================================
    // --- CÁC PHƯƠNG THỨC LOGIC ASSERT (PROTECTED) ĐỂ LỚP CON SỬ DỤNG ---
    // =================================================================================

    protected void performVisibilityAssertion(ObjectUI uiObject, boolean expectedVisibility, boolean isSoft) {
        boolean actualVisibility;
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(3));
            wait.until(ExpectedConditions.visibilityOf(findElement(uiObject)));
            actualVisibility = true;
        } catch (Exception e) {
            actualVisibility = false;
        }

        String message = String.format("Element '%s' expected visibility is %b but actual is %b.",
                uiObject.getName(), expectedVisibility, actualVisibility);
        logger.info(String.format("Checking element '%s' expected visibility is %b and actual is %b.",
                uiObject.getName(), expectedVisibility, actualVisibility));

        if (isSoft) {
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
            softAssert.assertEquals(actualVisibility, expectedVisibility, "SOFT ASSERT FAILED: " + message);
        } else {
            Assert.assertEquals(actualVisibility, expectedVisibility, "HARD ASSERT FAILED: " + message);
        }
    }

    protected void performTextAssertion(ObjectUI uiObject, String expectedText, boolean isSoft) {
        WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(DEFAULT_TIMEOUT.getSeconds()));
        String actualText = "";
        try {
            wait.until(driver -> getText(uiObject).equals(expectedText));
            logger.info("Condition met: Text of element '{}' matches '{}' within {} seconds.", uiObject.getName(), expectedText, DEFAULT_TIMEOUT.getSeconds());
        } catch (TimeoutException e) {
            logger.warn("Timeout after {} seconds waiting for text of element '{}' to be '{}'.", DEFAULT_TIMEOUT.getSeconds(), uiObject.getName(), expectedText);
        }
        actualText = getText(uiObject);
        logger.info("Performing final assertion: Expected value: '{}' , Actual value: '{}' of object {}", expectedText, actualText, uiObject.getName());
        String message = "Text of element '" + uiObject.getName() + "' does not match after waiting for " + DEFAULT_TIMEOUT.getSeconds() + " seconds.";
        if (isSoft) {
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
            softAssert.assertEquals(actualText, expectedText, "SOFT ASSERT FAILED: " + message);
        } else {
            Assert.assertEquals(actualText, expectedText, "HARD ASSERT FAILED: " + message);
        }
    }

    protected void performTextContainsAssertion(ObjectUI uiObject, String partialText, boolean isSoft) {
        String actualText = getText(uiObject);
        boolean isContains = actualText.contains(partialText);
        String message = "Text of element '" + uiObject.getName() + "' ('" + actualText + "') does not contain '" + partialText + "'.";
        logger.info("Checking text of element '{}' ('{}') should contain '{}' .", uiObject.getName(), actualText, partialText);
        if (isSoft) {
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
            softAssert.assertTrue(isContains, "SOFT ASSERT FAILED: " + message);
        } else {
            Assert.assertTrue(isContains, "HARD ASSERT FAILED: " + message);
        }
    }

    protected void performAttributeAssertion(ObjectUI uiObject, String attributeName, String expectedValue, boolean isSoft) {
        WebElement element = findElement(uiObject);
        String actualValue = element.getAttribute(attributeName);
        String message = "Attribute '" + attributeName + "' of element '" + uiObject.getName() + "' does not match.";
        logger.info("Checking attribute '{}' of element '{}'", attributeName, uiObject.getName());
        if (isSoft) {
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
            softAssert.assertEquals(actualValue, expectedValue, "SOFT ASSERT FAILED: " + message);
        } else {
            Assert.assertEquals(actualValue, expectedValue, "HARD ASSERT FAILED: " + message);
        }
    }

    protected void performStateAssertion(ObjectUI uiObject, boolean expectedState, boolean isSoft) {
        boolean actualState = findElement(uiObject).isEnabled();
        String message = String.format("'enabled' state of '%s' expected %b but actual is %b.",
                uiObject.getName(), expectedState, actualState);
        logger.info(String.format("Checking 'enabled' state of '%s' expected %b and actual is %b.",
                uiObject.getName(), expectedState, actualState));
        if (isSoft) {
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
        } else {
            Assert.assertEquals(actualState, expectedState, "HARD ASSERT FAILED: " + message);
        }
    }

    protected void performSelectionAssertion(ObjectUI uiObject, boolean expectedSelection, boolean isSoft) {
        boolean actualSelection = Objects.requireNonNull(findElement(uiObject).getDomProperty("checked")).equalsIgnoreCase("true");
        String message = String.format("Selection state of '%s' expected %b but actual is %b.",
                uiObject.getName(), expectedSelection, actualSelection);
        logger.info("Verifying expected value: '{}' , actual value: '{}' of object {}",expectedSelection,actualSelection,uiObject.getName());
        if (isSoft) {
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }

            softAssert.assertEquals(actualSelection, expectedSelection, "SOFT ASSERT FAILED: " + message);
        } else {
            Assert.assertEquals(actualSelection, expectedSelection, "HARD ASSERT FAILED: " + message);
        }
    }

    protected void performRegexAssertion(ObjectUI uiObject, String pattern, boolean isSoft) {
        String actualText = getText(uiObject);
        boolean matches = actualText.matches(pattern);
        String message = String.format("Text '%s' of '%s' does not match regex pattern '%s'.",
                actualText, uiObject.getName(), pattern);
        logger.info(String.format("Checking text '%s' of '%s' against regex pattern '%s'.",
                actualText, uiObject.getName(), pattern));
        if (isSoft) {
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
            softAssert.assertTrue(matches, "SOFT ASSERT FAILED: " + message);
        } else {
            Assert.assertTrue(matches, "HARD ASSERT FAILED: " + message);
        }
    }

    protected void performAttributeContainsAssertion(ObjectUI uiObject, String attribute, String partialValue, boolean isSoft) {
        String actualValue = findElement(uiObject).getAttribute(attribute);
        boolean contains = actualValue != null && actualValue.contains(partialValue);
        String message = String.format("Attribute '%s' ('%s') of '%s' does not contain '%s'.",
                attribute, actualValue, uiObject.getName(), partialValue);
        logger.info(String.format("Checking attribute '%s' ('%s') of '%s' should contain '%s'.",
                attribute, actualValue, uiObject.getName(), partialValue));
        if (isSoft) {
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
            softAssert.assertTrue(contains, "SOFT ASSERT FAILED: " + message);
        } else {
            Assert.assertTrue(contains, "HARD ASSERT FAILED: " + message);
        }
    }

    protected void performCssValueAssertion(ObjectUI uiObject, String cssName, String expectedValue, boolean isSoft) {
        String actualValue = findElement(uiObject).getCssValue(cssName);
        String message = String.format("Giá trị CSS '%s' của '%s' mong đợi là '%s' nhưng thực tế là '%s'.",
                cssName, uiObject.getName(), expectedValue, actualValue);
        logger.info(String.format("Checking CSS value '%s' of '%s' expected '%s' and actual is '%s'.",
                cssName, uiObject.getName(), expectedValue, actualValue));
        if (isSoft) {
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
            softAssert.assertEquals(actualValue, expectedValue, "SOFT ASSERT FAILED: " + message);
        } else {
            Assert.assertEquals(actualValue, expectedValue, "HARD ASSERT FAILED: " + message);
        }
    }

    public void verifyElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            try {
                WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
                wait.until(d -> findElement(uiObject) != null);
            } catch (Exception e) {
                throw new AssertionError("HARD ASSERT FAILED: Element '" + uiObject.getName() + "' does not exist.");
            }
            return null;
        }, uiObject, timeoutInSeconds);
    }

    public void verifyElementVisibleHard(ObjectUI uiObject, boolean isVisible) {
        performVisibilityAssertion(uiObject, isVisible, false);
    }

    public void verifyTextHard(ObjectUI uiObject, String expectedText) {
        performTextAssertion(uiObject, expectedText, false);
    }

    /**
     * Logic cốt lõi để kiểm tra sự tồn tại của phần tử.
     * Được thiết kế để các lớp con gọi.
     */
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
        } finally {
            // Khôi phục lại implicit wait về giá trị mặc định của framework (là 0)
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        }
    }

    protected String getAttribute(ObjectUI uiObject, String attributeName) {
        return execute(() -> {
            WebElement element = findElement(uiObject);
            return element.getAttribute(attributeName);
        }, uiObject, attributeName);
    }
}