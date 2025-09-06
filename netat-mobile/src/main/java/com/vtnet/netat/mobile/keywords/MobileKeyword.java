// File: netat-mobile/src/main/java/com/vtnet/netat/mobile/keywords/MobileKeyword.java
package com.vtnet.netat.mobile.keywords;

import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.driver.DriverManager;
import com.vtnet.netat.core.ui.ObjectUI;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

public class MobileKeyword {

    // Helper method để lấy AppiumDriver
    private AppiumDriver getDriver() {
        return (AppiumDriver) DriverManager.getDriver();
    }

    // Helper method để tìm element (tương tự WebKeyword)
    private WebElement findElement(ObjectUI uiObject) {
        // TODO: Logic tìm kiếm phần tử, có thể tái sử dụng từ WebKeyword nếu phù hợp
        return null;
    }

    @NetatKeyword(name = "tap", category = "MOBILE", parameters = {"ObjectUI: uiObject"})
    @Step("Chạm vào phần tử: {0.name}")
    public void tap(ObjectUI uiObject) {
        WebElement element = findElement(uiObject);
        element.click(); // Với Appium, .click() thường tương đương với .tap()
    }

    @NetatKeyword(name = "sendKeys", category = "MOBILE", parameters = {"ObjectUI: uiObject", "String: text"})
    @Step("Nhập văn bản '{1}' vào phần tử: {0.name}")
    public void sendKeys(ObjectUI uiObject, String text) {
        WebElement element = findElement(uiObject);
        element.sendKeys(text);
    }
}