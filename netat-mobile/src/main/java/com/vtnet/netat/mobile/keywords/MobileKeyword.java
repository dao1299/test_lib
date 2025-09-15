package com.vtnet.netat.mobile.keywords;

import com.vtnet.netat.core.BaseUiKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.ui.ObjectUI;
import com.vtnet.netat.core.utils.ScreenshotUtils;
import com.vtnet.netat.driver.DriverManager;
import io.appium.java_client.*;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.clipboard.HasClipboard;
import io.appium.java_client.ios.IOSDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;


/**
 * Cung cấp bộ keyword nền tảng để tương tác và kiểm thử các ứng dụng di động.
 */
public class MobileKeyword extends BaseUiKeyword {

    private AppiumDriver getAppiumDriver() {
        return (AppiumDriver) DriverManager.getDriver();
    }

    private AndroidDriver getAndroidDriver() {
        WebDriver driver = DriverManager.getDriver();
        if (driver instanceof AndroidDriver) {
            return (AndroidDriver) driver;
        }
        throw new UnsupportedOperationException("Current driver is not an AndroidDriver");
    }

    private IOSDriver getIOSDriver() {
        WebDriver driver = DriverManager.getDriver();
        if (driver instanceof IOSDriver) {
            return (IOSDriver) driver;
        }
        throw new UnsupportedOperationException("Current driver is not an IOSDriver");
    }

    // =================================================================================
    // --- 1. QUẢN LÝ VÒNG ĐỜI ỨNG DỤNG ---
    // =================================================================================

    @NetatKeyword(
            name = "installApp",
            description = "Cài đặt ứng dụng từ một đường dẫn file .apk (Android) hoặc .ipa (iOS) vào thiết bị đang kết nối. Lưu ý: Đường dẫn phải trỏ đến một file hợp lệ và có thể truy cập từ máy thực thi test. Trên iOS, file .ipa phải được ký đúng cách để có thể cài đặt. Trên Android, thiết bị phải cho phép cài đặt từ nguồn không xác định.",
            category = "Mobile/AppLifecycle",
            parameters = {"String: appPath - Đường dẫn tuyệt đối đến file ứng dụng (.apk hoặc .ipa)."},
            returnValue = "void: Không trả về giá trị",
            example = "// Cài đặt ứng dụng Android từ thư mục local\n" +
                    "mobileKeyword.installApp(\"C:/apps/my-app.apk\");\n\n" +
                    "// Cài đặt ứng dụng iOS từ đường dẫn mạng (cần tải về trước)\n" +
                    "mobileKeyword.installApp(\"/tmp/downloaded-app.ipa\");\n\n" +
                    "// Cài đặt phiên bản mới của ứng dụng để kiểm tra tính năng cập nhật\n" +
                    "mobileKeyword.installApp(\"C:/builds/app-v2.0.apk\");",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Có quyền cài đặt ứng dụng trên thiết bị",
                    "Trên Android: Đã bật 'Cài đặt từ nguồn không xác định'",
                    "Trên iOS: File .ipa đã được ký đúng cách"},
            exceptions = {"WebDriverException: Nếu không thể cài đặt ứng dụng",
                    "FileNotFoundException: Nếu không tìm thấy file ứng dụng"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "app", "installation"}
    )
    @Step("Cài đặt ứng dụng từ: {0}")
    public void installApp(String appPath) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).installApp(appPath);
            return null;
        }, appPath);
    }

    @NetatKeyword(
            name = "uninstallApp",
            description = "Gỡ cài đặt một ứng dụng khỏi thiết bị dựa trên định danh của ứng dụng. Trên Android, đây là package name (ví dụ: com.example.myapp). Trên iOS, đây là bundle ID (ví dụ: com.example.MyApp). Lưu ý: Một số ứng dụng hệ thống không thể gỡ cài đặt ngay cả khi có quyền root/jailbreak.",
            category = "Mobile/AppLifecycle",
            parameters = {"String: appId - AppPackage (Android) hoặc BundleID (iOS) của ứng dụng cần gỡ cài đặt."},
            returnValue = "void: Không trả về giá trị",
            example = "// Gỡ cài đặt ứng dụng Android\n" +
                    "mobileKeyword.uninstallApp(\"com.example.myapp\");\n\n" +
                    "// Gỡ cài đặt ứng dụng iOS\n" +
                    "mobileKeyword.uninstallApp(\"com.example.MyApp\");\n\n" +
                    "// Gỡ cài đặt để chuẩn bị cho test case cài đặt mới\n" +
                    "mobileKeyword.uninstallApp(\"com.banking.app\");\n" +
                    "mobileKeyword.installApp(\"C:/apps/banking-app.apk\");",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Có quyền gỡ cài đặt ứng dụng trên thiết bị"},
            exceptions = {"WebDriverException: Nếu không thể gỡ cài đặt ứng dụng",
                    "IllegalArgumentException: Nếu appId không hợp lệ"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "app", "uninstallation"}
    )
    @Step("Gỡ cài đặt ứng dụng: {0}")
    public void uninstallApp(String appId) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).removeApp(appId);
            return null;
        }, appId);
    }

    @NetatKeyword(
            name = "activateApp",
            description = "Đưa một ứng dụng đã được cài đặt lên foreground (màn hình chính). Hữu ích khi cần chuyển đổi giữa các ứng dụng hoặc kích hoạt lại ứng dụng đang chạy nền. Ứng dụng phải đã được cài đặt trên thiết bị, nếu không sẽ gây ra lỗi. Không giống như startActivity trên Android, phương thức này hoạt động trên cả Android và iOS với cùng một cú pháp.",
            category = "Mobile/AppLifecycle",
            parameters = {"String: appId - AppPackage (Android) hoặc BundleID (iOS) của ứng dụng cần kích hoạt."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kích hoạt ứng dụng chính đang test\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\");\n\n" +
                    "// Chuyển sang ứng dụng cài đặt để thay đổi cấu hình thiết bị\n" +
                    "mobileKeyword.activateApp(\"com.android.settings\"); // Android\n" +
                    "// hoặc\n" +
                    "mobileKeyword.activateApp(\"com.apple.Preferences\"); // iOS\n\n" +
                    "// Quay lại ứng dụng chính sau khi thực hiện thao tác trên ứng dụng khác\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\");",
            prerequisites = {"Ứng dụng đã được cài đặt trên thiết bị",
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium"},
            exceptions = {"WebDriverException: Nếu không thể kích hoạt ứng dụng",
                    "NoSuchAppException: Nếu ứng dụng không được cài đặt trên thiết bị"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "app", "activation", "foreground"}
    )
    @Step("Kích hoạt ứng dụng: {0}")
    public void activateApp(String appId) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).activateApp(appId);
            return null;
        }, appId);
    }

    @NetatKeyword(
            name = "terminateApp",
            description = "Buộc dừng (kill) một tiến trình ứng dụng đang chạy. Khác với việc chỉ đưa ứng dụng về background, phương thức này thực sự kết thúc tiến trình của ứng dụng. Hữu ích khi cần kiểm tra khả năng khôi phục trạng thái của ứng dụng sau khi bị buộc dừng, hoặc để đảm bảo ứng dụng bắt đầu từ trạng thái sạch. Trả về true nếu ứng dụng đã được dừng thành công, false nếu ứng dụng không chạy.",
            category = "Mobile/AppLifecycle",
            parameters = {"String: appId - AppPackage (Android) hoặc BundleID (iOS) của ứng dụng cần dừng."},
            returnValue = "boolean: True nếu ứng dụng đã được dừng thành công, false nếu ứng dụng không chạy",
            example = "// Dừng ứng dụng đang test\n" +
                    "mobileKeyword.terminateApp(\"com.example.myapp\");\n\n" +
                    "// Dừng ứng dụng và khởi động lại để kiểm tra tính năng khôi phục\n" +
                    "mobileKeyword.terminateApp(\"com.example.myapp\");\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\");\n\n" +
                    "// Kiểm tra xử lý lỗi khi ứng dụng bị crash\n" +
                    "mobileKeyword.tap(crashButton); // Gây ra crash\n" +
                    "mobileKeyword.terminateApp(\"com.example.myapp\"); // Đảm bảo ứng dụng đã dừng\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\"); // Khởi động lại",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium"},
            exceptions = {"WebDriverException: Nếu không thể dừng ứng dụng",
                    "IllegalArgumentException: Nếu appId không hợp lệ"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "app", "termination", "process"}
    )
    @Step("Dừng ứng dụng: {0}")
    public void terminateApp(String appId) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).terminateApp(appId);
            return null;
        }, appId);
    }

    @NetatKeyword(
            name = "resetApp",
            description = "Reset ứng dụng về trạng thái ban đầu, tương đương với việc xóa dữ liệu ứng dụng. Phương thức này giúp đưa ứng dụng về trạng thái như mới cài đặt mà không cần gỡ và cài đặt lại. Lưu ý: Phương thức này chỉ reset trạng thái đầu vào (input state) của ứng dụng, không phải toàn bộ dữ liệu. Để xóa hoàn toàn dữ liệu ứng dụng, nên sử dụng executeMobileCommand với 'mobile:clearApp' trên Android hoặc gỡ và cài đặt lại trên iOS.",
            category = "Mobile/AppLifecycle",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Reset ứng dụng về trạng thái ban đầu trước mỗi test case\n" +
                    "mobileKeyword.resetApp();\n\n" +
                    "// Reset sau khi hoàn thành một luồng test để chuẩn bị cho luồng tiếp theo\n" +
                    "mobileKeyword.completeCheckout();\n" +
                    "mobileKeyword.resetApp();\n" +
                    "mobileKeyword.loginWithCredentials(username, password);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Ứng dụng đã được khởi động trước đó"},
            exceptions = {"WebDriverException: Nếu không thể reset ứng dụng"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "app", "reset", "clean-state"}
    )
    @Step("Reset ứng dụng")
    public void resetApp() {
        execute(() -> {
            // Lưu ý: resetApp không phải là một phần của InteractsWithApps,
            // nó vẫn nằm trong AppiumDriver. Do đó, logic này vẫn đúng.
            ((AppiumDriver) DriverManager.getDriver()).resetInputState();
            return null;
        });
    }

    @NetatKeyword(
            name = "backgroundApp",
            description = "Đưa ứng dụng hiện tại về chạy nền trong một khoảng thời gian xác định, sau đó tự động đưa lại lên foreground. Hữu ích để kiểm tra khả năng lưu trữ và khôi phục trạng thái của ứng dụng, hoặc để mô phỏng việc người dùng tạm thời chuyển sang ứng dụng khác. Lưu ý: Nếu thời gian là -1, ứng dụng sẽ ở chế độ nền cho đến khi được kích hoạt lại bằng activateApp.",
            category = "Mobile/AppLifecycle",
            parameters = {"int: seconds - Số giây ứng dụng chạy nền. Sử dụng -1 để giữ ứng dụng ở nền vô thời hạn."},
            returnValue = "void: Không trả về giá trị",
            example = "// Đưa ứng dụng về nền trong 5 giây để kiểm tra khả năng lưu trạng thái\n" +
                    "mobileKeyword.sendText(noteInput, \"Ghi chú quan trọng\");\n" +
                    "mobileKeyword.backgroundApp(5);\n" +
                    "mobileKeyword.assertTextEquals(noteInput, \"Ghi chú quan trọng\"); // Kiểm tra dữ liệu còn nguyên\n\n" +
                    "// Mô phỏng việc chuyển sang ứng dụng khác và quay lại\n" +
                    "mobileKeyword.backgroundApp(10);\n\n" +
                    "// Đưa ứng dụng về nền vô thời hạn và sau đó kích hoạt lại thủ công\n" +
                    "mobileKeyword.backgroundApp(-1);\n" +
                    "// Thực hiện các thao tác khác...\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\");",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Ứng dụng đang chạy ở foreground"},
            exceptions = {"WebDriverException: Nếu không thể đưa ứng dụng về background"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "app", "background", "state-preservation"}
    )
    @Step("Đưa ứng dụng về nền trong {0} giây")
    public void backgroundApp(int seconds) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).runAppInBackground(Duration.ofSeconds(seconds));
            return null;
        }, seconds);
    }

