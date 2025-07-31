package com.vtnet.netat.web.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.exceptions.NetatException;
import com.vtnet.netat.web.elements.NetatUIObject;
import com.vtnet.netat.web.elements.NetatWebElement;
import com.vtnet.netat.web.elements.UIObjectRepository; // Import UIObjectRepository
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
                    return clickElement((NetatUIObject) params[0]);
                case "Input Text":
                    return inputText((NetatUIObject) params[0], (String) params[1]);
                case "Get Text":
                    return getText((NetatUIObject) params[0]);
                case "Close Browser":
                    return closeBrowser();
                case "Element Should Be Visible":
                    elementShouldBeVisible((NetatUIObject) params[0]);
                    return null; // void method
                // Không cần case cho findUIObject vì nó không phải là keyword được gọi qua performKeywordAction
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
                getNetatLogger().info("Browser opened and navigated to: {}", url);
            } else {
                getNetatLogger().info("Browser opened: {}", browserType);
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
    public static Object closeBrowser() {
        logKeywordExecution("Close Browser");

        try {
            WebDriver driver = getWebDriver();
            if (driver != null) {
                driver.quit();
                getContext().setWebDriver(null);
                getNetatLogger().info("Browser closed successfully");
            } else {
                getNetatLogger().warn("No browser instance to close");
            }
        } catch (Exception e) {
            getNetatLogger().warn("Error closing browser: {}", e.getMessage());
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
            getNetatLogger().info("Navigated to URL: {}", url);

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
            getNetatLogger().info("Page title: {}", title);
            return title;

        } catch (Exception e) {
            throw new NetatException("Failed to get page title", e);
        }
    }

    // ==================== ELEMENT INTERACTION KEYWORDS ====================

    @NetatKeyword(
            name = "Click Element",
            description = "Clicks on the specified UI element.",
            category = "INTERACTION",
            parameters = {"uiObject"},
            example = "Click Element | WebKeywords.findUIObject(\"LoginPage/loginButton\")", // Cập nhật ví dụ
            timeout = 10,
            retryOnFailure = true,
            maxRetries = 3
    )
    public static Object clickElement(NetatUIObject uiObject) {
        logKeywordExecution("Click Element", uiObject.getPath());
        validateParameters(uiObject);

        try {
            NetatWebElement element = findElement(uiObject);
            element.click();
            getNetatLogger().info("Clicked element for UIObject: {}", uiObject.getPath());

        } catch (Exception e) {
            throw new NetatException("Failed to click element for UIObject: " + uiObject.getPath(), e);
        }
        return null;
    }

    @NetatKeyword(
            name = "Input Text",
            description = "Inputs text into the specified UI element (clears first).",
            category = "INTERACTION",
            parameters = {"uiObject", "text"},
            example = "Input Text | WebKeywords.findUIObject(\"LoginPage/usernameField\") | john.doe", // Cập nhật ví dụ
            timeout = 10
    )
    public static Object inputText(NetatUIObject uiObject, String text) {
        logKeywordExecution("Input Text", uiObject.getPath(), text);
        validateParameters(uiObject, text);

        try {
            NetatWebElement element = findElement(uiObject);
            element.sendKeys(text);
            getNetatLogger().info("Input text '{}' to element for UIObject: {}", text, uiObject.getPath());

        } catch (Exception e) {
            throw new NetatException("Failed to input text to element for UIObject: " + uiObject.getPath(), e);
        }
        return null;
    }

    @NetatKeyword(
            name = "Get Text",
            description = "Gets text from the specified UI element.",
            category = "VERIFICATION",
            parameters = {"uiObject"},
            example = "Get Text | WebKeywords.findUIObject(\"DashboardPage/welcomeMessage\")", // Cập nhật ví dụ
            timeout = 10
    )
    public static String getText(NetatUIObject uiObject) {
        logKeywordExecution("Get Text", uiObject.getPath());
        validateParameters(uiObject);

        try {
            NetatWebElement element = findElement(uiObject);
            String text = element.getText();
            getNetatLogger().info("Got text '{}' from element for UIObject: {}", text, uiObject.getPath());
            return text;

        } catch (Exception e) {
            throw new NetatException("Failed to get text from element for UIObject: " + uiObject.getPath(), e);
        }
    }

    @NetatKeyword(
            name = "Element Should Be Visible",
            description = "Verifies that the specified UI element is visible.",
            category = "VERIFICATION",
            parameters = {"uiObject"},
            example = "Element Should Be Visible | WebKeywords.findUIObject(\"DashboardPage/welcomeMessage\")", // Cập nhật ví dụ
            timeout = 10
    )
    public static void elementShouldBeVisible(NetatUIObject uiObject) {
        logKeywordExecution("Element Should Be Visible", uiObject.getPath());
        validateParameters(uiObject);

        try {
            NetatWebElement element = findElement(uiObject);
            if (!element.isDisplayed()) {
                throw new NetatException("Element is not visible for UIObject: " + uiObject.getPath());
            }
            getNetatLogger().info("Element is visible for UIObject: {}", uiObject.getPath());

        } catch (Exception e) {
            throw new NetatException("Element visibility verification failed for UIObject: " + uiObject.getPath(), e);
        }
    }

    // ==================== UI OBJECT RETRIEVAL METHOD ====================

    /**
     * Finds and returns a NetatUIObject from the UIObjectRepository based on its path.
     * This method acts as a convenient entry point for test case writers to retrieve UI objects.
     *
     * @param objectPath The logical path of the UI object (e.g., "ecommerce/customer/LoginPage/loginButton").
     * @return The NetatUIObject found and merged from the repository.
     * @throws NetatException if the UI object is not found or an error occurs during retrieval/merging.
     */
    public static NetatUIObject findUIObject(String objectPath) { // Phương thức mới
        logKeywordExecution("Find UI Object", objectPath); // Log việc tìm kiếm UIObject
        validateParameters(objectPath); // Đảm bảo objectPath không null/rỗng

        try {
            return UIObjectRepository.getInstance().getUIObjectByPath(objectPath); // Gọi UIObjectRepository
        } catch (NetatException e) {
            // Bao bọc lại exception để cung cấp thêm ngữ cảnh
            throw new NetatException("Failed to find UI object for path: " + objectPath, e);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Finds a {@link NetatWebElement} based on the provided {@link NetatUIObject}.
     * This method utilizes the locator strategies and properties defined within the NetatUIObject.
     *
     * @param uiObject The NetatUIObject containing information about the element to find.
     * @return A NetatWebElement instance representing the found element.
     * @throws NetatException if no browser instance is available or the element cannot be found.
     */
    private static NetatWebElement findElement(NetatUIObject uiObject) {
        WebDriver driver = getWebDriver();
        if (driver == null) {
            throw new NetatException("No browser instance available. Please open browser first.");
        }

        if (uiObject == null) {
            throw new NetatException("NetatUIObject cannot be null.");
        }

        int waitTimeout = 30; // Default timeout
        if (uiObject.getProperties() != null && uiObject.getProperties().containsKey("waitTimeout")) {
            Object timeoutProp = uiObject.getProperties().get("waitTimeout");
            if (timeoutProp instanceof Integer) {
                waitTimeout = (Integer) timeoutProp;
            } else if (timeoutProp instanceof String) {
                try {
                    waitTimeout = Integer.parseInt((String) timeoutProp);
                } catch (NumberFormatException e) {
                    getNetatLogger().warn("Invalid 'waitTimeout' format in UIObject properties for {}. Using default (30s).", uiObject.getPath());
                }
            }
        }

        WebElement element = uiObject.convertToWebElementWithTimeout(driver, waitTimeout);
        if (element == null) {
            throw new NetatException("Failed to find WebElement for UIObject: " + uiObject.getPath() + " within " + waitTimeout + " seconds.");
        }

        return new NetatWebElement(element, driver, uiObject.getPrimaryLocator() != null ? uiObject.getPrimaryLocator().convertToSeleniumBy() : null);
    }
}