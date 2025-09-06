package com.vtnet.netat.web.keywords;

import com.vtnet.netat.core.BaseUiKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.ui.ObjectUI;
import com.vtnet.netat.driver.ConfigReader;
import com.vtnet.netat.driver.DriverManager;
import com.vtnet.netat.web.ai.AiModelFactory;
import dev.langchain4j.model.chat.ChatModel;
import io.qameta.allure.Step;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebKeyword extends BaseUiKeyword {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration SECONDARY_TIMEOUT = Duration.ofSeconds(5);

    public WebKeyword() {
        // Constructor rỗng
    }

    /**
     * Ghi đè phương thức findElement để thêm cơ chế AI Self-Healing.
     */
    @Override
    protected WebElement findElement(ObjectUI uiObject) {
        try {
            // 1. Thử tìm bằng logic chung của lớp cha trước (duyệt qua các locator đã định nghĩa)
            return super.findElement(uiObject);
        } catch (NoSuchElementException e) {
            logger.warn("Thất bại với tất cả locator đã định nghĩa. Chuyển sang cơ chế tìm kiếm bằng AI.");

            // 2. Thử với cơ chế Self-Healing bằng AI
            try {
                String aiLocatorValue = getLocatorByAI(uiObject.getName() + " :[description: " + uiObject.getDescription() + "] ", DriverManager.getDriver().getPageSource());
                if (aiLocatorValue != null && !aiLocatorValue.isEmpty()) {
                    logger.info("AI đã đề xuất locator mới (CSS): '{}'", aiLocatorValue);
                    WebDriverWait aiWait = new WebDriverWait(DriverManager.getDriver(), SECONDARY_TIMEOUT);
                    return aiWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(aiLocatorValue)));
                }
            } catch (Exception aiException) {
                logger.error("Tìm kiếm bằng locator do AI đề xuất cũng thất bại.", aiException);
            }

            // 3. Nếu tất cả đều thất bại
            throw new NoSuchElementException("Không thể tìm thấy phần tử '" + uiObject.getName() + "' bằng bất kỳ phương pháp nào.");
        }
    }

    // --- CÁC KEYWORD CHUNG ĐƯỢC GHI ĐÈ ĐỂ THÊM ANNOTATION ---

    @Override
    @NetatKeyword(
            name = "click",
            description = "Thực hiện hành động click chuột vào một phần tử trên giao diện.",
            category = "WEB",
            example = "click | LoginPage/login_button |"
    )
    @Step("Click vào phần tử: {0.name}")
    public void click(ObjectUI uiObject) {
        super.click(uiObject); // Gọi logic từ lớp cha
    }

    @Override
    @NetatKeyword(
            name = "sendKeys",
            description = "Nhập một chuỗi văn bản vào một phần tử (ô input, textarea).",
            category = "WEB",
            example = "sendKeys | LoginPage/username_input | my_user_name"
    )
    @Step("Nhập văn bản '{1}' vào phần tử: {0.name}")
    public void sendKeys(ObjectUI uiObject, String text) {
        super.sendKeys(uiObject, text); // Gọi logic từ lớp cha
    }

    @Override
    @NetatKeyword(
            name = "getText",
            description = "Trả về chuỗi văn bản đang HIỂN THỊ của phần tử.",
            category = "WEB",
            example = "getText | LoginPage/welcome_label"
    )
    @Step("Get text from element {0}")
    public String getText(ObjectUI uiObject) {
        return super.getText(uiObject); // Gọi logic từ lớp cha
    }

    // --- CÁC KEYWORD ĐẶC THÙ CỦA WEB ---

    @NetatKeyword(
            name = "openUrl",
            description = "Mở một trang web với URL được chỉ định.",
            category = "WEB",
            parameters = {"String: url - Địa chỉ trang web cần mở."},
            example = "openUrl | | https://google.com"
    )
    @Step("Mở URL: {0}")
    public void openUrl(String url) {
        DriverManager.getDriver().get(url);
    }

    @NetatKeyword(
            name = "scrollToElement",
            description = "Cuộn trang đến khi phần tử nằm trong vùng nhìn thấy.",
            category = "WEB",
            example = "scrollToElement | Common/submit_button"
    )
    @Step("Scroll to element {0}")
    public void scrollToElement(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            JavascriptExecutor jsExecutor = (JavascriptExecutor) DriverManager.getDriver();
            jsExecutor.executeScript("arguments[0].scrollIntoView(true);", element);
            return null;
        }, uiObject);
    }

    // --- PHƯƠNG THỨC HỖ TRỢ (PRIVATE) ---

    private String getLocatorByAI(String elementName, String html) {
        // ... giữ nguyên logic của phương thức này ...
        boolean isEnabled = Boolean.parseBoolean(ConfigReader.getProperty("ai.self.healing.enabled", "false"));
        if (!isEnabled) {
            return "";
        }

        ChatModel model = AiModelFactory.createModel();
        if (model == null) return "";

        final String PROMPT = "You are a web automation expert. Your task is to generate a selenium locator for a given element based on the provided HTML.  get the selenium locator for {element} with the following html ```html  {html} ```  return with css selector, dont explain, just return the locator ";

        try {
            String prompt = PROMPT.replace("{element}", elementName).replace("{html}", html);
//            logger.info("PROMPT: "+prompt);
            String chatResponse = model.chat(prompt.replace("{element}", elementName).replace("{html}", html)).trim();
//            logger.info("\n\nRESPONSE: "+chatResponse);
            Pattern pattern = Pattern.compile("(?s)```.*?\\n(.*?)\\n```");
            Matcher matcher = pattern.matcher(chatResponse);
            String locator = "";
            if (matcher.find()) {
                locator = matcher.group(1).trim();
            } else {
                locator = chatResponse.replace("RESPONSE:", "")
                        .replace("```css", "")
                        .replace("```", "")
                        .trim();
            }
            return locator;
        } catch (Exception e) {
            logger.error("Đã xảy ra lỗi khi giao tiếp với mô hình AI.", e);
        }
        return "";
    }

    @NetatKeyword(
            name = "goBack",
            description = "Quay lại trang trước đó trong lịch sử trình duyệt.",
            category = "WEB",
            example = "goBack | |"
    )
    @Step("Quay lại trang trước")
    public void goBack() {
        execute(() -> {
            DriverManager.getDriver().navigate().back();
            return null;
        });
    }

    @NetatKeyword(
            name = "goForward",
            description = "Đi tới trang tiếp theo trong lịch sử trình duyệt.",
            category = "WEB",
            example = "goForward | |"
    )
    @Step("Đi tới trang sau")
    public void goForward() {
        execute(() -> {
            DriverManager.getDriver().navigate().forward();
            return null;
        });
    }

    @NetatKeyword(
            name = "refresh",
            description = "Tải lại (làm mới) trang web hiện tại.",
            category = "WEB",
            example = "refresh | |"
    )
    @Step("Tải lại trang")
    public void refresh() {
        execute(() -> {
            DriverManager.getDriver().navigate().refresh();
            return null;
        });
    }

    @NetatKeyword(
            name = "switchToWindowByTitle",
            description = "Chuyển sang cửa sổ hoặc tab có tiêu đề chính xác như mong đợi.",
            category = "WEB",
            example = "switchToWindowByTitle | | Trang sản phẩm"
    )
    @Step("Chuyển sang cửa sổ có tiêu đề: {0}")
    public void switchToWindowByTitle(String windowTitle) {
        execute(() -> {
            String currentWindow = DriverManager.getDriver().getWindowHandle();
            Set<String> allWindows = DriverManager.getDriver().getWindowHandles();
            for (String windowHandle : allWindows) {
                if (!windowHandle.equals(currentWindow)) {
                    DriverManager.getDriver().switchTo().window(windowHandle);
                    if (DriverManager.getDriver().getTitle().equals(windowTitle)) {
                        return null; // Tìm thấy và chuyển thành công
                    }
                }
            }
            // Nếu không tìm thấy, quay lại cửa sổ ban đầu và báo lỗi
            DriverManager.getDriver().switchTo().window(currentWindow);
            throw new NoSuchWindowException("Không tìm thấy cửa sổ nào có tiêu đề: " + windowTitle);
        }, windowTitle);
    }

    @NetatKeyword(
            name = "switchToFrame",
            description = "Chuyển ngữ cảnh điều khiển vào một iframe.",
            category = "WEB",
            example = "switchToFrame | HomePage/payment_iframe |"
    )
    @Step("Chuyển vào iframe: {0.name}")
    public void switchToFrame(ObjectUI uiObject) {
        execute(() -> {
            WebElement frameElement = findElement(uiObject);
            DriverManager.getDriver().switchTo().frame(frameElement);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "switchToParentFrame",
            description = "Thoát khỏi ngữ cảnh iframe hiện tại và quay về iframe cha gần nhất.",
            category = "WEB",
            example = "switchToParentFrame | |"
    )
    @Step("Quay về iframe cha")
    public void switchToParentFrame() {
        execute(() -> {
            DriverManager.getDriver().switchTo().parentFrame();
            return null;
        });
    }

// --- GIAI ĐOẠN 1: NHÓM KEYWORD ĐỒNG BỘ HÓA (WAITS) ---

    @NetatKeyword(
            name = "waitForElementClickable",
            description = "Chờ cho đến khi một phần tử hiển thị và sẵn sàng để được click.",
            category = "WEB",
            example = "waitForElementClickable | CheckoutPage/submit_button |"
    )
    @Step("Chờ cho phần tử {0.name} sẵn sàng để click")
    public void waitForElementClickable(ObjectUI uiObject) {
        execute(() -> {
            new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(findElement(uiObject)));
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "waitForElementNotVisible",
            description = "Chờ cho đến khi một phần tử không còn hiển thị trên giao diện.",
            category = "WEB",
            example = "waitForElementNotVisible | Common/loading_spinner |"
    )
    @Step("Chờ cho phần tử {0.name} biến mất")
    public void waitForElementNotVisible(ObjectUI uiObject) {
        execute(() -> {
            // Phải tìm element trước, sau đó mới chờ nó biến mất
            WebElement element = findElement(uiObject);
            new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.invisibilityOf(element));
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "contextClick",
            description = "Thực hiện hành động click chuột phải vào một phần tử.",
            category = "WEB",
            example = "contextClick | Files/document_icon |"
    )
    @Step("Click chuột phải vào phần tử: {0.name}")
    public void contextClick(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            Actions actions = new Actions(DriverManager.getDriver());
            actions.contextClick(element).perform();
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "dragAndDrop",
            description = "Kéo một phần tử nguồn và thả vào vị trí của phần tử đích.",
            category = "WEB",
            example = "dragAndDrop | Draggable/source_item | Droppable/target_area"
    )
    @Step("Kéo phần tử {0.name} và thả vào {1.name}")
    public void dragAndDrop(ObjectUI sourceObject, ObjectUI targetObject) {
        execute(() -> {
            WebElement sourceElement = findElement(sourceObject);
            WebElement targetElement = findElement(targetObject);
            Actions actions = new Actions(DriverManager.getDriver());
            actions.dragAndDrop(sourceElement, targetElement).perform();
            return null;
        }, sourceObject, targetObject);
    }

    @NetatKeyword(
            name = "check",
            description = "Đảm bảo một checkbox hoặc radio button được chọn. Nếu chưa được chọn, sẽ click để chọn.",
            category = "WEB",
            example = "check | Form/terms_and_conditions_checkbox |"
    )
    @Step("Đảm bảo phần tử {0.name} đã được chọn")
    public void check(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            if (!element.isSelected()) {
                element.click();
            }
            return null;
        }, uiObject);
    }

// --- GIAI ĐOẠN 2: XỬ LÝ SHADOW DOM ---

    @NetatKeyword(
            name = "findElementInShadowDom",
            description = "Tìm một phần tử nằm bên trong một Shadow DOM. Yêu cầu ObjectUI của Shadow Host và CSS Selector của phần tử bên trong.",
            category = "WEB",
            example = "findElementInShadowDom | Page/app_container | css=input#username"
    )
    @Step("Tìm phần tử '{1}' bên trong Shadow DOM của {0.name}")
    public WebElement findElementInShadowDom(ObjectUI shadowHostObject, String cssSelectorInShadow) {
        return execute(() -> {
            WebElement shadowHost = findElement(shadowHostObject);
            SearchContext shadowRoot = shadowHost.getShadowRoot();
            return shadowRoot.findElement(By.cssSelector(cssSelectorInShadow));
        }, shadowHostObject, cssSelectorInShadow);
    }

// --- GIAI ĐOẠN 2: NHÓM CUỘN VÀ VIEWPORT ---

    @NetatKeyword(
            name = "scrollToCoordinates",
            description = "Cuộn trang đến một tọa độ (x, y) cụ thể.",
            category = "WEB",
            example = "scrollToCoordinates | | 0, 500"
    )
    @Step("Cuộn trang đến tọa độ ({0}, {1})")
    public void scrollToCoordinates(int x, int y) {
        execute(() -> {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
            js.executeScript("window.scrollTo(arguments[0], arguments[1])", x, y);
            return null;
        }, x, y);
    }

    @NetatKeyword(
            name = "resizeWindow",
            description = "Thay đổi kích thước cửa sổ trình duyệt.",
            category = "WEB",
            example = "resizeWindow | | 1280, 720"
    )
    @Step("Thay đổi kích thước cửa sổ thành {0}x{1}")
    public void resizeWindow(int width, int height) {
        execute(() -> {
            Dimension dimension = new Dimension(width, height);
            DriverManager.getDriver().manage().window().setSize(dimension);
            return null;
        }, width, height);
    }


    // =================================================================================
    // --- NHÓM KEYWORD KIỂM CHỨNG (ASSERTIONS) CHO WEB ---
    // =================================================================================

    // --- Verify Element Visible ---
    @NetatKeyword(name = "verifyElementVisibleHard", description = "Kiểm tra phần tử có hiển thị. Dừng lại nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) phần tử {0.name} có hiển thị là {1}")
    public void verifyElementVisibleHard(ObjectUI uiObject, boolean isVisible) {
        performVisibilityAssertion(uiObject, isVisible, false);
    }

    @NetatKeyword(name = "verifyElementVisibleSoft", description = "Kiểm tra phần tử có hiển thị. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) phần tử {0.name} có hiển thị là {1}")
    public void verifyElementVisibleSoft(ObjectUI uiObject, boolean isVisible) {
        performVisibilityAssertion(uiObject, isVisible, true);
    }

    // --- Verify Text ---
    @NetatKeyword(name = "verifyTextHard", description = "So sánh văn bản chính xác. Dừng lại nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) văn bản của {0.name} là '{1}'")
    public void verifyTextHard(ObjectUI uiObject, String expectedText) {
        performTextAssertion(uiObject, expectedText, false);
    }

    @NetatKeyword(name = "verifyTextSoft", description = "So sánh văn bản chính xác. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) văn bản của {0.name} là '{1}'")
    public void verifyTextSoft(ObjectUI uiObject, String expectedText) {
        performTextAssertion(uiObject, expectedText, true);
    }

    // --- Verify Text Contains ---
    @NetatKeyword(name = "verifyTextContainsHard", description = "Kiểm tra văn bản có chứa chuỗi con. Dừng lại nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) văn bản của {0.name} có chứa '{1}'")
    public void verifyTextContainsHard(ObjectUI uiObject, String partialText) {
        performTextContainsAssertion(uiObject, partialText, false);
    }

    @NetatKeyword(name = "verifyTextContainsSoft", description = "Kiểm tra văn bản có chứa chuỗi con. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) văn bản của {0.name} có chứa '{1}'")
    public void verifyTextContainsSoft(ObjectUI uiObject, String partialText) {
        performTextContainsAssertion(uiObject, partialText, true);
    }

    // --- Verify Element Attribute ---
    @NetatKeyword(name = "verifyElementAttributeHard", description = "Kiểm tra giá trị thuộc tính. Dừng lại nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) thuộc tính '{1}' của {0.name} là '{2}'")
    public void verifyElementAttributeHard(ObjectUI uiObject, String attributeName, String expectedValue) {
        performAttributeAssertion(uiObject, attributeName, expectedValue, false);
    }

    @NetatKeyword(name = "verifyElementAttributeSoft", description = "Kiểm tra giá trị thuộc tính. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) thuộc tính '{1}' của {0.name} là '{2}'")
    public void verifyElementAttributeSoft(ObjectUI uiObject, String attributeName, String expectedValue) {
        performAttributeAssertion(uiObject, attributeName, expectedValue, true);
    }

    // --- CÁC KEYWORD ĐẶC THÙ CỦA WEB ---

    @NetatKeyword(name = "verifyTitleHard", description = "Kiểm tra tiêu đề trang. Dừng lại nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) tiêu đề trang là '{0}'")
    public void verifyTitleHard(String expectedTitle) {
        // Logic cho các keyword đặc thù của web có thể được viết trực tiếp ở đây
        execute(() -> {
            String actualTitle = DriverManager.getDriver().getTitle();
            Assert.assertEquals(actualTitle, expectedTitle, "HARD ASSERT FAILED: Tiêu đề trang không khớp.");
            return null;
        }, expectedTitle);
    }

    // =================================================================================
    // --- 1. TƯƠNG TÁC NÂNG CAO ---
    // =================================================================================

    @NetatKeyword(name = "clearText", description = "Xóa toàn bộ văn bản trong một ô input hoặc textarea.", category = "WEB")
    @Step("Xóa văn bản trong phần tử: {0.name}")
    public void clearText(ObjectUI uiObject) {
        execute(() -> {
            findElement(uiObject).clear();
            return null;
        }, uiObject);
    }

    @NetatKeyword(name = "selectByIndex", description = "Chọn một tùy chọn trong dropdown dựa trên chỉ số (index).", category = "WEB")
    @Step("Chọn tùy chọn ở vị trí {1} cho dropdown {0.name}")
    public void selectByIndex(ObjectUI uiObject, int index) {
        execute(() -> {
            Select select = new Select(findElement(uiObject));
            select.selectByIndex(index);
            return null;
        }, uiObject, index);
    }

    // =================================================================================
    // --- 2. LẤY THÔNG TIN (GETTER) ---
    // =================================================================================

    @NetatKeyword(name = "getAttribute", description = "Lấy giá trị của một thuộc tính (attribute) trên phần tử.", category = "WEB")
    @Step("Lấy thuộc tính '{1}' của phần tử {0.name}")
    public String getAttribute(ObjectUI uiObject, String attributeName) {
        return execute(() -> findElement(uiObject).getAttribute(attributeName), uiObject, attributeName);
    }

    @NetatKeyword(name = "getCssValue", description = "Lấy giá trị của một thuộc tính CSS trên phần tử.", category = "WEB")
    @Step("Lấy giá trị CSS '{1}' của phần tử {0.name}")
    public String getCssValue(ObjectUI uiObject, String cssPropertyName) {
        return execute(() -> findElement(uiObject).getCssValue(cssPropertyName), uiObject, cssPropertyName);
    }

    @NetatKeyword(name = "getCurrentUrl", description = "Lấy URL của trang hiện tại.", category = "WEB")
    @Step("Lấy URL hiện tại")
    public String getCurrentUrl() {
        return execute(() -> DriverManager.getDriver().getCurrentUrl());
    }

    @NetatKeyword(name = "getPageTitle", description = "Lấy tiêu đề của trang hiện tại.", category = "WEB")
    @Step("Lấy tiêu đề trang")
    public String getPageTitle() {
        return execute(() -> DriverManager.getDriver().getTitle());
    }

    @NetatKeyword(name = "getElementCount", description = "Đếm số lượng phần tử khớp với locator.", category = "WEB")
    @Step("Đếm số lượng phần tử của: {0.name}")
    public int getElementCount(ObjectUI uiObject) {
        return execute(() -> {
            // Chỉ dùng locator đầu tiên để đếm
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return DriverManager.getDriver().findElements(by).size();
        }, uiObject);
    }

    // =================================================================================
    // --- 3. CHỜ ĐIỀU KIỆN (WAIT/SYNC) ---
    // =================================================================================

    @NetatKeyword(name = "waitForElementPresent", description = "Chờ cho đến khi một phần tử tồn tại trong DOM.", category = "WEB")
    @Step("Chờ phần tử {0.name} tồn tại trong DOM trong {1} giây")
    public void waitForElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.presenceOfElementLocated(uiObject.getActiveLocators().get(0).convertToBy()));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(name = "waitForPageLoaded", description = "Chờ cho trang tải xong hoàn toàn (document.readyState is complete).", category = "WEB")
    @Step("Chờ trang tải xong trong {0} giây")
    public void waitForPageLoaded(int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete"));
            return null;
        }, timeoutInSeconds);
    }

    // =================================================================================
    // --- 4. ASSERT (HARD) & 5. VERIFY (SOFT) ---
    // =================================================================================

    // (Bạn đã có các assert/verify về text, attribute, visibility. Dưới đây là phần bổ sung)

    protected void performSelectionAssertion(ObjectUI uiObject, boolean expectedSelection, boolean isSoft) {
        execute(() -> {
            boolean actualSelection = findElement(uiObject).isSelected();
            String message = String.format("Trạng thái lựa chọn của '%s' mong đợi là %b nhưng thực tế là %b.",
                    uiObject.getName(), expectedSelection, actualSelection);
            if (isSoft) {
                // Soft Assert logic
            } else {
                Assert.assertEquals(actualSelection, expectedSelection, "HARD ASSERT FAILED: " + message);
            }
            return null;
        }, uiObject, expectedSelection, isSoft);
    }

    @NetatKeyword(name = "assertElementSelected", description = "Khẳng định checkbox/radio được chọn. Dừng nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) phần tử {0.name} đã được chọn")
    public void assertElementSelected(ObjectUI uiObject) {
        // Gọi execute() ở đây
        execute(() -> {
            // Gọi đến phương thức logic của LỚP CHA (BaseUiKeyword) bằng "super"
            super.performSelectionAssertion(uiObject, true, false);
            return null;
        }, uiObject);
    }

    @NetatKeyword(name = "assertElementNotSelected", description = "Khẳng định checkbox/radio chưa được chọn. Dừng nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) phần tử {0.name} chưa được chọn")
    public void assertElementNotSelected(ObjectUI uiObject) {
        // Gọi execute() ở đây
        execute(() -> {
            // Gọi đến phương thức logic của LỚP CHA (BaseUiKeyword)
            super.performSelectionAssertion(uiObject, false, false);
            return null;
        }, uiObject);
    }

    // ... bạn có thể bổ sung các keyword verifyElementSelected, assertElementNotSelected, ... tương tự


    // =================================================================================
    // --- 6. WINDOW/FRAME/TAB CONTROL ---
    // =================================================================================

    @NetatKeyword(name = "switchToDefaultContent", description = "Thoát khỏi tất cả các iframe và quay về trang chính.", category = "WEB")
    @Step("Chuyển về nội dung trang chính")
    public void switchToDefaultContent() {
        execute(() -> {
            DriverManager.getDriver().switchTo().defaultContent();
            return null;
        });
    }

    // =================================================================================
    // --- 7. ALERT/POPUP HANDLING ---
    // =================================================================================

    @NetatKeyword(name = "getAlertText", description = "Lấy văn bản từ hộp thoại alert.", category = "WEB")
    @Step("Lấy văn bản từ alert")
    public String getAlertText() {
        return execute(() -> {
            Alert alert = new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.alertIsPresent());
            return alert.getText();
        });
    }

    @NetatKeyword(name = "sendKeysToAlert", description = "Nhập văn bản vào hộp thoại prompt.", category = "WEB")
    @Step("Nhập văn bản '{0}' vào alert")
    public void sendKeysToAlert(String text) {
        execute(() -> {
            Alert alert = new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.alertIsPresent());
            alert.sendKeys(text);
            return null;
        }, text);
    }

    // =================================================================================
    // --- 9. BROWSER/NAVIGATION & 10. UTILITY/SUPPORT ---
    // =================================================================================

    @NetatKeyword(name = "maximizeWindow", description = "Phóng to cửa sổ trình duyệt.", category = "WEB")
    @Step("Phóng to cửa sổ trình duyệt")
    public void maximizeWindow() {
        execute(() -> {
            DriverManager.getDriver().manage().window().maximize();
            return null;
        });
    }

    @NetatKeyword(name = "takeScreenshot", description = "Chụp ảnh toàn bộ màn hình và lưu vào đường dẫn được chỉ định.", category = "WEB")
    @Step("Chụp ảnh màn hình và lưu tại: {0}")
    public void takeScreenshot(String filePath) {
        execute(() -> {
            try {
                File scrFile = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(scrFile, new File(filePath));
            } catch (IOException e) {
                throw new RuntimeException("Không thể chụp hoặc lưu ảnh màn hình.", e);
            }
            return null;
        }, filePath);
    }

    @NetatKeyword(name = "highlightElement", description = "Làm nổi bật một phần tử trên trang để dễ dàng gỡ lỗi.", category = "WEB")
    @Step("Làm nổi bật phần tử: {0.name}")
    public void highlightElement(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
            js.executeScript("arguments[0].style.border='3px solid red'", element);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            js.executeScript("arguments[0].style.border=''", element);
            return null;
        }, uiObject);
    }

    @NetatKeyword(name = "pause", description = "Tạm dừng kịch bản trong một khoảng thời gian tĩnh (tính bằng mili giây).", category = "WEB")
    @Step("Tạm dừng trong {0} ms")
    public void pause(int milliseconds) {
        execute(() -> {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }, milliseconds);
    }

    // =================================================================================
