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

    // =================================================================================
    // --- CÁC KEYWORD HÀNH ĐỘNG CHUNG (PUBLIC) ---
    // =================================================================================

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
                    logger.warn("Không thể lấy text bằng JavaScript. Trả về chuỗi rỗng.");
                    text = ""; // Trả về rỗng nếu có lỗi xảy ra
                }
            }

            // Trả về kết quả, đảm bảo không bao giờ là null và đã được cắt khoảng trắng thừa
            return text != null ? text.trim() : "";
        }, uiObject);
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

        String message = String.format("Phần tử '%s' có hiển thị mong đợi là %b nhưng thực tế là %b.",
                uiObject.getName(), expectedVisibility, actualVisibility);
        logger.info(String.format("Kiểm tra phần tử '%s' có hiển thị mong đợi là %b và thực tế là %b.",
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
        String actualText = getText(uiObject);
        String message = "Văn bản của phần tử '" + uiObject.getName() + "' không khớp.";
        logger.info("Kiểm tra giá trị mong đợi: '{}', Giá trị hiện tại: '{}' của object {}",expectedText,actualText,uiObject.getName());
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
        String message = "Văn bản của phần tử '" + uiObject.getName() + "' ('" + actualText + "') không chứa '" + partialText + "'.";
        logger.info("Kiểm tra văn bản của phần tử '" + uiObject.getName() + "' ('" + actualText + "')  cần chứa '" + partialText + "'.");
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
        String message = "Thuộc tính '" + attributeName + "' của phần tử '" + uiObject.getName() + "' không khớp.";
        logger.info("Kiểm tra thuộc tính '" + attributeName + "' của phần tử '" + uiObject.getName() + "'");
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
        String message = String.format("Trạng thái 'enabled' của '%s' mong đợi là %b nhưng thực tế là %b.",
                uiObject.getName(), expectedState, actualState);
        logger.info(String.format("Kiểm tra trạng thái 'enabled' của '%s' mong đợi là %b và thực tế là %b.",
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
        boolean actualSelection = findElement(uiObject).isSelected();
        String message = String.format("Trạng thái lựa chọn của '%s' mong đợi là %b nhưng thực tế là %b.",
                uiObject.getName(), expectedSelection, actualSelection);
        logger.info("Kiểm tra giá trị mong đợi: '{}', Giá trị hiện tại: '{}' của của object {}",expectedSelection,actualSelection,uiObject.getName());
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
        String message = String.format("Văn bản '%s' của '%s' không khớp với mẫu regex '%s'.",
                actualText, uiObject.getName(), pattern);
        logger.info(String.format("Kiểm tra văn bản '%s' của '%s' và mẫu regex '%s'.",
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
        String message = String.format("Thuộc tính '%s' ('%s') của '%s' không chứa chuỗi '%s'.",
                attribute, actualValue, uiObject.getName(), partialValue);
        logger.info(String.format("Kiểm tra thuộc tính '%s' ('%s') của '%s' và chuỗi cần chứa '%s'.",
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
        logger.info(String.format("Kiểm tra giá trị CSS '%s' của '%s' mong đợi là '%s' và thực tế là '%s'.",
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
}