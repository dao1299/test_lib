package com.vtnet.netat.web.elements;

import com.vtnet.netat.core.exceptions.NetatException;
import com.vtnet.netat.core.logging.NetatLogger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Wrapper class cho WebElement với enhanced functionality
 * Provides additional methods và error handling
 */
public class NetatWebElement {

    private final WebElement element;
    private final WebDriver driver;
    private final By locator;
    private final NetatLogger logger;
    private final int defaultTimeout;

    public NetatWebElement(WebElement element, WebDriver driver, By locator) {
        this.element = element;
        this.driver = driver;
        this.locator = locator;
        this.logger = NetatLogger.getInstance(NetatWebElement.class);
        this.defaultTimeout = 10; // seconds
    }

    public NetatWebElement(WebElement element, WebDriver driver) {
        this(element, driver, null);
    }

    // Basic WebElement operations with enhanced error handling

    /**
     * Click element with wait and retry
     */
    public void click() {
        try {
            waitForClickable();
            scrollIntoView();
            element.click();
            logger.debug("Clicked element: {}", getElementDescription());
        } catch (Exception e) {
            throw new NetatException("Failed to click element: " + getElementDescription(), e);
        }
    }

    /**
     * Click using JavaScript (for stubborn elements)
     */
    public void clickJS() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", element);
            logger.debug("JavaScript clicked element: {}", getElementDescription());
        } catch (Exception e) {
            throw new NetatException("Failed to JavaScript click element: " + getElementDescription(), e);
        }
    }

    /**
     * Send text with clear first
     */
    public void sendKeys(String text) {
        try {
            waitForVisible();
            scrollIntoView();
            element.clear();
            element.sendKeys(text);
            logger.debug("Sent text '{}' to element: {}", text, getElementDescription());
        } catch (Exception e) {
            throw new NetatException("Failed to send text to element: " + getElementDescription(), e);
        }
    }

    /**
     * Send text without clearing first
     */
    public void type(String text) {
        try {
            waitForVisible();
            scrollIntoView();
            element.sendKeys(text);
            logger.debug("Typed text '{}' to element: {}", text, getElementDescription());
        } catch (Exception e) {
            throw new NetatException("Failed to type text to element: " + getElementDescription(), e);
        }
    }

    /**
     * Clear element text
     */
    public void clear() {
        try {
            waitForVisible();
            element.clear();
            logger.debug("Cleared element: {}", getElementDescription());
        } catch (Exception e) {
            throw new NetatException("Failed to clear element: " + getElementDescription(), e);
        }
    }

    /**
     * Get element text
     */
    public String getText() {
        try {
            waitForVisible();
            String text = element.getText();
            logger.debug("Got text '{}' from element: {}", text, getElementDescription());
            return text;
        } catch (Exception e) {
            throw new NetatException("Failed to get text from element: " + getElementDescription(), e);
        }
    }

    /**
     * Get element attribute value
     */
    public String getAttribute(String attributeName) {
        try {
            waitForPresent();
            String value = element.getAttribute(attributeName);
            logger.debug("Got attribute '{}' = '{}' from element: {}", attributeName, value, getElementDescription());
            return value;
        } catch (Exception e) {
            throw new NetatException("Failed to get attribute from element: " + getElementDescription(), e);
        }
    }

    /**
     * Check if element is displayed
     */
    public boolean isDisplayed() {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is enabled
     */
    public boolean isEnabled() {
        try {
            return element.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is selected (for checkboxes, radio buttons)
     */
    public boolean isSelected() {
        try {
            return element.isSelected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Select dropdown option by visible text
     */
    public void selectByText(String text) {
        try {
            waitForVisible();
            Select select = new Select(element);
            select.selectByVisibleText(text);
            logger.debug("Selected option '{}' from dropdown: {}", text, getElementDescription());
        } catch (Exception e) {
            throw new NetatException("Failed to select option by text: " + getElementDescription(), e);
        }
    }

    /**
     * Select dropdown option by value
     */
    public void selectByValue(String value) {
        try {
            waitForVisible();
            Select select = new Select(element);
            select.selectByValue(value);
            logger.debug("Selected option by value '{}' from dropdown: {}", value, getElementDescription());
        } catch (Exception e) {
            throw new NetatException("Failed to select option by value: " + getElementDescription(), e);
        }
    }

    /**
     * Select dropdown option by index
     */
    public void selectByIndex(int index) {
        try {
            waitForVisible();
            Select select = new Select(element);
            select.selectByIndex(index);
            logger.debug("Selected option by index '{}' from dropdown: {}", index, getElementDescription());
        } catch (Exception e) {
            throw new NetatException("Failed to select option by index: " + getElementDescription(), e);
        }
    }

    /**
     * Get all dropdown options
     */
    public List<WebElement> getSelectOptions() {
        try {
            waitForVisible();
            Select select = new Select(element);
            return select.getOptions();
        } catch (Exception e) {
            throw new NetatException("Failed to get dropdown options: " + getElementDescription(), e);
        }
    }

    // Wait methods

    /**
     * Wait for element to be present
     */
    public NetatWebElement waitForPresent() {
        return waitForPresent(defaultTimeout);
    }

    public NetatWebElement waitForPresent(int timeoutSeconds) {
        if (locator != null) {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        }
        return this;
    }

    /**
     * Wait for element to be visible
     */
    public NetatWebElement waitForVisible() {
        return waitForVisible(defaultTimeout);
    }

    public NetatWebElement waitForVisible(int timeoutSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            if (locator != null) {
                wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            } else {
                wait.until(ExpectedConditions.visibilityOf(element));
            }
        } catch (TimeoutException e) {
            throw new NetatException("Element not visible within " + timeoutSeconds + " seconds: " + getElementDescription(), e);
        }
        return this;
    }

    /**
     * Wait for element to be clickable
     */
    public NetatWebElement waitForClickable() {
        return waitForClickable(defaultTimeout);
    }

    public NetatWebElement waitForClickable(int timeoutSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            if (locator != null) {
                wait.until(ExpectedConditions.elementToBeClickable(locator));
            } else {
                wait.until(ExpectedConditions.elementToBeClickable(element));
            }
        } catch (TimeoutException e) {
            throw new NetatException("Element not clickable within " + timeoutSeconds + " seconds: " + getElementDescription(), e);
        }
        return this;
    }

    /**
     * Scroll element into view
     */
    public NetatWebElement scrollIntoView() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            Thread.sleep(500); // Small delay for smooth scrolling
        } catch (Exception e) {
            logger.warn("Failed to scroll element into view: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Highlight element (for debugging)
     */
    public NetatWebElement highlight() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].style.border='3px solid red';", element);
            Thread.sleep(1000);
            js.executeScript("arguments[0].style.border='';", element);
        } catch (Exception e) {
            logger.warn("Failed to highlight element: {}", e.getMessage());
        }
        return this;
    }

    // Utility methods

    /**
     * Get element description for logging
     */
    private String getElementDescription() {
        try {
            if (locator != null) {
                return locator.toString();
            }

            String tagName = element.getTagName();
            String id = element.getAttribute("id");
            String className = element.getAttribute("class");
            String text = element.getText();

            StringBuilder desc = new StringBuilder(tagName);
            if (id != null && !id.isEmpty()) {
                desc.append("[id='").append(id).append("']");
            }
            if (className != null && !className.isEmpty()) {
                desc.append("[class='").append(className).append("']");
            }
            if (text != null && !text.isEmpty() && text.length() < 50) {
                desc.append("[text='").append(text).append("']");
            }

            return desc.toString();
        } catch (Exception e) {
            return "WebElement";
        }
    }

    /**
     * Get underlying WebElement
     */
    public WebElement getWebElement() {
        return element;
    }

    /**
     * Get locator
     */
    public By getLocator() {
        return locator;
    }
}