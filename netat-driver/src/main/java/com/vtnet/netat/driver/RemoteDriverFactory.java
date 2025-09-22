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
    public WebDriver createDriver(String platform, MutableCapabilities capabilities) {
        try {
            String gridUrl = ConfigReader.getProperty("grid.url");
            log.info("Initializing remote driver for {} at Grid: {}", platform, gridUrl);

            return new RemoteWebDriver(new URL(gridUrl), capabilities);
        } catch (MalformedURLException e) {
            log.error("The Grid URL is invalid.", e);
            throw new RuntimeException("The Grid URL is invalid.", e);
        }
    }
}