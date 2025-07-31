package com.vtnet.netat.web.test;

import com.vtnet.netat.web.elements.Locator;
import com.vtnet.netat.web.elements.NetatUIObject;
import java.util.ArrayList;
import java.util.List;

public class UIObjectLocatorExample {
    public static void main(String[] args) {
        // Create list to store locators
        List<Locator> locators = new ArrayList<>();

        // Create ID locator
        Locator idLocator = new Locator();
        idLocator.setStrategy("id");
        idLocator.setValue("loginButton");
        idLocator.setActive(true);
        idLocator.setPriority(1);
        idLocator.setReliability(0.9);
        locators.add(idLocator);

        // Create XPath locator
        Locator xpathLocator = new Locator();
        xpathLocator.setStrategy("xpath");
        xpathLocator.setValue("//button[@type='submit' and contains(@class, 'login')]");
        xpathLocator.setActive(true);
        xpathLocator.setPriority(2);
        xpathLocator.setReliability(0.8);
        locators.add(xpathLocator);

        // Create CSS locator
        Locator cssLocator = new Locator();
        cssLocator.setStrategy("css");
        cssLocator.setValue(".login-button");
        cssLocator.setActive(true);
        cssLocator.setPriority(3);
        cssLocator.setReliability(0.7);
        locators.add(cssLocator);

        // Create name locator
        Locator nameLocator = new Locator();
        nameLocator.setStrategy("name");
        nameLocator.setValue("login");
        nameLocator.setActive(true);
        nameLocator.setPriority(4);
        nameLocator.setReliability(0.6);
        locators.add(nameLocator);

        // Create NetatUIObject with the locators
        NetatUIObject loginButton = new NetatUIObject();
        loginButton.setPath("login/loginButton");
        loginButton.setName("Login Button");
        loginButton.setDescription("Main login button on the login page");
        loginButton.setType("Button");
        loginButton.setLocators(locators);

        // Print the JSON representation
        System.out.println(loginButton.toPrettyJson());
    }
}