// --- TƯƠNG TÁC (INTERACTION) ---
// =================================================================================
    @NetatKeyword(name = "uncheck", description = "Bỏ chọn một checkbox nếu nó đang được chọn.", category = "WEB")
    @Step("Bỏ chọn checkbox: {0.name}")
    public void uncheck(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            if (element.isSelected()) {
                element.click();
            }
            return null;
        }, uiObject);
    }

    @NetatKeyword(name = "selectRadioByValue", description = "Chọn một radio button trong nhóm dựa trên thuộc tính 'value'.", category = "WEB")
    @Step("Chọn radio button có value '{1}' trong nhóm {0.name}")
    public void selectRadioByValue(ObjectUI uiObject, String value) {
        execute(() -> {
            // Locator của uiObject nên trỏ đến tất cả các radio button trong nhóm, ví dụ: //input[@name='gender']
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            List<WebElement> radioButtons = DriverManager.getDriver().findElements(by);
            for (WebElement radio : radioButtons) {
                if (radio.getAttribute("value").equals(value)) {
                    radio.click();
                    return null;
                }
            }
            throw new NoSuchElementException("Không tìm thấy radio button với value: " + value);
        }, uiObject, value);
    }

    @NetatKeyword(name = "dragAndDropByOffset", description = "Kéo một phần tử theo một độ lệch (x, y), hữu ích cho slider.", category = "WEB")
    @Step("Kéo phần tử {0.name} theo độ lệch ({1}, {2})")
    public void dragAndDropByOffset(ObjectUI uiObject, int xOffset, int yOffset) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new Actions(DriverManager.getDriver()).dragAndDropBy(element, xOffset, yOffset).perform();
            return null;
        }, uiObject, xOffset, yOffset);
    }

    @NetatKeyword(name = "pressKeys", description = "Gửi một chuỗi hoặc tổ hợp phím tới trình duyệt.", category = "WEB")
    @Step("Gửi tổ hợp phím: {0}")
    public void pressKeys(String... keys) {
        execute(() -> {
            new Actions(DriverManager.getDriver()).sendKeys(keys).perform();
            return null;
        }, (Object[]) keys);
    }

    @NetatKeyword(name = "scrollToTop", description = "Cuộn lên đầu trang.", category = "WEB")
    @Step("Cuộn lên đầu trang")
    public void scrollToTop() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("window.scrollTo(0, 0)");
            return null;
        });
    }

    @NetatKeyword(name = "scrollToBottom", description = "Cuộn xuống cuối trang.", category = "WEB")
    @Step("Cuộn xuống cuối trang")
    public void scrollToBottom() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("window.scrollTo(0, document.body.scrollHeight)");
            return null;
        });
    }

    @NetatKeyword(name = "takeElementScreenshot", description = "Chụp ảnh của một phần tử cụ thể và lưu vào đường dẫn.", category = "WEB")
    @Step("Chụp ảnh phần tử {0.name} và lưu tại: {1}")
    public void takeElementScreenshot(ObjectUI uiObject, String filePath) {
        execute(() -> {
            try {
                File scrFile = findElement(uiObject).getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(scrFile, new File(filePath));
            } catch (IOException e) {
                throw new RuntimeException("Không thể chụp hoặc lưu ảnh phần tử.", e);
            }
            return null;
        }, uiObject, filePath);
    }

    @NetatKeyword(name = "openNewTab", description = "Mở một tab mới và chuyển sang tab đó.", category = "WEB")
    @Step("Mở tab mới với URL: {0}")
    public void openNewTab(String url) {
        execute(() -> {
            DriverManager.getDriver().switchTo().newWindow(WindowType.TAB);
            if (url != null && !url.isEmpty()) {
                DriverManager.getDriver().get(url);
            }
            return null;
        }, url);
    }

    @NetatKeyword(name = "switchToWindowByIndex", description = "Chuyển sang tab/cửa sổ theo chỉ số (index).", category = "WEB")
    @Step("Chuyển sang cửa sổ ở vị trí {0}")
    public void switchToWindowByIndex(int index) {
        execute(() -> {
            ArrayList<String> tabs = new ArrayList<>(DriverManager.getDriver().getWindowHandles());
            if (index < 0 || index >= tabs.size()) {
                throw new IndexOutOfBoundsException("Index cửa sổ không hợp lệ: " + index);
            }
            DriverManager.getDriver().switchTo().window(tabs.get(index));
            return null;
        }, index);
    }

