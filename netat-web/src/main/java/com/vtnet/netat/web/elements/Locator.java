package com.vtnet.netat.web.elements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Locator {
    public enum Strategy {
        ID,
        NAME,
        XPATH,
        CSS_SELECTOR,
        CLASS_NAME,
        LINK_TEXT,
        PARTIAL_LINK_TEXT,
        TAG_NAME,
        ACCESSIBILITY_ID,
        ANDROID_UIAUTOMATOR,
        IOS_PREDICATE_STRING,
        IOS_CLASS_CHAIN,
        // Thêm các loại khác nếu cần
        IMAGE,
        JQUERY;
    }

    private Strategy strategy;

    private String value;    // Giá trị của locator, ví dụ: "login-btn", "//button[@id='login']"
    private List<String> type; // Có thể giữ nguyên nếu muốn đa kiểu (ví dụ: ["web", "mobile"])
    private boolean active;  // True nếu locator này đang hoạt động
    private int priority;    // Độ ưu tiên của locator, số nhỏ hơn có ưu tiên cao hơn (ví dụ: 1 là cao nhất)
    private double reliability; // Độ tin cậy của locator (ví dụ: 0.95)

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }



    private static final ObjectMapper MAPPER = new ObjectMapper();


    // Constructors (có thể thêm constructor không tham số nếu cần)
    public Locator() {
    }

    public Locator(Strategy strategy, String value, int priority, boolean active, double reliability) {
        this.strategy = strategy;
        this.value = value;
        this.priority = priority;
        this.active = active;
        this.reliability = reliability;
    }

    // --- Getters and Setters ---

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public double getReliability() {
        return reliability;
    }

    public void setReliability(double reliability) {
        this.reliability = reliability;
    }

    // --- JSON Conversion ---

    /**
     * Chuyển đổi đối tượng Locator hiện tại thành một chuỗi JSON.
     * @return Chuỗi JSON đại diện cho đối tượng.
     * @throws JsonProcessingException nếu có lỗi xảy ra trong quá trình chuyển đổi.
     */
    public String toJson() throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    /**
     * Tạo một đối tượng Locator từ một chuỗi JSON.
     * @param json Chuỗi JSON đầu vào.
     * @return Một đối tượng Locator.
     * @throws JsonProcessingException nếu có lỗi xảy ra trong quá trình phân tích.
     */
    public static Locator fromJson(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, Locator.class);
    }

    // --- Core Logic ---

    /**
     * Chuyển đổi thông tin của Locator thành đối tượng 'By' của Selenium/Appium.
     * @return Một đối tượng 'By' tương ứng với chiến lược và giá trị của locator.
     * @throws UnsupportedOperationException nếu chiến lược locator không được hỗ trợ.
     */
    public By convertToBy() {
        if (this.value == null || this.value.trim().isEmpty()) {
            throw new IllegalStateException("Giá trị (value) của locator không được để trống.");
        }

        switch (strategy) {
            case ID:
                return By.id(value);
            case NAME:
                return By.name(value);
            case XPATH:
                return By.xpath(value);
            case CSS_SELECTOR:
                return By.cssSelector(value);
            case CLASS_NAME:
                return By.className(value);
            case LINK_TEXT:
                return By.linkText(value);
            case PARTIAL_LINK_TEXT:
                return By.partialLinkText(value);
            case TAG_NAME:
                return By.tagName(value);
            case ACCESSIBILITY_ID:
                return AppiumBy.accessibilityId(value);
            case ANDROID_UIAUTOMATOR:
                return AppiumBy.androidUIAutomator(value);
            case IOS_PREDICATE_STRING:
                return AppiumBy.iOSNsPredicateString(value);
            case IOS_CLASS_CHAIN:
                return AppiumBy.iOSClassChain(value);
            default:
                // Ném ra exception cho các chiến lược không được hỗ trợ để báo lỗi ngay lập tức.
                throw new UnsupportedOperationException("Chiến lược locator '" + strategy + "' không được hỗ trợ.");
        }
    }

    @Override
    public String toString() {
        return "Locator{" +
                "strategy=" + strategy +
                ", value='" + value + '\'' +
                '}';
    }

    /**
     * Converts any object to JSON string.
     *
     * @param object Object to convert
     * @return JSON string representation of the object
     * @throws JsonProcessingException if serialization fails
     */
    public static String convertObjectToJson(Object object) throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }


}