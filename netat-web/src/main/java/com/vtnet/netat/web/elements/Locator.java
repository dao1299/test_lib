package com.vtnet.netat.web.elements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Locator {
    public static final String LOCATOR_ID = "ID";   //  prew3c locator (using css instead)
    public static final String LOCATOR_ID_BASE = "id";
    public static final String LOCATOR_NAME = "Name";   //  prew3c locator (using css instead)
    public static final String LOCATOR_NAME_BASE = "name";
    public static final String LOCATOR_LINKTEXT = "Linktext";
    public static final String LOCATOR_PARTIAL_LINKTEXT = "Partial Linktext";
    public static final String LOCATOR_TAG_NAME = "Tag Name";
    public static final String LOCATOR_CLASS = "Class";
    public static final String LOCATOR_CSS = "CSS";
    public static final String LOCATOR_XPATH = "XPath";
    public static final String LOCATOR_IMAGE = "Image";
    public static final String LOCATOR_CLASS_NAME = "Class Name";
    public static final String LOCATOR_ACCESSIBILITY_ID = "Accessibility ID";
    public static final String LOCATOR_ANDROID_UIAUTOMATOR = "android uiautomator";
    public static final String LOCATOR_IOS_PREDICATE_STRING = "ios predicate string";
    public static final String LOCATOR_IOS_CLASS_CHAIN = "ios class chain";
    public static final String LOCATOR_JQUERY = "Javascript Query Selector";
    public static final String LOCATOR_AUTOMATION_ID = "automation id";
    public static final String LOCATOR_OCR = "ocr";

    private String strategy; // Tên chiến lược locator, ví dụ: "ID", "XPATH", "CSS"
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

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
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

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Locator fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Locator.class);
    }

    public By convertToSeleniumBy() {
        // Cần đảm bảo rằng các hằng số chiến lược được ánh xạ đúng với các phương thức By của Selenium
        // và AppiumBy nếu bạn dùng chung class Locator cho cả web và mobile.
        // Hiện tại các hằng số LOCATOR_ID và LOCATOR_ID_BASE có thể gây nhầm lẫn.
        // Nên chuẩn hóa một bộ hằng số duy nhất.
        switch (strategy) {
            case LOCATOR_ID:
            case LOCATOR_ID_BASE: // Nếu bạn muốn ID và ID_BASE đều map tới By.id
                return By.id(value);
            case LOCATOR_NAME:
            case LOCATOR_NAME_BASE: // Tương tự cho Name
                return By.name(value);
            case LOCATOR_LINKTEXT:
                return By.linkText(value);
            case LOCATOR_PARTIAL_LINKTEXT:
                return By.partialLinkText(value);
            case LOCATOR_TAG_NAME:
                return By.tagName(value);
            case LOCATOR_CLASS:
            case LOCATOR_CLASS_NAME: // Class và Class_Name đều map tới By.className
                return By.className(value);
            case LOCATOR_CSS:
                return By.cssSelector(value);
            case LOCATOR_XPATH:
                return By.xpath(value);
            case LOCATOR_ACCESSIBILITY_ID:
                return AppiumBy.accessibilityId(value);
            case LOCATOR_ANDROID_UIAUTOMATOR:
                return AppiumBy.androidUIAutomator(value);
            case LOCATOR_IOS_PREDICATE_STRING:
                return AppiumBy.iOSNsPredicateString(value);
            case LOCATOR_IOS_CLASS_CHAIN:
                return AppiumBy.iOSClassChain(value);
            case LOCATOR_IMAGE:
            case LOCATOR_JQUERY:
            case LOCATOR_AUTOMATION_ID: // Có thể cần thư viện hoặc triển khai riêng cho các loại này
            case LOCATOR_OCR:
                System.err.println("Warning: Locator strategy '" + strategy + "' is not directly supported by Selenium/Appium's By class.");
                return null;
            default:
                return null;
        }
    }
}