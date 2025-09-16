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

    @NetatKeyword(
            name = "findElements",
            description = "Tìm và trả về một danh sách (List) tất cả các phần tử WebElement khớp với locator được cung cấp. " +
                    "Trả về danh sách rỗng nếu không tìm thấy, không ném ra exception.",
            category = "Web/Finder",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện đại diện cho các phần tử cần tìm"
            },
            returnValue = "List<WebElement> - Danh sách các phần tử web khớp với locator, hoặc danh sách rỗng nếu không tìm thấy phần tử nào",
            example = "// Lấy danh sách tất cả các sản phẩm\n" +
                    "List<WebElement> productList = webKeyword.findElements(productListItemObject);\n\n" +
                    "// Đếm số lượng kết quả tìm kiếm\n" +
                    "int resultCount = webKeyword.findElements(searchResultObject).size();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và đối tượng ObjectUI phải có ít nhất một locator được định nghĩa. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "InvalidSelectorException nếu locator không hợp lệ, " +
                    "hoặc NullPointerException nếu uiObject là null hoặc không có locator nào được kích hoạt."
    )
    @Step("Tìm danh sách các phần tử: {0.name}")
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
            category = "Web/Browser",
            parameters = {
                    "url: String - Địa chỉ trang web đầy đủ cần mở (ví dụ: 'https://www.google.com')"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Mở trang chủ Google\n" +
                    "webKeyword.openUrl(\"https://www.google.com\");\n\n" +
                    "// Mở trang đăng nhập\n" +
                    "webKeyword.openUrl(\"https://example.com/login\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "URL phải là một địa chỉ hợp lệ và có thể truy cập được, và kết nối mạng phải hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "InvalidArgumentException nếu URL không hợp lệ, " +
                    "hoặc TimeoutException nếu trang không tải trong thời gian chờ mặc định."
    )
    @Step("Mở URL: {0}")
    public void openUrl(String url) {
        DriverManager.getDriver().get(url);
    }

    @NetatKeyword(
            name = "goBack",
            description = "Thực hiện hành động quay lại trang trước đó trong lịch sử của trình duyệt, " +
                    "tương đương với việc người dùng nhấn nút 'Back'.",
            category = "Web/Browser",
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
                    "và phải có ít nhất một trang đã được truy cập trước đó trong lịch sử của phiên hiện tại. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
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
            description = "Thực hiện hành động đi tới trang tiếp theo trong lịch sử của trình duyệt, " +
                    "tương đương với việc người dùng nhấn nút 'Forward'.",
            category = "Web/Browser",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Điều hướng qua lại giữa các trang\n" +
                    "webKeyword.openUrl(\"https://example.com/page1\");\n" +
                    "webKeyword.openUrl(\"https://example.com/page2\");\n" +
                    "webKeyword.goBack(); // Quay lại page1\n" +
                    "webKeyword.goForward(); // Tiến tới page2 lần nữa\n\n" +
                    "// Kiểm tra luồng điều hướng\n" +
                    "webKeyword.click(productLink);\n" +
                    "webKeyword.goBack();\n" +
                    "webKeyword.goForward();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phải đã sử dụng goBack() hoặc có trang tiếp theo trong lịch sử điều hướng. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
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
            description = "Tải lại (làm mới) trang web hiện tại đang hiển thị trên trình duyệt. " +
                    "Tương đương với việc người dùng nhấn phím F5 hoặc nút 'Reload'.",
            category = "Web/Browser",
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
            category = "Web/Browser",
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
            category = "Web/Browser",
            parameters = {
                    "width: int - Chiều rộng mới của cửa sổ (pixel)",
                    "height: int - Chiều cao mới của cửa sổ (pixel)"
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
                    "hoặc IllegalArgumentException nếu chiều rộng hoặc chiều cao là số âm."
    )
    @Step("Thay đổi kích thước cửa sổ thành {0}x{1}")
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
            description = "Thực hiện hành động click chuột vào một phần tử trên giao diện. " +
                    "Keyword sẽ tự động chờ cho đến khi phần tử sẵn sàng để được click.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện (nút bấm, liên kết,...) cần thực hiện hành động click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click vào nút đăng nhập\n" +
                    "webKeyword.click(loginButtonObject);\n\n" +
                    "// Click vào liên kết\n" +
                    "webKeyword.click(registerLinkObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần click phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và không bị che khuất bởi các phần tử khác. " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementClickInterceptedException nếu phần tử bị che khuất bởi phần tử khác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Ô input hoặc textarea cần nhập dữ liệu",
                    "text: String - Chuỗi văn bản cần nhập vào phần tử"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Nhập tên đăng nhập\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"my_username\");\n\n" +
                    "// Nhập nội dung tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop gaming\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần nhập liệu phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và phải là loại có thể nhập liệu (input, textarea, contenteditable). " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Nhập văn bản '{1}' vào phần tử: {0.name}")
    public void sendKeys(ObjectUI uiObject, String text) {
        super.sendKeys(uiObject, text);
    }

    @NetatKeyword(
            name = "clearText",
            description = "Xóa toàn bộ văn bản đang có trong một phần tử có thể nhập liệu như ô input hoặc textarea.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần xóa nội dung"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Xóa nội dung trong ô tìm kiếm\n" +
                    "webKeyword.clearText(searchInputObject);\n\n" +
                    "// Xóa nội dung trước khi nhập dữ liệu mới\n" +
                    "webKeyword.clearText(usernameInputObject);\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"new_username\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần xóa nội dung phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và phải là loại có thể nhập liệu (input, textarea, contenteditable). " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            description = "Kiểm tra và đảm bảo một checkbox hoặc radio button đang ở trạng thái được chọn. " +
                    "Nếu phần tử chưa được chọn, keyword sẽ thực hiện click để chọn nó.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử checkbox hoặc radio button cần kiểm tra và chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Đảm bảo checkbox Điều khoản và Điều kiện đã được chọn\n" +
                    "webKeyword.check(termsAndConditionsCheckbox);\n\n" +
                    "// Chọn phương thức thanh toán\n" +
                    "webKeyword.check(creditCardRadioButton);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần chọn phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và phải là checkbox hoặc radio button (input type=\"checkbox\" hoặc type=\"radio\"). " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            description = "Kiểm tra và đảm bảo một checkbox đang ở trạng thái không được chọn. " +
                    "Nếu phần tử đang được chọn, keyword sẽ thực hiện click để bỏ chọn nó.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử checkbox cần bỏ chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Bỏ chọn checkbox đăng ký nhận bản tin\n" +
                    "webKeyword.uncheck(newsletterCheckbox);\n\n" +
                    "// Bỏ chọn tùy chọn gửi hàng nhanh\n" +
                    "webKeyword.uncheck(expressShippingCheckbox);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần bỏ chọn phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "phải là checkbox (input type=\"checkbox\"), và lưu ý phương thức này chỉ hoạt động với checkbox, không dùng cho radio button. " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần thực hiện hành động click chuột phải"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click chuột phải vào biểu tượng file\n" +
                    "webKeyword.contextClick(fileIconObject);\n" +
                    "webKeyword.waitForElementVisible(contextMenuObject);\n\n" +
                    "// Click chuột phải vào hình ảnh để tải xuống\n" +
                    "webKeyword.contextClick(productImageObject);\n" +
                    "webKeyword.click(saveImageOptionObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần click phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "không bị che khuất bởi các phần tử khác, và trình duyệt phải hỗ trợ thao tác chuột phải " +
                    "(một số trình duyệt di động có thể không hỗ trợ). " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu phần tử nằm ngoài viewport hiện tại."
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
            name = "doubleClick",
            description = "Thực hiện hành động click chuột hai lần (double-click) vào một phần tử.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần thực hiện double-click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Double-click vào biểu tượng chỉnh sửa\n" +
                    "webKeyword.doubleClick(editIconObject);\n" +
                    "webKeyword.waitForElementVisible(editFormObject);\n\n" +
                    "// Double-click để chọn toàn bộ văn bản\n" +
                    "webKeyword.doubleClick(textParagraphObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần double-click phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "và không bị che khuất bởi các phần tử khác. " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu phần tử nằm ngoài viewport hiện tại."
    )
    @Step("Double-click vào phần tử: {0.name}")
    public void doubleClick(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new Actions(DriverManager.getDriver()).doubleClick(element).perform();
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "hover",
            description = "Di chuyển con trỏ chuột đến vị trí của một phần tử để hiển thị các menu con, tooltip, hoặc các hiệu ứng khác.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần di chuột đến"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Di chuột đến menu chính để hiển thị menu con\n" +
                    "webKeyword.hover(mainMenuObject);\n" +
                    "webKeyword.waitForElementVisible(subMenuObject);\n\n" +
                    "// Di chuột đến biểu tượng để hiển thị tooltip\n" +
                    "webKeyword.hover(infoIconObject);\n" +
                    "webKeyword.waitForElementVisible(tooltipObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần hover phải tồn tại trong DOM, phải hiển thị trên trang, " +
                    "và trình duyệt phải hỗ trợ thao tác di chuột (một số trình duyệt di động có thể không hỗ trợ). " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu phần tử nằm ngoài viewport hiện tại."
    )
    @Step("Di chuột đến phần tử: {0.name}")
    public void hover(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            new Actions(DriverManager.getDriver()).moveToElement(element).perform();
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "uploadFile",
            description = "Tải lên một file từ máy local bằng cách gửi đường dẫn file vào một phần tử <input type='file'>.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử input (type='file') để tải file lên",
                    "filePath: String - Đường dẫn tuyệt đối đến file cần tải lên trên máy"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Tải lên ảnh đại diện\n" +
                    "webKeyword.uploadFile(avatarUploadInput, \"C:/Users/Tester/Pictures/avatar.jpg\");\n\n" +
                    "// Tải lên tài liệu PDF\n" +
                    "webKeyword.uploadFile(documentUploadInput, \"D:/Documents/report.pdf\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử input phải có thuộc tính type='file', phải tồn tại trong DOM (có thể ẩn nhưng phải tồn tại), " +
                    "file cần tải lên phải tồn tại tại đường dẫn được chỉ định, " +
                    "người dùng thực thi test phải có quyền truy cập vào file, và đường dẫn file phải là đường dẫn tuyệt đối. " +
                    "Có thể throw InvalidArgumentException nếu đường dẫn file không hợp lệ hoặc file không tồn tại, " +
                    "ElementNotInteractableException nếu phần tử không phải là input type='file', " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Tải file '{1}' lên phần tử {0.name}")
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
            description = "Thực hiện thao tác kéo một phần tử (nguồn) và thả nó vào vị trí của một phần tử khác (đích).",
            category = "Web/Interaction",
            parameters = {
                    "sourceObject: ObjectUI - Phần tử nguồn cần được kéo đi",
                    "targetObject: ObjectUI - Phần tử đích, nơi phần tử nguồn sẽ được thả vào"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kéo và thả một mục vào giỏ hàng\n" +
                    "webKeyword.dragAndDrop(productItemObject, cartDropZoneObject);\n\n" +
                    "// Kéo và thả để sắp xếp lại danh sách\n" +
                    "webKeyword.dragAndDrop(taskItemObject, topOfListObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "cả hai phần tử nguồn và đích phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "trang web phải hỗ trợ thao tác kéo và thả, và trình duyệt phải hỗ trợ thao tác kéo và thả " +
                    "(một số trình duyệt di động có thể không hỗ trợ đầy đủ). " +
                    "Có thể throw ElementNotVisibleException nếu một trong hai phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu một trong hai phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu một trong hai phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu một trong hai phần tử không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu phần tử đích nằm ngoài viewport hiện tại."
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
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kéo",
                    "xOffset: int - Độ lệch theo trục ngang (pixel)",
                    "yOffset: int - Độ lệch theo trục dọc (pixel)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kéo thanh trượt giá sang phải 100px\n" +
                    "webKeyword.dragAndDropByOffset(priceSliderHandle, 100, 0);\n\n" +
                    "// Kéo thanh trượt âm lượng xuống 50px\n" +
                    "webKeyword.dragAndDropByOffset(volumeSliderObject, 0, -50);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kéo phải tồn tại trong DOM, phải hiển thị và có thể tương tác được, " +
                    "trang web phải hỗ trợ thao tác kéo và thả, và trình duyệt phải hỗ trợ thao tác kéo và thả " +
                    "(một số trình duyệt di động có thể không hỗ trợ đầy đủ). " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc MoveTargetOutOfBoundsException nếu vị trí đích nằm ngoài viewport hiện tại."
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
            category = "Web/Interaction",
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
                    "phần tử cần nhận tổ hợp phím phải đang được focus, " +
                    "trình duyệt phải hỗ trợ các tổ hợp phím được sử dụng, " +
                    "và các phím đặc biệt phải được định nghĩa trong org.openqa.selenium.Keys. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "NoSuchSessionException nếu phiên WebDriver không còn hợp lệ, " +
                    "UnsupportedOperationException nếu trình duyệt không hỗ trợ thao tác phím được yêu cầu, " +
                    "hoặc IllegalArgumentException nếu tham số keys không hợp lệ."
    )
    @Step("Gửi tổ hợp phím: {0}")
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
            description = "Thực hiện click vào một phần tử bằng JavaScript. Hữu ích khi click thông thường không hoạt động.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click vào nút ẩn\n" +
                    "webKeyword.clickWithJavascript(hiddenButtonObject);\n\n" +
                    "// Click vào phần tử bị che khuất bởi phần tử khác\n" +
                    "webKeyword.clickWithJavascript(overlappedElementObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần click phải tồn tại trong DOM, " +
                    "trình duyệt phải hỗ trợ thực thi JavaScript, " +
                    "và người dùng phải có quyền thực thi JavaScript trên trang. " +
                    "Có thể throw JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
    )
    @Step("Click vào phần tử {0.name} bằng JavaScript")
    public void clickWithJavascript(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("arguments[0].click();", element);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "selectByIndex",
            description = "Chọn một tùy chọn (option) trong một phần tử dropdown (thẻ select) dựa trên chỉ số của nó (bắt đầu từ 0).",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử dropdown (thẻ select)",
                    "index: int - Chỉ số của tùy chọn cần chọn (ví dụ: 0 cho tùy chọn đầu tiên)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chọn tùy chọn thứ hai trong dropdown quốc gia\n" +
                    "webKeyword.selectByIndex(countryDropdownObject, 1); // Chỉ số bắt đầu từ 0\n\n" +
                    "// Chọn tùy chọn đầu tiên trong dropdown ngôn ngữ\n" +
                    "webKeyword.selectByIndex(languageDropdownObject, 0);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử dropdown phải tồn tại trong DOM, phải là thẻ <select> hợp lệ, " +
                    "phải hiển thị và có thể tương tác được, " +
                    "và chỉ số phải nằm trong phạm vi hợp lệ (0 đến số lượng tùy chọn - 1). " +
                    "Có thể throw NoSuchElementException nếu phần tử dropdown không tồn tại, " +
                    "ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "IndexOutOfBoundsException nếu chỉ số nằm ngoài phạm vi hợp lệ, " +
                    "UnexpectedTagNameException nếu phần tử không phải là thẻ <select>, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Đại diện cho nhóm radio button (ví dụ locator chung là '//input[@name=\"gender\"]')",
                    "value: String - Giá trị trong thuộc tính 'value' của radio button cần chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chọn radio button giới tính nữ\n" +
                    "webKeyword.selectRadioByValue(genderRadioGroup, \"female\");\n\n" +
                    "// Chọn phương thức thanh toán\n" +
                    "webKeyword.selectRadioByValue(paymentMethodRadioGroup, \"credit_card\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "nhóm radio button phải tồn tại trong DOM, " +
                    "ít nhất một radio button trong nhóm phải có thuộc tính 'value' khớp với giá trị cần chọn, " +
                    "và phần tử phải hiển thị và có thể tương tác được. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy radio button với value chỉ định, " +
                    "ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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


    @NetatKeyword(
            name = "selectByValue",
            description = "Chọn một tùy chọn trong dropdown dựa trên giá trị của thuộc tính 'value'.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử dropdown (thẻ select)",
                    "value: String - Giá trị thuộc tính 'value' của tùy chọn cần chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chọn thành phố Hà Nội từ dropdown\n" +
                    "webKeyword.selectByValue(cityDropdown, \"HN\");\n\n" +
                    "// Chọn phương thức vận chuyển\n" +
                    "webKeyword.selectByValue(shippingMethodDropdown, \"express\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử dropdown phải tồn tại trong DOM, phải là thẻ <select> hợp lệ, " +
                    "phải hiển thị và có thể tương tác được, " +
                    "và phải tồn tại ít nhất một tùy chọn có thuộc tính value khớp với giá trị cần chọn. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy tùy chọn với value chỉ định, " +
                    "ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "UnexpectedTagNameException nếu phần tử không phải là thẻ <select>, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Chọn tùy chọn có value '{1}' cho dropdown {0.name}")
    public void selectByValue(ObjectUI uiObject, String value) {
        execute(() -> {
            Select select = new Select(findElement(uiObject));
            select.selectByValue(value);
            return null;
        }, uiObject, value);
    }

    @NetatKeyword(
            name = "selectByVisibleText",
            description = "Chọn một tùy chọn trong dropdown dựa trên văn bản hiển thị của nó.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử dropdown (thẻ select)",
                    "text: String - Văn bản hiển thị của tùy chọn cần chọn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chọn quốc gia từ dropdown theo tên hiển thị\n" +
                    "webKeyword.selectByVisibleText(countryDropdown, \"Việt Nam\");\n\n" +
                    "// Chọn danh mục sản phẩm\n" +
                    "webKeyword.selectByVisibleText(categoryDropdown, \"Điện thoại & Máy tính bảng\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử dropdown phải tồn tại trong DOM, phải là thẻ <select> hợp lệ, " +
                    "phải hiển thị và có thể tương tác được, " +
                    "phải tồn tại ít nhất một tùy chọn có văn bản hiển thị khớp chính xác với text cần chọn, " +
                    "và văn bản cần chọn phải khớp chính xác với văn bản hiển thị (phân biệt chữ hoa/thường, khoảng trắng, ký tự đặc biệt). " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy tùy chọn với văn bản hiển thị chỉ định, " +
                    "ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "UnexpectedTagNameException nếu phần tử không phải là thẻ <select>, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Chọn tùy chọn có văn bản '{1}' cho dropdown {0.name}")
    public void selectByVisibleText(ObjectUI uiObject, String text) {
        execute(() -> {
            Select select = new Select(findElement(uiObject));
            select.selectByVisibleText(text);
            return null;
        }, uiObject, text);
    }

    @NetatKeyword(
            name = "clickElementByIndex",
            description = "Click vào một phần tử cụ thể trong một danh sách các phần tử dựa trên chỉ số (index) của nó (bắt đầu từ 0).",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện đại diện cho danh sách phần tử",
                    "index: int - Vị trí của phần tử cần click (0 cho phần tử đầu tiên)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click vào kết quả tìm kiếm thứ 3\n" +
                    "webKeyword.clickElementByIndex(searchResultLinks, 2); // Index bắt đầu từ 0\n\n" +
                    "// Click vào mục đầu tiên trong danh sách\n" +
                    "webKeyword.clickElementByIndex(menuItems, 0);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "danh sách phần tử phải tồn tại trong DOM, " +
                    "chỉ số phải nằm trong phạm vi hợp lệ (0 đến số lượng phần tử - 1), " +
                    "và phần tử tại chỉ số cần click phải hiển thị và có thể tương tác được. " +
                    "Có thể throw IndexOutOfBoundsException nếu chỉ số nằm ngoài phạm vi hợp lệ, " +
                    "ElementNotVisibleException nếu phần tử không hiển thị trên trang, " +
                    "ElementNotInteractableException nếu phần tử không thể tương tác, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchElementException nếu danh sách phần tử không tồn tại."
    )
    @Step("Click vào phần tử ở vị trí {1} trong danh sách {0.name}")
    public void clickElementByIndex(ObjectUI uiObject, int index) {
        execute(() -> {
            List<WebElement> elements = findElements(uiObject);
            if (index >= 0 && index < elements.size()) {
                elements.get(index).click();
            } else {
                throw new IndexOutOfBoundsException("Chỉ số (index) không hợp lệ: " + index + ". Danh sách chỉ có " + elements.size() + " phần tử.");
            }
            return null;
        }, uiObject, index);
    }

    @NetatKeyword(
            name = "scrollToElement",
            description = "Cuộn trang đến khi phần tử được chỉ định nằm trong vùng có thể nhìn thấy của trình duyệt. " +
                    "Rất cần thiết khi cần tương tác với các phần tử ở cuối trang.",
            category = "Web/Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử đích cần cuộn đến"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Cuộn đến phần chân trang\n" +
                    "webKeyword.scrollToElement(footerSectionObject);\n" +
                    "webKeyword.click(privacyPolicyLinkObject);\n\n" +
                    "// Cuộn đến nút gửi ở cuối form\n" +
                    "webKeyword.scrollToElement(submitButtonObject);\n" +
                    "webKeyword.click(submitButtonObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần cuộn đến phải tồn tại trong DOM, " +
                    "và trình duyệt phải hỗ trợ thực thi JavaScript. " +
                    "Có thể throw NoSuchElementException nếu phần tử không tồn tại, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Interaction",
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
                    "và tọa độ phải nằm trong phạm vi hợp lệ của trang web. " +
                    "Có thể throw JavascriptException nếu có lỗi khi thực thi JavaScript, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Interaction",
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
            category = "Web/Interaction",
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
    @Step("Cuộn xuống cuối trang")
    public void scrollToBottom() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("window.scrollTo(0, document.body.scrollHeight)");
            return null;
        });
    }


    @Override
    @NetatKeyword(
            name = "getText",
            description = "Lấy và trả về văn bản của phần tử. Keyword này sẽ tự động thử nhiều cách: " +
                    "1. Lấy thuộc tính 'value' (cho ô input, textarea). " +
                    "2. Lấy văn bản hiển thị thông thường. " +
                    "3. Lấy 'textContent' hoặc 'innerText' nếu 2 cách trên thất bại.",
            category = "Web/Getter",
            parameters = {
                    "uiObject: ObjectUI - Phần tử chứa văn bản cần lấy"
            },
            returnValue = "String - Văn bản của phần tử hoặc chuỗi rỗng nếu không có văn bản",
            example = "// Lấy văn bản từ phần tử hiển thị\n" +
                    "String welcomeMessage = webKeyword.getText(welcomeMessageObject);\n" +
                    "webKeyword.verifyEqual(welcomeMessage, \"Chào mừng bạn!\");\n\n" +
                    "// Lấy giá trị từ ô input\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "String username = webKeyword.getText(usernameInputObject);\n" +
                    "webKeyword.verifyEqual(username, \"testuser\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần lấy văn bản phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu phần tử không tồn tại, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Lấy văn bản từ phần tử: {0.name}")
    public String getText(ObjectUI uiObject) {
        return super.getText(uiObject);
    }

    @NetatKeyword(
            name = "getAttribute",
            description = "Lấy và trả về giá trị của một thuộc tính (attribute) cụ thể trên một phần tử HTML.",
            category = "Web/Getter",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần lấy thuộc tính",
                    "attributeName: String - Tên của thuộc tính cần lấy giá trị (ví dụ: 'href', 'class', 'value')"
            },
            returnValue = "String - Giá trị của thuộc tính hoặc null nếu thuộc tính không tồn tại",
            example = "// Lấy URL từ thẻ liên kết\n" +
                    "String linkUrl = webKeyword.getAttribute(linkObject, \"href\");\n" +
                    "webKeyword.verifyContains(linkUrl, \"https://example.com\");\n\n" +
                    "// Kiểm tra trạng thái của checkbox\n" +
                    "String isChecked = webKeyword.getAttribute(termsCheckboxObject, \"checked\");\n" +
                    "webKeyword.verifyNotNull(isChecked);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần lấy thuộc tính phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu phần tử không tồn tại, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Lấy thuộc tính '{1}' của phần tử {0.name}")
    public String getAttribute(ObjectUI uiObject, String attributeName) {
        return execute(() -> findElement(uiObject).getAttribute(attributeName), uiObject, attributeName);
    }

    @NetatKeyword(
            name = "getCssValue",
            description = "Lấy giá trị của một thuộc tính CSS được áp dụng trên một phần tử.",
            category = "Web/Getter",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần lấy giá trị CSS",
                    "cssPropertyName: String - Tên của thuộc tính CSS (ví dụ: 'color', 'font-size', 'background-color')"
            },
            returnValue = "String - Giá trị của thuộc tính CSS được chỉ định",
            example = "// Lấy màu chữ của nút\n" +
                    "String buttonColor = webKeyword.getCssValue(buttonObject, \"color\");\n" +
                    "webKeyword.verifyEqual(buttonColor, \"rgba(255, 255, 255, 1)\");\n\n" +
                    "// Kiểm tra kích thước font\n" +
                    "String fontSize = webKeyword.getCssValue(headingObject, \"font-size\");\n" +
                    "webKeyword.verifyEqual(fontSize, \"24px\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần lấy giá trị CSS phải tồn tại trong DOM, " +
                    "và thuộc tính CSS cần lấy phải được áp dụng cho phần tử (trực tiếp hoặc được kế thừa). " +
                    "Có thể throw NoSuchElementException nếu phần tử không tồn tại, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Lấy giá trị CSS '{1}' của phần tử {0.name}")
    public String getCssValue(ObjectUI uiObject, String cssPropertyName) {
        return execute(() -> findElement(uiObject).getCssValue(cssPropertyName), uiObject, cssPropertyName);
    }

    @NetatKeyword(
            name = "getCurrentUrl",
            description = "Lấy và trả về URL đầy đủ của trang web hiện tại mà trình duyệt đang hiển thị.",
            category = "Web/Getter",
            parameters = {},
            returnValue = "String - URL đầy đủ của trang web hiện tại",
            example = "// Kiểm tra URL sau khi điều hướng\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/products\");\n" +
                    "String currentUrl = webKeyword.getCurrentUrl();\n" +
                    "webKeyword.verifyEqual(currentUrl, \"https://example.com/products\");\n\n" +
                    "// Kiểm tra URL sau khi gửi form\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "String resultUrl = webKeyword.getCurrentUrl();\n" +
                    "webKeyword.verifyContains(resultUrl, \"success=true\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
    )
    @Step("Lấy URL hiện tại")
    public String getCurrentUrl() {
        return execute(() -> DriverManager.getDriver().getCurrentUrl());
    }

    @NetatKeyword(
            name = "getPageTitle",
            description = "Lấy và trả về tiêu đề (title) của trang web hiện tại.",
            category = "Web/Getter",
            parameters = {},
            returnValue = "String - Tiêu đề của trang web hiện tại",
            example = "// Kiểm tra tiêu đề trang sau khi điều hướng\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/about\");\n" +
                    "String pageTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.verifyEqual(pageTitle, \"Về chúng tôi - Example Company\");\n\n" +
                    "// Kiểm tra tiêu đề trang sau khi tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "String searchResultTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.verifyContains(searchResultTitle, \"Kết quả tìm kiếm\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc NoSuchSessionException nếu phiên WebDriver không còn hợp lệ."
    )
    @Step("Lấy tiêu đề trang")
    public String getPageTitle() {
        return execute(() -> DriverManager.getDriver().getTitle());
    }

    @NetatKeyword(
            name = "getElementCount",
            description = "Đếm và trả về số lượng phần tử trên trang khớp với locator được cung cấp. Hữu ích để kiểm tra số lượng kết quả tìm kiếm, số hàng trong bảng,...",
            category = "Web/Getter",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện đại diện cho các phần tử cần đếm"
            },
            returnValue = "int - Số lượng phần tử tìm thấy",
            example = "// Đếm số lượng sản phẩm trong danh sách\n" +
                    "int numberOfProducts = webKeyword.getElementCount(productListItemObject);\n" +
                    "webKeyword.verifyEqual(numberOfProducts, 10);\n\n" +
                    "// Kiểm tra số lượng kết quả tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"smartphone\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "int resultCount = webKeyword.getElementCount(searchResultItemObject);\n" +
                    "System.out.println(\"Tìm thấy \" + resultCount + \" kết quả\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và locator của đối tượng giao diện phải hợp lệ. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "NoSuchSessionException nếu phiên WebDriver không còn hợp lệ, " +
                    "hoặc InvalidSelectorException nếu locator không hợp lệ."
    )
    @Step("Đếm số lượng phần tử của: {0.name}")
    public int getElementCount(ObjectUI uiObject) {
        return execute(() -> {
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return DriverManager.getDriver().findElements(by).size();
        }, uiObject);
    }

    @NetatKeyword(
            name = "getTextFromElements",
            description = "Lấy và trả về một danh sách (List) các chuỗi văn bản từ mỗi phần tử trong một danh sách các phần tử.",
            category = "Web/Getter",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng giao diện đại diện cho các phần tử cần lấy văn bản"
            },
            returnValue = "List<String> - Danh sách các chuỗi văn bản từ các phần tử tìm thấy",
            example = "// Lấy danh sách tên sản phẩm\n" +
                    "List<String> productNames = webKeyword.getTextFromElements(productNameObject);\n" +
                    "System.out.println(\"Tìm thấy \" + productNames.size() + \" sản phẩm\");\n\n" +
                    "// Kiểm tra danh sách giá sản phẩm\n" +
                    "List<String> prices = webKeyword.getTextFromElements(productPriceObject);\n" +
                    "for (String price : prices) {\n" +
                    "    webKeyword.verifyTrue(price.contains(\"₫\"), \"Giá không đúng định dạng\");\n" +
                    "}",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "locator của đối tượng giao diện phải hợp lệ, " +
                    "và các phần tử cần lấy văn bản phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử nào khớp với locator, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM trong quá trình xử lý, " +
                    "TimeoutException nếu các phần tử không xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Lấy văn bản từ danh sách các phần tử: {0.name}")
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
            description = "Tạm dừng kịch bản cho đến khi một phần tử không chỉ hiển thị mà còn ở trạng thái sẵn sàng để được click (enabled).",
            category = "Web/Wait",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần chờ để sẵn sàng click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ nút gửi sẵn sàng để click sau khi điền form\n" +
                    "webKeyword.sendKeys(emailInputObject, \"test@example.com\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.waitForElementClickable(submitButtonObject);\n" +
                    "webKeyword.click(submitButtonObject);\n\n" +
                    "// Chờ nút được kích hoạt sau khi chọn một tùy chọn\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.waitForElementClickable(continueButtonObject);\n" +
                    "webKeyword.click(continueButtonObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần chờ phải tồn tại trong DOM, " +
                    "và phần tử sẽ trở thành hiển thị và có thể click trong khoảng thời gian chờ. " +
                    "Có thể throw TimeoutException nếu phần tử không trở nên có thể click trong thời gian chờ mặc định, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Wait",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần chờ cho đến khi nó biến mất"
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
                    "phần tử cần chờ phải tồn tại trong DOM hoặc đã hiển thị trước đó, " +
                    "và phần tử sẽ trở thành không hiển thị trong khoảng thời gian chờ. " +
                    "Có thể throw TimeoutException nếu phần tử vẫn còn hiển thị sau thời gian chờ mặc định, " +
                    "NoSuchElementException nếu không tìm thấy phần tử ban đầu, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Wait",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần chờ cho đến khi nó tồn tại",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ phần tử động được tạo bởi JavaScript\n" +
                    "webKeyword.click(loadDynamicContentButton);\n" +
                    "webKeyword.waitForElementPresent(dynamicContentObject, 10);\n" +
                    "webKeyword.verifyElementPresent(dynamicContentObject);\n\n" +
                    "// Chờ phần tử được tạo sau khi chọn tùy chọn\n" +
                    "webKeyword.selectByVisibleText(categoryDropdownObject, \"Điện thoại\");\n" +
                    "webKeyword.waitForElementPresent(subcategoryListObject, 5);\n" +
                    "webKeyword.verifyElementCount(subcategoryItemObject, 5);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "locator của phần tử phải hợp lệ, " +
                    "và phần tử sẽ được thêm vào DOM trong khoảng thời gian chờ đã chỉ định. " +
                    "Có thể throw TimeoutException nếu phần tử không xuất hiện trong DOM trong thời gian chờ đã chỉ định, " +
                    "InvalidSelectorException nếu locator không hợp lệ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Wait",
            parameters = {
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng giây)"
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
            category = "Web/Wait",
            parameters = {
                    "partialUrl: String - Chuỗi con mà URL cần chứa",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng giây)"
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
                    "và URL của trang sẽ chứa chuỗi con đã chỉ định trong khoảng thời gian chờ. " +
                    "Có thể throw TimeoutException nếu URL không chứa chuỗi con đã chỉ định trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Wait",
            parameters = {
                    "expectedTitle: String - Tiêu đề trang mong đợi",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng giây)"
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
    @Step("Chờ tiêu đề trang là '{0}' trong {1} giây")
    public void waitForTitleIs(String expectedTitle, int timeoutInSeconds) {
        execute(() -> {
            new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds))
                    .until(ExpectedConditions.titleIs(expectedTitle));
            return null;
        }, expectedTitle, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "verifyElementVisibleHard",
            description = "Kiểm tra một phần tử có đang hiển thị trên giao diện hay không. Nếu kiểm tra thất bại (phần tử không hiển thị như mong đợi), kịch bản sẽ DỪNG LẠI ngay lập tức.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra",
                    "isVisible: boolean - Trạng thái hiển thị mong đợi (true cho hiển thị, false cho bị ẩn)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra thông báo lỗi hiển thị sau khi gửi form không hợp lệ\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form trống\n" +
                    "webKeyword.verifyElementVisibleHard(errorMesssageObject, true);\n\n" +
                    "// Kiểm tra phần tử không hiển thị sau khi đóng\n" +
                    "webKeyword.click(closePopupButtonObject);\n" +
                    "webKeyword.verifyElementVisibleHard(popupObject, false);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu trạng thái hiển thị của phần tử không khớp với kỳ vọng, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} có hiển thị là {1}")
    public void verifyElementVisibleHard(ObjectUI uiObject, boolean isVisible) {
        performVisibilityAssertion(uiObject, isVisible, false);
    }


    @NetatKeyword(
            name = "verifyElementVisibleSoft",
            description = "Kiểm tra một phần tử có đang hiển thị trên giao diện hay không. Nếu kiểm tra thất bại, kịch bản sẽ ghi nhận lỗi nhưng vẫn TIẾP TỤC chạy các bước tiếp theo.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra",
                    "isVisible: boolean - Trạng thái hiển thị mong đợi (true cho hiển thị, false cho bị ẩn)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra thông báo thành công hiển thị sau khi lưu\n" +
                    "webKeyword.click(saveButtonObject);\n" +
                    "webKeyword.verifyElementVisibleSoft(successMessageObject, true);\n" +
                    "// Kịch bản vẫn tiếp tục ngay cả khi thông báo không hiển thị\n\n" +
                    "// Kiểm tra nhiều phần tử trên trang\n" +
                    "webKeyword.verifyElementVisibleSoft(headerLogoObject, true);\n" +
                    "webKeyword.verifyElementVisibleSoft(navigationMenuObject, true);\n" +
                    "webKeyword.click(mainButtonObject); // Thực hiện hành động tiếp theo",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Soft) phần tử {0.name} có hiển thị là {1}")
    public void verifyElementVisibleSoft(ObjectUI uiObject, boolean isVisible) {
        performVisibilityAssertion(uiObject, isVisible, true);
    }

    @NetatKeyword(
            name = "verifyTextHard",
            description = "So sánh văn bản của một phần tử với một chuỗi ký tự mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử chứa văn bản cần kiểm tra",
                    "expectedText: String - Chuỗi văn bản mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra tiêu đề trang chính xác\n" +
                    "webKeyword.verifyTextHard(pageTitleObject, \"Chào mừng đến với trang chủ\");\n\n" +
                    "// Kiểm tra kết quả tính toán\n" +
                    "webKeyword.sendKeys(number1InputObject, \"5\");\n" +
                    "webKeyword.sendKeys(number2InputObject, \"7\");\n" +
                    "webKeyword.click(calculateButtonObject);\n" +
                    "webKeyword.verifyTextHard(resultObject, \"12\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản. " +
                    "Có thể throw AssertionError nếu văn bản của phần tử không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Hard) văn bản của {0.name} là '{1}'")
    public void verifyTextHard(ObjectUI uiObject, String expectedText) {
        performTextAssertion(uiObject, expectedText, false);
    }

    @NetatKeyword(
            name = "verifyTextSoft",
            description = "So sánh văn bản của một phần tử với một chuỗi ký tự mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử chứa văn bản cần kiểm tra",
                    "expectedText: String - Chuỗi văn bản mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra nhãn trên form đăng nhập\n" +
                    "webKeyword.verifyTextSoft(usernameLabelObject, \"Tên đăng nhập\");\n" +
                    "webKeyword.verifyTextSoft(passwordLabelObject, \"Mật khẩu\");\n" +
                    "// Kịch bản tiếp tục ngay cả khi có nhãn không khớp\n\n" +
                    "// Kiểm tra nhiều giá trị hiển thị\n" +
                    "webKeyword.verifyTextSoft(productNameObject, \"Điện thoại thông minh X1\");\n" +
                    "webKeyword.verifyTextSoft(productPriceObject, \"5.990.000 ₫\");\n" +
                    "webKeyword.click(addToCartButtonObject); // Tiếp tục thực hiện hành động",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Soft) văn bản của {0.name} là '{1}'")
    public void verifyTextSoft(ObjectUI uiObject, String expectedText) {
        performTextAssertion(uiObject, expectedText, true);
    }

    @NetatKeyword(
            name = "verifyTextContainsHard",
            description = "Kiểm tra văn bản của một phần tử có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ DỪNG LẠI.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử chứa văn bản cần kiểm tra",
                    "partialText: String - Chuỗi văn bản con mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra thông báo chào mừng có chứa tên người dùng\n" +
                    "webKeyword.verifyTextContainsHard(welcomeMessageObject, \"Xin chào\");\n\n" +
                    "// Kiểm tra kết quả tìm kiếm có chứa từ khóa đã tìm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForElementVisible(searchResultsObject);\n" +
                    "webKeyword.verifyTextContainsHard(searchResultTitleObject, \"laptop\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản. " +
                    "Có thể throw AssertionError nếu văn bản của phần tử không chứa chuỗi con mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Hard) văn bản của {0.name} có chứa '{1}'")
    public void verifyTextContainsHard(ObjectUI uiObject, String partialText) {
        performTextContainsAssertion(uiObject, partialText, false);
    }

    @NetatKeyword(
            name = "verifyTextContainsSoft",
            description = "Kiểm tra văn bản của một phần tử có chứa một chuỗi con hay không. Nếu không chứa, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử chứa văn bản cần kiểm tra",
                    "partialText: String - Chuỗi văn bản con mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra kết quả tìm kiếm có chứa thông tin số lượng\n" +
                    "webKeyword.sendKeys(searchInputObject, \"điện thoại\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.verifyTextContainsSoft(searchResultSummary, \"kết quả\");\n" +
                    "webKeyword.verifyTextContainsSoft(searchResultSummary, \"điện thoại\");\n\n" +
                    "// Kiểm tra nhiều thông tin trên trang sản phẩm\n" +
                    "webKeyword.verifyTextContainsSoft(productDescriptionObject, \"chống nước\");\n" +
                    "webKeyword.verifyTextContainsSoft(productDescriptionObject, \"bảo hành\");\n" +
                    "webKeyword.click(addToCartButtonObject); // Tiếp tục thực hiện hành động",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Soft) văn bản của {0.name} có chứa '{1}'")
    public void verifyTextContainsSoft(ObjectUI uiObject, String partialText) {
        performTextContainsAssertion(uiObject, partialText, true);
    }

    @NetatKeyword(
            name = "verifyElementAttributeHard",
            description = "Kiểm tra giá trị của một thuộc tính (attribute) trên phần tử. Nếu giá trị không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra",
                    "attributeName: String - Tên của thuộc tính (ví dụ: 'href', 'class', 'value')",
                    "expectedValue: String - Giá trị mong đợi của thuộc tính"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra đường dẫn của liên kết\n" +
                    "webKeyword.verifyElementAttributeHard(linkObject, \"href\", \"/products/123\");\n\n" +
                    "// Kiểm tra trạng thái của checkbox\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.verifyElementAttributeHard(termsCheckboxObject, \"checked\", \"true\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kiểm tra phải tồn tại trong DOM, " +
                    "và thuộc tính cần kiểm tra phải tồn tại trên phần tử. " +
                    "Có thể throw AssertionError nếu giá trị thuộc tính không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Hard) thuộc tính '{1}' của {0.name} là '{2}'")
    public void verifyElementAttributeHard(ObjectUI uiObject, String attributeName, String expectedValue) {
        performAttributeAssertion(uiObject, attributeName, expectedValue, false);
    }

    @NetatKeyword(
            name = "verifyElementAttributeSoft",
            description = "Kiểm tra giá trị của một thuộc tính (attribute) trên phần tử. Nếu giá trị không khớp, kịch bản sẽ ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra",
                    "attributeName: String - Tên của thuộc tính (ví dụ: 'href', 'class', 'value')",
                    "expectedValue: String - Giá trị mong đợi của thuộc tính"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra nhiều thuộc tính của một phần tử\n" +
                    "webKeyword.verifyElementAttributeSoft(buttonObject, \"type\", \"submit\");\n" +
                    "webKeyword.verifyElementAttributeSoft(buttonObject, \"class\", \"btn-primary\");\n" +
                    "webKeyword.click(buttonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra thuộc tính của nhiều phần tử\n" +
                    "webKeyword.verifyElementAttributeSoft(usernameInputObject, \"placeholder\", \"Nhập tên đăng nhập\");\n" +
                    "webKeyword.verifyElementAttributeSoft(passwordInputObject, \"type\", \"password\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kiểm tra phải tồn tại trong DOM, " +
                    "và thuộc tính cần kiểm tra phải tồn tại trên phần tử. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Soft) thuộc tính '{1}' của {0.name} là '{2}'")
    public void verifyElementAttributeSoft(ObjectUI uiObject, String attributeName, String expectedValue) {
        performAttributeAssertion(uiObject, attributeName, expectedValue, true);
    }


    @NetatKeyword(
            name = "verifyUrlHard",
            description = "So sánh URL của trang hiện tại với một chuỗi mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web/Assert",
            parameters = {
                    "expectedUrl: String - URL đầy đủ mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra URL sau khi đăng nhập thành công\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForUrlContains(\"/dashboard\", 10);\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/dashboard\");\n\n" +
                    "// Kiểm tra URL sau khi hoàn thành quy trình\n" +
                    "webKeyword.click(completeOrderButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(20);\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/order-confirmation\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trang web đã hoàn thành quá trình tải. " +
                    "Có thể throw AssertionError nếu URL hiện tại không khớp với URL mong đợi, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Assert",
            parameters = {
                    "expectedUrl: String - URL đầy đủ mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra URL trong quy trình nhiều bước\n" +
                    "webKeyword.click(nextButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/checkout/step1\");\n" +
                    "webKeyword.fillCheckoutForm(); // Tiếp tục quy trình ngay cả khi URL không đúng\n\n" +
                    "// Kiểm tra nhiều điều kiện bao gồm URL\n" +
                    "webKeyword.verifyElementVisibleSoft(pageHeaderObject, true);\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/products\");\n" +
                    "webKeyword.click(firstProductObject); // Tiếp tục thực hiện hành động",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trang web đã hoàn thành quá trình tải. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Soft) URL của trang là '{0}'")
    public void verifyUrlSoft(String expectedUrl) {
        execute(() -> {
            String actualUrl = DriverManager.getDriver().getCurrentUrl();
            logger.info("Kiểm tra tiêu đề: Mong đợi '{}', Thực tế: '{}'", expectedUrl, actualUrl);
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
            category = "Web/Assert",
            parameters = {
                    "expectedTitle: String - Tiêu đề trang mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra tiêu đề trang chủ\n" +
                    "webKeyword.navigateToUrl(\"https://example.com\");\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyTitleHard(\"Trang chủ - Website ABC\");\n\n" +
                    "// Kiểm tra tiêu đề sau khi đăng nhập\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.verifyTitleHard(\"Dashboard - Website ABC\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trang web đã hoàn thành quá trình tải. " +
                    "Có thể throw AssertionError nếu tiêu đề trang không khớp với tiêu đề mong đợi, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Assert",
            parameters = {
                    "expectedTitle: String - Tiêu đề trang mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra tiêu đề trang giỏ hàng sau khi thêm sản phẩm\n" +
                    "webKeyword.click(addToCartButtonObject);\n" +
                    "webKeyword.click(viewCartButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyTitleSoft(\"Giỏ hàng (1 sản phẩm)\");\n" +
                    "webKeyword.click(checkoutButtonObject); // Tiếp tục quy trình thanh toán\n\n" +
                    "// Kiểm tra nhiều điều kiện trong quy trình đặt hàng\n" +
                    "webKeyword.verifyTitleSoft(\"Thanh toán - Bước 1: Thông tin giao hàng\");\n" +
                    "webKeyword.verifyElementVisibleSoft(shippingFormObject, true);\n" +
                    "webKeyword.fillShippingForm(); // Tiếp tục điền form",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và trang web đã hoàn thành quá trình tải. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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

    @NetatKeyword(
            name = "assertElementEnabled",
            description = "Khẳng định rằng một phần tử đang ở trạng thái có thể tương tác (enabled). Nếu phần tử bị vô hiệu hóa (disabled), kịch bản sẽ DỪNG LẠI.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra nút gửi form đã được kích hoạt sau khi điền đầy đủ thông tin\n" +
                    "webKeyword.sendKeys(nameInputObject, \"Nguyễn Văn A\");\n" +
                    "webKeyword.sendKeys(emailInputObject, \"nguyenvana@example.com\");\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.assertElementEnabled(submitButtonObject);\n" +
                    "webKeyword.click(submitButtonObject);\n\n" +
                    "// Kiểm tra nút thanh toán đã được kích hoạt sau khi chọn phương thức thanh toán\n" +
                    "webKeyword.click(creditCardOptionObject);\n" +
                    "webKeyword.sendKeys(cardNumberInputObject, \"1234567890123456\");\n" +
                    "webKeyword.assertElementEnabled(payNowButtonObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu phần tử đang ở trạng thái disabled, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra nút gửi form bị vô hiệu hóa khi chưa điền thông tin bắt buộc\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/registration\");\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.assertElementDisabled(submitButtonBeforeFillForm);\n\n" +
                    "// Kiểm tra nút thanh toán bị vô hiệu hóa khi chưa chọn phương thức thanh toán\n" +
                    "webKeyword.click(checkoutButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.assertElementDisabled(paymentButtonObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu phần tử đang ở trạng thái enabled, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra nhiều trường nhập liệu tùy chọn có thể tương tác\n" +
                    "webKeyword.verifyElementEnabledSoft(optionalFieldObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(commentFieldObject);\n" +
                    "webKeyword.sendKeys(commentFieldObject, \"Đây là bình luận của tôi\"); // Tiếp tục ngay cả khi có trường không enabled\n\n" +
                    "// Kiểm tra các nút chức năng trong trang quản trị\n" +
                    "webKeyword.verifyElementEnabledSoft(addButtonObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(editButtonObject);\n" +
                    "webKeyword.click(addButtonObject); // Tiếp tục thực hiện hành động",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra các tính năng bị khóa trong phiên bản dùng thử\n" +
                    "webKeyword.verifyElementDisabledSoft(lockedFeatureButton);\n" +
                    "webKeyword.verifyElementDisabledSoft(premiumFeatureButton);\n" +
                    "webKeyword.click(upgradeAccountButton); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra các trường không thể chỉnh sửa trong chế độ xem\n" +
                    "webKeyword.click(viewModeButton);\n" +
                    "webKeyword.verifyElementDisabledSoft(nameFieldInViewMode);\n" +
                    "webKeyword.click(editModeButton); // Chuyển sang chế độ chỉnh sửa",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử checkbox hoặc radio button cần kiểm tra"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra checkbox \"Ghi nhớ đăng nhập\" đã được chọn\n" +
                    "webKeyword.click(rememberMeCheckbox);\n" +
                    "webKeyword.assertElementSelected(rememberMeCheckbox);\n" +
                    "webKeyword.click(loginButtonObject);\n\n" +
                    "// Kiểm tra radio button phương thức thanh toán đã được chọn\n" +
                    "webKeyword.click(creditCardRadioButton);\n" +
                    "webKeyword.assertElementSelected(creditCardRadioButton);\n" +
                    "webKeyword.sendKeys(cardNumberInputObject, \"1234567890123456\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kiểm tra phải tồn tại trong DOM và là checkbox hoặc radio button. " +
                    "Có thể throw AssertionError nếu phần tử không ở trạng thái được chọn, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc IllegalArgumentException nếu phần tử không phải là checkbox hoặc radio button."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử checkbox hoặc radio button cần kiểm tra"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra checkbox thông báo chưa được chọn mặc định\n" +
                    "webKeyword.assertElementNotSelected(newsletterCheckbox);\n" +
                    "webKeyword.click(newsletterCheckbox); // Chọn để nhận thông báo\n\n" +
                    "// Kiểm tra các tùy chọn bổ sung chưa được chọn\n" +
                    "webKeyword.assertElementNotSelected(expressShippingRadio);\n" +
                    "webKeyword.assertElementNotSelected(giftWrappingCheckbox);\n" +
                    "webKeyword.click(expressShippingRadio); // Chọn vận chuyển nhanh",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kiểm tra phải tồn tại trong DOM và là checkbox hoặc radio button. " +
                    "Có thể throw AssertionError nếu phần tử đang ở trạng thái được chọn, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc IllegalArgumentException nếu phần tử không phải là checkbox hoặc radio button."
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} chưa được chọn")
    public void assertElementNotSelected(ObjectUI uiObject) {
        execute(() -> {
            super.performSelectionAssertion(uiObject, false, false);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "verifyTextMatchesRegexHard",
            description = "Kiểm tra văn bản của một phần tử có khớp với một biểu thức chính quy (regex) hay không. Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử chứa văn bản cần kiểm tra",
                    "pattern: String - Biểu thức chính quy để so khớp"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra mã đơn hàng có đúng định dạng\n" +
                    "webKeyword.click(viewOrderButtonObject);\n" +
                    "webKeyword.waitForElementVisible(orderIdObject);\n" +
                    "webKeyword.verifyTextMatchesRegexHard(orderIdObject, \"^DH-\\\\d{5}$\"); // Khớp với DH-12345\n\n" +
                    "// Kiểm tra số điện thoại có đúng định dạng\n" +
                    "webKeyword.verifyTextMatchesRegexHard(phoneNumberObject, \"^(\\\\+84|0)[0-9]{9,10}$\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản. " +
                    "Có thể throw AssertionError nếu văn bản không khớp với biểu thức chính quy, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc PatternSyntaxException nếu biểu thức chính quy không hợp lệ."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử chứa văn bản cần kiểm tra",
                    "pattern: String - Biểu thức chính quy để so khớp"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra định dạng email hiển thị trên trang\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(emailFormatObject, \"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$\");\n" +
                    "webKeyword.click(continueButtonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra nhiều định dạng khác nhau trên trang thông tin\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(zipCodeObject, \"^\\\\d{5}(-\\\\d{4})?$\"); // Mã bưu điện\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(taxIdObject, \"^\\\\d{10}$\"); // Mã số thuế\n" +
                    "webKeyword.click(saveButtonObject); // Tiếp tục lưu thông tin",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc PatternSyntaxException nếu biểu thức chính quy không hợp lệ."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra",
                    "attribute: String - Tên của thuộc tính (ví dụ: 'class')",
                    "partialValue: String - Chuỗi con mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra phần tử có class 'active'\n" +
                    "webKeyword.click(tabButtonObject);\n" +
                    "webKeyword.verifyAttributeContainsHard(tabButtonObject, \"class\", \"active\");\n\n" +
                    "// Kiểm tra đường dẫn hình ảnh chứa tên sản phẩm\n" +
                    "webKeyword.verifyAttributeContainsHard(productImageObject, \"src\", \"iphone-13\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kiểm tra phải tồn tại trong DOM, " +
                    "và thuộc tính cần kiểm tra phải tồn tại trên phần tử. " +
                    "Có thể throw AssertionError nếu giá trị thuộc tính không chứa chuỗi con mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra",
                    "attribute: String - Tên của thuộc tính (ví dụ: 'class')",
                    "partialValue: String - Chuỗi con mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra thuộc tính style có chứa thông tin hiển thị\n" +
                    "webKeyword.verifyAttributeContainsSoft(elementObject, \"style\", \"display: block\");\n" +
                    "webKeyword.click(elementObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra nhiều thuộc tính của một phần tử\n" +
                    "webKeyword.verifyAttributeContainsSoft(buttonObject, \"class\", \"btn\");\n" +
                    "webKeyword.verifyAttributeContainsSoft(buttonObject, \"data-action\", \"submit\");\n" +
                    "webKeyword.click(buttonObject); // Tiếp tục thực hiện hành động",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kiểm tra phải tồn tại trong DOM, " +
                    "và thuộc tính cần kiểm tra phải tồn tại trên phần tử. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra",
                    "cssName: String - Tên thuộc tính CSS (ví dụ: 'color')",
                    "expectedValue: String - Giá trị CSS mong đợi (ví dụ: 'rgb(255, 0, 0)')"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra màu của thông báo lỗi\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form không hợp lệ\n" +
                    "webKeyword.waitForElementVisible(errorMessageObject);\n" +
                    "webKeyword.verifyCssValueHard(errorMessageObject, \"color\", \"rgba(255, 0, 0, 1)\");\n\n" +
                    "// Kiểm tra background-color của nút đã chọn\n" +
                    "webKeyword.click(selectButtonObject);\n" +
                    "webKeyword.verifyCssValueHard(selectButtonObject, \"background-color\", \"rgba(0, 123, 255, 1)\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu giá trị CSS không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra",
                    "cssName: String - Tên thuộc tính CSS (ví dụ: 'font-weight')",
                    "expectedValue: String - Giá trị CSS mong đợi (ví dụ: '700')"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra độ đậm của tiêu đề\n" +
                    "webKeyword.verifyCssValueSoft(titleObject, \"font-weight\", \"700\");\n" +
                    "webKeyword.click(nextButtonObject); // Tiếp tục quy trình\n\n" +
                    "// Kiểm tra màu nền của nút\n" +
                    "webKeyword.verifyCssValueSoft(buttonObject, \"background-color\", \"rgb(0, 123, 255)\");\n" +
                    "webKeyword.click(buttonObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "và phần tử cần kiểm tra phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Soft) CSS '{1}' của {0.name} là '{2}'")
    public void verifyCssValueSoft(ObjectUI uiObject, String cssName, String expectedValue) {
        execute(() -> {
            performCssValueAssertion(uiObject, cssName, expectedValue, true);
            return null;
        }, uiObject, cssName, expectedValue);
    }

    @NetatKeyword(
            name = "verifyElementNotPresentHard",
            description = "Khẳng định rằng một phần tử KHÔNG tồn tại trong DOM sau một khoảng thời gian chờ. Nếu phần tử vẫn tồn tại, kịch bản sẽ DỪNG LẠI.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra",
                    "timeoutInSeconds: int - Thời gian chờ tối đa"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra phần tử đã bị xóa\n" +
                    "webKeyword.click(deleteButtonObject);\n" +
                    "webKeyword.verifyElementNotPresentHard(deletedItemObject, 5);\n\n" +
                    "// Kiểm tra thông báo lỗi đã biến mất\n" +
                    "webKeyword.sendKeys(emailInput, \"valid@example.com\");\n" +
                    "webKeyword.verifyElementNotPresentHard(errorMessageObject, 3);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw AssertionError nếu phần tử vẫn tồn tại trong DOM sau thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} không tồn tại trong {1} giây")
    public void verifyElementNotPresentHard(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            boolean isPresent = isElementPresent(uiObject, timeoutInSeconds);
            Assert.assertFalse(isPresent, "HARD ASSERT FAILED: Phần tử '" + uiObject.getName() + "' vẫn tồn tại sau " + timeoutInSeconds + " giây.");
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "verifyOptionSelectedByLabelHard",
            description = "Khẳng định rằng tùy chọn có văn bản hiển thị (label) cụ thể đang được chọn trong dropdown.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử dropdown (thẻ select)",
                    "expectedLabel: String - Văn bản hiển thị của tùy chọn mong đợi"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra quốc gia đã chọn\n" +
                    "webKeyword.selectByVisibleText(countryDropdown, \"Việt Nam\");\n" +
                    "webKeyword.verifyOptionSelectedByLabelHard(countryDropdown, \"Việt Nam\");\n\n" +
                    "// Kiểm tra danh mục đã chọn\n" +
                    "webKeyword.verifyOptionSelectedByLabelHard(categoryDropdown, \"Điện thoại\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần kiểm tra phải là thẻ select và tồn tại trong DOM. " +
                    "Có thể throw AssertionError nếu tùy chọn được chọn không khớp với tùy chọn mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình duyệt, " +
                    "hoặc UnexpectedTagNameException nếu phần tử không phải là thẻ select."
    )
    @Step("Kiểm tra (Hard) tùy chọn '{1}' đã được chọn trong dropdown {0.name}")
    public void verifyOptionSelectedByLabelHard(ObjectUI uiObject, String expectedLabel) {
        execute(() -> {
            Select select = new Select(findElement(uiObject));
            String actualLabel = select.getFirstSelectedOption().getText();
            Assert.assertEquals(actualLabel, expectedLabel, "HARD ASSERT FAILED: Tùy chọn được chọn không khớp.");
            return null;
        }, uiObject, expectedLabel);
    }


    @NetatKeyword(
            name = "isElementPresent",
            description = "Kiểm tra xem một phần tử có tồn tại trong DOM hay không trong một khoảng thời gian chờ nhất định. Trả về true nếu tìm thấy, false nếu không tìm thấy và không ném ra exception.",
            category = "Web/Assert",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần tìm kiếm",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng giây)"
            },
            returnValue = "boolean - true nếu phần tử tồn tại, false nếu không tồn tại",
            example = "// Kiểm tra thông báo lỗi có xuất hiện không\n" +
                    "boolean isErrorVisible = webKeyword.isElementPresent(errorMessageObject, 5);\n" +
                    "if (isErrorVisible) {\n" +
                    "    // Xử lý khi có lỗi\n" +
                    "}\n\n" +
                    "// Kiểm tra phần tử tùy chọn có tồn tại không\n" +
                    "if (webKeyword.isElementPresent(optionalElementObject, 2)) {\n" +
                    "    webKeyword.click(optionalElementObject);\n" +
                    "}",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra sự tồn tại của phần tử {0.name} trong {1} giây")
    public boolean isElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        return execute(() -> {
            WebDriver driver = DriverManager.getDriver();
            // Sử dụng locator đầu tiên được kích hoạt để kiểm tra
            By by = uiObject.getActiveLocators().get(0).convertToBy();

            try {
                // Tạm thời tắt implicit wait để WebDriverWait hoạt động chính xác
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
                // Chờ cho đến khi danh sách các phần tử tìm thấy không còn rỗng
                wait.until(d -> !d.findElements(by).isEmpty());

                return true; // Tìm thấy phần tử
            } catch (TimeoutException e) {
                return false; // Hết thời gian chờ mà không tìm thấy
            } finally {
                // Framework NETAT mặc định implicit wait là 0 nên không cần khôi phục.
            }
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "verifyAlertPresent",
            description = "Khẳng định rằng một hộp thoại alert đang hiển thị trong một khoảng thời gian chờ.",
            category = "Web/Assert",
            parameters = {
                    "timeoutInSeconds: int - Thời gian chờ tối đa"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra alert xuất hiện sau khi xóa\n" +
                    "webKeyword.click(deleteButtonObject);\n" +
                    "webKeyword.verifyAlertPresent(5);\n" +
                    "webKeyword.acceptAlert(); // Xác nhận xóa\n\n" +
                    "// Kiểm tra alert xuất hiện khi rời trang có dữ liệu chưa lưu\n" +
                    "webKeyword.sendKeys(commentField, \"Bình luận mới\");\n" +
                    "webKeyword.click(backButtonObject);\n" +
                    "webKeyword.verifyAlertPresent(3);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trang web có thể hiển thị hộp thoại alert. " +
                    "Có thể throw AssertionError nếu không có hộp thoại alert xuất hiện trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Kiểm tra alert có xuất hiện trong {0} giây")
    public void verifyAlertPresent(int timeoutInSeconds) {
        execute(() -> {
            try {
                WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
                wait.until(ExpectedConditions.alertIsPresent());
            } catch (Exception e) {
                throw new AssertionError("HARD ASSERT FAILED: Hộp thoại alert không xuất hiện sau " + timeoutInSeconds + " giây.");
            }
            return null;
        }, timeoutInSeconds);
    }

// =================================================================================
// --- 8. WINDOW, TAB & FRAME MANAGEMENT ---
// =================================================================================

    @NetatKeyword(
            name = "switchToWindowByTitle",
            description = "Duyệt qua tất cả các cửa sổ hoặc tab đang mở và chuyển sự điều khiển của WebDriver sang cửa sổ/tab có tiêu đề khớp chính xác với chuỗi được cung cấp.",
            category = "Web/Window&Frame",
            parameters = {
                    "windowTitle: String - Tiêu đề chính xác của cửa sổ hoặc tab cần chuyển đến"
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
            category = "Web/Window&Frame",
            parameters = {
                    "index: int - Chỉ số của cửa sổ/tab cần chuyển đến (0 là cửa sổ đầu tiên)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Mở liên kết trong tab mới và chuyển sang tab đó\n" +
                    "webKeyword.rightClickAndSelect(productLinkObject, \"Mở trong tab mới\");\n" +
                    "webKeyword.switchToWindowByIndex(1); // Chuyển sang tab thứ hai\n\n" +
                    "// Quay lại tab chính sau khi hoàn thành\n" +
                    "webKeyword.switchToWindowByIndex(0);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "có đủ số lượng cửa sổ/tab đang mở để chuyển đến chỉ số được chỉ định. " +
                    "Có thể throw IndexOutOfBoundsException nếu chỉ số nằm ngoài phạm vi của số lượng cửa sổ/tab đang mở, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Window&Frame",
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
                    "phần tử iframe cần chuyển vào phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử iframe, " +
                    "StaleElementReferenceException nếu phần tử iframe không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Window&Frame",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Thoát khỏi iframe con và quay về iframe cha\n" +
                    "webKeyword.switchToFrame(mainIframeObject);\n" +
                    "webKeyword.switchToFrame(nestedIframeObject);\n" +
                    "webKeyword.switchToParentFrame(); // Quay về iframe cha\n" +
                    "webKeyword.click(nextButtonObject);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "WebDriver đang ở trong ngữ cảnh của một iframe. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Window&Frame",
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
            category = "Web/Window&Frame",
            parameters = {
                    "url: String - (Tùy chọn) URL để mở trong tab mới. Nếu để trống, sẽ mở tab trống"
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


    @NetatKeyword(
            name = "clickAndSwitchToNewTab",
            description = "Click vào một phần tử (thường là link) và tự động chuyển sự điều khiển sang tab/cửa sổ mới vừa được mở ra.",
            category = "Web/Window&Frame",
            parameters = {
                    "uiObject: ObjectUI - Phần tử link cần click"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Click vào liên kết mở trong tab mới\n" +
                    "webKeyword.clickAndSwitchToNewTab(externalLinkObject);\n" +
                    "webKeyword.waitForPageLoaded();\n\n" +
                    "// Click vào nút xem chi tiết sản phẩm\n" +
                    "webKeyword.clickAndSwitchToNewTab(viewDetailsButtonObject);\n" +
                    "webKeyword.verifyElementVisibleHard(productSpecificationsObject, 10);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần click phải tồn tại trong DOM và có khả năng mở tab mới (ví dụ: có thuộc tính target='_blank'). " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "TimeoutException nếu tab mới không mở trong thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Click và chuyển sang tab mới từ phần tử: {0.name}")
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
            description = "Chờ cho đến khi một hộp thoại alert, prompt, hoặc confirm của trình duyệt xuất hiện và lấy về nội dung văn bản của nó.",
            category = "Web/Alert",
            parameters = {},
            returnValue = "String - Nội dung văn bản của hộp thoại alert",
            example = "// Lấy và kiểm tra nội dung thông báo xác nhận\n" +
                    "webKeyword.click(deleteButtonObject);\n" +
                    "String alertMessage = webKeyword.getAlertText();\n" +
                    "if (alertMessage.contains(\"Bạn có chắc chắn muốn xóa?\")) {\n" +
                    "    webKeyword.acceptAlert();\n" +
                    "}\n\n" +
                    "// Lấy thông báo lỗi từ alert\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "String errorMessage = webKeyword.getAlertText();\n" +
                    "logger.info(\"Thông báo lỗi: \" + errorMessage);",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "một hộp thoại alert đang hiển thị hoặc sẽ xuất hiện. " +
                    "Có thể throw TimeoutException nếu không có hộp thoại alert xuất hiện trong thời gian chờ, " +
                    "NoAlertPresentException nếu không có hộp thoại alert đang hiển thị, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Alert",
            parameters = {
                    "text: String - Chuỗi văn bản cần nhập vào hộp thoại"
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
                    "ElementNotInteractableException nếu hộp thoại không phải là prompt và không cho phép nhập liệu, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
    // --- 11. COOKIES & STORAGE KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "findElementInShadowDom",
            description = "Tìm kiếm và trả về một phần tử nằm bên trong một Shadow DOM. Yêu cầu cung cấp phần tử chủ (shadow host) và một CSS selector để định vị phần tử con.",
            category = "Web/Interaction",
            parameters = {
                    "shadowHostObject: ObjectUI - Phần tử chủ (host) chứa Shadow DOM",
                    "cssSelectorInShadow: String - Chuỗi CSS selector để tìm phần tử bên trong Shadow DOM"
            },
            returnValue = "WebElement - Phần tử web được tìm thấy bên trong Shadow DOM",
            example = "// Tìm và tương tác với phần tử input trong Shadow DOM\n" +
                    "WebElement usernameInput = webKeyword.findElementInShadowDom(appContainerObject, \"#username\");\n" +
                    "usernameInput.sendKeys(\"admin@example.com\");\n\n" +
                    "// Tìm và click vào nút trong Shadow DOM\n" +
                    "WebElement submitButton = webKeyword.findElementInShadowDom(loginFormObject, \".submit-button\");\n" +
                    "submitButton.click();",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trình duyệt hỗ trợ Shadow DOM (Chrome, Firefox mới), " +
                    "phần tử chủ (host) phải tồn tại và có Shadow DOM đính kèm. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử chủ hoặc phần tử con, " +
                    "StaleElementReferenceException nếu phần tử chủ không còn gắn với DOM, " +
                    "UnsupportedOperationException nếu trình duyệt không hỗ trợ Shadow DOM API, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Tìm phần tử '{1}' bên trong Shadow DOM của {0.name}")
    public WebElement findElementInShadowDom(ObjectUI shadowHostObject, String cssSelectorInShadow) {
        return execute(() -> {
            WebElement shadowHost = findElement(shadowHostObject);
            SearchContext shadowRoot = shadowHost.getShadowRoot();
            return shadowRoot.findElement(By.cssSelector(cssSelectorInShadow));
        }, shadowHostObject, cssSelectorInShadow);
    }

    @NetatKeyword(
            name = "setLocalStorage",
            description = "Ghi một cặp khóa-giá trị vào Local Storage của trình duyệt. Hữu ích để thiết lập trạng thái ứng dụng hoặc token.",
            category = "Web/Storage",
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
            category = "Web/Storage",
            parameters = {
                    "key: String - Khóa (key) của giá trị cần đọc"
            },
            returnValue = "String - Giá trị được lưu trữ trong Local Storage với khóa đã chỉ định, hoặc null nếu không tìm thấy",
            example = "// Kiểm tra token xác thực\n" +
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
    @Step("Đọc từ Local Storage với key='{0}'")
    public String getLocalStorage(String key) {
        return execute(() -> (String) ((JavascriptExecutor) DriverManager.getDriver()).executeScript("return localStorage.getItem(arguments[0]);", key), key);
    }

    @NetatKeyword(
            name = "clearLocalStorage",
            description = "Xóa toàn bộ dữ liệu đang được lưu trữ trong Local Storage của trang web hiện tại.",
            category = "Web/Storage",
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
    @Step("Xóa toàn bộ Local Storage")
    public void clearLocalStorage() {
        execute(() -> {
            ((JavascriptExecutor) DriverManager.getDriver()).executeScript("localStorage.clear();");
            return null;
        });
    }

    @NetatKeyword(
            name = "deleteAllCookies",
            description = "Xóa tất cả các cookie của phiên làm việc hiện tại trên trình duyệt.",
            category = "Web/Storage",
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
    @Step("Xóa tất cả cookies")
    public void deleteAllCookies() {
        execute(() -> {
            DriverManager.getDriver().manage().deleteAllCookies();
            return null;
        });
    }

    @NetatKeyword(
            name = "getCookie",
            description = "Lấy thông tin của một cookie cụ thể dựa trên tên của nó.",
            category = "Web/Storage",
            parameters = {
                    "cookieName: String - Tên của cookie cần lấy"
            },
            returnValue = "Cookie - Đối tượng Cookie chứa thông tin của cookie được yêu cầu, hoặc null nếu không tìm thấy",
            example = "// Kiểm tra cookie phiên làm việc\n" +
                    "Cookie sessionCookie = webKeyword.getCookie(\"session_id\");\n" +
                    "if (sessionCookie == null) {\n" +
                    "    webKeyword.navigate(\"https://example.com/login\");\n" +
                    "} else {\n" +
                    "    logger.info(\"Phiên làm việc: \" + sessionCookie.getValue());\n" +
                    "}\n\n" +
                    "// Kiểm tra thời hạn cookie\n" +
                    "Cookie authCookie = webKeyword.getCookie(\"auth_token\");\n" +
                    "logger.info(\"Cookie hết hạn vào: \" + authCookie.getExpiry());",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "trang web đã được tải hoàn toàn. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình duyệt."
    )
    @Step("Lấy cookie có tên: {0}")
    public Cookie getCookie(String cookieName) {
        return execute(() -> DriverManager.getDriver().manage().getCookieNamed(cookieName), cookieName);
    }


    // =================================================================================
    // --- 12. UTILITY & SUPPORT KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "takeScreenshot",
            description = "Chụp lại ảnh toàn bộ màn hình (viewport) của trình duyệt và lưu vào một file tại đường dẫn được chỉ định.",
            category = "Web/Utility",
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
            category = "Web/Utility",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần chụp ảnh",
                    "filePath: String - Đường dẫn đầy đủ để lưu file ảnh"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chụp ảnh phần tử để kiểm tra hiển thị\n" +
                    "webKeyword.waitForElementVisible(loginFormObject, 10);\n" +
                    "webKeyword.takeElementScreenshot(loginFormObject, \"D:/screenshots/login_form.png\");\n\n" +
                    "// Chụp ảnh phần tử khi gặp lỗi hiển thị\n" +
                    "if (!webKeyword.verifyElementTextContains(errorMessageObject, \"Invalid credentials\")) {\n" +
                    "    webKeyword.takeElementScreenshot(errorMessageObject, \"D:/screenshots/error.png\");\n" +
                    "}",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần chụp phải hiển thị trên màn hình, " +
                    "thư mục đích phải tồn tại hoặc có quyền tạo thư mục. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "ElementNotVisibleException nếu phần tử không hiển thị, " +
                    "RuntimeException nếu không thể chụp hoặc lưu ảnh phần tử, " +
                    "IOException nếu có lỗi khi ghi file, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Utility",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần làm nổi bật"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Làm nổi bật các phần tử trong quá trình điền form\n" +
                    "webKeyword.highlightElement(usernameFieldObject);\n" +
                    "webKeyword.sendKeys(usernameFieldObject, \"admin@example.com\");\n" +
                    "webKeyword.highlightElement(passwordFieldObject);\n" +
                    "webKeyword.sendKeys(passwordFieldObject, \"password123\");\n\n" +
                    "// Làm nổi bật phần tử để gỡ lỗi\n" +
                    "webKeyword.waitForElementVisible(tableRowObject, 10);\n" +
                    "webKeyword.highlightElement(tableRowObject);\n" +
                    "webKeyword.takeElementScreenshot(tableRowObject, \"D:/screenshots/table_row.png\");",
            note = "Áp dụng cho nền tảng Web. WebDriver đã được khởi tạo và đang hoạt động, " +
                    "phần tử cần làm nổi bật phải tồn tại trong DOM. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình duyệt."
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
            category = "Web/Utility",
            parameters = {
                    "milliseconds: int - Thời gian cần tạm dừng (tính bằng mili giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Tạm dừng để đợi animation hoàn thành\n" +
                    "webKeyword.click(expandMenuButtonObject);\n" +
                    "webKeyword.pause(1000); // Đợi 1 giây cho animation menu mở ra\n" +
                    "webKeyword.click(menuItemObject);\n\n" +
                    "// Tạm dừng để đợi dữ liệu được xử lý\n" +
                    "webKeyword.click(generateReportButtonObject);\n" +
                    "webKeyword.pause(3000); // Đợi 3 giây cho quá trình xử lý\n" +
                    "webKeyword.verifyElementVisibleHard(reportResultObject, 10);",
            note = "Áp dụng cho nền tảng Web. Không có điều kiện tiên quyết đặc biệt. " +
                    "Có thể throw InterruptedException nếu luồng thực thi bị gián đoạn trong khi tạm dừng."
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