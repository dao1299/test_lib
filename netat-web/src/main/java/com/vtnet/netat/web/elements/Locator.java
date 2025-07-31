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

    private String strategy;
    private String value;
    private List<String> type;
    private boolean active;



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

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Locator fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Locator.class);
    }

    public By convertToSeleniumBy() {
        switch (strategy) {
            case LOCATOR_ID:
                return By.id(value);
            case LOCATOR_ID_BASE:
                return AppiumBy.id(value);
            case LOCATOR_NAME:
                return By.name(value);
            case LOCATOR_NAME_BASE:
                return AppiumBy.name(value);
            case LOCATOR_LINKTEXT:
                return By.linkText(value);
            case LOCATOR_PARTIAL_LINKTEXT:
                return By.partialLinkText(value);
            case LOCATOR_TAG_NAME:
                return By.tagName(value);
            case LOCATOR_CLASS:
                return By.className(value);
            case LOCATOR_CSS:
                return By.cssSelector(value);
            case LOCATOR_XPATH:
                return By.xpath(value);
            case LOCATOR_IMAGE:
                return AppiumBy.image(value);
            case LOCATOR_CLASS_NAME:
                return AppiumBy.className(value);
            case LOCATOR_ACCESSIBILITY_ID:
                return AppiumBy.accessibilityId(value);
            case LOCATOR_ANDROID_UIAUTOMATOR:
                return AppiumBy.androidUIAutomator(value);
            case LOCATOR_IOS_PREDICATE_STRING:
                return AppiumBy.iOSNsPredicateString(value);
            case LOCATOR_IOS_CLASS_CHAIN:
                return AppiumBy.iOSClassChain(value);
            default:
                return null;
        }
    }
}
