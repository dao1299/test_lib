package com.vtnet.netat.core;

import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.ui.Locator;
import com.vtnet.netat.core.ui.ObjectUI;
import com.vtnet.netat.driver.DriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import java.time.Duration;
import java.util.List;

/**
 * Lớp trừu tượng cơ sở cho tất cả các keyword liên quan đến UI (Web, Mobile).
 * Chứa logic chung để tìm kiếm phần tử và các hành động, kiểm chứng cơ bản.
 */
public abstract class BaseUiKeyword extends BaseKeyword {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration PRIMARY_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration SECONDARY_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Phương thức tìm kiếm WebElement chung cho cả Web và Mobile.
     */
    protected WebElement findElement(ObjectUI uiObject) {
        WebDriver driver = DriverManager.getDriver();
        List<Locator> locators = uiObject.getActiveLocators();

        if (locators.isEmpty()) {
            throw new IllegalArgumentException("Không có locator nào active cho đối tượng: " + uiObject.getName());
        }

        for (int i = 0; i < locators.size(); i++) {
            Locator locator = locators.get(i);
            Duration timeout = (i == 0) ? PRIMARY_TIMEOUT : SECONDARY_TIMEOUT;
            WebDriverWait wait = new WebDriverWait(driver, timeout);

            try {
                By by = locator.convertToBy();
                logger.info("Đang tìm element '{}' bằng locator: {} (Timeout: {}s)", uiObject.getName(), locator, timeout.getSeconds());
                return wait.until(ExpectedConditions.presenceOfElementLocated(by));
            } catch (Exception e) {
                logger.warn("Không tìm thấy element '{}' với locator {}.", uiObject.getName(), locator);
            }
        }

        throw new NoSuchElementException("Không thể tìm thấy phần tử '" + uiObject.getName() + "' bằng các locator đã định nghĩa.");
    }

    // --- LOGIC CÁC HÀNH ĐỘNG CHUNG ---

    public void click(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new WebDriverWait(DriverManager.getDriver(), PRIMARY_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(element)).click();
            return null;
        }, uiObject);
    }

    public void sendKeys(ObjectUI uiObject, String text) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new WebDriverWait(DriverManager.getDriver(), PRIMARY_TIMEOUT)
                    .until(ExpectedConditions.visibilityOf(element));
            element.clear();
            element.sendKeys(text);
            return null;
        }, uiObject, text);
    }

    public String getText(ObjectUI uiObject) {
        return execute(() -> {
            WebElement element = findElement(uiObject);
            return new WebDriverWait(DriverManager.getDriver(), PRIMARY_TIMEOUT)
                    .until(ExpectedConditions.visibilityOf(element)).getText();
        }, uiObject);
    }

    // --- CÁC PHƯƠNG THỨC LOGIC ASSERT (PROTECTED) ĐỂ LỚP CON SỬ DỤNG ---

    protected void performVisibilityAssertion(ObjectUI uiObject, boolean expectedVisibility, boolean isSoft) {
        execute(() -> {
            boolean actualVisibility;
            try {
                WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(3));
                wait.until(ExpectedConditions.visibilityOf(findElement(uiObject)));
                actualVisibility = true;
            } catch (Exception e) {
                actualVisibility = false;
            }

            String message = String.format("Phần tử '%s' có hiển thị mong đợi là %b nhưng thực tế là %b.",
                    uiObject.getName(), expectedVisibility, actualVisibility);

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
            return null;
        }, uiObject, expectedVisibility, isSoft);
    }

    protected void performTextAssertion(ObjectUI uiObject, String expectedText, boolean isSoft) {
        execute(() -> {
            String actualText = getText(uiObject);
            String message = "Văn bản của phần tử '" + uiObject.getName() + "' không khớp.";

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
            return null;
        }, uiObject, expectedText, isSoft);
    }

    protected void performTextContainsAssertion(ObjectUI uiObject, String partialText, boolean isSoft) {
        execute(() -> {
            String actualText = getText(uiObject);
            String message = "Văn bản của phần tử '" + uiObject.getName() + "' ('" + actualText + "') không chứa '" + partialText + "'.";
            boolean isContains = actualText.contains(partialText);

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
            return null;
        }, uiObject, partialText, isSoft);
    }

    protected void performAttributeAssertion(ObjectUI uiObject, String attributeName, String expectedValue, boolean isSoft) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            String actualValue = element.getAttribute(attributeName);
            String message = "Thuộc tính '" + attributeName + "' của phần tử '" + uiObject.getName() + "' không khớp.";

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
            return null;
        }, uiObject, attributeName, expectedValue, isSoft);
    }

    protected void performStateAssertion(ObjectUI uiObject, boolean expectedState, boolean isSoft) {
        execute(() -> {
            boolean actualState = findElement(uiObject).isEnabled();
            String message = String.format("Trạng thái 'enabled' của '%s' mong đợi là %b nhưng thực tế là %b.",
                    uiObject.getName(), expectedState, actualState);
            if (isSoft) {
                SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
                if (softAssert == null) {
                    softAssert = new SoftAssert();
                    ExecutionContext.getInstance().setSoftAssert(softAssert);
                }
                softAssert.assertEquals(actualState, expectedState, "SOFT ASSERT FAILED: " + message);
            } else {
                Assert.assertEquals(actualState, expectedState, "HARD ASSERT FAILED: " + message);
            }
            return null;
        }, uiObject, expectedState, isSoft);
    }

    protected void performRegexAssertion(ObjectUI uiObject, String pattern, boolean isSoft) {
        execute(() -> {
            String actualText = getText(uiObject);
            boolean matches = actualText.matches(pattern);
            String message = String.format("Văn bản '%s' của '%s' không khớp với mẫu regex '%s'.",
                    actualText, uiObject.getName(), pattern);
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
            return null;
        }, uiObject, pattern, isSoft);
    }

    protected void performAttributeContainsAssertion(ObjectUI uiObject, String attribute, String partialValue, boolean isSoft) {
        execute(() -> {
            String actualValue = findElement(uiObject).getAttribute(attribute);
            boolean contains = actualValue != null && actualValue.contains(partialValue);
            String message = String.format("Thuộc tính '%s' ('%s') của '%s' không chứa chuỗi '%s'.",
                    attribute, actualValue, uiObject.getName(), partialValue);
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
            return null;
        }, uiObject, attribute, partialValue, isSoft);
    }

    protected void performCssValueAssertion(ObjectUI uiObject, String cssName, String expectedValue, boolean isSoft) {
        execute(() -> {
            String actualValue = findElement(uiObject).getCssValue(cssName);
            String message = String.format("Giá trị CSS '%s' của '%s' mong đợi là '%s' nhưng thực tế là '%s'.",
                    cssName, uiObject.getName(), expectedValue, actualValue);
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
            return null;
        }, uiObject, cssName, expectedValue, isSoft);
    }
}