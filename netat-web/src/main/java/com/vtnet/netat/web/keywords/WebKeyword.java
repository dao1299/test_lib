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
            description = "Tìm và trả về một danh sách (List) tất cả các phần tử WebElement khớp với locator được cung cấp. Trả về danh sách rỗng nếu không tìm thấy, không ném ra exception.",
            category = "Web/Finder",
            parameters = {"ObjectUI: uiObject - Đối tượng giao diện đại diện cho các phần tử cần tìm."},
            returnValue = "List<WebElement>: Danh sách các phần tử web khớp với locator, hoặc danh sách rỗng nếu không tìm thấy phần tử nào",
            example = "// Lấy danh sách tất cả các sản phẩm\n" +
                    "List<WebElement> productList = webKeyword.findElements(productListItemObject);\n\n" +
                    "// Đếm số lượng kết quả tìm kiếm\n" +
                    "int resultCount = webKeyword.findElements(searchResultObject).size();\n" +
                    "System.out.println(\"Tìm thấy \" + resultCount + \" kết quả\");\n\n" +
                    "// Lặp qua danh sách các phần tử để xử lý\n" +
                    "List<WebElement> rows = webKeyword.findElements(tableRowObject);\n" +
                    "for (WebElement row : rows) {\n" +
                    "    String text = row.getText();\n" +
                    "    if (text.contains(\"Khuyến mãi\")) {\n" +
                    "        row.click();\n" +
                    "        break;\n" +
                    "    }\n" +
                    "}\n\n" +
                    "// Kiểm tra danh sách rỗng\n" +
                    "List<WebElement> errorMessages = webKeyword.findElements(errorObject);\n" +
                    "Assert.assertTrue(errorMessages.isEmpty(), \"Không nên có lỗi hiển thị\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Đối tượng ObjectUI phải có ít nhất một locator được định nghĩa"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "InvalidSelectorException: Nếu locator không hợp lệ",
                    "NullPointerException: Nếu uiObject là null hoặc không có locator nào được kích hoạt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "finder", "elements", "collection", "list", "locate", "search", "multiple"}
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
            parameters = {"String: url - Địa chỉ trang web đầy đủ cần mở (ví dụ: 'https://www.google.com')."},
            returnValue = "void: Không trả về giá trị",
            example = "// Mở trang chủ Google\n" +
                    "webKeyword.openUrl(\"https://www.google.com\");\n\n" +
                    "// Mở trang đăng nhập\n" +
                    "webKeyword.openUrl(\"https://example.com/login\");\n\n" +
                    "// Mở URL với tham số truy vấn\n" +
                    "webKeyword.openUrl(\"https://example.com/search?q=selenium&lang=vi\");\n\n" +
                    "// Mở URL từ biến cấu hình\n" +
                    "String baseUrl = ConfigReader.getProperty(\"app.url\");\n" +
                    "webKeyword.openUrl(baseUrl + \"/dashboard\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "URL phải là một địa chỉ hợp lệ và có thể truy cập được",
                    "Kết nối mạng phải hoạt động"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "InvalidArgumentException: Nếu URL không hợp lệ",
                    "TimeoutException: Nếu trang không tải trong thời gian chờ mặc định"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "browser", "navigation", "url", "open", "load", "page", "visit"}
    )
    @Step("Mở URL: {0}")
    public void openUrl(String url) {
        DriverManager.getDriver().get(url);
    }

    @NetatKeyword(
            name = "goBack",
            description = "Thực hiện hành động quay lại trang trước đó trong lịch sử của trình duyệt, tương đương với việc người dùng nhấn nút 'Back'.",
            category = "Web/Browser",
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Quay lại trang trước sau khi đã điều hướng\n" +
                    "webKeyword.openUrl(\"https://example.com/page1\");\n" +
                    "webKeyword.openUrl(\"https://example.com/page2\");\n" +
                    "webKeyword.goBack(); // Quay lại page1\n\n" +
                    "// Quay lại sau khi nhấp vào liên kết\n" +
                    "webKeyword.click(linkToDetailsPage);\n" +
                    "webKeyword.waitForPageLoaded();\n" +
                    "webKeyword.goBack();\n\n" +
                    "// Kiểm tra URL sau khi quay lại\n" +
                    "webKeyword.goBack();\n" +
                    "String currentUrl = DriverManager.getDriver().getCurrentUrl();\n" +
                    "Assert.assertEquals(\"https://example.com/expected-page\", currentUrl);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phải có ít nhất một trang đã được truy cập trước đó trong lịch sử của phiên hiện tại"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "browser", "navigation", "back", "history", "previous", "return"}
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
            category = "Web/Browser",
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Điều hướng qua lại giữa các trang\n" +
                    "webKeyword.openUrl(\"https://example.com/page1\");\n" +
                    "webKeyword.openUrl(\"https://example.com/page2\");\n" +
                    "webKeyword.goBack(); // Quay lại page1\n" +
                    "webKeyword.goForward(); // Tiến tới page2 lần nữa\n\n" +
                    "// Kiểm tra luồng điều hướng\n" +
                    "webKeyword.click(productLink);\n" +
                    "webKeyword.waitForPageLoaded();\n" +
                    "webKeyword.goBack();\n" +
                    "webKeyword.waitForPageLoaded();\n" +
                    "webKeyword.goForward();\n" +
                    "webKeyword.verifyElementPresent(productDetail);\n\n" +
                    "// Kiểm tra URL sau khi đi tới\n" +
                    "webKeyword.goForward();\n" +
                    "String currentUrl = DriverManager.getDriver().getCurrentUrl();\n" +
                    "Assert.assertTrue(currentUrl.contains(\"/expected-forward-page\"));",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phải đã sử dụng goBack() hoặc có trang tiếp theo trong lịch sử điều hướng"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "browser", "navigation", "forward", "history", "next", "advance"}
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
            category = "Web/Browser",
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Làm mới trang hiện tại\n" +
                    "webKeyword.refresh();\n\n" +
                    "// Làm mới trang sau khi gửi biểu mẫu\n" +
                    "webKeyword.click(submitButton);\n" +
                    "webKeyword.waitForElementNotVisible(loadingIndicator);\n" +
                    "webKeyword.refresh();\n" +
                    "webKeyword.waitForPageLoaded();\n\n" +
                    "// Làm mới để xóa dữ liệu biểu mẫu\n" +
                    "webKeyword.sendText(usernameField, \"invalid_user\");\n" +
                    "webKeyword.sendText(passwordField, \"wrong_password\");\n" +
                    "webKeyword.refresh(); // Xóa dữ liệu đã nhập\n\n" +
                    "// Làm mới để cập nhật dữ liệu\n" +
                    "webKeyword.click(updateDataButton);\n" +
                    "webKeyword.waitForElementVisible(successMessage);\n" +
                    "webKeyword.refresh();\n" +
                    "webKeyword.verifyElementText(lastUpdatedTime, expectedTime);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Đã tải một trang web trước đó"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ",
                    "TimeoutException: Nếu trang không tải lại trong thời gian chờ mặc định"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "browser", "reload", "refresh", "update", "reset", "F5"}
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
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Phóng to cửa sổ trình duyệt khi bắt đầu kiểm thử\n" +
                    "webKeyword.openUrl(\"https://example.com\");\n" +
                    "webKeyword.maximizeWindow();\n\n" +
                    "// Phóng to cửa sổ trước khi chụp ảnh màn hình\n" +
                    "webKeyword.maximizeWindow();\n" +
                    "webKeyword.takeScreenshot(\"full_page\");\n\n" +
                    "// Phóng to cửa sổ để xem các phần tử ẩn\n" +
                    "webKeyword.openUrl(\"https://example.com/responsive-page\");\n" +
                    "webKeyword.maximizeWindow();\n" +
                    "webKeyword.verifyElementVisible(menuItemOnlyVisibleOnLargeScreen);\n\n" +
                    "// Phóng to cửa sổ để kiểm tra bố cục responsive\n" +
                    "webKeyword.maximizeWindow();\n" +
                    "int headerWidth = webKeyword.findElement(headerElement).getSize().getWidth();\n" +
                    "int screenWidth = DriverManager.getDriver().manage().window().getSize().getWidth();\n" +
                    "Assert.assertEquals(headerWidth, screenWidth, \"Header phải có chiều rộng bằng với màn hình\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trình duyệt phải hỗ trợ thay đổi kích thước cửa sổ"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ",
                    "UnsupportedOperationException: Nếu trình duyệt không hỗ trợ thay đổi kích thước"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "browser", "window", "maximize", "resize", "fullscreen", "display", "viewport"}
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
                    "int: width - Chiều rộng mới của cửa sổ (pixel).",
                    "int: height - Chiều cao mới của cửa sổ (pixel)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Thay đổi kích thước cửa sổ thành HD (720p)\n" +
                    "webKeyword.resizeWindow(1280, 720);\n\n" +
                    "// Thay đổi kích thước thành Full HD\n" +
                    "webKeyword.resizeWindow(1920, 1080);\n\n" +
                    "// Mô phỏng kích thước màn hình tablet\n" +
                    "webKeyword.resizeWindow(768, 1024);\n" +
                    "webKeyword.verifyElementVisible(tabletMenuIcon);\n\n" +
                    "// Mô phỏng kích thước điện thoại di động\n" +
                    "webKeyword.resizeWindow(375, 667); // iPhone 8\n" +
                    "webKeyword.verifyElementVisible(mobileNavigation);\n\n" +
                    "// Kiểm tra tính năng responsive\n" +
                    "webKeyword.resizeWindow(1200, 800);\n" +
                    "webKeyword.verifyElementVisible(desktopMenu);\n" +
                    "webKeyword.resizeWindow(600, 800);\n" +
                    "webKeyword.verifyElementNotVisible(desktopMenu);\n" +
                    "webKeyword.verifyElementVisible(mobileMenu);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trình duyệt phải hỗ trợ thay đổi kích thước cửa sổ",
                    "Kích thước yêu cầu phải nằm trong giới hạn hợp lý (lớn hơn 0 và nhỏ hơn kích thước màn hình vật lý)"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ",
                    "UnsupportedOperationException: Nếu trình duyệt không hỗ trợ thay đổi kích thước",
                    "IllegalArgumentException: Nếu chiều rộng hoặc chiều cao là số âm"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "browser", "window", "resize", "dimension", "responsive", "viewport", "size", "width", "height"}
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
            parameters = {"ObjectUI: uiObject - Đối tượng giao diện (nút bấm, liên kết,...) cần thực hiện hành động click."},
            returnValue = "void: Không trả về giá trị",
            example = "// Click vào nút đăng nhập\n" +
                    "webKeyword.click(loginButtonObject);\n\n" +
                    "// Click vào liên kết\n" +
                    "webKeyword.click(registerLinkObject);\n\n" +
                    "// Click vào tab\n" +
                    "webKeyword.click(productTabObject);\n" +
                    "webKeyword.waitForElementVisible(productListObject);\n\n" +
                    "// Click vào nút tìm kiếm sau khi nhập từ khóa\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n\n" +
                    "// Click để mở dropdown menu\n" +
                    "webKeyword.click(dropdownMenuObject);\n" +
                    "webKeyword.waitForElementVisible(dropdownItemsObject);\n" +
                    "webKeyword.click(dropdownItemObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần click phải tồn tại trong DOM",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Phần tử không bị che khuất bởi các phần tử khác"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementClickInterceptedException: Nếu phần tử bị che khuất bởi phần tử khác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "click", "button", "link", "element", "action", "mouse", "tap"}
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
                    "ObjectUI: uiObject - Ô input hoặc textarea cần nhập dữ liệu.",
                    "String: text - Chuỗi văn bản cần nhập vào phần tử."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Nhập tên đăng nhập\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"my_username\");\n\n" +
                    "// Nhập mật khẩu\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"secure_password123\");\n\n" +
                    "// Nhập nội dung tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop gaming\");\n\n" +
                    "// Nhập nội dung vào textarea\n" +
                    "webKeyword.sendKeys(commentTextareaObject, \"Đây là một bình luận dài về sản phẩm này. Tôi rất hài lòng với chất lượng và giá cả.\");\n\n" +
                    "// Nhập dữ liệu từ biến\n" +
                    "String email = \"user\" + System.currentTimeMillis() + \"@example.com\";\n" +
                    "webKeyword.sendKeys(emailInputObject, email);\n\n" +
                    "// Nhập dữ liệu có ký tự đặc biệt\n" +
                    "webKeyword.sendKeys(codeInputObject, \"function test() { return true; }\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần nhập liệu phải tồn tại trong DOM",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Phần tử phải là loại có thể nhập liệu (input, textarea, contenteditable)"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "input", "text", "type", "enter", "fill", "form", "data", "keyboard"}
    )
    @Step("Nhập văn bản '{1}' vào phần tử: {0.name}")
    public void sendKeys(ObjectUI uiObject, String text) {
        super.sendKeys(uiObject, text);
    }

    @NetatKeyword(
            name = "clearText",
            description = "Xóa toàn bộ văn bản đang có trong một phần tử có thể nhập liệu như ô input hoặc textarea.",
            category = "Web/Interaction",
            parameters = {"ObjectUI: uiObject - Phần tử cần xóa nội dung."},
            returnValue = "void: Không trả về giá trị",
            example = "// Xóa nội dung trong ô tìm kiếm\n" +
                    "webKeyword.clearText(searchInputObject);\n\n" +
                    "// Xóa nội dung trước khi nhập dữ liệu mới\n" +
                    "webKeyword.clearText(usernameInputObject);\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"new_username\");\n\n" +
                    "// Xóa nội dung trong textarea\n" +
                    "webKeyword.clearText(commentTextareaObject);\n\n" +
                    "// Xóa nội dung trong nhiều trường input\n" +
                    "webKeyword.clearText(firstNameInputObject);\n" +
                    "webKeyword.clearText(lastNameInputObject);\n" +
                    "webKeyword.clearText(emailInputObject);\n\n" +
                    "// Xóa nội dung và kiểm tra rỗng\n" +
                    "webKeyword.clearText(messageInputObject);\n" +
                    "String value = webKeyword.findElement(messageInputObject).getAttribute(\"value\");\n" +
                    "Assert.assertEquals(\"\", value, \"Trường input phải rỗng sau khi xóa\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần xóa nội dung phải tồn tại trong DOM",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Phần tử phải là loại có thể nhập liệu (input, textarea, contenteditable)"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "clear", "input", "text", "delete", "remove", "erase", "form", "reset"}
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
            category = "Web/Interaction",
            parameters = {"ObjectUI: uiObject - Phần tử checkbox hoặc radio button cần kiểm tra và chọn."},
            returnValue = "void: Không trả về giá trị",
            example = "// Đảm bảo checkbox Điều khoản và Điều kiện đã được chọn\n" +
                    "webKeyword.check(termsAndConditionsCheckbox);\n\n" +
                    "// Chọn phương thức thanh toán\n" +
                    "webKeyword.check(creditCardRadioButton);\n\n" +
                    "// Đăng ký nhận thông báo\n" +
                    "webKeyword.check(newsletterCheckbox);\n\n" +
                    "// Chọn nhiều tùy chọn\n" +
                    "webKeyword.check(expressShippingCheckbox);\n" +
                    "webKeyword.check(giftWrappingCheckbox);\n\n" +
                    "// Chọn và kiểm tra trạng thái\n" +
                    "webKeyword.check(rememberMeCheckbox);\n" +
                    "boolean isSelected = webKeyword.findElement(rememberMeCheckbox).isSelected();\n" +
                    "Assert.assertTrue(isSelected, \"Checkbox 'Ghi nhớ đăng nhập' phải được chọn\");\n\n" +
                    "// Chọn một radio button trong nhóm\n" +
                    "webKeyword.check(maleGenderRadio); // Sẽ tự động bỏ chọn các radio button khác trong cùng nhóm",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần chọn phải tồn tại trong DOM",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Phần tử phải là checkbox hoặc radio button (input type=\"checkbox\" hoặc type=\"radio\")"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "checkbox", "radio", "select", "check", "toggle", "form", "input", "boolean"}
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
            category = "Web/Interaction",
            parameters = {"ObjectUI: uiObject - Phần tử checkbox cần bỏ chọn."},
            returnValue = "void: Không trả về giá trị",
            example = "// Bỏ chọn checkbox đăng ký nhận bản tin\n" +
                    "webKeyword.uncheck(newsletterCheckbox);\n\n" +
                    "// Bỏ chọn tùy chọn gửi hàng nhanh\n" +
                    "webKeyword.uncheck(expressShippingCheckbox);\n\n" +
                    "// Bỏ chọn và kiểm tra trạng thái\n" +
                    "webKeyword.uncheck(saveCardDetailsCheckbox);\n" +
                    "boolean isSelected = webKeyword.findElement(saveCardDetailsCheckbox).isSelected();\n" +
                    "Assert.assertFalse(isSelected, \"Checkbox 'Lưu thông tin thẻ' không được chọn\");\n\n" +
                    "// Bỏ chọn nhiều checkbox\n" +
                    "webKeyword.uncheck(emailNotificationCheckbox);\n" +
                    "webKeyword.uncheck(smsNotificationCheckbox);\n" +
                    "webKeyword.uncheck(pushNotificationCheckbox);\n\n" +
                    "// Bỏ chọn tất cả các checkbox trong một danh sách\n" +
                    "List<WebElement> optionalFeatures = webKeyword.findElements(optionalFeaturesCheckboxes);\n" +
                    "for (WebElement feature : optionalFeatures) {\n" +
                    "    if (feature.isSelected()) {\n" +
                    "        feature.click();\n" +
                    "    }\n" +
                    "}",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần bỏ chọn phải tồn tại trong DOM",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Phần tử phải là checkbox (input type=\"checkbox\")",
                    "Lưu ý: Phương thức này chỉ hoạt động với checkbox, không dùng cho radio button"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "checkbox", "uncheck", "deselect", "unselect", "toggle", "form", "input", "boolean"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần thực hiện hành động click chuột phải."},
            returnValue = "void: Không trả về giá trị",
            example = "// Click chuột phải vào biểu tượng file\n" +
                    "webKeyword.contextClick(fileIconObject);\n" +
                    "webKeyword.waitForElementVisible(contextMenuObject);\n\n" +
                    "// Click chuột phải vào hình ảnh để tải xuống\n" +
                    "webKeyword.contextClick(productImageObject);\n" +
                    "webKeyword.click(saveImageOptionObject);\n\n" +
                    "// Click chuột phải vào ô trong bảng\n" +
                    "webKeyword.contextClick(tableCellObject);\n" +
                    "webKeyword.click(editCellOptionObject);\n\n" +
                    "// Click chuột phải vào văn bản để sao chép\n" +
                    "webKeyword.contextClick(selectedTextObject);\n" +
                    "webKeyword.click(copyOptionObject);\n\n" +
                    "// Click chuột phải để mở menu tùy chọn nâng cao\n" +
                    "webKeyword.contextClick(documentObject);\n" +
                    "webKeyword.waitForElementVisible(advancedOptionsMenu);\n" +
                    "webKeyword.click(propertiesOption);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần click phải tồn tại trong DOM",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Phần tử không bị che khuất bởi các phần tử khác",
                    "Trình duyệt phải hỗ trợ thao tác chuột phải (một số trình duyệt di động có thể không hỗ trợ)"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "MoveTargetOutOfBoundsException: Nếu phần tử nằm ngoài viewport hiện tại"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "right-click", "context-click", "context-menu", "mouse", "action", "advanced"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần thực hiện double-click."},
            returnValue = "void: Không trả về giá trị",
            example = "// Double-click vào biểu tượng chỉnh sửa\n" +
                    "webKeyword.doubleClick(editIconObject);\n" +
                    "webKeyword.waitForElementVisible(editFormObject);\n\n" +
                    "// Double-click để chọn toàn bộ văn bản\n" +
                    "webKeyword.doubleClick(textParagraphObject);\n" +
                    "webKeyword.verifyElementAttribute(textParagraphObject, \"class\", \"selected\");\n\n" +
                    "// Double-click vào ô trong bảng để chỉnh sửa\n" +
                    "webKeyword.doubleClick(tableCellObject);\n" +
                    "webKeyword.waitForElementVisible(cellEditModeObject);\n" +
                    "webKeyword.sendKeys(cellEditModeObject, \"Giá trị mới\");\n\n" +
                    "// Double-click để mở file\n" +
                    "webKeyword.doubleClick(fileIconObject);\n" +
                    "webKeyword.waitForElementVisible(fileContentObject);\n\n" +
                    "// Double-click để phóng to hình ảnh\n" +
                    "webKeyword.doubleClick(thumbnailImageObject);\n" +
                    "webKeyword.waitForElementVisible(enlargedImageObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần double-click phải tồn tại trong DOM",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Phần tử không bị che khuất bởi các phần tử khác"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "MoveTargetOutOfBoundsException: Nếu phần tử nằm ngoài viewport hiện tại"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "double-click", "dblclick", "mouse", "action", "select", "edit", "open"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần di chuột đến."},
            returnValue = "void: Không trả về giá trị",
            example = "// Di chuột đến menu chính để hiển thị menu con\n" +
                    "webKeyword.hover(mainMenuObject);\n" +
                    "webKeyword.waitForElementVisible(subMenuObject);\n" +
                    "webKeyword.click(subMenuItemObject);\n\n" +
                    "// Di chuột đến biểu tượng để hiển thị tooltip\n" +
                    "webKeyword.hover(infoIconObject);\n" +
                    "webKeyword.waitForElementVisible(tooltipObject);\n" +
                    "webKeyword.verifyElementText(tooltipObject, \"Thông tin chi tiết\");\n\n" +
                    "// Di chuột đến hình ảnh để hiển thị nút phóng to\n" +
                    "webKeyword.hover(productImageObject);\n" +
                    "webKeyword.waitForElementVisible(zoomButtonObject);\n" +
                    "webKeyword.click(zoomButtonObject);\n\n" +
                    "// Di chuột đến phần tử để kích hoạt hiệu ứng\n" +
                    "webKeyword.hover(cardObject);\n" +
                    "webKeyword.verifyElementAttribute(cardObject, \"class\", \"card-hover\");\n\n" +
                    "// Di chuột qua các mục trong thanh điều hướng\n" +
                    "List<WebElement> navItems = webKeyword.findElements(navItemsObject);\n" +
                    "for (WebElement item : navItems) {\n" +
                    "    new Actions(DriverManager.getDriver()).moveToElement(item).perform();\n" +
                    "    Thread.sleep(500); // Dừng để quan sát hiệu ứng\n" +
                    "}",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần hover phải tồn tại trong DOM",
                    "Phần tử phải hiển thị trên trang",
                    "Trình duyệt phải hỗ trợ thao tác di chuột (một số trình duyệt di động có thể không hỗ trợ)"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "MoveTargetOutOfBoundsException: Nếu phần tử nằm ngoài viewport hiện tại"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "interaction", "hover", "mouseover", "mouse", "tooltip", "submenu", "dropdown", "effect"}
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
                    "ObjectUI: uiObject - Phần tử input (type='file') để tải file lên.",
                    "String: filePath - Đường dẫn tuyệt đối đến file cần tải lên trên máy."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Tải lên ảnh đại diện\n" +
                    "webKeyword.uploadFile(avatarUploadInput, \"C:/Users/Tester/Pictures/avatar.jpg\");\n" +
                    "webKeyword.waitForElementVisible(uploadSuccessMessage);\n\n" +
                    "// Tải lên tài liệu PDF\n" +
                    "webKeyword.uploadFile(documentUploadInput, \"D:/Documents/report.pdf\");\n" +
                    "webKeyword.waitForElementVisible(documentPreviewObject);\n\n" +
                    "// Tải lên nhiều file cùng lúc (nếu input hỗ trợ multiple)\n" +
                    "webKeyword.uploadFile(multipleFilesInput, \"C:/Files/doc1.pdf\\nC:/Files/doc2.pdf\");\n\n" +
                    "// Tải lên file từ đường dẫn động\n" +
                    "String testDataPath = System.getProperty(\"user.dir\") + \"/src/test/resources/testdata/\";\n" +
                    "webKeyword.uploadFile(fileUploadInput, testDataPath + \"sample.xlsx\");\n\n" +
                    "// Kiểm tra sau khi tải lên\n" +
                    "webKeyword.uploadFile(csvUploadInput, \"D:/Data/users.csv\");\n" +
                    "webKeyword.waitForElementVisible(fileNameLabel);\n" +
                    "webKeyword.verifyElementText(fileNameLabel, \"users.csv\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử input phải có thuộc tính type='file'",
                    "Phần tử input phải tồn tại trong DOM (có thể ẩn nhưng phải tồn tại)",
                    "File cần tải lên phải tồn tại tại đường dẫn được chỉ định",
                    "Người dùng thực thi test phải có quyền truy cập vào file",
                    "Đường dẫn file phải là đường dẫn tuyệt đối"
            },
            exceptions = {
                    "InvalidArgumentException: Nếu đường dẫn file không hợp lệ hoặc file không tồn tại",
                    "ElementNotInteractableException: Nếu phần tử không phải là input type='file'",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "upload", "file", "input", "form", "attachment", "document", "image"}
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
                    "ObjectUI: sourceObject - Phần tử nguồn cần được kéo đi.",
                    "ObjectUI: targetObject - Phần tử đích, nơi phần tử nguồn sẽ được thả vào."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kéo và thả một mục vào giỏ hàng\n" +
                    "webKeyword.dragAndDrop(productItemObject, cartDropZoneObject);\n" +
                    "webKeyword.waitForElementVisible(cartNotificationObject);\n\n" +
                    "// Kéo và thả để sắp xếp lại danh sách\n" +
                    "webKeyword.dragAndDrop(taskItemObject, topOfListObject);\n" +
                    "webKeyword.verifyElementText(firstTaskObject, \"Nhiệm vụ đã kéo\");\n\n" +
                    "// Kéo và thả file vào khu vực tải lên\n" +
                    "webKeyword.dragAndDrop(fileIconObject, uploadAreaObject);\n" +
                    "webKeyword.waitForElementVisible(uploadProgressObject);\n\n" +
                    "// Kéo và thả để di chuyển phần tử trong bảng\n" +
                    "webKeyword.dragAndDrop(tableRowObject, targetRowPositionObject);\n" +
                    "webKeyword.waitForElementAttributeChange(tableObject, \"data-order-changed\", \"true\");\n\n" +
                    "// Kéo và thả để thay đổi kích thước phần tử\n" +
                    "webKeyword.dragAndDrop(resizeHandleObject, newSizePositionObject);\n" +
                    "int newWidth = webKeyword.findElement(resizableElementObject).getSize().getWidth();\n" +
                    "Assert.assertTrue(newWidth > originalWidth, \"Phần tử phải được thay đổi kích thước\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Cả hai phần tử nguồn và đích phải tồn tại trong DOM",
                    "Cả hai phần tử phải hiển thị và có thể tương tác được",
                    "Trang web phải hỗ trợ thao tác kéo và thả",
                    "Trình duyệt phải hỗ trợ thao tác kéo và thả (một số trình duyệt di động có thể không hỗ trợ đầy đủ)"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu một trong hai phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu một trong hai phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu một trong hai phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu một trong hai phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "MoveTargetOutOfBoundsException: Nếu phần tử đích nằm ngoài viewport hiện tại"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "MODERATE",
            tags = {"web", "interaction", "drag", "drop", "dragndrop", "move", "reorder", "sort", "mouse", "action"}
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
                    "ObjectUI: uiObject - Phần tử cần kéo.",
                    "int: xOffset - Độ lệch theo trục ngang (pixel).",
                    "int: yOffset - Độ lệch theo trục dọc (pixel)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kéo thanh trượt giá sang phải 100px\n" +
                    "webKeyword.dragAndDropByOffset(priceSliderHandle, 100, 0);\n" +
                    "webKeyword.verifyElementAttribute(priceValueObject, \"value\", \"500\");\n\n" +
                    "// Kéo thanh trượt âm lượng xuống 50px\n" +
                    "webKeyword.dragAndDropByOffset(volumeSliderObject, 0, -50);\n" +
                    "webKeyword.verifyElementAttribute(volumeValueObject, \"value\", \"25\");\n\n" +
                    "// Kéo phần tử theo đường chéo\n" +
                    "webKeyword.dragAndDropByOffset(draggableObject, 150, 100);\n" +
                    "Point newPosition = webKeyword.findElement(draggableObject).getLocation();\n" +
                    "Assert.assertTrue(newPosition.getX() > originalPosition.getX() + 140, \"Phần tử phải được di chuyển theo trục X\");\n" +
                    "Assert.assertTrue(newPosition.getY() > originalPosition.getY() + 90, \"Phần tử phải được di chuyển theo trục Y\");\n\n" +
                    "// Di chuyển phần tử trong canvas\n" +
                    "webKeyword.dragAndDropByOffset(canvasElementObject, 50, 30);\n" +
                    "webKeyword.verifyElementAttribute(canvasPositionObject, \"data-x\", \"50\");\n" +
                    "webKeyword.verifyElementAttribute(canvasPositionObject, \"data-y\", \"30\");\n\n" +
                    "// Điều chỉnh độ sáng trong trình chỉnh sửa ảnh\n" +
                    "webKeyword.dragAndDropByOffset(brightnessSliderObject, 75, 0);\n" +
                    "webKeyword.waitForElementAttributeChange(previewImageObject, \"data-brightness\", \"75\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kéo phải tồn tại trong DOM",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Trang web phải hỗ trợ thao tác kéo và thả",
                    "Trình duyệt phải hỗ trợ thao tác kéo và thả (một số trình duyệt di động có thể không hỗ trợ đầy đủ)"
            },
            exceptions = {
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "MoveTargetOutOfBoundsException: Nếu vị trí đích nằm ngoài viewport hiện tại"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "MODERATE",
            tags = {"web", "interaction", "drag", "offset", "slider", "move", "position", "coordinate", "mouse", "action"}
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
            parameters = {"CharSequence...: keys - Một hoặc nhiều chuỗi ký tự hoặc phím đặc biệt từ org.openqa.selenium.Keys."},
            returnValue = "void: Không trả về giá trị",
            example = "// Gửi tổ hợp phím Ctrl + A để chọn tất cả\n" +
                    "webKeyword.pressKeys(Keys.CONTROL, \"a\");\n\n" +
                    "// Gửi phím Enter để xác nhận form\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.pressKeys(Keys.ENTER);\n" +
                    "webKeyword.waitForElementVisible(searchResultsObject);\n\n" +
                    "// Gửi phím Tab để di chuyển giữa các trường form\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.pressKeys(Keys.TAB);\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n\n" +
                    "// Sao chép văn bản đã chọn\n" +
                    "webKeyword.doubleClick(textObject); // Chọn văn bản\n" +
                    "webKeyword.pressKeys(Keys.CONTROL, \"c\"); // Sao chép\n" +
                    "webKeyword.click(anotherInputObject);\n" +
                    "webKeyword.pressKeys(Keys.CONTROL, \"v\"); // Dán\n\n" +
                    "// Sử dụng phím mũi tên để điều hướng\n" +
                    "webKeyword.click(listItemObject);\n" +
                    "webKeyword.pressKeys(Keys.ARROW_DOWN, Keys.ARROW_DOWN); // Di chuyển xuống 2 mục\n" +
                    "webKeyword.pressKeys(Keys.ENTER); // Chọn mục hiện tại",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần nhận tổ hợp phím phải đang được focus",
                    "Trình duyệt phải hỗ trợ các tổ hợp phím được sử dụng",
                    "Các phím đặc biệt phải được định nghĩa trong org.openqa.selenium.Keys"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ",
                    "UnsupportedOperationException: Nếu trình duyệt không hỗ trợ thao tác phím được yêu cầu",
                    "IllegalArgumentException: Nếu tham số keys không hợp lệ"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "keyboard", "shortcut", "hotkey", "key", "press", "combination", "input", "action"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần click."},
            returnValue = "void: Không trả về giá trị",
            example = "// Click vào nút ẩn\n" +
                    "webKeyword.clickWithJavascript(hiddenButtonObject);\n" +
                    "webKeyword.waitForElementVisible(confirmationMessageObject);\n\n" +
                    "// Click vào phần tử bị che khuất bởi phần tử khác\n" +
                    "webKeyword.clickWithJavascript(overlappedElementObject);\n\n" +
                    "// Click vào phần tử không thể tương tác thông thường\n" +
                    "try {\n" +
                    "    webKeyword.click(disabledButtonObject); // Sẽ thất bại\n" +
                    "} catch (ElementNotInteractableException e) {\n" +
                    "    webKeyword.clickWithJavascript(disabledButtonObject); // Sẽ thành công\n" +
                    "}\n\n" +
                    "// Click vào phần tử nằm ngoài viewport\n" +
                    "webKeyword.clickWithJavascript(elementOutOfViewportObject);\n\n" +
                    "// Click vào phần tử có sự kiện JavaScript phức tạp\n" +
                    "webKeyword.clickWithJavascript(customJsButtonObject);\n" +
                    "webKeyword.waitForElementVisible(customDialogObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần click phải tồn tại trong DOM",
                    "Trình duyệt phải hỗ trợ thực thi JavaScript",
                    "Người dùng phải có quyền thực thi JavaScript trên trang"
            },
            exceptions = {
                    "JavascriptException: Nếu có lỗi khi thực thi JavaScript",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "javascript", "click", "js", "hidden", "disabled", "bypass", "element", "action"}
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
                    "ObjectUI: uiObject - Phần tử dropdown (thẻ select).",
                    "int: index - Chỉ số của tùy chọn cần chọn (ví dụ: 0 cho tùy chọn đầu tiên)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chọn tùy chọn thứ hai trong dropdown quốc gia\n" +
                    "webKeyword.selectByIndex(countryDropdownObject, 1); // Chỉ số bắt đầu từ 0\n" +
                    "webKeyword.verifyElementAttribute(selectedCountryObject, \"value\", \"USA\");\n\n" +
                    "// Chọn tùy chọn đầu tiên trong dropdown ngôn ngữ\n" +
                    "webKeyword.selectByIndex(languageDropdownObject, 0);\n" +
                    "webKeyword.verifyElementText(selectedLanguageObject, \"English\");\n\n" +
                    "// Chọn tùy chọn cuối cùng trong dropdown\n" +
                    "WebElement dropdown = webKeyword.findElement(categoryDropdownObject);\n" +
                    "Select select = new Select(dropdown);\n" +
                    "int lastIndex = select.getOptions().size() - 1;\n" +
                    "webKeyword.selectByIndex(categoryDropdownObject, lastIndex);\n\n" +
                    "// Chọn tùy chọn và kiểm tra sự thay đổi\n" +
                    "webKeyword.selectByIndex(sortDropdownObject, 2); // Chọn \"Giá: Cao đến thấp\"\n" +
                    "webKeyword.waitForElementAttributeChange(productListObject, \"data-sort\", \"price-desc\");\n\n" +
                    "// Lặp qua và chọn từng tùy chọn trong dropdown\n" +
                    "WebElement dropdown = webKeyword.findElement(yearDropdownObject);\n" +
                    "Select select = new Select(dropdown);\n" +
                    "int optionCount = select.getOptions().size();\n" +
                    "for (int i = 0; i < optionCount; i++) {\n" +
                    "    webKeyword.selectByIndex(yearDropdownObject, i);\n" +
                    "    webKeyword.waitForElementVisible(yearDataObject);\n" +
                    "    // Kiểm tra dữ liệu cho năm đã chọn\n" +
                    "}",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử dropdown phải tồn tại trong DOM",
                    "Phần tử phải là thẻ <select> hợp lệ",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Chỉ số phải nằm trong phạm vi hợp lệ (0 đến số lượng tùy chọn - 1)"
            },
            exceptions = {
                    "NoSuchElementException: Nếu phần tử dropdown không tồn tại",
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "IndexOutOfBoundsException: Nếu chỉ số nằm ngoài phạm vi hợp lệ",
                    "UnexpectedTagNameException: Nếu phần tử không phải là thẻ <select>",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "select", "dropdown", "option", "index", "form", "choose", "combobox", "list"}
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
                    "ObjectUI: uiObject - Đại diện cho nhóm radio button (ví dụ locator chung là '//input[@name=\"gender\"]').",
                    "String: value - Giá trị trong thuộc tính 'value' của radio button cần chọn."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chọn radio button giới tính nữ\n" +
                    "webKeyword.selectRadioByValue(genderRadioGroup, \"female\");\n" +
                    "webKeyword.verifyElementAttribute(genderRadioGroup + \"[@value='female']\", \"checked\", \"true\");\n\n" +
                    "// Chọn phương thức thanh toán\n" +
                    "webKeyword.selectRadioByValue(paymentMethodRadioGroup, \"credit_card\");\n" +
                    "webKeyword.waitForElementVisible(creditCardFormObject);\n\n" +
                    "// Chọn gói dịch vụ\n" +
                    "webKeyword.selectRadioByValue(servicePlanRadioGroup, \"premium\");\n" +
                    "webKeyword.verifyElementText(planPriceObject, \"$99.99\");\n\n" +
                    "// Chọn phương thức giao hàng\n" +
                    "webKeyword.selectRadioByValue(shippingMethodRadioGroup, \"express\");\n" +
                    "webKeyword.waitForElementAttributeChange(totalPriceObject, \"data-value\", \"129.99\");\n\n" +
                    "// Chọn radio button và kiểm tra các phần tử liên quan\n" +
                    "webKeyword.selectRadioByValue(accountTypeRadioGroup, \"business\");\n" +
                    "webKeyword.waitForElementVisible(businessDetailsFormObject);\n" +
                    "webKeyword.verifyElementNotVisible(personalDetailsFormObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Nhóm radio button phải tồn tại trong DOM",
                    "Ít nhất một radio button trong nhóm phải có thuộc tính 'value' khớp với giá trị cần chọn",
                    "Phần tử phải hiển thị và có thể tương tác được"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy radio button với value chỉ định",
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "radio", "button", "select", "value", "form", "choose", "option", "group"}
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
                    "ObjectUI: uiObject - Phần tử dropdown (thẻ select).",
                    "String: value - Giá trị thuộc tính 'value' của tùy chọn cần chọn."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chọn thành phố Hà Nội từ dropdown\n" +
                    "webKeyword.selectByValue(cityDropdown, \"HN\");\n" +
                    "webKeyword.verifyElementText(selectedCityLabel, \"Hà Nội\");\n\n" +
                    "// Chọn phương thức vận chuyển\n" +
                    "webKeyword.selectByValue(shippingMethodDropdown, \"express\");\n" +
                    "webKeyword.waitForElementText(deliveryTimeLabel, \"1-2 ngày\");\n\n" +
                    "// Chọn kích thước sản phẩm\n" +
                    "webKeyword.selectByValue(sizeDropdown, \"XL\");\n" +
                    "webKeyword.verifyElementAttribute(sizeDropdown, \"data-selected\", \"XL\");\n\n" +
                    "// Chọn quốc gia và kiểm tra thay đổi trong form\n" +
                    "webKeyword.selectByValue(countryDropdown, \"VN\");\n" +
                    "webKeyword.waitForElementVisible(vietnamProvinceDropdown);\n" +
                    "webKeyword.verifyElementNotVisible(stateDropdown); // US states dropdown should be hidden\n\n" +
                    "// Xử lý trường hợp không tìm thấy giá trị\n" +
                    "try {\n" +
                    "    webKeyword.selectByValue(categoryDropdown, \"non_existent_value\");\n" +
                    "} catch (NoSuchElementException e) {\n" +
                    "    System.out.println(\"Không tìm thấy tùy chọn với giá trị đã chỉ định\");\n" +
                    "    webKeyword.selectByValue(categoryDropdown, \"default\");\n" +
                    "}",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử dropdown phải tồn tại trong DOM",
                    "Phần tử phải là thẻ <select> hợp lệ",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Phải tồn tại ít nhất một tùy chọn có thuộc tính value khớp với giá trị cần chọn"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy tùy chọn với value chỉ định",
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "UnexpectedTagNameException: Nếu phần tử không phải là thẻ <select>",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "select", "dropdown", "value", "form", "choose", "option", "combobox", "list"}
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
                    "ObjectUI: uiObject - Phần tử dropdown (thẻ select).",
                    "String: text - Văn bản hiển thị của tùy chọn cần chọn."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chọn quốc gia từ dropdown theo tên hiển thị\n" +
                    "webKeyword.selectByVisibleText(countryDropdown, \"Việt Nam\");\n" +
                    "webKeyword.verifyElementAttribute(countryDropdown, \"value\", \"VN\");\n\n" +
                    "// Chọn danh mục sản phẩm\n" +
                    "webKeyword.selectByVisibleText(categoryDropdown, \"Điện thoại & Máy tính bảng\");\n" +
                    "webKeyword.waitForElementVisible(mobileProductsGrid);\n\n" +
                    "// Chọn ngôn ngữ hiển thị\n" +
                    "webKeyword.selectByVisibleText(languageDropdown, \"Tiếng Việt\");\n" +
                    "webKeyword.waitForPageToLoad();\n" +
                    "webKeyword.verifyElementText(welcomeMessage, \"Chào mừng bạn!\");\n\n" +
                    "// Chọn phương thức thanh toán\n" +
                    "webKeyword.selectByVisibleText(paymentMethodDropdown, \"Thanh toán khi nhận hàng\");\n" +
                    "webKeyword.waitForElementVisible(codInstructionsPanel);\n" +
                    "webKeyword.verifyElementNotVisible(cardDetailsForm);\n\n" +
                    "// Xử lý văn bản có khoảng trắng và ký tự đặc biệt\n" +
                    "webKeyword.selectByVisibleText(sortDropdown, \"Giá: Thấp đến cao (đề xuất)\");\n" +
                    "webKeyword.verifyElementAttribute(sortDropdown, \"data-sort\", \"price-asc\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử dropdown phải tồn tại trong DOM",
                    "Phần tử phải là thẻ <select> hợp lệ",
                    "Phần tử phải hiển thị và có thể tương tác được",
                    "Phải tồn tại ít nhất một tùy chọn có văn bản hiển thị khớp chính xác với text cần chọn",
                    "Văn bản cần chọn phải khớp chính xác với văn bản hiển thị (phân biệt chữ hoa/thường, khoảng trắng, ký tự đặc biệt)"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy tùy chọn với văn bản hiển thị chỉ định",
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "UnexpectedTagNameException: Nếu phần tử không phải là thẻ <select>",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "select", "dropdown", "text", "visible-text", "form", "choose", "option", "combobox", "list"}
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
                    "ObjectUI: uiObject - Đối tượng giao diện đại diện cho danh sách phần tử.",
                    "int: index - Vị trí của phần tử cần click (0 cho phần tử đầu tiên)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Click vào kết quả tìm kiếm thứ 3\n" +
                    "webKeyword.clickElementByIndex(searchResultLinks, 2); // Index bắt đầu từ 0\n" +
                    "webKeyword.waitForPageToLoad();\n" +
                    "webKeyword.verifyElementVisible(productDetailPage);\n\n" +
                    "// Click vào mục đầu tiên trong danh sách\n" +
                    "webKeyword.clickElementByIndex(menuItems, 0);\n" +
                    "webKeyword.waitForElementVisible(firstMenuContent);\n\n" +
                    "// Click vào nút xóa của hàng thứ 2 trong bảng\n" +
                    "webKeyword.clickElementByIndex(deleteButtons, 1);\n" +
                    "webKeyword.waitForElementVisible(confirmDeleteDialog);\n" +
                    "webKeyword.click(confirmButton);\n\n" +
                    "// Kiểm tra số lượng phần tử trước khi click\n" +
                    "List<WebElement> items = webKeyword.findElements(listItemsObject);\n" +
                    "if (items.size() > 3) {\n" +
                    "    webKeyword.clickElementByIndex(listItemsObject, 3);\n" +
                    "} else {\n" +
                    "    System.out.println(\"Không đủ phần tử để click vào vị trí thứ 4\");\n" +
                    "}\n\n" +
                    "// Click vào phần tử cuối cùng trong danh sách\n" +
                    "List<WebElement> tabs = webKeyword.findElements(tabsObject);\n" +
                    "webKeyword.clickElementByIndex(tabsObject, tabs.size() - 1);\n" +
                    "webKeyword.waitForElementVisible(lastTabContent);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Danh sách phần tử phải tồn tại trong DOM",
                    "Chỉ số phải nằm trong phạm vi hợp lệ (0 đến số lượng phần tử - 1)",
                    "Phần tử tại chỉ số cần click phải hiển thị và có thể tương tác được"
            },
            exceptions = {
                    "IndexOutOfBoundsException: Nếu chỉ số nằm ngoài phạm vi hợp lệ",
                    "ElementNotVisibleException: Nếu phần tử không hiển thị trên trang",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchElementException: Nếu danh sách phần tử không tồn tại"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "interaction", "click", "index", "list", "collection", "array", "element", "position", "multiple"}
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
            parameters = {"ObjectUI: uiObject - Phần tử đích cần cuộn đến."},
            returnValue = "void: Không trả về giá trị",
            example = "// Cuộn đến phần chân trang\n" +
                    "webKeyword.scrollToElement(footerSectionObject);\n" +
                    "webKeyword.click(privacyPolicyLinkObject);\n\n" +
                    "// Cuộn đến nút gửi ở cuối form\n" +
                    "webKeyword.scrollToElement(submitButtonObject);\n" +
                    "webKeyword.click(submitButtonObject);\n\n" +
                    "// Cuộn đến phần tử không hiển thị trong viewport\n" +
                    "webKeyword.scrollToElement(hiddenSectionObject);\n" +
                    "webKeyword.verifyElementVisible(hiddenSectionObject);\n\n" +
                    "// Cuộn đến phần tử và chờ cho hiệu ứng lazy-load hoạt động\n" +
                    "webKeyword.scrollToElement(lazyLoadSectionObject);\n" +
                    "webKeyword.waitForElementVisible(lazyLoadedImagesObject);\n\n" +
                    "// Cuộn đến phần tử và kiểm tra hiệu ứng animation\n" +
                    "webKeyword.scrollToElement(animatedSectionObject);\n" +
                    "webKeyword.waitForElementAttributeChange(animatedSectionObject, \"class\", \"section animated visible\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần cuộn đến phải tồn tại trong DOM",
                    "Trình duyệt phải hỗ trợ thực thi JavaScript"
            },
            exceptions = {
                    "NoSuchElementException: Nếu phần tử không tồn tại",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "JavascriptException: Nếu có lỗi khi thực thi JavaScript",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "interaction", "scroll", "view", "visibility", "element", "viewport", "page", "position", "javascript"}
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
                    "int: x - Tọa độ theo trục hoành (pixel).",
                    "int: y - Tọa độ theo trục tung (pixel)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Cuộn xuống 500px từ đầu trang\n" +
                    "webKeyword.scrollToCoordinates(0, 500);\n" +
                    "webKeyword.verifyElementVisible(midPageBannerObject);\n\n" +
                    "// Cuộn đến đầu trang\n" +
                    "webKeyword.scrollToCoordinates(0, 0);\n" +
                    "webKeyword.verifyElementVisible(headerObject);\n\n" +
                    "// Cuộn đến cuối trang\n" +
                    "webKeyword.executeJavaScript(\"return Math.max(document.documentElement.scrollHeight, document.body.scrollHeight)\", (height) -> {\n" +
                    "    webKeyword.scrollToCoordinates(0, (int) height);\n" +
                    "    return null;\n" +
                    "});\n" +
                    "webKeyword.verifyElementVisible(footerObject);\n\n" +
                    "// Cuộn đến vị trí cụ thể để hiển thị phần tử\n" +
                    "webKeyword.scrollToCoordinates(0, 1200);\n" +
                    "webKeyword.waitForElementVisible(middleSectionObject);\n\n" +
                    "// Cuộn theo chiều ngang trong bảng rộng\n" +
                    "webKeyword.scrollToCoordinates(500, 800);\n" +
                    "webKeyword.verifyElementVisible(tableRightColumnObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trình duyệt phải hỗ trợ thực thi JavaScript",
                    "Tọa độ phải nằm trong phạm vi hợp lệ của trang web"
            },
            exceptions = {
                    "JavascriptException: Nếu có lỗi khi thực thi JavaScript",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "interaction", "scroll", "coordinate", "position", "pixel", "viewport", "page", "javascript", "xy"}
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
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Cuộn lên đầu trang để truy cập menu chính\n" +
                    "webKeyword.scrollToTop();\n" +
                    "webKeyword.verifyElementVisible(mainMenuObject);\n" +
                    "webKeyword.click(homeMenuItemObject);\n\n" +
                    "// Cuộn xuống để xem sản phẩm, sau đó cuộn lại lên đầu trang\n" +
                    "webKeyword.scrollToElement(productSectionObject);\n" +
                    "webKeyword.verifyElementVisible(productItemObject);\n" +
                    "webKeyword.scrollToTop();\n" +
                    "webKeyword.verifyElementVisible(headerLogoObject);\n\n" +
                    "// Cuộn lên đầu trang sau khi hoàn thành form dài\n" +
                    "webKeyword.sendKeys(lastFormFieldObject, \"Test data\");\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "webKeyword.waitForElementVisible(confirmationMessageObject);\n" +
                    "webKeyword.scrollToTop();\n\n" +
                    "// Kiểm tra sticky header sau khi cuộn lên đầu trang\n" +
                    "webKeyword.scrollToBottom();\n" +
                    "webKeyword.verifyElementAttributeValue(headerObject, \"class\", \"contains\", \"sticky\");\n" +
                    "webKeyword.scrollToTop();\n" +
                    "webKeyword.verifyElementAttributeValue(headerObject, \"class\", \"not contains\", \"sticky\");\n\n" +
                    "// Cuộn lên đầu trang để reset trạng thái trang\n" +
                    "webKeyword.scrollToTop();\n" +
                    "webKeyword.executeJavaScript(\"return window.pageYOffset\", (yOffset) -> {\n" +
                    "    webKeyword.verifyEqual(yOffset, 0);\n" +
                    "    return null;\n" +
                    "});",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trình duyệt phải hỗ trợ thực thi JavaScript"
            },
            exceptions = {
                    "JavascriptException: Nếu có lỗi khi thực thi JavaScript",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "interaction", "scroll", "top", "page", "navigation", "position", "viewport", "javascript"}
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
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Cuộn xuống cuối trang để truy cập chân trang\n" +
                    "webKeyword.scrollToBottom();\n" +
                    "webKeyword.verifyElementVisible(footerLinksObject);\n" +
                    "webKeyword.click(contactUsLinkObject);\n\n" +
                    "// Cuộn xuống cuối trang để tải thêm dữ liệu (infinite scroll)\n" +
                    "webKeyword.scrollToBottom();\n" +
                    "webKeyword.waitForElementVisible(loadingIndicatorObject);\n" +
                    "webKeyword.waitForElementInvisible(loadingIndicatorObject);\n" +
                    "webKeyword.verifyElementCount(productItemsObject, 20); // Kiểm tra đã tải thêm sản phẩm\n\n" +
                    "// Kiểm tra nút \"Back to top\" xuất hiện khi cuộn xuống cuối trang\n" +
                    "webKeyword.scrollToBottom();\n" +
                    "webKeyword.waitForElementVisible(backToTopButtonObject);\n" +
                    "webKeyword.click(backToTopButtonObject);\n" +
                    "webKeyword.executeJavaScript(\"return window.pageYOffset\", (yOffset) -> {\n" +
                    "    webKeyword.verifyEqual(yOffset, 0);\n" +
                    "    return null;\n" +
                    "});\n\n" +
                    "// Cuộn xuống cuối trang để kiểm tra tất cả nội dung đã tải\n" +
                    "webKeyword.scrollToBottom();\n" +
                    "webKeyword.verifyElementVisible(copyrightTextObject);\n\n" +
                    "// Cuộn xuống cuối trang để đăng ký nhận bản tin\n" +
                    "webKeyword.scrollToBottom();\n" +
                    "webKeyword.sendKeys(newsletterEmailInputObject, \"test@example.com\");\n" +
                    "webKeyword.click(newsletterSubmitButtonObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trình duyệt phải hỗ trợ thực thi JavaScript"
            },
            exceptions = {
                    "JavascriptException: Nếu có lỗi khi thực thi JavaScript",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "interaction", "scroll", "bottom", "page", "navigation", "position", "viewport", "javascript", "footer"}
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
            parameters = {"ObjectUI: uiObject - Phần tử chứa văn bản cần lấy."},
            returnValue = "String: Văn bản của phần tử hoặc chuỗi rỗng nếu không có văn bản",
            example = "// Lấy văn bản từ phần tử hiển thị\n" +
                    "String welcomeMessage = webKeyword.getText(welcomeMessageObject);\n" +
                    "webKeyword.verifyEqual(welcomeMessage, \"Chào mừng bạn đến với trang web của chúng tôi!\");\n\n" +
                    "// Lấy giá trị từ ô input\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "String username = webKeyword.getText(usernameInputObject);\n" +
                    "webKeyword.verifyEqual(username, \"testuser\");\n\n" +
                    "// Lấy văn bản từ phần tử động\n" +
                    "webKeyword.click(showDetailsButtonObject);\n" +
                    "webKeyword.waitForElementVisible(detailsTextObject);\n" +
                    "String details = webKeyword.getText(detailsTextObject);\n" +
                    "webKeyword.verifyContains(details, \"Thông tin chi tiết\");\n\n" +
                    "// Lấy văn bản từ textarea\n" +
                    "webKeyword.sendKeys(commentTextareaObject, \"Đây là bình luận test\");\n" +
                    "String comment = webKeyword.getText(commentTextareaObject);\n" +
                    "webKeyword.verifyEqual(comment, \"Đây là bình luận test\");\n\n" +
                    "// Lấy văn bản từ phần tử có định dạng HTML\n" +
                    "String formattedText = webKeyword.getText(formattedTextObject);\n" +
                    "System.out.println(\"Văn bản đã lấy: \" + formattedText);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần lấy văn bản phải tồn tại trong DOM"
            },
            exceptions = {
                    "NoSuchElementException: Nếu phần tử không tồn tại",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "getter", "text", "value", "content", "read", "extract", "information", "input", "display"}
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
                    "ObjectUI: uiObject - Phần tử cần lấy thuộc tính.",
                    "String: attributeName - Tên của thuộc tính cần lấy giá trị (ví dụ: 'href', 'class', 'value')."
            },
            returnValue = "String: Giá trị của thuộc tính hoặc null nếu thuộc tính không tồn tại",
            example = "// Lấy URL từ thẻ liên kết\n" +
                    "String linkUrl = webKeyword.getAttribute(linkObject, \"href\");\n" +
                    "webKeyword.verifyContains(linkUrl, \"https://example.com\");\n\n" +
                    "// Kiểm tra trạng thái của checkbox\n" +
                    "String isChecked = webKeyword.getAttribute(termsCheckboxObject, \"checked\");\n" +
                    "if (isChecked != null && isChecked.equals(\"true\")) {\n" +
                    "    System.out.println(\"Checkbox đã được chọn\");\n" +
                    "}\n\n" +
                    "// Lấy ID của phần tử\n" +
                    "String elementId = webKeyword.getAttribute(formObject, \"id\");\n" +
                    "webKeyword.verifyEqual(elementId, \"registration-form\");\n\n" +
                    "// Kiểm tra trạng thái disabled của nút\n" +
                    "String isDisabled = webKeyword.getAttribute(submitButtonObject, \"disabled\");\n" +
                    "webKeyword.verifyNotNull(isDisabled); // Nút đang bị vô hiệu hóa\n\n" +
                    "// Lấy giá trị data attribute\n" +
                    "String productId = webKeyword.getAttribute(productItemObject, \"data-product-id\");\n" +
                    "webKeyword.verifyEqual(productId, \"12345\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần lấy thuộc tính phải tồn tại trong DOM"
            },
            exceptions = {
                    "NoSuchElementException: Nếu phần tử không tồn tại",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "getter", "attribute", "property", "html", "dom", "read", "extract", "information", "element"}
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
                    "ObjectUI: uiObject - Phần tử cần lấy giá trị CSS.",
                    "String: cssPropertyName - Tên của thuộc tính CSS (ví dụ: 'color', 'font-size', 'background-color')."
            },
            returnValue = "String: Giá trị của thuộc tính CSS được chỉ định",
            example = "// Lấy màu chữ của nút\n" +
                    "String buttonColor = webKeyword.getCssValue(buttonObject, \"color\");\n" +
                    "webKeyword.verifyEqual(buttonColor, \"rgba(255, 255, 255, 1)\"); // Màu trắng\n\n" +
                    "// Kiểm tra kích thước font\n" +
                    "String fontSize = webKeyword.getCssValue(headingObject, \"font-size\");\n" +
                    "webKeyword.verifyEqual(fontSize, \"24px\");\n\n" +
                    "// Kiểm tra thuộc tính hiển thị\n" +
                    "String display = webKeyword.getCssValue(hiddenElementObject, \"display\");\n" +
                    "webKeyword.verifyEqual(display, \"none\"); // Phần tử đang bị ẩn\n\n" +
                    "// Kiểm tra màu nền khi hover\n" +
                    "webKeyword.hoverElement(menuItemObject);\n" +
                    "String backgroundColor = webKeyword.getCssValue(menuItemObject, \"background-color\");\n" +
                    "webKeyword.verifyEqual(backgroundColor, \"rgba(240, 240, 240, 1)\"); // Màu xám nhạt\n\n" +
                    "// Kiểm tra thuộc tính border\n" +
                    "String borderStyle = webKeyword.getCssValue(inputErrorObject, \"border-color\");\n" +
                    "webKeyword.verifyEqual(borderStyle, \"rgba(255, 0, 0, 1)\"); // Màu đỏ",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần lấy giá trị CSS phải tồn tại trong DOM",
                    "Thuộc tính CSS cần lấy phải được áp dụng cho phần tử (trực tiếp hoặc được kế thừa)"
            },
            exceptions = {
                    "NoSuchElementException: Nếu phần tử không tồn tại",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "getter", "css", "style", "property", "visual", "read", "extract", "information", "design"}
    )
    @Step("Lấy giá trị CSS '{1}' của phần tử {0.name}")
    public String getCssValue(ObjectUI uiObject, String cssPropertyName) {
        return execute(() -> findElement(uiObject).getCssValue(cssPropertyName), uiObject, cssPropertyName);
    }

    @NetatKeyword(
            name = "getCurrentUrl",
            description = "Lấy và trả về URL đầy đủ của trang web hiện tại mà trình duyệt đang hiển thị.",
            category = "Web/Getter",
            parameters = {"Không có tham số."},
            returnValue = "String: URL đầy đủ của trang web hiện tại",
            example = "// Kiểm tra URL sau khi điều hướng\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/products\");\n" +
                    "String currentUrl = webKeyword.getCurrentUrl();\n" +
                    "webKeyword.verifyEqual(currentUrl, \"https://example.com/products\");\n\n" +
                    "// Kiểm tra URL sau khi gửi form\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "webKeyword.waitForPageToLoad();\n" +
                    "String resultUrl = webKeyword.getCurrentUrl();\n" +
                    "webKeyword.verifyContains(resultUrl, \"success=true\");\n\n" +
                    "// Lưu URL hiện tại để quay lại sau\n" +
                    "String originalUrl = webKeyword.getCurrentUrl();\n" +
                    "webKeyword.click(detailsLinkObject);\n" +
                    "webKeyword.waitForPageToLoad();\n" +
                    "// Thực hiện một số thao tác khác\n" +
                    "webKeyword.navigateToUrl(originalUrl); // Quay lại trang ban đầu\n\n" +
                    "// Kiểm tra chuyển hướng\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForPageToLoad();\n" +
                    "String redirectedUrl = webKeyword.getCurrentUrl();\n" +
                    "webKeyword.verifyContains(redirectedUrl, \"/auth/login\");\n\n" +
                    "// Trích xuất tham số từ URL\n" +
                    "String url = webKeyword.getCurrentUrl();\n" +
                    "if (url.contains(\"?id=\")) {\n" +
                    "    String id = url.split(\"\\\\?id=\")[1].split(\"&\")[0];\n" +
                    "    System.out.println(\"ID từ URL: \" + id);\n" +
                    "}",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "getter", "url", "address", "navigation", "read", "extract", "information", "location", "browser"}
    )
    @Step("Lấy URL hiện tại")
    public String getCurrentUrl() {
        return execute(() -> DriverManager.getDriver().getCurrentUrl());
    }

    @NetatKeyword(
            name = "getPageTitle",
            description = "Lấy và trả về tiêu đề (title) của trang web hiện tại.",
            category = "Web/Getter",
            parameters = {"Không có tham số."},
            returnValue = "String: Tiêu đề của trang web hiện tại",
            example = "// Kiểm tra tiêu đề trang sau khi điều hướng\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/about\");\n" +
                    "String pageTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.verifyEqual(pageTitle, \"Về chúng tôi - Example Company\");\n\n" +
                    "// Kiểm tra tiêu đề trang sau khi tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForPageToLoad();\n" +
                    "String searchResultTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.verifyContains(searchResultTitle, \"Kết quả tìm kiếm cho: laptop\");\n\n" +
                    "// Kiểm tra tiêu đề trang sau khi đăng nhập\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForPageToLoad();\n" +
                    "String dashboardTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.verifyEqual(dashboardTitle, \"Bảng điều khiển người dùng\");\n\n" +
                    "// Lưu tiêu đề trang để kiểm tra sau\n" +
                    "String originalTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.click(tabObject);\n" +
                    "webKeyword.waitForPageToLoad();\n" +
                    "String newTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.verifyNotEqual(originalTitle, newTitle);\n\n" +
                    "// Kiểm tra tiêu đề trang động\n" +
                    "webKeyword.click(productLinkObject);\n" +
                    "webKeyword.waitForPageToLoad();\n" +
                    "String productTitle = webKeyword.getPageTitle();\n" +
                    "webKeyword.verifyContains(productTitle, \"Sản phẩm:\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "getter", "title", "page", "header", "read", "extract", "information", "browser", "metadata"}
    )
    @Step("Lấy tiêu đề trang")
    public String getPageTitle() {
        return execute(() -> DriverManager.getDriver().getTitle());
    }

    @NetatKeyword(
            name = "getElementCount",
            description = "Đếm và trả về số lượng phần tử trên trang khớp với locator được cung cấp. Hữu ích để kiểm tra số lượng kết quả tìm kiếm, số hàng trong bảng,...",
            category = "Web/Getter",
            parameters = {"ObjectUI: uiObject - Đối tượng giao diện đại diện cho các phần tử cần đếm."},
            returnValue = "int: Số lượng phần tử tìm thấy",
            example = "// Đếm số lượng sản phẩm trong danh sách\n" +
                    "int numberOfProducts = webKeyword.getElementCount(productListItemObject);\n" +
                    "webKeyword.verifyEqual(numberOfProducts, 10);\n\n" +
                    "// Kiểm tra số lượng kết quả tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"smartphone\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForElementVisible(searchResultsObject);\n" +
                    "int resultCount = webKeyword.getElementCount(searchResultItemObject);\n" +
                    "System.out.println(\"Tìm thấy \" + resultCount + \" kết quả\");\n\n" +
                    "// Đếm số hàng trong bảng\n" +
                    "int rowCount = webKeyword.getElementCount(tableRowObject);\n" +
                    "webKeyword.verifyGreaterThan(rowCount, 0, \"Bảng không có dữ liệu\");\n\n" +
                    "// Kiểm tra số lượng lỗi hiển thị trong form\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form trống\n" +
                    "int errorCount = webKeyword.getElementCount(errorMessageObject);\n" +
                    "webKeyword.verifyEqual(errorCount, 3, \"Số lượng thông báo lỗi không đúng\");\n\n" +
                    "// Kiểm tra phân trang\n" +
                    "webKeyword.click(loadMoreButtonObject);\n" +
                    "webKeyword.waitForElementVisible(loadingIndicatorObject);\n" +
                    "webKeyword.waitForElementInvisible(loadingIndicatorObject);\n" +
                    "int newItemCount = webKeyword.getElementCount(listItemObject);\n" +
                    "webKeyword.verifyEqual(newItemCount, 20, \"Không tải thêm được 10 mục mới\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Locator của đối tượng giao diện phải hợp lệ"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "NoSuchSessionException: Nếu phiên WebDriver không còn hợp lệ",
                    "InvalidSelectorException: Nếu locator không hợp lệ"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "getter", "count", "elements", "size", "quantity", "read", "extract", "information", "list"}
    )
    @Step("Đếm số lượng phần tử của: {0.name}")
    public int getElementCount(ObjectUI uiObject) {
        return execute(() -> {
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return DriverManager.getDriver().findElements(by).size();
        }, uiObject);
    }


    // Thêm phương thức này vào bên trong class WebKeyword
    @NetatKeyword(
            name = "getTextFromElements",
            description = "Lấy và trả về một danh sách (List) các chuỗi văn bản từ mỗi phần tử trong một danh sách các phần tử.",
            category = "Web/Getter",
            parameters = {"ObjectUI: uiObject - Đối tượng giao diện đại diện cho các phần tử cần lấy văn bản."},
            returnValue = "List<String>: Danh sách các chuỗi văn bản từ các phần tử tìm thấy",
            example = "// Lấy danh sách tên sản phẩm\n" +
                    "List<String> productNames = webKeyword.getTextFromElements(productNameObject);\n" +
                    "System.out.println(\"Tìm thấy \" + productNames.size() + \" sản phẩm\");\n" +
                    "for (String name : productNames) {\n" +
                    "    System.out.println(\"- \" + name);\n" +
                    "}\n\n" +
                    "// Kiểm tra danh sách giá sản phẩm\n" +
                    "List<String> prices = webKeyword.getTextFromElements(productPriceObject);\n" +
                    "for (String price : prices) {\n" +
                    "    webKeyword.verifyTrue(price.contains(\"₫\"), \"Giá không đúng định dạng tiền Việt Nam\");\n" +
                    "}\n\n" +
                    "// Kiểm tra thứ tự sắp xếp theo bảng chữ cái\n" +
                    "List<String> categoryNames = webKeyword.getTextFromElements(categoryListObject);\n" +
                    "List<String> sortedNames = new ArrayList<>(categoryNames);\n" +
                    "Collections.sort(sortedNames);\n" +
                    "webKeyword.verifyEqual(categoryNames, sortedNames, \"Danh mục không được sắp xếp theo bảng chữ cái\");\n\n" +
                    "// Tìm kiếm một giá trị cụ thể trong danh sách\n" +
                    "List<String> menuItems = webKeyword.getTextFromElements(menuItemObject);\n" +
                    "boolean hasLogout = menuItems.stream().anyMatch(item -> item.contains(\"Đăng xuất\"));\n" +
                    "webKeyword.verifyTrue(hasLogout, \"Menu không chứa tùy chọn đăng xuất\");\n\n" +
                    "// Lấy và xử lý danh sách lỗi\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form không hợp lệ\n" +
                    "List<String> errorMessages = webKeyword.getTextFromElements(errorMessageObject);\n" +
                    "webKeyword.verifyGreaterThan(errorMessages.size(), 0, \"Không hiển thị thông báo lỗi\");\n" +
                    "webKeyword.verifyTrue(errorMessages.stream().anyMatch(msg -> msg.contains(\"Email\")), \"Không có lỗi về email\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Locator của đối tượng giao diện phải hợp lệ",
                    "Các phần tử cần lấy văn bản phải tồn tại trong DOM"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử nào khớp với locator",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM trong quá trình xử lý",
                    "TimeoutException: Nếu các phần tử không xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "getter", "text", "list", "collection", "multiple", "read", "extract", "information", "elements"}
    )
    @Step("Lấy văn bản từ danh sách các phần tử: {0.name}")
    public List<String> getTextFromElements(ObjectUI uiObject) {
        return execute(() -> {
            List<WebElement> elements = findElements(uiObject);
            // Sử dụng Stream API của Java 8 để xử lý một cách thanh lịch
            return elements.stream()
                    .map(WebElement::getText)
                    .collect(Collectors.toList());
        }, uiObject);
    }

    @NetatKeyword(
            name = "waitForElementClickable",
            description = "Tạm dừng kịch bản cho đến khi một phần tử không chỉ hiển thị mà còn ở trạng thái sẵn sàng để được click (enabled).",
            category = "Web/Wait",
            parameters = {"ObjectUI: uiObject - Phần tử cần chờ để sẵn sàng click."},
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ nút gửi sẵn sàng để click sau khi điền form\n" +
                    "webKeyword.sendKeys(emailInputObject, \"test@example.com\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.waitForElementClickable(submitButtonObject);\n" +
                    "webKeyword.click(submitButtonObject);\n\n" +
                    "// Chờ nút sẵn sàng sau khi tải dữ liệu\n" +
                    "webKeyword.click(loadDataButtonObject);\n" +
                    "webKeyword.waitForElementNotVisible(loadingIndicatorObject);\n" +
                    "webKeyword.waitForElementClickable(saveButtonObject);\n" +
                    "webKeyword.click(saveButtonObject);\n\n" +
                    "// Chờ nút được kích hoạt sau khi chọn một tùy chọn\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.waitForElementClickable(continueButtonObject);\n" +
                    "webKeyword.verifyElementEnabled(continueButtonObject);\n" +
                    "webKeyword.click(continueButtonObject);\n\n" +
                    "// Chờ tab trở nên có thể click sau khi tải xong\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/dashboard\");\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.waitForElementClickable(reportsTabObject);\n" +
                    "webKeyword.click(reportsTabObject);\n\n" +
                    "// Chờ nút trở nên có thể click sau khi xác thực\n" +
                    "webKeyword.sendKeys(otpInputObject, \"123456\");\n" +
                    "webKeyword.waitForElementClickable(verifyButtonObject);\n" +
                    "webKeyword.click(verifyButtonObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần chờ phải tồn tại trong DOM",
                    "Phần tử sẽ trở thành hiển thị và có thể click trong khoảng thời gian chờ"
            },
            exceptions = {
                    "TimeoutException: Nếu phần tử không trở nên có thể click trong thời gian chờ mặc định",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "wait", "synchronization", "clickable", "enabled", "interaction", "ready", "element", "condition", "timeout"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần chờ cho đến khi nó biến mất."},
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ biểu tượng loading biến mất sau khi gửi form\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "webKeyword.waitForElementNotVisible(loadingSpinnerObject);\n" +
                    "webKeyword.verifyElementVisible(successMessageObject);\n\n" +
                    "// Chờ thông báo tạm thời biến mất\n" +
                    "webKeyword.click(saveButtonObject);\n" +
                    "webKeyword.waitForElementVisible(toastMessageObject);\n" +
                    "webKeyword.waitForElementNotVisible(toastMessageObject);\n\n" +
                    "// Chờ popup đóng sau khi nhấn nút đóng\n" +
                    "webKeyword.click(openPopupButtonObject);\n" +
                    "webKeyword.waitForElementVisible(popupObject);\n" +
                    "webKeyword.click(closePopupButtonObject);\n" +
                    "webKeyword.waitForElementNotVisible(popupObject);\n\n" +
                    "// Chờ overlay mờ biến mất sau khi tải dữ liệu\n" +
                    "webKeyword.click(refreshButtonObject);\n" +
                    "webKeyword.waitForElementVisible(overlayObject);\n" +
                    "webKeyword.waitForElementNotVisible(overlayObject);\n" +
                    "webKeyword.verifyElementVisible(dataTableObject);\n\n" +
                    "// Chờ thông báo lỗi biến mất sau khi sửa lỗi\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form không hợp lệ\n" +
                    "webKeyword.waitForElementVisible(emailErrorObject);\n" +
                    "webKeyword.sendKeys(emailInputObject, \"valid@example.com\");\n" +
                    "webKeyword.waitForElementNotVisible(emailErrorObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần chờ phải tồn tại trong DOM hoặc đã hiển thị trước đó",
                    "Phần tử sẽ trở thành không hiển thị trong khoảng thời gian chờ"
            },
            exceptions = {
                    "TimeoutException: Nếu phần tử vẫn còn hiển thị sau thời gian chờ mặc định",
                    "NoSuchElementException: Nếu không tìm thấy phần tử ban đầu",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "wait", "synchronization", "invisible", "hidden", "disappear", "element", "condition", "timeout", "loading"}
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
                    "ObjectUI: uiObject - Phần tử cần chờ cho đến khi nó tồn tại.",
                    "int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ phần tử động được tạo bởi JavaScript\n" +
                    "webKeyword.click(loadDynamicContentButton);\n" +
                    "webKeyword.waitForElementPresent(dynamicContentObject, 10);\n" +
                    "webKeyword.verifyElementPresent(dynamicContentObject);\n\n" +
                    "// Chờ phần tử trong iframe được tải\n" +
                    "webKeyword.switchToFrame(iframeObject);\n" +
                    "webKeyword.waitForElementPresent(iframeContentObject, 15);\n" +
                    "webKeyword.verifyElementText(iframeContentObject, \"Nội dung trong iframe\");\n" +
                    "webKeyword.switchToDefaultContent();\n\n" +
                    "// Chờ phần tử được tạo sau khi gọi API\n" +
                    "webKeyword.click(fetchDataButtonObject);\n" +
                    "webKeyword.waitForElementPresent(dataContainerObject, 20);\n" +
                    "webKeyword.verifyElementAttributeValue(dataContainerObject, \"data-loaded\", \"true\");\n\n" +
                    "// Chờ phần tử được tạo sau khi chọn tùy chọn\n" +
                    "webKeyword.selectByVisibleText(categoryDropdownObject, \"Điện thoại\");\n" +
                    "webKeyword.waitForElementPresent(subcategoryListObject, 5);\n" +
                    "webKeyword.verifyElementCount(subcategoryItemObject, 5);\n\n" +
                    "// Chờ phần tử được tạo sau khi tải trang, với timeout dài hơn\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/heavy-page\");\n" +
                    "webKeyword.waitForElementPresent(complexWidgetObject, 30);\n" +
                    "webKeyword.click(widgetButtonObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Locator của phần tử phải hợp lệ",
                    "Phần tử sẽ được thêm vào DOM trong khoảng thời gian chờ đã chỉ định"
            },
            exceptions = {
                    "TimeoutException: Nếu phần tử không xuất hiện trong DOM trong thời gian chờ đã chỉ định",
                    "InvalidSelectorException: Nếu locator không hợp lệ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "wait", "synchronization", "present", "dom", "exist", "element", "condition", "timeout", "javascript"}
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
            parameters = {"int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."},
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ trang tải xong sau khi điều hướng\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/dashboard\");\n" +
                    "webKeyword.waitForPageLoaded(30);\n" +
                    "webKeyword.verifyElementVisible(dashboardWidgetsObject);\n\n" +
                    "// Chờ trang tải xong sau khi gửi form\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(20);\n" +
                    "webKeyword.verifyElementVisible(successMessageObject);\n\n" +
                    "// Chờ trang tải xong sau khi chuyển tab\n" +
                    "webKeyword.click(ordersTabObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.verifyElementVisible(orderListObject);\n\n" +
                    "// Chờ trang tải xong sau khi đăng nhập\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(25);\n" +
                    "webKeyword.verifyElementVisible(userProfileObject);\n\n" +
                    "// Chờ trang tải xong và kiểm tra URL\n" +
                    "webKeyword.click(checkoutButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(30);\n" +
                    "String currentUrl = webKeyword.getCurrentUrl();\n" +
                    "webKeyword.verifyContains(currentUrl, \"/checkout\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trình duyệt phải hỗ trợ thực thi JavaScript",
                    "Trang web sẽ hoàn thành quá trình tải trong khoảng thời gian chờ đã chỉ định"
            },
            exceptions = {
                    "TimeoutException: Nếu trang không tải xong trong thời gian chờ đã chỉ định",
                    "JavascriptException: Nếu có lỗi khi thực thi JavaScript",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "wait", "synchronization", "page", "load", "ready", "complete", "document", "timeout", "navigation"}
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
                    "String: partialUrl - Chuỗi con mà URL cần chứa.",
                    "int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ chuyển hướng đến trang dashboard\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForUrlContains(\"/dashboard\", 15);\n" +
                    "webKeyword.verifyElementVisible(welcomeMessageObject);\n\n" +
                    "// Chờ chuyển hướng sau khi hoàn thành thanh toán\n" +
                    "webKeyword.click(payNowButtonObject);\n" +
                    "webKeyword.waitForUrlContains(\"order-confirmation\", 30);\n" +
                    "webKeyword.verifyElementVisible(orderConfirmationObject);\n\n" +
                    "// Chờ chuyển hướng sau khi chọn danh mục sản phẩm\n" +
                    "webKeyword.click(electronicsLinkObject);\n" +
                    "webKeyword.waitForUrlContains(\"category=electronics\", 10);\n" +
                    "webKeyword.verifyElementVisible(productGridObject);\n\n" +
                    "// Chờ chuyển hướng sau khi tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForUrlContains(\"search=laptop\", 10);\n" +
                    "webKeyword.verifyElementVisible(searchResultsObject);\n\n" +
                    "// Chờ chuyển hướng sau khi đăng xuất\n" +
                    "webKeyword.click(logoutButtonObject);\n" +
                    "webKeyword.waitForUrlContains(\"/login\", 15);\n" +
                    "webKeyword.verifyElementVisible(loginFormObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "URL của trang sẽ chứa chuỗi con đã chỉ định trong khoảng thời gian chờ"
            },
            exceptions = {
                    "TimeoutException: Nếu URL không chứa chuỗi con đã chỉ định trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "wait", "synchronization", "url", "navigation", "redirect", "address", "location", "timeout", "browser"}
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
                    "String: expectedTitle - Tiêu đề trang mong đợi.",
                    "int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ tiêu đề trang sau khi tải xuống hoàn tất\n" +
                    "webKeyword.click(downloadButtonObject);\n" +
                    "webKeyword.waitForTitleIs(\"Tải xuống hoàn tất\", 20);\n" +
                    "webKeyword.verifyElementVisible(downloadCompleteMessageObject);\n\n" +
                    "// Chờ tiêu đề trang sau khi đăng nhập thành công\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForTitleIs(\"Bảng điều khiển người dùng\", 15);\n" +
                    "webKeyword.verifyElementVisible(dashboardWidgetsObject);\n\n" +
                    "// Chờ tiêu đề trang sau khi chuyển tab\n" +
                    "webKeyword.click(profileTabObject);\n" +
                    "webKeyword.waitForTitleIs(\"Thông tin cá nhân\", 10);\n" +
                    "webKeyword.verifyElementVisible(profileFormObject);\n\n" +
                    "// Chờ tiêu đề trang sau khi hoàn thành một quy trình\n" +
                    "webKeyword.click(completeOrderButtonObject);\n" +
                    "webKeyword.waitForTitleIs(\"Đặt hàng thành công\", 30);\n" +
                    "webKeyword.verifyElementVisible(orderConfirmationObject);\n\n" +
                    "// Chờ tiêu đề trang sau khi chuyển ngôn ngữ\n" +
                    "webKeyword.click(languageSelectorObject);\n" +
                    "webKeyword.click(englishOptionObject);\n" +
                    "webKeyword.waitForTitleIs(\"Welcome to our website\", 15);\n" +
                    "webKeyword.verifyElementVisible(englishContentObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Tiêu đề trang sẽ thay đổi thành giá trị mong đợi trong khoảng thời gian chờ"
            },
            exceptions = {
                    "TimeoutException: Nếu tiêu đề trang không khớp với giá trị mong đợi trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "wait", "synchronization", "title", "page", "exact", "match", "header", "timeout", "browser"}
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
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "boolean: isVisible - Trạng thái hiển thị mong đợi (true cho hiển thị, false cho bị ẩn)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra thông báo lỗi hiển thị sau khi gửi form không hợp lệ\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form trống\n" +
                    "webKeyword.verifyElementVisibleHard(errorMesssageObject, true);\n\n" +
                    "// Kiểm tra nút đăng nhập hiển thị trên trang chủ\n" +
                    "webKeyword.navigateToUrl(\"https://example.com\");\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyElementVisibleHard(loginButtonObject, true);\n\n" +
                    "// Kiểm tra phần tử không hiển thị sau khi đóng\n" +
                    "webKeyword.click(closePopupButtonObject);\n" +
                    "webKeyword.verifyElementVisibleHard(popupObject, false);\n\n" +
                    "// Kiểm tra menu dropdown hiển thị sau khi hover\n" +
                    "webKeyword.hoverElement(menuObject);\n" +
                    "webKeyword.verifyElementVisibleHard(dropdownMenuObject, true);\n\n" +
                    "// Kiểm tra biểu tượng loading không còn hiển thị sau khi tải xong\n" +
                    "webKeyword.click(refreshButtonObject);\n" +
                    "webKeyword.waitForElementNotVisible(loadingIconObject);\n" +
                    "webKeyword.verifyElementVisibleHard(loadingIconObject, false);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM"
            },
            exceptions = {
                    "AssertionError: Nếu trạng thái hiển thị của phần tử không khớp với kỳ vọng",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "visibility", "display", "element", "visible", "hidden", "validation"}
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
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "boolean: isVisible - Trạng thái hiển thị mong đợi (true cho hiển thị, false cho bị ẩn)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra thông báo thành công hiển thị sau khi lưu\n" +
                    "webKeyword.click(saveButtonObject);\n" +
                    "webKeyword.verifyElementVisibleSoft(successMessageObject, true);\n" +
                    "// Kịch bản vẫn tiếp tục ngay cả khi thông báo không hiển thị\n\n" +
                    "// Kiểm tra nhiều phần tử trên trang\n" +
                    "webKeyword.verifyElementVisibleSoft(headerLogoObject, true);\n" +
                    "webKeyword.verifyElementVisibleSoft(navigationMenuObject, true);\n" +
                    "webKeyword.verifyElementVisibleSoft(searchBarObject, true);\n" +
                    "webKeyword.verifyElementVisibleSoft(footerObject, true);\n\n" +
                    "// Kiểm tra các phần tử tùy chọn\n" +
                    "webKeyword.verifyElementVisibleSoft(promotionBannerObject, true); // Có thể không hiển thị nhưng kịch bản vẫn tiếp tục\n" +
                    "webKeyword.click(mainButtonObject); // Thực hiện hành động tiếp theo\n\n" +
                    "// Kiểm tra phần tử không hiển thị sau khi đóng\n" +
                    "webKeyword.click(closeNotificationObject);\n" +
                    "webKeyword.verifyElementVisibleSoft(notificationObject, false);\n" +
                    "webKeyword.click(nextButtonObject); // Tiếp tục ngay cả khi thông báo vẫn còn hiển thị\n\n" +
                    "// Kiểm tra nhiều điều kiện liên quan đến hiển thị\n" +
                    "webKeyword.verifyElementVisibleSoft(mainContentObject, true);\n" +
                    "webKeyword.verifyElementVisibleSoft(sidebarObject, true);\n" +
                    "webKeyword.verifyElementVisibleSoft(advertisementObject, false); // Quảng cáo không nên hiển thị",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "visibility", "display", "element", "visible", "hidden", "validation"}
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
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: expectedText - Chuỗi văn bản mong đợi."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra tiêu đề trang chính xác\n" +
                    "webKeyword.verifyTextHard(pageTitleObject, \"Chào mừng đến với trang chủ\");\n\n" +
                    "// Kiểm tra thông báo lỗi chính xác\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form không hợp lệ\n" +
                    "webKeyword.verifyTextHard(emailErrorObject, \"Email không được để trống\");\n\n" +
                    "// Kiểm tra kết quả tính toán\n" +
                    "webKeyword.sendKeys(number1InputObject, \"5\");\n" +
                    "webKeyword.sendKeys(number2InputObject, \"7\");\n" +
                    "webKeyword.click(calculateButtonObject);\n" +
                    "webKeyword.verifyTextHard(resultObject, \"12\");\n\n" +
                    "// Kiểm tra tên người dùng sau khi đăng nhập\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForElementVisible(welcomeMessageObject);\n" +
                    "webKeyword.verifyTextHard(userDisplayNameObject, \"Test User\");\n\n" +
                    "// Kiểm tra giá trị được hiển thị sau khi chọn\n" +
                    "webKeyword.selectByVisibleText(countryDropdownObject, \"Việt Nam\");\n" +
                    "webKeyword.verifyTextHard(selectedCountryObject, \"Việt Nam\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản"
            },
            exceptions = {
                    "AssertionError: Nếu văn bản của phần tử không khớp với giá trị mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "text", "content", "exact", "match", "string", "validation"}
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
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: expectedText - Chuỗi văn bản mong đợi."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra nhãn trên form đăng nhập\n" +
                    "webKeyword.verifyTextSoft(usernameLabelObject, \"Tên đăng nhập\");\n" +
                    "webKeyword.verifyTextSoft(passwordLabelObject, \"Mật khẩu\");\n" +
                    "webKeyword.verifyTextSoft(loginButtonTextObject, \"Đăng nhập\");\n" +
                    "// Kịch bản tiếp tục ngay cả khi có nhãn không khớp\n\n" +
                    "// Kiểm tra nhiều giá trị hiển thị\n" +
                    "webKeyword.verifyTextSoft(productNameObject, \"Điện thoại thông minh X1\");\n" +
                    "webKeyword.verifyTextSoft(productPriceObject, \"5.990.000 ₫\");\n" +
                    "webKeyword.verifyTextSoft(productStockObject, \"Còn hàng\");\n" +
                    "webKeyword.click(addToCartButtonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra thông tin người dùng\n" +
                    "webKeyword.verifyTextSoft(userFullNameObject, \"Nguyễn Văn A\");\n" +
                    "webKeyword.verifyTextSoft(userEmailObject, \"nguyenvana@example.com\");\n" +
                    "webKeyword.verifyTextSoft(userRoleObject, \"Người dùng\");\n\n" +
                    "// Kiểm tra thông tin đơn hàng\n" +
                    "webKeyword.verifyTextSoft(orderNumberObject, \"ORD-12345\");\n" +
                    "webKeyword.verifyTextSoft(orderDateObject, \"15/09/2023\");\n" +
                    "webKeyword.verifyTextSoft(orderStatusObject, \"Đang xử lý\");\n" +
                    "webKeyword.click(viewDetailsButtonObject); // Tiếp tục xem chi tiết",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "text", "content", "exact", "match", "string", "validation"}
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
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: partialText - Chuỗi văn bản con mong đợi."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra thông báo chào mừng có chứa tên người dùng\n" +
                    "webKeyword.verifyTextContainsHard(welcomeMessageObject, \"Xin chào\");\n\n" +
                    "// Kiểm tra thông báo lỗi chứa thông tin về trường bắt buộc\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form không hợp lệ\n" +
                    "webKeyword.verifyTextContainsHard(formErrorObject, \"bắt buộc\");\n\n" +
                    "// Kiểm tra mô tả sản phẩm có chứa từ khóa\n" +
                    "webKeyword.click(productCardObject);\n" +
                    "webKeyword.waitForElementVisible(productDescriptionObject);\n" +
                    "webKeyword.verifyTextContainsHard(productDescriptionObject, \"chống nước\");\n\n" +
                    "// Kiểm tra tiêu đề trang có chứa tên công ty\n" +
                    "webKeyword.verifyTextContainsHard(pageTitleObject, \"Example Corp\");\n\n" +
                    "// Kiểm tra kết quả tìm kiếm có chứa từ khóa đã tìm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForElementVisible(searchResultsObject);\n" +
                    "webKeyword.verifyTextContainsHard(searchResultTitleObject, \"laptop\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản"
            },
            exceptions = {
                    "AssertionError: Nếu văn bản của phần tử không chứa chuỗi con mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "text", "content", "contains", "substring", "partial", "validation"}
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
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: partialText - Chuỗi văn bản con mong đợi."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra kết quả tìm kiếm có chứa thông tin số lượng\n" +
                    "webKeyword.sendKeys(searchInputObject, \"điện thoại\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForElementVisible(searchResultSummary);\n" +
                    "webKeyword.verifyTextContainsSoft(searchResultSummary, \"kết quả\");\n" +
                    "webKeyword.verifyTextContainsSoft(searchResultSummary, \"điện thoại\");\n\n" +
                    "// Kiểm tra nhiều thông tin trên trang sản phẩm\n" +
                    "webKeyword.verifyTextContainsSoft(productDescriptionObject, \"chống nước\");\n" +
                    "webKeyword.verifyTextContainsSoft(productDescriptionObject, \"bảo hành\");\n" +
                    "webKeyword.verifyTextContainsSoft(productDescriptionObject, \"12 tháng\");\n" +
                    "webKeyword.click(addToCartButtonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra thông báo xác nhận có chứa thông tin đơn hàng\n" +
                    "webKeyword.click(placeOrderButtonObject);\n" +
                    "webKeyword.waitForElementVisible(confirmationMessageObject);\n" +
                    "webKeyword.verifyTextContainsSoft(confirmationMessageObject, \"đặt hàng thành công\");\n" +
                    "webKeyword.verifyTextContainsSoft(confirmationMessageObject, \"mã đơn hàng\");\n\n" +
                    "// Kiểm tra trang hồ sơ người dùng\n" +
                    "webKeyword.click(profileLinkObject);\n" +
                    "webKeyword.verifyTextContainsSoft(profileInfoObject, \"Nguyễn\"); // Tên người dùng\n" +
                    "webKeyword.verifyTextContainsSoft(profileInfoObject, \"@example.com\"); // Email\n\n" +
                    "// Kiểm tra thông báo lỗi\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form không hợp lệ\n" +
                    "webKeyword.verifyTextContainsSoft(validationMessageObject, \"không được để trống\");\n" +
                    "webKeyword.sendKeys(emailInputObject, \"test@example.com\"); // Tiếp tục điền form",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "text", "content", "contains", "substring", "partial", "validation"}
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
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: attributeName - Tên của thuộc tính (ví dụ: 'href', 'class', 'value').",
                    "String: expectedValue - Giá trị mong đợi của thuộc tính."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra đường dẫn của liên kết\n" +
                    "webKeyword.verifyElementAttributeHard(linkObject, \"href\", \"/products/123\");\n\n" +
                    "// Kiểm tra giá trị của trường nhập liệu\n" +
                    "webKeyword.sendKeys(emailInputObject, \"test@example.com\");\n" +
                    "webKeyword.verifyElementAttributeHard(emailInputObject, \"value\", \"test@example.com\");\n\n" +
                    "// Kiểm tra trạng thái của checkbox\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.verifyElementAttributeHard(termsCheckboxObject, \"checked\", \"true\");\n\n" +
                    "// Kiểm tra class của phần tử sau khi thay đổi trạng thái\n" +
                    "webKeyword.click(expandButtonObject);\n" +
                    "webKeyword.verifyElementAttributeHard(contentPanelObject, \"class\", \"panel-expanded\");\n\n" +
                    "// Kiểm tra thuộc tính data-* tùy chỉnh\n" +
                    "webKeyword.verifyElementAttributeHard(productCardObject, \"data-product-id\", \"12345\");\n\n" +
                    "// Kiểm tra thuộc tính src của hình ảnh\n" +
                    "webKeyword.verifyElementAttributeHard(logoImageObject, \"src\", \"https://example.com/logo.png\");\n\n" +
                    "// Kiểm tra thuộc tính disabled của nút\n" +
                    "webKeyword.verifyElementAttributeHard(submitButtonObject, \"disabled\", \"true\");\n" +
                    "webKeyword.sendKeys(requiredFieldObject, \"Some value\");\n" +
                    "webKeyword.verifyElementAttributeHard(submitButtonObject, \"disabled\", null); // Nút không còn bị vô hiệu hóa",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM",
                    "Thuộc tính cần kiểm tra phải tồn tại trên phần tử"
            },
            exceptions = {
                    "AssertionError: Nếu giá trị thuộc tính không khớp với giá trị mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "attribute", "property", "value", "element", "html", "validation"}
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
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: attributeName - Tên của thuộc tính (ví dụ: 'href', 'class', 'value').",
                    "String: expectedValue - Giá trị mong đợi của thuộc tính."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra thuộc tính alt của hình ảnh\n" +
                    "webKeyword.verifyElementAttributeSoft(imageObject, \"alt\", \"Mô tả hình ảnh\");\n\n" +
                    "// Kiểm tra nhiều thuộc tính của một phần tử\n" +
                    "webKeyword.verifyElementAttributeSoft(buttonObject, \"type\", \"submit\");\n" +
                    "webKeyword.verifyElementAttributeSoft(buttonObject, \"class\", \"btn-primary\");\n" +
                    "webKeyword.verifyElementAttributeSoft(buttonObject, \"data-action\", \"save\");\n" +
                    "webKeyword.click(buttonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra thuộc tính của nhiều phần tử\n" +
                    "webKeyword.verifyElementAttributeSoft(usernameInputObject, \"placeholder\", \"Nhập tên đăng nhập\");\n" +
                    "webKeyword.verifyElementAttributeSoft(passwordInputObject, \"placeholder\", \"Nhập mật khẩu\");\n" +
                    "webKeyword.verifyElementAttributeSoft(passwordInputObject, \"type\", \"password\");\n\n" +
                    "// Kiểm tra thuộc tính aria-* cho accessibility\n" +
                    "webKeyword.verifyElementAttributeSoft(modalObject, \"aria-hidden\", \"false\");\n" +
                    "webKeyword.verifyElementAttributeSoft(closeButtonObject, \"aria-label\", \"Đóng\");\n\n" +
                    "// Kiểm tra thuộc tính style inline\n" +
                    "webKeyword.verifyElementAttributeSoft(highlightedTextObject, \"style\", \"color: red;\");\n\n" +
                    "// Kiểm tra thuộc tính maxlength của trường nhập liệu\n" +
                    "webKeyword.verifyElementAttributeSoft(phoneInputObject, \"maxlength\", \"10\");\n" +
                    "webKeyword.sendKeys(phoneInputObject, \"0123456789\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM",
                    "Thuộc tính cần kiểm tra phải tồn tại trên phần tử"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "attribute", "property", "value", "element", "html", "validation"}
    )
    @Step("Kiểm tra (Soft) thuộc tính '{1}' của {0.name} là '{2}'")
    public void verifyElementAttributeSoft(ObjectUI uiObject, String attributeName, String expectedValue) {
        performAttributeAssertion(uiObject, attributeName, expectedValue, true);
    }

    @NetatKeyword(
            name = "verifyUrlHard",
            description = "So sánh URL của trang hiện tại với một chuỗi mong đợi (phải khớp chính xác). Nếu không khớp, kịch bản sẽ DỪNG LẠI.",
            category = "Web/Assert",
            parameters = {"String: expectedUrl - URL đầy đủ mong đợi."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra URL sau khi đăng nhập thành công\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForUrlContains(\"/dashboard\", 10);\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/dashboard\");\n\n" +
                    "// Kiểm tra URL sau khi chuyển hướng với tham số truy vấn\n" +
                    "webKeyword.click(checkoutButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/checkout?step=payment&session=abc123\");\n\n" +
                    "// Kiểm tra URL sau khi hoàn thành quy trình\n" +
                    "webKeyword.click(completeOrderButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(20);\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/order-confirmation\");\n\n" +
                    "// Kiểm tra URL sau khi chọn ngôn ngữ\n" +
                    "webKeyword.click(languageSelectorObject);\n" +
                    "webKeyword.click(englishOptionObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/en/home\");\n\n" +
                    "// Kiểm tra URL sau khi đăng xuất\n" +
                    "webKeyword.click(userMenuObject);\n" +
                    "webKeyword.click(logoutOptionObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyUrlHard(\"https://example.com/login?status=logout\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trang web đã hoàn thành quá trình tải"
            },
            exceptions = {
                    "AssertionError: Nếu URL hiện tại không khớp với URL mong đợi",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "url", "address", "location", "navigation", "browser", "validation"}
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
            parameters = {"String: expectedUrl - URL đầy đủ mong đợi."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra URL trong quy trình nhiều bước\n" +
                    "webKeyword.click(nextButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/checkout/step1\");\n" +
                    "webKeyword.fillCheckoutForm(); // Tiếp tục quy trình ngay cả khi URL không đúng\n\n" +
                    "// Kiểm tra nhiều điều kiện bao gồm URL\n" +
                    "webKeyword.verifyElementVisibleSoft(pageHeaderObject, true);\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/products\");\n" +
                    "webKeyword.verifyTextSoft(categoryTitleObject, \"Danh mục sản phẩm\");\n" +
                    "webKeyword.click(firstProductObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra URL sau khi áp dụng bộ lọc\n" +
                    "webKeyword.click(filterButtonObject);\n" +
                    "webKeyword.click(priceRangeObject);\n" +
                    "webKeyword.click(applyFilterButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/products?filter=price&range=1000-2000\");\n\n" +
                    "// Kiểm tra URL sau khi thêm sản phẩm vào giỏ hàng\n" +
                    "webKeyword.click(addToCartButtonObject);\n" +
                    "webKeyword.waitForElementVisible(cartConfirmationObject);\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/products?cart=updated\");\n" +
                    "webKeyword.click(continueShoppingButtonObject); // Tiếp tục mua sắm\n\n" +
                    "// Kiểm tra URL sau khi tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyUrlSoft(\"https://example.com/search?q=laptop\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trang web đã hoàn thành quá trình tải"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "url", "address", "location", "navigation", "browser", "validation"}
    )
    @Step("Kiểm tra (Soft) URL của trang là '{0}'")
    public void verifyUrlSoft(String expectedUrl) {
        execute(() -> {
            String actualUrl = DriverManager.getDriver().getCurrentUrl();
            logger.info("Kiểm tra tiêu đề: Mong đợi '{}', Thực tế: '{}'",expectedUrl,actualUrl);
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
            parameters = {"String: expectedTitle - Tiêu đề trang mong đợi."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra tiêu đề trang chủ\n" +
                    "webKeyword.navigateToUrl(\"https://example.com\");\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyTitleHard(\"Trang chủ - Website ABC\");\n\n" +
                    "// Kiểm tra tiêu đề sau khi đăng nhập\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.verifyTitleHard(\"Dashboard - Website ABC\");\n\n" +
                    "// Kiểm tra tiêu đề trang sản phẩm\n" +
                    "webKeyword.click(productLinkObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyTitleHard(\"Điện thoại XYZ - Chi tiết sản phẩm\");\n\n" +
                    "// Kiểm tra tiêu đề trang kết quả tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.verifyTitleHard(\"Kết quả tìm kiếm cho: laptop\");\n\n" +
                    "// Kiểm tra tiêu đề trang sau khi chuyển ngôn ngữ\n" +
                    "webKeyword.click(languageSelectorObject);\n" +
                    "webKeyword.click(englishOptionObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyTitleHard(\"Home - Website ABC\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trang web đã hoàn thành quá trình tải"
            },
            exceptions = {
                    "AssertionError: Nếu tiêu đề trang không khớp với tiêu đề mong đợi",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "title", "page", "header", "browser", "validation", "metadata"}
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
            parameters = {"String: expectedTitle - Tiêu đề trang mong đợi."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra tiêu đề trang giỏ hàng sau khi thêm sản phẩm\n" +
                    "webKeyword.click(addToCartButtonObject);\n" +
                    "webKeyword.click(viewCartButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyTitleSoft(\"Giỏ hàng (1 sản phẩm)\");\n" +
                    "webKeyword.click(checkoutButtonObject); // Tiếp tục quy trình thanh toán\n\n" +
                    "// Kiểm tra nhiều điều kiện trong quy trình đặt hàng\n" +
                    "webKeyword.verifyTitleSoft(\"Thanh toán - Bước 1: Thông tin giao hàng\");\n" +
                    "webKeyword.verifyElementVisibleSoft(shippingFormObject, true);\n" +
                    "webKeyword.fillShippingForm(); // Tiếp tục điền form\n\n" +
                    "// Kiểm tra tiêu đề trang trong quy trình nhiều bước\n" +
                    "webKeyword.click(step1ButtonObject);\n" +
                    "webKeyword.verifyTitleSoft(\"Bước 1: Thông tin cá nhân\");\n" +
                    "webKeyword.click(step2ButtonObject);\n" +
                    "webKeyword.verifyTitleSoft(\"Bước 2: Thông tin liên hệ\");\n" +
                    "webKeyword.click(step3ButtonObject);\n" +
                    "webKeyword.verifyTitleSoft(\"Bước 3: Xác nhận\");\n\n" +
                    "// Kiểm tra tiêu đề trang sau khi lọc sản phẩm\n" +
                    "webKeyword.selectByVisibleText(categoryDropdownObject, \"Điện thoại\");\n" +
                    "webKeyword.click(filterButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.verifyTitleSoft(\"Điện thoại - Danh sách sản phẩm\");\n\n" +
                    "// Kiểm tra tiêu đề trang sau khi tìm kiếm\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop gaming\");\n" +
                    "webKeyword.click(searchButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.verifyTitleSoft(\"Kết quả tìm kiếm: laptop gaming\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trang web đã hoàn thành quá trình tải"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "title", "page", "header", "browser", "validation", "metadata"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra nút gửi form đã được kích hoạt sau khi điền đầy đủ thông tin\n" +
                    "webKeyword.sendKeys(nameInputObject, \"Nguyễn Văn A\");\n" +
                    "webKeyword.sendKeys(emailInputObject, \"nguyenvana@example.com\");\n" +
                    "webKeyword.sendKeys(phoneInputObject, \"0123456789\");\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.assertElementEnabled(submitButtonObject);\n" +
                    "webKeyword.click(submitButtonObject);\n\n" +
                    "// Kiểm tra nút thanh toán đã được kích hoạt sau khi chọn phương thức thanh toán\n" +
                    "webKeyword.click(creditCardOptionObject);\n" +
                    "webKeyword.sendKeys(cardNumberInputObject, \"1234567890123456\");\n" +
                    "webKeyword.sendKeys(cardExpiryInputObject, \"12/25\");\n" +
                    "webKeyword.sendKeys(cardCvvInputObject, \"123\");\n" +
                    "webKeyword.assertElementEnabled(payNowButtonObject);\n\n" +
                    "// Kiểm tra nút tiếp tục đã được kích hoạt sau khi hoàn thành bước hiện tại\n" +
                    "webKeyword.selectByVisibleText(countryDropdownObject, \"Việt Nam\");\n" +
                    "webKeyword.sendKeys(addressInputObject, \"123 Đường ABC\");\n" +
                    "webKeyword.sendKeys(cityInputObject, \"Hồ Chí Minh\");\n" +
                    "webKeyword.assertElementEnabled(nextStepButtonObject);\n\n" +
                    "// Kiểm tra nút tải xuống đã được kích hoạt sau khi chọn tệp\n" +
                    "webKeyword.click(fileOptionObject);\n" +
                    "webKeyword.assertElementEnabled(downloadButtonObject);\n\n" +
                    "// Kiểm tra nút đăng ký đã được kích hoạt sau khi đồng ý với điều khoản\n" +
                    "webKeyword.click(agreeTermsCheckboxObject);\n" +
                    "webKeyword.assertElementEnabled(registerButtonObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM"
            },
            exceptions = {
                    "AssertionError: Nếu phần tử đang ở trạng thái disabled",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "enabled", "disabled", "state", "interactive", "button", "input"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra nút gửi form bị vô hiệu hóa khi chưa điền thông tin bắt buộc\n" +
                    "webKeyword.navigateToUrl(\"https://example.com/registration\");\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.assertElementDisabled(submitButtonBeforeFillForm);\n\n" +
                    "// Kiểm tra nút thanh toán bị vô hiệu hóa khi chưa chọn phương thức thanh toán\n" +
                    "webKeyword.click(checkoutButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(15);\n" +
                    "webKeyword.assertElementDisabled(paymentButtonObject);\n\n" +
                    "// Kiểm tra nút xác nhận bị vô hiệu hóa khi chưa đồng ý điều khoản\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"testuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.assertElementDisabled(confirmButtonObject);\n\n" +
                    "// Kiểm tra trường nhập liệu bị vô hiệu hóa trong chế độ xem\n" +
                    "webKeyword.click(viewModeButtonObject);\n" +
                    "webKeyword.assertElementDisabled(nameFieldInViewMode);\n\n" +
                    "// Kiểm tra nút chỉnh sửa bị vô hiệu hóa cho người dùng không có quyền\n" +
                    "webKeyword.sendKeys(usernameInputObject, \"readonlyuser\");\n" +
                    "webKeyword.sendKeys(passwordInputObject, \"password123\");\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.waitForPageLoaded(10);\n" +
                    "webKeyword.assertElementDisabled(editButtonObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM"
            },
            exceptions = {
                    "AssertionError: Nếu phần tử đang ở trạng thái enabled",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "enabled", "disabled", "state", "interactive", "button", "input"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra nhiều trường nhập liệu tùy chọn có thể tương tác\n" +
                    "webKeyword.verifyElementEnabledSoft(optionalFieldObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(commentFieldObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(ratingFieldObject);\n" +
                    "webKeyword.sendKeys(commentFieldObject, \"Đây là bình luận của tôi\"); // Tiếp tục ngay cả khi có trường không enabled\n\n" +
                    "// Kiểm tra các nút chức năng trong trang quản trị\n" +
                    "webKeyword.verifyElementEnabledSoft(addButtonObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(editButtonObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(deleteButtonObject);\n" +
                    "webKeyword.click(addButtonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra các trường trong form sau khi mở khóa\n" +
                    "webKeyword.click(unlockFormButtonObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(nameFieldObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(emailFieldObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(phoneFieldObject);\n" +
                    "webKeyword.sendKeys(nameFieldObject, \"Nguyễn Văn A\"); // Tiếp tục điền form\n\n" +
                    "// Kiểm tra các tùy chọn trong menu\n" +
                    "webKeyword.click(userMenuObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(profileOptionObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(settingsOptionObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(logoutOptionObject);\n" +
                    "webKeyword.click(profileOptionObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra các nút điều hướng\n" +
                    "webKeyword.verifyElementEnabledSoft(previousButtonObject);\n" +
                    "webKeyword.verifyElementEnabledSoft(nextButtonObject);\n" +
                    "webKeyword.click(nextButtonObject); // Tiếp tục điều hướng",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "enabled", "disabled", "state", "interactive", "button", "input"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra các tính năng bị khóa trong phiên bản dùng thử\n" +
                    "webKeyword.verifyElementDisabledSoft(lockedFeatureButton);\n" +
                    "webKeyword.verifyElementDisabledSoft(premiumFeatureButton);\n" +
                    "webKeyword.verifyElementDisabledSoft(exportDataButton);\n" +
                    "webKeyword.click(upgradeAccountButton); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra các trường không thể chỉnh sửa trong chế độ xem\n" +
                    "webKeyword.click(viewModeButton);\n" +
                    "webKeyword.verifyElementDisabledSoft(nameFieldInViewMode);\n" +
                    "webKeyword.verifyElementDisabledSoft(emailFieldInViewMode);\n" +
                    "webKeyword.verifyElementDisabledSoft(phoneFieldInViewMode);\n" +
                    "webKeyword.click(editModeButton); // Chuyển sang chế độ chỉnh sửa\n\n" +
                    "// Kiểm tra nút tiếp theo bị vô hiệu hóa khi chưa hoàn thành bước hiện tại\n" +
                    "webKeyword.verifyElementDisabledSoft(nextStepButtonObject);\n" +
                    "webKeyword.sendKeys(requiredFieldObject, \"Thông tin bắt buộc\"); // Điền thông tin\n" +
                    "webKeyword.click(nextStepButtonObject); // Tiếp tục quy trình\n\n" +
                    "// Kiểm tra các tùy chọn không khả dụng dựa trên lựa chọn hiện tại\n" +
                    "webKeyword.click(option1RadioButton);\n" +
                    "webKeyword.verifyElementDisabledSoft(subOption2ForOption1);\n" +
                    "webKeyword.verifyElementDisabledSoft(subOption3ForOption1);\n" +
                    "webKeyword.click(subOption1ForOption1); // Chọn tùy chọn phụ khả dụng\n\n" +
                    "// Kiểm tra các nút điều hướng trong phân trang\n" +
                    "webKeyword.verifyElementDisabledSoft(previousPageButtonOnFirstPage);\n" +
                    "webKeyword.click(nextPageButton); // Chuyển đến trang tiếp theo",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "enabled", "disabled", "state", "interactive", "button", "input"}
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
            parameters = {"ObjectUI: uiObject - Phần tử checkbox hoặc radio button cần kiểm tra."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra checkbox \"Ghi nhớ đăng nhập\" đã được chọn\n" +
                    "webKeyword.click(rememberMeCheckbox);\n" +
                    "webKeyword.assertElementSelected(rememberMeCheckbox);\n" +
                    "webKeyword.click(loginButtonObject);\n\n" +
                    "// Kiểm tra radio button phương thức thanh toán đã được chọn\n" +
                    "webKeyword.click(creditCardRadioButton);\n" +
                    "webKeyword.assertElementSelected(creditCardRadioButton);\n" +
                    "webKeyword.sendKeys(cardNumberInputObject, \"1234567890123456\");\n\n" +
                    "// Kiểm tra checkbox điều khoản đã được chọn trước khi tiếp tục\n" +
                    "webKeyword.click(termsCheckboxObject);\n" +
                    "webKeyword.assertElementSelected(termsCheckboxObject);\n" +
                    "webKeyword.click(continueButtonObject);\n\n" +
                    "// Kiểm tra tùy chọn mặc định đã được chọn\n" +
                    "webKeyword.assertElementSelected(defaultShippingOptionRadio);\n" +
                    "webKeyword.click(nextStepButtonObject);\n\n" +
                    "// Kiểm tra checkbox thông báo đã được chọn sau khi đăng ký\n" +
                    "webKeyword.click(newsletterCheckboxObject);\n" +
                    "webKeyword.assertElementSelected(newsletterCheckboxObject);\n" +
                    "webKeyword.click(completeRegistrationButton);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM và là checkbox hoặc radio button"
            },
            exceptions = {
                    "AssertionError: Nếu phần tử không ở trạng thái được chọn",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "IllegalArgumentException: Nếu phần tử không phải là checkbox hoặc radio button"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "selected", "checked", "checkbox", "radio", "state", "input"}
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
            parameters = {"ObjectUI: uiObject - Phần tử checkbox hoặc radio button cần kiểm tra."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra checkbox thông báo chưa được chọn mặc định\n" +
                    "webKeyword.assertElementNotSelected(newsletterCheckbox);\n" +
                    "webKeyword.click(newsletterCheckbox); // Chọn để nhận thông báo\n\n" +
                    "// Kiểm tra các tùy chọn bổ sung chưa được chọn\n" +
                    "webKeyword.assertElementNotSelected(expressShippingRadio);\n" +
                    "webKeyword.assertElementNotSelected(giftWrappingCheckbox);\n" +
                    "webKeyword.click(expressShippingRadio); // Chọn vận chuyển nhanh\n\n" +
                    "// Kiểm tra tùy chọn cao cấp chưa được chọn trước khi hiển thị thông tin chi tiết\n" +
                    "webKeyword.assertElementNotSelected(premiumFeaturesCheckbox);\n" +
                    "webKeyword.click(premiumFeaturesCheckbox);\n" +
                    "webKeyword.waitForElementVisible(premiumFeaturesDetailsPanel);\n\n" +
                    "// Kiểm tra radio button phương thức thanh toán chưa được chọn\n" +
                    "webKeyword.assertElementNotSelected(paypalRadioButton);\n" +
                    "webKeyword.click(paypalRadioButton);\n" +
                    "webKeyword.waitForElementVisible(paypalLoginSection);\n\n" +
                    "// Kiểm tra checkbox lưu thông tin thẻ chưa được chọn mặc định\n" +
                    "webKeyword.assertElementNotSelected(saveCardInfoCheckbox);\n" +
                    "webKeyword.click(saveCardInfoCheckbox); // Chọn để lưu thông tin thẻ",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM và là checkbox hoặc radio button"
            },
            exceptions = {
                    "AssertionError: Nếu phần tử đang ở trạng thái được chọn",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "IllegalArgumentException: Nếu phần tử không phải là checkbox hoặc radio button"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "selected", "checked", "checkbox", "radio", "state", "input", "unchecked"}
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
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: pattern - Biểu thức chính quy để so khớp."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra mã đơn hàng có đúng định dạng\n" +
                    "webKeyword.click(viewOrderButtonObject);\n" +
                    "webKeyword.waitForElementVisible(orderIdObject);\n" +
                    "webKeyword.verifyTextMatchesRegexHard(orderIdObject, \"^DH-\\d{5}$\"); // Khớp với DH-12345\n\n" +
                    "// Kiểm tra số điện thoại có đúng định dạng\n" +
                    "webKeyword.verifyTextMatchesRegexHard(phoneNumberObject, \"^(\\\\+84|0)[0-9]{9,10}$\");\n\n" +
                    "// Kiểm tra mã sản phẩm có đúng định dạng\n" +
                    "webKeyword.click(productDetailsButtonObject);\n" +
                    "webKeyword.waitForElementVisible(productCodeObject);\n" +
                    "webKeyword.verifyTextMatchesRegexHard(productCodeObject, \"^SP-[A-Z]{2}-\\d{4}$\"); // Khớp với SP-DT-1234\n\n" +
                    "// Kiểm tra định dạng ngày tháng\n" +
                    "webKeyword.verifyTextMatchesRegexHard(orderDateObject, \"^\\d{2}/\\d{2}/\\d{4}$\"); // Khớp với 01/01/2023\n\n" +
                    "// Kiểm tra định dạng giá tiền\n" +
                    "webKeyword.verifyTextMatchesRegexHard(priceObject, \"^\\d{1,3}(,\\d{3})*(\\.\\d{1,2})?₫$\"); // Khớp với 1,000,000₫",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản"
            },
            exceptions = {
                    "AssertionError: Nếu văn bản không khớp với biểu thức chính quy",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "PatternSyntaxException: Nếu biểu thức chính quy không hợp lệ"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "text", "regex", "pattern", "format", "validation", "regular expression"}
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
                    "ObjectUI: uiObject - Phần tử chứa văn bản cần kiểm tra.",
                    "String: pattern - Biểu thức chính quy để so khớp."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra định dạng email hiển thị trên trang\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(emailFormatObject, \"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$\");\n" +
                    "webKeyword.click(continueButtonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra nhiều định dạng khác nhau trên trang thông tin\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(zipCodeObject, \"^\\d{5}(-\\d{4})?$\"); // Mã bưu điện\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(taxIdObject, \"^\\d{10}$\"); // Mã số thuế\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(websiteObject, \"^https?://[\\\\w.-]+\\\\.[a-zA-Z]{2,}(/.*)?$\"); // URL\n" +
                    "webKeyword.click(saveButtonObject); // Tiếp tục lưu thông tin\n\n" +
                    "// Kiểm tra định dạng thông tin sản phẩm\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(productWeightObject, \"^\\d+(\\.\\d+)? (kg|g)$\"); // Cân nặng\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(productDimensionsObject, \"^\\d+x\\d+x\\d+ (cm|mm)$\"); // Kích thước\n" +
                    "webKeyword.click(addToCartButtonObject); // Tiếp tục thêm vào giỏ hàng\n\n" +
                    "// Kiểm tra định dạng thông tin tài khoản\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(usernameObject, \"^[a-zA-Z0-9_]{3,20}$\"); // Tên người dùng\n" +
                    "webKeyword.verifyTextMatchesRegexSoft(memberSinceObject, \"^Thành viên từ: \\d{2}/\\d{2}/\\d{4}$\"); // Ngày tham gia\n" +
                    "webKeyword.click(editProfileButtonObject); // Tiếp tục chỉnh sửa hồ sơ",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM và có văn bản"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "PatternSyntaxException: Nếu biểu thức chính quy không hợp lệ"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "text", "regex", "pattern", "format", "validation", "regular expression"}
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
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: attribute - Tên của thuộc tính (ví dụ: 'class').",
                    "String: partialValue - Chuỗi con mong đợi."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra phần tử có class 'active'\n" +
                    "webKeyword.click(tabButtonObject);\n" +
                    "webKeyword.verifyAttributeContainsHard(tabButtonObject, \"class\", \"active\");\n\n" +
                    "// Kiểm tra đường dẫn hình ảnh chứa tên sản phẩm\n" +
                    "webKeyword.verifyAttributeContainsHard(productImageObject, \"src\", \"iphone-13\");\n\n" +
                    "// Kiểm tra thuộc tính data-id chứa mã sản phẩm\n" +
                    "webKeyword.verifyAttributeContainsHard(productCardObject, \"data-id\", \"SP-1234\");\n\n" +
                    "// Kiểm tra liên kết chứa thông tin danh mục\n" +
                    "webKeyword.verifyAttributeContainsHard(categoryLinkObject, \"href\", \"category=electronics\");\n\n" +
                    "// Kiểm tra thuộc tính aria-label chứa thông tin trợ năng\n" +
                    "webKeyword.verifyAttributeContainsHard(closeButtonObject, \"aria-label\", \"Đóng\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM",
                    "Thuộc tính cần kiểm tra phải tồn tại trên phần tử"
            },
            exceptions = {
                    "AssertionError: Nếu giá trị thuộc tính không chứa chuỗi con mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "attribute", "property", "contains", "substring", "partial", "validation"}
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
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: attribute - Tên của thuộc tính (ví dụ: 'class').",
                    "String: partialValue - Chuỗi con mong đợi."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra thuộc tính style có chứa thông tin hiển thị\n" +
                    "webKeyword.verifyAttributeContainsSoft(elementObject, \"style\", \"display: block\");\n" +
                    "webKeyword.click(elementObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra nhiều thuộc tính của một phần tử\n" +
                    "webKeyword.verifyAttributeContainsSoft(buttonObject, \"class\", \"btn\");\n" +
                    "webKeyword.verifyAttributeContainsSoft(buttonObject, \"data-action\", \"submit\");\n" +
                    "webKeyword.verifyAttributeContainsSoft(buttonObject, \"id\", \"save\");\n" +
                    "webKeyword.click(buttonObject); // Tiếp tục thực hiện hành động\n\n" +
                    "// Kiểm tra thuộc tính của nhiều phần tử\n" +
                    "webKeyword.verifyAttributeContainsSoft(menuItemObject1, \"class\", \"menu-item\");\n" +
                    "webKeyword.verifyAttributeContainsSoft(menuItemObject2, \"class\", \"menu-item\");\n" +
                    "webKeyword.verifyAttributeContainsSoft(activeMenuItemObject, \"class\", \"active\");\n" +
                    "webKeyword.click(menuItemObject1); // Tiếp tục điều hướng\n\n" +
                    "// Kiểm tra thuộc tính placeholder có chứa hướng dẫn\n" +
                    "webKeyword.verifyAttributeContainsSoft(searchInputObject, \"placeholder\", \"Tìm kiếm\");\n" +
                    "webKeyword.sendKeys(searchInputObject, \"laptop\"); // Tiếp tục tìm kiếm\n\n" +
                    "// Kiểm tra thuộc tính alt của hình ảnh có chứa mô tả\n" +
                    "webKeyword.verifyAttributeContainsSoft(productImageObject, \"alt\", \"Điện thoại\");\n" +
                    "webKeyword.click(productImageObject); // Tiếp tục xem chi tiết sản phẩm",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM",
                    "Thuộc tính cần kiểm tra phải tồn tại trên phần tử"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "attribute", "property", "contains", "substring", "partial", "validation"}
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
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: cssName - Tên thuộc tính CSS (ví dụ: 'color').",
                    "String: expectedValue - Giá trị CSS mong đợi (ví dụ: 'rgb(255, 0, 0)')."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra màu của thông báo lỗi\n" +
                    "webKeyword.click(submitButtonObject); // Gửi form không hợp lệ\n" +
                    "webKeyword.waitForElementVisible(errorMessageObject);\n" +
                    "webKeyword.verifyCssValueHard(errorMessageObject, \"color\", \"rgba(255, 0, 0, 1)\");\n\n" +
                    "// Kiểm tra font-size của tiêu đề\n" +
                    "webKeyword.verifyCssValueHard(pageTitleObject, \"font-size\", \"24px\");\n\n" +
                    "// Kiểm tra background-color của nút đã chọn\n" +
                    "webKeyword.click(selectButtonObject);\n" +
                    "webKeyword.verifyCssValueHard(selectButtonObject, \"background-color\", \"rgba(0, 123, 255, 1)\");\n\n" +
                    "// Kiểm tra border của trường nhập liệu không hợp lệ\n" +
                    "webKeyword.sendKeys(emailInputObject, \"invalid-email\");\n" +
                    "webKeyword.click(outsideFormObject); // Click ra ngoài để kích hoạt validation\n" +
                    "webKeyword.verifyCssValueHard(emailInputObject, \"border-color\", \"rgba(220, 53, 69, 1)\");\n\n" +
                    "// Kiểm tra display của phần tử sau khi hiển thị\n" +
                    "webKeyword.click(showDetailsButtonObject);\n" +
                    "webKeyword.verifyCssValueHard(detailsContainerObject, \"display\", \"block\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM"
            },
            exceptions = {
                    "AssertionError: Nếu giá trị CSS không khớp với giá trị mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "css", "style", "property", "visual", "appearance", "validation"}
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
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "String: cssName - Tên thuộc tính CSS (ví dụ: 'font-weight').",
                    "String: expectedValue - Giá trị CSS mong đợi (ví dụ: '700')."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra độ đậm của tiêu đề\n" +
                    "webKeyword.verifyCssValueSoft(titleObject, \"font-weight\", \"700\");\n" +
                    "webKeyword.click(nextButtonObject); // Tiếp tục quy trình\n\n" +
                    "// Kiểm tra màu nền của nút\n" +
                    "webKeyword.verifyCssValueSoft(buttonObject, \"background-color\", \"rgb(0, 123, 255)\");\n" +
                    "webKeyword.click(buttonObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải tồn tại trong DOM"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "soft", "css", "style", "property", "visual", "appearance", "validation"}
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
                    "ObjectUI: uiObject - Phần tử cần kiểm tra.",
                    "int: timeoutInSeconds - Thời gian chờ tối đa."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra phần tử đã bị xóa\n" +
                    "webKeyword.click(deleteButtonObject);\n" +
                    "webKeyword.verifyElementNotPresentHard(deletedItemObject, 5);\n\n" +
                    "// Kiểm tra thông báo lỗi đã biến mất\n" +
                    "webKeyword.sendKeys(emailInput, \"valid@example.com\");\n" +
                    "webKeyword.verifyElementNotPresentHard(errorMessageObject, 3);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động"
            },
            exceptions = {
                    "AssertionError: Nếu phần tử vẫn tồn tại trong DOM sau thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "not present", "absence", "removed", "deleted", "invisible", "dom"}
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
                    "ObjectUI: uiObject - Phần tử dropdown (thẻ select).",
                    "String: expectedLabel - Văn bản hiển thị của tùy chọn mong đợi."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra quốc gia đã chọn\n" +
                    "webKeyword.selectByVisibleText(countryDropdown, \"Việt Nam\");\n" +
                    "webKeyword.verifyOptionSelectedByLabelHard(countryDropdown, \"Việt Nam\");\n\n" +
                    "// Kiểm tra danh mục đã chọn\n" +
                    "webKeyword.verifyOptionSelectedByLabelHard(categoryDropdown, \"Điện thoại\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần kiểm tra phải là thẻ select và tồn tại trong DOM"
            },
            exceptions = {
                    "AssertionError: Nếu tùy chọn được chọn không khớp với tùy chọn mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "UnexpectedTagNameException: Nếu phần tử không phải là thẻ select"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "hard", "dropdown", "select", "option", "combobox", "selected", "value"}
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
                    "ObjectUI: uiObject - Phần tử cần tìm kiếm.",
                    "int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."
            },
            returnValue = "boolean: true nếu phần tử tồn tại, false nếu không tồn tại",
            example = "// Kiểm tra thông báo lỗi có xuất hiện không\n" +
                    "boolean isErrorVisible = webKeyword.isElementPresent(errorMessageObject, 5);\n" +
                    "if (isErrorVisible) {\n" +
                    "    // Xử lý khi có lỗi\n" +
                    "}\n\n" +
                    "// Kiểm tra phần tử tùy chọn có tồn tại không\n" +
                    "if (webKeyword.isElementPresent(optionalElementObject, 2)) {\n" +
                    "    webKeyword.click(optionalElementObject);\n" +
                    "}",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "presence", "exists", "dom", "element", "check", "condition", "wait"}
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
            parameters = {"int: timeoutInSeconds - Thời gian chờ tối đa."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra alert xuất hiện sau khi xóa\n" +
                    "webKeyword.click(deleteButtonObject);\n" +
                    "webKeyword.verifyAlertPresent(5);\n" +
                    "webKeyword.acceptAlert(); // Xác nhận xóa\n\n" +
                    "// Kiểm tra alert xuất hiện khi rời trang có dữ liệu chưa lưu\n" +
                    "webKeyword.sendKeys(commentField, \"Bình luận mới\");\n" +
                    "webKeyword.click(backButtonObject);\n" +
                    "webKeyword.verifyAlertPresent(3);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trang web có thể hiển thị hộp thoại alert"
            },
            exceptions = {
                    "AssertionError: Nếu không có hộp thoại alert xuất hiện trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "assert", "verify", "alert", "popup", "dialog", "notification", "javascript", "confirmation"}
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
            parameters = {"String: windowTitle - Tiêu đề chính xác của cửa sổ hoặc tab cần chuyển đến."},
            returnValue = "void: Không trả về giá trị",
            example = "// Chuyển sang tab chi tiết sản phẩm\n" +
                    "webKeyword.click(viewDetailsLinkObject); // Mở tab mới\n" +
                    "webKeyword.switchToWindowByTitle(\"Chi tiết sản phẩm ABC\");\n" +
                    "webKeyword.waitForElementVisible(productSpecificationsObject);\n\n" +
                    "// Chuyển sang tab thanh toán\n" +
                    "webKeyword.click(checkoutButtonObject); // Mở tab thanh toán\n" +
                    "webKeyword.switchToWindowByTitle(\"Thanh toán đơn hàng\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Có ít nhất một cửa sổ/tab đang mở với tiêu đề cần chuyển đến"
            },
            exceptions = {
                    "NoSuchWindowException: Nếu không tìm thấy cửa sổ nào có tiêu đề được chỉ định",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "NAVIGATION",
            stability = "STABLE",
            tags = {"web", "window", "tab", "navigation", "switch", "title", "browser", "context", "handle"}
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
            parameters = {"int: index - Chỉ số của cửa sổ/tab cần chuyển đến (0 là cửa sổ đầu tiên)."},
            returnValue = "void: Không trả về giá trị",
            example = "// Mở liên kết trong tab mới và chuyển sang tab đó\n" +
                    "webKeyword.rightClickAndSelect(productLinkObject, \"Mở trong tab mới\");\n" +
                    "webKeyword.switchToWindowByIndex(1); // Chuyển sang tab thứ hai\n\n" +
                    "// Quay lại tab chính sau khi hoàn thành\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "webKeyword.switchToWindowByIndex(0);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Có đủ số lượng cửa sổ/tab đang mở để chuyển đến chỉ số được chỉ định"
            },
            exceptions = {
                    "IndexOutOfBoundsException: Nếu chỉ số nằm ngoài phạm vi của số lượng cửa sổ/tab đang mở",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "NAVIGATION",
            stability = "STABLE",
            tags = {"web", "window", "tab", "navigation", "switch", "index", "browser", "context", "handle"}
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
            parameters = {"ObjectUI: uiObject - Đối tượng giao diện đại diện cho thẻ iframe cần chuyển vào."},
            returnValue = "void: Không trả về giá trị",
            example = "// Chuyển vào iframe thanh toán\n" +
                    "webKeyword.waitForElementVisible(paymentIframeObject);\n" +
                    "webKeyword.switchToFrame(paymentIframeObject);\n" +
                    "webKeyword.sendKeys(cardNumberObject, \"4111111111111111\");\n\n" +
                    "// Chuyển vào iframe trình soạn thảo\n" +
                    "webKeyword.switchToFrame(richTextEditorObject);\n" +
                    "webKeyword.sendKeys(editorBodyObject, \"Nội dung bài viết\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử iframe cần chuyển vào phải tồn tại trong DOM"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử iframe",
                    "StaleElementReferenceException: Nếu phần tử iframe không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "NAVIGATION",
            stability = "STABLE",
            tags = {"web", "iframe", "frame", "navigation", "switch", "context", "embedded", "content"}
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
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Thoát khỏi iframe con và quay về iframe cha\n" +
                    "webKeyword.switchToFrame(mainIframeObject);\n" +
                    "webKeyword.switchToFrame(nestedIframeObject);\n" +
                    "webKeyword.click(submitButtonObject);\n" +
                    "webKeyword.switchToParentFrame(); // Quay về iframe cha\n" +
                    "webKeyword.click(nextButtonObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "WebDriver đang ở trong ngữ cảnh của một iframe"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "NAVIGATION",
            stability = "STABLE",
            tags = {"web", "iframe", "frame", "navigation", "switch", "context", "parent", "nested"}
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
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Thoát khỏi tất cả các iframe và quay về nội dung chính\n" +
                    "webKeyword.switchToFrame(paymentIframeObject);\n" +
                    "webKeyword.sendKeys(cardNumberObject, \"4111111111111111\");\n" +
                    "webKeyword.click(submitPaymentObject);\n" +
                    "webKeyword.switchToDefaultContent(); // Quay về nội dung chính\n" +
                    "webKeyword.waitForElementVisible(confirmationMessageObject);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "NAVIGATION",
            stability = "STABLE",
            tags = {"web", "iframe", "frame", "navigation", "switch", "context", "main", "root", "default"}
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
            parameters = {"String: url - (Tùy chọn) URL để mở trong tab mới. Nếu để trống, sẽ mở tab trống."},
            returnValue = "void: Không trả về giá trị",
            example = "// Mở tab mới với URL cụ thể\n" +
                    "webKeyword.openNewTab(\"https://google.com\");\n" +
                    "webKeyword.waitForElementVisible(searchBoxObject);\n\n" +
                    "// Mở tab trống và điều hướng sau đó\n" +
                    "webKeyword.openNewTab(\"\");\n" +
                    "webKeyword.navigate(\"https://example.com\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trình duyệt hỗ trợ việc mở tab mới thông qua WebDriver"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt",
                    "UnsupportedCommandException: Nếu trình duyệt không hỗ trợ lệnh mở tab mới"
            },
            platform = "WEB",
            systemImpact = "NAVIGATION",
            stability = "STABLE",
            tags = {"web", "window", "tab", "navigation", "open", "new", "browser", "url"}
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
            parameters = {"ObjectUI: uiObject - Phần tử link cần click."},
            returnValue = "void: Không trả về giá trị",
            example = "// Click vào liên kết mở trong tab mới\n" +
                    "webKeyword.clickAndSwitchToNewTab(externalLinkObject);\n" +
                    "webKeyword.waitForPageLoaded();\n\n" +
                    "// Click vào nút xem chi tiết sản phẩm (mở tab mới)\n" +
                    "webKeyword.clickAndSwitchToNewTab(viewDetailsButtonObject);\n" +
                    "webKeyword.verifyElementVisibleHard(productSpecificationsObject, 10);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần click phải tồn tại trong DOM và có khả năng mở tab mới (ví dụ: có thuộc tính target='_blank')"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "TimeoutException: Nếu tab mới không mở trong thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "NAVIGATION",
            stability = "STABLE",
            tags = {"web", "window", "tab", "navigation", "click", "switch", "browser", "link"}
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
            parameters = {"Không có tham số."},
            returnValue = "String: Nội dung văn bản của hộp thoại alert",
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
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Một hộp thoại alert đang hiển thị hoặc sẽ xuất hiện"
            },
            exceptions = {
                    "TimeoutException: Nếu không có hộp thoại alert xuất hiện trong thời gian chờ",
                    "NoAlertPresentException: Nếu không có hộp thoại alert đang hiển thị",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "alert", "popup", "dialog", "text", "message", "javascript", "notification", "get"}
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
            parameters = {"String: text - Chuỗi văn bản cần nhập vào hộp thoại."},
            returnValue = "void: Không trả về giá trị",
            example = "// Nhập tên người dùng vào hộp thoại prompt\n" +
                    "webKeyword.click(loginButtonObject);\n" +
                    "webKeyword.sendKeysToAlert(\"Nguyễn Văn A\");\n" +
                    "webKeyword.acceptAlert();\n\n" +
                    "// Nhập lý do hủy đơn hàng\n" +
                    "webKeyword.click(cancelOrderButtonObject);\n" +
                    "webKeyword.sendKeysToAlert(\"Thay đổi thông tin giao hàng\");\n" +
                    "webKeyword.acceptAlert();",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Một hộp thoại prompt đang hiển thị hoặc sẽ xuất hiện"
            },
            exceptions = {
                    "TimeoutException: Nếu không có hộp thoại alert xuất hiện trong thời gian chờ",
                    "NoAlertPresentException: Nếu không có hộp thoại alert đang hiển thị",
                    "ElementNotInteractableException: Nếu hộp thoại không phải là prompt và không cho phép nhập liệu",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "alert", "prompt", "dialog", "input", "text", "javascript", "interaction", "sendkeys"}
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
                    "ObjectUI: shadowHostObject - Phần tử chủ (host) chứa Shadow DOM.",
                    "String: cssSelectorInShadow - Chuỗi CSS selector để tìm phần tử bên trong Shadow DOM."
            },
            returnValue = "WebElement: Phần tử web được tìm thấy bên trong Shadow DOM",
            example = "// Tìm và tương tác với phần tử input trong Shadow DOM\n" +
                    "WebElement usernameInput = webKeyword.findElementInShadowDom(appContainerObject, \"#username\");\n" +
                    "usernameInput.sendKeys(\"admin@example.com\");\n\n" +
                    "// Tìm và click vào nút trong Shadow DOM\n" +
                    "WebElement submitButton = webKeyword.findElementInShadowDom(loginFormObject, \".submit-button\");\n" +
                    "submitButton.click();",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trình duyệt hỗ trợ Shadow DOM (Chrome, Firefox mới)",
                    "Phần tử chủ (host) phải tồn tại và có Shadow DOM đính kèm"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử chủ hoặc phần tử con",
                    "StaleElementReferenceException: Nếu phần tử chủ không còn gắn với DOM",
                    "UnsupportedOperationException: Nếu trình duyệt không hỗ trợ Shadow DOM API",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "shadow-dom", "component", "encapsulation", "css", "selector", "find", "element", "web-components"}
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
                    "String: key - Khóa (key) để lưu trữ.",
                    "String: value - Giá trị (value) tương ứng."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Thiết lập token xác thực người dùng\n" +
                    "webKeyword.setLocalStorage(\"user_token\", \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\");\n" +
                    "webKeyword.navigate(\"https://example.com/dashboard\");\n\n" +
                    "// Lưu trạng thái giỏ hàng\n" +
                    "webKeyword.setLocalStorage(\"cart_items\", \"[{\\\"id\\\":123,\\\"quantity\\\":2}]\");\n" +
                    "webKeyword.refreshPage();",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trang web đã được tải hoàn toàn",
                    "Trình duyệt hỗ trợ Local Storage"
            },
            exceptions = {
                    "JavascriptException: Nếu có lỗi khi thực thi JavaScript",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "storage", "local-storage", "state", "data", "persistence", "browser", "javascript", "token"}
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
            parameters = {"String: key - Khóa (key) của giá trị cần đọc."},
            returnValue = "String: Giá trị được lưu trữ trong Local Storage với khóa đã chỉ định, hoặc null nếu không tìm thấy",
            example = "// Kiểm tra token xác thực\n" +
                    "String userToken = webKeyword.getLocalStorage(\"user_token\");\n" +
                    "if (userToken == null || userToken.isEmpty()) {\n" +
                    "    webKeyword.navigate(\"https://example.com/login\");\n" +
                    "}\n\n" +
                    "// Đọc thông tin giỏ hàng\n" +
                    "String cartItems = webKeyword.getLocalStorage(\"cart_items\");\n" +
                    "logger.info(\"Số lượng sản phẩm trong giỏ: \" + cartItems);",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trang web đã được tải hoàn toàn",
                    "Trình duyệt hỗ trợ Local Storage"
            },
            exceptions = {
                    "JavascriptException: Nếu có lỗi khi thực thi JavaScript",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "storage", "local-storage", "state", "data", "persistence", "browser", "javascript", "get"}
    )
    @Step("Đọc từ Local Storage với key='{0}'")
    public String getLocalStorage(String key) {
        return execute(() -> (String) ((JavascriptExecutor) DriverManager.getDriver()).executeScript("return localStorage.getItem(arguments[0]);", key), key);
    }

    @NetatKeyword(
            name = "clearLocalStorage",
            description = "Xóa toàn bộ dữ liệu đang được lưu trữ trong Local Storage của trang web hiện tại.",
            category = "Web/Storage",
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Đăng xuất và xóa dữ liệu người dùng\n" +
                    "webKeyword.click(logoutButtonObject);\n" +
                    "webKeyword.clearLocalStorage();\n" +
                    "webKeyword.navigate(\"https://example.com/login\");\n\n" +
                    "// Xóa dữ liệu trước khi chạy kiểm thử\n" +
                    "webKeyword.navigate(\"https://example.com\");\n" +
                    "webKeyword.clearLocalStorage();\n" +
                    "webKeyword.refreshPage();",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trang web đã được tải hoàn toàn",
                    "Trình duyệt hỗ trợ Local Storage"
            },
            exceptions = {
                    "JavascriptException: Nếu có lỗi khi thực thi JavaScript",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "storage", "local-storage", "clear", "reset", "cleanup", "browser", "javascript", "delete"}
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
            parameters = {"Không có tham số."},
            returnValue = "void: Không trả về giá trị",
            example = "// Đăng xuất và xóa cookies\n" +
                    "webKeyword.click(logoutButtonObject);\n" +
                    "webKeyword.deleteAllCookies();\n" +
                    "webKeyword.navigate(\"https://example.com/login\");\n\n" +
                    "// Thiết lập lại trạng thái trình duyệt\n" +
                    "webKeyword.deleteAllCookies();\n" +
                    "webKeyword.clearLocalStorage();\n" +
                    "webKeyword.navigate(\"https://example.com\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "cookie", "session", "clear", "reset", "cleanup", "browser", "authentication", "delete"}
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
            parameters = {"String: cookieName - Tên của cookie cần lấy."},
            returnValue = "Cookie: Đối tượng Cookie chứa thông tin của cookie được yêu cầu, hoặc null nếu không tìm thấy",
            example = "// Kiểm tra cookie phiên làm việc\n" +
                    "Cookie sessionCookie = webKeyword.getCookie(\"session_id\");\n" +
                    "if (sessionCookie == null) {\n" +
                    "    logger.info(\"Người dùng chưa đăng nhập\");\n" +
                    "    webKeyword.navigate(\"https://example.com/login\");\n" +
                    "} else {\n" +
                    "    logger.info(\"Phiên làm việc: \" + sessionCookie.getValue());\n" +
                    "}\n\n" +
                    "// Kiểm tra thời hạn cookie\n" +
                    "Cookie authCookie = webKeyword.getCookie(\"auth_token\");\n" +
                    "logger.info(\"Cookie hết hạn vào: \" + authCookie.getExpiry());",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Trang web đã được tải hoàn toàn"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "cookie", "session", "get", "browser", "authentication", "storage", "http"}
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
            parameters = {"String: filePath - Đường dẫn đầy đủ để lưu file ảnh (ví dụ: 'C:/screenshots/error.png')."},
            returnValue = "void: Không trả về giá trị",
            example = "// Chụp ảnh màn hình khi gặp lỗi\n" +
                    "try {\n" +
                    "    webKeyword.click(submitButtonObject);\n" +
                    "    webKeyword.verifyElementVisibleHard(confirmationMessageObject, 5);\n" +
                    "} catch (Exception e) {\n" +
                    "    webKeyword.takeScreenshot(\"D:/test-reports/screenshots/submit_error_\" + System.currentTimeMillis() + \".png\");\n" +
                    "    throw e;\n" +
                    "}\n\n" +
                    "// Chụp ảnh màn hình để lưu trữ trạng thái\n" +
                    "webKeyword.waitForPageLoaded();\n" +
                    "webKeyword.takeScreenshot(\"D:/test-reports/screenshots/homepage.png\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Thư mục đích phải tồn tại hoặc có quyền tạo thư mục"
            },
            exceptions = {
                    "RuntimeException: Nếu không thể chụp hoặc lưu ảnh màn hình",
                    "IOException: Nếu có lỗi khi ghi file",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "screenshot", "debug", "evidence", "report", "image", "capture", "utility", "visual"}
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
                    "ObjectUI: uiObject - Phần tử cần chụp ảnh.",
                    "String: filePath - Đường dẫn đầy đủ để lưu file ảnh."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chụp ảnh phần tử để kiểm tra hiển thị\n" +
                    "webKeyword.waitForElementVisible(loginFormObject, 10);\n" +
                    "webKeyword.takeElementScreenshot(loginFormObject, \"D:/screenshots/login_form.png\");\n\n" +
                    "// Chụp ảnh phần tử khi gặp lỗi hiển thị\n" +
                    "if (!webKeyword.verifyElementTextContains(errorMessageObject, \"Invalid credentials\")) {\n" +
                    "    webKeyword.takeElementScreenshot(errorMessageObject, \"D:/screenshots/unexpected_error_\" + System.currentTimeMillis() + \".png\");\n" +
                    "}",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần chụp phải hiển thị trên màn hình",
                    "Thư mục đích phải tồn tại hoặc có quyền tạo thư mục"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "ElementNotVisibleException: Nếu phần tử không hiển thị",
                    "RuntimeException: Nếu không thể chụp hoặc lưu ảnh phần tử",
                    "IOException: Nếu có lỗi khi ghi file",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"web", "screenshot", "element", "debug", "evidence", "report", "image", "capture", "utility", "visual"}
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
            parameters = {"ObjectUI: uiObject - Phần tử cần làm nổi bật."},
            returnValue = "void: Không trả về giá trị",
            example = "// Làm nổi bật các phần tử trong quá trình điền form\n" +
                    "webKeyword.highlightElement(usernameFieldObject);\n" +
                    "webKeyword.sendKeys(usernameFieldObject, \"admin@example.com\");\n" +
                    "webKeyword.highlightElement(passwordFieldObject);\n" +
                    "webKeyword.sendKeys(passwordFieldObject, \"password123\");\n" +
                    "webKeyword.highlightElement(loginButtonObject);\n" +
                    "webKeyword.click(loginButtonObject);\n\n" +
                    "// Làm nổi bật phần tử để gỡ lỗi\n" +
                    "webKeyword.waitForElementVisible(tableRowObject, 10);\n" +
                    "webKeyword.highlightElement(tableRowObject);\n" +
                    "webKeyword.takeElementScreenshot(tableRowObject, \"D:/screenshots/table_row.png\");",
            prerequisites = {
                    "WebDriver đã được khởi tạo và đang hoạt động",
                    "Phần tử cần làm nổi bật phải tồn tại trong DOM"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình duyệt"
            },
            platform = "WEB",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"web", "highlight", "debug", "visual", "element", "utility", "javascript", "style", "border"}
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
            parameters = {"int: milliseconds - Thời gian cần tạm dừng (tính bằng mili giây)."},
            returnValue = "void: Không trả về giá trị",
            example = "// Tạm dừng để đợi animation hoàn thành\n" +
                    "webKeyword.click(expandMenuButtonObject);\n" +
                    "webKeyword.pause(1000); // Đợi 1 giây cho animation menu mở ra\n" +
                    "webKeyword.click(menuItemObject);\n\n" +
                    "// Tạm dừng để đợi dữ liệu được xử lý\n" +
                    "webKeyword.click(generateReportButtonObject);\n" +
                    "webKeyword.pause(3000); // Đợi 3 giây cho quá trình xử lý\n" +
                    "webKeyword.verifyElementVisibleHard(reportResultObject, 10);",
            prerequisites = {
                    "Không có điều kiện tiên quyết đặc biệt"
            },
            exceptions = {
                    "InterruptedException: Nếu luồng thực thi bị gián đoạn trong khi tạm dừng"
            },
            platform = "WEB",
            systemImpact = "NONE",
            stability = "STABLE",
            tags = {"web", "pause", "wait", "delay", "sleep", "timing", "utility", "synchronization"}
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