// =================================================================================
// --- ĐỢI/ĐỒNG BỘ (WAIT/SYNC) ---
// =================================================================================

    @NetatKeyword(name = "waitForUrlContains", description = "Chờ cho đến khi URL hiện tại chứa một chuỗi con.", category = "WEB")
    @Step("Chờ URL chứa '{0}' trong {1} giây")
    public void waitForUrlContains(String partialUrl, int timeoutInSeconds) {
        execute(() -> {
            new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds))
                    .until(ExpectedConditions.urlContains(partialUrl));
            return null;
        }, partialUrl, timeoutInSeconds);
    }

    @NetatKeyword(name = "waitForTitleIs", description = "Chờ cho đến khi tiêu đề trang chính xác như mong đợi.", category = "WEB")
    @Step("Chờ tiêu đề trang là '{0}' trong {1} giây")
    public void waitForTitleIs(String expectedTitle, int timeoutInSeconds) {
        execute(() -> {
            new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds))
                    .until(ExpectedConditions.titleIs(expectedTitle));
            return null;
        }, expectedTitle, timeoutInSeconds);
    }

// =================================================================================
    // --- ASSERT / VERIFY (HARD/SOFT) ---
    // =================================================================================

    // --- Verify URL ---
    @NetatKeyword(name = "verifyUrlHard", description = "So sánh URL của trang. Dừng lại nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) URL của trang là '{0}'")
    public void verifyUrlHard(String expectedUrl) {
        execute(() -> {
            String actualUrl = DriverManager.getDriver().getCurrentUrl();
            Assert.assertEquals(actualUrl, expectedUrl, "HARD ASSERT FAILED: URL của trang không khớp.");
            return null;
        }, expectedUrl);
    }

    @NetatKeyword(name = "verifyUrlSoft", description = "So sánh URL của trang. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) URL của trang là '{0}'")
    public void verifyUrlSoft(String expectedUrl) {
        execute(() -> {
            String actualUrl = DriverManager.getDriver().getCurrentUrl();
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
            softAssert.assertEquals(actualUrl, expectedUrl, "SOFT ASSERT FAILED: URL của trang không khớp.");
            return null;
        }, expectedUrl);
    }

    // --- Verify Title ---
    @NetatKeyword(name = "verifyTitleSoft", description = "So sánh tiêu đề của trang. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) tiêu đề trang là '{0}'")
    public void verifyTitleSoft(String expectedTitle) {
        execute(() -> {
            String actualTitle = DriverManager.getDriver().getTitle();
            SoftAssert softAssert = ExecutionContext.getInstance().getSoftAssert();
            if (softAssert == null) {
                softAssert = new SoftAssert();
                ExecutionContext.getInstance().setSoftAssert(softAssert);
            }
            softAssert.assertEquals(actualTitle, expectedTitle, "SOFT ASSERT FAILED: Tiêu đề trang không khớp.");
            return null;
        }, expectedTitle);
    }

    // --- Verify Element Enabled/Disabled ---
    @NetatKeyword(name = "assertElementEnabled", description = "Khẳng định một phần tử đang ở trạng thái enabled. Dừng nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) phần tử {0.name} là enabled")
    public void assertElementEnabled(ObjectUI uiObject) {
        execute(() -> {
            performStateAssertion(uiObject, true, false);
            return null;
        }, uiObject);
    }

    @NetatKeyword(name = "assertElementDisabled", description = "Khẳng định một phần tử đang ở trạng thái disabled. Dừng nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) phần tử {0.name} là disabled")
    public void assertElementDisabled(ObjectUI uiObject) {
        execute(() -> {
            performStateAssertion(uiObject, false, false);
            return null;
        }, uiObject);
    }

    @NetatKeyword(name = "verifyElementEnabledSoft", description = "Kiểm tra một phần tử đang ở trạng thái enabled. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) phần tử {0.name} là enabled")
    public void verifyElementEnabledSoft(ObjectUI uiObject) {
        execute(() -> {
            performStateAssertion(uiObject, true, true);
            return null;
        }, uiObject);
    }

    @NetatKeyword(name = "verifyElementDisabledSoft", description = "Kiểm tra một phần tử đang ở trạng thái disabled. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) phần tử {0.name} là disabled")
    public void verifyElementDisabledSoft(ObjectUI uiObject) {
        execute(() -> {
            performStateAssertion(uiObject, false, true);
            return null;
        }, uiObject);
    }

    // --- Verify Text Matches Regex ---
    @NetatKeyword(name = "verifyTextMatchesRegexHard", description = "So khớp văn bản của phần tử với một mẫu regex. Dừng nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) văn bản của {0.name} khớp với regex '{1}'")
    public void verifyTextMatchesRegexHard(ObjectUI uiObject, String pattern) {
        execute(() -> {
            performRegexAssertion(uiObject, pattern, false);
            return null;
        }, uiObject, pattern);
    }

    @NetatKeyword(name = "verifyTextMatchesRegexSoft", description = "So khớp văn bản của phần tử với một mẫu regex. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) văn bản của {0.name} khớp với regex '{1}'")
    public void verifyTextMatchesRegexSoft(ObjectUI uiObject, String pattern) {
        execute(() -> {
            performRegexAssertion(uiObject, pattern, true);
            return null;
        }, uiObject, pattern);
    }

    // --- Verify Attribute Contains ---
    @NetatKeyword(name = "verifyAttributeContainsHard", description = "Kiểm tra thuộc tính của phần tử có chứa một chuỗi con. Dừng nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) thuộc tính '{1}' của {0.name} chứa '{2}'")
    public void verifyAttributeContainsHard(ObjectUI uiObject, String attribute, String partialValue) {
        execute(() -> {
            performAttributeContainsAssertion(uiObject, attribute, partialValue, false);
            return null;
        }, uiObject, attribute, partialValue);
    }

    @NetatKeyword(name = "verifyAttributeContainsSoft", description = "Kiểm tra thuộc tính của phần tử có chứa một chuỗi con. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) thuộc tính '{1}' của {0.name} chứa '{2}'")
    public void verifyAttributeContainsSoft(ObjectUI uiObject, String attribute, String partialValue) {
        execute(() -> {
            performAttributeContainsAssertion(uiObject, attribute, partialValue, true);
            return null;
        }, uiObject, attribute, partialValue);
    }

    // --- Verify CSS Value ---
    @NetatKeyword(name = "verifyCssValueHard", description = "So sánh giá trị một thuộc tính CSS. Dừng nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Hard) CSS '{1}' của {0.name} là '{2}'")
    public void verifyCssValueHard(ObjectUI uiObject, String cssName, String expectedValue) {
        execute(() -> {
            performCssValueAssertion(uiObject, cssName, expectedValue, false);
            return null;
        }, uiObject, cssName, expectedValue);
    }

    @NetatKeyword(name = "verifyCssValueSoft", description = "So sánh giá trị một thuộc tính CSS. Tiếp tục nếu thất bại.", category = "WEB")
    @Step("Kiểm tra (Soft) CSS '{1}' của {0.name} là '{2}'")
    public void verifyCssValueSoft(ObjectUI uiObject, String cssName, String expectedValue) {
        execute(() -> {
            performCssValueAssertion(uiObject, cssName, expectedValue, true);
            return null;
        }, uiObject, cssName, expectedValue);
    }

// =================================================================================
// --- COOKIES & STORAGE ---
// =================================================================================

    @NetatKeyword(name = "setLocalStorage", description = "Ghi một cặp key/value vào Local Storage của trình duyệt.", category = "WEB")
    @Step("Ghi vào Local Storage: key='{0}', value='{1}'")
    public void setLocalStorage(String key, String value) {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("localStorage.setItem(arguments[0], arguments[1]);", key, value);
            return null;
        }, key, value);
    }

    @NetatKeyword(name = "getLocalStorage", description = "Đọc một giá trị từ Local Storage bằng key.", category = "WEB")
    @Step("Đọc từ Local Storage với key='{0}'")
    public String getLocalStorage(String key) {
        return execute(() -> (String) ((JavascriptExecutor) DriverManager.getDriver()).executeScript("return localStorage.getItem(arguments[0]);", key), key);
    }

    @NetatKeyword(name = "clearLocalStorage", description = "Xóa toàn bộ dữ liệu trong Local Storage.", category = "WEB")
    @Step("Xóa toàn bộ Local Storage")
    public void clearLocalStorage() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("localStorage.clear();");
            return null;
        });
    }
}