package com.vtnet.netat.web.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.exceptions.NetatException;
import com.vtnet.netat.web.elements.NetatUIObject;
import com.vtnet.netat.web.elements.NetatWebElement;
import com.vtnet.netat.web.utils.WebDriverFactory;
import org.openqa.selenium.*;

/**
 * Basic Web Keywords for NETAT platform
 * Contains fundamental web automation operations
 */
public class WebKeywords extends BaseKeyword {

    @Override
    protected Object performKeywordAction(String methodName, Object... params) {
        try {
            switch (methodName) {
                case "Open Browser":
                    return openBrowser((String) params[0], (String) params[1]);
                case "Go To":
                    return goTo((String) params[0]);
                case "Click Element":
                    return clickElement((String) params[0]);
                case "Input Text":
                    return inputText((String) params[0], (String) params[1]);
                case "Get Text":
                    return getText((String) params[0]);
                case "Close Browser":
                    return closeBrowser();
                default:
                    throw new NetatException("Unknown keyword: " + methodName);
            }
        } catch (Exception e) {
            throw new NetatException("Keyword execution failed: " + methodName, e);
        }
    }

    // ==================== BROWSER MANAGEMENT KEYWORDS ====================

    @NetatKeyword(
            name = "Open Browser",
            description = "Opens a web browser with specified browser type and optional URL",
            category = "BROWSER",
            parameters = {"browserType", "url"},
            example = "Open Browser | chrome | https://google.com",
            screenshot = false
    )
    public static Object openBrowser(String browserType, String url) {
        logKeywordExecution("Open Browser", browserType, url);

        try {
            WebDriver driver = WebDriverFactory.createDriver(browserType);
            getContext().setWebDriver(driver);

            if (url != null && !url.trim().isEmpty()) {
                driver.get(url);
                netatLogger.info("Browser opened and navigated to: {}", url);
            } else {
                netatLogger.info("Browser opened: {}", browserType);
            }

        } catch (Exception e) {
            throw new NetatException("Failed to open browser: " + browserType, e);
        }
        return null;
    }

    @NetatKeyword(
            name = "Close Browser",
            description = "Closes the current browser instance",
            category = "BROWSER",
            example = "Close Browser",
            screenshot = false
    )
    public Object closeBrowser() {
        logKeywordExecution("Close Browser");

        try {
            WebDriver driver = getWebDriver();
            if (driver != null) {
                driver.quit();
                getContext().setWebDriver(null);
                netatLogger.info("Browser closed successfully");
            } else {
                netatLogger.warn("No browser instance to close");
            }
        } catch (Exception e) {
            netatLogger.warn("Error closing browser: {}", e.getMessage());
        }
        return null;
    }

    // ==================== NAVIGATION KEYWORDS ====================

    @NetatKeyword(
            name = "Go To",
            description = "Navigates to the specified URL",
            category = "NAVIGATION",
            parameters = {"url"},
            example = "Go To | https://example.com",
            timeout = 30
    )
    public static Object goTo(String url) {
        logKeywordExecution("Go To", url);
        validateParameters(url);

        try {
            WebDriver driver = getWebDriver();
            if (driver == null) {
                throw new NetatException("No browser instance available. Please open browser first.");
            }

            driver.get(url);
            netatLogger.info("Navigated to URL: {}", url);

        } catch (Exception e) {
            throw new NetatException("Failed to navigate to URL: " + url, e);
        }
        return null;
    }

    @NetatKeyword(
            name = "Get Title",
            description = "Gets the current page title",
            category = "NAVIGATION",
            example = "Get Title"
    )
    public static String getTitle() {
        logKeywordExecution("Get Title");

        try {
            WebDriver driver = getWebDriver();
            if (driver == null) {
                throw new NetatException("No browser instance available");
            }

            String title = driver.getTitle();
            netatLogger.info("Page title: {}", title);
            return title;

        } catch (Exception e) {
            throw new NetatException("Failed to get page title", e);
        }
    }

    // ==================== ELEMENT INTERACTION KEYWORDS ====================