// =================================================================================
// --- 2. TƯƠNG TÁC PHẦN TỬ CƠ BẢN ---
// =================================================================================

    @NetatKeyword(
            name = "tap",
            description = "Thực hiện một hành động chạm (tap) vào một phần tử trên màn hình. Đây là thao tác tương đương với click trên web nhưng được tối ưu cho thiết bị di động. Phương thức này sẽ đợi phần tử hiển thị và có thể tương tác trước khi thực hiện chạm.",
            category = "Mobile/Interaction",
            parameters = {"ObjectUI: uiObject - Phần tử cần chạm vào."},
            returnValue = "void: Không trả về giá trị",
            example = "// Chạm vào nút đăng nhập\n" +
                    "mobileKeyword.tap(loginButtonObject);\n\n" +
                    "// Chạm vào menu hamburger để mở navigation drawer\n" +
                    "mobileKeyword.tap(menuButton);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử UI cần tương tác phải hiển thị trên màn hình"},
            exceptions = {"ElementNotVisibleException: Nếu phần tử không hiển thị",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "interaction", "tap", "touch"}
    )
    @Step("Chạm vào phần tử: {0.name}")
    public void tap(ObjectUI uiObject) {
        // Tái sử dụng logic click của lớp cha, Appium sẽ tự động diễn dịch thành 'tap'
        super.click(uiObject);
    }

    @NetatKeyword(
            name = "sendText",
            description = "Nhập văn bản vào một ô input có thể chỉnh sửa. Lưu ý: Chỉ hoạt động với các phần tử có thuộc tính 'editable' là true như TextField, EditText, TextArea, v.v. Không thể sử dụng với các phần tử không cho phép nhập liệu như Button, Label.",
            category = "Mobile/Interaction",
            parameters = {
                    "ObjectUI: uiObject - Đối tượng đầu vào có thể chỉnh sửa (như TextField, EditText).",
                    "String: text - Văn bản cần nhập vào phần tử."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Nhập tên đăng nhập vào ô username\n" +
                    "mobileKeyword.sendText(usernameInput, \"admin@example.com\");\n\n" +
                    "// Nhập mật khẩu vào ô password\n" +
                    "mobileKeyword.sendText(passwordInput, \"SecurePassword123\");",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử UI cần phải là trường nhập liệu có thể chỉnh sửa"},
            exceptions = {"ElementNotVisibleException: Nếu phần tử không hiển thị",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác hoặc không phải trường nhập liệu"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "interaction", "input", "text-entry"}
    )
    @Step("Nhập văn bản '{1}' vào phần tử: {0.name}")
    public void sendText(ObjectUI uiObject, String text) {
        super.sendKeys(uiObject, text);
    }

    @NetatKeyword(
            name = "clear",
            description = "Xóa văn bản trong một ô input có thể chỉnh sửa. Chỉ áp dụng cho các phần tử có thuộc tính 'editable' là true như TextField, EditText. Không hoạt động với các phần tử không phải là trường nhập liệu.",
            category = "Mobile/Interaction",
            parameters = {"ObjectUI: uiObject - Phần tử input cần xóa văn bản."},
            returnValue = "void: Không trả về giá trị",
            example = "// Xóa văn bản trong ô tìm kiếm\n" +
                    "mobileKeyword.clear(searchInput);\n\n" +
                    "// Xóa nội dung trong ô email trước khi nhập giá trị mới\n" +
                    "mobileKeyword.clear(emailInput);\n" +
                    "mobileKeyword.sendText(emailInput, \"new.email@example.com\");",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử UI cần phải là trường nhập liệu có thể chỉnh sửa"},
            exceptions = {"ElementNotVisibleException: Nếu phần tử không hiển thị",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác hoặc không phải trường nhập liệu"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "interaction", "clear", "input"}
    )
    @Step("Xóa văn bản trong phần tử: {0.name}")
    public void clear(ObjectUI uiObject) {
        super.clear(uiObject);
    }

    @NetatKeyword(
            name = "longPress",
            description = "Thực hiện hành động chạm và giữ (long press) vào một phần tử trong một khoảng thời gian xác định. Hữu ích cho các thao tác như hiển thị menu ngữ cảnh, kéo thả, hoặc các tương tác đặc biệt yêu cầu nhấn giữ. Phương thức sẽ đợi phần tử hiển thị trước khi thực hiện.",
            category = "Mobile/Interaction",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần chạm và giữ.",
                    "int: durationInSeconds - Thời gian giữ phần tử, tính bằng giây."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chạm và giữ một hình ảnh trong 2 giây để hiển thị menu lưu ảnh\n" +
                    "mobileKeyword.longPress(imageObject, 2);\n\n" +
                    "// Chạm và giữ một mục trong danh sách để hiển thị menu xóa\n" +
                    "mobileKeyword.longPress(listItemObject, 1);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử UI cần tương tác phải hiển thị trên màn hình"},
            exceptions = {"ElementNotVisibleException: Nếu phần tử không hiển thị",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "ElementNotInteractableException: Nếu phần tử không thể tương tác"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "interaction", "long-press", "context-menu", "gesture"}
    )
    @Step("Chạm và giữ phần tử {0.name} trong {1} giây")
    public void longPress(ObjectUI uiObject, int durationInSeconds) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            Point centerOfElement = getCenterOfElement(element.getLocation(), element.getSize());

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence longPressSequence = new Sequence(finger, 1);
            longPressSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerOfElement.getX(), centerOfElement.getY()));
            longPressSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            longPressSequence.addAction(new Pause(finger, Duration.ofSeconds(durationInSeconds)));
            longPressSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            ((AppiumDriver) DriverManager.getDriver()).perform(Collections.singletonList(longPressSequence));
            return null;
        }, uiObject, durationInSeconds);
    }

    @NetatKeyword(
            name = "hideKeyboard",
            description = "Ẩn bàn phím ảo nếu nó đang hiển thị trên màn hình. Hữu ích khi cần giải phóng không gian màn hình sau khi nhập liệu hoặc trước khi thực hiện các thao tác khác. Lưu ý: Nếu bàn phím không hiển thị, phương thức này có thể gây ra lỗi trên một số thiết bị.",
            category = "Mobile/Interaction",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Nhập văn bản vào ô tìm kiếm, sau đó ẩn bàn phím\n" +
                    "mobileKeyword.sendText(searchInput, \"điện thoại samsung\");\n" +
                    "mobileKeyword.hideKeyboard();\n\n" +
                    "// Ẩn bàn phím trước khi chạm vào nút tìm kiếm\n" +
                    "mobileKeyword.hideKeyboard();\n" +
                    "mobileKeyword.tap(searchButton);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium"},
            exceptions = {"WebDriverException: Nếu không thể ẩn bàn phím hoặc bàn phím không hiển thị"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "interaction", "keyboard", "ui"}
    )
    @Step("Ẩn bàn phím")
    public void hideKeyboard() {
        execute(() -> {
            ((HidesKeyboard) DriverManager.getDriver()).hideKeyboard();
            return null;
        });
    }

    @NetatKeyword(
            name = "pressBack",
            description = "Mô phỏng hành động nhấn nút 'Back' vật lý của thiết bị. Hữu ích để điều hướng ngược lại màn hình trước đó, đóng dialog, hoặc hủy thao tác hiện tại. Trên iOS, hành động này tương đương với việc nhấn nút quay lại ở góc trên bên trái của nhiều ứng dụng.",
            category = "Mobile/Interaction",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Quay lại màn hình trước đó\n" +
                    "mobileKeyword.pressBack();\n\n" +
                    "// Đóng dialog bằng cách nhấn nút Back\n" +
                    "mobileKeyword.pressBack();\n\n" +
                    "// Hủy thao tác nhập liệu\n" +
                    "mobileKeyword.sendText(searchInput, \"text\");\n" +
                    "mobileKeyword.pressBack(); // Hủy và quay lại màn hình trước",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium"},
            exceptions = {"WebDriverException: Nếu không thể thực hiện hành động Back"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "navigation", "back-button", "device-key"}
    )
    @Step("Nhấn nút Back")
    public void pressBack() {
        execute(() -> {
            DriverManager.getDriver().navigate().back();
            return null;
        });
    }


    // =================================================================================
    // --- 3. ĐỒNG BỘ HÓA (WAITS) ---
    // =================================================================================

    @NetatKeyword(
            name = "waitForVisible",
            description = "Chờ cho đến khi một phần tử hiển thị trên màn hình hoặc cho đến khi hết thời gian chờ. Phần tử được coi là hiển thị khi nó tồn tại trong DOM và có thể nhìn thấy được (visible). Phương thức này hữu ích khi cần đảm bảo một phần tử đã xuất hiện trước khi tương tác với nó. Nếu phần tử không hiển thị sau khi hết thời gian chờ, một TimeoutException sẽ được ném ra.",
            category = "Mobile/Wait",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần chờ hiển thị.",
                    "int: timeoutInSeconds - Thời gian tối đa (giây) để chờ phần tử hiển thị."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ nút đăng nhập hiển thị sau khi nhập thông tin\n" +
                    "mobileKeyword.sendText(usernameInput, \"user@example.com\");\n" +
                    "mobileKeyword.sendText(passwordInput, \"password123\");\n" +
                    "mobileKeyword.waitForVisible(loginButton, 5);\n" +
                    "mobileKeyword.tap(loginButton);\n\n" +
                    "// Chờ thông báo thành công hiển thị sau khi gửi biểu mẫu\n" +
                    "mobileKeyword.tap(submitButton);\n" +
                    "mobileKeyword.waitForVisible(successMessage, 10);\n\n" +
                    "// Chờ màn hình chính tải xong sau khi đăng nhập\n" +
                    "mobileKeyword.waitForVisible(homeScreenIndicator, 15);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Đã xác định chính xác phần tử UI cần chờ đợi"},
            exceptions = {"TimeoutException: Nếu phần tử không hiển thị trong thời gian chờ đợi",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "NoSuchElementException: Nếu không tìm thấy phần tử trong DOM"},
            platform = "MOBILE",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"mobile", "wait", "visibility", "synchronization"}
    )
    @Step("Chờ phần tử {0.name} hiển thị trong {1} giây")
    public void waitForVisible(ObjectUI uiObject, int timeoutInSeconds) {
        super.waitForElementVisible(uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForNotVisible",
            description = "Chờ cho đến khi một phần tử biến mất khỏi màn hình hoặc cho đến khi hết thời gian chờ. Phần tử được coi là không hiển thị khi nó không tồn tại trong DOM hoặc không thể nhìn thấy được (invisible). Hữu ích khi cần đảm bảo một phần tử đã biến mất (như màn hình loading) trước khi tiếp tục. Nếu phần tử vẫn hiển thị sau khi hết thời gian chờ, một TimeoutException sẽ được ném ra.",
            category = "Mobile/Wait",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần chờ biến mất.",
                    "int: timeoutInSeconds - Thời gian tối đa (giây) để chờ phần tử biến mất."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ màn hình splash biến mất\n" +
                    "mobileKeyword.waitForNotVisible(splashScreen, 10);\n\n" +
                    "// Chờ biểu tượng loading biến mất sau khi tải dữ liệu\n" +
                    "mobileKeyword.tap(refreshButton);\n" +
                    "mobileKeyword.waitForNotVisible(loadingSpinner, 15);\n\n" +
                    "// Chờ thông báo lỗi tự động đóng\n" +
                    "mobileKeyword.waitForNotVisible(errorToast, 5);\n\n" +
                    "// Đảm bảo hộp thoại đã đóng sau khi nhấn nút hủy\n" +
                    "mobileKeyword.tap(cancelButton);\n" +
                    "mobileKeyword.waitForNotVisible(dialog, 3);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Đã xác định chính xác phần tử UI cần chờ đợi"},
            exceptions = {"TimeoutException: Nếu phần tử vẫn hiển thị sau khi hết thời gian chờ",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM"},
            platform = "MOBILE",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"mobile", "wait", "invisibility", "synchronization"}
    )
    @Step("Chờ phần tử {0.name} biến mất trong {1} giây")
    public void waitForNotVisible(ObjectUI uiObject, int timeoutInSeconds) {
        super.waitForElementNotVisible(uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForClickable",
            description = "Chờ cho đến khi một phần tử sẵn sàng để được chạm vào (clickable/tappable) hoặc cho đến khi hết thời gian chờ. Phần tử được coi là clickable khi nó hiển thị và có thể tương tác được (không bị disabled). Khác với waitForVisible, phương thức này còn kiểm tra khả năng tương tác của phần tử. Nếu phần tử không clickable sau khi hết thời gian chờ, một TimeoutException sẽ được ném ra.",
            category = "Mobile/Wait",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần chờ sẵn sàng để tương tác.",
                    "int: timeoutInSeconds - Thời gian tối đa (giây) để chờ phần tử có thể tương tác."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ nút đăng nhập có thể nhấn sau khi nhập đủ thông tin\n" +
                    "mobileKeyword.sendText(usernameInput, \"user@example.com\");\n" +
                    "mobileKeyword.sendText(passwordInput, \"password123\");\n" +
                    "mobileKeyword.waitForClickable(loginButton, 5);\n" +
                    "mobileKeyword.tap(loginButton);\n\n" +
                    "// Chờ nút tiếp tục có thể nhấn sau khi hoàn thành xử lý\n" +
                    "mobileKeyword.tap(processButton);\n" +
                    "mobileKeyword.waitForClickable(continueButton, 10);\n" +
                    "mobileKeyword.tap(continueButton);\n\n" +
                    "// Chờ nút thanh toán có thể nhấn sau khi chọn phương thức thanh toán\n" +
                    "mobileKeyword.tap(creditCardOption);\n" +
                    "mobileKeyword.waitForClickable(payButton, 3);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Đã xác định chính xác phần tử UI cần chờ đợi"},
            exceptions = {"TimeoutException: Nếu phần tử không trở nên clickable trong thời gian chờ đợi",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "NoSuchElementException: Nếu không tìm thấy phần tử trong DOM"},
            platform = "MOBILE",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"mobile", "wait", "clickable", "interactable", "synchronization"}
    )
    @Step("Chờ phần tử {0.name} sẵn sàng để chạm trong {1} giây")
    public void waitForClickable(ObjectUI uiObject, int timeoutInSeconds) {
        super.waitForElementClickable(uiObject, timeoutInSeconds);
    }


    // =================================================================================
    // --- 4. KIỂM CHỨNG (ASSERTIONS) ---
    // =================================================================================

    @NetatKeyword(
            name = "assertElementPresent",
            description = "Khẳng định rằng một phần tử tồn tại trong cấu trúc DOM của màn hình, không nhất thiết phải hiển thị. Phương thức này kiểm tra ngay lập tức (timeout = 0) và ném AssertionError nếu phần tử không tồn tại. Lưu ý: Phương thức này chỉ kiểm tra sự tồn tại, không kiểm tra tính hiển thị của phần tử.",
            category = "Mobile/Assertion",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra sự tồn tại."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra rằng nút đăng nhập tồn tại trên màn hình\n" +
                    "mobileKeyword.assertElementPresent(loginButton);\n\n" +
                    "// Xác minh rằng menu hamburger tồn tại trong header\n" +
                    "mobileKeyword.assertElementPresent(hamburgerMenu);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Đã xác định chính xác phần tử UI cần kiểm tra"},
            exceptions = {"AssertionError: Nếu phần tử không tồn tại trong DOM",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển"},
            platform = "MOBILE",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"mobile", "assertion", "element", "presence", "verification"}
    )
    @Step("Kiểm tra (Hard) sự tồn tại của phần tử: {0.name}")
    public void assertElementPresent(ObjectUI uiObject) {
        super.verifyElementPresent(uiObject, 0); // timeout 0 để kiểm tra ngay lập tức
    }

    @NetatKeyword(
            name = "assertElementVisible",
            description = "Khẳng định rằng một phần tử đang được hiển thị trên màn hình và người dùng có thể nhìn thấy. Khác với assertElementPresent, phương thức này kiểm tra cả sự tồn tại và tính hiển thị của phần tử. Nếu phần tử không tồn tại hoặc không hiển thị, một AssertionError sẽ được ném ra.",
            category = "Mobile/Assertion",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra tính hiển thị."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra rằng thông báo thành công hiển thị sau khi đăng ký\n" +
                    "mobileKeyword.assertElementVisible(successMessage);\n\n" +
                    "// Xác minh rằng biểu tượng giỏ hàng hiển thị sau khi thêm sản phẩm\n" +
                    "mobileKeyword.assertElementVisible(cartIcon);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Đã xác định chính xác phần tử UI cần kiểm tra"},
            exceptions = {"AssertionError: Nếu phần tử không tồn tại hoặc không hiển thị",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM"},
            platform = "MOBILE",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"mobile", "assertion", "element", "visibility", "verification"}
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} đang hiển thị")
    public void assertElementVisible(ObjectUI uiObject) {
        super.verifyElementVisibleHard(uiObject, true);
    }

    @NetatKeyword(
            name = "assertTextEquals",
            description = "Khẳng định rằng văn bản của một phần tử khớp chính xác với chuỗi mong đợi. Phương thức này trích xuất nội dung văn bản của phần tử và so sánh với giá trị mong đợi, ném AssertionError nếu không khớp. Hữu ích để kiểm tra nội dung văn bản, nhãn, thông báo lỗi hoặc các phần tử hiển thị khác.",
            category = "Mobile/Assertion",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra văn bản.",
                    "String: expectedText - Chuỗi văn bản mong đợi để so sánh."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra tiêu đề màn hình\n" +
                    "mobileKeyword.assertTextEquals(title, \"Đăng nhập\");\n\n" +
                    "// Xác minh thông báo lỗi\n" +
                    "mobileKeyword.sendText(emailInput, \"invalid\");\n" +
                    "mobileKeyword.tap(submitButton);\n" +
                    "mobileKeyword.assertTextEquals(errorMessage, \"Email không hợp lệ\");\n\n" +
                    "// Kiểm tra giá trị hiển thị sau khi tính toán\n" +
                    "mobileKeyword.sendText(amountInput, \"100\");\n" +
                    "mobileKeyword.tap(calculateButton);\n" +
                    "mobileKeyword.assertTextEquals(resultLabel, \"Tổng: 110.00\");",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử UI cần kiểm tra phải hiển thị và chứa văn bản"},
            exceptions = {"AssertionError: Nếu văn bản của phần tử không khớp với giá trị mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM"},
            platform = "MOBILE",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"mobile", "assertion", "text", "content", "verification"}
    )
    @Step("Kiểm tra (Hard) văn bản của {0.name} là '{1}'")
    public void assertTextEquals(ObjectUI uiObject, String expectedText) {
        super.verifyTextHard(uiObject, expectedText);
    }

    @NetatKeyword(
            name = "assertChecked",
            description = "Khẳng định rằng một switch, checkbox hoặc radio button đang ở trạng thái được chọn/bật. Phương thức này kiểm tra thuộc tính 'checked' của phần tử và ném AssertionError nếu phần tử không được chọn. Áp dụng cho các phần tử có thể chọn/bỏ chọn như checkbox, radio button, toggle switch. Lưu ý: Phần tử phải hỗ trợ thuộc tính 'checked', nếu không có thể gây ra lỗi.",
            category = "Mobile/Assertion",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra trạng thái."},
            returnValue = "void: Không trả về giá trị",
            example = "// Kiểm tra rằng tùy chọn đã được chọn\n" +
                    "mobileKeyword.tap(agreeToTermsCheckbox);\n" +
                    "mobileKeyword.assertChecked(agreeToTermsCheckbox);\n\n" +
                    "// Kiểm tra rằng công tắc thông báo đã được bật\n" +
                    "mobileKeyword.tap(notificationSwitch);\n" +
                    "mobileKeyword.assertChecked(notificationSwitch);\n\n" +
                    "// Xác minh rằng radio button đã được chọn sau khi tap\n" +
                    "mobileKeyword.tap(femaleGenderOption);\n" +
                    "mobileKeyword.assertChecked(femaleGenderOption);\n\n" +
                    "// Kiểm tra trạng thái mặc định của một tùy chọn\n" +
                    "mobileKeyword.assertChecked(defaultSelectedOption);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử UI cần kiểm tra phải là loại có thể chọn/bỏ chọn",
                    "Phần tử phải hỗ trợ thuộc tính 'checked'"},
            exceptions = {"AssertionError: Nếu phần tử không ở trạng thái được chọn/bật",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "WebDriverException: Nếu không thể truy cập thuộc tính 'checked'"},
            platform = "MOBILE",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"mobile", "assertion", "checkbox", "radio", "toggle", "state", "verification"}
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} đang được chọn/bật")
    public void assertChecked(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            // Appium dùng thuộc tính 'checked' cho cả Android và iOS
            boolean isChecked = Boolean.parseBoolean(element.getAttribute("checked"));
            Assert.assertTrue(isChecked, "HARD ASSERT FAILED: Phần tử '" + uiObject.getName() + "' không ở trạng thái được chọn/bật.");
            return null;
        }, uiObject);
    }

    // --- Private Helper Methods ---
    private Point getCenterOfElement(Point location, Dimension size) {
        return new Point(location.getX() + size.getWidth() / 2, location.getY() + size.getHeight() / 2);
    }

    @NetatKeyword(
            name = "swipe",
            description = "Thực hiện hành động vuốt trên màn hình từ điểm bắt đầu đến điểm kết thúc. Cho phép kiểm soát chính xác tọa độ bắt đầu, kết thúc và tốc độ vuốt. Tọa độ được tính theo pixel từ góc trên bên trái của màn hình (0,0).",
            category = "Mobile/Gesture",
            parameters = {
                    "int: startX - Tọa độ X điểm bắt đầu vuốt.",
                    "int: startY - Tọa độ Y điểm bắt đầu vuốt.",
                    "int: endX - Tọa độ X điểm kết thúc vuốt.",
                    "int: endY - Tọa độ Y điểm kết thúc vuốt.",
                    "int: durationInMs - Thời gian thực hiện vuốt (ms), giá trị thấp hơn = vuốt nhanh hơn."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Vuốt từ giữa màn hình xuống dưới (mở notification drawer trên Android)\n" +
                    "mobileKeyword.swipe(500, 100, 500, 1500, 300);\n\n" +
                    "// Vuốt từ phải sang trái (để lật trang hoặc xóa mục trong danh sách)\n" +
                    "mobileKeyword.swipe(900, 500, 100, 500, 200);",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium"},
            exceptions = {"WebDriverException: Nếu không thể thực hiện hành động vuốt",
                    "IllegalArgumentException: Nếu tọa độ nằm ngoài kích thước màn hình"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "gesture", "swipe", "touch", "interaction"}
    )
    @Step("Vuốt từ ({0},{1}) đến ({2},{3})")
    public void swipe(int startX, int startY, int endX, int endY, int durationInMs) {
        execute(() -> {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipeSequence = new Sequence(finger, 1);
            swipeSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
            swipeSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipeSequence.addAction(finger.createPointerMove(Duration.ofMillis(durationInMs), PointerInput.Origin.viewport(), endX, endY));
            swipeSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            ((AppiumDriver) DriverManager.getDriver()).perform(Collections.singletonList(swipeSequence));
            return null;
        }, startX, startY, endX, endY, durationInMs);
    }

    @NetatKeyword(
            name = "swipeUp",
            description = "Thực hiện hành động vuốt lên trên màn hình, tương đương với thao tác cuộn xuống để xem nội dung bên dưới. Phương thức này tự động tính toán các tọa độ dựa trên kích thước màn hình thiết bị.",
            category = "Mobile/Gesture",
            parameters = {
                    "Integer...: durationInMs - (Tùy chọn) Thời gian thực hiện vuốt (ms). Mặc định là 500ms nếu không được chỉ định."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Vuốt lên với tốc độ mặc định để xem thêm nội dung\n" +
                    "mobileKeyword.swipeUp();\n\n" +
                    "// Vuốt lên với tốc độ chậm hơn (1000ms)\n" +
                    "mobileKeyword.swipeUp(1000);\n\n" +
                    "// Cuộn xuống danh sách sản phẩm để tải thêm\n" +
                    "for (int i = 0; i < 3; i++) {\n" +
                    "    mobileKeyword.swipeUp();\n" +
                    "    mobileKeyword.waitForVisible(loadingIndicator, 2);\n" +
                    "}",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium"},
            exceptions = {"WebDriverException: Nếu không thể thực hiện hành động vuốt"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "gesture", "swipe", "scroll", "up"}
    )
    @Step("Vuốt lên trên màn hình")
    public void swipeUp(Integer... durationInMs) {
        int duration = (durationInMs != null && durationInMs.length > 0 && durationInMs[0] != null)
                ? durationInMs[0]
                : 500;
        execute(() -> {
            Dimension size = DriverManager.getDriver().manage().window().getSize();
            int startX = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.8);
            int endY = (int) (size.getHeight() * 0.2);
            swipe(startX, startY, startX, endY, duration);
            return null;
        }, (Object[]) durationInMs);
    }

    @NetatKeyword(
            name = "swipeDown",
            description = "Thực hiện hành động vuốt xuống dưới màn hình, tương đương với thao tác cuộn lên để xem nội dung phía trên. Phương thức này tự động tính toán các tọa độ dựa trên kích thước màn hình thiết bị hiện tại.",
            category = "Mobile/Gesture",
            parameters = {
                    "Integer...: durationInMs - (Tùy chọn) Thời gian thực hiện vuốt (ms). Mặc định là 500ms nếu không được chỉ định."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Vuốt xuống với tốc độ mặc định để làm mới trang\n" +
                    "mobileKeyword.swipeDown();\n\n" +
                    "// Vuốt xuống với tốc độ chậm hơn (800ms)\n" +
                    "mobileKeyword.swipeDown(800);\n\n" +
                    "// Cuộn lên đầu danh sách\n" +
                    "for (int i = 0; i < 2; i++) {\n" +
                    "    mobileKeyword.swipeDown();\n" +
                    "}",
            prerequisites = {"Thiết bị di động đã được kết nối và cấu hình đúng với Appium"},
            exceptions = {"WebDriverException: Nếu không thể thực hiện hành động vuốt"},
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "gesture", "swipe", "scroll", "down", "refresh"}
    )
    @Step("Vuốt xuống dưới màn hình")
    public void swipeDown(Integer... durationInMs) {
        execute(() -> {
            Dimension size = DriverManager.getDriver().manage().window().getSize();
            int startX = size.getWidth() / 2;
            int startY = (int) (size.getHeight() * 0.2);
            int endY = (int) (size.getHeight() * 0.8);
            int duration = (durationInMs.length > 0) ? durationInMs[0] : 500;
            swipe(startX, startY, startX, endY, duration);
            return null;
        }, (Object[]) durationInMs);
    }


    @NetatKeyword(
            name = "scrollToText",
            description = "Tự động cuộn màn hình (vuốt lên) cho đến khi tìm thấy một phần tử chứa văn bản mong muốn. Phương thức này sẽ thực hiện tối đa 10 lần vuốt lên để tìm kiếm. Cách hoạt động khác nhau giữa Android (sử dụng UiScrollable) và iOS (sử dụng vuốt tuần tự). Trả về WebElement nếu tìm thấy, hoặc ném NoSuchElementException nếu không tìm thấy sau khi đã cuộn hết.",
            category = "Mobile/Gesture",
            parameters = {
                    "String: textToFind - Văn bản cần tìm kiếm trên màn hình. Có thể là toàn bộ hoặc một phần của văn bản hiển thị."
            },
            returnValue = "WebElement: Phần tử chứa văn bản được tìm thấy",
            example = "// Cuộn đến khi thấy nút \"Đăng ký\" và chạm vào nó\n" +
                    "WebElement registerButton = mobileKeyword.scrollToText(\"Đăng ký\");\n" +
                    "registerButton.click();\n\n" +
                    "// Cuộn đến phần \"Điều khoản sử dụng\" trong một trang dài\n" +
                    "mobileKeyword.scrollToText(\"Điều khoản sử dụng\");\n\n" +
                    "// Tìm và tương tác với một mục trong danh sách dài\n" +
                    "WebElement productItem = mobileKeyword.scrollToText(\"iPhone 14 Pro\");\n" +
                    "mobileKeyword.tap(new ObjectUI(\"Sản phẩm\", productItem));",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Văn bản cần tìm phải tồn tại trên màn hình (có thể cần cuộn để hiển thị)",
                    "Trên Android: Phần tử cha chứa nội dung cần cuộn phải có thuộc tính scrollable=true"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử chứa văn bản sau khi cuộn hết",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển",
                    "IllegalStateException: Nếu không thể xác định nền tảng hoặc không hỗ trợ"
            },
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "gesture", "scroll", "text", "search", "find", "automation"}
    )
    @Step("Cuộn đến khi thấy văn bản: {0}")
    public WebElement scrollToText(String textToFind) {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            String platform = driver.getCapabilities().getPlatformName().toString();

            if ("android".equalsIgnoreCase(platform)) {
                // Android có API tích hợp để cuộn đến văn bản
                By locator = AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true))" +
                                ".scrollIntoView(new UiSelector().textContains(\"" + textToFind + "\"))"
                );
                return driver.findElement(locator);
            } else { // iOS
                // iOS cần logic phức tạp hơn
                By locator = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND value CONTAINS '" + textToFind + "'");

                // Thử tìm trước khi cuộn
                List<WebElement> elements = driver.findElements(locator);
                if (!elements.isEmpty()) {
                    return elements.get(0);
                }

                // Cuộn và tìm kiếm
                int maxScrolls = 10; // Giới hạn số lần cuộn
                for (int i = 0; i < maxScrolls; i++) {
                    swipeUp(500); // Cuộn lên
                    elements = driver.findElements(locator);
                    if (!elements.isEmpty()) {
                        return elements.get(0);
                    }
                }

                throw new NoSuchElementException("Không tìm thấy phần tử có văn bản: " + textToFind + " sau " + maxScrolls + " lần cuộn");
            }
        }, textToFind);
    }

    @NetatKeyword(
            name = "dragAndDrop",
            description = "Kéo một phần tử từ vị trí nguồn và thả vào vị trí của phần tử đích. Hữu ích cho các thao tác như sắp xếp lại danh sách, di chuyển các phần tử trong giao diện, hoặc kéo thả vào vùng đích. Phương thức sẽ tự động tính toán tọa độ trung tâm của cả hai phần tử để thực hiện thao tác chính xác.",
            category = "Mobile/Gesture",
            parameters = {
                    "ObjectUI: source - Phần tử nguồn cần kéo.",
                    "ObjectUI: destination - Phần tử đích để thả vào."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Kéo một mục từ danh sách và thả vào thùng rác\n" +
                    "mobileKeyword.dragAndDrop(listItem, trashBin);\n\n" +
                    "// Sắp xếp lại thứ tự các mục trong danh sách bằng cách kéo mục đầu tiên xuống vị trí thứ ba\n" +
                    "mobileKeyword.dragAndDrop(firstItem, thirdItem);\n\n" +
                    "// Kéo một hình ảnh vào album\n" +
                    "mobileKeyword.dragAndDrop(photoObject, albumObject);\n\n" +
                    "// Kéo thả để điều chỉnh thanh trượt\n" +
                    "mobileKeyword.dragAndDrop(sliderHandle, sliderTarget);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Cả phần tử nguồn và đích phải hiển thị trên màn hình",
                    "Ứng dụng phải hỗ trợ thao tác kéo thả cho các phần tử này"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử nguồn hoặc đích",
                    "WebDriverException: Nếu có lỗi khi thực hiện thao tác kéo thả",
                    "ElementNotInteractableException: Nếu không thể tương tác với phần tử nguồn hoặc đích"
            },
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "gesture", "drag", "drop", "touch", "interaction", "move"}
    )
    @Step("Kéo phần tử {0.name} và thả vào {1.name}")
    public void dragAndDrop(ObjectUI source, ObjectUI destination) {
        execute(() -> {
            WebElement sourceElement = findElement(source);
            WebElement destElement = findElement(destination);
            Point sourceCenter = getCenterOfElement(sourceElement.getLocation(), sourceElement.getSize());
            Point destCenter = getCenterOfElement(destElement.getLocation(), destElement.getSize());

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence dragAndDropSequence = new Sequence(finger, 1);
            dragAndDropSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), sourceCenter.getX(), sourceCenter.getY()));
            dragAndDropSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            dragAndDropSequence.addAction(new Pause(finger, Duration.ofMillis(500))); // Chờ một chút trước khi kéo
            dragAndDropSequence.addAction(finger.createPointerMove(Duration.ofMillis(1000), PointerInput.Origin.viewport(), destCenter.getX(), destCenter.getY()));
            dragAndDropSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            ((AppiumDriver) DriverManager.getDriver()).perform(Collections.singletonList(dragAndDropSequence));
            return null;
        }, source, destination);
    }


    // --- 2. XỬ LÝ WEBVIEW (HYBRID APP) ---

