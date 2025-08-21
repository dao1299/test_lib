package com.vtnet.netat.driver;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.MutableCapabilities;
import java.net.URL;

public class MobileDriverFactory implements IDriverFactory{
    @Override
    public AppiumDriver createDriver() {
        String platform = ConfigReader.getProperty("platform.name");
        String appiumServerUrl = ConfigReader.getProperty("appium.server.url", "http://127.0.0.1:4723/");

        try {
            MutableCapabilities capabilities = CapabilityFactory.getCapabilities(platform);

            if ("android".equalsIgnoreCase(platform)) {
                return new AndroidDriver(new URL(appiumServerUrl), capabilities);
            } else if ("ios".equalsIgnoreCase(platform)) {
                return new IOSDriver(new URL(appiumServerUrl), capabilities);
            } else {
                throw new IllegalArgumentException("Nền tảng di động không được hỗ trợ: " + platform);
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi tạo Appium driver.", e);
        }
    }
}
