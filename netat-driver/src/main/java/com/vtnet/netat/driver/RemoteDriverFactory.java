package com.vtnet.netat.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class RemoteDriverFactory implements IDriverFactory {
    private static final Logger log = LoggerFactory.getLogger(RemoteDriverFactory.class);

    @Override
    public WebDriver createDriver(String platform) {
        try {
            String gridUrl = ConfigReader.getProperty("grid.url");


            log.info("Khởi tạo remote driver cho {} tại Grid: {}", platform, gridUrl);


            MutableCapabilities capabilities = CapabilityFactory.getCapabilities(platform);

            return new RemoteWebDriver(new URL(gridUrl), capabilities);
        } catch (MalformedURLException e) {
            log.error("URL của Grid không hợp lệ.", e);
            throw new RuntimeException("URL của Grid không hợp lệ.", e);
        }
    }
}