//    @NetatKeyword(
//            name = "switchToContext",
//            description = "Chuyển sự điều khiển của driver sang một ngữ cảnh (context) khác, ví dụ như một WebView.",
//            category = "Mobile/WebView",
//            parameters = {"String: contextName - Tên của context cần chuyển đến (ví dụ: 'WEBVIEW_com.myapp')."},
//            example = "mobileKeyword.switchToContext(\"WEBVIEW_1\");"
//    )
//    @Step("Chuyển sang context: {0}")
//    public void switchToContext(String contextName) {
//        execute(() -> {
//            ((ContextAware) DriverManager.getDriver()).context(contextName);
//            logger.info("Đã chuyển sang context: {}", contextName);
//            return null;
//        }, contextName);
//    }
//
//    @NetatKeyword(
//            name = "switchToNativeContext",
//            description = "Chuyển sự điều khiển của driver về lại ngữ cảnh gốc của ứng dụng (NATIVE_APP).",
//            category = "Mobile/WebView",
//            parameters = {},
//            example = "mobileKeyword.switchToNativeContext();"
//    )
//    @Step("Chuyển về context gốc (NATIVE_APP)")
//    public void switchToNativeContext() {
//        execute(() -> {
//            ((ContextAware) DriverManager.getDriver()).context("NATIVE_APP");
//            logger.info("Đã chuyển về context gốc: NATIVE_APP");
//            return null;
//        });
//    }

    // --- 3. HỘP THOẠI HỆ THỐNG & QUYỀN ---

    @NetatKeyword(
            name = "acceptSystemDialog",
            description = "Tự động tìm và nhấn vào các nút hệ thống có văn bản khẳng định như 'Allow', 'OK', 'Accept', 'While using the app'. Hữu ích để xử lý các hộp thoại cấp quyền hoặc thông báo hệ thống. Phương thức sẽ tìm kiếm các nút phổ biến và nhấn vào nút đầu tiên tìm thấy. Nếu không tìm thấy nút nào, một cảnh báo sẽ được ghi vào log.",
            category = "Mobile/System",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Chấp nhận hộp thoại yêu cầu quyền truy cập vị trí\n" +
                    "mobileKeyword.tap(locationButton);\n" +
                    "mobileKeyword.acceptSystemDialog();\n\n" +
                    "// Chấp nhận thông báo cập nhật ứng dụng\n" +
                    "mobileKeyword.acceptSystemDialog();\n\n" +
                    "// Xử lý nhiều hộp thoại liên tiếp\n" +
                    "for (int i = 0; i < 3; i++) {\n" +
                    "    mobileKeyword.acceptSystemDialog();\n" +
                    "    Thread.sleep(1000); // Đợi hộp thoại tiếp theo xuất hiện\n" +
                    "}\n\n" +
                    "// Chấp nhận hộp thoại yêu cầu quyền thông báo\n" +
                    "mobileKeyword.tap(notificationSettingsButton);\n" +
                    "mobileKeyword.acceptSystemDialog();",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Hộp thoại hệ thống đang hiển thị trên màn hình",
                    "Hộp thoại chứa ít nhất một nút có văn bản khẳng định được hỗ trợ"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển",
                    "NoSuchElementException: Nếu không tìm thấy nút nào khớp với danh sách văn bản đã cho"
            },
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "system", "dialog", "permission", "accept", "allow", "popup"}
    )
    @Step("Chấp nhận hộp thoại hệ thống")
    public void acceptSystemDialog() {
        execute(() -> {
            List<String> buttonTexts = Arrays.asList("Allow", "OK", "Accept", "While using the app");
            clickSystemButtonWithText(buttonTexts);
            return null;
        });
    }

    @NetatKeyword(
            name = "denySystemDialog",
            description = "Tự động tìm và nhấn vào các nút hệ thống có văn bản phủ định như 'Deny', 'Cancel', 'Don't allow'. Hữu ích để từ chối các yêu cầu cấp quyền hoặc đóng các thông báo hệ thống không mong muốn. Phương thức sẽ tìm kiếm các nút phổ biến và nhấn vào nút đầu tiên tìm thấy. Nếu không tìm thấy nút nào, một cảnh báo sẽ được ghi vào log.",
            category = "Mobile/System",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Từ chối yêu cầu quyền truy cập vị trí để kiểm tra xử lý khi không có quyền\n" +
                    "mobileKeyword.tap(locationFeatureButton);\n" +
                    "mobileKeyword.denySystemDialog();\n" +
                    "mobileKeyword.assertElementVisible(locationPermissionDeniedMessage);\n\n" +
                    "// Từ chối thông báo cập nhật ứng dụng để tiếp tục với phiên bản hiện tại\n" +
                    "mobileKeyword.denySystemDialog();\n\n" +
                    "// Từ chối nhiều hộp thoại liên tiếp\n" +
                    "for (int i = 0; i < 3; i++) {\n" +
                    "    mobileKeyword.denySystemDialog();\n" +
                    "    Thread.sleep(1000); // Đợi hộp thoại tiếp theo xuất hiện\n" +
                    "}\n\n" +
                    "// Từ chối yêu cầu đánh giá ứng dụng\n" +
                    "mobileKeyword.denySystemDialog();",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Hộp thoại hệ thống đang hiển thị trên màn hình",
                    "Hộp thoại chứa ít nhất một nút có văn bản phủ định được hỗ trợ"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển",
                    "NoSuchElementException: Nếu không tìm thấy nút nào khớp với danh sách văn bản đã cho"
            },
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "system", "dialog", "permission", "deny", "cancel", "popup", "reject"}
    )
    @Step("Từ chối hộp thoại hệ thống")
    public void denySystemDialog() {
        execute(() -> {
            List<String> buttonTexts = Arrays.asList("Deny", "Cancel", "Don't allow");
            clickSystemButtonWithText(buttonTexts);
            return null;
        });
    }

