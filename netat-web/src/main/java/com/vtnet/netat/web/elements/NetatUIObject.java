package com.vtnet.netat.web.elements;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude; // Để bỏ qua các trường null/empty khi JSON hóa

 // Không hiển thị các trường null khi JSON hóa
public class NetatUIObject {
    private String udid; // ID duy nhất trong cơ sở dữ liệu
    private String name; // Tên hiển thị của đối tượng
    private String path; // Đường dẫn trong Object Repository (ví dụ: ecommerce/customer/LoginPage/loginButton)
    private String description; // Mô tả chi tiết về đối tượng
    private String type; // Loại phần tử (Button, Input, Dropdown, v.v.)
    private String version; // Phiên bản của đối tượng
    private LocalDateTime lastModified; // Thời gian cập nhật cuối cùng
    private String author; // Người tạo hoặc chỉnh sửa đối tượng

    // Dành cho tính kế thừa
    private String parentPath; // Đường dẫn đến UIObject cha mà đối tượng này kế thừa từ

    // Locator Information
    private List<Locator> locators; // Danh sách các locator với thứ tự ưu tiên và độ tin cậy
    // private Locator primaryLocator; // Locator hiện đang được sử dụng (có thể được xác định động)

    // Properties & Behavior
    private Map<String, Object> properties; // Các thuộc tính như thời gian chờ (waitTimeout), có highlight hay không (highlight), v.v.

    // Metadata
    private Map<String, Object> customAttributes; // Thuộc tính tùy chỉnh mở rộng (dữ liệu kiểm thử, thông tin hiệu suất, v.v.)

    // Relationships
    // Có thể đơn giản hóa hoặc mở rộng tùy theo độ phức tạp của relationships bạn muốn quản lý
    private Map<String, List<String>> relationships; // parent, children, siblings, dependencies (lưu paths)
    private List<NetatUIObject> children; // Đối tượng con (để hỗ trợ Page Object/Component Model, có thể lazy load)
    private boolean inShadowRoot; // True nếu element nằm trong Shadow DOM

    // Validation Rules
    // Bạn cần định nghĩa class ValidationRule và sử dụng nó ở đây
    // private List<ValidationRule> validationRules; // Quy tắc xác thực

