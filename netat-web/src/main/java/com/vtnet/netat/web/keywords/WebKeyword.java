package com.vtnet.netat.web.keywords;
import com.vtnet.netat.core.BaseUiKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.core.ui.ObjectUI;
import com.vtnet.netat.core.utils.SecureText;
import com.vtnet.netat.driver.ConfigReader;
import com.vtnet.netat.driver.DriverManager;
import com.vtnet.netat.driver.SessionManager;
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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            // 1. Thử tìm bằng logic chung of lớp cha trước (duyệt qua các locator đã định nghĩa)
            return super.findElement(uiObject);
        } catch (NoSuchElementException e) {
            logger.warn("Failed with all defined locators. Switching to AI-based self-healing search.");

            // 2. Thử với cơ chế Self-Healing bằng AI
            try {
                String aiLocatorValue = getLocatorByAI(uiObject.getName() + " :[description: " + uiObject.getDescription() + "] ", DriverManager.getDriver().getPageSource());
                if (aiLocatorValue != null && !aiLocatorValue.isEmpty()) {
                    logger.info("AI suggested new locator (CSS): '{}'", aiLocatorValue);
                    WebDriverWait aiWait = new WebDriverWait(DriverManager.getDriver(), SECONDARY_TIMEOUT);
                    return aiWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(aiLocatorValue)));
                }
            } catch (Exception aiException) {
                logger.error("Search with AI-suggested locator also failed.", aiException);
            }

            // 3. Nếu tất cả đều thất bại
            throw new NoSuchElementException("Cannot find element '" + uiObject.getName() + "' using any available method.");
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
            logger.error("An error occurred while communicating with the AI model.", e);
        }
        return "";
    }

    @NetatKeyword(
            name = "findElements",
            description = "Tìm và trả về một danh sách (List) tất cả các element WebElement khớp với locator được cung cấp. " +
                    "Trả về danh sách rỗng nếu không tìm thấy, không ném ra exception.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện đại diện cho các element cần tìm"
            },
            returnValue = "List<WebElement> - Danh sách các element web khớp với locator, hoặc danh sách rỗng nếu không tìm thấy element nào",
            example = "// Lấy danh sách tất cả các sản phẩm\n" +
                    "List<WebElement> productList = webKeyword.findElements(productListItemObject);\n\n" +
                    "// Đếm số lượng kết quả tìm kiếm\n" +
                    "int resultCount = webKeyword.findElements(searchResultObject).size();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và đối tượng ObjectUI phải có ít nhất một locator được định nghĩa. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "InvalidSelectorException nếu locator không hợp lệ, " +
                    "hoặc NullPointerException nếu uiObject is null hoặc không có locator nào được kích hoạt."
    )
    @Step("Find elements: {0.name}")
    public List<WebElement> findElements(ObjectUI uiObject) {
        return execute(() -> {
            // Sử dụng locator đầu tiên được kích hoạt để tìm kiếm
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return DriverManager.getDriver().findElements(by);
        }, uiObject);
    }

    @NetatKeyword(
            name = "openUrl",
            description = "Điều hướng trình duyệt đến một địa chỉ web (URL) cụ thể.",
            category = "Web",
            subCategory = "Browser",
            parameters = {
                    "url: String - Địa chỉ trang web đầy đủ cần mở (ví dụ: 'https://www.google.com')"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Mở trang chủ Google\n" +
                    "webKeyword.openUrl(\"https://www.google.com\");\n\n" +
                    "// Mở trang đăng nhập\n" +
                    "webKeyword.openUrl(\"https://example.com/login\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "URL phải is một địa chỉ hợp lệ và có thể truy cập được, và kết nối mạng phải hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "InvalidArgumentException nếu URL không hợp lệ, " +
                    "hoặc TimeoutException nếu trang không tải thành công trong thời gian chờ mặc định."
    )
    @Step("Open URL: {0}")
    public void openUrl(String url) {
        DriverManager.getDriver().get(url);
    }

    @NetatKeyword(
            name = "goBack",
            description = "Thực hiện hành động quay lại trang trước đó trong lịch sử of trình duyệt, " +
                    "tương đương với việc người dùng nhấn nút 'Back'.",
            category = "Web",
            subCategory = "Browser",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Quay lại trang trước sau khi đã điều hướng\n" +
                    "webKeyword.openUrl(\"https://example.com/page1\");\n" +
                    "webKeyword.openUrl(\"https://example.com/page2\");\n" +
                    "webKeyword.goBack(); // Quay lại page1\n\n" +
                    "// Quay lại sau khi nhấp vào liên kết\n" +
                    "webKeyword.click(linkToDetailsPage);\n" +
                    "webKeyword.goBack();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phải có ít nhất một trang đã được truy cập trước đó trong lịch sử of phiên hiện tại. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
    )
    @Step("Go back to previous page")
    public void goBack() {
        execute(() -> {
            DriverManager.getDriver().navigate().back();
            return null;
        });
    }

    @NetatKeyword(
            name = "goForward",
            description = "Thực hiện hành động đi tới trang tiếp theo trong lịch sử of trình duyệt, " +
                    "tương đương với việc người dùng nhấn nút 'Forward'.",
            category = "Web",
            subCategory = "Browser",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Điều hướng qua lại giữa các trang\n" +
                    "webKeyword.openUrl(\"https://example.com/page1\");\n" +
                    "webKeyword.openUrl(\"https://example.com/page2\");\n" +
                    "webKeyword.goBack(); // Quay lại page1\n" +
                    "webKeyword.goForward(); // Tiến tới page2 lần nữa\n\n" +
                    "// Verify luồng điều hướng\n" +
                    "webKeyword.click(productLink);\n" +
                    "webKeyword.goBack();\n" +
                    "webKeyword.goForward();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phải đã sử dụng goBack() hoặc có trang tiếp theo trong lịch sử điều hướng. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
    )
    @Step("Go forward to next page")
    public void goForward() {
        execute(() -> {
            DriverManager.getDriver().navigate().forward();
            return null;
        });
    }

    @NetatKeyword(
            name = "refresh",
            description = "Tải lại (làm mới) trang web hiện tại đang hiển thị trên trình duyệt. " +
                    "Tương đương với việc người dùng nhấn phím F5 hoặc nút 'Reload'.",
            category = "Web",
            subCategory = "Browser",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Làm mới trang hiện tại\n" +
                    "webKeyword.refresh();\n\n" +
                    "// Làm mới trang sau khi gửi biểu mẫu\n" +
                    "webKeyword.click(submitButton);\n" +
                    "webKeyword.refresh();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và đã tải một trang web trước đó. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "NoSuchSessionException nếu phiên WebDriver không còn hợp lệ, " +
                    "hoặc TimeoutException nếu trang không tải lại trong thời gian chờ mặc định."
    )
    @Step("Refresh page")
    public void refresh() {
        execute(() -> {
            DriverManager.getDriver().navigate().refresh();
            return null;
        });
    }

    @NetatKeyword(
            name = "maximizeWindow",
            description = "Phóng to cửa sổ trình duyệt hiện tại ra kích thước lớn nhất có thể trên màn hình.",
            category = "Web",
            subCategory = "Browser",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Phóng to cửa sổ trình duyệt khi bắt đầu kiểm thử\n" +
                    "webKeyword.openUrl(\"https://example.com\");\n" +
                    "webKeyword.maximizeWindow();\n\n" +
                    "// Phóng to cửa sổ trước khi chụp ảnh màn hình\n" +
                    "webKeyword.maximizeWindow();\n" +
                    "webKeyword.takeScreenshot(\"full_page\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trình duyệt phải hỗ trợ thay đổi kích thước cửa sổ. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "NoSuchSessionException nếu phiên WebDriver không còn hợp lệ, " +
                    "hoặc UnsupportedOperationException nếu trình duyệt không hỗ trợ thay đổi kích thước."
    )
    @Step("Maximize browser window")
    public void maximizeWindow() {
        execute(() -> {
            DriverManager.getDriver().manage().window().maximize();
            return null;
        });
    }

    @NetatKeyword(
            name = "resizeWindow",
            description = "Thay đổi kích thước of cửa sổ trình duyệt hiện tại theo chiều rộng và chiều cao được chỉ định.",
            category = "Web",
            subCategory = "Browser",
            parameters = {
                    "width: int - Chiều rộng mới of cửa sổ (pixel)",
                    "height: int - Chiều cao mới of cửa sổ (pixel)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Thay đổi kích thước cửa sổ thành HD (720p)\n" +
                    "webKeyword.resizeWindow(1280, 720);\n\n" +
                    "// Mô phỏng kích thước màn hình tablet\n" +
                    "webKeyword.resizeWindow(768, 1024);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trình duyệt phải hỗ trợ thay đổi kích thước cửa sổ, " +
                    "và kích thước yêu cầu phải nằm trong giới hạn hợp lý (lớn hơn 0 và nhỏ hơn kích thước màn hình vật lý). " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "NoSuchSessionException nếu phiên WebDriver không còn hợp lệ, " +
                    "UnsupportedOperationException nếu trình duyệt không hỗ trợ thay đổi kích thước, " +
                    "hoặc IllegalArgumentException nếu chiều rộng hoặc chiều cao is số âm."
    )
    @Step("Resize window to {0}x{1}")
    public void resizeWindow(int width, int height) {
        execute(() -> {
            Dimension dimension = new Dimension(width, height);
            DriverManager.getDriver().manage().window().setSize(dimension);
            return null;
        }, width, height);
    }


    @Override
    @NetatKeyword(
            name = "click",
            description = "Thực hiện hành động click chuột vào một element trên giao diện. " +
                    "Keyword sẽ tự động chờ cho đến khi element sẵn sàng để được click.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện (nút bấm, liên kết,...) cần thực hiện hành động click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click vào nút đăng nhập\n" +
                    "webKeyword.click(loginButtonObject);\n\n" +
                    "// Click vào liên kết\n" +
                    "webKeyword.click(registerLinkObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần click phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và không bị che khuất bởi các element khác. " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementClickInterceptedException nếu element bị che khuất bởi element khác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Click element: {0.name}")
    public void click(ObjectUI uiObject) {
        super.click(uiObject);
    }

    @Override
    @NetatKeyword(
            name = "sendKeys",
            description = "Nhập một chuỗi text vào một element (thường is ô input hoặc textarea). " +
                    "Keyword sẽ tự động xóa nội dung có sẵn trong ô trước khi Enter text mới.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Ô input hoặc textarea cần nhập dữ liệu",
                    "text: String - Chuỗi text cần nhập vào element"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Nhập tên đăng nhập\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"my_username\");\n\n" +
                    "// Nhập nội dung tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop gaming\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần nhập liệu phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và phải is loại có thể nhập liệu (input, textarea, contenteditable). " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Enter text '{1}' into element: {0.name}")
    public void sendKeys(ObjectUI uiObject, String text) {
        super.sendKeys(uiObject, text);
    }

    @NetatKeyword(
            name = "clearText",
            description = "Xóa toàn bộ text đang có trong một element có thể nhập liệu như ô input hoặc textarea.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element cần xóa nội dung"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Xóa nội dung trong ô tìm kiếm\n" +
                    "webKeyword.clearText(searchInputObject);\n\n" +
                    "// Xóa nội dung trước khi nhập dữ liệu mới\n" +
                    "webKeyword.clearText(usernameInputObject);\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"new_username\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần xóa nội dung phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và phải is loại có thể nhập liệu (input, textarea, contenteditable). " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Clear text in element: {0.name}")
    public void clearText(ObjectUI uiObject) {
        execute(() -> {
            findElement(uiObject).clear();
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "check",
            description = "Verify và đảm bảo một checkbox hoặc radio button đang ở trạng thái được chọn. " +
                    "Nếu element is not selected, keyword sẽ thực hiện click để chọn nó.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element checkbox hoặc radio button cần Verify và chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Đảm bảo checkbox Điều khoản và Điều kiện is selected\n" +
                    "webKeyword.check(termsAndConditionsCheckbox);\n\n" +
                    "// Chọn phương thức thanh toán\n" +
                    "webKeyword.check(creditCardRadioButton);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần chọn phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và phải is checkbox hoặc radio button (input type=\"checkbox\" hoặc type=\"radio\"). " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Ensure element {0.name} is selected")
    public void check(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            if (!"true".equalsIgnoreCase(element.getDomAttribute("checked"))) {
                element.click();
            }
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "uncheck",
            description = "Verify và đảm bảo một checkbox đang ở trạng thái không được chọn. " +
                    "Nếu element đang được chọn, keyword sẽ thực hiện click để bỏ chọn nó.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element checkbox cần bỏ chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Bỏ chọn checkbox đăng ký nhận bản tin\n" +
                    "webKeyword.uncheck(newsletterCheckbox);\n\n" +
                    "// Bỏ chọn option gửi hàng nhanh\n" +
                    "webKeyword.uncheck(expressShippingCheckbox);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần bỏ chọn phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "phải is checkbox (input type=\"checkbox\"), và lưu ý phương thức này chỉ hoạt động với checkbox, không dùng cho radio button. " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Uncheck checkbox: {0.name}")
    public void uncheck(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            if ("true".equalsIgnoreCase(element.getDomAttribute("checked"))) {
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
            description = "Thực hiện hành động click chuột phải vào một element. Thường dùng để mở các menu ngữ cảnh (context menu).",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element cần thực hiện hành động click chuột phải"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click chuột phải vào biểu tượng file\n" +
                    "webKeyword.contextClick(fileIconObject);\n" +
                    "webKeyword.waitForElementVisible(contextMenuObject);\n\n" +
                    "// Click chuột phải vào hình ảnh để tải xuống\n" +
                    "webKeyword.contextClick(productImageObject);\n" +
                    "webKeyword.click(saveImageOptionObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần click phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "không bị che khuất bởi các element khác, và trình duyệt phải hỗ trợ thao tác chuột phải " +
                    "(một số trình duyệt di động có thể không hỗ trợ). " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu element nằm ngoài viewport hiện tại."
    )
    @Step("Right-click element: {0.name}")
    public void contextClick(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            Actions actions = new Actions(DriverManager.getDriver());
            actions.contextClick(element).perform();
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "doubleClick",
            description = "Thực hiện hành động click chuột hai lần (double-click) vào một element.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element cần thực hiện double-click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Double-click vào biểu tượng chỉnh sửa\n" +
                    "webKeyword.doubleClick(editIconObject);\n" +
                    "webKeyword.waitForElementVisible(editFormObject);\n\n" +
                    "// Double-click để chọn toàn bộ text\n" +
                    "webKeyword.doubleClick(textParagraphObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần double-click phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và không bị che khuất bởi các element khác. " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu element nằm ngoài viewport hiện tại."
    )
    @Step("Double-click element: {0.name}")
    public void doubleClick(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new Actions(DriverManager.getDriver()).doubleClick(element).perform();
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "hover",
            description = "Di chuyển con trỏ chuột đến vị trí of một element để hiển thị các menu con, tooltip, hoặc các hiệu ứng khác.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element cần di chuột đến"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Di chuột đến menu chính để hiển thị menu con\n" +
                    "webKeyword.hover(mainMenuObject);\n" +
                    "webKeyword.waitForElementVisible(subMenuObject);\n\n" +
                    "// Di chuột đến biểu tượng để hiển thị tooltip\n" +
                    "webKeyword.hover(infoIconObject);\n" +
                    "webKeyword.waitForElementVisible(tooltipObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần hover phải tồn tại trong DOM, phải hiển thị trên trang, " +
                    "và trình duyệt phải hỗ trợ thao tác di chuột (một số trình duyệt di động có thể không hỗ trợ). " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu element nằm ngoài viewport hiện tại."
    )
    @Step("Hover over element: {0.name}")
    public void hover(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new Actions(DriverManager.getDriver()).moveToElement(element).perform();
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "uploadFile",
            description = "Tải lên một file từ máy local bằng cách gửi đường dẫn file vào một element <input type='file'>.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element input (type='file') để tải file lên",
                    "filePath: String - Đường dẫn tuyệt đối đến file cần tải lên trên máy"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Tải lên ảnh đại diện\n" +
                    "webKeyword.uploadFile(avatarUploadInput, \"C:/Users/Tester/Pictures/avatar.jpg\");\n\n" +
                    "// Tải lên tài liệu PDF\n" +
                    "webKeyword.uploadFile(documentUploadInput, \"D:/Documents/report.pdf\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element input phải có thuộc tính type='file', phải tồn tại trong DOM (có thể ẩn nhưng phải tồn tại), " +
                    "file cần tải lên phải tồn tại tại đường dẫn được chỉ định, " +
                    "người dùng thực thi test phải có quyền truy cập vào file, và đường dẫn file phải is đường dẫn tuyệt đối. " +
                    "Có thể throw InvalidArgumentException nếu đường dẫn file không hợp lệ hoặc file không tồn tại, " +
                    "ElementNotInteractableException nếu element không phải is input type='file', " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Upload file '{1}' to element {0.name}")
    public void uploadFile(ObjectUI uiObject, String filePath) {
        execute(() -> {
            // Với input type="file", không cần click, chỉ cần sendKeys đường dẫn file
            WebElement element = findElement(uiObject);
            element.sendKeys(filePath);
            return null;
        }, uiObject, filePath);
    }

    @NetatKeyword(
            name = "dragAndDrop",
            description = "Thực hiện thao tác kéo một element (nguồn) và thả nó vào vị trí of một element khác (đích).",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "sourceObject: ObjectUI - element nguồn cần được kéo đi",
                    "targetObject: ObjectUI - element đích, nơi element nguồn sẽ được thả vào"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kéo và thả một mục vào giỏ hàng\n" +
                    "webKeyword.dragAndDrop(productItemObject, cartDropZoneObject);\n\n" +
                    "// Kéo và thả để sắp xếp lại danh sách\n" +
                    "webKeyword.dragAndDrop(taskItemObject, topOfListObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "cả hai element nguồn và đích phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "trang web phải hỗ trợ thao tác kéo và thả, và trình duyệt phải hỗ trợ thao tác kéo và thả " +
                    "(một số trình duyệt di động có thể không hỗ trợ đầy đủ). " +
                    "Có thể throw ElementNotVisibleException nếu một trong hai element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu một trong hai element không thể tương tác, " +
                    "StaleElementReferenceException nếu một trong hai element không còn gắn với DOM, " +
                    "TimeoutException nếu một trong hai element không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu element đích nằm ngoài viewport hiện tại."
    )
    @Step("Drag element {0.name} and drop to {1.name}")
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
            description = "Kéo một element theo một khoảng cách (độ lệch x, y) so với vị trí hiện tại of nó. Rất hữu ích cho các thanh trượt (slider).",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element cần kéo",
                    "xOffset: int - Độ lệch theo trục ngang (pixel)",
                    "yOffset: int - Độ lệch theo trục dọc (pixel)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kéo thanh trượt giá sang phải 100px\n" +
                    "webKeyword.dragAndDropByOffset(priceSliderHandle, 100, 0);\n\n" +
                    "// Kéo thanh trượt âm lượng xuống 50px\n" +
                    "webKeyword.dragAndDropByOffset(volumeSliderObject, 0, -50);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần kéo phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "trang web phải hỗ trợ thao tác kéo và thả, và trình duyệt phải hỗ trợ thao tác kéo và thả " +
                    "(một số trình duyệt di động có thể không hỗ trợ đầy đủ). " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu vị trí đích nằm ngoài viewport hiện tại."
    )
    @Step("Drag element {0.name} by offset ({1}, {2})")
    public void dragAndDropByOffset(ObjectUI uiObject, int xOffset, int yOffset) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new Actions(DriverManager.getDriver()).dragAndDropBy(element, xOffset, yOffset).perform();
            return null;
        }, uiObject, xOffset, yOffset);
    }

    @NetatKeyword(
            name = "pressKeys",
            description = "Gửi một chuỗi ký tự hoặc một tổ hợp phím (ví dụ: Ctrl+C, Enter) tới element đang được focus trên trình duyệt.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "keys: CharSequence... - Một hoặc nhiều chuỗi ký tự hoặc phím đặc biệt từ org.openqa.selenium.Keys"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Gửi tổ hợp phím Ctrl + A để chọn tất cả\n" +
                    "webKeyword.pressKeys(Keys.CONTROL, \"a\");\n\n" +
                    "// Gửi phím Enter để xác nhận form\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.pressKeys(Keys.ENTER);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần nhận tổ hợp phím phải đang được focus, " +
                    "trình duyệt phải hỗ trợ các tổ hợp phím được sử dụng, " +
                    "và các phím đặc biệt phải được định nghĩa trong org.openqa.selenium.Keys. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "NoSuchSessionException nếu phiên WebDriver không còn hợp lệ, " +
                    "UnsupportedOperationException nếu trình duyệt không hỗ trợ thao tác phím được yêu cầu, " +
                    "hoặc IllegalArgumentException nếu tham số keys không hợp lệ."
    )
    @Step("Send key combination: {0}")
    public void pressKeys(CharSequence... keys) {
        execute(() -> {
            Actions actions = new Actions(DriverManager.getDriver());

            List<Keys> modifierKeys = new ArrayList<>();
            List<CharSequence> regularKeys = new ArrayList<>();

            for (CharSequence key : keys) {
                if (key instanceof Keys && (key.equals(Keys.CONTROL) || key.equals(Keys.ALT) || key.equals(Keys.SHIFT))) {
                    modifierKeys.add((Keys) key);
                } else {
                    regularKeys.add(key);
                }
            }
            if (!modifierKeys.isEmpty()) {
                for (Keys modifier : modifierKeys) {
                    actions.keyDown(modifier);
                }
                for (CharSequence regularKey : regularKeys) {
                    actions.sendKeys(regularKey);
                }
                for (Keys modifier : modifierKeys) {
                    actions.keyUp(modifier);
                }
            } else {
                for (CharSequence key : regularKeys) {
                    actions.sendKeys(key);
                }
            }
            actions.perform();
            return null;
        }, (Object[]) keys);
    }

    @NetatKeyword(
            name = "clickWithJavascript",
            description = "Thực hiện click vào một element bằng JavaScript. Hữu ích khi click thông thường không hoạt động.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element cần click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click vào nút ẩn\n" +
                    "webKeyword.clickWithJavascript(hiddenButtonObject);\n\n" +
                    "// Click vào element bị che khuất bởi element khác\n" +
                    "webKeyword.clickWithJavascript(overlappedElementObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần click phải tồn tại trong DOM, " +
                    "trình duyệt phải hỗ trợ thực thi JavaScript, " +
                    "và người dùng phải có quyền thực thi JavaScript trên trang. " +
                    "Có thể throw JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
    )
    @Step("Click element {0.name} via JavaScript")
    public void clickWithJavascript(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].click();", element);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "selectByIndex",
            description = "Chọn một option (option) trong một element dropdown (thẻ select) dựa trên chỉ số of nó (bắt đầu từ 0).",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element dropdown (thẻ select)",
                    "index: int - Chỉ số of option cần chọn (ví dụ: 0 cho option đầu tiên)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chọn option thứ hai in dropdown quốc gia\n" +
                    "webKeyword.selectByIndex(countryDropdownObject, 1); // Chỉ số bắt đầu từ 0\n\n" +
                    "// Chọn option đầu tiên in dropdown ngôn ngữ\n" +
                    "webKeyword.selectByIndex(languageDropdownObject, 0);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element dropdown phải tồn tại trong DOM, phải is thẻ <select> hợp lệ, " +
                    "phải hiển thị và có thể tương tác được, " +
                    "và chỉ số phải nằm trong phạm vi hợp lệ (0 đến số lượng option - 1). " +
                    "Có thể throw NoSuchElementException nếu element dropdown không tồn tại, " +
                    "ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "IndexOutOfBoundsException nếu chỉ số nằm ngoài phạm vi hợp lệ, " +
                    "UnexpectedTagNameException nếu element không phải is thẻ <select>, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Select option at index {1} for dropdown {0.name}")
    public void selectByIndex(ObjectUI uiObject, int index) {
        execute(() -> {
            Select select = new Select(findElement(uiObject));
            select.selectByIndex(index);
            return null;
        }, uiObject, index);
    }

    @NetatKeyword(
            name = "selectRadioByValue",
            description = "Chọn một radio button trong một nhóm các radio button dựa trên giá trị of thuộc tính 'value'.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Đại diện cho nhóm radio button (ví dụ locator chung is '//input[@name=\"gender\"]')",
                    "value: String - Giá trị trong thuộc tính 'value' of radio button cần chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chọn radio button giới tính nữ\n" +
                    "webKeyword.selectRadioByValue(genderRadioGroup, \"female\");\n\n" +
                    "// Chọn phương thức thanh toán\n" +
                    "webKeyword.selectRadioByValue(paymentMethodRadioGroup, \"credit_card\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "nhóm radio button phải tồn tại trong DOM, " +
                    "ít nhất một radio button trong nhóm phải có thuộc tính 'value' khớp với giá trị cần chọn, " +
                    "và element phải hiển thị và có thể tương tác được. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy radio button với value chỉ định, " +
                    "ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Select radio button with value '{1}' in group {0.name}")
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
            throw new NoSuchElementException("Radio button with value not found: " + value);
        }, uiObject, value);
    }


    @NetatKeyword(
            name = "selectByValue",
            description = "Chọn một option in dropdown dựa trên giá trị of thuộc tính 'value'.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element dropdown (thẻ select)",
                    "value: String - Giá trị thuộc tính 'value' of option cần chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chọn thành phố Hà Nội từ dropdown\n" +
                    "webKeyword.selectByValue(cityDropdown, \"HN\");\n\n" +
                    "// Chọn phương thức vận chuyển\n" +
                    "webKeyword.selectByValue(shippingMethodDropdown, \"express\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element dropdown phải tồn tại trong DOM, phải is thẻ <select> hợp lệ, " +
                    "phải hiển thị và có thể tương tác được, " +
                    "và phải tồn tại ít nhất một option có thuộc tính value khớp với giá trị cần chọn. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy option với value chỉ định, " +
                    "ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "UnexpectedTagNameException nếu element không phải is thẻ <select>, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Select option with value '{1}' for dropdown {0.name}")
    public void selectByValue(ObjectUI uiObject, String value) {
        execute(() -> {
            Select select = new Select(findElement(uiObject));
            select.selectByValue(value);
            return null;
        }, uiObject, value);
    }

    @NetatKeyword(
            name = "selectByVisibleText",
            description = "Chọn một option in dropdown dựa trên text hiển thị of nó.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element dropdown (thẻ select)",
                    "text: String - text hiển thị of option cần chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chọn quốc gia từ dropdown theo tên hiển thị\n" +
                    "webKeyword.selectByVisibleText(countryDropdown, \"Việt Nam\");\n\n" +
                    "// Chọn danh mục sản phẩm\n" +
                    "webKeyword.selectByVisibleText(categoryDropdown, \"Điện thoại & Máy tính bảng\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element dropdown phải tồn tại trong DOM, phải is thẻ <select> hợp lệ, " +
                    "phải hiển thị và có thể tương tác được, " +
                    "phải tồn tại ít nhất một option có text hiển thị khớp chính xác với text cần chọn, " +
                    "và text cần chọn phải khớp chính xác với text hiển thị (phân biệt chữ hoa/thường, khoảng trắng, ký tự đặc biệt). " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy option với text hiển thị chỉ định, " +
                    "ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "UnexpectedTagNameException nếu element không phải is thẻ <select>, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Select option with text '{1}' for dropdown {0.name}")
    public void selectByVisibleText(ObjectUI uiObject, String text) {
        execute(() -> {
            Select select = new Select(findElement(uiObject));
            select.selectByVisibleText(text);
            return null;
        }, uiObject, text);
    }

    @NetatKeyword(
            name = "clickElementByIndex",
            description = "Click vào một element cụ thể trong một danh sách các element dựa trên chỉ số (index) of nó (bắt đầu từ 0).",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện đại diện cho danh sách element",
                    "index: int - Vị trí of element cần click (0 cho element đầu tiên)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click vào kết quả tìm kiếm thứ 3\n" +
                    "webKeyword.clickElementByIndex(searchResultLinks, 2); // Index bắt đầu từ 0\n\n" +
                    "// Click vào mục đầu tiên trong danh sách\n" +
                    "webKeyword.clickElementByIndex(menuItems, 0);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "danh sách element phải tồn tại trong DOM, " +
                    "chỉ số phải nằm trong phạm vi hợp lệ (0 đến số lượng element - 1), " +
                    "và element tại chỉ số cần click phải hiển thị và có thể tương tác được. " +
                    "Có thể throw IndexOutOfBoundsException nếu chỉ số nằm ngoài phạm vi hợp lệ, " +
                    "ElementNotVisibleException nếu element không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu element không thể tương tác, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchElementException nếu danh sách element không tồn tại."
    )
    @Step("Click element at index {1} in list {0.name}")
    public void clickElementByIndex(ObjectUI uiObject, int index) {
        execute(() -> {
            List<WebElement> elements = findElements(uiObject);
            if (index >= 0 && index < elements.size()) {
                elements.get(index).click();
            } else {
                throw new IndexOutOfBoundsException("Invalid index: " + index + ". The list contains only " + elements.size() + " element(s).");
            }
            return null;
        }, uiObject, index);
    }

    @NetatKeyword(
            name = "scrollToElement",
            description = "Cuộn trang đến khi element được chỉ định nằm trong vùng có thể nhìn thấy of trình duyệt. " +
                    "Rất cần thiết khi cần tương tác với các element ở cuối trang.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - element đích cần cuộn đến"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Cuộn đến phần chân trang\n" +
                    "webKeyword.scrollToElement(footerSectionObject);\n" +
                    "webKeyword.click(privacyPolicyLinkObject);\n\n" +
                    "// Cuộn đến nút gửi ở cuối form\n" +
                    "webKeyword.scrollToElement(submitButtonObject);\n" +
                    "webKeyword.click(submitButtonObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần cuộn đến phải tồn tại trong DOM, " +
                    "và trình duyệt phải hỗ trợ thực thi JavaScript. " +
                    "Có thể throw NoSuchElementException nếu element không tồn tại, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Scroll to element: {0.name}")
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
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "x: int - Tọa độ theo trục hoành (pixel)",
                    "y: int - Tọa độ theo trục tung (pixel)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Cuộn xuống 500px từ đầu trang\n" +
                    "webKeyword.scrollToCoordinates(0, 500);\n\n" +
                    "// Cuộn đến đầu trang\n" +
                    "webKeyword.scrollToCoordinates(0, 0);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trình duyệt phải hỗ trợ thực thi JavaScript, " +
                    "và tọa độ phải nằm trong phạm vi hợp lệ of trang web. " +
                    "Có thể throw JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Scroll page to coordinates ({0}, {1})")
    public void scrollToCoordinates(int x, int y) {
        execute(() -> {
            JavascriptExecutor js = (JavascriptExecutor) DriverManager.getDriver();
            js.executeScript("window.scrollTo(arguments[0], arguments[1])", x, y);
            return null;
        }, x, y);
    }

    @NetatKeyword(
            name = "scrollToTop",
            description = "Cuộn lên vị trí cao nhất (đầu trang) of trang web.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Cuộn lên đầu trang để truy cập menu chính\n" +
                    "webKeyword.scrollToTop();\n" +
                    "webKeyword.verifyElementVisible(mainMenuObject);\n\n" +
                    "// Cuộn lên đầu trang sau khi hoàn thành form dài\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "webKeyword.scrollToTop();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trình duyệt phải hỗ trợ thực thi JavaScript. " +
                    "Có thể throw JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Scroll to top of page")
    public void scrollToTop() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("window.scrollTo(0, 0)");
            return null;
        });
    }

    @NetatKeyword(
            name = "scrollToBottom",
            description = "Cuộn xuống vị trí thấp nhất (cuối trang) of trang web.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Cuộn xuống cuối trang để truy cập chân trang\n" +
                    "webKeyword.scrollToBottom();\n" +
                    "webKeyword.verifyElementVisible(footerLinksObject);\n\n" +
                    "// Cuộn xuống cuối trang để tải thêm dữ liệu (infinite scroll)\n" +
                    "webKeyword.scrollToBottom();\n" +
                    "webKeyword.waitForElementVisible(loadingIndicatorObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trình duyệt phải hỗ trợ thực thi JavaScript. " +
                    "Có thể throw JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Scroll to bottom of page")
    public void scrollToBottom() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("window.scrollTo(0, document.body.scrollHeight)");
            return null;
        });
    }


    @Override
    @NetatKeyword(
            name = "getText",
            description = "Lấy và trả về text of element. Keyword này sẽ tự động thử nhiều cách: " +
                    "1. Lấy thuộc tính 'value' (cho ô input, textarea). " +
                    "2. Lấy text hiển thị thông thường. " +
                    "3. Lấy 'textContent' hoặc 'innerText' nếu 2 cách trên thất bại.",
            category = "Web",
            subCategory = "Getter",
            parameters = {
                    "uiObject: ObjectUI - element contains text cần lấy"
            },
            returnValue = "String - text of element hoặc chuỗi rỗng nếu không có text",
            example = "// Lấy text từ element hiển thị\n" +
                    "String welcomeMessage = webKeyword.getText(welcomeMessageObject);\n" +
                    "webKeyword.verifyEqual(welcomeMessage, \"Chào mừng bạn!\");\n\n" +
                    "// Lấy giá trị từ ô input\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "String username = webKeyword.getText(usernameInputObject);\n" +
                    "webKeyword.verifyEqual(username, \"testuser\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần lấy text phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu element không tồn tại, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Get text from element: {0.name}")
    public String getText(ObjectUI uiObject) {
        return super.getText(uiObject);
    }

    @NetatKeyword(
            name = "getAttribute",
            description = "Lấy và trả về giá trị of một thuộc tính (attribute) cụ thể trên một element HTML.",
            category = "Web",
            subCategory = "Getter",
            parameters = {
                    "uiObject: ObjectUI - element cần lấy thuộc tính",
                    "attributeName: String - Tên of thuộc tính cần lấy giá trị (ví dụ: 'href', 'class', 'value')"
            },
            returnValue = "String - Giá trị of thuộc tính hoặc null nếu thuộc tính không tồn tại",
            example = "// Lấy URL từ thẻ liên kết\n" +
                    "String linkUrl = webKeyword.getAttribute(linkObject, \"href\");\n" +
                    "webKeyword.verifyContains(linkUrl, \"https://example.com\");\n\n" +
                    "// Verify trạng thái of checkbox\n" +
                    "String isChecked = webKeyword.getAttribute(termsCheckboxObject, \"checked\");\n" +
                    "webKeyword.verifyNotNull(isChecked);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần lấy thuộc tính phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu element không tồn tại, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Get attribute '{1}' of element {0.name}")
    public String getAttribute(ObjectUI uiObject, String attributeName) {
        return execute(() -> findElement(uiObject).getAttribute(attributeName), uiObject, attributeName);
    }

    @NetatKeyword(
            name = "getCssValue",
            description = "Lấy giá trị of một thuộc tính CSS được áp dụng trên một element.",
            category = "Web",
            subCategory = "Getter",
            parameters = {
                    "uiObject: ObjectUI - element cần lấy giá trị CSS",
                    "cssPropertyName: String - Tên of thuộc tính CSS (ví dụ: 'color', 'font-size', 'background-color')"
            },
            returnValue = "String - Giá trị of thuộc tính CSS được chỉ định",
            example = "// Lấy màu chữ of nút\n" +
                    "String buttonColor = webKeyword.getCssValue(buttonObject, \"color\");\n" +
                    "webKeyword.verifyEqual(buttonColor, \"rgba(255, 255, 255, 1)\");\n\n" +
                    "// Verify kích thước font\n" +
                    "String fontSize = webKeyword.getCssValue(headingObject, \"font-size\");\n" +
                    "webKeyword.verifyEqual(fontSize, \"24px\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần lấy giá trị CSS phải tồn tại trong DOM, " +
                    "và thuộc tính CSS cần lấy phải được áp dụng cho element (trực tiếp hoặc được kế thừa). " +
                    "Có thể throw NoSuchElementException nếu element không tồn tại, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Get CSS value '{1}' of element {0.name}")
    public String getCssValue(ObjectUI uiObject, String cssPropertyName) {
        return execute(() -> findElement(uiObject).getCssValue(cssPropertyName), uiObject, cssPropertyName);
    }

    @NetatKeyword(
            name = "getCurrentUrl",
            description = "Lấy và trả về URL đầy đủ of trang web hiện tại mà trình duyệt đang hiển thị.",
            category = "Web",
            subCategory = "Getter",
            parameters = {},
            returnValue = "String - URL đầy đủ of trang web hiện tại",
            example = "// Verify URL sau khi điều hướng\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/products\");\n" +
                    "String currentUrl = webKeyword.getCurrentUrl();\n" +
                    "webKeyword.verifyEqual(currentUrl, \"https://example.com/products\");\n\n" +
                    "// Verify URL sau khi gửi form\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "String resultUrl = webKeyword.getCurrentUrl();\n" +
                    "webKeyword.verifyContains(resultUrl, \"success=true\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
    )
    @Step("Get current URL")
    public String getCurrentUrl() {
        return execute(() -> DriverManager.getDriver().getCurrentUrl());
    }

    @NetatKeyword(
            name = "getPageTitle",
            description = "Lấy và trả về tiêu đề (title) of trang web hiện tại.",
            category = "Web",
            subCategory = "Getter",
            parameters = {},
            returnValue = "String - Tiêu đề of trang web hiện tại",
            example = "// Verify tiêu đề trang sau khi điều hướng\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/about\");\n" +
                    "String pageTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.verifyEqual(pageTitle, \"Về chúng tôi - Example Company\");\n\n" +
                    "// Verify tiêu đề trang sau khi tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "String searchResultTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.verifyContains(searchResultTitle, \"Kết quả tìm kiếm\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
    )
    @Step("Get page title")
    public String getPageTitle() {
        return execute(() -> DriverManager.getDriver().getTitle());
    }

    @NetatKeyword(
            name = "getElementCount",
            description = "Đếm và trả về số lượng element trên trang khớp với locator được cung cấp. Hữu ích để Verify số lượng kết quả tìm kiếm, số hàng trong bảng,...",
            category = "Web",
            subCategory = "Getter",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện đại diện cho các element cần đếm"
            },
            returnValue = "int - Số lượng element tìm thấy",
            example = "// Đếm số lượng sản phẩm trong danh sách\n" +
                    "int numberOfProducts = webKeyword.getElementCount(productListItemObject);\n" +
                    "webKeyword.verifyEqual(numberOfProducts, 10);\n\n" +
                    "// Verify số lượng kết quả tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"smartphone\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "int resultCount = webKeyword.getElementCount(searchResultItemObject);\n" +
                    "System.out.println(\"Tìm thấy \" + resultCount + \" kết quả\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và locator of đối tượng giao diện phải hợp lệ. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "NoSuchSessionException nếu phiên WebDriver không còn hợp lệ, " +
                    "hoặc InvalidSelectorException nếu locator không hợp lệ."
    )
    @Step("Count elements of: {0.name}")
    public int getElementCount(ObjectUI uiObject) {
        return execute(() -> {
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return DriverManager.getDriver().findElements(by).size();
        }, uiObject);
    }

    @NetatKeyword(
            name = "getTextFromElements",
            description = "Lấy và trả về một danh sách (List) các chuỗi text từ mỗi element trong một danh sách các element.",
            category = "Web",
            subCategory = "Getter",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện đại diện cho các element cần lấy text"
            },
            returnValue = "List<String> - Danh sách các chuỗi text từ các element tìm thấy",
            example = "// Lấy danh sách tên sản phẩm\n" +
                    "List<String> productNames = webKeyword.getTextFromElements(productNameObject);\n" +
                    "System.out.println(\"Tìm thấy \" + productNames.size() + \" sản phẩm\");\n\n" +
                    "// Verify danh sách giá sản phẩm\n" +
                    "List<String> prices = webKeyword.getTextFromElements(productPriceObject);\n" +
                    "for (String price : prices) {\n" +
                    "    webKeyword.verifyTrue(price.contains(\"₫\"), \"Giá không đúng định dạng\");\n" +
                    "}",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "locator of đối tượng giao diện phải hợp lệ, " +
                    "và các element cần lấy text phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element nào khớp với locator, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM trong quá trình xử lý, " +
                    "TimeoutException nếu các element không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Get text from list of elements: {0.name}")
    public List<String> getTextFromElements(ObjectUI uiObject) {
        return execute(() -> {
            List<WebElement> elements = findElements(uiObject);
            return elements.stream()
                    .map(WebElement::getText)
                    .collect(Collectors.toList());
        }, uiObject);
    }

    @NetatKeyword(
            name = "waitForElementClickable",
            description = "Tạm dừng kịch bản cho đến khi một element không chỉ hiển thị mà còn ở trạng thái sẵn sàng để được click (enabled).",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - element cần chờ để sẵn sàng click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ nút gửi sẵn sàng để click sau khi điền form\n" +
                    "webKeyword.sendKeys(emailInputObject, \"test@example.com\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.waitForElementClickable(submitButtonObject);\n" +
                    "webKeyword.click(submitButtonObject);\n\n" +
                    "// Chờ nút được kích hoạt sau khi chọn một option\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.waitForElementClickable(continueButtonObject);\n" +
                    "webKeyword.click(continueButtonObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần chờ phải tồn tại trong DOM, " +
                    "và element sẽ trở thành hiển thị và có thể click trong khoảng thời gian chờ. " +
                    "Có thể throw TimeoutException nếu element không trở nên có thể click trong thời gian chờ mặc định, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Wait for element {0.name} to be clickable")
    public void waitForElementClickable(ObjectUI uiObject) {
        execute(() -> {
            new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(findElement(uiObject)));
            return null;
        }, uiObject);
    }


    @NetatKeyword(
            name = "waitForElementNotVisible",
            description = "Tạm dừng kịch bản cho đến khi một element không còn hiển thị trên giao diện. Rất hữu ích để chờ các biểu tượng loading hoặc thông báo tạm thời biến mất.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - element cần chờ cho đến khi nó biến mất"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ biểu tượng loading biến mất sau khi gửi form\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "webKeyword.waitForElementNotVisible(loadingSpinnerObject);\n" +
                    "webKeyword.verifyElementVisible(successMessageObject);\n\n" +
                    "// Chờ popup đóng sau khi nhấn nút đóng\n" +
                    "webKeyword.click(closePopupButtonObject);\n" +
                    "webKeyword.waitForElementNotVisible(popupObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần chờ phải tồn tại trong DOM hoặc đã hiển thị trước đó, " +
                    "và element sẽ trở thành không hiển thị trong khoảng thời gian chờ. " +
                    "Có thể throw TimeoutException nếu element vẫn còn hiển thị sau thời gian chờ mặc định, " +
                    "NoSuchElementException nếu không tìm thấy element ban đầu, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Wait for element {0.name} to disappear")
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
            description = "Tạm dừng kịch bản cho đến khi một element tồn tại trong DOM of trang, không nhất thiết phải hiển thị. Hữu ích để chờ các element được tạo ra bởi JavaScript.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - element cần chờ cho đến khi nó tồn tại",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng seconds)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ element động được tạo bởi JavaScript\n" +
                    "webKeyword.click(loadDynamicContentButton);\n" +
                    "webKeyword.waitForElementPresent(dynamicContentObject, 10);\n" +
                    "webKeyword.verifyElementPresent(dynamicContentObject);\n\n" +
                    "// Chờ element được tạo sau khi chọn option\n" +
                    "webKeyword.selectByVisibleText(categoryDropdownObject, \"Điện thoại\");\n" +
                    "webKeyword.waitForElementPresent(subcategoryListObject, 5);\n" +
                    "webKeyword.verifyElementCount(subcategoryItemObject, 5);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "locator of element phải hợp lệ, " +
                    "và element sẽ được thêm vào DOM trong khoảng thời gian chờ đã chỉ định. " +
                    "Có thể throw TimeoutException nếu element không xuất hiện trong DOM trong thời gian chờ đã chỉ định, " +
                    "InvalidSelectorException nếu locator không hợp lệ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Wait for element {0.name} to be present in DOM trong {1} seconds")
    public void waitForElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.presenceOfElementLocated(uiObject.getActiveLocators().get(0).convertToBy()));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForPageLoaded",
            description = "Tạm dừng kịch bản cho đến khi trang web tải xong hoàn toàn (trạng thái 'document.readyState' is 'complete').",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng seconds)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ trang tải xong sau khi điều hướng\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/dashboard\");\n" +
                    "webKeyword.waitForPageLoaded(30);\n" +
                    "webKeyword.verifyElementVisible(dashboardWidgetsObject);\n\n" +
                    "// Chờ trang tải xong sau khi đăng nhập\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(25);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trình duyệt phải hỗ trợ thực thi JavaScript, " +
                    "và trang web sẽ hoàn thành quá trình tải trong khoảng thời gian chờ đã chỉ định. " +
                    "Có thể throw TimeoutException nếu trang không tải xong trong thời gian chờ đã chỉ định, " +
                    "JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Wait for page to load trong {0} seconds")
    public void waitForPageLoaded(int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete"));
            return null;
        }, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForUrlContains",
            description = "Tạm dừng kịch bản cho đến khi URL of trang hiện tại contains một chuỗi con được chỉ định.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "partialUrl: String - Chuỗi con mà URL cần contains",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng seconds)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ chuyển hướng đến trang dashboard\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForUrlContains(\"/dashboard\", 15);\n" +
                    "webKeyword.verifyElementVisible(welcomeMessageObject);\n\n" +
                    "// Chờ chuyển hướng sau khi tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForUrlContains(\"search=laptop\", 10);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và URL of trang sẽ contains chuỗi con đã chỉ định trong khoảng thời gian chờ. " +
                    "Có thể throw TimeoutException nếu URL không contains chuỗi con đã chỉ định trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Wait for URL to contain '{0}' trong {1} seconds")
    public void waitForUrlContains(String partialUrl, int timeoutInSeconds) {
        execute(() -> {
            new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds))
                    .until(ExpectedConditions.urlContains(partialUrl));
            return null;
        }, partialUrl, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForTitleIs",
            description = "Tạm dừng kịch bản cho đến khi tiêu đề of trang hiện tại khớp chính xác với chuỗi được chỉ định.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "expectedTitle: String - Tiêu đề trang mong đợi",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng seconds)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ tiêu đề trang sau khi đăng nhập thành công\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForTitleIs(\"Bảng điều khiển người dùng\", 15);\n\n" +
                    "// Chờ tiêu đề trang sau khi chuyển tab\n" +
                    "webKeyword.click(profileTabObject);\n" +
                    "webKeyword.waitForTitleIs(\"Thông tin cá nhân\", 10);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và tiêu đề trang sẽ thay đổi thành giá trị mong đợi trong khoảng thời gian chờ. " +
                    "Có thể throw TimeoutException nếu tiêu đề trang không khớp với giá trị mong đợi trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Wait for page title to be '{0}' trong {1} seconds")
    public void waitForTitleIs(String expectedTitle, int timeoutInSeconds) {
        execute(() -> {
            new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds))
                    .until(ExpectedConditions.titleIs(expectedTitle));
            return null;
        }, expectedTitle, timeoutInSeconds);
    }


    @NetatKeyword(
            name = "waitForElementVisible",
            description = "Chờ đợi một element trở nên visible (hiển thị) trên màn hình trong khoảng thời gian chỉ định. Sử dụng explicit wait để đảm bảo element không chỉ present trong DOM mà còn thực sự hiển thị cho người dùng.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần chờ đợi hiển thị",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "LƯU Ý: Method này chờ element VISIBLE (display: block, opacity > 0, không bị che khuất), khác với waitForElementPresent chỉ cần element có trong DOM. Sẽ throw TimeoutException nếu vượt quá thời gian chờ.",
            example = "// Chờ button login hiển thị trong 10 giây\nmobileKeyword.waitForElementVisible(loginButton, 10);\n\n// Chờ popup xuất hiện trong 5 giây\nmobileKeyword.waitForElementVisible(popupDialog, 5);"
    )
    @Step("Wait for element '{0}' to be visible within {1} seconds")
    public void waitForElementVisible(ObjectUI uiObject, int timeoutInSeconds) {
        super.waitForElementVisible(uiObject, timeoutInSeconds);
    }


    @NetatKeyword(
            name = "waitForElementClickable",
            description = "Chờ đợi một element trở nên clickable (có thể click được) trong khoảng thời gian chỉ định. Element phải đồng thời visible và enabled để được coi là clickable. Đây là điều kiện lý tưởng trước khi thực hiện click action.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần chờ đợi có thể click",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "BEST PRACTICE: Luôn sử dụng method này trước khi click vào element để tránh ElementNotInteractableException. Element clickable = visible + enabled + không bị overlay che khuất.",
            example = "// Chờ button submit có thể click trong 15 giây\nmobileKeyword.waitForElementClickable(submitButton, 15);\nmobileKeyword.click(submitButton);\n\n// Chờ link navigation sẵn sàng click\nmobileKeyword.waitForElementClickable(navLink, 8);"
    )
    @Step("Wait for element '{0}' to be clickable within {1} seconds")
    public void waitForElementClickable(ObjectUI uiObject, int timeoutInSeconds) {
        super.waitForElementClickable(uiObject, timeoutInSeconds);
    }



    @NetatKeyword(
            name = "waitForElementNotPresent",
            description = "Chờ đợi một element bị xóa hoàn toàn khỏi DOM trong khoảng thời gian chỉ định. Khác với waitForElementNotVisible (chỉ ẩn), method này đảm bảo element không còn tồn tại trong page source. Rất hữu ích cho cleanup testing và dynamic content.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần chờ đợi bị xóa khỏi DOM",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "QUAN TRỌNG: waitForElementNotPresent (xóa khỏi DOM) khác với waitForElementNotVisible (chỉ ẩn đi). Sử dụng khi cần đảm bảo element thực sự bị remove, không phải chỉ display:none. Thường dùng sau delete actions hoặc dynamic loading.",
            example = "// Chờ popup dialog bị xóa hoàn toàn sau khi đóng\nwebKeyword.click(closeButton);\nwebKeyword.waitForElementNotPresent(popupDialog, 10);\n\n// Chờ loading spinner biến mất sau AJAX\nwebKeyword.waitForElementNotPresent(loadingSpinner, 15);"
    )
    @Step("Wait for element '{0}' to be removed from DOM within {1} seconds")
    public void waitForElementNotPresent(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            wait.until(ExpectedConditions.not(ExpectedConditions.presenceOfElementLocated(by)));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForElementTextContains",
            description = "Chờ đợi text của element chứa substring chỉ định trong khoảng thời gian cho phép. Thực hiện partial matching, rất hữu ích cho validation nội dung động, messages, notifications hoặc khi text có thể thay đổi nhưng vẫn chứa keyword quan trọng.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần kiểm tra text content",
                    "String expectedText - Substring mà text của element phải chứa",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "USE CASE: Lý tưởng cho validation messages động, notifications, counters, hoặc content được load từ API. Khác với waitForElementTextToBe (exact match), method này linh hoạt hơn với partial matching.",
            example = "// Chờ success message chứa 'thành công'\nwebKeyword.waitForElementTextContains(successMessage, \"thành công\", 10);\n\n// Chờ counter chứa số lượng items\nwebKeyword.waitForElementTextContains(itemCounter, \"items\", 8);\n\n// Validation error message\nwebKeyword.waitForElementTextContains(errorMsg, \"Invalid\", 5);"
    )
    @Step("Wait for element '{0}' text to contain '{1}' within {2} seconds")
    public void waitForElementTextContains(ObjectUI uiObject, String expectedText, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.textToBePresentInElement(findElement(uiObject), expectedText));
            return null;
        }, uiObject, expectedText, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForElementAttributeToBe",
            description = "Chờ đợi attribute của element có giá trị chính xác như mong đợi trong khoảng thời gian chỉ định. Rất hữu ích cho validation các thay đổi attribute động như status, state, data-attributes, class names, hoặc các thuộc tính được cập nhật qua JavaScript.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần kiểm tra attribute",
                    "String attributeName - Tên attribute cần kiểm tra (vd: class, data-status, disabled)",
                    "String expectedValue - Giá trị mong đợi của attribute",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "COMMON ATTRIBUTES: class, data-*, disabled, checked, selected, aria-*, style, value. Đặc biệt hữu ích cho SPA applications khi attributes thay đổi theo state. Lưu ý: attribute value khác với property value.",
            example = "// Chờ button chuyển sang disabled state\nwebKeyword.waitForElementAttributeToBe(submitBtn, \"disabled\", \"true\", 10);\n\n// Chờ element có class 'active'\nwebKeyword.waitForElementAttributeToBe(menuItem, \"class\", \"menu-item active\", 8);\n\n// Chờ data attribute cập nhật\nwebKeyword.waitForElementAttributeToBe(statusDiv, \"data-status\", \"completed\", 15);"
    )
    @Step("Wait for element '{0}' attribute '{1}' to be '{2}' within {3} seconds")
    public void waitForElementAttributeToBe(ObjectUI uiObject, String attributeName, String expectedValue, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.attributeToBe(findElement(uiObject), attributeName, expectedValue));
            return null;
        }, uiObject, attributeName, expectedValue, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForJavaScriptReturnsValue",
            description = "Chờ đợi JavaScript code thực thi và trả về giá trị mong đợi trong khoảng thời gian chỉ định. Rất hữu ích cho việc chờ đợi AJAX calls hoàn thành, custom conditions, hoặc các trạng thái phức tạp của trang web không thể detect bằng DOM elements.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "String script - JavaScript code cần thực thi",
                    "Object expectedValue - Giá trị mong đợi từ script (có thể là String, Boolean, Number)",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "ADVANCED USE CASE: Thích hợp cho SPA applications, AJAX monitoring, custom loading states. Script có thể return any type (String, Boolean, Number, null). Sử dụng khi standard WebDriver waits không đủ mạnh.",
            example = "// Chờ jQuery AJAX calls hoàn thành\nwebKeyword.waitForJavaScriptReturnsValue(\"return jQuery.active\", 0, 30);\n\n// Chờ custom loading flag\nwebKeyword.waitForJavaScriptReturnsValue(\"return window.isLoading\", false, 20);\n\n// Chờ API response được set\nwebKeyword.waitForJavaScriptReturnsValue(\"return window.apiData !== undefined\", true, 15);"
    )
    @Step("Wait for JavaScript '{0}' to return '{1}' within {2} seconds")
    public void waitForJavaScriptReturnsValue(String script, Object expectedValue, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    Object actualValue = js.executeScript(script);
                    return Objects.equals(actualValue, expectedValue);
                } catch (Exception e) {
                    return false;
                }
            });
            return null;
        }, script, expectedValue, timeoutInSeconds);
    }


    @NetatKeyword(
            name = "waitForElementTextNotContains",
            description = "Chờ đợi text của element KHÔNG chứa substring chỉ định trong khoảng thời gian cho phép. Thực hiện negative partial matching, hữu ích cho validation rằng nội dung đã được xóa, thay đổi, hoặc error messages đã biến mất.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần kiểm tra text content",
                    "String unwantedText - Substring mà text của element KHÔNG được chứa",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "NEGATIVE VALIDATION: Hữu ích khi cần đảm bảo error messages biến mất, loading text được thay thế, hoặc unwanted content không còn xuất hiện. Thường dùng sau cleanup actions hoặc content updates.",
            example = "// Chờ error message không còn chứa 'Error'\nwebKeyword.waitForElementTextNotContains(messageDiv, \"Error\", 10);\n\n// Chờ loading text biến mất\nwebKeyword.waitForElementTextNotContains(statusLabel, \"Loading\", 15);\n\n// Validation content đã được update\nwebKeyword.waitForElementTextNotContains(titleElement, \"Draft\", 8);"
    )
    @Step("Wait for element '{0}' text to NOT contain '{1}' within {2} seconds")
    public void waitForElementTextNotContains(ObjectUI uiObject, String unwantedText, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                try {
                    String actualText = getText(uiObject);
                    return !actualText.contains(unwantedText);
                } catch (Exception e) {
                    return false;
                }
            });
            return null;
        }, uiObject, unwantedText, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForElementTextToBe",
            description = "Chờ đợi text của element có giá trị chính xác như mong đợi trong khoảng thời gian chỉ định. Thực hiện exact matching, hữu ích cho validation chính xác nội dung, status messages, hoặc khi cần đảm bảo text hoàn toàn khớp.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần kiểm tra text content",
                    "String expectedText - Text chính xác mà element phải có",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "EXACT MATCH: Khác với waitForElementTextContains (partial), method này yêu cầu text phải hoàn toàn khớp. Sử dụng cho validation chính xác status, labels, hoặc khi text có format cố định.",
            example = "// Chờ status chính xác\nwebKeyword.waitForElementTextToBe(statusLabel, \"Completed\", 10);\n\n// Chờ counter có giá trị exact\nwebKeyword.waitForElementTextToBe(itemCount, \"5 items\", 8);\n\n// Validation button text\nwebKeyword.waitForElementTextToBe(submitButton, \"Submit\", 5);"
    )
    @Step("Wait for element '{0}' text to be '{1}' within {2} seconds")
    public void waitForElementTextToBe(ObjectUI uiObject, String expectedText, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                try {
                    String actualText = getText(uiObject);
                    return actualText.equals(expectedText);
                } catch (Exception e) {
                    return false;
                }
            });
            return null;
        }, uiObject, expectedText, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForElementTextNotToBe",
            description = "Chờ đợi text của element KHÔNG có giá trị chỉ định trong khoảng thời gian cho phép. Thực hiện negative exact matching, hữu ích cho validation rằng nội dung đã thay đổi khỏi trạng thái cũ, loading states đã kết thúc.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần kiểm tra text content",
                    "String unwantedText - Text chính xác mà element KHÔNG được có",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "NEGATIVE EXACT MATCH: Hữu ích khi cần đảm bảo text đã thay đổi khỏi giá trị cũ, loading states kết thúc, hoặc placeholder text được thay thế. Thường dùng sau update actions.",
            example = "// Chờ status thay đổi khỏi 'Pending'\nwebKeyword.waitForElementTextNotToBe(statusLabel, \"Pending\", 15);\n\n// Chờ placeholder biến mất\nwebKeyword.waitForElementTextNotToBe(inputField, \"Enter text...\", 5);\n\n// Chờ loading text thay đổi\nwebKeyword.waitForElementTextNotToBe(loadingDiv, \"Loading...\", 20);"
    )
    @Step("Wait for element '{0}' text to NOT be '{1}' within {2} seconds")
    public void waitForElementTextNotToBe(ObjectUI uiObject, String unwantedText, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                try {
                    String actualText = getText(uiObject);
                    return !actualText.equals(unwantedText);
                } catch (Exception e) {
                    return false;
                }
            });
            return null;
        }, uiObject, unwantedText, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForElementAttributeNotToBe",
            description = "Chờ đợi attribute của element KHÔNG có giá trị chỉ định trong khoảng thời gian cho phép. Hữu ích cho validation rằng attributes đã thay đổi khỏi trạng thái trước đó, như disabled thành enabled, loading class bị remove.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần kiểm tra attribute",
                    "String attributeName - Tên attribute cần kiểm tra",
                    "String unwantedValue - Giá trị mà attribute KHÔNG được có",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "NEGATIVE ATTRIBUTE CHECK: Thường dùng khi cần đảm bảo element đã thay đổi state, như từ disabled sang enabled, loading class đã bị remove, hoặc error states đã được clear.",
            example = "// Chờ button không còn disabled\nwebKeyword.waitForElementAttributeNotToBe(submitBtn, \"disabled\", \"true\", 10);\n\n// Chờ loading class bị remove\nwebKeyword.waitForElementAttributeNotToBe(container, \"class\", \"loading\", 15);\n\n// Chờ error state được clear\nwebKeyword.waitForElementAttributeNotToBe(inputField, \"data-error\", \"true\", 8);"
    )
    @Step("Wait for element '{0}' attribute '{1}' to NOT be '{2}' within {3} seconds")
    public void waitForElementAttributeNotToBe(ObjectUI uiObject, String attributeName, String unwantedValue, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                try {
                    String actualValue = getAttribute(uiObject, attributeName);
                    return !Objects.equals(actualValue, unwantedValue);
                } catch (Exception e) {
                    return false;
                }
            });
            return null;
        }, uiObject, attributeName, unwantedValue, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForElementAttributeContains",
            description = "Chờ đợi attribute của element chứa substring chỉ định trong khoảng thời gian cho phép. Rất hữu ích cho validation partial attribute values như CSS classes, composite data attributes, hoặc khi attribute có nhiều giá trị được nối với nhau.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần kiểm tra attribute",
                    "String attributeName - Tên attribute cần kiểm tra",
                    "String partialValue - Substring mà attribute phải chứa",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "PARTIAL ATTRIBUTE MATCH: Đặc biệt hữu ích cho CSS classes (class attribute thường chứa nhiều classes), data attributes có format phức tạp, hoặc style attributes. Linh hoạt hơn so với exact match.",
            example = "// Chờ element có class 'active' (trong nhiều classes)\nwebKeyword.waitForElementAttributeContains(menuItem, \"class\", \"active\", 10);\n\n// Chờ style chứa 'display: block'\nwebKeyword.waitForElementAttributeContains(modal, \"style\", \"display: block\", 8);\n\n// Chờ data attribute chứa status\nwebKeyword.waitForElementAttributeContains(item, \"data-info\", \"status:ready\", 15);"
    )
    @Step("Wait for element '{0}' attribute '{1}' to contain '{2}' within {3} seconds")
    public void waitForElementAttributeContains(ObjectUI uiObject, String attributeName, String partialValue, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                try {
                    String actualValue = getAttribute(uiObject, attributeName);
                    return actualValue != null && actualValue.contains(partialValue);
                } catch (Exception e) {
                    return false;
                }
            });
            return null;
        }, uiObject, attributeName, partialValue, timeoutInSeconds);
    }
    @NetatKeyword(
            name = "waitForElementAttributeNotContains",
            description = "Chờ đợi attribute của element KHÔNG chứa substring chỉ định trong khoảng thời gian cho phép. Hữu ích cho validation rằng partial attribute values đã được remove hoặc thay đổi, như CSS classes bị xóa, error states được clear.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI element cần kiểm tra attribute",
                    "String attributeName - Tên attribute cần kiểm tra",
                    "String unwantedPartialValue - Substring mà attribute KHÔNG được chứa",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "NEGATIVE PARTIAL MATCH: Thường dùng khi cần đảm bảo CSS classes đã bị remove (như 'loading', 'error'), style properties đã thay đổi, hoặc data attributes đã được cleanup.",
            example = "// Chờ loading class bị remove\nwebKeyword.waitForElementAttributeNotContains(container, \"class\", \"loading\", 15);\n\n// Chờ error class biến mất\nwebKeyword.waitForElementAttributeNotContains(inputField, \"class\", \"error\", 10);\n\n// Chờ style không còn chứa 'display: none'\nwebKeyword.waitForElementAttributeNotContains(modal, \"style\", \"display: none\", 8);"
    )
    @Step("Wait for element '{0}' attribute '{1}' to NOT contain '{2}' within {3} seconds")
    public void waitForElementAttributeNotContains(ObjectUI uiObject, String attributeName, String unwantedPartialValue, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                try {
                    String actualValue = getAttribute(uiObject, attributeName);
                    return actualValue == null || !actualValue.contains(unwantedPartialValue);
                } catch (Exception e) {
                    return false;
                }
            });
            return null;
        }, uiObject, attributeName, unwantedPartialValue, timeoutInSeconds);
    }


    @NetatKeyword(
            name = "waitForJavaScriptNotReturnsValue",
            description = "Chờ đợi JavaScript code thực thi và KHÔNG trả về giá trị chỉ định trong khoảng thời gian cho phép. Hữu ích cho việc chờ đợi các conditions không còn đúng nữa, states đã thay đổi, hoặc loading processes đã kết thúc.",
            category = "Web",
            subCategory = "Wait",
            parameters = {
                    "String script - JavaScript code cần thực thi",
                    "Object unwantedValue - Giá trị mà script KHÔNG được trả về",
                    "int timeoutInSeconds - Thời gian chờ tối đa tính bằng giây"
            },
            note = "NEGATIVE JS CONDITION: Thường dùng khi cần đảm bảo loading states đã kết thúc, error flags đã được clear, hoặc unwanted conditions không còn tồn tại. Complement của waitForJavaScriptReturnsValue.",
            example = "// Chờ loading flag không còn true\nwebKeyword.waitForJavaScriptNotReturnsValue(\"return window.isLoading\", true, 20);\n\n// Chờ error state được clear\nwebKeyword.waitForJavaScriptNotReturnsValue(\"return window.hasError\", true, 10);\n\n// Chờ AJAX calls không còn pending\nwebKeyword.waitForJavaScriptNotReturnsValue(\"return fetch.pending\", true, 30);"
    )
    @Step("Wait for JavaScript '{0}' to NOT return '{1}' within {2} seconds")
    public void waitForJavaScriptNotReturnsValue(String script, Object unwantedValue, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    Object actualValue = js.executeScript(script);
                    return !Objects.equals(actualValue, unwantedValue);
                } catch (Exception e) {
                    return false;
                }
            });
            return null;
        }, script, unwantedValue, timeoutInSeconds);
    }


    @NetatKeyword(
            name = "verifyElementVisibleHard",
            description = "Verify một element có đang hiển thị trên giao diện hay không. Nếu Verify thất bại (element không hiển thị như mong đợi), kịch bản sẽ DỪNG LẠI ngay lập tức.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "isVisible: boolean - Trạng thái hiển thị mong đợi (true cho hiển thị, false cho bị ẩn)",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify thông báo lỗi hiển thị sau khi gửi form không hợp lệ\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form trống\n" +
                    "webKeyword.verifyElementVisibleHard(errorMessageObject, true);\n\n" +
                    "// Verify element không hiển thị sau khi đóng với custom message\n" +
                    "webKeyword.click(closePopupButtonObject);\n" +
                    "webKeyword.verifyElementVisibleHard(popupObject, false, \"Popup phải được đóng hoàn toàn\");\n\n" +
                    "// Verify element quan trọng phải hiển thị\n" +
                    "webKeyword.verifyElementVisibleHard(loginButtonObject, true, \"Nút đăng nhập bắt buộc phải có\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu trạng thái hiển thị của element không khớp với kỳ vọng, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) element {0.name} visibility is {1}")
    public void verifyElementVisibleHard(ObjectUI uiObject, boolean isVisible, String... customMessage) {
        performVisibilityAssertion(uiObject, isVisible, false, customMessage);
    }

    @NetatKeyword(
            name = "verifyElementVisibleSoft",
            description = "Verify một element có đang hiển thị trên giao diện hay không. Nếu Verify thất bại, kịch bản sẽ ghi nhận lỗi nhưng vẫn TIẾP TỤC chạy các bước tiếp theo.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "isVisible: boolean - Trạng thái hiển thị mong đợi (true cho hiển thị, false cho bị ẩn)",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify thông báo thành công hiển thị sau khi lưu\n" +
                    "webKeyword.click(saveButtonObject);\n" +
                    "webKeyword.verifyElementVisibleSoft(successMessageObject, true);\n" +
                    "// Kịch bản vẫn tiếp tục ngay cả khi thông báo không hiển thị\n\n" +
                    "// Verify với custom message\n" +
                    "webKeyword.verifyElementVisibleSoft(headerLogoObject, true, \"Logo phải hiển thị trên header\");\n" +
                    "webKeyword.verifyElementVisibleSoft(navigationMenuObject, true, \"Menu điều hướng bị thiếu\");\n" +
                    "webKeyword.click(mainButtonObject); // Thực hiện hành động tiếp theo",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Soft) element {uiObject} is visible: {isVisible}"
    )
    public void verifyElementVisibleSoft(ObjectUI uiObject, boolean isVisible, String... customMessage) {
        execute(() -> {
                    performVisibilityAssertion(uiObject, isVisible, true, customMessage);
                    return null;
                },                       // params để hiện trong Allure
                uiObject != null ? uiObject.getName() : "null",
                isVisible,
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : ""
        );
    }

    @NetatKeyword(
            name = "verifyTextHard",
            description = "So sánh text của một element với một chuỗi ký tự mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element chứa text cần Verify",
                    "expectedText: String - Chuỗi text mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify tiêu đề trang chính xác\n" +
                    "webKeyword.verifyTextHard(pageTitleObject, \"Chào mừng đến với trang chủ\");\n\n" +
                    "// Verify kết quả tính toán với custom message\n" +
                    "webKeyword.sendKeys(number1InputObject, \"5\");\n" +
                    "webKeyword.sendKeys(number2InputObject, \"7\");\n" +
                    "webKeyword.click(calculateButtonObject);\n" +
                    "webKeyword.verifyTextHard(resultObject, \"12\", \"Kết quả phép cộng không chính xác\");\n\n" +
                    "// Verify thông tin quan trọng\n" +
                    "webKeyword.verifyTextHard(userNameObject, \"admin\", \"Tên người dùng hiển thị sai\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM và có text. " +
                    "Có thể throw AssertionError nếu text của element không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) text of {0.name} is '{1}'")
    public void verifyTextHard(ObjectUI uiObject, String expectedText, String... customMessage) {
        performTextAssertion(uiObject, expectedText, false, customMessage);
    }

    @NetatKeyword(
            name = "verifyTextSoft",
            description = "So sánh text của một element với một chuỗi ký tự mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element chứa text cần Verify",
                    "expectedText: String - Chuỗi text mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify nhãn trên form đăng nhập\n" +
                    "webKeyword.verifyTextSoft(usernameLabelObject, \"Tên đăng nhập\");\n" +
                    "webKeyword.verifyTextSoft(passwordLabelObject, \"Mật khẩu\");\n" +
                    "// Kịch bản tiếp tục ngay cả khi có nhãn không khớp\n\n" +
                    "// Verify nhiều giá trị hiển thị với custom message\n" +
                    "webKeyword.verifyTextSoft(productNameObject, \"Điện thoại thông minh X1\", \"Tên sản phẩm không đúng\");\n" +
                    "webKeyword.verifyTextSoft(productPriceObject, \"5.990.000 ₫\", \"Giá sản phẩm hiển thị sai\");\n" +
                    "webKeyword.click(addToCartButtonObject); // Tiếp tục thực hiện hành động",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM và có text. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Soft) text of {uiObject} is '{expectedText}'"
    )
