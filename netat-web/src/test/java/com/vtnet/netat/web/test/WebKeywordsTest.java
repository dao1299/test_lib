package com.vtnet.netat.web.test;

import com.vtnet.netat.web.keywords.WebKeywords;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Demo test class for Web Keywords
 * Shows how to use basic web automation keywords
 */
public class WebKeywordsTest {

    private WebKeywords webKeywords;

    @BeforeMethod
    public void setUp() {
        webKeywords = new WebKeywords();
    }

    @AfterMethod
    public void tearDown() {
        try {
            webKeywords.closeBrowser();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testGoogleSearch() {
        // Open browser and navigate to Google
        webKeywords.openBrowser("chrome", "https://www.google.com");

        // Search for something
        webKeywords.inputText("name=q", "NETAT automation framework");
        webKeywords.clickElement("name=btnK");

        // Verify results page
        webKeywords.elementShouldBeVisible("id=search");

        String pageTitle = webKeywords.getTitle();
        System.out.println("Page title: " + pageTitle);
    }

    @Test
    public void testFormInteraction() {
        // Open a demo form page
        webKeywords.openBrowser("chrome", "https://www.w3schools.com/html/html_forms.asp");

        // Fill out form fields
        webKeywords.inputText("xpath=//input[@name='firstname']", "John");
        webKeywords.inputText("xpath=//input[@name='lastname']", "Doe");

        // Get text from elements
        String firstName = webKeywords.getText("xpath=//input[@name='firstname']");
        System.out.println("First name: " + firstName);
    }

    @Test
    public void testElementVerification() {
        webKeywords.openBrowser("chrome", "https://example.com");

        // Verify page elements
        webKeywords.elementShouldBeVisible("tag=h1");

        String headerText = webKeywords.getText("tag=h1");
        System.out.println("Header text: " + headerText);

        String pageTitle = webKeywords.getTitle();
        System.out.println("Page title: " + pageTitle);
    }
}