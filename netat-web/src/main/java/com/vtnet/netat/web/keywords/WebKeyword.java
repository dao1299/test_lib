package com.vtnet.netat.web.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.driver.ConfigReader;
import com.vtnet.netat.driver.DriverManager; // QUAN TRỌNG: Import DriverManager
import com.vtnet.netat.web.ai.AiModelFactory;
import com.vtnet.netat.web.elements.Locator;
import com.vtnet.netat.web.elements.ObjectUI;
import dev.langchain4j.model.chat.ChatModel;
import io.qameta.allure.Step;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebKeyword extends BaseKeyword {


    // Cấu hình thời gian chờ mặc định, bạn cũng có thể đọc từ ConfigReader
    private static final Logger log = LoggerFactory.getLogger(WebKeyword.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration PRIMARY_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration SECONDARY_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Constructor rỗng. Lớp này sẽ tự lấy WebDriver từ DriverManager.
     */
    public WebKeyword() {
        // Không cần làm gì ở đây
    }

    @NetatKeyword(
            name = "openUrl",
            description = "Mở một trang web với URL được chỉ định.",
            category = "WEB",
            parameters = {"String: url - Địa chỉ trang web cần mở."},
            example = "openUrl | | https://google.com"
    )
    @Step("Mở URL: {0}")
    public void openUrl(String url) {
        log.info("Mở URL: {}", url);
        // Lấy driver từ DriverManager và thực hiện hành động
        DriverManager.getDriver().get(url);
    }

    /**
     * Tìm kiếm một WebElement.
     */
    private WebElement findElement(ObjectUI uiObject) {
        WebDriver driver = DriverManager.getDriver();
        List<Locator> locators = uiObject.getActiveLocators();

        if (locators.isEmpty()) {
            throw new IllegalArgumentException("Không có locator nào active cho đối tượng: " + uiObject.getName());
        }

        // --- BƯỚC 1: Thử với các locator đã được định nghĩa ---
        for (int i = 0; i < locators.size(); i++) {
            Locator locator = locators.get(i);
            Duration timeout = (i == 0) ? PRIMARY_TIMEOUT : SECONDARY_TIMEOUT;
            WebDriverWait wait = new WebDriverWait(driver, timeout);

            try {
                By by = locator.convertToBy();
                log.info("Đang tìm element '{}' bằng locator: {} (Timeout: {}s)", uiObject.getName(), locator, timeout.getSeconds());
                return wait.until(ExpectedConditions.presenceOfElementLocated(by));
            } catch (Exception e) {
                log.warn("Không tìm thấy element '{}' với locator {}.", uiObject.getName(), locator);
            }
        }

        log.warn("Thất bại với tất cả locator đã định nghĩa. Chuyển sang cơ chế tìm kiếm bằng AI.");

        // --- BƯỚC 2: Thử với cơ chế Self-Healing bằng AI ---
        try {
            String aiLocatorValue = getLocatorByAI(uiObject.getName()+" :[description: "+uiObject.getDescription()+"] ", driver.getPageSource());

            if (aiLocatorValue != null && !aiLocatorValue.isEmpty()) {
                log.info("AI đã đề xuất locator mới (CSS): '{}'", aiLocatorValue);
                WebDriverWait aiWait = new WebDriverWait(driver, SECONDARY_TIMEOUT);
                return aiWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(aiLocatorValue)));
            }
        } catch (Exception e) {
            log.error("Tìm kiếm bằng locator do AI đề xuất cũng thất bại.", e);
        }

        // --- BƯỚC 3: Nếu tất cả đều thất bại ---
        throw new NoSuchElementException("Không thể tìm thấy phần tử '" + uiObject.getName() + "' bằng bất kỳ phương pháp nào.");
    }

    /**
     * Gọi đến mô hình AI để lấy locator đề xuất.
     */
    private String getLocatorByAI(String elementName, String html) {
        boolean isEnabled = Boolean.parseBoolean(ConfigReader.getProperty("ai.self.healing.enabled", "false"));
        if (!isEnabled) {
            return "";
        }

        ChatModel model = AiModelFactory.createModel();
        if (model == null) return "";

        final String PROMPT = "You are a web automation expert. Your task is to generate a selenium locator for a given element based on the provided HTML.  get the selenium locator for {element} with the following html ```html  {html} ```  return with css selector, dont explain, just return the locator ";

        try {
            String prompt = PROMPT.replace("{element}", elementName).replace("{html}", html);
            log.info("PROMPT: "+prompt);
            String chatResponse  = model.chat(prompt.replace("{element}", elementName).replace("{html}", html)).trim();
            log.info("\n\nRESPONSE: "+chatResponse);
            Pattern pattern = Pattern.compile("(?s)```.*?\\n(.*?)\\n```");
            Matcher matcher = pattern.matcher(chatResponse);
            String locator = "";
            if (matcher.find()) {
                locator = matcher.group(1).trim();
            } else {
                // Trường hợp 2: Inline, sau RESPONSE:
                locator = chatResponse.replace("RESPONSE:", "")
                        .replace("```css", "")
                        .replace("```", "")
                        .trim();
            }
            return locator;
        } catch (Exception e) {
            log.error("Đã xảy ra lỗi khi giao tiếp với mô hình AI.", e);
        }
        return "";
    }

    @NetatKeyword(
            name = "click",
            description = "Thực hiện hành động click chuột vào một phần tử trên giao diện.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Đối tượng cần click."},
            example = "click | LoginPage/login_button |"
    )
    @Step("Click vào phần tử: {0.name}")
    public void click(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(element)).click();
            return null; // Trả về null cho các phương thức void
        }, uiObject);
    }

    /**
     * Nhập văn bản vào một phần tử.
     */
    @NetatKeyword(
            name = "sendKeys",
            description = "Nhập một chuỗi văn bản vào một phần tử (ô input, textarea).",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Đối tượng cần nhập liệu.",
                    "String: text - Chuỗi văn bản cần nhập."
            },
            example = "sendKeys | LoginPage/username_input | my_user_name"
    )
    @Step("Nhập văn bản '{1}' vào phần tử: {0.name}")
    public void sendKeys(ObjectUI uiObject, String text) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOf(element));
            element.clear();
            element.sendKeys(text);
            return null;
        }, uiObject, text);
    }

    /**
     * Lấy văn bản từ một phần tử.
     */
    @NetatKeyword(
            name = "getText",
            description = "Trả về chuỗi văn bản đang HIỂN THỊ của phần tử (giá trị tương đương WebElement#getText()). " +
                    "Keyword sẽ CHỜ cho đến khi phần tử ở trạng thái visible trước khi đọc text, giúp hạn chế lỗi do DOM chưa sẵn sàng.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Đối tượng UI cần lấy văn bản (được định nghĩa trong Object Repository)."
            },
            example = "getText | LoginPage/welcome_label"
    )
    @Step("Get text from element {0} ")
    public String getText(ObjectUI uiObject) {
        return execute(() -> {
            WebElement element = findElement(uiObject);
            return new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOf(element)).getText();
        }, uiObject);
    }

    /**
     * Cuộn đến vị trí của một phần tử trên trang.
     */
    @NetatKeyword(
            name = "scrollToElement",
            description = "Cuộn trang đến khi phần tử nằm trong vùng nhìn thấy (viewport) bằng JavaScript " +
                    "(scrollIntoView(true)). Không thực hiện thao tác click; chỉ đảm bảo phần tử xuất hiện trong tầm nhìn " +
                    "để các bước tiếp theo (click, sendKeys, assert...) ổn định hơn.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Đối tượng UI cần cuộn tới (được định nghĩa trong Object Repository)."
            },
            example = "scrollToElement | Common/submit_button"
    )
    @Step("Scroll to element {0}")
    public void scrollToElement(ObjectUI uiObject) {
        log.info("Cuộn tới phần tử: {}", uiObject.getName());
        WebElement element = findElement(uiObject);
        // Lấy JavascriptExecutor từ driver hiện tại
        JavascriptExecutor jsExecutor = (JavascriptExecutor) DriverManager.getDriver();
        jsExecutor.executeScript("arguments[0].scrollIntoView(true);", element);
    }

    // Các phương thức waitForElementVisible và getAttribute cũng được cập nhật tương tự...

}