//    @Step("Verify (Soft) text of {0.name} is '{1}'")
    public void verifyTextSoft(ObjectUI uiObject, String expectedText, String... customMessage) {

        execute(() -> {
                    performTextAssertion(uiObject, expectedText, true, customMessage);
                    return null;
                },
                // Params để hiện trong Allure (đừng truyền thẳng object nặng)
                uiObject != null ? uiObject.getName() : "null",
                expectedText,
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : ""
        );
    }

    @NetatKeyword(
            name = "verifyTextContainsHard",
            description = "Verify text của một element có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element chứa text cần Verify",
                    "partialText: String - Chuỗi text con mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify thông báo chào mừng có chứa tên người dùng\n" +
                    "webKeyword.verifyTextContainsHard(welcomeMessageObject, \"Xin chào\");\n\n" +
                    "// Verify kết quả tìm kiếm có chứa từ khóa đã tìm với custom message\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForElementVisible(searchResultsObject);\n" +
                    "webKeyword.verifyTextContainsHard(searchResultTitleObject, \"laptop\", \n" +
                    "    \"Kết quả tìm kiếm phải chứa từ khóa 'laptop'\");\n\n" +
                    "// Verify URL chứa thông tin cần thiết\n" +
                    "webKeyword.verifyTextContainsHard(currentUrlObject, \"/products\", \"URL phải chứa đường dẫn sản phẩm\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM và có text. " +
                    "Có thể throw AssertionError nếu text của element không chứa chuỗi con mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) text of {0.name} contains '{1}'")
    public void verifyTextContainsHard(ObjectUI uiObject, String partialText, String... customMessage) {
        performTextContainsAssertion(uiObject, partialText, false, customMessage);
    }

    @NetatKeyword(
            name = "verifyTextContainsSoft",
            description = "Verify text của một element có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element chứa text cần Verify",
                    "partialText: String - Chuỗi text con mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify kết quả tìm kiếm có chứa thông tin số lượng\n" +
                    "webKeyword.sendKeys(searchInputObject, \"điện thoại\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.verifyTextContainsSoft(searchResultSummary, \"kết quả\");\n" +
                    "webKeyword.verifyTextContainsSoft(searchResultSummary, \"điện thoại\");\n\n" +
                    "// Verify nhiều thông tin trên trang sản phẩm với custom message\n" +
                    "webKeyword.verifyTextContainsSoft(productDescriptionObject, \"chống nước\", \"Mô tả thiếu thông tin chống nước\");\n" +
                    "webKeyword.verifyTextContainsSoft(productDescriptionObject, \"bảo hành\", \"Thông tin bảo hành không được hiển thị\");\n" +
                    "webKeyword.click(addToCartButtonObject); // Tiếp tục thực hiện hành động",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM và có text. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Soft) text of {uiObject} contains '{partialText}'"
    )
    public void verifyTextContainsSoft(ObjectUI uiObject, String partialText, String... customMessage) {
        execute(() -> {
                    performTextContainsAssertion(uiObject, partialText, true, customMessage); // isSoft = true
                    return null;
                },
                uiObject != null ? uiObject.getName() : "null",
                partialText,
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : ""
        );
    }

    @NetatKeyword(
            name = "verifyElementAttributeHard",
            description = "Verify giá trị của một thuộc tính (attribute) trên element. Nếu giá trị không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "attributeName: String - Tên của thuộc tính (ví dụ: 'href', 'class', 'value')",
                    "expectedValue: String - Giá trị mong đợi của thuộc tính",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify đường dẫn của liên kết\n" +
                    "webKeyword.verifyElementAttributeHard(linkObject, \"href\", \"/products/123\");\n\n" +
                    "// Verify trạng thái của checkbox với custom message\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.verifyElementAttributeHard(termsCheckboxObject, \"checked\", \"true\", \n" +
                    "    \"Checkbox điều khoản phải được chọn\");\n\n" +
                    "// Verify thuộc tính quan trọng của form\n" +
                    "webKeyword.verifyElementAttributeHard(formObject, \"method\", \"POST\", \"Form phải sử dụng method POST\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần Verify phải tồn tại trong DOM, " +
                    "và thuộc tính cần Verify phải tồn tại trên element. " +
                    "Có thể throw AssertionError nếu giá trị thuộc tính không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) attribute '{1}' of {0.name} is '{2}'")
    public void verifyElementAttributeHard(ObjectUI uiObject, String attributeName, String expectedValue, String... customMessage) {
        performAttributeAssertion(uiObject, attributeName, expectedValue, false, customMessage);
    }

    @NetatKeyword(
            name = "verifyElementAttributeSoft",
            description = "Verify giá trị của một thuộc tính (attribute) trên element. Nếu giá trị không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "attributeName: String - Tên của thuộc tính (ví dụ: 'href', 'class', 'value')",
                    "expectedValue: String - Giá trị mong đợi của thuộc tính",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify nhiều thuộc tính của một element\n" +
                    "webKeyword.verifyElementAttributeSoft(buttonObject, \"type\", \"submit\");\n" +
                    "webKeyword.verifyElementAttributeSoft(buttonObject, \"class\", \"btn-primary\");\n" +
                    "webKeyword.click(buttonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Verify thuộc tính của nhiều element với custom message\n" +
                    "webKeyword.verifyElementAttributeSoft(usernameInputObject, \"placeholder\", \"Nhập tên đăng nhập\", \n" +
                    "    \"Placeholder của username input không đúng\");\n" +
                    "webKeyword.verifyElementAttributeSoft(passwordInputObject, \"type\", \"password\", \n" +
                    "    \"Input password phải có type là 'password'\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần Verify phải tồn tại trong DOM, " +
                    "và thuộc tính cần Verify phải tồn tại trên element. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Soft) attribute '{attributeName}' of {uiObject} is '{expectedValue}'"
    )
    public void verifyElementAttributeSoft(ObjectUI uiObject, String attributeName, String expectedValue, String... customMessage) {
        execute(() -> {
                    performAttributeAssertion(uiObject, attributeName, expectedValue, true, customMessage); // isSoft = true
                    return null;
                },
                uiObject != null ? uiObject.getName() : "null",
                attributeName,
                expectedValue,
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : ""
        );
    }

    @NetatKeyword(
            name = "verifyUrlHard",
            description = "So sánh URL của trang hiện tại với một chuỗi mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "expectedUrl: String - URL đầy đủ mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify URL sau khi đăng nhập thành công\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForUrlContains(\"/dashboard\", 10);\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/dashboard\");\n\n" +
                    "// Verify URL sau khi hoàn thành quy trình với custom message\n" +
                    "webKeyword.click(completeOrderButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(20);\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/order-confirmation\", \n" +
                    "    \"Sau khi hoàn thành đơn hàng phải chuyển đến trang xác nhận\");\n\n" +
                    "// Verify URL trong quy trình thanh toán\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/checkout\", \"URL thanh toán không chính xác\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trang web đã hoàn thành quá trình tải. " +
                    "Có thể throw AssertionError nếu URL hiện tại không khớp với URL mong đợi, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) URL of the page is '{0}'")
    public void verifyUrlHard(String expectedUrl, String... customMessage) {
        execute(() -> {
            String actualUrl = DriverManager.getDriver().getCurrentUrl();
            String messageCustom = (customMessage != null && customMessage.length > 0) ? customMessage[0] : "";
            String finalMessage = "HARD ASSERT FAILED: URL của trang không khớp. " + messageCustom;
            Assert.assertEquals(actualUrl, expectedUrl, finalMessage);
            return null;
        }, expectedUrl);
    }

    @NetatKeyword(
            name = "verifyUrlSoft",
            description = "So sánh URL của trang hiện tại với một chuỗi mong đợi. Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "expectedUrl: String - URL đầy đủ mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify URL trong quy trình nhiều bước\n" +
                    "webKeyword.click(nextButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/checkout/step1\");\n" +
                    "webKeyword.fillCheckoutForm(); // Tiếp tục quy trình ngay cả khi URL không đúng\n\n" +
                    "// Verify nhiều điều kiện bao gồm URL với custom message\n" +
                    "webKeyword.verifyElementVisibleSoft(pageHeaderObject, true);\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/products\", \"Trang sản phẩm không load đúng URL\");\n" +
                    "webKeyword.click(firstProductObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Verify URL trong flow navigation\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/profile\", \"URL trang profile không chính xác\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trang web đã hoàn thành quá trình tải. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Soft) URL of the page is '{expectedUrl}'"
    )
    public void verifyUrlSoft(String expectedUrl, String... customMessage) {
        execute(() -> {
                    String actualUrl = DriverManager.getDriver().getCurrentUrl();
                    String finalMessage = String.format(
                            "Page URL expected '%s' but was '%s'%s",
                            expectedUrl,
                            actualUrl,
                            (customMessage != null && customMessage.length > 0 && customMessage[0] != null && !customMessage[0].trim().isEmpty())
                                    ? " | " + customMessage[0]
                                    : ""
                    );
                    logger.info("Checking URL: expected='{}', actual='{}'", expectedUrl, actualUrl);

                    ExecutionContext.getInstance().getSoftAssert().assertEquals(actualUrl, expectedUrl, finalMessage);

                    return null;
                },
                expectedUrl,
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : ""
        );
    }

    @NetatKeyword(
            name = "verifyTitleHard",
            description = "Verify tiêu đề (title) của trang web hiện tại. Nếu tiêu đề không khớp chính xác, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "expectedTitle: String - Tiêu đề trang mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify tiêu đề trang chủ\n" +
                    "webKeyword.navigateToUrl(\"https://example.com\");\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyTitleHard(\"Trang chủ - Website ABC\");\n\n" +
                    "// Verify tiêu đề sau khi đăng nhập với custom message\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.verifyTitleHard(\"Dashboard - Website ABC\", \"Tiêu đề trang dashboard không chính xác\");\n\n" +
                    "// Verify tiêu đề trang sản phẩm\n" +
                    "webKeyword.verifyTitleHard(\"Chi tiết sản phẩm - iPhone 15\", \"Tiêu đề trang sản phẩm bị sai\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trang web đã hoàn thành quá trình tải. " +
                    "Có thể throw AssertionError nếu tiêu đề trang không khớp với tiêu đề mong đợi, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) page title is '{0}'")
    public void verifyTitleHard(String expectedTitle, String... customMessage) {
        execute(() -> {
            String actualTitle = DriverManager.getDriver().getTitle();
            String messageCustom = (customMessage != null && customMessage.length > 0) ? customMessage[0] : "";
            String finalMessage = "HARD ASSERT FAILED: Title not match. actual: '"+actualTitle +"' expect: '"+expectedTitle+"'." + messageCustom;
            Assert.assertEquals(actualTitle, expectedTitle, finalMessage);
            return null;
        }, expectedTitle);
    }

    @NetatKeyword(
            name = "verifyTitleSoft",
            description = "So sánh tiêu đề của trang hiện tại với một chuỗi mong đợi. Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "expectedTitle: String - Tiêu đề trang mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify tiêu đề trang giỏ hàng sau khi thêm sản phẩm\n" +
                    "webKeyword.click(addToCartButtonObject);\n" +
                    "webKeyword.click(viewCartButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyTitleSoft(\"Giỏ hàng (1 sản phẩm)\");\n" +
                    "webKeyword.click(checkoutButtonObject); // Tiếp tục quy trình thanh toán\n\n" +
                    "// Verify nhiều điều kiện trong quy trình đặt hàng với custom message\n" +
                    "webKeyword.verifyTitleSoft(\"Thanh toán - Bước 1: Thông tin giao hàng\", \n" +
                    "    \"Tiêu đề bước thanh toán không đúng\");\n" +
                    "webKeyword.verifyElementVisibleSoft(shippingFormObject, true);\n" +
                    "webKeyword.fillShippingForm(); // Tiếp tục điền form",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trang web đã hoàn thành quá trình tải. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Soft) page title is '{expectedTitle}'"
    )
    public void verifyTitleSoft(String expectedTitle, String... customMessage) {
        execute(() -> {
                    String actualTitle = DriverManager.getDriver().getTitle();

                    String finalMessage = String.format(
                            "Page title expected '%s' but was '%s'%s",
                            expectedTitle,
                            actualTitle,
                            (customMessage != null && customMessage.length > 0 && customMessage[0] != null && !customMessage[0].trim().isEmpty())
                                    ? " | " + customMessage[0].trim()
                                    : ""
                    );

                    logger.info("Checking page title: expected='{}', actual='{}'", expectedTitle, actualTitle);

                    ExecutionContext.getInstance().getSoftAssert().assertEquals(actualTitle, expectedTitle, finalMessage);

                    return null;
                },
                expectedTitle,
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : ""
        );
    }

    @NetatKeyword(
            name = "assertElementEnabled",
            description = "Khẳng định rằng một element đang ở trạng thái có thể tương tác (enabled). Nếu element bị vô hiệu hóa (disabled), kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify nút gửi form đã được kích hoạt sau khi điền đầy đủ thông tin\n" +
                    "webKeyword.sendKeys(nameInputObject, \"Nguyễn Văn A\");\n" +
                    "webKeyword.sendKeys(emailInputObject, \"nguyenvana@example.com\");\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.assertElementEnabled(submitButtonObject);\n" +
                    "webKeyword.click(submitButtonObject);\n\n" +
                    "// Verify nút thanh toán đã được kích hoạt với custom message\n" +
                    "webKeyword.click(creditCardOptionObject);\n" +
                    "webKeyword.sendKeys(cardNumberInputObject, \"1234567890123456\");\n" +
                    "webKeyword.assertElementEnabled(payNowButtonObject, \"Nút thanh toán phải được kích hoạt sau khi nhập thẻ\");\n\n" +
                    "// Verify button quan trọng phải enabled\n" +
                    "webKeyword.assertElementEnabled(confirmOrderButtonObject, \"Nút xác nhận đơn hàng bắt buộc phải hoạt động\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu element đang ở trạng thái disabled, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Hard) element '{uiObject}' is enabled"
    )
    @Step("Verify (Hard) element {0.name} is enabled")
    public void assertElementEnabled(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            performStateAssertion(uiObject, true, false, customMessage);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertElementDisabled",
            description = "Khẳng định rằng một element đang ở trạng thái không thể tương tác (disabled). Nếu element đang enabled, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify nút gửi form bị vô hiệu hóa khi chưa điền thông tin bắt buộc\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/registration\");\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.assertElementDisabled(submitButtonBeforeFillForm);\n\n" +
                    "// Verify nút thanh toán bị vô hiệu hóa với custom message\n" +
                    "webKeyword.click(checkoutButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.assertElementDisabled(paymentButtonObject, \n" +
                    "    \"Nút thanh toán phải bị vô hiệu hóa khi chưa chọn phương thức\");\n\n" +
                    "// Verify button bị khóa trong trial version\n" +
                    "webKeyword.assertElementDisabled(premiumFeatureButtonObject, \"Tính năng premium phải bị khóa\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu element đang ở trạng thái enabled, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) element {0.name} is disabled")
    public void assertElementDisabled(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            performStateAssertion(uiObject, false, false, customMessage);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "verifyElementEnabledSoft",
            description = "Verify một element có đang ở trạng thái enabled hay không. Nếu không, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify nhiều trường nhập liệu tùy chọn có thể tương tác\n" +
                    "webKeyword.verifyElementEnabledSoft(optionalFieldObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(commentFieldObject);\n" +
                    "webKeyword.sendKeys(commentFieldObject, \"Đây là bình luận của tôi\"); // Tiếp tục ngay cả khi có trường không enabled\n\n" +
                    "// Verify các nút chức năng trong trang quản trị với custom message\n" +
                    "webKeyword.verifyElementEnabledSoft(addButtonObject, \"Nút thêm mới nên được kích hoạt\");\n" +
                    "webKeyword.verifyElementEnabledSoft(editButtonObject, \"Nút chỉnh sửa nên hoạt động\");\n" +
                    "webKeyword.click(addButtonObject); // Tiếp tục thực hiện hành động",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Soft) element '{uiObject}' is enabled"
    )