    @NetatKeyword(
            name = "Click Element",
            description = "Clicks on the specified element",
            category = "INTERACTION",
            parameters = {"locator"},
            example = "Click Element | id=submit-button",
            timeout = 10,
            retryOnFailure = true,
            maxRetries = 3
    )
    public Object clickElement(String locator) {
        logKeywordExecution("Click Element", locator);
        validateParameters(locator);

        try {
            NetatWebElement element = findElement(locator);
            element.click();
            netatLogger.info("Clicked element: {}", locator);

        } catch (Exception e) {
            throw new NetatException("Failed to click element: " + locator, e);
        }
        return null;
    }

    @NetatKeyword(
            name = "Input Text",
            description = "Inputs text into the specified element (clears first)",
            category = "INTERACTION",
            parameters = {"locator", "text"},
            example = "Input Text | id=username | john.doe",
            timeout = 10
    )
    public static Object inputText(String locator, String text) {
        logKeywordExecution("Input Text", locator, text);
        validateParameters(locator, text);

        try {
            NetatWebElement element = findElement(locator);
            element.sendKeys(text);
            netatLogger.info("Input text '{}' to element: {}", text, locator);

        } catch (Exception e) {
            throw new NetatException("Failed to input text to element: " + locator, e);
        }
        return null;
    }

    @NetatKeyword(
            name = "Get Text",
            description = "Gets text from the specified element",
            category = "VERIFICATION",
            parameters = {"locator"},
            example = "Get Text | class=error-message",
            timeout = 10
    )
    public String getText(String locator) {
        logKeywordExecution("Get Text", locator);
        validateParameters(locator);

        try {
            NetatWebElement element = findElement(locator);
            String text = element.getText();
            netatLogger.info("Got text '{}' from element: {}", text, locator);
            return text;

        } catch (Exception e) {
            throw new NetatException("Failed to get text from element: " + locator, e);
        }
    }

    @NetatKeyword(
            name = "Element Should Be Visible",
            description = "Verifies that the specified element is visible",
            category = "VERIFICATION",
            parameters = {"locator"},
            example = "Element Should Be Visible | id=welcome-message",
            timeout = 10
    )
    public static void elementShouldBeVisible(String locator) {
        logKeywordExecution("Element Should Be Visible", locator);
        validateParameters(locator);

        try {
            NetatWebElement element = findElement(locator);
            if (!element.isDisplayed()) {
                throw new NetatException("Element is not visible: " + locator);
            }
            netatLogger.info("Element is visible: {}", locator);

        } catch (Exception e) {
            throw new NetatException("Element visibility verification failed: " + locator, e);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Find element by locator string
     */
    private static NetatWebElement findElement(String locatorString) {
        WebDriver driver = getWebDriver();
        if (driver == null) {
            throw new NetatException("No browser instance available");
        }

        By locator = parseLocator(locatorString);
        WebElement element = driver.findElement(locator);
        return new NetatWebElement(element, driver, locator);
    }

    /**
     * Parse locator string to By object
     * Supports: id=, name=, class=, tag=, xpath=, css=, linktext=, partiallinktext=
     */
    private static By parseLocator(String locatorString) {
        if (locatorString == null || locatorString.trim().isEmpty()) {
            throw new NetatException("Locator string cannot be null or empty");
        }

        String locator = locatorString.trim();

        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        } else if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        } else if (locator.startsWith("class=")) {
            return By.className(locator.substring(6));
        } else if (locator.startsWith("tag=")) {
            return By.tagName(locator.substring(4));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        } else if (locator.startsWith("linktext=")) {
            return By.linkText(locator.substring(9));
        } else if (locator.startsWith("partiallinktext=")) {
            return By.partialLinkText(locator.substring(16));
        } else {
            // Default to xpath if no prefix specified
            return By.xpath(locator);
        }
    }

    /**
     * Get WebDriver from context
     */
    public static WebDriver getWebDriver() {
        return getContext().getWebDriver();
    }

    /**
     * Validate required parameters
     */
    public static void validateParameters(Object... params) {
        for (Object param : params) {
            if (param == null || (param instanceof String && ((String) param).trim().isEmpty())) {
                throw new NetatException("Required parameter cannot be null or empty");
            }
        }
    }

    public static void clickElementUIObject(NetatUIObject netatUIObject){
//        WebElement element =
    }
}