    public NetatUIObject() {
        this.locators = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public NetatUIObject(String path) {
        this.locators = new ArrayList<>();
    }

    // Constructor đơn giản cho việc khởi tạo ban đầu
    public NetatUIObject(String path, List<Locator> locators) {
        this();
        this.path = path;
        this.locators = locators;
    }

    // --- Getters and Setters for all fields ---

    public String getUdid() {
        return udid;
    }

    public void setUdid(String udid) {
        this.udid = udid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public List<Locator> getLocators() {
        return locators;
    }

    public void setLocators(List<Locator> locators) {
        this.locators = locators;
    }

    // Lấy locator chính theo độ ưu tiên cao nhất, có thể tùy chỉnh logic này
    public Locator getPrimaryLocator() {
        if (locators == null || locators.isEmpty()) {
            return null;
        }
        // Sắp xếp theo ưu tiên và độ tin cậy, rồi chọn cái đầu tiên
        locators.sort((l1, l2) -> {
            int priorityCompare = Integer.compare(l1.getPriority(), l2.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Double.compare(l2.getReliability(), l1.getReliability()); // Ưu tiên độ tin cậy cao hơn
        });
        return locators.stream().filter(Locator::isActive).findFirst().orElse(null);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public Map<String, List<String>> getRelationships() {
        return relationships;
    }

    public void setRelationships(Map<String, List<String>> relationships) {
        this.relationships = relationships;
    }

    public List<NetatUIObject> getChildren() {
        return children;
    }

    public void setChildren(List<NetatUIObject> children) {
        this.children = children;
    }

    public boolean isInShadowRoot() {
        return inShadowRoot;
    }

    public void setInShadowRoot(boolean inShadowRoot) {
        this.inShadowRoot = inShadowRoot;
    }

    // --- Existing methods from previous NetatUIObject.java (modified to use 'locators') ---

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
        if (rs == null) throw new NoSuchElementException("Cannot find any element with given NetatUIObject: " + this.path);
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

        // Nếu có children, tìm kiếm trong các children
        if (children != null && !children.isEmpty()) {
            List<WebElement> resultList = convertToWebElementList(driver, includeImageElement);
            if (!resultList.isEmpty()) rs = resultList.get(0);
        } else {
            // Sắp xếp locators theo priority và reliability trước khi tìm kiếm
            locators.sort((l1, l2) -> {
                int priorityCompare = Integer.compare(l1.getPriority(), l2.getPriority());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                return Double.compare(l2.getReliability(), l1.getReliability());
            });

            for (Locator locator : locators) { // Sử dụng 'locators' thay vì 'locatorList'
                if (!locator.isActive() || locator.getValue() == null || locator.getValue().isEmpty()) continue;
                // Bỏ qua các chiến lược chưa hỗ trợ hoặc yêu cầu thư viện khác
                if (Locator.LOCATOR_JQUERY.equalsIgnoreCase(locator.getStrategy()) ||
                        Locator.LOCATOR_IMAGE.equalsIgnoreCase(locator.getStrategy())) {
                    System.out.println("[WARN] Skipping unsupported locator strategy: " + locator.getStrategy());
                    continue;
                }

                By strategy = locator.convertToSeleniumBy();
                if (strategy == null) continue;

                try {
                    rs = driver.findElement(strategy);
                } catch (Exception ex) {
                    System.out.println("No element found with strategy " + locator.getStrategy() + " and value "
                            + locator.getValue() + " for UIObject: " + this.path + ". Error: " + ex.getMessage());
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

        // Sắp xếp locators theo priority và reliability trước khi tìm kiếm
        locators.sort((l1, l2) -> {
            int priorityCompare = Integer.compare(l1.getPriority(), l2.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Double.compare(l2.getReliability(), l1.getReliability());
        });

        for(Locator locator: locators) { // Sử dụng 'locators'
            if (!locator.isActive() || locator.getValue() == null || locator.getValue().isEmpty()) continue;
            // Bỏ qua các chiến lược chưa hỗ trợ hoặc yêu cầu thư viện khác
            if (Locator.LOCATOR_JQUERY.equalsIgnoreCase(locator.getStrategy()) ||
                    Locator.LOCATOR_IMAGE.equalsIgnoreCase(locator.getStrategy())) {
                System.out.println("[WARN] Skipping unsupported locator strategy: " + locator.getStrategy());
                continue;
            }

            By strategy = locator.convertToSeleniumBy();
            if (strategy == null) continue;
            foundElements = driver.findElements(strategy);

            if (children != null && !children.isEmpty()) { // Sử dụng 'children' thay vì 'childObj'
                for (WebElement foundElement: foundElements) {
                    // Recursive call for children within the found parent element
                    for (NetatUIObject childUiObject : children) {
                        rs.addAll(childUiObject.convertToWebElementListWithinParentElement(foundElement, driver, includeImageElement));
                    }
                }
            } else {
                rs.addAll(foundElements);
            }
            if (!rs.isEmpty()) break; // Break after finding elements with the first successful locator
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
        if (inShadowRoot) { // Sử dụng thuộc tính inShadowRoot của chính UIObject này
            try {
                trueParent = parentElement.getShadowRoot();
            } catch (NoSuchShadowRootException ex) {
                System.out.println("Cannot switch to shadow-root for UIObject: " + this.path + ". Error: " + ex.getMessage());
                return rs;
            }
        }

        // Sắp xếp locators theo priority và reliability trước khi tìm kiếm
        locators.sort((l1, l2) -> {
            int priorityCompare = Integer.compare(l1.getPriority(), l2.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Double.compare(l2.getReliability(), l1.getReliability());
        });

        for(Locator locator: locators) { // Sử dụng 'locators'
            if (!locator.isActive() || locator.getValue() == null || locator.getValue().isEmpty()) continue;

            List<WebElement> foundElements = new ArrayList<>();
            if (Locator.LOCATOR_JQUERY.equalsIgnoreCase(locator.getStrategy())) {
                System.err.println("[WARN] Javascript query selector is not supported in find element from parent element for UIObject: " + this.path);
                continue;
            } else if (Locator.LOCATOR_IMAGE.equals(locator.getStrategy())) {
                System.err.println("[WARN] Image locator is not supported in find element from parent element for UIObject: " + this.path);
                continue;
            } else {
                By strategy = locator.convertToSeleniumBy();
                if (strategy == null) continue;
                try {
                    foundElements = trueParent.findElements(strategy);
                } catch (Exception ex) {
                    System.out.println("No element found within parent with strategy " + locator.getStrategy() + " and value "
                            + locator.getValue() + " for UIObject: " + this.path + ". Error: " + ex.getMessage());
                }
            }

            if (children != null && !children.isEmpty()) { // Sử dụng 'children'
                for (WebElement foundElement: foundElements) {
                    // Recursive call for children within the found parent element
                    for (NetatUIObject childUiObject : children) {
                        rs.addAll(childUiObject.convertToWebElementListWithinParentElement(foundElement, driver, includeImageElement));
                    }
                }
            } else {
                rs.addAll(foundElements);
            }
            if (!rs.isEmpty()) break; // Break after finding elements with the first successful locator
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
        return convertToWebElementWithTimeout(driver, Duration.ofSeconds(timeout != null ? timeout : 0));
    }

    public WebElement convertToWebElementWithTimeout(WebDriver driver, Duration timeout) {
        return convertToWebElementWithTimeout(driver, timeout, false);
    }

    public WebElement convertToWebElementWithTimeout(WebDriver driver, Duration timeout, boolean includeImageElement) {
        WebElement rs = convertToWebElementWithTimeoutQuietly(driver, timeout, includeImageElement);
        if (rs == null) throw new NoSuchElementException("Cannot find any element with given NetatUIObject: " + this.path + " after " + timeout);
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
        return convertToWebElementWithTimeoutQuietly(driver, Duration.ofSeconds(timeout != null ? timeout : 0));
    }

    public WebElement convertToWebElementWithTimeoutQuietly(WebDriver driver, Duration timeout) {
        return convertToWebElementWithTimeoutQuietly(driver, timeout, false);
    }

    public WebElement convertToWebElementWithTimeoutQuietly(WebDriver driver, Duration timeout, boolean includeImageElement) {
        try {
            // Sử dụng timeout của UIObject nếu được định nghĩa trong properties, nếu không dùng timeout mặc định
            int waitTimeoutSeconds = timeout.getSeconds() > 0 ? (int) timeout.getSeconds() : getDefaultWaitTimeout();

            WebElement element = new WebDriverWait(driver, Duration.ofSeconds(waitTimeoutSeconds), Duration.ofMillis(200)).until(new ExpectedCondition<WebElement>(){
                @Override
                public WebElement apply(WebDriver input) {
                    return convertToWebElementQuietly(driver, includeImageElement);
                }
            });
            return element;
        } catch (Exception ex) {
            System.out.println("Element not found quietly with timeout for UIObject: " + this.path + ". Error: " + ex.getMessage());
        }
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
            // Sử dụng timeout của UIObject nếu được định nghĩa trong properties, nếu không dùng timeout mặc định
            int waitTimeoutSeconds = timeout.getSeconds() > 0 ? (int) timeout.getSeconds() : getDefaultWaitTimeout();

            List<WebElement> element = new WebDriverWait(driver, Duration.ofSeconds(waitTimeoutSeconds), Duration.ofMillis(200)).until(new ExpectedCondition<List<WebElement>>(){
                @Override
                public List<WebElement> apply(WebDriver input) {
                    List<WebElement> elementsList = convertToWebElementList(driver, includeImageElement);
                    if (elementsList.isEmpty()) return null; // WebDriverWait cần null để tiếp tục chờ
                    return elementsList;
                }
            });
            return element;
        } catch (Exception ex) {
            System.out.println("Element list not found with timeout for UIObject: " + this.path + ". Error: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper method to get waitTimeout from properties map
    private int getDefaultWaitTimeout() {
        if (properties != null && properties.containsKey("waitTimeout")) {
            Object timeoutObj = properties.get("waitTimeout");
            if (timeoutObj instanceof Integer) {
                return (Integer) timeoutObj;
            }
        }
        return 30; // Default timeout if not specified in properties
    }


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