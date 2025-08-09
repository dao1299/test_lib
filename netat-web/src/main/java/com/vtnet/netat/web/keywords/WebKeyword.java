package com.vtnet.netat.web.keywords;

import com.vtnet.netat.driver.DriverManager; // QUAN TRỌNG: Import DriverManager
import com.vtnet.netat.web.elements.Locator;
import com.vtnet.netat.web.elements.ObjectUI;
import io.qameta.allure.Step;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class WebKeyword {

    private static final Logger log = LoggerFactory.getLogger(WebKeyword.class);
    // Cấu hình thời gian chờ mặc định, bạn cũng có thể đọc từ ConfigReader
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Constructor rỗng. Lớp này sẽ tự lấy WebDriver từ DriverManager.
     */
    public WebKeyword() {
        // Không cần làm gì ở đây
    }

    /**
     * Mở một URL trên trình duyệt.
     */
    public void openUrl(String url) {
        log.info("Mở URL: {}", url);
        // Lấy driver từ DriverManager và thực hiện hành động
        DriverManager.getDriver().get(url);
    }

    /**
     * Tìm kiếm một WebElement.
     */
    private WebElement findElement(ObjectUI uiObject) {
        // Lấy driver của luồng hiện tại
        WebDriver driver = DriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);

        List<Locator> locators = uiObject.getActiveAndSortedLocators();
        if (locators.isEmpty()) {
            throw new IllegalArgumentException("Không có locator nào active cho đối tượng: " + uiObject.getName());
        }

        for (Locator locator : locators) {
            try {
                By by = locator.convertToBy();
                log.debug("Đang tìm element '{}' bằng locator: {}", uiObject.getName(), locator);
                return wait.until(ExpectedConditions.presenceOfElementLocated(by));
            } catch (TimeoutException e) {
                log.warn("Không tìm thấy element '{}' với locator {} trong thời gian chờ.", uiObject.getName(), locator);
            } catch (Exception e) {
                log.error("Lỗi với locator của đối tượng '{}': {}", uiObject.getName(), e.getMessage());
            }
        }
        throw new NoSuchElementException("Không thể tìm thấy phần tử '" + uiObject.getName() + "' bằng bất kỳ locator nào đã cung cấp.");
    }

    /**
     * Click vào một phần tử.
     */
    @Step("Click to element {0} ")
    public void click(ObjectUI uiObject) {
        log.info("Click vào phần tử: {}", uiObject.getName());
        WebElement element = findElement(uiObject);
        // Tạo WebDriverWait tạm thời để chờ click
        new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                .until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    /**
     * Nhập văn bản vào một phần tử.
     */
    @Step("Send value {1} to element {0} ")
    public void sendKeys(ObjectUI uiObject, String text) {
        log.info("Nhập văn bản '{}' vào phần tử: {}", text, uiObject.getName());
        WebElement element = findElement(uiObject);
        new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOf(element));
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Lấy văn bản từ một phần tử.
     */
    @Step("Get text from element {0} ")
    public String getText(ObjectUI uiObject) {
        log.info("Lấy văn bản từ phần tử: {}", uiObject.getName());
        WebElement element = findElement(uiObject);
        return new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                .until(ExpectedConditions.visibilityOf(element)).getText();
    }

    /**
     * Cuộn đến vị trí của một phần tử trên trang.
     */
    public void scrollToElement(ObjectUI uiObject) {
        log.info("Cuộn tới phần tử: {}", uiObject.getName());
        WebElement element = findElement(uiObject);
        // Lấy JavascriptExecutor từ driver hiện tại
        JavascriptExecutor jsExecutor = (JavascriptExecutor) DriverManager.getDriver();
        jsExecutor.executeScript("arguments[0].scrollIntoView(true);", element);
    }

    // Các phương thức waitForElementVisible và getAttribute cũng được cập nhật tương tự...

}