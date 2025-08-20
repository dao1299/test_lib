package com.vtnet.netat.web.elements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Locator {
    public enum Strategy {
        ID, NAME, XPATH, CSS_SELECTOR, CLASS_NAME, LINK_TEXT, PARTIAL_LINK_TEXT,
        TAG_NAME, ACCESSIBILITY_ID, ANDROID_UIAUTOMATOR, IOS_PREDICATE_STRING,
        IOS_CLASS_CHAIN, IMAGE, JQUERY;
    }

    private Strategy strategy;
    private String value;
    private boolean active;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // --- Constructors ---
    public Locator() {}

    public Locator(Strategy strategy, String value, boolean active) {
        this.strategy = strategy;
        this.value = value;
        this.active = active;
    }

    // --- Getters and Setters ---
    public Strategy getStrategy() { return strategy; }
    public void setStrategy(Strategy strategy) { this.strategy = strategy; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // --- Core & JSON Logic (Giữ nguyên) ---

    public By convertToBy() {
        if (this.value == null || this.value.trim().isEmpty()) {
            throw new IllegalStateException("Giá trị (value) của locator không được để trống.");
        }
        switch (strategy) {
            case ID: return By.id(value);
            case NAME: return By.name(value);
            case XPATH: return By.xpath(value);
            case CSS_SELECTOR: return By.cssSelector(value);
            case CLASS_NAME: return By.className(value);
            case LINK_TEXT: return By.linkText(value);
            case PARTIAL_LINK_TEXT: return By.partialLinkText(value);
            case TAG_NAME: return By.tagName(value);
            case ACCESSIBILITY_ID: return AppiumBy.accessibilityId(value);
            case ANDROID_UIAUTOMATOR: return AppiumBy.androidUIAutomator(value);
            case IOS_PREDICATE_STRING: return AppiumBy.iOSNsPredicateString(value);
            case IOS_CLASS_CHAIN: return AppiumBy.iOSClassChain(value);
            default:
                throw new UnsupportedOperationException("Chiến lược locator '" + strategy + "' không được hỗ trợ.");
        }
    }

    @Override
    public String toString() {
        return "Locator{" + "strategy=" + strategy + ", value='" + value + '\'' + '}';
    }

    public String toJson() throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public static Locator fromJson(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, Locator.class);
    }
}