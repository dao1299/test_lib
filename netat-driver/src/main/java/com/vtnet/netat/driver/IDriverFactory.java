package com.vtnet.netat.driver;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;

public interface IDriverFactory {
//    WebDriver createDriver(String platform);
    WebDriver createDriver(String platform, MutableCapabilities capabilities);
}
