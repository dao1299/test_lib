package com.vtnet.netat.web.elements;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class NetatUIObject {
    private String uuid;
    private List<Locator> locatorList;
    private String type;

    private boolean inShadowRoot;
    private NetatUIObject childObj;

    public NetatUIObject(List<Locator> locatorList) {
        this.locatorList = locatorList;
    }

    public List<Locator> getLocatorList() {
        return locatorList;
    }

    public void setLocatorList(List<Locator> locatorList) {
        this.locatorList = locatorList;
    }

    public String getUuid() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NetatUIObject getChildObj() {
        return childObj;
    }

    public void setChildObj(NetatUIObject childObj) {
        this.childObj = childObj;
    }

    public boolean isInShadowRoot() {
        return inShadowRoot;
    }

    public void setInShadowRoot(boolean inShadowRoot) {
        this.inShadowRoot = inShadowRoot;
    }

    /**
     * Find the first {@link WebElement} based on {@link NetatUIObject}
     *
     * @param driver Driver that is used to find element
     *
     * @throws RuntimeException if cannot find element
     */
    public WebElement convertToWebElement(WebDriver driver) {
        return convertToWebElement(driver, false);
    }

    public WebElement convertToWebElement(WebDriver driver, boolean includeImageElement) {
        WebElement rs = convertToWebElementQuietly(driver, includeImageElement);
        if (rs == null) throw new NoSuchElementException("Cannot find any element with given NetatUIObject");
        return rs;
    }

    /**
     * Find the first {@link WebElement} based on {@link NetatUIObject}
     * <p>
     * Will not throw exception if element does not exist
     *
     * @param driver Driver that is used to find element
     */
    public WebElement convertToWebElementQuietly(WebDriver driver) {
        return convertToWebElementQuietly(driver, false);
    }

    public WebElement convertToWebElementQuietly(WebDriver driver, boolean includeImageElement) {
        WebElement rs = null;

        if (childObj != null) {
            List<WebElement> resultList = convertToWebElementList(driver, includeImageElement);
            if (!resultList.isEmpty()) rs = resultList.get(0);
        } else {
            for (Locator locator : locatorList) {
                if (!locator.isActive() || locator.getValue() == null || locator.getValue().isEmpty()) continue;
                if (Locator.LOCATOR_JQUERY.equalsIgnoreCase(locator.getStrategy())) {
//                    try {
//                        rs = WebDriverUtilities.findElementByJavascript(locator.getValue(), driver).get(0);
//                    } catch (Exception e) {
//                        System.out.println("No element found with strategy " + locator.getStrategy() + " and value "
//                                + locator.getValue());
//                    }
                } else if (Locator.LOCATOR_IMAGE.equalsIgnoreCase(locator.getStrategy())) {
//                    List<WebElement> foundElements = null;
//                    try {
//                        foundElements = FeatureMatching.findElementByImage(locator, driver, includeImageElement);
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                    if (foundElements == null || foundElements.isEmpty()) {
//                        try {
//                            foundElements = ImageTemplate.findElementsByImage(locator, driver, 1, includeImageElement);
//                        } catch (Exception ex) {
//                            ex.printStackTrace();
//                        }
//                    }
//                    if (foundElements.size() != 1) continue;
//                    rs = foundElements.get(0);
                } else {
                    By strategy = locator.convertToSeleniumBy();
                    if (strategy == null) continue;

                    try {
                        rs = driver.findElement(strategy);
                    } catch (Exception ex) {
                        System.out.println("No element found with strategy " + locator.getStrategy() + " and value "
                                + locator.getValue());
                    }
                }

                if (rs != null) break;
            }
        }

        return rs;
    }

    /**
     * Find a list of {@link WebElement} based on {@link NetatUIObject}.
     *
     * @param driver Driver that is used to find element
     *
     * @return A list of {@link WebElement}. Empty list if cannot find any element
     */
    public List<WebElement> convertToWebElementList(WebDriver driver) {
        return convertToWebElementList(driver, false);
    }

    public List<WebElement> convertToWebElementList(WebDriver driver, boolean includeImageElement) {
        List<WebElement> rs = new ArrayList<>();
        List<WebElement> foundElements = new ArrayList<>();
        for(Locator locator: locatorList) {
            if (!locator.isActive() || locator.getValue() == null || locator.getValue().isEmpty()) continue;
            if (Locator.LOCATOR_JQUERY.equalsIgnoreCase(locator.getStrategy())) {
//                foundElements = WebDriverUtilities.findElementByJavascript(locator.getValue(), driver);
            } else if (Locator.LOCATOR_IMAGE.equals(locator.getStrategy())) {
//                try {
//                    foundElements = FeatureMatching.findElementByImage(locator, driver, includeImageElement);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//                if (foundElements == null || foundElements.isEmpty()) {
//                    try {
//                        foundElements = ImageTemplate.findElementsByImage(locator, driver, includeImageElement);
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
            } else {
                By strategy = locator.convertToSeleniumBy();
                if (strategy == null) continue;
                foundElements = driver.findElements(strategy);
            }
            if (childObj != null) {
                for (WebElement foundElement: foundElements) {
                    rs.addAll(childObj.convertToWebElementListWithinParentElement(foundElement, driver, includeImageElement));
                }
            } else {
                rs.addAll(foundElements);
            }
            if (!rs.isEmpty()) break;
        }

        return rs;
    }

    /**
     * Find a list of {@link WebElement} within a parent {@link WebElement} based on {@link NetatUIObject}
     *
     * @param parentElement Parent element that is used to find element
     * @param driver If element is inside shadow-root, pass the {@link WebDriver} to switch to the
     * shadow-root, otherwise pass {@code null}
     *
     * @return A list of {@link WebElement}. Empty list if cannot find any element
     */
    private List<WebElement> convertToWebElementListWithinParentElement(WebElement parentElement, WebDriver driver, boolean includeImageElement) {
        List<WebElement> rs = new ArrayList<>();

        SearchContext trueParent = parentElement;
        if (inShadowRoot) {
            try {
                trueParent = parentElement.getShadowRoot();
            } catch (NoSuchShadowRootException ex) {
                System.out.println("Cannot switch to shadow-root");
                return rs;
            }
        }

        for(Locator locator: locatorList) {
            if (!locator.isActive() || locator.getValue() == null || locator.getValue().isEmpty()) continue;

            List<WebElement> foundElements = new ArrayList<>();
            if (Locator.LOCATOR_JQUERY.equalsIgnoreCase(locator.getStrategy())) {
                System.err.println("[WARN] Javascript query selector is not supported in find element from parent element");
                continue;
            } else if (Locator.LOCATOR_IMAGE.equals(locator.getStrategy())) {
//                try {
//                    foundElements = FeatureMatching.findElementFromParentElementByImage(parentElement, driver, locator, includeImageElement);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//                if (foundElements == null || foundElements.isEmpty()) {
//                    try {
//                        foundElements = ImageTemplate.findElementsFromParentElementByImage(parentElement, driver, locator, includeImageElement);
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
            } else {
                By strategy = locator.convertToSeleniumBy();
                if (strategy == null) continue;
                try {
                    foundElements = trueParent.findElements(strategy);
                } catch (Exception ex) {}
            }

            if (childObj != null) {
                for (WebElement foundElement: foundElements) {
                    rs.addAll(childObj.convertToWebElementListWithinParentElement(foundElement, driver, includeImageElement));
                }
            } else {
                rs.addAll(foundElements);
            }
            if (!rs.isEmpty()) break;
        }

        return rs;
    }

    /**
     * Find the first {@link WebElement} based on {@link NetatUIObject}
     *
     * @param driver Driver that is used to find element
     * @param timeout The timeout in seconds to find element
     *
     * @throws RuntimeException if cannot find element
     */
    public WebElement convertToWebElementWithTimeout(WebDriver driver, Integer timeout) {
        return convertToWebElementWithTimeout(driver, Duration.ofSeconds(timeout));
    }

    public WebElement convertToWebElementWithTimeout(WebDriver driver, Duration timeout) {
        return convertToWebElementWithTimeout(driver, timeout, false);
    }

    public WebElement convertToWebElementWithTimeout(WebDriver driver, Duration timeout, boolean includeImageElement) {
        WebElement rs = convertToWebElementWithTimeoutQuietly(driver, timeout, includeImageElement);
        if (rs == null) throw new NoSuchElementException("Cannot find any element with given NetatUIObject after " + timeout);
        return rs;
    }

    /**
     * Find the first {@link WebElement} based on {@link NetatUIObject}
     * <p>
     * Will not throw exception if element does not exist
     *
     * @param driver Driver that is used to find element
     * @param timeout The timeout in seconds to find element
     */
    public WebElement convertToWebElementWithTimeoutQuietly(WebDriver driver, Integer timeout) {
        return convertToWebElementWithTimeoutQuietly(driver, Duration.ofSeconds(timeout));
    }

    public WebElement convertToWebElementWithTimeoutQuietly(WebDriver driver, Duration timeout) {
        return convertToWebElementWithTimeoutQuietly(driver, timeout, false);
    }

    public WebElement convertToWebElementWithTimeoutQuietly(WebDriver driver, Duration timeout, boolean includeImageElement) {
        try {
            WebElement element = new WebDriverWait(driver, timeout, Duration.ofMillis(200)).until(new ExpectedCondition<WebElement>(){
                @Override
                public WebElement apply(WebDriver input) {
                    return convertToWebElementQuietly(driver, includeImageElement);
                }
            });
            return element;
        } catch (Exception ex) {}

        return null;
    }

    /**
     * Find a list of {@link WebElement} based on {@link NetatUIObject}.
     *
     * @param driver Driver that is used to find element
     * @param timeout The timeout in seconds to find element
     *
     * @return A list of {@link WebElement}. Empty list if cannot find any element
     */
    public List<WebElement> convertToWebElementListWithTimeout(WebDriver driver, Duration timeout) {
        return convertToWebElementListWithTimeout(driver, timeout, false);
    }

    public List<WebElement> convertToWebElementListWithTimeout(WebDriver driver, Duration timeout, boolean includeImageElement) {
        try {
            List<WebElement> element = new WebDriverWait(driver, timeout, Duration.ofMillis(200)).until(new ExpectedCondition<List<WebElement>>(){
                @Override
                public List<WebElement> apply(WebDriver input) {
                    List<WebElement> elementsList = convertToWebElementList(driver, includeImageElement);
                    if (elementsList.isEmpty()) return null;
                    return elementsList;
                }
            });
            return element;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

//    @Override
//    public String toString() {
//        return Constants.gs.toJson(this);
//    }
    

    
    
    /**
     * Convert this object to JSON string
     * @return JSON string representation of the object
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting object to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Convert this object to pretty formatted JSON string
     * @return Formatted JSON string representation of the object
     */
    public String toPrettyJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting object to pretty JSON: " + e.getMessage(), e);
        }
    }
}