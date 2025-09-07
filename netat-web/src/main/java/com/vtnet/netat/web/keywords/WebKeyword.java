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

    // =================================================================================
    // --- CORE LOGIC (FIND ELEMENT & AI SELF-HEALING) ---
    // =================================================================================

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
            String chatResponse = model.chat(prompt.replace("{element}", elementName).replace("{html}", html)).trim();
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

    // =================================================================================
    // --- 1. BROWSER & NAVIGATION KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "openUrl",
            description = "Điều hướng trình duyệt đến một địa chỉ web (URL) cụ thể.",
            category = "WEB",
            parameters = {"String: url - Địa chỉ trang web đầy đủ cần mở (ví dụ: 'https://www.google.com')."},
            example = "webKeyword.openUrl(\"https://www.google.com\");"
    )
    @Step("Mở URL: {0}")
    public void openUrl(String url) {
        DriverManager.getDriver().get(url);
    }

    @NetatKeyword(
            name = "goBack",
            description = "Thực hiện hành động quay lại trang trước đó trong lịch sử của trình duyệt, tương đương với việc người dùng nhấn nút 'Back'.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "webKeyword.goBack();"
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
            description = "Thực hiện hành động đi tới trang tiếp theo trong lịch sử của trình duyệt, tương đương với việc người dùng nhấn nút 'Forward'.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "webKeyword.goForward();"
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
            description = "Tải lại (làm mới) trang web hiện tại đang hiển thị trên trình duyệt. Tương đương với việc người dùng nhấn phím F5 hoặc nút 'Reload'.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "webKeyword.refresh();"
    )
    @Step("Tải lại trang")
    public void refresh() {
        execute(() -> {
            DriverManager.getDriver().navigate().refresh();
            return null;
        });
    }

    @NetatKeyword(
            name = "maximizeWindow",
            description = "Phóng to cửa sổ trình duyệt hiện tại ra kích thước lớn nhất có thể trên màn hình.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "webKeyword.maximizeWindow();"
    )
    @Step("Phóng to cửa sổ trình duyệt")
    public void maximizeWindow() {
        execute(() -> {
            DriverManager.getDriver().manage().window().maximize();
            return null;
        });
    }

    @NetatKeyword(
            name = "resizeWindow",
            description = "Thay đổi kích thước của cửa sổ trình duyệt hiện tại theo chiều rộng và chiều cao được chỉ định.",
            category = "WEB",
            parameters = {
                    "int: width - Chiều rộng mới của cửa sổ (pixel).",
                    "int: height - Chiều cao mới của cửa sổ (pixel)."
            },
            example = "webKeyword.resizeWindow(1280, 720);"
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
    // --- 2. BASIC ELEMENT INTERACTION ---
    // =================================================================================

    @Override
    @NetatKeyword(
            name = "click",
            description = "Thực hiện hành động click chuột vào một phần tử trên giao diện. " +
                    "Keyword sẽ tự động chờ cho đến khi phần tử sẵn sàng để được click.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Đối tượng giao diện (nút bấm, liên kết,...) cần thực hiện hành động click."},
            example = "webKeyword.click(loginButtonObject);"
    )
    @Step("Click vào phần tử: {0.name}")
    public void click(ObjectUI uiObject) {
        super.click(uiObject);
    }

    @Override
    @NetatKeyword(
            name = "sendKeys",
            description = "Nhập một chuỗi văn bản vào một phần tử (thường là ô input hoặc textarea). " +
                    "Keyword sẽ tự động xóa nội dung có sẵn trong ô trước khi nhập văn bản mới.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Ô input hoặc textarea cần nhập dữ liệu.",
                    "String: text - Chuỗi văn bản cần nhập vào phần tử."
            },
            example = "webKeyword.sendKeys(usernameInputObject, \"my_username\");"
    )
    @Step("Nhập văn bản '{1}' vào phần tử: {0.name}")
    public void sendKeys(ObjectUI uiObject, String text) {
        super.sendKeys(uiObject, text);
    }

    @NetatKeyword(
            name = "clearText",
            description = "Xóa toàn bộ văn bản đang có trong một phần tử có thể nhập liệu như ô input hoặc textarea.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử cần xóa nội dung."},
            example = "webKeyword.clearText(searchInputObject);"
    )
    @Step("Xóa văn bản trong phần tử: {0.name}")
    public void clearText(ObjectUI uiObject) {
        execute(() -> {
            findElement(uiObject).clear();
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "check",
            description = "Kiểm tra và đảm bảo một checkbox hoặc radio button đang ở trạng thái được chọn. Nếu phần tử chưa được chọn, keyword sẽ thực hiện click để chọn nó.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử checkbox hoặc radio button cần kiểm tra và chọn."},
            example = "webKeyword.check(termsAndConditionsCheckbox);"
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

    @NetatKeyword(
            name = "uncheck",
            description = "Kiểm tra và đảm bảo một checkbox đang ở trạng thái không được chọn. Nếu phần tử đang được chọn, keyword sẽ thực hiện click để bỏ chọn nó.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử checkbox cần bỏ chọn."},
            example = "webKeyword.uncheck(newsletterCheckbox);"
    )
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

    // =================================================================================
    // --- 3. ADVANCED ELEMENT INTERACTION ---
    // =================================================================================

    @NetatKeyword(
            name = "contextClick",
            description = "Thực hiện hành động click chuột phải vào một phần tử. Thường dùng để mở các menu ngữ cảnh (context menu).",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử cần thực hiện hành động click chuột phải."},
            example = "webKeyword.contextClick(fileIconObject);"
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
            description = "Thực hiện thao tác kéo một phần tử (nguồn) và thả nó vào vị trí của một phần tử khác (đích).",
            category = "WEB",
            parameters = {
                    "ObjectUI: sourceObject - Phần tử nguồn cần được kéo đi.",
                    "ObjectUI: targetObject - Phần tử đích, nơi phần tử nguồn sẽ được thả vào."
            },
            example = "webKeyword.dragAndDrop(draggableItemObject, dropZoneObject);"
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
            name = "dragAndDropByOffset",
            description = "Kéo một phần tử theo một khoảng cách (độ lệch x, y) so với vị trí hiện tại của nó. Rất hữu ích cho các thanh trượt (slider).",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kéo.",
                    "int: xOffset - Độ lệch theo trục ngang (pixel).",
                    "int: yOffset - Độ lệch theo trục dọc (pixel)."
            },
            example = "webKeyword.dragAndDropByOffset(priceSliderHandle, 100, 0); // Kéo sang phải 100px"
    )
    @Step("Kéo phần tử {0.name} theo độ lệch ({1}, {2})")
    public void dragAndDropByOffset(ObjectUI uiObject, int xOffset, int yOffset) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new Actions(DriverManager.getDriver()).dragAndDropBy(element, xOffset, yOffset).perform();
            return null;
        }, uiObject, xOffset, yOffset);
    }

    @NetatKeyword(
            name = "pressKeys",
            description = "Gửi một chuỗi ký tự hoặc một tổ hợp phím (ví dụ: Ctrl+C, Enter) tới phần tử đang được focus trên trình duyệt.",
            category = "WEB",
            parameters = {"String...: keys - Một hoặc nhiều chuỗi ký tự hoặc phím đặc biệt từ `org.openqa.selenium.Keys`."},
            example = "webKeyword.pressKeys(Keys.CONTROL, \"a\"); // Gửi tổ hợp phím Ctrl + A"
    )
    @Step("Gửi tổ hợp phím: {0}")
    public void pressKeys(String... keys) {
        execute(() -> {
            new Actions(DriverManager.getDriver()).sendKeys(keys).perform();
            return null;
        }, (Object[]) keys);
    }

    @NetatKeyword(
            name = "selectByIndex",
            description = "Chọn một tùy chọn (option) trong một phần tử dropdown (thẻ select) dựa trên chỉ số của nó (bắt đầu từ 0).",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử dropdown (thẻ select).",
                    "int: index - Chỉ số của tùy chọn cần chọn (ví dụ: 0 cho tùy chọn đầu tiên)."
            },
            example = "webKeyword.selectByIndex(countryDropdownObject, 1);"
    )
    @Step("Chọn tùy chọn ở vị trí {1} cho dropdown {0.name}")
    public void selectByIndex(ObjectUI uiObject, int index) {
        execute(() -> {
            Select select = new Select(findElement(uiObject));
            select.selectByIndex(index);
            return null;
        }, uiObject, index);
    }

    @NetatKeyword(
            name = "selectRadioByValue",
            description = "Chọn một radio button trong một nhóm các radio button dựa trên giá trị của thuộc tính 'value'.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Đại diện cho nhóm radio button (ví dụ locator chung là '//input[@name=\"gender\"]').",
                    "String: value - Giá trị trong thuộc tính 'value' của radio button cần chọn."
            },
            example = "webKeyword.selectRadioByValue(genderRadioGroup, \"female\");"
    )
    @Step("Chọn radio button có value '{1}' trong nhóm {0.name}")
    public void selectRadioByValue(ObjectUI uiObject, String value) {
        execute(() -> {
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

    // =================================================================================
    // --- 4. SCROLL & VIEWPORT KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "scrollToElement",
            description = "Cuộn trang đến khi phần tử được chỉ định nằm trong vùng có thể nhìn thấy của trình duyệt. " +
                    "Rất cần thiết khi cần tương tác với các phần tử ở cuối trang.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử đích cần cuộn đến."},
            example = "webKeyword.scrollToElement(footerSectionObject);"
    )
    @Step("Cuộn đến phần tử: {0.name}")
    public void scrollToElement(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            JavascriptExecutor jsExecutor = (JavascriptExecutor) DriverManager.getDriver();
            jsExecutor.executeScript("arguments[0].scrollIntoView(true);", element);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "scrollToCoordinates",
            description = "Cuộn trang web đến một tọa độ (x, y) cụ thể trong viewport.",
            category = "WEB",
            parameters = {
                    "int: x - Tọa độ theo trục hoành (pixel).",
                    "int: y - Tọa độ theo trục tung (pixel)."
            },
            example = "webKeyword.scrollToCoordinates(0, 500);"
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
            name = "scrollToTop",
            description = "Cuộn lên vị trí cao nhất (đầu trang) của trang web.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "webKeyword.scrollToTop();"
    )
    @Step("Cuộn lên đầu trang")
    public void scrollToTop() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("window.scrollTo(0, 0)");
            return null;
        });
    }

    @NetatKeyword(
            name = "scrollToBottom",
            description = "Cuộn xuống vị trí thấp nhất (cuối trang) của trang web.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "webKeyword.scrollToBottom();"
    )
    @Step("Cuộn xuống cuối trang")
    public void scrollToBottom() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("window.scrollTo(0, document.body.scrollHeight)");
            return null;
        });
    }

    // =================================================================================
    // --- 5. GETTER KEYWORDS (LẤY THÔNG TIN) ---
    // =================================================================================

    @Override
    @NetatKeyword(
            name = "getText",
            description = "Lấy và trả về văn bản của phần tử. Keyword này sẽ tự động thử nhiều cách: " +
                    "1. Lấy thuộc tính 'value' (cho ô input, textarea). " +
                    "2. Lấy văn bản hiển thị thông thường. " +
                    "3. Lấy 'textContent' hoặc 'innerText' nếu 2 cách trên thất bại.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử chứa văn bản cần lấy."},
            example = "String text = webKeyword.getText(usernameInputObject);"
    )
    @Step("Lấy văn bản từ phần tử: {0.name}")
    public String getText(ObjectUI uiObject) {
        return super.getText(uiObject);
    }

    @NetatKeyword(
            name = "getAttribute",
            description = "Lấy và trả về giá trị của một thuộc tính (attribute) cụ thể trên một phần tử HTML.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần lấy thuộc tính.",
                    "String: attributeName - Tên của thuộc tính cần lấy giá trị (ví dụ: 'href', 'class', 'value')."
            },
            example = "String linkUrl = webKeyword.getAttribute(linkObject, \"href\");"
    )
    @Step("Lấy thuộc tính '{1}' của phần tử {0.name}")
    public String getAttribute(ObjectUI uiObject, String attributeName) {
        return execute(() -> findElement(uiObject).getAttribute(attributeName), uiObject, attributeName);
    }

    @NetatKeyword(
            name = "getCssValue",
            description = "Lấy giá trị của một thuộc tính CSS được áp dụng trên một phần tử.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần lấy giá trị CSS.",
                    "String: cssPropertyName - Tên của thuộc tính CSS (ví dụ: 'color', 'font-size', 'background-color')."
            },
            example = "String elementColor = webKeyword.getCssValue(buttonObject, \"color\");"
    )
    @Step("Lấy giá trị CSS '{1}' của phần tử {0.name}")
    public String getCssValue(ObjectUI uiObject, String cssPropertyName) {
        return execute(() -> findElement(uiObject).getCssValue(cssPropertyName), uiObject, cssPropertyName);
    }

    @NetatKeyword(
            name = "getCurrentUrl",
            description = "Lấy và trả về URL đầy đủ của trang web hiện tại mà trình duyệt đang hiển thị.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "String pageUrl = webKeyword.getCurrentUrl();"
    )
    @Step("Lấy URL hiện tại")
    public String getCurrentUrl() {
        return execute(() -> DriverManager.getDriver().getCurrentUrl());
    }

    @NetatKeyword(
            name = "getPageTitle",
            description = "Lấy và trả về tiêu đề (title) của trang web hiện tại.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "String pageTitle = webKeyword.getPageTitle();"
    )
    @Step("Lấy tiêu đề trang")
    public String getPageTitle() {
        return execute(() -> DriverManager.getDriver().getTitle());
    }

    @NetatKeyword(
            name = "getElementCount",
            description = "Đếm và trả về số lượng phần tử trên trang khớp với locator được cung cấp. Hữu ích để kiểm tra số lượng kết quả tìm kiếm, số hàng trong bảng,...",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Đối tượng giao diện đại diện cho các phần tử cần đếm."},
            example = "int numberOfProducts = webKeyword.getElementCount(productListItemObject);"
    )
    @Step("Đếm số lượng phần tử của: {0.name}")
    public int getElementCount(ObjectUI uiObject) {
        return execute(() -> {
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return DriverManager.getDriver().findElements(by).size();
        }, uiObject);
    }

    // =================================================================================
    // --- 6. WAIT & SYNCHRONIZATION KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "waitForElementClickable",
            description = "Tạm dừng kịch bản cho đến khi một phần tử không chỉ hiển thị mà còn ở trạng thái sẵn sàng để được click (enabled).",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử cần chờ để sẵn sàng click."},
            example = "webKeyword.waitForElementClickable(submitButtonObject);"
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
            description = "Tạm dừng kịch bản cho đến khi một phần tử không còn hiển thị trên giao diện. Rất hữu ích để chờ các biểu tượng loading hoặc thông báo tạm thời biến mất.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử cần chờ cho đến khi nó biến mất."},
            example = "webKeyword.waitForElementNotVisible(loadingSpinnerObject);"
    )
    @Step("Chờ cho phần tử {0.name} biến mất")
    public void waitForElementNotVisible(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.invisibilityOf(element));
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "waitForElementPresent",
            description = "Tạm dừng kịch bản cho đến khi một phần tử tồn tại trong DOM của trang, không nhất thiết phải hiển thị. Hữu ích để chờ các phần tử được tạo ra bởi JavaScript.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần chờ cho đến khi nó tồn tại.",
                    "int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."
            },
            example = "webKeyword.waitForElementPresent(dynamicContentObject, 10);"
    )
    @Step("Chờ phần tử {0.name} tồn tại trong DOM trong {1} giây")
    public void waitForElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.presenceOfElementLocated(uiObject.getActiveLocators().get(0).convertToBy()));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForPageLoaded",
            description = "Tạm dừng kịch bản cho đến khi trang web tải xong hoàn toàn (trạng thái 'document.readyState' là 'complete').",
            category = "WEB",
            parameters = {"int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."},
            example = "webKeyword.waitForPageLoaded(30);"
    )
    @Step("Chờ trang tải xong trong {0} giây")
    public void waitForPageLoaded(int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete"));
            return null;
        }, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForUrlContains",
            description = "Tạm dừng kịch bản cho đến khi URL của trang hiện tại chứa một chuỗi con được chỉ định.",
            category = "WEB",
            parameters = {
                    "String: partialUrl - Chuỗi con mà URL cần chứa.",
                    "int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."
            },
            example = "webKeyword.waitForUrlContains(\"/dashboard\", 15);"
    )
    @Step("Chờ URL chứa '{0}' trong {1} giây")
    public void waitForUrlContains(String partialUrl, int timeoutInSeconds) {
        execute(() -> {
            new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds))
                    .until(ExpectedConditions.urlContains(partialUrl));
            return null;
        }, partialUrl, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForTitleIs",
            description = "Tạm dừng kịch bản cho đến khi tiêu đề của trang hiện tại khớp chính xác với chuỗi được chỉ định.",
            category = "WEB",
            parameters = {
                    "String: expectedTitle - Tiêu đề trang mong đợi.",
                    "int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."
            },
            example = "webKeyword.waitForTitleIs(\"Tải xuống hoàn tất\", 20);"
    )
    @Step("Chờ tiêu đề trang là '{0}' trong {1} giây")
    public void waitForTitleIs(String expectedTitle, int timeoutInSeconds) {
        execute(() -> {
            new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds))
                    .until(ExpectedConditions.titleIs(expectedTitle));
            return null;
        }, expectedTitle, timeoutInSeconds);
    }

    // =================================================================================
    // --- 7. VERIFICATION & ASSERTION KEYWORDS ---
    // =================================================================================

    // --- Visibility ---
    @NetatKeyword(
            name = "verifyElementVisibleHard",
            description = "Kiểm tra một phần tử có đang hiển thị trên giao diện hay không. Nếu kiểm tra thất bại (phần tử không hiển thị như mong đợi), kịch bản sẽ DỪNG LẠI ngay lập tức.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "boolean: isVisible - Trạng thái hiển thị mong đợi (true cho hiển thị, false cho bị ẩn)."
            },
            example = "webKeyword.verifyElementVisibleHard(errorMesssageObject, true);"
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} có hiển thị là {1}")
    public void verifyElementVisibleHard(ObjectUI uiObject, boolean isVisible) {
        performVisibilityAssertion(uiObject, isVisible, false);
    }

    @NetatKeyword(
            name = "verifyElementVisibleSoft",
            description = "Kiểm tra một phần tử có đang hiển thị trên giao diện hay không. Nếu kiểm tra thất bại, kịch bản sẽ ghi nhận lỗi nhưng vẫn TIẾP TỤC chạy các bước tiếp theo.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "boolean: isVisible - Trạng thái hiển thị mong đợi (true cho hiển thị, false cho bị ẩn)."
            },
            example = "webKeyword.verifyElementVisibleSoft(successMessageObject, true);"
    )
    @Step("Kiểm tra (Soft) phần tử {0.name} có hiển thị là {1}")
    public void verifyElementVisibleSoft(ObjectUI uiObject, boolean isVisible) {
        performVisibilityAssertion(uiObject, isVisible, true);
    }

    // --- Text ---
    @NetatKeyword(
            name = "verifyTextHard",
            description = "So sánh văn bản của một phần tử với một chuỗi ký tự mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: expectedText - Chuỗi văn bản mong đợi."
            },
            example = "webKeyword.verifyTextHard(pageTitleObject, \"Chào mừng đến với trang chủ\");"
    )
    @Step("Kiểm tra (Hard) văn bản của {0.name} là '{1}'")
    public void verifyTextHard(ObjectUI uiObject, String expectedText) {
        performTextAssertion(uiObject, expectedText, false);
    }

    @NetatKeyword(
            name = "verifyTextSoft",
            description = "So sánh văn bản của một phần tử với một chuỗi ký tự mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: expectedText - Chuỗi văn bản mong đợi."
            },
            example = "webKeyword.verifyTextSoft(usernameLabelObject, \"Tên đăng nhập\");"
    )
    @Step("Kiểm tra (Soft) văn bản của {0.name} là '{1}'")
    public void verifyTextSoft(ObjectUI uiObject, String expectedText) {
        performTextAssertion(uiObject, expectedText, true);
    }

    @NetatKeyword(
            name = "verifyTextContainsHard",
            description = "Kiểm tra văn bản của một phần tử có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: partialText - Chuỗi văn bản con mong đợi."
            },
            example = "webKeyword.verifyTextContainsHard(welcomeMessageObject, \"Xin chào\");"
    )
    @Step("Kiểm tra (Hard) văn bản của {0.name} có chứa '{1}'")
    public void verifyTextContainsHard(ObjectUI uiObject, String partialText) {
        performTextContainsAssertion(uiObject, partialText, false);
    }

    @NetatKeyword(
            name = "verifyTextContainsSoft",
            description = "Kiểm tra văn bản của một phần tử có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: partialText - Chuỗi văn bản con mong đợi."
            },
            example = "webKeyword.verifyTextContainsSoft(searchResultSummary, \"kết quả\");"
    )
    @Step("Kiểm tra (Soft) văn bản của {0.name} có chứa '{1}'")
    public void verifyTextContainsSoft(ObjectUI uiObject, String partialText) {
        performTextContainsAssertion(uiObject, partialText, true);
    }

    // --- Attribute ---
    @NetatKeyword(
            name = "verifyElementAttributeHard",
            description = "Kiểm tra giá trị của một thuộc tính (attribute) trên phần tử. Nếu giá trị không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: attributeName - Tên của thuộc tính (ví dụ: 'href', 'class', 'value').",
                    "String: expectedValue - Giá trị mong đợi của thuộc tính."
            },
            example = "webKeyword.verifyElementAttributeHard(linkObject, \"href\", \"/products/123\");"
    )
    @Step("Kiểm tra (Hard) thuộc tính '{1}' của {0.name} là '{2}'")
    public void verifyElementAttributeHard(ObjectUI uiObject, String attributeName, String expectedValue) {
        performAttributeAssertion(uiObject, attributeName, expectedValue, false);
    }

    @NetatKeyword(
            name = "verifyElementAttributeSoft",
            description = "Kiểm tra giá trị của một thuộc tính (attribute) trên phần tử. Nếu giá trị không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: attributeName - Tên của thuộc tính (ví dụ: 'href', 'class', 'value').",
                    "String: expectedValue - Giá trị mong đợi của thuộc tính."
            },
            example = "webKeyword.verifyElementAttributeSoft(imageObject, \"alt\", \"Mô tả hình ảnh\");"
    )
    @Step("Kiểm tra (Soft) thuộc tính '{1}' của {0.name} là '{2}'")
    public void verifyElementAttributeSoft(ObjectUI uiObject, String attributeName, String expectedValue) {
        performAttributeAssertion(uiObject, attributeName, expectedValue, true);
    }

    // --- Page URL & Title ---
    @NetatKeyword(
            name = "verifyUrlHard",
            description = "So sánh URL của trang hiện tại với một chuỗi mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {"String: expectedUrl - URL đầy đủ mong đợi."},
            example = "webKeyword.verifyUrlHard(\"https://example.com/login?status=success\");"
    )
    @Step("Kiểm tra (Hard) URL của trang là '{0}'")
    public void verifyUrlHard(String expectedUrl) {
        execute(() -> {
            String actualUrl = DriverManager.getDriver().getCurrentUrl();
            Assert.assertEquals(actualUrl, expectedUrl, "HARD ASSERT FAILED: URL của trang không khớp.");
            return null;
        }, expectedUrl);
    }

    @NetatKeyword(
            name = "verifyUrlSoft",
            description = "So sánh URL của trang hiện tại với một chuỗi mong đợi. Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {"String: expectedUrl - URL đầy đủ mong đợi."},
            example = "webKeyword.verifyUrlSoft(\"https://example.com/checkout/step1\");"
    )
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

    @NetatKeyword(
            name = "verifyTitleHard",
            description = "Kiểm tra tiêu đề (title) của trang web hiện tại. Nếu tiêu đề không khớp chính xác, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {"String: expectedTitle - Tiêu đề trang mong đợi."},
            example = "webKeyword.verifyTitleHard(\"Trang chủ - Website ABC\");"
    )
    @Step("Kiểm tra (Hard) tiêu đề trang là '{0}'")
    public void verifyTitleHard(String expectedTitle) {
        execute(() -> {
            String actualTitle = DriverManager.getDriver().getTitle();
            Assert.assertEquals(actualTitle, expectedTitle, "HARD ASSERT FAILED: Tiêu đề trang không khớp.");
            return null;
        }, expectedTitle);
    }

    @NetatKeyword(
            name = "verifyTitleSoft",
            description = "So sánh tiêu đề của trang hiện tại với một chuỗi mong đợi. Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {"String: expectedTitle - Tiêu đề trang mong đợi."},
            example = "webKeyword.verifyTitleSoft(\"Giỏ hàng (1 sản phẩm)\");"
    )
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

    // --- State & Selection ---
    @NetatKeyword(
            name = "assertElementEnabled",
            description = "Khẳng định rằng một phần tử đang ở trạng thái có thể tương tác (enabled). Nếu phần tử bị vô hiệu hóa (disabled), kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra."},
            example = "webKeyword.assertElementEnabled(submitButtonObject);"
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} là enabled")
    public void assertElementEnabled(ObjectUI uiObject) {
        execute(() -> {
            performStateAssertion(uiObject, true, false);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertElementDisabled",
            description = "Khẳng định rằng một phần tử đang ở trạng thái không thể tương tác (disabled). Nếu phần tử đang enabled, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra."},
            example = "webKeyword.assertElementDisabled(submitButtonBeforeFillForm);"
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} là disabled")
    public void assertElementDisabled(ObjectUI uiObject) {
        execute(() -> {
            performStateAssertion(uiObject, false, false);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "verifyElementEnabledSoft",
            description = "Kiểm tra một phần tử có đang ở trạng thái enabled hay không. Nếu không, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra."},
            example = "webKeyword.verifyElementEnabledSoft(optionalFieldObject);"
    )
    @Step("Kiểm tra (Soft) phần tử {0.name} là enabled")
    public void verifyElementEnabledSoft(ObjectUI uiObject) {
        execute(() -> {
            performStateAssertion(uiObject, true, true);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "verifyElementDisabledSoft",
            description = "Kiểm tra một phần tử có đang ở trạng thái disabled hay không. Nếu không, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra."},
            example = "webKeyword.verifyElementDisabledSoft(lockedFeatureButton);"
    )
    @Step("Kiểm tra (Soft) phần tử {0.name} là disabled")
    public void verifyElementDisabledSoft(ObjectUI uiObject) {
        execute(() -> {
            performStateAssertion(uiObject, false, true);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertElementSelected",
            description = "Khẳng định rằng một phần tử (checkbox hoặc radio button) đang ở trạng thái được chọn. Nếu không, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử checkbox hoặc radio button cần kiểm tra."},
            example = "webKeyword.assertElementSelected(rememberMeCheckbox);"
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} đã được chọn")
    public void assertElementSelected(ObjectUI uiObject) {
        execute(() -> {
            super.performSelectionAssertion(uiObject, true, false);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertElementNotSelected",
            description = "Khẳng định rằng một phần tử (checkbox hoặc radio button) đang ở trạng thái không được chọn. Nếu đang được chọn, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử checkbox hoặc radio button cần kiểm tra."},
            example = "webKeyword.assertElementNotSelected(newsletterCheckbox);"
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} chưa được chọn")
    public void assertElementNotSelected(ObjectUI uiObject) {
        execute(() -> {
            super.performSelectionAssertion(uiObject, false, false);
            return null;
        }, uiObject);
    }

    // --- Advanced Verification ---
    @NetatKeyword(
            name = "verifyTextMatchesRegexHard",
            description = "Kiểm tra văn bản của một phần tử có khớp với một biểu thức chính quy (regex) hay không. Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: pattern - Biểu thức chính quy để so khớp."
            },
            example = "webKeyword.verifyTextMatchesRegexHard(orderIdObject, \"^DH-\\d{5}$ \"); // Khớp với DH-12345"
    )
    @Step("Kiểm tra (Hard) văn bản của {0.name} khớp với regex '{1}'")
    public void verifyTextMatchesRegexHard(ObjectUI uiObject, String pattern) {
        execute(() -> {
            performRegexAssertion(uiObject, pattern, false);
            return null;
        }, uiObject, pattern);
    }

    @NetatKeyword(
            name = "verifyTextMatchesRegexSoft",
            description = "Kiểm tra văn bản của một phần tử có khớp với một biểu thức chính quy (regex) hay không. Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: pattern - Biểu thức chính quy để so khớp."
            },
            example = "webKeyword.verifyTextMatchesRegexSoft(emailFormatObject, \"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$\");"
    )
    @Step("Kiểm tra (Soft) văn bản của {0.name} khớp với regex '{1}'")
    public void verifyTextMatchesRegexSoft(ObjectUI uiObject, String pattern) {
        execute(() -> {
            performRegexAssertion(uiObject, pattern, true);
            return null;
        }, uiObject, pattern);
    }

    @NetatKeyword(
            name = "verifyAttributeContainsHard",
            description = "Kiểm tra giá trị của một thuộc tính trên phần tử có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: attribute - Tên của thuộc tính (ví dụ: 'class').",
                    "String: partialValue - Chuỗi con mong đợi."
            },
            example = "webKeyword.verifyAttributeContainsHard(elementObject, \"class\", \"active\");"
    )
    @Step("Kiểm tra (Hard) thuộc tính '{1}' của {0.name} chứa '{2}'")
    public void verifyAttributeContainsHard(ObjectUI uiObject, String attribute, String partialValue) {
        execute(() -> {
            performAttributeContainsAssertion(uiObject, attribute, partialValue, false);
            return null;
        }, uiObject, attribute, partialValue);
    }

    @NetatKeyword(
            name = "verifyAttributeContainsSoft",
            description = "Kiểm tra giá trị của một thuộc tính trên phần tử có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: attribute - Tên của thuộc tính (ví dụ: 'class').",
                    "String: partialValue - Chuỗi con mong đợi."
            },
            example = "webKeyword.verifyAttributeContainsSoft(elementObject, \"style\", \"display: block\");"
    )
    @Step("Kiểm tra (Soft) thuộc tính '{1}' của {0.name} chứa '{2}'")
    public void verifyAttributeContainsSoft(ObjectUI uiObject, String attribute, String partialValue) {
        execute(() -> {
            performAttributeContainsAssertion(uiObject, attribute, partialValue, true);
            return null;
        }, uiObject, attribute, partialValue);
    }

    @NetatKeyword(
            name = "verifyCssValueHard",
            description = "So sánh giá trị của một thuộc tính CSS trên phần tử. Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: cssName - Tên thuộc tính CSS (ví dụ: 'color').",
                    "String: expectedValue - Giá trị CSS mong đợi (ví dụ: 'rgb(255, 0, 0)')."
            },
            example = "webKeyword.verifyCssValueHard(errorMessageObject, \"color\", \"rgba(255, 0, 0, 1)\");"
    )
    @Step("Kiểm tra (Hard) CSS '{1}' của {0.name} là '{2}'")
    public void verifyCssValueHard(ObjectUI uiObject, String cssName, String expectedValue) {
        execute(() -> {
            performCssValueAssertion(uiObject, cssName, expectedValue, false);
            return null;
        }, uiObject, cssName, expectedValue);
    }

    @NetatKeyword(
            name = "verifyCssValueSoft",
            description = "So sánh giá trị của một thuộc tính CSS trên phần tử. Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: cssName - Tên thuộc tính CSS (ví dụ: 'font-weight').",
                    "String: expectedValue - Giá trị CSS mong đợi (ví dụ: '700')."
            },
            example = "webKeyword.verifyCssValueSoft(titleObject, \"font-weight\", \"700\");"
    )
    @Step("Kiểm tra (Soft) CSS '{1}' của {0.name} là '{2}'")
    public void verifyCssValueSoft(ObjectUI uiObject, String cssName, String expectedValue) {
        execute(() -> {
            performCssValueAssertion(uiObject, cssName, expectedValue, true);
            return null;
        }, uiObject, cssName, expectedValue);
    }

    // =================================================================================
    // --- 8. WINDOW, TAB & FRAME MANAGEMENT ---
    // =================================================================================

    @NetatKeyword(
            name = "switchToWindowByTitle",
            description = "Duyệt qua tất cả các cửa sổ hoặc tab đang mở và chuyển sự điều khiển của WebDriver sang cửa sổ/tab có tiêu đề khớp chính xác với chuỗi được cung cấp.",
            category = "WEB",
            parameters = {"String: windowTitle - Tiêu đề chính xác của cửa sổ hoặc tab cần chuyển đến."},
            example = "webKeyword.switchToWindowByTitle(\"Sản phẩm ABC\");"
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
                        return null;
                    }
                }
            }
            DriverManager.getDriver().switchTo().window(currentWindow);
            throw new NoSuchWindowException("Không tìm thấy cửa sổ nào có tiêu đề: " + windowTitle);
        }, windowTitle);
    }

    @NetatKeyword(
            name = "switchToWindowByIndex",
            description = "Chuyển sự điều khiển của WebDriver sang một tab hoặc cửa sổ khác dựa trên chỉ số (index) của nó (bắt đầu từ 0).",
            category = "WEB",
            parameters = {"int: index - Chỉ số của cửa sổ/tab cần chuyển đến (0 là cửa sổ đầu tiên)."},
            example = "webKeyword.switchToWindowByIndex(1); // Chuyển sang tab thứ hai"
    )
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

    @NetatKeyword(
            name = "switchToFrame",
            description = "Chuyển sự điều khiển của WebDriver vào một phần tử iframe trên trang. Mọi hành động sau đó sẽ được thực hiện trong ngữ cảnh của iframe này.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Đối tượng giao diện đại diện cho thẻ iframe cần chuyển vào."},
            example = "webKeyword.switchToFrame(paymentIframeObject);"
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
            description = "Thoát khỏi ngữ cảnh iframe hiện tại và quay về iframe cha ngay trước nó. Nếu đang ở iframe cấp cao nhất, hành động này sẽ quay về nội dung chính của trang.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "webKeyword.switchToParentFrame();"
    )
    @Step("Quay về iframe cha")
    public void switchToParentFrame() {
        execute(() -> {
            DriverManager.getDriver().switchTo().parentFrame();
            return null;
        });
    }

    @NetatKeyword(
            name = "switchToDefaultContent",
            description = "Chuyển sự điều khiển của WebDriver ra khỏi tất cả các iframe và quay về nội dung chính, cấp cao nhất của trang web.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "webKeyword.switchToDefaultContent();"
    )
    @Step("Chuyển về nội dung trang chính")
    public void switchToDefaultContent() {
        execute(() -> {
            DriverManager.getDriver().switchTo().defaultContent();
            return null;
        });
    }

    @NetatKeyword(
            name = "openNewTab",
            description = "Mở một tab mới trong trình duyệt và tự động chuyển sự điều khiển sang tab mới đó. Có thể tùy chọn mở một URL cụ thể trong tab mới.",
            category = "WEB",
            parameters = {"String: url - (Tùy chọn) URL để mở trong tab mới. Nếu để trống, sẽ mở tab trống."},
            example = "webKeyword.openNewTab(\"https://google.com\");"
    )
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

    // =================================================================================
    // --- 9. ALERT & POPUP HANDLING ---
    // =================================================================================

    @NetatKeyword(
            name = "getAlertText",
            description = "Chờ cho đến khi một hộp thoại alert, prompt, hoặc confirm của trình duyệt xuất hiện và lấy về nội dung văn bản của nó.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "String alertMessage = webKeyword.getAlertText();"
    )
    @Step("Lấy văn bản từ alert")
    public String getAlertText() {
        return execute(() -> {
            Alert alert = new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.alertIsPresent());
            return alert.getText();
        });
    }

    @NetatKeyword(
            name = "sendKeysToAlert",
            description = "Chờ cho đến khi một hộp thoại prompt của trình duyệt xuất hiện và nhập một chuỗi văn bản vào đó.",
            category = "WEB",
            parameters = {"String: text - Chuỗi văn bản cần nhập vào hộp thoại."},
            example = "webKeyword.sendKeysToAlert(\"Tên của tôi\");"
    )
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
    // --- 10. SHADOW DOM KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "findElementInShadowDom",
            description = "Tìm kiếm và trả về một phần tử nằm bên trong một Shadow DOM. Yêu cầu cung cấp phần tử chủ (shadow host) và một CSS selector để định vị phần tử con.",
            category = "WEB",
            parameters = {
                    "ObjectUI: shadowHostObject - Phần tử chủ (host) chứa Shadow DOM.",
                    "String: cssSelectorInShadow - Chuỗi CSS selector để tìm phần tử bên trong Shadow DOM."
            },
            example = "WebElement usernameInput = webKeyword.findElementInShadowDom(appContainerObject, \"#username\");"
    )
    @Step("Tìm phần tử '{1}' bên trong Shadow DOM của {0.name}")
    public WebElement findElementInShadowDom(ObjectUI shadowHostObject, String cssSelectorInShadow) {
        return execute(() -> {
            WebElement shadowHost = findElement(shadowHostObject);
            SearchContext shadowRoot = shadowHost.getShadowRoot();
            return shadowRoot.findElement(By.cssSelector(cssSelectorInShadow));
        }, shadowHostObject, cssSelectorInShadow);
    }

    // =================================================================================
    // --- 11. COOKIES & STORAGE KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "setLocalStorage",
            description = "Ghi một cặp khóa-giá trị vào Local Storage của trình duyệt. Hữu ích để thiết lập trạng thái ứng dụng hoặc token.",
            category = "WEB",
            parameters = {
                    "String: key - Khóa (key) để lưu trữ.",
                    "String: value - Giá trị (value) tương ứng."
            },
            example = "webKeyword.setLocalStorage(\"user_token\", \"abcdef123456\");"
    )
    @Step("Ghi vào Local Storage: key='{0}', value='{1}'")
    public void setLocalStorage(String key, String value) {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("localStorage.setItem(arguments[0], arguments[1]);", key, value);
            return null;
        }, key, value);
    }

    @NetatKeyword(
            name = "getLocalStorage",
            description = "Đọc và trả về giá trị từ Local Storage của trình duyệt dựa trên một khóa (key) được cung cấp.",
            category = "WEB",
            parameters = {"String: key - Khóa (key) của giá trị cần đọc."},
            example = "String userToken = webKeyword.getLocalStorage(\"user_token\");"
    )
    @Step("Đọc từ Local Storage với key='{0}'")
    public String getLocalStorage(String key) {
        return execute(() -> (String) ((JavascriptExecutor) DriverManager.getDriver()).executeScript("return localStorage.getItem(arguments[0]);", key), key);
    }

    @NetatKeyword(
            name = "clearLocalStorage",
            description = "Xóa toàn bộ dữ liệu đang được lưu trữ trong Local Storage của trang web hiện tại.",
            category = "WEB",
            parameters = {"Không có tham số."},
            example = "webKeyword.clearLocalStorage();"
    )
    @Step("Xóa toàn bộ Local Storage")
    public void clearLocalStorage() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("localStorage.clear();");
            return null;
        });
    }

    // =================================================================================
    // --- 12. UTILITY & SUPPORT KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "takeScreenshot",
            description = "Chụp lại ảnh toàn bộ màn hình (viewport) của trình duyệt và lưu vào một file tại đường dẫn được chỉ định.",
            category = "WEB",
            parameters = {"String: filePath - Đường dẫn đầy đủ để lưu file ảnh (ví dụ: 'C:/screenshots/error.png')."},
            example = "webKeyword.takeScreenshot(\"D:/test-reports/screenshots/homepage.png\");"
    )
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

    @NetatKeyword(
            name = "takeElementScreenshot",
            description = "Chụp ảnh chỉ riêng một phần tử cụ thể trên trang và lưu vào file tại đường dẫn được chỉ định.",
            category = "WEB",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần chụp ảnh.",
                    "String: filePath - Đường dẫn đầy đủ để lưu file ảnh."
            },
            example = "webKeyword.takeElementScreenshot(loginFormObject, \"D:/screenshots/login_form.png\");"
    )
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

    @NetatKeyword(
            name = "highlightElement",
            description = "Tạm thời vẽ một đường viền màu đỏ xung quanh một phần tử trên trang để dễ dàng nhận biết và gỡ lỗi trong quá trình chạy kịch bản.",
            category = "WEB",
            parameters = {"ObjectUI: uiObject - Phần tử cần làm nổi bật."},
            example = "webKeyword.highlightElement(loginButtonObject);"
    )
    @Step("Làm nổi bật phần tử: {0.name}")
    public void highlightElement(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
            js.executeScript("arguments[0].style.border='3px solid red'", element);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignored
            }
            js.executeScript("arguments[0].style.border=''", element);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "pause",
            description = "Tạm dừng việc thực thi kịch bản trong một khoảng thời gian tĩnh. (Lưu ý: Chỉ nên dùng khi thực sự cần thiết, ưu tiên các keyword chờ động).",
            category = "WEB",
            parameters = {"int: milliseconds - Thời gian cần tạm dừng (tính bằng mili giây)."},
            example = "webKeyword.pause(3000); // Dừng 3 giây"
    )
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
}