//    @Step("Verify (Soft) element {0.name} is enabled")
    public void verifyElementEnabledSoft(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
                    performStateAssertion(uiObject, true, true, customMessage);
                    return null;
                },
                uiObject.getName(),
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : ""
        );
    }

    @NetatKeyword(
            name = "verifyElementDisabledSoft",
            description = "Verify một element có đang ở trạng thái disabled hay không. Nếu không, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify các tính năng bị khóa trong phiên bản dùng thử\n" +
                    "webKeyword.verifyElementDisabledSoft(lockedFeatureButton);\n" +
                    "webKeyword.verifyElementDisabledSoft(premiumFeatureButton);\n" +
                    "webKeyword.click(upgradeAccountButton); // Tiếp tục thực hiện hành động\n\n" +
                    "// Verify các trường không thể chỉnh sửa trong chế độ xem với custom message\n" +
                    "webKeyword.click(viewModeButton);\n" +
                    "webKeyword.verifyElementDisabledSoft(nameFieldInViewMode, \n" +
                    "    \"Trường tên phải bị vô hiệu hóa trong chế độ xem\");\n" +
                    "webKeyword.click(editModeButton); // Chuyển sang chế độ chỉnh sửa",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Soft) element '{uiObject}' is disabled"
    )
//    @Step("Verify (Soft) element {0.name} is disabled")
    public void verifyElementDisabledSoft(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
                    performStateAssertion(uiObject, false, true, customMessage);
                    return null;
                },
                uiObject.getName(),
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : ""
        );
    }

    @NetatKeyword(
            name = "assertElementSelected",
            description = "Khẳng định rằng một element (checkbox hoặc radio button) đang ở trạng thái được chọn. Nếu không, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element checkbox hoặc radio button cần Verify",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify checkbox \"Ghi nhớ đăng nhập\" được chọn\n" +
                    "webKeyword.click(rememberMeCheckbox);\n" +
                    "webKeyword.assertElementSelected(rememberMeCheckbox);\n" +
                    "webKeyword.click(loginButtonObject);\n\n" +
                    "// Verify radio button phương thức thanh toán được chọn với custom message\n" +
                    "webKeyword.click(creditCardRadioButton);\n" +
                    "webKeyword.assertElementSelected(creditCardRadioButton, \n" +
                    "    \"Radio button thẻ tín dụng phải được chọn\");\n" +
                    "webKeyword.sendKeys(cardNumberInputObject, \"1234567890123456\");\n\n" +
                    "// Verify checkbox điều khoản bắt buộc\n" +
                    "webKeyword.assertElementSelected(termsCheckboxObject, \"Checkbox điều khoản phải được chọn\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần Verify phải tồn tại trong DOM và là checkbox hoặc radio button. " +
                    "Có thể throw AssertionError nếu element không ở trạng thái được chọn, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc IllegalArgumentException nếu element không phải là checkbox hoặc radio button."
    )
    @Step("Verify (Hard) element {0.name} is selected")
    public void assertElementSelected(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            super.performSelectionAssertion(uiObject, true, false, customMessage);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertElementNotSelected",
            description = "Khẳng định rằng một element (checkbox hoặc radio button) đang ở trạng thái không được chọn. Nếu đang được chọn, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element checkbox hoặc radio button cần Verify",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify checkbox thông báo không được chọn mặc định\n" +
                    "webKeyword.assertElementNotSelected(newsletterCheckbox);\n" +
                    "webKeyword.click(newsletterCheckbox); // Chọn để nhận thông báo\n\n" +
                    "// Verify các tùy chọn bổ sung không được chọn với custom message\n" +
                    "webKeyword.assertElementNotSelected(expressShippingRadio, \n" +
                    "    \"Vận chuyển nhanh không nên được chọn mặc định\");\n" +
                    "webKeyword.assertElementNotSelected(giftWrappingCheckbox, \n" +
                    "    \"Gói quà không nên được chọn mặc định\");\n" +
                    "webKeyword.click(expressShippingRadio); // Chọn vận chuyển nhanh",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần Verify phải tồn tại trong DOM và là checkbox hoặc radio button. " +
                    "Có thể throw AssertionError nếu element đang ở trạng thái được chọn, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc IllegalArgumentException nếu element không phải là checkbox hoặc radio button."
    )
    @Step("Verify (Hard) element {0.name} is not selected")
    public void assertElementNotSelected(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            super.performSelectionAssertion(uiObject, false, false, customMessage);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "verifyTextMatchesRegexHard",
            description = "Verify text của một element có khớp với một biểu thức chính quy (regex) hay không. Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element chứa text cần Verify",
                    "pattern: String - Biểu thức chính quy để so khớp",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify mã đơn hàng có đúng định dạng\n" +
                    "webKeyword.click(viewOrderButtonObject);\n" +
                    "webKeyword.waitForElementVisible(orderIdObject);\n" +
                    "webKeyword.verifyTextMatchesRegexHard(orderIdObject, \"^DH-\\\\d{5}$\"); // Khớp với DH-12345\n\n" +
                    "// Verify số điện thoại có đúng định dạng với custom message\n" +
                    "webKeyword.verifyTextMatchesRegexHard(phoneNumberObject, \"^(\\\\+84|0)[0-9]{9,10}$\", \n" +
                    "    \"Số điện thoại không đúng định dạng Việt Nam\");\n\n" +
                    "// Verify email có đúng format\n" +
                    "webKeyword.verifyTextMatchesRegexHard(emailObject, \"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$\", \n" +
                    "    \"Email không đúng định dạng\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần Verify phải tồn tại trong DOM và có text. " +
                    "Có thể throw AssertionError nếu text không khớp với biểu thức chính quy, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc PatternSyntaxException nếu biểu thức chính quy không hợp lệ."
    )
    @Step("Verify (Hard) text of {0.name} matches regex '{1}'")
    public void verifyTextMatchesRegexHard(ObjectUI uiObject, String pattern, String... customMessage) {
        execute(() -> {
            performRegexAssertion(uiObject, pattern, false, customMessage);
            return null;
        }, uiObject, pattern);
    }

    @NetatKeyword(
            name = "verifyTextMatchesRegexSoft",
            description = "Verify text của một element có khớp với một biểu thức chính quy (regex) hay không. Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element chứa text cần Verify",
                    "pattern: String - Biểu thức chính quy để so khớp",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify định dạng email hiển thị trên trang\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(emailFormatObject, \"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$\");\n" +
                    "webKeyword.click(continueButtonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Verify nhiều định dạng khác nhau trên trang thông tin với custom message\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(zipCodeObject, \"^\\\\d{5}(-\\\\d{4})?$\", \n" +
                    "    \"Mã bưu điện không đúng định dạng\"); // Mã bưu điện\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(taxIdObject, \"^\\\\d{10}$\", \n" +
                    "    \"Mã số thuế phải có 10 chữ số\"); // Mã số thuế\n" +
                    "webKeyword.click(saveButtonObject); // Tiếp tục lưu thông tin",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần Verify phải tồn tại trong DOM và có text. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc PatternSyntaxException nếu biểu thức chính quy không hợp lệ.",
            explainer = "Verify (Soft) text of {uiObject} matches regex '{pattern}'"
    )
    public void verifyTextMatchesRegexSoft(ObjectUI uiObject, String pattern, String... customMessage) {
        execute(() -> {
                    performRegexAssertion(uiObject, pattern, true, customMessage);
                    return null;
                },
                uiObject.getName(),
                pattern,
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : ""
        );
    }

    @NetatKeyword(
            name = "verifyAttributeContainsHard",
            description = "Verify giá trị của một thuộc tính trên element có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "attribute: String - Tên của thuộc tính (ví dụ: 'class')",
                    "partialValue: String - Chuỗi con mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify element có class 'active'\n" +
                    "webKeyword.click(tabButtonObject);\n" +
                    "webKeyword.verifyAttributeContainsHard(tabButtonObject, \"class\", \"active\");\n\n" +
                    "// Verify đường dẫn hình ảnh chứa tên sản phẩm với custom message\n" +
                    "webKeyword.verifyAttributeContainsHard(productImageObject, \"src\", \"iphone-13\", \n" +
                    "    \"Hình ảnh sản phẩm phải chứa tên iphone-13\");\n\n" +
                    "// Verify button có class quan trọng\n" +
                    "webKeyword.verifyAttributeContainsHard(submitButtonObject, \"class\", \"btn-primary\", \n" +
                    "    \"Nút submit phải có class btn-primary\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần Verify phải tồn tại trong DOM, " +
                    "và thuộc tính cần Verify phải tồn tại trên element. " +
                    "Có thể throw AssertionError nếu giá trị thuộc tính không chứa chuỗi con mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) attribute '{1}' of {0.name} contains '{2}'")
    public void verifyAttributeContainsHard(ObjectUI uiObject, String attribute, String partialValue, String... customMessage) {
        execute(() -> {
            performAttributeContainsAssertion(uiObject, attribute, partialValue, false, customMessage);
            return null;
        }, uiObject, attribute, partialValue);
    }

    @NetatKeyword(
            name = "verifyAttributeContainsSoft",
            description = "Verify giá trị của một thuộc tính trên element có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "attribute: String - Tên của thuộc tính (ví dụ: 'class')",
                    "partialValue: String - Chuỗi con mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify thuộc tính style có chứa thông tin hiển thị\n" +
                    "webKeyword.verifyAttributeContainsSoft(elementObject, \"style\", \"display: block\");\n" +
                    "webKeyword.click(elementObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Verify nhiều thuộc tính của một element với custom message\n" +
                    "webKeyword.verifyAttributeContainsSoft(buttonObject, \"class\", \"btn\", \n" +
                    "    \"Button nên có class chứa 'btn'\");\n" +
                    "webKeyword.verifyAttributeContainsSoft(buttonObject, \"data-action\", \"submit\", \n" +
                    "    \"Button nên có data-action chứa 'submit'\");\n" +
                    "webKeyword.click(buttonObject); // Tiếp tục thực hiện hành động",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần Verify phải tồn tại trong DOM, " +
                    "và thuộc tính cần Verify phải tồn tại trên element. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt.",
            explainer = "Verify (Soft) attribute '{attribute}' of {uiObject} contains '{partialValue}'"
    )
    public void verifyAttributeContainsSoft(ObjectUI uiObject, String attribute, String partialValue, String... customMessage) {
        execute(() -> {
                    performAttributeContainsAssertion(uiObject, attribute, partialValue, true, customMessage);
                    return null;
                },
                uiObject != null ? uiObject.getName() : "null",
                attribute,
                partialValue,
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : "");
    }


    @NetatKeyword(
            name = "verifyCssValueHard",
            description = "So sánh giá trị của một thuộc tính CSS trên element. Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "cssName: String - Tên thuộc tính CSS (ví dụ: 'color')",
                    "expectedValue: String - Giá trị CSS mong đợi (ví dụ: 'rgb(255, 0, 0)')",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify màu của thông báo lỗi\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form không hợp lệ\n" +
                    "webKeyword.waitForElementVisible(errorMessageObject);\n" +
                    "webKeyword.verifyCssValueHard(errorMessageObject, \"color\", \"rgba(255, 0, 0, 1)\");\n\n" +
                    "// Verify background-color của nút đã chọn với custom message\n" +
                    "webKeyword.click(selectButtonObject);\n" +
                    "webKeyword.verifyCssValueHard(selectButtonObject, \"background-color\", \"rgba(0, 123, 255, 1)\", \n" +
                    "    \"Màu nền của nút được chọn phải là màu xanh\");\n\n" +
                    "// Verify font-size của tiêu đề\n" +
                    "webKeyword.verifyCssValueHard(titleObject, \"font-size\", \"24px\", \n" +
                    "    \"Kích thước font của tiêu đề phải là 24px\");\n\n" +
                    "// Verify display property của element ẩn\n" +
                    "webKeyword.verifyCssValueHard(hiddenElementObject, \"display\", \"none\", \n" +
                    "    \"Element phải được ẩn với display: none\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu giá trị CSS không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) CSS '{1}' of {0.name} is '{2}'")
    public void verifyCssValueHard(ObjectUI uiObject, String cssName, String expectedValue, String... customMessage) {
        execute(() -> {
            performCssValueAssertion(uiObject, cssName, expectedValue, false, customMessage);
            return null;
        }, uiObject, cssName, expectedValue);
    }

    @NetatKeyword(
            name = "verifyCssValueSoft",
            description = "So sánh giá trị của một thuộc tính CSS trên element. Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "cssName: String - Tên thuộc tính CSS (ví dụ: 'font-weight')",
                    "expectedValue: String - Giá trị CSS mong đợi (ví dụ: '700')",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify độ đậm của tiêu đề\n" +
                    "webKeyword.verifyCssValueSoft(titleObject, \"font-weight\", \"700\");\n" +
                    "webKeyword.click(nextButtonObject); // Tiếp tục quy trình\n\n" +
                    "// Verify màu nền của nút với custom message\n" +
                    "webKeyword.verifyCssValueSoft(buttonObject, \"background-color\", \"rgb(0, 123, 255)\", \n" +
                    "    \"Nút phải có màu xanh theo thiết kế\");\n" +
                    "webKeyword.click(buttonObject);\n\n" +
                    "// Verify nhiều thuộc tính CSS\n" +
                    "webKeyword.verifyCssValueSoft(headerObject, \"height\", \"60px\", \"Header phải có chiều cao 60px\");\n" +
                    "webKeyword.verifyCssValueSoft(menuObject, \"display\", \"flex\", \"Menu phải sử dụng flexbox layout\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và element cần Verify phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt. " +
                    "Giá trị CSS trả về có thể khác với giá trị trong stylesheet do browser normalization.",
            explainer = "Verify (Soft) CSS '{cssName}' of {uiObject} is '{expectedValue}'"
    )
    public void verifyCssValueSoft(ObjectUI uiObject, String cssName, String expectedValue, String... customMessage) {
        execute(() -> {
                    performCssValueAssertion(uiObject, cssName, expectedValue, true, customMessage);
                    return null;
                },
                uiObject != null ? uiObject.getName() : "null",
                cssName,
                expectedValue,
                (customMessage != null && customMessage.length > 0) ? customMessage[0] : "");
    }


    @NetatKeyword(
            name = "verifyElementNotPresentHard",
            description = "Khẳng định rằng một element không tồn tại trong DOM sau một khoảng thời gian chờ. Nếu element vẫn tồn tại, kịch bản sẽ DỪNG LẠI.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần Verify",
                    "timeoutInSeconds: int - Thời gian chờ tối đa",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify element đã bị xóa\n" +
                    "webKeyword.click(deleteButtonObject);\n" +
                    "webKeyword.verifyElementNotPresentHard(deletedItemObject, 5);\n\n" +
                    "// Verify thông báo lỗi đã biến mất với custom message\n" +
                    "webKeyword.sendKeys(emailInput, \"valid@example.com\");\n" +
                    "webKeyword.verifyElementNotPresentHard(errorMessageObject, 3, \n" +
                    "    \"Thông báo lỗi phải biến mất sau khi nhập email hợp lệ\");\n\n" +
                    "// Verify popup đã đóng\n" +
                    "webKeyword.click(closePopupButtonObject);\n" +
                    "webKeyword.verifyElementNotPresentHard(popupModalObject, 10, \n" +
                    "    \"Popup modal phải đóng sau khi click nút đóng\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw AssertionError nếu element vẫn tồn tại trong DOM sau thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify (Hard) element {0.name} is not present after {1} seconds")
    public void verifyElementNotPresentHard(ObjectUI uiObject, int timeoutInSeconds, String... customMessage) {
        execute(() -> {
            boolean isPresent = isElementPresent(uiObject, timeoutInSeconds);
            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: element '{0}' not found in {1} seconds";
            Assert.assertFalse(isPresent, message);
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "verifyOptionSelectedByLabelHard",
            description = "Khẳng định rằng option có text hiển thị (label) cụ thể đang được chọn trong dropdown.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element dropdown (thẻ select)",
                    "expectedLabel: String - text hiển thị của option mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify quốc gia đã chọn\n" +
                    "webKeyword.selectByVisibleText(countryDropdown, \"Việt Nam\");\n" +
                    "webKeyword.verifyOptionSelectedByLabelHard(countryDropdown, \"Việt Nam\");\n\n" +
                    "// Verify danh mục đã chọn với custom message\n" +
                    "webKeyword.verifyOptionSelectedByLabelHard(categoryDropdown, \"Điện thoại\", \n" +
                    "    \"Danh mục điện thoại phải được chọn\");\n\n" +
                    "// Verify trạng thái đơn hàng\n" +
                    "webKeyword.verifyOptionSelectedByLabelHard(statusDropdown, \"Đang xử lý\", \n" +
                    "    \"Trạng thái đơn hàng phải là 'Đang xử lý'\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần Verify phải là thẻ select và tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu option được chọn không khớp với option mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc UnexpectedTagNameException nếu element không phải là thẻ select."
    )
    @Step("Verify (Hard) option '{1}' được chọn trong dropdown {0.name}")
    public void verifyOptionSelectedByLabelHard(ObjectUI uiObject, String expectedLabel, String... customMessage) {
        execute(() -> {
            Select select = new Select(findElement(uiObject));
            String actualLabel = select.getFirstSelectedOption().getText();
            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: selected option '" + actualLabel + "' does not match expected '" + expectedLabel + "'.";
            Assert.assertEquals(actualLabel, expectedLabel, message);
            return null;
        }, uiObject, expectedLabel);
    }


    @NetatKeyword(
            name = "isElementPresent",
            description = "Verify xem một element có tồn tại trong DOM hay không trong một khoảng thời gian chờ nhất định. Trả về true nếu tìm thấy, false nếu không tìm thấy và không ném ra exception.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - element cần tìm kiếm",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng seconds)"
            },
            returnValue = "boolean - true nếu element tồn tại, false nếu không tồn tại",
            example = "// Verify thông báo lỗi có xuất hiện không\n" +
                    "boolean isErrorVisible = webKeyword.isElementPresent(errorMessageObject, 5);\n" +
                    "if (isErrorVisible) {\n" +
                    "    // Xử lý khi có lỗi\n" +
                    "}\n\n" +
                    "// Verify element option có tồn tại không\n" +
                    "if (webKeyword.isElementPresent(optionalElementObject, 2)) {\n" +
                    "    webKeyword.click(optionalElementObject);\n" +
                    "}",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify existence of element {0.name} trong {1} seconds")
    public boolean isElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        return execute(() -> {
            WebDriver driver = DriverManager.getDriver();
            // Sử dụng locator đầu tiên được kích hoạt để Verify
            By by = uiObject.getActiveLocators().get(0).convertToBy();

            try {
                // Tạm thời tắt implicit wait để WebDriverWait hoạt động chính xác
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
                // Chờ cho đến khi danh sách các element tìm thấy không còn rỗng
                wait.until(d -> !d.findElements(by).isEmpty());

                return true; // Tìm thấy element
            } catch (TimeoutException e) {
                return false; // Hết thời gian chờ mà không tìm thấy
            } finally {
                // Framework NETAT mặc định implicit wait is 0 nên không cần khôi phục.
            }
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "verifyAlertPresent",
            description = "Khẳng định rằng một hộp thoại alert đang hiển thị trong một khoảng thời gian chờ.",
            category = "Web",
            subCategory = "Assertion",
            parameters = {
                    "timeoutInSeconds: int - Thời gian chờ tối đa"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify alert xuất hiện sau khi xóa\n" +
                    "webKeyword.click(deleteButtonObject);\n" +
                    "webKeyword.verifyAlertPresent(5);\n" +
                    "webKeyword.acceptAlert(); // Xác nhận xóa\n\n" +
                    "// Verify alert xuất hiện khi rời trang có dữ liệu chưa lưu\n" +
                    "webKeyword.sendKeys(commentField, \"Bình luận mới\");\n" +
                    "webKeyword.click(backButtonObject);\n" +
                    "webKeyword.verifyAlertPresent(3);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trang web có thể hiển thị hộp thoại alert. " +
                    "Có thể throw AssertionError nếu không có hộp thoại alert xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Verify alert appears trong {0} seconds")
    public void verifyAlertPresent(int timeoutInSeconds) {
        execute(() -> {
            try {
                WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
                wait.until(ExpectedConditions.alertIsPresent());
            } catch (Exception e) {
                throw new AssertionError("HARD ASSERT FAILED: Alert does not appear after " + timeoutInSeconds + " seconds.");
            }
            return null;
        }, timeoutInSeconds);
    }

// =================================================================================
// --- 8. WINDOW, TAB & FRAME MANAGEMENT ---
// =================================================================================

    @NetatKeyword(
            name = "switchToWindowByTitle",
            description = "Duyệt qua tất cả các cửa sổ hoặc tab đang mở và chuyển sự điều khiển of WebDriver sang cửa sổ/tab có tiêu đề khớp chính xác với chuỗi được cung cấp.",
            category = "Web",
            subCategory = "Window&Frame",
            parameters = {
                    "windowTitle: String - Tiêu đề chính xác of cửa sổ hoặc tab cần chuyển đến"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chuyển sang tab chi tiết sản phẩm\n" +
                    "webKeyword.click(viewDetailsLinkObject); // Mở tab mới\n" +
                    "webKeyword.switchToWindowByTitle(\"Chi tiết sản phẩm ABC\");\n\n" +
                    "// Chuyển sang tab thanh toán\n" +
                    "webKeyword.click(checkoutButtonObject);\n" +
                    "webKeyword.switchToWindowByTitle(\"Thanh toán đơn hàng\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "có ít nhất một cửa sổ/tab đang mở với tiêu đề cần chuyển đến. " +
                    "Có thể throw NoSuchWindowException nếu không tìm thấy cửa sổ nào có tiêu đề được chỉ định, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Switch to window with title: {0}")
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
            throw new NoSuchWindowException("Cannot find window with title: " + windowTitle);
        }, windowTitle);
    }

    @NetatKeyword(
            name = "switchToWindowByIndex",
            description = "Chuyển sự điều khiển of WebDriver sang một tab hoặc cửa sổ khác dựa trên chỉ số (index) of nó (bắt đầu từ 0).",
            category = "Web",
            subCategory = "Window&Frame",
            parameters = {
                    "index: int - Chỉ số of cửa sổ/tab cần chuyển đến (0 is cửa sổ đầu tiên)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Mở liên kết trong tab mới và chuyển sang tab đó\n" +
                    "webKeyword.rightClickAndSelect(productLinkObject, \"Mở trong tab mới\");\n" +
                    "webKeyword.switchToWindowByIndex(1); // Chuyển sang tab thứ hai\n\n" +
                    "// Quay lại tab chính sau khi hoàn thành\n" +
                    "webKeyword.switchToWindowByIndex(0);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "có đủ số lượng cửa sổ/tab đang mở để chuyển đến chỉ số được chỉ định. " +
                    "Có thể throw IndexOutOfBoundsException nếu chỉ số nằm ngoài phạm vi of số lượng cửa sổ/tab đang mở, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Switch to window at position {0}")
    public void switchToWindowByIndex(int index) {
        execute(() -> {
            ArrayList<String> tabs = new ArrayList<>(DriverManager.getDriver().getWindowHandles());
            if (index < 0 || index >= tabs.size()) {
                throw new IndexOutOfBoundsException("Invalid index: " + index + ". The list contains only " + tabs.size() + " element(s).");
            }
            DriverManager.getDriver().switchTo().window(tabs.get(index));
            return null;
        }, index);
    }

    @NetatKeyword(
            name = "switchToFrame",
            description = "Chuyển sự điều khiển of WebDriver vào một element iframe trên trang. Mọi hành động sau đó sẽ được thực hiện trong ngữ cảnh of iframe này.",
            category = "Web",
            subCategory = "Window&Frame",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện đại diện cho thẻ iframe cần chuyển vào"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chuyển vào iframe thanh toán\n" +
                    "webKeyword.waitForElementVisible(paymentIframeObject);\n" +
                    "webKeyword.switchToFrame(paymentIframeObject);\n" +
                    "webKeyword.sendKeys(cardNumberObject, \"4111111111111111\");\n\n" +
                    "// Chuyển vào iframe trình soạn thảo\n" +
                    "webKeyword.switchToFrame(richTextEditorObject);\n" +
                    "webKeyword.sendKeys(editorBodyObject, \"Nội dung bài viết\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element iframe cần chuyển vào phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element iframe, " +
                    "StaleElementReferenceException nếu element iframe không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Switch to iframe: {0.name}")
    public void switchToFrame(ObjectUI uiObject) {
        execute(() -> {
            WebElement frameElement = findElement(uiObject);
            DriverManager.getDriver().switchTo().frame(frameElement);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "switchToParentFrame",
            description = "Thoát khỏi ngữ cảnh iframe hiện tại và Switch to parent frame ngay trước nó. Nếu đang ở iframe cấp cao nhất, hành động này sẽ quay về nội dung chính of trang.",
            category = "Web",
            subCategory = "Window&Frame",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Thoát khỏi iframe con và Switch to parent frame\n" +
                    "webKeyword.switchToFrame(mainIframeObject);\n" +
                    "webKeyword.switchToFrame(nestedIframeObject);\n" +
                    "webKeyword.switchToParentFrame(); // Switch to parent frame\n" +
                    "webKeyword.click(nextButtonObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "WebDriver đang ở trong ngữ cảnh of một iframe. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Switch to parent frame")
    public void switchToParentFrame() {
        execute(() -> {
            DriverManager.getDriver().switchTo().parentFrame();
            return null;
        });
    }

    @NetatKeyword(
            name = "switchToDefaultContent",
            description = "Chuyển sự điều khiển of WebDriver ra khỏi tất cả các iframe và quay về nội dung chính, cấp cao nhất of trang web.",
            category = "Web",
            subCategory = "Window&Frame",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Thoát khỏi iframe và quay về nội dung chính\n" +
                    "webKeyword.switchToFrame(paymentIframeObject);\n" +
                    "webKeyword.sendKeys(cardNumberObject, \"4111111111111111\");\n" +
                    "webKeyword.switchToDefaultContent(); // Quay về nội dung chính\n" +
                    "webKeyword.waitForElementVisible(confirmationMessageObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Switch to default content")
    public void switchToDefaultContent() {
        execute(() -> {
            DriverManager.getDriver().switchTo().defaultContent();
            return null;
        });
    }

    @NetatKeyword(
            name = "openNewTab",
            description = "Mở một tab mới trong trình duyệt và tự động chuyển sự điều khiển sang tab mới đó. Có thể option mở một URL cụ thể trong tab mới.",
            category = "Web",
            subCategory = "Window&Frame",
            parameters = {
                    "url: String - (option) URL để mở trong tab mới. Nếu để trống, sẽ mở tab trống"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Mở tab mới với URL cụ thể\n" +
                    "webKeyword.openNewTab(\"https://google.com\");\n" +
                    "webKeyword.waitForElementVisible(searchBoxObject);\n\n" +
                    "// Mở tab trống và điều hướng sau đó\n" +
                    "webKeyword.openNewTab(\"\");\n" +
                    "webKeyword.navigate(\"https://example.com\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trình duyệt hỗ trợ việc mở tab mới thông qua WebDriver. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc UnsupportedCommandException nếu trình duyệt không hỗ trợ lệnh mở tab mới."
    )
    @Step("Open new tab with URL: {0}")
    public void openNewTab(String url) {
        execute(() -> {
            DriverManager.getDriver().switchTo().newWindow(WindowType.TAB);
            if (url != null && !url.isEmpty()) {
                DriverManager.getDriver().get(url);
            }
            return null;
        }, url);
    }


    @NetatKeyword(
            name = "clickAndSwitchToNewTab",
            description = "Click vào một element (thường is link) và tự động chuyển sự điều khiển sang tab/cửa sổ mới vừa được mở ra.",
            category = "Web",
            subCategory = "Window&Frame",
            parameters = {
                    "uiObject: ObjectUI - element link cần click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click vào liên kết mở trong tab mới\n" +
                    "webKeyword.clickAndSwitchToNewTab(externalLinkObject);\n" +
                    "webKeyword.waitForPageLoaded();\n\n" +
                    "// Click vào nút xem chi tiết sản phẩm\n" +
                    "webKeyword.clickAndSwitchToNewTab(viewDetailsButtonObject);\n" +
                    "webKeyword.verifyElementVisibleHard(productSpecificationsObject, 10);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần click phải tồn tại trong DOM và có khả năng mở tab mới (ví dụ: có thuộc tính target='_blank'). " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "TimeoutException nếu tab mới không mở trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Click and switch to new tab from element: {0.name}")
    public void clickAndSwitchToNewTab(ObjectUI uiObject) {
        execute(() -> {
            WebDriver driver = DriverManager.getDriver();
            String originalHandle = driver.getWindowHandle();

            findElement(uiObject).click();

            // Chờ cho đến khi có cửa sổ mới xuất hiện
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ExpectedConditions.numberOfWindowsToBe(2));

            for (String handle : driver.getWindowHandles()) {
                if (!handle.equals(originalHandle)) {
                    driver.switchTo().window(handle);
                    break;
                }
            }
            return null;
        }, uiObject);
    }


// =================================================================================
// --- 9. ALERT & POPUP HANDLING ---
// =================================================================================

    @NetatKeyword(
            name = "getAlertText",
            description = "Chờ cho đến khi một hộp thoại alert, prompt, hoặc confirm of trình duyệt xuất hiện và lấy về nội dung text of nó.",
            category = "Web",
            subCategory = "Alert",
            parameters = {},
            returnValue = "String - Nội dung text of hộp thoại alert",
            example = "// Lấy và Verify nội dung thông báo xác nhận\n" +
                    "webKeyword.click(deleteButtonObject);\n" +
                    "String alertMessage = webKeyword.getAlertText();\n" +
                    "if (alertMessage.contains(\"Bạn có chắc chắn muốn xóa?\")) {\n" +
                    "    webKeyword.acceptAlert();\n" +
                    "}\n\n" +
                    "// Lấy thông báo lỗi từ alert\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "String errorMessage = webKeyword.getAlertText();\n" +
                    "logger.info(\"Error message: \" + errorMessage);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "một hộp thoại alert đang hiển thị hoặc sẽ xuất hiện. " +
                    "Có thể throw TimeoutException nếu không có hộp thoại alert xuất hiện trong thời gian chờ, " +
                    "NoAlertPresentException nếu không có hộp thoại alert đang hiển thị, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Get text from alert")
    public String getAlertText() {
        return execute(() -> {
            Alert alert = new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.alertIsPresent());
            return alert.getText();
        });
    }

    @NetatKeyword(
            name = "sendKeysToAlert",
            description = "Chờ cho đến khi một hộp thoại prompt of trình duyệt xuất hiện và nhập một chuỗi text vào đó.",
            category = "Web",
            subCategory = "Alert",
            parameters = {
                    "text: String - Chuỗi text cần nhập vào hộp thoại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Nhập tên người dùng vào hộp thoại prompt\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.sendKeysToAlert(\"Nguyễn Văn A\");\n" +
                    "webKeyword.acceptAlert();\n\n" +
                    "// Nhập lý do hủy đơn hàng\n" +
                    "webKeyword.click(cancelOrderButtonObject);\n" +
                    "webKeyword.sendKeysToAlert(\"Thay đổi thông tin giao hàng\");\n" +
                    "webKeyword.acceptAlert();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "một hộp thoại prompt đang hiển thị hoặc sẽ xuất hiện. " +
                    "Có thể throw TimeoutException nếu không có hộp thoại alert xuất hiện trong thời gian chờ, " +
                    "NoAlertPresentException nếu không có hộp thoại alert đang hiển thị, " +
                    "ElementNotInteractableException nếu hộp thoại không phải is prompt và không cho phép nhập liệu, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Enter text '{0}' into prompt")
    public void sendKeysToAlert(String text) {
        execute(() -> {
            Alert alert = new WebDriverWait(DriverManager.getDriver(), DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.alertIsPresent());
            alert.sendKeys(text);
            return null;
        }, text);
    }

    // =================================================================================
    // --- 11. COOKIES & STORAGE KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "findElementInShadowDom",
            description = "Tìm kiếm và trả về một element nằm bên trong một Shadow DOM. Yêu cầu cung cấp element chủ (shadow host) và một CSS selector để định vị element con.",
            category = "Web",
            subCategory = "Interaction",
            parameters = {
                    "shadowHostObject: ObjectUI - element chủ (host) contains Shadow DOM",
                    "cssSelectorInShadow: String - Chuỗi CSS selector để Find element inside Shadow DOM"
            },
            returnValue = "WebElement - element web được tìm thấy inside Shadow DOM",
            example = "// Tìm và tương tác với element input trong Shadow DOM\n" +
                    "WebElement usernameInput = webKeyword.findElementInShadowDom(appContainerObject, \"#username\");\n" +
                    "usernameInput.sendKeys(\"admin@example.com\");\n\n" +
                    "// Tìm và click vào nút trong Shadow DOM\n" +
                    "WebElement submitButton = webKeyword.findElementInShadowDom(loginFormObject, \".submit-button\");\n" +
                    "submitButton.click();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trình duyệt hỗ trợ Shadow DOM (Chrome, Firefox mới), " +
                    "element chủ (host) phải tồn tại và có Shadow DOM đính kèm. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element chủ hoặc element con, " +
                    "StaleElementReferenceException nếu element chủ không còn gắn với DOM, " +
                    "UnsupportedOperationException nếu trình duyệt không hỗ trợ Shadow DOM API, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Find element '{1}' inside Shadow DOM of {0.name}")
    public WebElement findElementInShadowDom(ObjectUI shadowHostObject, String cssSelectorInShadow) {
        return execute(() -> {
            WebElement shadowHost = findElement(shadowHostObject);
            SearchContext shadowRoot = shadowHost.getShadowRoot();
            return shadowRoot.findElement(By.cssSelector(cssSelectorInShadow));
        }, shadowHostObject, cssSelectorInShadow);
    }

    @NetatKeyword(
            name = "setLocalStorage",
            description = "Ghi một cặp khóa-giá trị vào Local Storage of trình duyệt. Hữu ích để thiết lập trạng thái ứng dụng hoặc token.",
            category = "Web",
            subCategory = "Storage",
            parameters = {
                    "key: String - Khóa (key) để lưu trữ",
                    "value: String - Giá trị (value) tương ứng"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Thiết lập token xác thực người dùng\n" +
                    "webKeyword.setLocalStorage(\"user_token\", \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\");\n" +
                    "webKeyword.navigate(\"https://example.com/dashboard\");\n\n" +
                    "// Lưu trạng thái giỏ hàng\n" +
                    "webKeyword.setLocalStorage(\"cart_items\", \"[{\\\"id\\\":123,\\\"quantity\\\":2}]\");\n" +
                    "webKeyword.refreshPage();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trang web đã được tải hoàn toàn, trình duyệt hỗ trợ Local Storage. " +
                    "Có thể throw JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Write to Local Storage: key='{0}', value='{1}'")
    public void setLocalStorage(String key, String value) {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("localStorage.setItem(arguments[0], arguments[1]);", key, value);
            return null;
        }, key, value);
    }

    @NetatKeyword(
            name = "getLocalStorage",
            description = "Đọc và trả về giá trị từ Local Storage of trình duyệt dựa trên một khóa (key) được cung cấp.",
            category = "Web",
            subCategory = "Storage",
            parameters = {
                    "key: String - Khóa (key) of giá trị cần đọc"
            },
            returnValue = "String - Giá trị được lưu trữ trong Local Storage với khóa đã chỉ định, hoặc null nếu không tìm thấy",
            example = "// Verify token xác thực\n" +
                    "String userToken = webKeyword.getLocalStorage(\"user_token\");\n" +
                    "if (userToken == null || userToken.isEmpty()) {\n" +
                    "    webKeyword.navigate(\"https://example.com/login\");\n" +
                    "}\n\n" +
                    "// Đọc thông tin giỏ hàng\n" +
                    "String cartItems = webKeyword.getLocalStorage(\"cart_items\");\n" +
                    "logger.info(\"Số lượng sản phẩm trong giỏ: \" + cartItems);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trang web đã được tải hoàn toàn, trình duyệt hỗ trợ Local Storage. " +
                    "Có thể throw JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Read from Local Storage with key='{0}'")
    public String getLocalStorage(String key) {
        return execute(() -> (String) ((JavascriptExecutor) DriverManager.getDriver()).executeScript("return localStorage.getItem(arguments[0]);", key), key);
    }

    @NetatKeyword(
            name = "clearLocalStorage",
            description = "Xóa toàn bộ dữ liệu đang được lưu trữ trong Local Storage of trang web hiện tại.",
            category = "Web",
            subCategory = "Storage",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Đăng xuất và xóa dữ liệu người dùng\n" +
                    "webKeyword.click(logoutButtonObject);\n" +
                    "webKeyword.clearLocalStorage();\n" +
                    "webKeyword.navigate(\"https://example.com/login\");\n\n" +
                    "// Xóa dữ liệu trước khi chạy kiểm thử\n" +
                    "webKeyword.navigate(\"https://example.com\");\n" +
                    "webKeyword.clearLocalStorage();\n" +
                    "webKeyword.refreshPage();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trang web đã được tải hoàn toàn, trình duyệt hỗ trợ Local Storage. " +
                    "Có thể throw JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Clear all Local Storage")
    public void clearLocalStorage() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("localStorage.clear();");
            return null;
        });
    }

    @NetatKeyword(
            name = "deleteAllCookies",
            description = "Xóa tất cả các cookie of phiên làm việc hiện tại trên trình duyệt.",
            category = "Web",
            subCategory = "Storage",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Đăng xuất và xóa cookies\n" +
                    "webKeyword.click(logoutButtonObject);\n" +
                    "webKeyword.deleteAllCookies();\n" +
                    "webKeyword.navigate(\"https://example.com/login\");\n\n" +
                    "// Thiết lập lại trạng thái trình duyệt\n" +
                    "webKeyword.deleteAllCookies();\n" +
                    "webKeyword.clearLocalStorage();\n" +
                    "webKeyword.navigate(\"https://example.com\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Delete all cookies")
    public void deleteAllCookies() {
        execute(() -> {
            DriverManager.getDriver().manage().deleteAllCookies();
            return null;
        });
    }

    @NetatKeyword(
            name = "getCookie",
            description = "Lấy thông tin of một cookie cụ thể dựa trên tên of nó.",
            category = "Web",
            subCategory = "Storage",
            parameters = {
                    "cookieName: String - Tên of cookie cần lấy"
            },
            returnValue = "Cookie - Đối tượng Cookie contains thông tin of cookie được yêu cầu, hoặc null nếu không tìm thấy",
            example = "// Verify cookie phiên làm việc\n" +
                    "Cookie sessionCookie = webKeyword.getCookie(\"session_id\");\n" +
                    "if (sessionCookie == null) {\n" +
                    "    webKeyword.navigate(\"https://example.com/login\");\n" +
                    "} else {\n" +
                    "    logger.info(\"Phiên làm việc: \" + sessionCookie.getValue());\n" +
                    "}\n\n" +
                    "// Verify thời hạn cookie\n" +
                    "Cookie authCookie = webKeyword.getCookie(\"auth_token\");\n" +
                    "logger.info(\"Cookie hết hạn vào: \" + authCookie.getExpiry());",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trang web đã được tải hoàn toàn. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Get cookie with name: {0}")
    public Cookie getCookie(String cookieName) {
        return execute(() -> DriverManager.getDriver().manage().getCookieNamed(cookieName), cookieName);
    }


    // =================================================================================
    // --- 12. UTILITY & SUPPORT KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "takeScreenshot",
            description = "Chụp lại ảnh toàn bộ màn hình (viewport) of trình duyệt và lưu vào một file tại đường dẫn được chỉ định.",
            category = "Web",
            subCategory = "Utility",
            parameters = {
                    "filePath: String - Đường dẫn đầy đủ để lưu file ảnh (ví dụ: 'C:/screenshots/error.png')"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chụp ảnh màn hình khi gặp lỗi\n" +
                    "try {\n" +
                    "    webKeyword.click(submitButtonObject);\n" +
                    "    webKeyword.verifyElementVisibleHard(confirmationMessageObject, 5);\n" +
                    "} catch (Exception e) {\n" +
                    "    webKeyword.takeScreenshot(\"D:/screenshots/error.png\");\n" +
                    "    throw e;\n" +
                    "}\n\n" +
                    "// Chụp ảnh màn hình để lưu trữ trạng thái\n" +
                    "webKeyword.waitForPageLoaded();\n" +
                    "webKeyword.takeScreenshot(\"D:/screenshots/homepage.png\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "thư mục đích phải tồn tại hoặc có quyền tạo thư mục. " +
                    "Có thể throw RuntimeException nếu không thể chụp hoặc lưu ảnh màn hình, " +
                    "IOException nếu có lỗi khi ghi file, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Take screenshot and save at: {0}")
    public void takeScreenshot(String filePath) {
        execute(() -> {
            try {
                WebDriver driver = ExecutionContext.getInstance().getWebDriver();
                if (driver==null) driver = SessionManager.getInstance().getCurrentDriver();
                if (driver instanceof TakesScreenshot) {
                    File scrFile = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.FILE);
                    FileUtils.copyFile(scrFile, new File(filePath));
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to capture or save the screenshot.", e);
            }
            return null;
        }, filePath);
    }

    @NetatKeyword(
            name = "takeElementScreenshot",
            description = "Chụp ảnh chỉ riêng một element cụ thể trên trang và lưu vào file tại đường dẫn được chỉ định.",
            category = "Web",
            subCategory = "Utility",
            parameters = {
                    "uiObject: ObjectUI - element cần chụp ảnh",
                    "filePath: String - Đường dẫn đầy đủ để lưu file ảnh"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Take element screenshot để Verify hiển thị\n" +
                    "webKeyword.waitForElementVisible(loginFormObject, 10);\n" +
                    "webKeyword.takeElementScreenshot(loginFormObject, \"D:/screenshots/login_form.png\");\n\n" +
                    "// Take element screenshot khi gặp lỗi hiển thị\n" +
                    "if (!webKeyword.verifyElementTextContains(errorMessageObject, \"Invalid credentials\")) {\n" +
                    "    webKeyword.takeElementScreenshot(errorMessageObject, \"D:/screenshots/error.png\");\n" +
                    "}",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần chụp phải hiển thị trên màn hình, " +
                    "thư mục đích phải tồn tại hoặc có quyền tạo thư mục. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "ElementNotVisibleException nếu element không hiển thị, " +
                    "RuntimeException nếu không thể chụp hoặc lưu ảnh element, " +
                    "IOException nếu có lỗi khi ghi file, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Take element screenshot {0.name} and save at: {1}")
    public void takeElementScreenshot(ObjectUI uiObject, String filePath) {
        execute(() -> {
            try {
                File scrFile = findElement(uiObject).getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(scrFile, new File(filePath));
            } catch (IOException e) {
                throw new RuntimeException("Unable to capture or save the element screenshot.", e);
            }
            return null;
        }, uiObject, filePath);
    }

    @NetatKeyword(
            name = "highlightElement",
            description = "Tạm thời vẽ một đường viền màu đỏ xung quanh một element trên trang để dễ dàng nhận biết và gỡ lỗi trong quá trình chạy kịch bản.",
            category = "Web",
            subCategory = "Utility",
            parameters = {
                    "uiObject: ObjectUI - element cần làm nổi bật"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Làm nổi bật các element trong quá trình điền form\n" +
                    "webKeyword.highlightElement(usernameFieldObject);\n" +
                    "webKeyword.sendKeys(usernameFieldObject, \"admin@example.com\");\n" +
                    "webKeyword.highlightElement(passwordFieldObject);\n" +
                    "webKeyword.sendKeys(passwordFieldObject, \"password123\");\n\n" +
                    "// Làm nổi bật element để gỡ lỗi\n" +
                    "webKeyword.waitForElementVisible(tableRowObject, 10);\n" +
                    "webKeyword.highlightElement(tableRowObject);\n" +
                    "webKeyword.takeElementScreenshot(tableRowObject, \"D:/screenshots/table_row.png\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "element cần làm nổi bật phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy element, " +
                    "StaleElementReferenceException nếu element không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Highlight element: {0.name}")
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
            category = "Web",
            subCategory = "Utility",
            parameters = {
                    "milliseconds: int - Thời gian cần tạm dừng (tính bằng mili seconds)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Tạm dừng để đợi animation hoàn thành\n" +
                    "webKeyword.click(expandMenuButtonObject);\n" +
                    "webKeyword.pause(1000); // Đợi 1 seconds cho animation menu mở ra\n" +
                    "webKeyword.click(menuItemObject);\n\n" +
                    "// Tạm dừng để đợi dữ liệu được xử lý\n" +
                    "webKeyword.click(generateReportButtonObject);\n" +
                    "webKeyword.pause(3000); // Đợi 3 seconds cho quá trình xử lý\n" +
                    "webKeyword.verifyElementVisibleHard(reportResultObject, 10);",
            note = "Áp dụng cho nền tảng Web. Không có điều kiện tiên quyết đặc biệt. " +
                    "Có thể throw InterruptedException nếu luồng thực thi bị gián đoạn trong khi tạm dừng."
    )
    @Step("Pause for {0} ms")
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