// --- Private Helper Methods ---

    private void clickSystemButtonWithText(List<String> possibleTexts) {
        AppiumDriver driver = getAppiumDriver();
        String platform = driver.getCapabilities().getPlatformName().toString();

        for (String text : possibleTexts) {
            try {
                By locator;
                if ("android".equalsIgnoreCase(platform)) {
                    locator = By.xpath("//*[@class='android.widget.Button' and (@text='" + text + "' or @text='" + text.toUpperCase() + "')]");
                } else { // iOS
                    locator = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (label == '" + text + "' OR label == '" + text.toUpperCase() + "')");
                }

                List<WebElement> elements = driver.findElements(locator);
                if (!elements.isEmpty()) {
                    elements.get(0).click();
                    logger.info("Đã nhấn vào nút hệ thống: '{}'", text);
                    return;
                }
            } catch (Exception e) {
                logger.debug("Không thể nhấn nút '{}': {}", text, e.getMessage());
            }
        }
        logger.warn("Không tìm thấy nút hệ thống nào với các văn bản đã cho: {}", possibleTexts);
    }

    @NetatKeyword(
            name = "tapByCoordinates",
            description = "Thực hiện một hành động chạm (tap) tại một tọa độ (x, y) cụ thể trên màn hình. Hữu ích khi cần tương tác với các phần tử không thể định vị bằng các locator thông thường, hoặc khi cần chạm vào một vị trí tương đối trên màn hình. Tọa độ được tính theo pixel từ góc trên bên trái của màn hình (0,0). Lưu ý: Tọa độ có thể khác nhau trên các thiết bị có kích thước màn hình khác nhau.",
            category = "Mobile/Gesture",
            parameters = {
                    "int: x - Tọa độ theo trục ngang (pixel).",
                    "int: y - Tọa độ theo trục dọc (pixel)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chạm vào trung tâm màn hình\n" +
                    "Dimension size = DriverManager.getDriver().manage().window().getSize();\n" +
                    "int centerX = size.width / 2;\n" +
                    "int centerY = size.height / 2;\n" +
                    "mobileKeyword.tapByCoordinates(centerX, centerY);\n\n" +
                    "// Chạm vào góc trên bên phải để đóng quảng cáo\n" +
                    "mobileKeyword.tapByCoordinates(size.width - 50, 50);\n\n" +
                    "// Chạm vào vị trí cụ thể trên bản đồ\n" +
                    "mobileKeyword.tapByCoordinates(540, 960);\n\n" +
                    "// Chạm vào vị trí tương đối (75% chiều rộng, 30% chiều cao)\n" +
                    "mobileKeyword.tapByCoordinates((int)(size.width * 0.75), (int)(size.height * 0.3));",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Tọa độ cần nằm trong phạm vi kích thước màn hình của thiết bị"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi thực hiện hành động chạm",
                    "IllegalArgumentException: Nếu tọa độ nằm ngoài kích thước màn hình"
            },
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "gesture", "tap", "touch", "coordinate", "position", "click"}
    )
    @Step("Chạm vào tọa độ ({0}, {1})")
    public void tapByCoordinates(int x, int y) {
        execute(() -> {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence tapSequence = new Sequence(finger, 1);
            tapSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            tapSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            tapSequence.addAction(new Pause(finger, Duration.ofMillis(100))); // Giả lập một cú chạm nhanh
            tapSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            ((AppiumDriver) DriverManager.getDriver()).perform(Collections.singletonList(tapSequence));
            return null;
        }, x, y);
    }

    @NetatKeyword(
            name = "longPressByCoordinates",
            description = "Thực hiện hành động chạm và giữ tại một tọa độ (x, y) trong một khoảng thời gian xác định. Hữu ích khi cần thực hiện các thao tác đặc biệt như hiển thị menu ngữ cảnh tại một vị trí cụ thể, hoặc khi tương tác với các phần tử không thể định vị bằng locator. Tọa độ được tính theo pixel từ góc trên bên trái của màn hình (0,0).",
            category = "Mobile/Gesture",
            parameters = {
                    "int: x - Tọa độ theo trục ngang (pixel).",
                    "int: y - Tọa độ theo trục dọc (pixel).",
                    "int: durationInSeconds - Thời gian giữ (tính bằng giây)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chạm và giữ tại trung tâm màn hình trong 2 giây\n" +
                    "Dimension size = DriverManager.getDriver().manage().window().getSize();\n" +
                    "int centerX = size.width / 2;\n" +
                    "int centerY = size.height / 2;\n" +
                    "mobileKeyword.longPressByCoordinates(centerX, centerY, 2);\n\n" +
                    "// Chạm và giữ tại một vị trí trên bản đồ để thả ghim\n" +
                    "mobileKeyword.longPressByCoordinates(450, 800, 1);\n\n" +
                    "// Chạm và giữ tại một vị trí trên hình ảnh để hiển thị menu lưu ảnh\n" +
                    "mobileKeyword.longPressByCoordinates(300, 500, 2);\n\n" +
                    "// Mô phỏng thao tác chỉnh sửa văn bản (chạm và giữ để hiển thị con trỏ)\n" +
                    "mobileKeyword.longPressByCoordinates(200, 600, 1);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Tọa độ cần nằm trong phạm vi kích thước màn hình của thiết bị",
                    "Ứng dụng phải hỗ trợ thao tác chạm và giữ tại vị trí đó"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi thực hiện hành động chạm và giữ",
                    "IllegalArgumentException: Nếu tọa độ nằm ngoài kích thước màn hình hoặc thời gian giữ không hợp lệ"
            },
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "gesture", "longpress", "touch", "coordinate", "position", "context", "menu"}
    )
    @Step("Chạm và giữ tại tọa độ ({0}, {1}) trong {2} giây")
    public void longPressByCoordinates(int x, int y, int durationInSeconds) {
        execute(() -> {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence longPressSequence = new Sequence(finger, 1);
            longPressSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            longPressSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            longPressSequence.addAction(new Pause(finger, Duration.ofSeconds(durationInSeconds)));
            longPressSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            ((AppiumDriver) DriverManager.getDriver()).perform(Collections.singletonList(longPressSequence));
            return null;
        }, x, y, durationInSeconds);
    }


//    @NetatKeyword(
//            name = "setGeoLocation",
//            description = "Giả lập và thiết lập vị trí GPS của thiết bị.",
//            category = "Mobile/System",
//            parameters = {
//                    "double: latitude - Vĩ độ.",
//                    "double: longitude - Kinh độ."
//            },
//            example = "mobileKeyword.setGeoLocation(21.028511, 105.804817); // Tọa độ Hà Nội"
//    )
//    @Step("Thiết lập vị trí GPS: Vĩ độ={0}, Kinh độ={1}")
//    public void setGeoLocation(double latitude, double longitude) {
//        execute(() -> {
//            Location location = new Location(latitude, longitude, 0); // altitude có thể để là 0
//            ((AppiumDriver) DriverManager.getDriver()).setLocation(location);
//            logger.info("Đã thiết lập vị trí GPS thành: ({}, {})", latitude, longitude);
//            return null;
//        }, latitude, longitude);
//    }

    @NetatKeyword(
            name = "toggleAirplaneMode",
            description = "Bật hoặc tắt chế độ máy bay trên thiết bị Android. Mỗi lần gọi sẽ chuyển đổi trạng thái hiện tại (nếu đang bật sẽ tắt, nếu đang tắt sẽ bật). Chỉ hoạt động trên Android, sẽ hiển thị cảnh báo nếu được gọi trên iOS. Hữu ích khi cần kiểm tra hành vi ứng dụng trong điều kiện không có kết nối mạng.",
            category = "Mobile/System",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Bật chế độ máy bay để kiểm tra xử lý offline\n" +
                    "mobileKeyword.toggleAirplaneMode(); // Bật chế độ máy bay\n\n" +
                    "// Thực hiện các thao tác kiểm tra offline\n" +
                    "mobileKeyword.tap(refreshButton);\n" +
                    "mobileKeyword.assertElementVisible(offlineMessage);\n\n" +
                    "// Tắt chế độ máy bay để kiểm tra khả năng phục hồi kết nối\n" +
                    "mobileKeyword.toggleAirplaneMode(); // Tắt chế độ máy bay\n" +
                    "mobileKeyword.waitForNotVisible(offlineMessage, 10);\n\n" +
                    "// Kiểm tra đồng bộ hóa sau khi kết nối lại\n" +
                    "mobileKeyword.waitForVisible(syncCompleteIndicator, 15);",
            prerequisites = {
                    "Thiết bị Android đã được kết nối và cấu hình đúng với Appium",
                    "Ứng dụng Appium phải có quyền thay đổi chế độ máy bay (cần quyền WRITE_SETTINGS)",
                    "Thiết bị Android phải hỗ trợ chế độ máy bay"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển",
                    "UnsupportedOperationException: Nếu được gọi trên thiết bị iOS",
                    "SecurityException: Nếu không có đủ quyền để thay đổi chế độ máy bay"
            },
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "system", "network", "airplane", "offline", "connectivity", "android"}
    )
    @Step("Bật/Tắt chế độ máy bay")
    public void toggleAirplaneMode() {
        execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).toggleAirplaneMode();
            } else {
                logger.warn("Keyword 'toggleAirplaneMode' chỉ được hỗ trợ trên Android.");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "takeScreenshot",
            description = "Chụp ảnh màn hình của thiết bị và lưu vào thư mục screenshots với tên file được chỉ định. Hữu ích khi cần ghi lại trạng thái màn hình tại các điểm quan trọng trong quá trình test, đặc biệt là khi gặp lỗi hoặc cần xác minh giao diện. Ảnh chụp màn hình sẽ được lưu với định dạng .png và tự động đính kèm vào báo cáo Allure nếu được cấu hình.",
            category = "Mobile/System",
            parameters = {"String: fileName - Tên file để lưu ảnh (không cần đuôi .png)."},
            returnValue = "void: Không trả về giá trị",
            example = "// Chụp màn hình tại các bước quan trọng\n" +
                    "mobileKeyword.takeScreenshot(\"login_screen\");\n" +
                    "mobileKeyword.sendText(usernameInput, \"user@example.com\");\n" +
                    "mobileKeyword.sendText(passwordInput, \"password123\");\n" +
                    "mobileKeyword.takeScreenshot(\"credentials_entered\");\n" +
                    "mobileKeyword.tap(loginButton);\n" +
                    "mobileKeyword.takeScreenshot(\"after_login\");\n\n" +
                    "// Chụp màn hình khi gặp lỗi\n" +
                    "try {\n" +
                    "    mobileKeyword.tap(submitButton);\n" +
                    "    mobileKeyword.waitForVisible(successMessage, 10);\n" +
                    "} catch (Exception e) {\n" +
                    "    mobileKeyword.takeScreenshot(\"error_submit_form\");\n" +
                    "    throw e;\n" +
                    "}\n\n" +
                    "// Chụp màn hình để xác minh giao diện\n" +
                    "mobileKeyword.takeScreenshot(\"product_details_page\");",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Thư mục screenshots phải tồn tại hoặc có quyền tạo",
                    "Ứng dụng Appium phải có quyền chụp ảnh màn hình"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi chụp ảnh màn hình",
                    "IOException: Nếu không thể lưu ảnh vào thư mục chỉ định",
                    "IllegalArgumentException: Nếu tên file không hợp lệ"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "system", "screenshot", "evidence", "debug", "report", "documentation"}
    )
    @Step("Chụp ảnh màn hình với tên file: {0}")
    public void takeScreenshot(String fileName) {
        // Tái sử dụng lại tiện ích chung đã có trong netat-core
        execute(() -> {
            ScreenshotUtils.takeScreenshot(fileName);
            return null;
        }, fileName);
    }

    @NetatKeyword(
            name = "pushFile",
            description = "Đẩy một file từ máy tính vào một đường dẫn trên thiết bị di động. Hữu ích khi cần chuẩn bị dữ liệu hoặc tài nguyên cho test case. Lưu ý: Đường dẫn trên thiết bị phải là đường dẫn mà ứng dụng có quyền ghi. Trên Android, thường là trong /sdcard/. Trên iOS, cần sử dụng bundle path.",
            category = "Mobile/File",
            parameters = {
                    "String: devicePath - Đường dẫn đích trên thiết bị di động.",
                    "String: localFilePath - Đường dẫn tuyệt đối đến file trên máy tính chạy test."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Đẩy một hình ảnh vào thư mục Downloads của thiết bị Android\n" +
                    "mobileKeyword.pushFile(\"/sdcard/Download/avatar.png\", \"C:/test-data/images/avatar.png\");\n\n" +
                    "// Đẩy một file JSON cấu hình vào thư mục Documents\n" +
                    "mobileKeyword.pushFile(\"/sdcard/Documents/config.json\", \"C:/test-data/config.json\");\n\n" +
                    "// Đẩy file CSV chứa dữ liệu test\n" +
                    "mobileKeyword.pushFile(\"/sdcard/TestData/users.csv\", \"src/test/resources/testdata/users.csv\");\n\n" +
                    "// Trên iOS, đẩy file vào bundle của ứng dụng\n" +
                    "mobileKeyword.pushFile(\"@com.example.myapp/Documents/settings.json\", \"src/test/resources/ios/settings.json\");",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "File nguồn phải tồn tại trên máy chạy test",
                    "Đường dẫn đích trên thiết bị phải có quyền ghi",
                    "Trên Android không root, chỉ có thể ghi vào thư mục shared như /sdcard/",
                    "Trên iOS, cần sử dụng đường dẫn bundle đúng cú pháp"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển",
                    "IOException: Nếu không thể đọc file nguồn",
                    "IllegalArgumentException: Nếu đường dẫn không hợp lệ",
                    "SecurityException: Nếu không có quyền ghi vào đường dẫn đích"
            },
            platform = "MOBILE",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"mobile", "file", "transfer", "upload", "data", "resource", "preparation"}
    )
    @Step("Đẩy file từ '{1}' vào thiết bị tại '{0}'")
    public void pushFile(String devicePath, String localFilePath) {
        execute(() -> {
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(localFilePath));
                ((PushesFiles) DriverManager.getDriver()).pushFile(devicePath, fileContent);
            } catch (IOException e) {
                throw new RuntimeException("Không thể đọc file từ đường dẫn: " + localFilePath, e);
            }
            return null;
        }, devicePath, localFilePath);
    }

    @NetatKeyword(
            name = "pullFile",
            description = "Kéo một file từ thiết bị về máy tính và trả về nội dung dưới dạng chuỗi (đã được giải mã Base64). Hữu ích khi cần lấy các file log, dữ liệu hoặc tài nguyên từ thiết bị để phân tích hoặc xác minh. Lưu ý: Đường dẫn phải trỏ đến một file có thể truy cập được từ ứng dụng (với quyền thích hợp). Trên Android không root, thường chỉ có thể truy cập các file trong thư mục ứng dụng.",
            category = "Mobile/File",
            parameters = {"String: devicePath - Đường dẫn đến file trên thiết bị."},
            returnValue = "String: Nội dung của file được trả về dưới dạng chuỗi (đã giải mã Base64)",
            example = "// Lấy nội dung file log để kiểm tra\n" +
                    "String logContent = mobileKeyword.pullFile(\"/sdcard/Download/app.log\");\n" +
                    "assert logContent.contains(\"Transaction completed\") : \"Log không chứa thông tin giao dịch\";\n\n" +
                    "// Lấy file cấu hình để xác minh thiết lập\n" +
                    "String configContent = mobileKeyword.pullFile(\"/data/data/com.example.myapp/files/config.json\");\n" +
                    "// Phân tích JSON và kiểm tra giá trị\n" +
                    "JSONObject config = new JSONObject(configContent);\n" +
                    "assert config.getBoolean(\"darkMode\") : \"Chế độ tối chưa được bật\";\n\n" +
                    "// Lấy file CSV đã tạo bởi ứng dụng để kiểm tra dữ liệu xuất\n" +
                    "String csvData = mobileKeyword.pullFile(\"/sdcard/Documents/export.csv\");\n" +
                    "assert csvData.split(\"\\n\").length > 1 : \"File CSV không chứa dữ liệu\";",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "File cần lấy phải tồn tại trên thiết bị",
                    "Ứng dụng phải có quyền đọc file đó",
                    "Trên Android không root, chỉ có thể đọc file từ thư mục ứng dụng hoặc thư mục shared",
                    "Trên iOS, cần sử dụng đường dẫn bundle đúng cú pháp"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển",
                    "NoSuchFileException: Nếu file không tồn tại trên thiết bị",
                    "SecurityException: Nếu không có quyền đọc file",
                    "IllegalArgumentException: Nếu đường dẫn không hợp lệ"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "file", "transfer", "download", "data", "verification", "log", "analysis"}
    )
    @Step("Kéo file từ thiết bị tại: {0}")
    public String pullFile(String devicePath) {
        return execute(() -> {
            byte[] fileBase64 = ((PullsFiles) DriverManager.getDriver()).pullFile(devicePath);
            return new String(Base64.getDecoder().decode(fileBase64));
        }, devicePath);
    }

    @NetatKeyword(
            name = "getClipboard",
            description = "Lấy và trả về nội dung văn bản hiện tại của clipboard trên thiết bị. Hữu ích khi cần kiểm tra nội dung đã được sao chép hoặc khi cần lấy dữ liệu từ clipboard để sử dụng trong các bước test tiếp theo. Phương thức này trả về một chuỗi chứa nội dung văn bản của clipboard. Lưu ý: Chỉ hỗ trợ nội dung văn bản, không hỗ trợ các loại dữ liệu khác như hình ảnh.",
            category = "Mobile/System",
            parameters = {},
            returnValue = "String: Nội dung văn bản hiện có trong clipboard của thiết bị",
            example = "// Kiểm tra nội dung đã được sao chép đúng\n" +
                    "mobileKeyword.longPress(emailText, 2); // Chọn văn bản\n" +
                    "mobileKeyword.tap(copyOption); // Chọn 'Sao chép' từ menu ngữ cảnh\n" +
                    "String copiedText = mobileKeyword.getClipboard();\n" +
                    "assert copiedText.equals(\"user@example.com\") : \"Email was not copied correctly\";\n\n" +
                    "// Sao chép mã xác minh và sử dụng nó\n" +
                    "mobileKeyword.longPress(verificationCode, 1);\n" +
                    "mobileKeyword.tap(copyButton);\n" +
                    "String code = mobileKeyword.getClipboard();\n" +
                    "mobileKeyword.sendText(codeInput, code);\n\n" +
                    "// Kiểm tra chức năng 'Sao chép liên kết'\n" +
                    "mobileKeyword.tap(copyLinkButton);\n" +
                    "String link = mobileKeyword.getClipboard();\n" +
                    "assert link.startsWith(\"https://\") : \"Invalid link format\";\n\n" +
                    "// Sao chép số điện thoại và xác minh định dạng\n" +
                    "mobileKeyword.tap(copyPhoneButton);\n" +
                    "String phone = mobileKeyword.getClipboard();\n" +
                    "assert phone.matches(\"\\\\d{10,}\") : \"Không phải định dạng số điện thoại hợp lệ\";",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Ứng dụng phải có quyền truy cập clipboard",
                    "Clipboard phải chứa nội dung văn bản (không phải hình ảnh hoặc dữ liệu nhị phân)"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển",
                    "UnsupportedOperationException: Nếu thiết bị không hỗ trợ truy cập clipboard",
                    "SecurityException: Nếu không có quyền truy cập clipboard"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "system", "clipboard", "copy", "paste", "text", "verification", "data"}
    )
    @Step("Lấy nội dung từ clipboard")
    public String getClipboard() {
        return execute(() -> ((HasClipboard) DriverManager.getDriver()).getClipboardText());
    }


    @NetatKeyword(
            name = "waitForText",
            description = "Chờ cho đến khi văn bản của một phần tử khớp chính xác với chuỗi mong đợi hoặc cho đến khi hết thời gian chờ. Hữu ích khi cần đảm bảo nội dung đã được cập nhật đúng trước khi tiếp tục. Phương thức này kiểm tra chính xác nội dung văn bản, phân biệt chữ hoa/thường và khoảng trắng. Nếu văn bản không khớp sau khi hết thời gian chờ, một TimeoutException sẽ được ném ra.",
            category = "Mobile/Wait",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra văn bản.",
                    "String: expectedText - Văn bản mong đợi phải khớp chính xác.",
                    "int: timeoutInSeconds - Thời gian tối đa (giây) để chờ văn bản khớp."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Chờ trạng thái đơn hàng cập nhật thành \"Đã hoàn thành\"\n" +
                    "mobileKeyword.waitForText(orderStatusLabel, \"Đã hoàn thành\", 15);\n\n" +
                    "// Chờ số dư tài khoản cập nhật sau khi thực hiện giao dịch\n" +
                    "mobileKeyword.tap(transferButton);\n" +
                    "mobileKeyword.waitForText(balanceAmount, \"1,250,000 VND\", 10);\n\n" +
                    "// Chờ thông báo xác nhận hiển thị đúng nội dung\n" +
                    "mobileKeyword.tap(submitButton);\n" +
                    "mobileKeyword.waitForText(confirmationMessage, \"Yêu cầu của bạn đã được gửi\", 5);\n\n" +
                    "// Chờ giá trị đếm ngược thay đổi\n" +
                    "mobileKeyword.tap(startCountdownButton);\n" +
                    "mobileKeyword.waitForText(countdownTimer, \"00:30\", 3);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần kiểm tra phải có thuộc tính văn bản (text)",
                    "Phần tử phải tồn tại trong DOM (có thể chưa hiển thị)"
            },
            exceptions = {
                    "TimeoutException: Nếu văn bản không khớp sau khi hết thời gian chờ",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM trong quá trình chờ",
                    "NoSuchElementException: Nếu không tìm thấy phần tử trong DOM"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "wait", "text", "content", "verification", "synchronization", "dynamic"}
    )
    @Step("Chờ văn bản của {0.name} là '{1}' trong {2} giây")
    public void waitForText(ObjectUI uiObject, String expectedText, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.textToBePresentInElement(findElement(uiObject), expectedText));
            return null;
        }, uiObject, expectedText, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "assertTextContains",
            description = "Khẳng định rằng văn bản của một phần tử có chứa một chuỗi con. Khác với assertTextEquals, phương thức này chỉ kiểm tra sự xuất hiện của chuỗi con trong văn bản, không yêu cầu khớp hoàn toàn. Hữu ích khi nội dung có thể thay đổi nhưng vẫn chứa các phần quan trọng cần kiểm tra. Phương thức phân biệt chữ hoa/thường.",
            category = "Mobile/Assertion",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra văn bản.",
                    "String: partialText - Chuỗi con cần tìm trong văn bản của phần tử."
            },
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thông báo chào mừng có chứa tên người dùng\n" +
                    "mobileKeyword.assertTextContains(welcomeMessage, \"Xin chào\");\n\n" +
                    "// Xác minh thông báo lỗi có chứa thông tin về mật khẩu\n" +
                    "mobileKeyword.assertTextContains(errorMessage, \"mật khẩu không đúng\");\n\n" +
                    "// Kiểm tra thông báo xác nhận có chứa mã đơn hàng\n" +
                    "mobileKeyword.assertTextContains(confirmationMessage, \"ĐH12345\");\n\n" +
                    "// Kiểm tra tiêu đề trang có chứa từ khóa tìm kiếm\n" +
                    "mobileKeyword.sendText(searchInput, \"điện thoại samsung\");\n" +
                    "mobileKeyword.tap(searchButton);\n" +
                    "mobileKeyword.assertTextContains(searchResultTitle, \"samsung\");\n\n" +
                    "// Kiểm tra thông báo lỗi chứa mã lỗi cụ thể\n" +
                    "mobileKeyword.assertTextContains(errorDialog, \"ERR-1234\");",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính văn bản (text)",
                    "Phần tử phải hiển thị trên màn hình để có thể đọc văn bản"
            },
            exceptions = {
                    "AssertionError: Nếu văn bản của phần tử không chứa chuỗi con mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "text", "contains", "verification", "substring", "validation"}
    )
    @Step("Kiểm tra (Hard) văn bản của {0.name} có chứa '{1}'")
    public void assertTextContains(ObjectUI uiObject, String partialText) {
        // Gọi phương thức logic từ lớp cha
        super.performTextContainsAssertion(uiObject, partialText, false);
    }

    @NetatKeyword(
            name = "assertNotChecked",
            description = "Khẳng định rằng một switch, checkbox hoặc radio button đang ở trạng thái không được chọn/tắt. Phương thức này kiểm tra thuộc tính 'checked' của phần tử và ném AssertionError nếu phần tử đang được chọn. Áp dụng cho các phần tử có thể chọn/bỏ chọn như checkbox, radio button, toggle switch. Lưu ý: Phần tử phải hỗ trợ thuộc tính 'checked', nếu không có thể gây ra lỗi.",
            category = "Mobile/Assertion",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra trạng thái."},
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra rằng tùy chọn chưa được chọn ban đầu\n" +
                    "mobileKeyword.assertNotChecked(optionalFeatureCheckbox);\n\n" +
                    "// Kiểm tra rằng công tắc đã được tắt sau khi tap\n" +
                    "mobileKeyword.tap(enabledSwitch); // Giả sử ban đầu đã bật\n" +
                    "mobileKeyword.assertNotChecked(enabledSwitch);\n\n" +
                    "// Xác minh rằng các radio button khác không được chọn\n" +
                    "mobileKeyword.tap(option1RadioButton);\n" +
                    "mobileKeyword.assertChecked(option1RadioButton);\n" +
                    "mobileKeyword.assertNotChecked(option2RadioButton);\n" +
                    "mobileKeyword.assertNotChecked(option3RadioButton);\n\n" +
                    "// Kiểm tra trạng thái mặc định của một tùy chọn nâng cao\n" +
                    "mobileKeyword.assertNotChecked(advancedFeaturesCheckbox);\n\n" +
                    "// Kiểm tra rằng các tùy chọn đã được bỏ chọn sau khi reset\n" +
                    "mobileKeyword.tap(resetButton);\n" +
                    "mobileKeyword.assertNotChecked(option1Checkbox);\n" +
                    "mobileKeyword.assertNotChecked(option2Checkbox);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính 'checked'",
                    "Phần tử phải là loại có thể chọn/bỏ chọn (checkbox, radio button, switch)"
            },
            exceptions = {
                    "AssertionError: Nếu phần tử đang ở trạng thái được chọn/bật",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu không thể lấy thuộc tính 'checked' của phần tử"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "checkbox", "radio", "switch", "toggle", "state", "unchecked", "off"}
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} đang không được chọn/tắt")
    public void assertNotChecked(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            boolean isChecked = Boolean.parseBoolean(element.getAttribute("checked"));
            Assert.assertFalse(isChecked, "HARD ASSERT FAILED: Phần tử '" + uiObject.getName() + "' đang ở trạng thái được chọn/bật.");
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertEnabled",
            description = "Khẳng định rằng một phần tử đang ở trạng thái có thể tương tác (enabled). Phương thức này kiểm tra thuộc tính 'enabled' của phần tử và ném AssertionError nếu phần tử bị vô hiệu hóa (disabled). Hữu ích khi cần đảm bảo một nút hoặc trường nhập liệu có thể tương tác được trước khi thực hiện các thao tác tiếp theo.",
            category = "Mobile/Assertion",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra trạng thái."},
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra rằng nút đăng nhập đã được kích hoạt sau khi nhập thông tin\n" +
                    "mobileKeyword.sendText(usernameInput, \"user@example.com\");\n" +
                    "mobileKeyword.sendText(passwordInput, \"password123\");\n" +
                    "mobileKeyword.assertEnabled(loginButton);\n\n" +
                    "// Xác minh rằng nút tiếp tục đã được kích hoạt sau khi hoàn thành biểu mẫu\n" +
                    "mobileKeyword.tap(agreeToTermsCheckbox);\n" +
                    "mobileKeyword.assertEnabled(continueButton);\n\n" +
                    "// Kiểm tra rằng trường nhập liệu đã được kích hoạt sau khi chọn tùy chọn\n" +
                    "mobileKeyword.tap(customAmountOption);\n" +
                    "mobileKeyword.assertEnabled(amountInput);\n\n" +
                    "// Kiểm tra rằng nút thanh toán được kích hoạt sau khi chọn phương thức thanh toán\n" +
                    "mobileKeyword.tap(creditCardOption);\n" +
                    "mobileKeyword.assertEnabled(payButton);\n\n" +
                    "// Kiểm tra rằng nút gửi được kích hoạt sau khi điền đủ thông tin bắt buộc\n" +
                    "mobileKeyword.sendText(nameField, \"Nguyễn Văn A\");\n" +
                    "mobileKeyword.sendText(phoneField, \"0912345678\");\n" +
                    "mobileKeyword.assertEnabled(submitButton);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính 'enabled'",
                    "Phần tử phải là loại có thể được kích hoạt/vô hiệu hóa (button, input, etc.)"
            },
            exceptions = {
                    "AssertionError: Nếu phần tử đang ở trạng thái bị vô hiệu hóa (disabled)",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu không thể lấy thuộc tính 'enabled' của phần tử"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "enabled", "interactive", "button", "input", "state", "active"}
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} là enabled")
    public void assertEnabled(ObjectUI uiObject) {
        // Gọi phương thức logic từ lớp cha
        super.performStateAssertion(uiObject, true, false);
    }

    @NetatKeyword(
            name = "assertElementNotPresent",
            description = "Khẳng định rằng một phần tử KHÔNG tồn tại trong cấu trúc màn hình sau một khoảng thời gian chờ. Hữu ích để xác minh rằng một phần tử đã bị xóa hoặc chưa được tạo. Phương thức sẽ đợi trong khoảng thời gian chỉ định và kiểm tra xem phần tử có xuất hiện không, nếu phần tử xuất hiện trong thời gian đó, một AssertionError sẽ được ném ra.",
            category = "Mobile/Assertion",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra sự không tồn tại.",
                    "int: timeoutInSeconds - Thời gian tối đa (giây) để đợi và xác nhận phần tử không xuất hiện."
            },
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra rằng thông báo lỗi không xuất hiện sau khi nhập đúng thông tin\n" +
                    "mobileKeyword.assertElementNotPresent(errorMessage, 3);\n\n" +
                    "// Xác minh rằng màn hình loading đã biến mất sau khi tải xong\n" +
                    "mobileKeyword.assertElementNotPresent(loadingSpinner, 10);\n\n" +
                    "// Kiểm tra rằng popup quảng cáo không xuất hiện sau khi đăng nhập\n" +
                    "mobileKeyword.tap(loginButton);\n" +
                    "mobileKeyword.assertElementNotPresent(advertisementPopup, 5);\n\n" +
                    "// Kiểm tra rằng thông báo hết hàng không xuất hiện\n" +
                    "mobileKeyword.tap(addToCartButton);\n" +
                    "mobileKeyword.assertElementNotPresent(outOfStockMessage, 2);\n\n" +
                    "// Kiểm tra rằng biểu tượng đang tải đã biến mất sau khi hoàn thành\n" +
                    "mobileKeyword.tap(refreshButton);\n" +
                    "mobileKeyword.assertElementNotPresent(loadingIcon, 8);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Locator của phần tử cần kiểm tra phải hợp lệ"
            },
            exceptions = {
                    "AssertionError: Nếu phần tử xuất hiện trong khoảng thời gian chờ",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "absence", "verification", "not present", "removed", "hidden", "deleted"}
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} không tồn tại trong {1} giây")
    public void assertElementNotPresent(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            boolean isPresent = isElementPresent(uiObject, timeoutInSeconds);
            Assert.assertFalse(isPresent,
                    "HARD ASSERT FAILED: Phần tử '" + uiObject.getName() + "' vẫn tồn tại sau " + timeoutInSeconds + " giây.");
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "isElementPresent",
            description = "Kiểm tra xem một phần tử có tồn tại trên màn hình hay không trong một khoảng thời gian chờ nhất định. Khác với các phương thức assertion, phương thức này trả về kết quả boolean (true/false) thay vì ném ra ngoại lệ, giúp xử lý các trường hợp phần tử có thể xuất hiện hoặc không. Hữu ích cho các điều kiện rẽ nhánh trong kịch bản test.",
            category = "Mobile/Assert",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần tìm kiếm.",
                    "int: timeoutInSeconds - Thời gian chờ tối đa (tính bằng giây)."
            },
            returnValue = "boolean: true nếu phần tử tồn tại, false nếu không tìm thấy sau thời gian chờ",
            example = "// Kiểm tra xem thông báo lỗi có xuất hiện không và xử lý tương ứng\n" +
                    "boolean isErrorVisible = mobileKeyword.isElementPresent(errorMessage, 5);\n" +
                    "if (isErrorVisible) {\n" +
                    "    // Xử lý lỗi\n" +
                    "    mobileKeyword.tap(dismissButton);\n" +
                    "} else {\n" +
                    "    // Tiếp tục luồng bình thường\n" +
                    "    mobileKeyword.tap(nextButton);\n" +
                    "}\n\n" +
                    "// Kiểm tra xem hướng dẫn có xuất hiện không (cho người dùng mới)\n" +
                    "if (mobileKeyword.isElementPresent(tutorialScreen, 3)) {\n" +
                    "    // Bỏ qua hướng dẫn\n" +
                    "    mobileKeyword.tap(skipButton);\n" +
                    "}\n\n" +
                    "// Kiểm tra điều kiện trước khi thực hiện hành động\n" +
                    "if (mobileKeyword.isElementPresent(saveButton, 2)) {\n" +
                    "    // Có thay đổi cần lưu\n" +
                    "    mobileKeyword.tap(saveButton);\n" +
                    "} else {\n" +
                    "    logger.info(\"Không có thay đổi cần lưu\");\n" +
                    "}\n\n" +
                    "// Xử lý các popup có thể xuất hiện hoặc không\n" +
                    "if (mobileKeyword.isElementPresent(rateAppPopup, 3)) {\n" +
                    "    mobileKeyword.tap(remindMeLaterButton);\n" +
                    "}\n\n" +
                    "// Kiểm tra trạng thái đăng nhập\n" +
                    "boolean isLoggedIn = mobileKeyword.isElementPresent(userProfileIcon, 2);\n" +
                    "if (!isLoggedIn) {\n" +
                    "    // Thực hiện đăng nhập\n" +
                    "    performLogin();\n" +
                    "}",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Locator của phần tử cần kiểm tra phải hợp lệ"
            },
            exceptions = {
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "verification", "presence", "condition", "branching", "boolean", "check", "exist"}
    )
    @Step("Kiểm tra sự tồn tại của phần tử {0.name} trong {1} giây")
    public boolean isElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        // Gọi cỗ máy execute() và bên trong gọi lại logic từ lớp cha
        return execute(() -> super._isElementPresent(uiObject, timeoutInSeconds), uiObject, timeoutInSeconds);
    }



    @NetatKeyword(
            name = "assertDisabled",
            description = "Khẳng định rằng một phần tử đang ở trạng thái không thể tương tác (disabled). Phương thức này kiểm tra thuộc tính 'enabled' của phần tử và ném AssertionError nếu phần tử đang được kích hoạt (enabled). Hữu ích khi cần đảm bảo một nút hoặc trường nhập liệu đã bị vô hiệu hóa trong các trường hợp nhất định.",
            category = "Mobile/Assertion",
            parameters = {"ObjectUI: uiObject - Phần tử cần kiểm tra trạng thái."},
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra rằng nút đăng nhập bị vô hiệu hóa khi chưa nhập thông tin\n" +
                    "mobileKeyword.assertDisabled(loginButton);\n\n" +
                    "// Xác minh rằng nút tiếp tục bị vô hiệu hóa khi chưa đồng ý điều khoản\n" +
                    "mobileKeyword.assertDisabled(continueButton);\n\n" +
                    "// Kiểm tra rằng trường nhập liệu số tiền bị vô hiệu hóa khi chọn số tiền cố định\n" +
                    "mobileKeyword.tap(fixedAmountOption);\n" +
                    "mobileKeyword.assertDisabled(amountInput);\n\n" +
                    "// Kiểm tra rằng nút xác nhận bị vô hiệu hóa khi thiếu thông tin bắt buộc\n" +
                    "mobileKeyword.assertDisabled(confirmButton);\n\n" +
                    "// Kiểm tra rằng các trường thông tin thẻ tín dụng bị vô hiệu hóa khi chọn phương thức thanh toán khác\n" +
                    "mobileKeyword.tap(paypalOption);\n" +
                    "mobileKeyword.assertDisabled(cardNumberInput);\n" +
                    "mobileKeyword.assertDisabled(expiryDateInput);\n\n" +
                    "// Kiểm tra rằng nút gửi bị vô hiệu hóa sau khi đã gửi thành công\n" +
                    "mobileKeyword.tap(submitButton);\n" +
                    "mobileKeyword.waitForVisible(successMessage, 5);\n" +
                    "mobileKeyword.assertDisabled(submitButton);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính 'enabled'",
                    "Phần tử phải là loại có thể được kích hoạt/vô hiệu hóa (button, input, etc.)"
            },
            exceptions = {
                    "AssertionError: Nếu phần tử đang ở trạng thái được kích hoạt (enabled)",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu không thể lấy thuộc tính 'enabled' của phần tử"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "disabled", "inactive", "button", "input", "state", "verification"}
    )
    @Step("Kiểm tra (Hard) phần tử {0.name} là disabled")
    public void assertDisabled(ObjectUI uiObject) {
        super.performStateAssertion(uiObject, false, false);
    }

    @NetatKeyword(
            name = "assertAttributeEquals",
            description = "Khẳng định rằng một thuộc tính của phần tử có giá trị chính xác như mong đợi. Hữu ích khi cần kiểm tra các thuộc tính đặc biệt như content-desc, resource-id, text, checked, v.v. Phương thức này so sánh chính xác giá trị thuộc tính, phân biệt chữ hoa/thường và khoảng trắng.",
            category = "Mobile/Assertion",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra thuộc tính.",
                    "String: attributeName - Tên thuộc tính cần kiểm tra (ví dụ: 'content-desc', 'text', 'resource-id').",
                    "String: expectedValue - Giá trị mong đợi của thuộc tính."
            },
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thuộc tính content-desc của nút\n" +
                    "mobileKeyword.assertAttributeEquals(menuButton, \"content-desc\", \"Menu chính\");\n\n" +
                    "// Xác minh resource-id của một phần tử\n" +
                    "mobileKeyword.assertAttributeEquals(loginButton, \"resource-id\", \"com.example.myapp:id/login_button\");\n\n" +
                    "// Kiểm tra thuộc tính text của nhãn\n" +
                    "mobileKeyword.assertAttributeEquals(statusLabel, \"text\", \"Hoàn thành\");\n\n" +
                    "// Kiểm tra thuộc tính checked của checkbox\n" +
                    "mobileKeyword.tap(rememberMeCheckbox);\n" +
                    "mobileKeyword.assertAttributeEquals(rememberMeCheckbox, \"checked\", \"true\");\n\n" +
                    "// Kiểm tra thuộc tính enabled của nút\n" +
                    "mobileKeyword.assertAttributeEquals(submitButton, \"enabled\", \"true\");\n\n" +
                    "// Kiểm tra thuộc tính selected của tab\n" +
                    "mobileKeyword.tap(profileTab);\n" +
                    "mobileKeyword.assertAttributeEquals(profileTab, \"selected\", \"true\");\n\n" +
                    "// Kiểm tra thuộc tính package để xác minh ứng dụng\n" +
                    "mobileKeyword.assertAttributeEquals(anyElement, \"package\", \"com.example.myapp\");",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần kiểm tra phải tồn tại",
                    "Thuộc tính cần kiểm tra phải tồn tại trên phần tử",
                    "Cần biết chính xác tên thuộc tính theo nền tảng (Android/iOS có thể khác nhau)"
            },
            exceptions = {
                    "AssertionError: Nếu giá trị thuộc tính không khớp với giá trị mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu không thể lấy thuộc tính của phần tử"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "attribute", "property", "exact match", "verification", "equality"}
    )
    @Step("Kiểm tra (Hard) thuộc tính '{1}' của {0.name} là '{2}'")
    public void assertAttributeEquals(ObjectUI uiObject, String attributeName, String expectedValue) {
        super.performAttributeAssertion(uiObject, attributeName, expectedValue, false);
    }

    @NetatKeyword(
            name = "assertAttributeContains",
            description = "Khẳng định rằng giá trị của một thuộc tính có chứa một chuỗi con. Khác với assertAttributeEquals, phương thức này chỉ kiểm tra sự xuất hiện của chuỗi con trong giá trị thuộc tính, không yêu cầu khớp hoàn toàn. Hữu ích khi giá trị thuộc tính có thể thay đổi nhưng vẫn chứa các phần quan trọng cần kiểm tra.",
            category = "Mobile/Assertion",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra thuộc tính.",
                    "String: attributeName - Tên thuộc tính cần kiểm tra.",
                    "String: partialValue - Chuỗi con cần tìm trong giá trị thuộc tính."
            },
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thuộc tính content-desc có chứa từ khóa\n" +
                    "mobileKeyword.assertAttributeContains(productItem, \"content-desc\", \"iPhone\");\n\n" +
                    "// Xác minh resource-id có chứa phần nhất định\n" +
                    "mobileKeyword.assertAttributeContains(anyElement, \"resource-id\", \"button_\");\n\n" +
                    "// Kiểm tra thuộc tính text có chứa thông tin quan trọng\n" +
                    "mobileKeyword.assertAttributeContains(orderSummary, \"text\", \"Tổng tiền\");\n\n" +
                    "// Kiểm tra thuộc tính class có chứa loại phần tử\n" +
                    "mobileKeyword.assertAttributeContains(inputField, \"class\", \"EditText\");\n\n" +
                    "// Kiểm tra thuộc tính package có chứa tên ứng dụng\n" +
                    "mobileKeyword.assertAttributeContains(anyElement, \"package\", \"com.example\");\n\n" +
                    "// Kiểm tra thuộc tính content-desc có chứa thông tin động\n" +
                    "String orderNumber = \"ORD-12345\";\n" +
                    "mobileKeyword.assertAttributeContains(orderDetails, \"content-desc\", orderNumber);\n\n" +
                    "// Kiểm tra thuộc tính text có chứa số tiền\n" +
                    "mobileKeyword.assertAttributeContains(totalAmount, \"text\", \"1,000,000\");",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần kiểm tra phải tồn tại",
                    "Thuộc tính cần kiểm tra phải tồn tại trên phần tử",
                    "Cần biết chính xác tên thuộc tính theo nền tảng (Android/iOS có thể khác nhau)"
            },
            exceptions = {
                    "AssertionError: Nếu giá trị thuộc tính không chứa chuỗi con mong đợi",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu không thể lấy thuộc tính của phần tử",
                    "NullPointerException: Nếu giá trị thuộc tính là null"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "attribute", "property", "substring", "contains", "partial match", "verification"}
    )
    @Step("Kiểm tra (Hard) thuộc tính '{1}' của {0.name} chứa '{2}'")
    public void assertAttributeContains(ObjectUI uiObject, String attributeName, String partialValue) {
        super.performAttributeContainsAssertion(uiObject, attributeName, partialValue, false);
    }

    @NetatKeyword(
            name = "assertElementCount",
            description = "Khẳng định rằng số lượng phần tử tìm thấy khớp với một con số mong đợi. Hữu ích khi cần kiểm tra số lượng các mục trong danh sách, số lượng tùy chọn, hoặc xác minh rằng một nhóm phần tử có số lượng chính xác. Phương thức này tìm tất cả các phần tử khớp với locator và so sánh số lượng với giá trị mong đợi.",
            category = "Mobile/Assertion",
            parameters = {
                    "ObjectUI: uiObject - Locator để tìm các phần tử.",
                    "int: expectedCount - Số lượng phần tử mong đợi."
            },
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra số lượng sản phẩm trong giỏ hàng\n" +
                    "mobileKeyword.assertElementCount(cartItems, 3);\n\n" +
                    "// Xác minh số lượng tùy chọn trong menu\n" +
                    "mobileKeyword.assertElementCount(menuOptions, 5);\n\n" +
                    "// Kiểm tra số lượng hình ảnh trong thư viện\n" +
                    "mobileKeyword.assertElementCount(galleryImages, 10);\n\n" +
                    "// Xác minh số lượng thông báo chưa đọc\n" +
                    "mobileKeyword.assertElementCount(unreadNotifications, 2);\n\n" +
                    "// Kiểm tra danh sách rỗng\n" +
                    "mobileKeyword.tap(clearAllButton);\n" +
                    "mobileKeyword.assertElementCount(listItems, 0);\n\n" +
                    "// Kiểm tra số lượng kết quả tìm kiếm\n" +
                    "mobileKeyword.sendText(searchInput, \"điện thoại\");\n" +
                    "mobileKeyword.tap(searchButton);\n" +
                    "mobileKeyword.assertElementCount(searchResults, 15);\n\n" +
                    "// Kiểm tra số lượng tab trong thanh điều hướng\n" +
                    "mobileKeyword.assertElementCount(navigationTabs, 4);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Locator của phần tử phải hợp lệ và có thể tìm thấy nhiều phần tử",
                    "Nếu mong đợi không tìm thấy phần tử nào (count = 0), locator vẫn phải hợp lệ"
            },
            exceptions = {
                    "AssertionError: Nếu số lượng phần tử tìm thấy không khớp với số lượng mong đợi",
                    "WebDriverException: Nếu có lỗi khi tương tác với trình điều khiển",
                    "InvalidSelectorException: Nếu locator không hợp lệ"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "count", "collection", "list", "quantity", "elements", "multiple", "verification"}
    )
    @Step("Kiểm tra (Hard) số lượng phần tử {0.name} là {1}")
    public void assertElementCount(ObjectUI uiObject, int expectedCount) {
        execute(() -> {
            List<WebElement> elements = findElements(uiObject);
            Assert.assertEquals(elements.size(), expectedCount,
                    "HARD ASSERT FAILED: Mong đợi tìm thấy " + expectedCount + " phần tử, nhưng thực tế tìm thấy " + elements.size() + ".");
            return null;
        }, uiObject, expectedCount);
    }


    @NetatKeyword(
            name = "assertTextWithOptions",
            description = "So sánh văn bản của phần tử với nhiều tùy chọn linh hoạt: có thể bỏ qua sự khác biệt giữa chữ hoa/thường và/hoặc cắt khoảng trắng ở đầu/cuối. Hữu ích khi cần kiểm tra nội dung mà không quan tâm đến định dạng chính xác. Nếu văn bản không khớp theo các tùy chọn đã chọn, một AssertionError sẽ được ném ra.",
            category = "Mobile/Assertion",
            parameters = {
                    "ObjectUI: uiObject - Phần tử cần kiểm tra văn bản.",
                    "String: expectedText - Chuỗi văn bản mong đợi.",
                    "boolean: ignoreCase - true để bỏ qua sự khác biệt giữa chữ hoa/thường, false để phân biệt.",
                    "boolean: trimText - true để cắt khoảng trắng ở đầu/cuối trước khi so sánh, false để giữ nguyên."
            },
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra nội dung chào mừng, bỏ qua chữ hoa/thường và khoảng trắng\n" +
                    "mobileKeyword.assertTextWithOptions(welcomeMessage, \"  xin chào, Người Dùng \", true, true);\n\n" +
                    "// Kiểm tra mã xác minh, chỉ bỏ qua khoảng trắng nhưng vẫn phân biệt chữ hoa/thường\n" +
                    "mobileKeyword.assertTextWithOptions(verificationCode, \" ABC123 \", false, true);\n\n" +
                    "// Kiểm tra thông báo lỗi, bỏ qua chữ hoa/thường nhưng giữ nguyên khoảng trắng\n" +
                    "mobileKeyword.assertTextWithOptions(errorMessage, \"Lỗi kết nối\", true, false);\n\n" +
                    "// Kiểm tra nội dung động có thể thay đổi định dạng\n" +
                    "String expectedName = \"NGUYỄN VĂN A\";\n" +
                    "mobileKeyword.assertTextWithOptions(userNameDisplay, expectedName, true, true);\n\n" +
                    "// Kiểm tra địa chỉ email không phân biệt chữ hoa/thường\n" +
                    "mobileKeyword.assertTextWithOptions(emailField, \"User@Example.com\", true, true);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính văn bản (text)",
                    "Phần tử phải hiển thị trên màn hình để có thể đọc văn bản"
            },
            exceptions = {
                    "AssertionError: Nếu văn bản không khớp theo các tùy chọn đã chọn",
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu không thể lấy văn bản của phần tử",
                    "NullPointerException: Nếu văn bản của phần tử là null và expectedText không phải null"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "text", "flexible", "ignoreCase", "trim", "comparison", "verification", "content"}
    )
    @Step("Kiểm tra văn bản của {0.name} là '{1}' (ignoreCase={2}, trim={3})")
    public void assertTextWithOptions(ObjectUI uiObject, String expectedText, boolean ignoreCase, boolean trimText) {
        execute(() -> {
            String actualText = getText(uiObject);
            String realExpectedText = expectedText;

            if (trimText) {
                actualText = (actualText != null) ? actualText.trim() : null;
                realExpectedText = (realExpectedText != null) ? realExpectedText.trim() : null;
            }

            if (ignoreCase) {
                boolean areEqual = (actualText == null && realExpectedText == null) ||
                        (actualText != null && actualText.equalsIgnoreCase(realExpectedText));
                Assert.assertTrue(areEqual, "HARD ASSERT FAILED: Văn bản không khớp (bỏ qua hoa/thường). Mong đợi: '" + realExpectedText + "', Thực tế: '" + actualText + "'");
            } else {
                Assert.assertEquals(actualText, realExpectedText, "HARD ASSERT FAILED: Văn bản không khớp.");
            }
            return null;
        }, uiObject, expectedText, ignoreCase, trimText);
    }

    @NetatKeyword(
            name = "getElementHeight",
            description = "Lấy và trả về chiều cao của một phần tử (tính bằng pixel). Hữu ích khi cần tính toán vị trí tương đối hoặc kiểm tra kích thước hiển thị của phần tử. Phương thức này trả về giá trị số nguyên đại diện cho chiều cao theo pixel của phần tử. Lưu ý: Phần tử phải hiển thị trên màn hình để có thể lấy được kích thước chính xác.",
            category = "Mobile/Getter",
            parameters = {"ObjectUI: uiObject - Phần tử cần lấy chiều cao."},
            returnValue = "int: Chiều cao của phần tử tính bằng pixel",
            example = "// Lấy chiều cao của một hình ảnh\n" +
                    "int imageHeight = mobileKeyword.getElementHeight(productImage);\n\n" +
                    "// Kiểm tra xem phần tử có kích thước đúng không\n" +
                    "int cardHeight = mobileKeyword.getElementHeight(cardElement);\n" +
                    "assert cardHeight > 200 : \"Card height is too small\";\n\n" +
                    "// Sử dụng chiều cao để tính toán vị trí cuộn\n" +
                    "int itemHeight = mobileKeyword.getElementHeight(listItem);\n" +
                    "int scrollDistance = itemHeight * 5; // Cuộn qua 5 mục\n\n" +
                    "// Tính toán vị trí tap dựa trên chiều cao\n" +
                    "int buttonHeight = mobileKeyword.getElementHeight(tallButton);\n" +
                    "Point location = tallButton.getLocation();\n" +
                    "mobileKeyword.tapByCoordinates(location.x + 10, location.y + buttonHeight / 2); // Tap ở giữa theo chiều cao\n\n" +
                    "// Kiểm tra tỷ lệ khung hình\n" +
                    "int height = mobileKeyword.getElementHeight(imageView);\n" +
                    "int width = mobileKeyword.getElementWidth(imageView);\n" +
                    "double ratio = (double) width / height;\n" +
                    "Assert.assertEquals(1.78, ratio, 0.1, \"Tỷ lệ khung hình phải là 16:9 (1.78)\");",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần đo phải tồn tại và hiển thị trên màn hình",
                    "Phần tử phải có kích thước xác định (không phải phần tử ẩn hoặc có kích thước 0)"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu không thể lấy kích thước của phần tử"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "getter", "dimension", "height", "size", "measurement", "pixel", "layout", "calculation"}
    )
    @Step("Lấy chiều cao của phần tử: {0.name}")
    public int getElementHeight(ObjectUI uiObject) {
        return execute(() -> findElement(uiObject).getSize().getHeight(), uiObject);
    }

    @NetatKeyword(
            name = "getElementWidth",
            description = "Lấy và trả về chiều rộng của một phần tử (tính bằng pixel). Hữu ích khi cần tính toán vị trí tương đối hoặc kiểm tra kích thước hiển thị của phần tử. Phương thức này trả về giá trị số nguyên đại diện cho chiều rộng theo pixel của phần tử. Lưu ý: Phần tử phải hiển thị trên màn hình để có thể lấy được kích thước chính xác.",
            category = "Mobile/Getter",
            parameters = {"ObjectUI: uiObject - Phần tử cần lấy chiều rộng."},
            returnValue = "int: Chiều rộng của phần tử tính bằng pixel",
            example = "// Lấy chiều rộng của một nút\n" +
                    "int buttonWidth = mobileKeyword.getElementWidth(submitButton);\n\n" +
                    "// Kiểm tra xem phần tử có chiều rộng đúng không\n" +
                    "int bannerWidth = mobileKeyword.getElementWidth(promotionBanner);\n" +
                    "assert bannerWidth == DriverManager.getDriver().manage().window().getSize().width : \"Banner should be full width\";\n\n" +
                    "// Tính toán vị trí tap dựa trên chiều rộng\n" +
                    "int progressBarWidth = mobileKeyword.getElementWidth(progressBar);\n" +
                    "Point location = progressBar.getLocation();\n" +
                    "mobileKeyword.tapByCoordinates(location.x + (int)(progressBarWidth * 0.7), location.y); // Tap at 70% of the progress bar\n\n" +
                    "// Kiểm tra độ rộng tương đối của các phần tử\n" +
                    "int containerWidth = mobileKeyword.getElementWidth(containerElement);\n" +
                    "int childWidth = mobileKeyword.getElementWidth(childElement);\n" +
                    "Assert.assertTrue(childWidth <= containerWidth, \"Phần tử con không được rộng hơn phần tử cha\");\n\n" +
                    "// Xác minh rằng nút đủ rộng để hiển thị văn bản\n" +
                    "int textWidth = mobileKeyword.getElementWidth(buttonText);\n" +
                    "int buttonWidth = mobileKeyword.getElementWidth(button);\n" +
                    "Assert.assertTrue(buttonWidth >= textWidth, \"Nút phải đủ rộng để hiển thị văn bản\");",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử cần đo phải tồn tại và hiển thị trên màn hình",
                    "Phần tử phải có kích thước xác định (không phải phần tử ẩn hoặc có kích thước 0)"
            },
            exceptions = {
                    "NoSuchElementException: Nếu không tìm thấy phần tử",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu không thể lấy kích thước của phần tử"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "getter", "dimension", "width", "size", "measurement", "pixel", "layout", "calculation"}
    )
    @Step("Lấy chiều rộng của phần tử: {0.name}")
    public int getElementWidth(ObjectUI uiObject) {
        return execute(() -> findElement(uiObject).getSize().getWidth(), uiObject);
    }

    @NetatKeyword(
            name = "pressKeyCode",
            description = "Mô phỏng hành động nhấn các phím vật lý của thiết bị Android như HOME, BACK, VOLUME_UP, v.v. Chỉ hoạt động trên Android, sẽ hiển thị cảnh báo nếu được gọi trên iOS. Tham số keyName phải là một giá trị hợp lệ từ enum AndroidKey. Hữu ích khi cần tương tác với các phím vật lý hoặc phím ảo của thiết bị.",
            category = "Mobile/System",
            parameters = {"String: keyName - Tên phím trong AndroidKey enum (ví dụ: 'HOME', 'BACK', 'VOLUME_UP')."},
            returnValue = "void: Không trả về giá trị",
            example = "// Nhấn nút Back để quay lại màn hình trước\n" +
                    "mobileKeyword.pressKeyCode(\"BACK\");\n\n" +
                    "// Tăng âm lượng thiết bị\n" +
                    "mobileKeyword.pressKeyCode(\"VOLUME_UP\");\n" +
                    "mobileKeyword.pressKeyCode(\"VOLUME_UP\");\n\n" +
                    "// Nhấn phím Home để quay về màn hình chính\n" +
                    "mobileKeyword.pressKeyCode(\"HOME\");\n\n" +
                    "// Mô phỏng nhấn phím Enter sau khi nhập văn bản\n" +
                    "mobileKeyword.sendText(searchInput, \"điện thoại samsung\");\n" +
                    "mobileKeyword.pressKeyCode(\"ENTER\");\n\n" +
                    "// Các phím số và ký tự đặc biệt\n" +
                    "mobileKeyword.pressKeyCode(\"0\");\n" +
                    "mobileKeyword.pressKeyCode(\"STAR\"); // Phím *\n" +
                    "mobileKeyword.pressKeyCode(\"POUND\"); // Phím #\n\n" +
                    "// Mô phỏng nhấn phím menu\n" +
                    "mobileKeyword.pressKeyCode(\"MENU\");\n\n" +
                    "// Mô phỏng nhấn phím camera\n" +
                    "mobileKeyword.pressKeyCode(\"CAMERA\");\n\n" +
                    "// Mô phỏng nhấn phím nguồn (thận trọng khi sử dụng)\n" +
                    "mobileKeyword.pressKeyCode(\"POWER\");",
            prerequisites = {
                    "Thiết bị Android đã được kết nối và cấu hình đúng với Appium",
                    "Đang sử dụng AndroidDriver (phương thức không hoạt động trên iOS)",
                    "Tham số keyName phải là một giá trị hợp lệ từ enum AndroidKey"
            },
            exceptions = {
                    "IllegalArgumentException: Nếu tên phím không hợp lệ",
                    "WebDriverException: Nếu không thể thực hiện hành động nhấn phím",
                    "UnsupportedCommandException: Nếu lệnh không được hỗ trợ trên thiết bị hiện tại"
            },
            platform = "MOBILE",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"mobile", "android", "key", "button", "physical", "system", "press", "input", "hardware"}
    )
    @Step("Nhấn phím hệ thống Android: {0}")
    public void pressKeyCode(String keyName) {
        execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            if (driver instanceof AndroidDriver) {
                // Chuyển đổi chuỗi thành hằng số enum AndroidKey
                AndroidKey key = AndroidKey.valueOf(keyName.toUpperCase());
                ((AndroidDriver) driver).pressKey(new KeyEvent(key));
            } else {
                logger.warn("Keyword 'pressKeyCode' chỉ được hỗ trợ trên Android.");
            }
            return null;
        }, keyName);
    }

    @NetatKeyword(
            name = "verifyOrientation",
            description = "Khẳng định rằng màn hình thiết bị đang ở hướng dọc (PORTRAIT) hoặc ngang (LANDSCAPE). Hữu ích để đảm bảo ứng dụng hiển thị đúng hướng trước khi thực hiện các thao tác tiếp theo. Phương thức hoạt động trên cả Android và iOS. Nếu hướng màn hình không khớp với giá trị mong đợi, một AssertionError sẽ được ném ra.",
            category = "Mobile/Assertion",
            parameters = {
                    "String: expectedOrientation - Hướng màn hình mong đợi, phải là 'PORTRAIT' hoặc 'LANDSCAPE' (không phân biệt hoa/thường)."
            },
            returnValue = "void: Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra rằng ứng dụng đang ở chế độ dọc trước khi tiếp tục\n" +
                    "mobileKeyword.verifyOrientation(\"PORTRAIT\");\n\n" +
                    "// Xoay thiết bị và xác minh hướng ngang\n" +
                    "// (Giả sử đã có phương thức để xoay thiết bị)\n" +
                    "rotateDevice();\n" +
                    "mobileKeyword.verifyOrientation(\"LANDSCAPE\");\n\n" +
                    "// Kiểm tra hướng màn hình trước khi chụp ảnh\n" +
                    "mobileKeyword.verifyOrientation(\"LANDSCAPE\");\n" +
                    "mobileKeyword.tap(captureButton);\n\n" +
                    "// Kiểm tra hướng màn hình sau khi xoay tự động\n" +
                    "mobileKeyword.tap(playVideoButton);\n" +
                    "mobileKeyword.waitForVisible(videoPlayer, 5);\n" +
                    "mobileKeyword.verifyOrientation(\"LANDSCAPE\");\n\n" +
                    "// Kiểm tra hướng màn hình sau khi quay lại từ ứng dụng khác\n" +
                    "mobileKeyword.pressKeyCode(\"HOME\");\n" +
                    "mobileKeyword.launchApp();\n" +
                    "mobileKeyword.verifyOrientation(\"PORTRAIT\");",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Đang sử dụng AndroidDriver hoặc IOSDriver",
                    "Tham số expectedOrientation phải là 'PORTRAIT' hoặc 'LANDSCAPE'"
            },
            exceptions = {
                    "AssertionError: Nếu hướng màn hình không khớp với giá trị mong đợi",
                    "IllegalArgumentException: Nếu giá trị expectedOrientation không hợp lệ",
                    "UnsupportedOperationException: Nếu loại driver không được hỗ trợ",
                    "WebDriverException: Nếu không thể lấy thông tin hướng màn hình"
            },
            platform = "MOBILE",
            systemImpact = "READ",
            stability = "STABLE",
            tags = {"mobile", "assertion", "orientation", "portrait", "landscape", "screen", "display", "rotation", "verification"}
    )
    @Step("Kiểm tra (Hard) hướng màn hình là: {0}")
    public void verifyOrientation(String expectedOrientation) {
        execute(() -> {
            ScreenOrientation expected = ScreenOrientation.valueOf(expectedOrientation.toUpperCase());

            ScreenOrientation actual;
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();

            // Sử dụng rotation để xác định orientation
            if (driver instanceof AndroidDriver) {
                actual = ((AndroidDriver) driver).getOrientation();
            } else if (driver instanceof IOSDriver) {
                actual = ((IOSDriver) driver).getOrientation();
            } else {
                throw new UnsupportedOperationException("Driver type not supported for orientation verification");
            }

            Assert.assertEquals(actual, expected, "HARD ASSERT FAILED: Hướng màn hình không khớp.");
            return null;
        }, expectedOrientation);
    }


// --- 3. KEYWORD TIỆN ÍCH CẤP CAO ---

    @NetatKeyword(
            name = "setSliderValue",
            description = "Thiết lập giá trị cho một thanh trượt (slider) bằng cách chạm vào vị trí tương ứng. Giá trị từ 0.0 (bên trái) đến 1.0 (bên phải). Phương thức này tự động tính toán tọa độ cần chạm dựa trên kích thước và vị trí của slider. Hữu ích khi cần điều chỉnh các điều khiển như âm lượng, độ sáng, hoặc các giá trị số trong khoảng. Lưu ý: Giá trị phải nằm trong khoảng từ 0.0 đến 1.0, nếu không sẽ gây ra ngoại lệ.",
            category = "Mobile/Interaction",
            parameters = {
                    "ObjectUI: uiObject - Phần tử slider cần điều chỉnh.",
                    "double: value - Giá trị cần thiết lập, từ 0.0 (nhỏ nhất/trái) đến 1.0 (lớn nhất/phải)."
            },
            returnValue = "void: Không trả về giá trị",
            example = "// Thiết lập thanh âm lượng ở mức 75%\n" +
                    "mobileKeyword.setSliderValue(volumeSlider, 0.75);\n\n" +
                    "// Thiết lập độ sáng màn hình ở mức thấp nhất\n" +
                    "mobileKeyword.setSliderValue(brightnessSlider, 0.0);\n\n" +
                    "// Thiết lập giá trị trung bình cho một bộ lọc\n" +
                    "mobileKeyword.setSliderValue(filterIntensitySlider, 0.5);\n\n" +
                    "// Thiết lập mức giá tối đa trong bộ lọc tìm kiếm\n" +
                    "mobileKeyword.setSliderValue(priceRangeSlider, 1.0);\n\n" +
                    "// Điều chỉnh thanh trượt đánh giá sao\n" +
                    "mobileKeyword.setSliderValue(ratingSlider, 0.8); // Đánh giá 4/5 sao\n\n" +
                    "// Thiết lập mức zoom cho camera\n" +
                    "mobileKeyword.setSliderValue(zoomSlider, 0.3);\n\n" +
                    "// Điều chỉnh tốc độ phát video\n" +
                    "mobileKeyword.setSliderValue(playbackSpeedSlider, 0.6);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Phần tử slider cần điều chỉnh phải tồn tại và hiển thị trên màn hình",
                    "Phần tử phải là loại có thể điều chỉnh bằng cách chạm vào vị trí khác nhau",
                    "Slider phải có chiều ngang đủ lớn để có thể chạm chính xác vào các vị trí khác nhau"
            },
            exceptions = {
                    "IllegalArgumentException: Nếu giá trị nằm ngoài khoảng từ 0.0 đến 1.0",
                    "NoSuchElementException: Nếu không tìm thấy phần tử slider",
                    "StaleElementReferenceException: Nếu phần tử không còn gắn với DOM",
                    "WebDriverException: Nếu không thể lấy vị trí hoặc kích thước của phần tử",
                    "ElementNotInteractableException: Nếu không thể tương tác với slider"
            },
            platform = "MOBILE",
            systemImpact = "WRITE",
            stability = "STABLE",
            tags = {"mobile", "interaction", "slider", "seekbar", "touch", "drag", "value", "adjustment", "range", "control"}
    )
    @Step("Thiết lập giá trị cho slider {0.name} thành {1}")
    public void setSliderValue(ObjectUI uiObject, double value) {
        execute(() -> {
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Giá trị của slider phải nằm trong khoảng từ 0.0 đến 1.0");
            }
            WebElement slider = findElement(uiObject);
            Point location = slider.getLocation();
            Dimension size = slider.getSize();

            // Tính toán tọa độ x cần chạm vào
            int tapX = location.getX() + (int) (size.getWidth() * value);
            // Giữ nguyên tọa độ y ở giữa
            int tapY = location.getY() + size.getHeight() / 2;

            // Tái sử dụng keyword tap theo tọa độ
            tapByCoordinates(tapX, tapY);
            return null;
        }, uiObject, value);
    }

    @NetatKeyword(
            name = "executeMobileCommand",
            description = "Thực thi một lệnh Appium tùy chỉnh không có sẵn trong các keyword tiêu chuẩn. Cung cấp sự linh hoạt tối đa cho các tình huống đặc thù hoặc các tính năng mới của Appium chưa được bao gồm trong framework. Phương thức này cho phép truyền các tham số phức tạp dưới dạng Map. Lưu ý: Cần hiểu rõ về lệnh Appium cụ thể trước khi sử dụng.",
            category = "Mobile/System",
            parameters = {
                    "String: commandName - Tên lệnh Appium cần thực thi (ví dụ: 'mobile: clearApp', 'mobile: shell').",
                    "Map<String, Object>: commandArgs - Các tham số của lệnh dưới dạng key-value."
            },
            returnValue = "Object: Kết quả trả về từ lệnh Appium, kiểu dữ liệu phụ thuộc vào lệnh được thực thi",
            example = "// Xóa dữ liệu của một ứng dụng trên Android mà không cần gỡ cài đặt\n" +
                    "Map<String, Object> args = new HashMap<>();\n" +
                    "args.put(\"appId\", \"com.example.app\");\n" +
                    "mobileKeyword.executeMobileCommand(\"mobile: clearApp\", args);\n\n" +
                    "// Thực hiện lệnh shell trên Android\n" +
                    "Map<String, Object> shellArgs = new HashMap<>();\n" +
                    "shellArgs.put(\"command\", \"dumpsys battery\");\n" +
                    "Object result = mobileKeyword.executeMobileCommand(\"mobile: shell\", shellArgs);\n" +
                    "System.out.println(\"Battery info: \" + result.toString());\n\n" +
                    "// Thực hiện vuốt đặc biệt trên iOS\n" +
                    "Map<String, Object> swipeArgs = new HashMap<>();\n" +
                    "swipeArgs.put(\"direction\", \"up\");\n" +
                    "swipeArgs.put(\"element\", findElement(scrollViewObject).getId());\n" +
                    "mobileKeyword.executeMobileCommand(\"mobile: swipe\", swipeArgs);\n\n" +
                    "// Lấy trạng thái mạng trên iOS\n" +
                    "Map<String, Object> networkArgs = new HashMap<>();\n" +
                    "Object networkStatus = mobileKeyword.executeMobileCommand(\"mobile: getConnectivity\", networkArgs);\n\n" +
                    "// Thực hiện thao tác biometrics (vân tay, Face ID)\n" +
                    "Map<String, Object> bioArgs = new HashMap<>();\n" +
                    "bioArgs.put(\"isEnabled\", true);\n" +
                    "mobileKeyword.executeMobileCommand(\"mobile: enrollBiometric\", bioArgs);\n\n" +
                    "// Thiết lập vị trí GPS giả lập\n" +
                    "Map<String, Object> locationArgs = new HashMap<>();\n" +
                    "locationArgs.put(\"latitude\", 10.762622);\n" +
                    "locationArgs.put(\"longitude\", 106.660172);\n" +
                    "mobileKeyword.executeMobileCommand(\"mobile: setLocation\", locationArgs);",
            prerequisites = {
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium",
                    "Đang sử dụng AppiumDriver (AndroidDriver hoặc IOSDriver)",
                    "Cần hiểu rõ về lệnh Appium cụ thể và các tham số của nó",
                    "Lệnh phải được hỗ trợ bởi phiên bản Appium và driver đang sử dụng",
                    "Một số lệnh có thể yêu cầu quyền đặc biệt hoặc cấu hình bổ sung"
            },
            exceptions = {
                    "WebDriverException: Nếu lệnh không được hỗ trợ hoặc không thể thực thi",
                    "InvalidArgumentException: Nếu tham số không đúng định dạng hoặc thiếu tham số bắt buộc",
                    "UnsupportedCommandException: Nếu lệnh không được hỗ trợ trên nền tảng hiện tại",
                    "SessionNotCreatedException: Nếu phiên Appium không còn hoạt động",
                    "NoSuchContextException: Nếu lệnh yêu cầu context không tồn tại"
            },
            platform = "MOBILE",
            systemImpact = "WRITE",
            stability = "EXPERIMENTAL",
            tags = {"mobile", "system", "advanced", "custom", "command", "script", "appium", "native", "extension", "flexible"}
    )
    @Step("Thực thi lệnh mobile: {0}")
    public Object executeMobileCommand(String commandName, Map<String, Object> commandArgs) {
        return execute(() -> {
            return ((AppiumDriver) DriverManager.getDriver()).executeScript(commandName, commandArgs);
        }, commandName, commandArgs);
    }
}