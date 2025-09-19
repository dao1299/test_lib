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
import java.util.stream.Collectors;


/**
 * Provides a set of platform keywords for interacting with and testing mobile applications.
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
    // --- 1. APPLICATION LIFECYCLE MANAGEMENT ---
    // =================================================================================

    @NetatKeyword(
            name = "installApp",
            description = "Cài đặt ứng dụng từ một đường dẫn file .apk (Android) hoặc .ipa (iOS) vào thiết bị đang kết nối. " +
                    "Đường dẫn phải trỏ đến một file hợp lệ và có thể truy cập từ máy thực thi test. " +
                    "Trên iOS, file .ipa phải được ký đúng cách để có thể cài đặt. " +
                    "Trên Android, thiết bị phải cho phép cài đặt từ nguồn không xác định.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {
                    "appPath: String - Đường dẫn tuyệt đối đến file ứng dụng (.apk hoặc .ipa)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Cài đặt ứng dụng Android từ thư mục local\n" +
                    "mobileKeyword.installApp(\"C:/apps/my-app.apk\");\n\n" +
                    "// Cài đặt ứng dụng iOS từ đường dẫn mạng (cần tải về trước)\n" +
                    "mobileKeyword.installApp(\"/tmp/downloaded-app.ipa\");\n\n" +
                    "// Cài đặt phiên bản mới của ứng dụng để kiểm tra tính năng cập nhật\n" +
                    "mobileKeyword.installApp(\"C:/builds/app-v2.0.apk\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Có quyền cài đặt ứng dụng trên thiết bị. Trên Android: Đã bật 'Cài đặt từ nguồn không xác định'. " +
                    "Trên iOS: File .ipa đã được ký đúng cách. " +
                    "Có thể throw WebDriverException nếu không thể cài đặt ứng dụng, " +
                    "hoặc FileNotFoundException nếu không tìm thấy file ứng dụng."
    )
    @Step("Install app from: {0}")
    public void installApp(String appPath) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).installApp(appPath);
            return null;
        }, appPath);
    }

    @NetatKeyword(
            name = "uninstallApp",
            description = "Gỡ cài đặt một ứng dụng khỏi thiết bị dựa trên định danh của ứng dụng. " +
                    "Trên Android, đây là package name (ví dụ: com.example.myapp). " +
                    "Trên iOS, đây là bundle ID (ví dụ: com.example.MyApp). " +
                    "Một số ứng dụng hệ thống không thể gỡ cài đặt ngay cả khi có quyền root/jailbreak.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {
                    "appId: String - AppPackage (Android) hoặc BundleID (iOS) của ứng dụng cần gỡ cài đặt"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Gỡ cài đặt ứng dụng Android\n" +
                    "mobileKeyword.uninstallApp(\"com.example.myapp\");\n\n" +
                    "// Gỡ cài đặt ứng dụng iOS\n" +
                    "mobileKeyword.uninstallApp(\"com.example.MyApp\");\n\n" +
                    "// Gỡ cài đặt để chuẩn bị cho test case cài đặt mới\n" +
                    "mobileKeyword.uninstallApp(\"com.banking.app\");\n" +
                    "mobileKeyword.installApp(\"C:/apps/banking-app.apk\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Có quyền gỡ cài đặt ứng dụng trên thiết bị. " +
                    "Có thể throw WebDriverException nếu không thể gỡ cài đặt ứng dụng, " +
                    "hoặc IllegalArgumentException nếu appId không hợp lệ."
    )
    @Step("Uninstall app: {0}")
    public void uninstallApp(String appId) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).removeApp(appId);
            return null;
        }, appId);
    }

    @NetatKeyword(
            name = "activateApp",
            description = "Đưa một ứng dụng đã được cài đặt lên foreground (màn hình chính). " +
                    "Hữu ích khi cần chuyển đổi giữa các ứng dụng hoặc kích hoạt lại ứng dụng đang chạy nền. " +
                    "Ứng dụng phải đã được cài đặt trên thiết bị, nếu không sẽ gây ra lỗi. " +
                    "Không giống như startActivity trên Android, phương thức này hoạt động trên cả Android và iOS với cùng một cú pháp.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {
                    "appId: String - AppPackage (Android) hoặc BundleID (iOS) của ứng dụng cần kích hoạt"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kích hoạt ứng dụng chính đang test\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\");\n\n" +
                    "// Chuyển sang ứng dụng cài đặt để thay đổi cấu hình thiết bị\n" +
                    "mobileKeyword.activateApp(\"com.android.settings\"); // Android\n" +
                    "// hoặc\n" +
                    "mobileKeyword.activateApp(\"com.apple.Preferences\"); // iOS\n\n" +
                    "// Quay lại ứng dụng chính sau khi thực hiện thao tác trên ứng dụng khác\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\");",
            note = "Áp dụng cho nền tảng Mobile. Ứng dụng đã được cài đặt trên thiết bị. " +
                    "Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Có thể throw WebDriverException nếu không thể kích hoạt ứng dụng, " +
                    "hoặc NoSuchAppException nếu ứng dụng không được cài đặt trên thiết bị."
    )
    @Step("Activate app: {0}")
    public void activateApp(String appId) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).activateApp(appId);
            return null;
        }, appId);
    }

    @NetatKeyword(
            name = "terminateApp",
            description = "Buộc dừng (kill) một tiến trình ứng dụng đang chạy. " +
                    "Khác với việc chỉ đưa ứng dụng về background, phương thức này thực sự kết thúc tiến trình của ứng dụng. " +
                    "Hữu ích khi cần kiểm tra khả năng khôi phục trạng thái của ứng dụng sau khi bị buộc dừng, " +
                    "hoặc để đảm bảo ứng dụng bắt đầu từ trạng thái sạch. " +
                    "Trả về true nếu ứng dụng đã được dừng thành công, false nếu ứng dụng không chạy.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {
                    "appId: String - AppPackage (Android) hoặc BundleID (iOS) của ứng dụng cần dừng"
            },
            returnValue = "boolean - True nếu ứng dụng đã được dừng thành công, false nếu ứng dụng không chạy",
            example = "// Dừng ứng dụng đang test\n" +
                    "mobileKeyword.terminateApp(\"com.example.myapp\");\n\n" +
                    "// Dừng ứng dụng và khởi động lại để kiểm tra tính năng khôi phục\n" +
                    "mobileKeyword.terminateApp(\"com.example.myapp\");\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\");\n\n" +
                    "// Kiểm tra xử lý lỗi khi ứng dụng bị crash\n" +
                    "mobileKeyword.tap(crashButton); // Gây ra crash\n" +
                    "mobileKeyword.terminateApp(\"com.example.myapp\"); // Đảm bảo ứng dụng đã dừng\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\"); // Khởi động lại",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Có thể throw WebDriverException nếu không thể dừng ứng dụng, " +
                    "hoặc IllegalArgumentException nếu appId không hợp lệ."
    )
    @Step("Terminate app: {0}")
    public void terminateApp(String appId) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).terminateApp(appId);
            return null;
        }, appId);
    }

    @NetatKeyword(
            name = "resetApp",
            description = "Reset ứng dụng về trạng thái ban đầu, tương đương với việc xóa dữ liệu ứng dụng. " +
                    "Phương thức này giúp đưa ứng dụng về trạng thái như mới cài đặt mà không cần gỡ và cài đặt lại. " +
                    "Phương thức này chỉ reset trạng thái đầu vào (input state) của ứng dụng, không phải toàn bộ dữ liệu. " +
                    "Để xóa hoàn toàn dữ liệu ứng dụng, nên sử dụng executeMobileCommand với 'mobile:clearApp' trên Android " +
                    "hoặc gỡ và cài đặt lại trên iOS.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Reset ứng dụng về trạng thái ban đầu trước mỗi test case\n" +
                    "mobileKeyword.resetApp();\n\n" +
                    "// Reset sau khi hoàn thành một luồng test để chuẩn bị cho luồng tiếp theo\n" +
                    "mobileKeyword.completeCheckout();\n" +
                    "mobileKeyword.resetApp();\n" +
                    "mobileKeyword.loginWithCredentials(username, password);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Ứng dụng đã được khởi động trước đó. " +
                    "Có thể throw WebDriverException nếu không thể reset ứng dụng."
    )
    @Step("Reset app")
    public void resetApp() {
        execute(() -> {
            // Note: resetApp is not part of InteractsWithApps,
            // it's still in AppiumDriver. So this logic is still correct.
            ((AppiumDriver) DriverManager.getDriver()).resetInputState();
            return null;
        });
    }

    @NetatKeyword(
            name = "backgroundApp",
            description = "Đưa ứng dụng hiện tại về chạy nền trong một khoảng thời gian xác định, sau đó tự động đưa lại lên foreground. " +
                    "Hữu ích để kiểm tra khả năng lưu trữ và khôi phục trạng thái của ứng dụng, " +
                    "hoặc để mô phỏng việc người dùng tạm thời chuyển sang ứng dụng khác. " +
                    "Nếu thời gian là -1, ứng dụng sẽ ở chế độ nền cho đến khi được kích hoạt lại bằng activateApp.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {
                    "seconds: int - Số giây ứng dụng chạy nền. Sử dụng -1 để giữ ứng dụng ở nền vô thời hạn"
            },
            returnValue = "void - Không trả về giá trị",
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
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Ứng dụng đang chạy ở foreground. " +
                    "Có thể throw WebDriverException nếu không thể đưa ứng dụng về background."
    )
    @Step("Background app for {0} seconds")
    public void backgroundApp(int seconds) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).runAppInBackground(Duration.ofSeconds(seconds));
            return null;
        }, seconds);
    }

// =================================================================================
// --- 2. BASIC ELEMENT INTERACTION ---
// =================================================================================

    @NetatKeyword(
            name = "tap",
            description = "Thực hiện một hành động chạm (tap) vào một phần tử trên màn hình. " +
                    "Đây là thao tác tương đương với click trên web nhưng được tối ưu cho thiết bị di động. " +
                    "Phương thức này sẽ đợi phần tử hiển thị và có thể tương tác trước khi thực hiện chạm.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần chạm vào"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chạm vào nút đăng nhập\n" +
                    "mobileKeyword.tap(loginButtonObject);\n\n" +
                    "// Chạm vào menu hamburger để mở navigation drawer\n" +
                    "mobileKeyword.tap(menuButton);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử UI cần tương tác phải hiển thị trên màn hình. " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "hoặc ElementNotInteractableException nếu phần tử không thể tương tác."
    )
    @Step("Tap element: {0.name}")
    public void tap(ObjectUI uiObject) {
        // Reuse parent class click logic, Appium will automatically interpret as 'tap'
        super.click(uiObject);
    }

    @NetatKeyword(
            name = "sendText",
            description = "Nhập văn bản vào một ô input có thể chỉnh sửa. " +
                    "Chỉ hoạt động với các phần tử có thuộc tính 'editable' là true như TextField, EditText, TextArea, v.v. " +
                    "Không thể sử dụng với các phần tử không cho phép nhập liệu như Button, Label.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng đầu vào có thể chỉnh sửa (như TextField, EditText)",
                    "text: String - Văn bản cần nhập vào phần tử"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Nhập tên đăng nhập vào ô username\n" +
                    "mobileKeyword.sendText(usernameInput, \"admin@example.com\");\n\n" +
                    "// Nhập mật khẩu vào ô password\n" +
                    "mobileKeyword.sendText(passwordInput, \"SecurePassword123\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử UI cần phải là trường nhập liệu có thể chỉnh sửa. " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "hoặc ElementNotInteractableException nếu phần tử không thể tương tác hoặc không phải trường nhập liệu."
    )
    @Step("Send text '{1}' to element: {0.name}")
    public void sendText(ObjectUI uiObject, String text) {
        super.sendKeys(uiObject, text);
    }

    @NetatKeyword(
            name = "clear",
            description = "Xóa văn bản trong một ô input có thể chỉnh sửa. " +
                    "Chỉ áp dụng cho các phần tử có thuộc tính 'editable' là true như TextField, EditText. " +
                    "Không hoạt động với các phần tử không phải là trường nhập liệu.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử input cần xóa văn bản"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Xóa văn bản trong ô tìm kiếm\n" +
                    "mobileKeyword.clear(searchInput);\n\n" +
                    "// Xóa nội dung trong ô email trước khi nhập giá trị mới\n" +
                    "mobileKeyword.clear(emailInput);\n" +
                    "mobileKeyword.sendText(emailInput, \"new.email@example.com\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử UI cần phải là trường nhập liệu có thể chỉnh sửa. " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "hoặc ElementNotInteractableException nếu phần tử không thể tương tác hoặc không phải trường nhập liệu."
    )
    @Step("Clear text in element: {0.name}")
    public void clear(ObjectUI uiObject) {
        super.clear(uiObject);
    }

    @NetatKeyword(
            name = "longPress",
            description = "Thực hiện hành động chạm và giữ (long press) vào một phần tử trong một khoảng thời gian xác định. " +
                    "Hữu ích cho các thao tác như hiển thị menu ngữ cảnh, kéo thả, hoặc các tương tác đặc biệt yêu cầu nhấn giữ. " +
                    "Phương thức sẽ đợi phần tử hiển thị trước khi thực hiện.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần chạm và giữ",
                    "durationInSeconds: int - Thời gian giữ phần tử, tính bằng giây"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chạm và giữ một hình ảnh trong 2 giây để hiển thị menu lưu ảnh\n" +
                    "mobileKeyword.longPress(imageObject, 2);\n\n" +
                    "// Chạm và giữ một mục trong danh sách để hiển thị menu xóa\n" +
                    "mobileKeyword.longPress(listItemObject, 1);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử UI cần tương tác phải hiển thị trên màn hình. " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "hoặc ElementNotInteractableException nếu phần tử không thể tương tác."
    )
    @Step("Long press element {0.name} for {1} seconds")
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
            description = "Ẩn bàn phím ảo nếu nó đang hiển thị trên màn hình. " +
                    "Hữu ích khi cần giải phóng không gian màn hình sau khi nhập liệu hoặc trước khi thực hiện các thao tác khác. " +
                    "Nếu bàn phím không hiển thị, phương thức này có thể gây ra lỗi trên một số thiết bị.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Ẩn bàn phím sau khi nhập văn bản\n" +
                    "mobileKeyword.sendText(searchInput, \"điện thoại\");\n" +
                    "mobileKeyword.hideKeyboard();",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Có thể throw WebDriverException nếu không thể ẩn bàn phím hoặc bàn phím không hiển thị."
    )
    @Step("Hide keyboard")
    public void hideKeyboard() {
        execute(() -> {
            ((HidesKeyboard) DriverManager.getDriver()).hideKeyboard();
            return null;
        });
    }

    @NetatKeyword(
            name = "pressBack",
            description = "Mô phỏng hành động nhấn nút 'Back' vật lý của thiết bị. " +
                    "Hữu ích để điều hướng ngược lại màn hình trước đó, đóng dialog, hoặc hủy thao tác hiện tại. " +
                    "Trên iOS, hành động này tương đương với việc nhấn nút quay lại ở góc trên bên trái của nhiều ứng dụng.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Quay lại màn hình trước\n" +
                    "mobileKeyword.pressBack();\n\n" +
                    "// Đóng dialog\n" +
                    "mobileKeyword.pressBack();",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Có thể throw WebDriverException nếu không thể thực hiện hành động Back."
    )
    @Step("Press Back button")
    public void pressBack() {
        execute(() -> {
            DriverManager.getDriver().navigate().back();
            return null;
        });
    }

// =================================================================================
// --- 3. SYNCHRONIZATION (WAITS) ---
// =================================================================================

    @NetatKeyword(
            name = "waitForVisible",
            description = "Chờ cho đến khi một phần tử hiển thị trên màn hình hoặc cho đến khi hết thời gian chờ. " +
                    "Phần tử được coi là hiển thị khi nó tồn tại trong DOM và có thể nhìn thấy được (visible). " +
                    "Hữu ích khi cần đảm bảo một phần tử đã xuất hiện trước khi tương tác với nó. " +
                    "Nếu phần tử không hiển thị sau khi hết thời gian chờ, một TimeoutException sẽ được ném ra.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần chờ hiển thị",
                    "timeoutInSeconds: int - Thời gian tối đa (giây) để chờ phần tử hiển thị"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ nút đăng nhập hiển thị\n" +
                    "mobileKeyword.waitForVisible(loginButton, 5);\n" +
                    "mobileKeyword.tap(loginButton);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Đã xác định chính xác phần tử UI cần chờ đợi. " +
                    "Có thể throw TimeoutException nếu phần tử không hiển thị trong thời gian chờ đợi, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc NoSuchElementException nếu không tìm thấy phần tử trong DOM."
    )
    @Step("Wait for element {0.name} to be visible within {1} seconds")
    public void waitForVisible(ObjectUI uiObject, int timeoutInSeconds) {
        super.waitForElementVisible(uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForNotVisible",
            description = "Chờ cho đến khi một phần tử biến mất khỏi màn hình hoặc cho đến khi hết thời gian chờ. " +
                    "Phần tử được coi là không hiển thị khi nó không tồn tại trong DOM hoặc không thể nhìn thấy được (invisible). " +
                    "Hữu ích khi cần đảm bảo một phần tử đã biến mất (như màn hình loading) trước khi tiếp tục. " +
                    "Nếu phần tử vẫn hiển thị sau khi hết thời gian chờ, một TimeoutException sẽ được ném ra.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần chờ biến mất",
                    "timeoutInSeconds: int - Thời gian tối đa (giây) để chờ phần tử biến mất"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ màn hình loading biến mất\n" +
                    "mobileKeyword.waitForNotVisible(loadingSpinner, 15);\n\n" +
                    "// Chờ dialog đóng\n" +
                    "mobileKeyword.waitForNotVisible(dialog, 3);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Đã xác định chính xác phần tử UI cần chờ đợi. " +
                    "Có thể throw TimeoutException nếu phần tử vẫn hiển thị sau khi hết thời gian chờ, " +
                    "hoặc StaleElementReferenceException nếu phần tử không còn gắn với DOM."
    )
    @Step("Wait for element {0.name} to disappear within {1} seconds")
    public void waitForNotVisible(ObjectUI uiObject, int timeoutInSeconds) {
        super.waitForElementNotVisible(uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForClickable",
            description = "Chờ cho đến khi một phần tử sẵn sàng để được chạm vào (clickable/tappable) hoặc cho đến khi hết thời gian chờ. " +
                    "Phần tử được coi là clickable khi nó hiển thị và có thể tương tác được (không bị disabled). " +
                    "Khác với waitForVisible, phương thức này còn kiểm tra khả năng tương tác của phần tử. " +
                    "Nếu phần tử không clickable sau khi hết thời gian chờ, một TimeoutException sẽ được ném ra.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần chờ sẵn sàng để tương tác",
                    "timeoutInSeconds: int - Thời gian tối đa (giây) để chờ phần tử có thể tương tác"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ nút có thể nhấn sau khi nhập thông tin\n" +
                    "mobileKeyword.waitForClickable(loginButton, 5);\n" +
                    "mobileKeyword.tap(loginButton);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Đã xác định chính xác phần tử UI cần chờ đợi. " +
                    "Có thể throw TimeoutException nếu phần tử không trở nên clickable trong thời gian chờ đợi, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc NoSuchElementException nếu không tìm thấy phần tử trong DOM."
    )
    @Step("Wait for element {0.name} to be clickable within {1} seconds")
    public void waitForClickable(ObjectUI uiObject, int timeoutInSeconds) {
        super.waitForElementClickable(uiObject, timeoutInSeconds);
    }

// =================================================================================
// --- 4. ASSERTIONS ---
// =================================================================================

    @NetatKeyword(
            name = "assertElementPresent",
            description = "Khẳng định rằng một phần tử tồn tại trong cấu trúc DOM của màn hình, không nhất thiết phải hiển thị. " +
                    "Phương thức này kiểm tra ngay lập tức (timeout = 0) và ném AssertionError nếu phần tử không tồn tại. " +
                    "Chỉ kiểm tra sự tồn tại, không kiểm tra tính hiển thị của phần tử.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra sự tồn tại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra nút đăng nhập tồn tại\n" +
                    "mobileKeyword.assertElementPresent(loginButton);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Đã xác định chính xác phần tử UI cần kiểm tra. " +
                    "Có thể throw AssertionError nếu phần tử không tồn tại trong DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình điều khiển."
    )
    @Step("Check (Hard) element existence: {0.name}")
    public void assertElementPresent(ObjectUI uiObject) {
        super.verifyElementPresent(uiObject, 0); // timeout 0 for immediate check
    }

    @NetatKeyword(
            name = "assertElementVisible",
            description = "Khẳng định rằng một phần tử đang được hiển thị trên màn hình và người dùng có thể nhìn thấy. " +
                    "Khác với assertElementPresent, phương thức này kiểm tra cả sự tồn tại và tính hiển thị của phần tử. " +
                    "Nếu phần tử không tồn tại hoặc không hiển thị, một AssertionError sẽ được ném ra.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra tính hiển thị"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra thông báo thành công hiển thị\n" +
                    "mobileKeyword.assertElementVisible(successMessage);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Đã xác định chính xác phần tử UI cần kiểm tra. " +
                    "Có thể throw AssertionError nếu phần tử không tồn tại hoặc không hiển thị, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình điều khiển, " +
                    "hoặc StaleElementReferenceException nếu phần tử không còn gắn với DOM."
    )
    @Step("Check (Hard) element {0.name} is visible")
    public void assertElementVisible(ObjectUI uiObject) {
        super.verifyElementVisibleHard(uiObject, true);
    }

    @NetatKeyword(
            name = "assertTextEquals",
            description = "Khẳng định rằng văn bản của một phần tử khớp chính xác với chuỗi mong đợi. " +
                    "Phương thức này trích xuất nội dung văn bản của phần tử và so sánh với giá trị mong đợi, ném AssertionError nếu không khớp. " +
                    "Hữu ích để kiểm tra nội dung văn bản, nhãn, thông báo lỗi hoặc các phần tử hiển thị khác.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra văn bản",
                    "expectedText: String - Chuỗi văn bản mong đợi để so sánh"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra tiêu đề màn hình\n" +
                    "mobileKeyword.assertTextEquals(title, \"Đăng nhập\");\n\n" +
                    "// Xác minh thông báo lỗi\n" +
                    "mobileKeyword.assertTextEquals(errorMessage, \"Email không hợp lệ\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử UI cần kiểm tra phải hiển thị và chứa văn bản. " +
                    "Có thể throw AssertionError nếu văn bản của phần tử không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "hoặc StaleElementReferenceException nếu phần tử không còn gắn với DOM."
    )
    @Step("Check (Hard) text of {0.name} equals '{1}'")
    public void assertTextEquals(ObjectUI uiObject, String expectedText) {
        super.verifyTextHard(uiObject, expectedText);
    }

    @NetatKeyword(
            name = "assertChecked",
            description = "Khẳng định rằng một switch, checkbox hoặc radio button đang ở trạng thái được chọn/bật. " +
                    "Phương thức này kiểm tra thuộc tính 'checked' của phần tử và ném AssertionError nếu phần tử không được chọn. " +
                    "Áp dụng cho các phần tử có thể chọn/bỏ chọn như checkbox, radio button, toggle switch. " +
                    "Phần tử phải hỗ trợ thuộc tính 'checked', nếu không có thể gây ra lỗi.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra trạng thái"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra checkbox đã được chọn\n" +
                    "mobileKeyword.tap(agreeToTermsCheckbox);\n" +
                    "mobileKeyword.assertChecked(agreeToTermsCheckbox);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử UI cần kiểm tra phải là loại có thể chọn/bỏ chọn và hỗ trợ thuộc tính 'checked'. " +
                    "Có thể throw AssertionError nếu phần tử không ở trạng thái được chọn/bật, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "hoặc WebDriverException nếu không thể truy cập thuộc tính 'checked'."
    )
    @Step("Check (Hard) element {0.name} is checked/enabled")
    public void assertChecked(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            // Appium uses 'checked' attribute for both Android and iOS
            boolean isChecked = Boolean.parseBoolean(element.getAttribute("checked"));
            Assert.assertTrue(isChecked, "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' is not in checked/enabled state.");
            return null;
        }, uiObject);
    }

    // --- Private Helper Methods ---
    private Point getCenterOfElement(Point location, Dimension size) {
        return new Point(location.getX() + size.getWidth() / 2, location.getY() + size.getHeight() / 2);
    }

    @NetatKeyword(
            name = "swipe",
            description = "Thực hiện hành động vuốt trên màn hình từ điểm bắt đầu đến điểm kết thúc. " +
                    "Cho phép kiểm soát chính xác tọa độ bắt đầu, kết thúc và tốc độ vuốt. " +
                    "Tọa độ được tính theo pixel từ góc trên bên trái của màn hình (0,0).",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "startX: int - Tọa độ X điểm bắt đầu vuốt",
                    "startY: int - Tọa độ Y điểm bắt đầu vuốt",
                    "endX: int - Tọa độ X điểm kết thúc vuốt",
                    "endY: int - Tọa độ Y điểm kết thúc vuốt",
                    "durationInMs: int - Thời gian thực hiện vuốt (ms), giá trị thấp hơn = vuốt nhanh hơn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Vuốt từ giữa màn hình xuống dưới\n" +
                    "mobileKeyword.swipe(500, 100, 500, 1500, 300);\n\n" +
                    "// Vuốt từ phải sang trái\n" +
                    "mobileKeyword.swipe(900, 500, 100, 500, 200);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Có thể throw WebDriverException nếu không thể thực hiện hành động vuốt, " +
                    "hoặc IllegalArgumentException nếu tọa độ nằm ngoài kích thước màn hình."
    )
    @Step("Swipe from ({0},{1}) to ({2},{3})")
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
            description = "Thực hiện hành động vuốt lên trên màn hình, tương đương với thao tác cuộn xuống để xem nội dung bên dưới. " +
                    "Phương thức này tự động tính toán các tọa độ dựa trên kích thước màn hình thiết bị.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "durationInMs: Integer... - (Tùy chọn) Thời gian thực hiện vuốt (ms). Mặc định là 500ms nếu không được chỉ định"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Vuốt lên với tốc độ mặc định\n" +
                    "mobileKeyword.swipeUp();\n\n" +
                    "// Vuốt lên với tốc độ chậm hơn\n" +
                    "mobileKeyword.swipeUp(1000);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Có thể throw WebDriverException nếu không thể thực hiện hành động vuốt."
    )
    @Step("Swipe up")
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
            description = "Thực hiện hành động vuốt xuống dưới màn hình, tương đương với thao tác cuộn lên để xem nội dung phía trên. " +
                    "Phương thức này tự động tính toán các tọa độ dựa trên kích thước màn hình thiết bị hiện tại.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "durationInMs: Integer... - (Tùy chọn) Thời gian thực hiện vuốt (ms). Mặc định là 500ms nếu không được chỉ định"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Vuốt xuống với tốc độ mặc định\n" +
                    "mobileKeyword.swipeDown();\n\n" +
                    "// Vuốt xuống với tốc độ chậm hơn\n" +
                    "mobileKeyword.swipeDown(800);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Có thể throw WebDriverException nếu không thể thực hiện hành động vuốt."
    )
    @Step("Swipe down")
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
            description = "Tự động cuộn màn hình (vuốt lên) cho đến khi tìm thấy một phần tử chứa văn bản mong muốn. " +
                    "Phương thức này sẽ thực hiện tối đa 10 lần vuốt lên để tìm kiếm. " +
                    "Cách hoạt động khác nhau giữa Android (sử dụng UiScrollable) và iOS (sử dụng vuốt tuần tự). " +
                    "Trả về WebElement nếu tìm thấy, hoặc ném NoSuchElementException nếu không tìm thấy sau khi đã cuộn hết.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "textToFind: String - Văn bản cần tìm kiếm trên màn hình. Có thể là toàn bộ hoặc một phần của văn bản hiển thị"
            },
            returnValue = "WebElement - Phần tử chứa văn bản được tìm thấy",
            example = "// Cuộn đến khi thấy nút và chạm vào nó\n" +
                    "WebElement registerButton = mobileKeyword.scrollToText(\"Đăng ký\");\n" +
                    "registerButton.click();",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Văn bản cần tìm phải tồn tại trên màn hình (có thể cần cuộn để hiển thị). " +
                    "Trên Android: Phần tử cha chứa nội dung cần cuộn phải có thuộc tính scrollable=true. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử chứa văn bản sau khi cuộn hết, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình điều khiển, " +
                    "hoặc IllegalStateException nếu không thể xác định nền tảng hoặc không hỗ trợ."
    )
    @Step("Scroll to text: {0}")
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

                throw new NoSuchElementException("Could not find element with text: " + textToFind + " after " + maxScrolls + " scrolls");
            }
        }, textToFind);
    }


    @NetatKeyword(
            name = "dragAndDrop",
            description = "Kéo một phần tử từ vị trí nguồn và thả vào vị trí của phần tử đích. " +
                    "Hữu ích cho các thao tác như sắp xếp lại danh sách, di chuyển các phần tử trong giao diện, hoặc kéo thả vào vùng đích. " +
                    "Phương thức sẽ tự động tính toán tọa độ trung tâm của cả hai phần tử để thực hiện thao tác chính xác.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "source: ObjectUI - Phần tử nguồn cần kéo",
                    "destination: ObjectUI - Phần tử đích để thả vào"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kéo một mục từ danh sách và thả vào thùng rác\n" +
                    "mobileKeyword.dragAndDrop(listItem, trashBin);\n\n" +
                    "// Sắp xếp lại thứ tự trong danh sách\n" +
                    "mobileKeyword.dragAndDrop(firstItem, thirdItem);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Cả phần tử nguồn và đích phải hiển thị trên màn hình và ứng dụng phải hỗ trợ thao tác kéo thả cho các phần tử này. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử nguồn hoặc đích, " +
                    "WebDriverException nếu có lỗi khi thực hiện thao tác kéo thả, " +
                    "hoặc ElementNotInteractableException nếu không thể tương tác với phần tử nguồn hoặc đích."
    )
    @Step("Drag and drop {0.name} to {1.name}")
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
//    @Step("Switch to context: {0}")
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
//    @Step("Switch to native context (NATIVE_APP)")
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
            description = "Tự động tìm và nhấn vào các nút hệ thống có văn bản khẳng định như 'Allow', 'OK', 'Accept', 'While using the app'. " +
                    "Hữu ích để xử lý các hộp thoại cấp quyền hoặc thông báo hệ thống. " +
                    "Phương thức sẽ tìm kiếm các nút phổ biến và nhấn vào nút đầu tiên tìm thấy. " +
                    "Nếu không tìm thấy nút nào, một cảnh báo sẽ được ghi vào log.",
            category = "Mobile",
            subCategory = "System",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Chấp nhận hộp thoại yêu cầu quyền truy cập vị trí\n" +
                    "mobileKeyword.tap(locationButton);\n" +
                    "mobileKeyword.acceptSystemDialog();\n\n" +
                    "// Chấp nhận thông báo cập nhật ứng dụng\n" +
                    "mobileKeyword.acceptSystemDialog();",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Hộp thoại hệ thống đang hiển thị trên màn hình và chứa ít nhất một nút có văn bản khẳng định được hỗ trợ. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình điều khiển, " +
                    "hoặc NoSuchElementException nếu không tìm thấy nút nào khớp với danh sách văn bản đã cho."
    )
    @Step("Accept system dialog")
    public void acceptSystemDialog() {
        execute(() -> {
            List<String> buttonTexts = Arrays.asList("Allow", "OK", "Accept", "While using the app");
            clickSystemButtonWithText(buttonTexts);
            return null;
        });
    }

    @NetatKeyword(
            name = "denySystemDialog",
            description = "Tự động tìm và nhấn vào các nút hệ thống có văn bản phủ định như 'Deny', 'Cancel', 'Don't allow'. " +
                    "Hữu ích để từ chối các yêu cầu cấp quyền hoặc đóng các thông báo hệ thống không mong muốn. " +
                    "Phương thức sẽ tìm kiếm các nút phổ biến và nhấn vào nút đầu tiên tìm thấy. " +
                    "Nếu không tìm thấy nút nào, một cảnh báo sẽ được ghi vào log.",
            category = "Mobile",
            subCategory = "System",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Từ chối yêu cầu quyền truy cập vị trí\n" +
                    "mobileKeyword.tap(locationFeatureButton);\n" +
                    "mobileKeyword.denySystemDialog();\n\n" +
                    "// Từ chối thông báo cập nhật ứng dụng\n" +
                    "mobileKeyword.denySystemDialog();",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Hộp thoại hệ thống đang hiển thị trên màn hình và chứa ít nhất một nút có văn bản phủ định được hỗ trợ. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình điều khiển, " +
                    "hoặc NoSuchElementException nếu không tìm thấy nút nào khớp với danh sách văn bản đã cho."
    )
    @Step("Deny system dialog")
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
                    locator = By.xpath("//*[(@text='" + text + "' or @text='" + text.toUpperCase() + "') or (@content-desc='" + text + "')]");
                } else { // iOS
                    locator = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (label == '" + text + "' OR label == '" + text.toUpperCase() + "')");
                }

                List<WebElement> elements = driver.findElements(locator);
                if (!elements.isEmpty()) {
                    elements.get(0).click();
                    logger.info("Tapped system button: '{}'", text);
                    return;
                }
            } catch (Exception e) {
                logger.debug("Unable to tap button '{}': {}", text, e.getMessage());
            }
        }
        logger.warn("No system buttons found for any of the provided texts: {}", possibleTexts);
    }

    @NetatKeyword(
            name = "tapByCoordinates",
            description = "Thực hiện một hành động chạm (tap) tại một tọa độ (x, y) cụ thể trên màn hình. " +
                    "Hữu ích khi cần tương tác với các phần tử không thể định vị bằng các locator thông thường, hoặc khi cần chạm vào một vị trí tương đối trên màn hình. " +
                    "Tọa độ được tính theo pixel từ góc trên bên trái của màn hình (0,0). " +
                    "Lưu ý: Tọa độ có thể khác nhau trên các thiết bị có kích thước màn hình khác nhau.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "x: int - Tọa độ theo trục ngang (pixel)",
                    "y: int - Tọa độ theo trục dọc (pixel)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chạm vào trung tâm màn hình\n" +
                    "Dimension size = DriverManager.getDriver().manage().window().getSize();\n" +
                    "mobileKeyword.tapByCoordinates(size.width / 2, size.height / 2);\n\n" +
                    "// Chạm vào góc trên bên phải để đóng quảng cáo\n" +
                    "mobileKeyword.tapByCoordinates(size.width - 50, 50);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Tọa độ cần nằm trong phạm vi kích thước màn hình của thiết bị. " +
                    "Có thể throw WebDriverException nếu có lỗi khi thực hiện hành động chạm, " +
                    "hoặc IllegalArgumentException nếu tọa độ nằm ngoài kích thước màn hình."
    )
    @Step("Tap at coordinates ({0}, {1})")
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
            description = "Thực hiện hành động chạm và giữ tại một tọa độ (x, y) trong một khoảng thời gian xác định. " +
                    "Hữu ích khi cần thực hiện các thao tác đặc biệt như hiển thị menu ngữ cảnh tại một vị trí cụ thể, hoặc khi tương tác với các phần tử không thể định vị bằng locator. " +
                    "Tọa độ được tính theo pixel từ góc trên bên trái của màn hình (0,0).",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "x: int - Tọa độ theo trục ngang (pixel)",
                    "y: int - Tọa độ theo trục dọc (pixel)",
                    "durationInSeconds: int - Thời gian giữ (tính bằng giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chạm và giữ tại trung tâm màn hình trong 2 giây\n" +
                    "Dimension size = DriverManager.getDriver().manage().window().getSize();\n" +
                    "mobileKeyword.longPressByCoordinates(size.width / 2, size.height / 2, 2);\n\n" +
                    "// Chạm và giữ tại một vị trí trên bản đồ để thả ghim\n" +
                    "mobileKeyword.longPressByCoordinates(450, 800, 1);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Tọa độ cần nằm trong phạm vi kích thước màn hình của thiết bị và ứng dụng phải hỗ trợ thao tác chạm và giữ tại vị trí đó. " +
                    "Có thể throw WebDriverException nếu có lỗi khi thực hiện hành động chạm và giữ, " +
                    "hoặc IllegalArgumentException nếu tọa độ nằm ngoài kích thước màn hình hoặc thời gian giữ không hợp lệ."
    )
    @Step("Long press at coordinates ({0}, {1}) for {2} seconds")
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
//            category = "Mobile",
//subCategory = "System",
//            parameters = {
//                    "double: latitude - Vĩ độ.",
//                    "double: longitude - Kinh độ."
//            },
//            example = "mobileKeyword.setGeoLocation(21.028511, 105.804817); // Tọa độ Hà Nội"
//    )
//    @Step("Set GPS location: Latitude={0}, Longitude={1}")
//    public void setGeoLocation(double latitude, double longitude) {
//        execute(() -> {
//            Location location = new Location(latitude, longitude, 0); // altitude có thể để là 0
//            ((AppiumDriver) DriverManager.getDriver()).setLocation(location);
//            logger.info("Đã thiết lập vị trí GPS thành: ({}, {})", latitude, longitude);
//            return null;
//        }, latitude, longitude);
//    }

//    @NetatKeyword(
//            name = "toggleAirplaneMode",
//            description = "Bật hoặc tắt chế độ máy bay trên thiết bị Android. " +
//                    "Mỗi lần gọi sẽ chuyển đổi trạng thái hiện tại (nếu đang bật sẽ tắt, nếu đang tắt sẽ bật). " +
//                    "Chỉ hoạt động trên Android, sẽ hiển thị cảnh báo nếu được gọi trên iOS. " +
//                    "Hữu ích khi cần kiểm tra hành vi ứng dụng trong điều kiện không có kết nối mạng.",
//            category = "Mobile",
//subCategory = "System",
//            parameters = {},
//            returnValue = "void - Không trả về giá trị",
//            example = "// Bật chế độ máy bay để kiểm tra xử lý offline\n" +
//                    "mobileKeyword.toggleAirplaneMode();\n" +
//                    "mobileKeyword.assertElementVisible(offlineMessage);\n\n" +
//                    "// Tắt chế độ máy bay để kiểm tra khả năng phục hồi kết nối\n" +
//                    "mobileKeyword.toggleAirplaneMode();\n" +
//                    "mobileKeyword.waitForVisible(syncCompleteIndicator, 15);",
//            note = "Áp dụng cho nền tảng Mobile (chỉ Android). Thiết bị Android đã được kết nối và cấu hình đúng với Appium. " +
//                    "Ứng dụng Appium phải có quyền thay đổi chế độ máy bay (cần quyền WRITE_SETTINGS) và thiết bị Android phải hỗ trợ chế độ máy bay. " +
//                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình điều khiển, " +
//                    "UnsupportedOperationException nếu được gọi trên thiết bị iOS, " +
//                    "hoặc SecurityException nếu không có đủ quyền để thay đổi chế độ máy bay."
//    )
//    @Step("Toggle airplane mode")
//    public void toggleAirplaneMode() {
//        execute(() -> {
//            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
//            if (driver instanceof AndroidDriver) {
//                AndroidDriver androidDriver = (AndroidDriver) driver;
//                try {
//                    // Kiểm tra trạng thái hiện tại
//                    ConnectionState currentState = androidDriver.getConnection();
//
//                    if (currentState.isAirplaneModeEnabled()) {
//                        // Tắt airplane mode
//                        logger.info("Tắt airplane mode");
//                        androidDriver.setConnection(new ConnectionStateBuilder()
//                                        .withAirplaneModeDisabled()
//                                .build());
//                    } else {
//                        // Bật airplane mode
//                        logger.info("Bật airplane mode");
//                        androidDriver.setConnection(new ConnectionStateBuilder()
//                                .withAirplaneModeEnabled()
//                                .build());
//                    }
//                } catch (Exception e) {
//                    logger.error("Lỗi khi toggle airplane mode: " + e.getMessage());
//                }
//            } else {
//                logger.warn("Airplane mode functionality is only supported on Android.");
//            }
//
//            return null;
//        });
//    }

    @NetatKeyword(
            name = "takeScreenshot",
            description = "Chụp ảnh màn hình của thiết bị và lưu vào thư mục screenshots với tên file được chỉ định. " +
                    "Hữu ích khi cần ghi lại trạng thái màn hình tại các điểm quan trọng trong quá trình test, đặc biệt là khi gặp lỗi hoặc cần xác minh giao diện. " +
                    "Ảnh chụp màn hình sẽ được lưu với định dạng .png và tự động đính kèm vào báo cáo Allure nếu được cấu hình.",
            category = "Mobile",
            subCategory = "System",
            parameters = {
                    "fileName: String - Tên file để lưu ảnh (không cần đuôi .png)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chụp màn hình tại các bước quan trọng\n" +
                    "mobileKeyword.takeScreenshot(\"login_screen\");\n" +
                    "mobileKeyword.tap(loginButton);\n" +
                    "mobileKeyword.takeScreenshot(\"after_login\");\n\n" +
                    "// Chụp màn hình khi gặp lỗi\n" +
                    "try {\n" +
                    "    mobileKeyword.tap(submitButton);\n" +
                    "} catch (Exception e) {\n" +
                    "    mobileKeyword.takeScreenshot(\"error_submit_form\");\n" +
                    "    throw e;\n" +
                    "}",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Thư mục screenshots phải tồn tại hoặc có quyền tạo và ứng dụng Appium phải có quyền chụp ảnh màn hình. " +
                    "Có thể throw WebDriverException nếu có lỗi khi chụp ảnh màn hình, " +
                    "IOException nếu không thể lưu ảnh vào thư mục chỉ định, " +
                    "hoặc IllegalArgumentException nếu tên file không hợp lệ."
    )
    @Step("Take screenshot with filename: {0}")
    public void takeScreenshot(String fileName) {
        // Tái sử dụng lại tiện ích chung đã có trong netat-core
        execute(() -> {
            ScreenshotUtils.takeScreenshot(fileName);
            return null;
        }, fileName);
    }

    @NetatKeyword(
            name = "pushFile",
            description = "Đẩy một file từ máy tính vào một đường dẫn trên thiết bị di động. " +
                    "Hữu ích khi cần chuẩn bị dữ liệu hoặc tài nguyên cho test case. " +
                    "Lưu ý: Đường dẫn trên thiết bị phải là đường dẫn mà ứng dụng có quyền ghi. " +
                    "Trên Android, thường là trong /sdcard/. Trên iOS, cần sử dụng bundle path.",
            category = "Mobile",
            subCategory = "Utility",
            parameters = {
                    "devicePath: String - Đường dẫn đích trên thiết bị di động",
                    "localFilePath: String - Đường dẫn tuyệt đối đến file trên máy tính chạy test"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Đẩy một hình ảnh vào thư mục Downloads của thiết bị Android\n" +
                    "mobileKeyword.pushFile(\"/sdcard/Download/avatar.png\", \"C:/test-data/images/avatar.png\");\n\n" +
                    "// Đẩy file CSV chứa dữ liệu test\n" +
                    "mobileKeyword.pushFile(\"/sdcard/TestData/users.csv\", \"src/test/resources/testdata/users.csv\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "File nguồn phải tồn tại trên máy chạy test và đường dẫn đích trên thiết bị phải có quyền ghi. " +
                    "Trên Android không root, chỉ có thể ghi vào thư mục shared như /sdcard/. Trên iOS, cần sử dụng đường dẫn bundle đúng cú pháp. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình điều khiển, " +
                    "IOException nếu không thể đọc file nguồn, IllegalArgumentException nếu đường dẫn không hợp lệ, " +
                    "hoặc SecurityException nếu không có quyền ghi vào đường dẫn đích."
    )
    @Step("Push file from '{1}' to device at '{0}'")
    public void pushFile(String devicePath, String localFilePath) {
        execute(() -> {
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(localFilePath));
                ((PushesFiles) DriverManager.getDriver()).pushFile(devicePath, fileContent);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read file from path: " + localFilePath, e);
            }
            return null;
        }, devicePath, localFilePath);
    }

    @NetatKeyword(
            name = "pullFile",
            description = "Kéo một file từ thiết bị về máy tính và trả về nội dung dưới dạng chuỗi (đã được giải mã Base64). " +
                    "Hữu ích khi cần lấy các file log, dữ liệu hoặc tài nguyên từ thiết bị để phân tích hoặc xác minh. " +
                    "Lưu ý: Đường dẫn phải trỏ đến một file có thể truy cập được từ ứng dụng (với quyền thích hợp). " +
                    "Trên Android không root, thường chỉ có thể truy cập các file trong thư mục ứng dụng.",
            category = "Mobile",
            subCategory = "Utility",
            parameters = {
                    "devicePath: String - Đường dẫn đến file trên thiết bị"
            },
            returnValue = "String - Nội dung của file được trả về dưới dạng chuỗi (đã giải mã Base64)",
            example = "// Lấy nội dung file log để kiểm tra\n" +
                    "String logContent = mobileKeyword.pullFile(\"/sdcard/Download/app.log\");\n" +
                    "assert logContent.contains(\"Transaction completed\");\n\n" +
                    "// Lấy file cấu hình để xác minh thiết lập\n" +
                    "String configContent = mobileKeyword.pullFile(\"/data/data/com.example.myapp/files/config.json\");\n" +
                    "JSONObject config = new JSONObject(configContent);\n" +
                    "assert config.getBoolean(\"darkMode\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "File cần lấy phải tồn tại trên thiết bị và ứng dụng phải có quyền đọc file đó. " +
                    "Trên Android không root, chỉ có thể đọc file từ thư mục ứng dụng hoặc thư mục shared. Trên iOS, cần sử dụng đường dẫn bundle đúng cú pháp. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình điều khiển, " +
                    "NoSuchFileException nếu file không tồn tại trên thiết bị, SecurityException nếu không có quyền đọc file, " +
                    "hoặc IllegalArgumentException nếu đường dẫn không hợp lệ."
    )
    @Step("Pull file from device at: {0}")
    public String pullFile(String devicePath) {
        return execute(() -> {
            byte[] fileBase64 = ((PullsFiles) DriverManager.getDriver()).pullFile(devicePath);
            return new String(Base64.getDecoder().decode(fileBase64));
        }, devicePath);
    }

    @NetatKeyword(
            name = "getClipboard",
            description = "Lấy và trả về nội dung văn bản hiện tại của clipboard trên thiết bị. " +
                    "Hữu ích khi cần kiểm tra nội dung đã được sao chép hoặc khi cần lấy dữ liệu từ clipboard để sử dụng trong các bước test tiếp theo. " +
                    "Phương thức này trả về một chuỗi chứa nội dung văn bản của clipboard. " +
                    "Lưu ý: Chỉ hỗ trợ nội dung văn bản, không hỗ trợ các loại dữ liệu khác như hình ảnh.",
            category = "Mobile",
            subCategory = "System",
            parameters = {},
            returnValue = "String - Nội dung văn bản hiện có trong clipboard của thiết bị",
            example = "// Kiểm tra nội dung đã được sao chép đúng\n" +
                    "mobileKeyword.longPress(emailText, 2);\n" +
                    "mobileKeyword.tap(copyOption);\n" +
                    "String copiedText = mobileKeyword.getClipboard();\n" +
                    "assert copiedText.equals(\"user@example.com\");\n\n" +
                    "// Sao chép mã xác minh và sử dụng nó\n" +
                    "mobileKeyword.tap(copyButton);\n" +
                    "String code = mobileKeyword.getClipboard();\n" +
                    "mobileKeyword.sendText(codeInput, code);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Ứng dụng phải có quyền truy cập clipboard và clipboard phải chứa nội dung văn bản (không phải hình ảnh hoặc dữ liệu nhị phân). " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình điều khiển, " +
                    "UnsupportedOperationException nếu thiết bị không hỗ trợ truy cập clipboard, " +
                    "hoặc SecurityException nếu không có quyền truy cập clipboard."
    )
    @Step("Get clipboard content")
    public String getClipboard() {
        return execute(() -> ((HasClipboard) DriverManager.getDriver()).getClipboardText());
    }

    @NetatKeyword(
            name = "waitForText",
            description = "Chờ cho đến khi văn bản của một phần tử khớp chính xác với chuỗi mong đợi hoặc cho đến khi hết thời gian chờ. " +
                    "Hữu ích khi cần đảm bảo nội dung đã được cập nhật đúng trước khi tiếp tục. " +
                    "Phương thức này kiểm tra chính xác nội dung văn bản, phân biệt chữ hoa/thường và khoảng trắng. " +
                    "Nếu văn bản không khớp sau khi hết thời gian chờ, một TimeoutException sẽ được ném ra.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra văn bản",
                    "expectedText: String - Văn bản mong đợi phải khớp chính xác",
                    "timeoutInSeconds: int - Thời gian tối đa (giây) để chờ văn bản khớp"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Chờ trạng thái đơn hàng cập nhật\n" +
                    "mobileKeyword.waitForText(orderStatusLabel, \"Đã hoàn thành\", 15);\n\n" +
                    "// Chờ số dư tài khoản cập nhật sau giao dịch\n" +
                    "mobileKeyword.tap(transferButton);\n" +
                    "mobileKeyword.waitForText(balanceAmount, \"1,250,000 VND\", 10);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải có thuộc tính văn bản (text) và phải tồn tại trong DOM (có thể chưa hiển thị). " +
                    "Có thể throw TimeoutException nếu văn bản không khớp sau khi hết thời gian chờ, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM trong quá trình chờ, " +
                    "hoặc NoSuchElementException nếu không tìm thấy phần tử trong DOM."
    )
    @Step("Wait for text of {0.name} to be '{1}' within {2} seconds")
    public void waitForText(ObjectUI uiObject, String expectedText, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.textToBePresentInElement(findElement(uiObject), expectedText));
            return null;
        }, uiObject, expectedText, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "assertTextContains",
            description = "Khẳng định rằng văn bản của một phần tử có chứa một chuỗi con. " +
                    "Khác với assertTextEquals, phương thức này chỉ kiểm tra sự xuất hiện của chuỗi con trong văn bản, không yêu cầu khớp hoàn toàn. " +
                    "Hữu ích khi nội dung có thể thay đổi nhưng vẫn chứa các phần quan trọng cần kiểm tra. " +
                    "Phương thức phân biệt chữ hoa/thường.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra văn bản",
                    "partialText: String - Chuỗi con cần tìm trong văn bản của phần tử"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thông báo chào mừng có chứa tên người dùng\n" +
                    "mobileKeyword.assertTextContains(welcomeMessage, \"Xin chào\");\n\n" +
                    "// Xác minh thông báo lỗi có chứa thông tin về mật khẩu\n" +
                    "mobileKeyword.assertTextContains(errorMessage, \"mật khẩu không đúng\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính văn bản (text), và phải hiển thị trên màn hình để có thể đọc văn bản. " +
                    "Có thể throw AssertionError nếu văn bản của phần tử không chứa chuỗi con mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "hoặc StaleElementReferenceException nếu phần tử không còn gắn với DOM."
    )
    @Step("Check (Hard) if text of {0.name} contains '{1}'")
    public void assertTextContains(ObjectUI uiObject, String partialText) {
        // Gọi phương thức logic từ lớp cha
        super.performTextContainsAssertion(uiObject, partialText, false);
    }

    @NetatKeyword(
            name = "assertNotChecked",
            description = "Khẳng định rằng một switch, checkbox hoặc radio button đang ở trạng thái không được chọn/tắt. " +
                    "Phương thức này kiểm tra thuộc tính 'checked' của phần tử và ném AssertionError nếu phần tử đang được chọn. " +
                    "Áp dụng cho các phần tử có thể chọn/bỏ chọn như checkbox, radio button, toggle switch. " +
                    "Lưu ý: Phần tử phải hỗ trợ thuộc tính 'checked', nếu không có thể gây ra lỗi.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra trạng thái"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra rằng tùy chọn chưa được chọn ban đầu\n" +
                    "mobileKeyword.assertNotChecked(optionalFeatureCheckbox);\n\n" +
                    "// Xác minh các radio button khác không được chọn\n" +
                    "mobileKeyword.tap(option1RadioButton);\n" +
                    "mobileKeyword.assertChecked(option1RadioButton);\n" +
                    "mobileKeyword.assertNotChecked(option2RadioButton);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính 'checked', và phải là loại có thể chọn/bỏ chọn (checkbox, radio button, switch). " +
                    "Có thể throw AssertionError nếu phần tử đang ở trạng thái được chọn/bật, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu không thể lấy thuộc tính 'checked' của phần tử."
    )
    @Step("Check (Hard) if element {0.name} is not checked/enabled")
    public void assertNotChecked(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            boolean isChecked = Boolean.parseBoolean(element.getAttribute("checked"));
            Assert.assertFalse(isChecked, "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' is in checked/enabled state.");
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertEnabled",
            description = "Khẳng định rằng một phần tử đang ở trạng thái có thể tương tác (enabled). " +
                    "Phương thức này kiểm tra thuộc tính 'enabled' của phần tử và ném AssertionError nếu phần tử bị vô hiệu hóa (disabled). " +
                    "Hữu ích khi cần đảm bảo một nút hoặc trường nhập liệu có thể tương tác được trước khi thực hiện các thao tác tiếp theo.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra trạng thái"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra nút đăng nhập được kích hoạt sau khi nhập thông tin\n" +
                    "mobileKeyword.sendText(usernameInput, \"user@example.com\");\n" +
                    "mobileKeyword.sendText(passwordInput, \"password123\");\n" +
                    "mobileKeyword.assertEnabled(loginButton);\n\n" +
                    "// Xác minh nút tiếp tục được kích hoạt sau khi đồng ý điều khoản\n" +
                    "mobileKeyword.tap(agreeToTermsCheckbox);\n" +
                    "mobileKeyword.assertEnabled(continueButton);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính 'enabled', và phải là loại có thể được kích hoạt/vô hiệu hóa (button, input, etc.). " +
                    "Có thể throw AssertionError nếu phần tử đang ở trạng thái bị vô hiệu hóa (disabled), " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu không thể lấy thuộc tính 'enabled' của phần tử."
    )
    @Step("Check (Hard) if element {0.name} is enabled")
    public void assertEnabled(ObjectUI uiObject) {
        // Gọi phương thức logic từ lớp cha
        super.performStateAssertion(uiObject, true, false);
    }

    @NetatKeyword(
            name = "assertElementNotPresent",
            description = "Khẳng định rằng một phần tử KHÔNG tồn tại trong cấu trúc màn hình sau một khoảng thời gian chờ. " +
                    "Hữu ích để xác minh rằng một phần tử đã bị xóa hoặc chưa được tạo. " +
                    "Phương thức sẽ đợi trong khoảng thời gian chỉ định và kiểm tra xem phần tử có xuất hiện không, " +
                    "nếu phần tử xuất hiện trong thời gian đó, một AssertionError sẽ được ném ra.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra sự không tồn tại",
                    "timeoutInSeconds: int - Thời gian tối đa (giây) để đợi và xác nhận phần tử không xuất hiện"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thông báo lỗi không xuất hiện sau khi nhập đúng thông tin\n" +
                    "mobileKeyword.assertElementNotPresent(errorMessage, 3);\n\n" +
                    "// Xác minh màn hình loading đã biến mất sau khi tải xong\n" +
                    "mobileKeyword.assertElementNotPresent(loadingSpinner, 10);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Locator của phần tử cần kiểm tra phải hợp lệ. " +
                    "Có thể throw AssertionError nếu phần tử xuất hiện trong khoảng thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình điều khiển."
    )
    @Step("Check (Hard) if element {0.name} is not present within {1} seconds")
    public void assertElementNotPresent(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            boolean isPresent = isElementPresent(uiObject, timeoutInSeconds);
            Assert.assertFalse(isPresent,
                    "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' is still present after " + timeoutInSeconds + " seconds.");
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "isElementPresent",
            description = "Kiểm tra xem một phần tử có tồn tại trên màn hình hay không trong một khoảng thời gian chờ nhất định. " +
                    "Khác với các phương thức assertion, phương thức này trả về kết quả boolean (true/false) thay vì ném ra ngoại lệ, " +
                    "giúp xử lý các trường hợp phần tử có thể xuất hiện hoặc không. " +
                    "Hữu ích cho các điều kiện rẽ nhánh trong kịch bản test.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần tìm kiếm",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng giây)"
            },
            returnValue = "boolean - true nếu phần tử tồn tại, false nếu không tìm thấy sau thời gian chờ",
            example = "// Kiểm tra thông báo lỗi và xử lý tương ứng\n" +
                    "boolean isErrorVisible = mobileKeyword.isElementPresent(errorMessage, 5);\n" +
                    "if (isErrorVisible) {\n" +
                    "    mobileKeyword.tap(dismissButton);\n" +
                    "} else {\n" +
                    "    mobileKeyword.tap(nextButton);\n" +
                    "}\n\n" +
                    "// Kiểm tra popup và bỏ qua nếu có\n" +
                    "if (mobileKeyword.isElementPresent(rateAppPopup, 3)) {\n" +
                    "    mobileKeyword.tap(remindMeLaterButton);\n" +
                    "}",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Locator của phần tử cần kiểm tra phải hợp lệ. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình điều khiển."
    )
    @Step("Check if element {0.name} exists within {1} seconds")
    public boolean isElementPresent(ObjectUI uiObject, int timeoutInSeconds) {
        // Gọi cỗ máy execute() và bên trong gọi lại logic từ lớp cha
        return execute(() -> super._isElementPresent(uiObject, timeoutInSeconds), uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "assertDisabled",
            description = "Khẳng định rằng một phần tử đang ở trạng thái không thể tương tác (disabled). " +
                    "Phương thức này kiểm tra thuộc tính 'enabled' của phần tử và ném AssertionError nếu phần tử đang được kích hoạt (enabled). " +
                    "Hữu ích khi cần đảm bảo một nút hoặc trường nhập liệu đã bị vô hiệu hóa trong các trường hợp nhất định.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra trạng thái"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra nút đăng nhập bị vô hiệu hóa khi chưa nhập thông tin\n" +
                    "mobileKeyword.assertDisabled(loginButton);\n\n" +
                    "// Xác minh trường nhập số tiền bị vô hiệu hóa khi chọn số tiền cố định\n" +
                    "mobileKeyword.tap(fixedAmountOption);\n" +
                    "mobileKeyword.assertDisabled(amountInput);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính 'enabled', và phải là loại có thể được kích hoạt/vô hiệu hóa (button, input, etc.). " +
                    "Có thể throw AssertionError nếu phần tử đang ở trạng thái được kích hoạt (enabled), " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu không thể lấy thuộc tính 'enabled' của phần tử."
    )
    @Step("Check (Hard) if element {0.name} is disabled")
    public void assertDisabled(ObjectUI uiObject) {
        super.performStateAssertion(uiObject, false, false);
    }

    @NetatKeyword(
            name = "assertAttributeEquals",
            description = "Khẳng định rằng một thuộc tính của phần tử có giá trị chính xác như mong đợi. " +
                    "Hữu ích khi cần kiểm tra các thuộc tính đặc biệt như content-desc, resource-id, text, checked, v.v. " +
                    "Phương thức này so sánh chính xác giá trị thuộc tính, phân biệt chữ hoa/thường và khoảng trắng.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra thuộc tính",
                    "attributeName: String - Tên thuộc tính cần kiểm tra (ví dụ: 'content-desc', 'text', 'resource-id')",
                    "expectedValue: String - Giá trị mong đợi của thuộc tính"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thuộc tính content-desc của nút\n" +
                    "mobileKeyword.assertAttributeEquals(menuButton, \"content-desc\", \"Menu chính\");\n\n" +
                    "// Xác minh resource-id của một phần tử\n" +
                    "mobileKeyword.assertAttributeEquals(loginButton, \"resource-id\", \"com.example.myapp:id/login_button\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại, thuộc tính cần kiểm tra phải tồn tại trên phần tử, " +
                    "và cần biết chính xác tên thuộc tính theo nền tảng (Android/iOS có thể khác nhau). " +
                    "Có thể throw AssertionError nếu giá trị thuộc tính không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu không thể lấy thuộc tính của phần tử."
    )
    @Step("Check (Hard) attribute '{1}' of {0.name} is '{2}'")
    public void assertAttributeEquals(ObjectUI uiObject, String attributeName, String expectedValue) {
        super.performAttributeAssertion(uiObject, attributeName, expectedValue, false);
    }

    @NetatKeyword(
            name = "assertAttributeContains",
            description = "Khẳng định rằng giá trị của một thuộc tính có chứa một chuỗi con. " +
                    "Khác với assertAttributeEquals, phương thức này chỉ kiểm tra sự xuất hiện của chuỗi con trong giá trị thuộc tính, không yêu cầu khớp hoàn toàn. " +
                    "Hữu ích khi giá trị thuộc tính có thể thay đổi nhưng vẫn chứa các phần quan trọng cần kiểm tra.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra thuộc tính",
                    "attributeName: String - Tên thuộc tính cần kiểm tra",
                    "partialValue: String - Chuỗi con cần tìm trong giá trị thuộc tính"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thuộc tính content-desc có chứa từ khóa\n" +
                    "mobileKeyword.assertAttributeContains(productItem, \"content-desc\", \"iPhone\");\n\n" +
                    "// Xác minh resource-id có chứa phần nhất định\n" +
                    "mobileKeyword.assertAttributeContains(anyElement, \"resource-id\", \"button_\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại, thuộc tính cần kiểm tra phải tồn tại trên phần tử, " +
                    "và cần biết chính xác tên thuộc tính theo nền tảng (Android/iOS có thể khác nhau). " +
                    "Có thể throw AssertionError nếu giá trị thuộc tính không chứa chuỗi con mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu không thể lấy thuộc tính của phần tử, hoặc NullPointerException nếu giá trị thuộc tính là null."
    )
    @Step("Check (Hard) if attribute '{1}' of {0.name} contains '{2}'")
    public void assertAttributeContains(ObjectUI uiObject, String attributeName, String partialValue) {
        super.performAttributeContainsAssertion(uiObject, attributeName, partialValue, false);
    }

    @NetatKeyword(
            name = "assertElementCount",
            description = "Khẳng định rằng số lượng phần tử tìm thấy khớp với một con số mong đợi. " +
                    "Hữu ích khi cần kiểm tra số lượng các mục trong danh sách, số lượng tùy chọn, hoặc xác minh rằng một nhóm phần tử có số lượng chính xác. " +
                    "Phương thức này tìm tất cả các phần tử khớp với locator và so sánh số lượng với giá trị mong đợi.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Locator để tìm các phần tử",
                    "expectedCount: int - Số lượng phần tử mong đợi"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra số lượng sản phẩm trong giỏ hàng\n" +
                    "mobileKeyword.assertElementCount(cartItems, 3);\n\n" +
                    "// Xác minh danh sách rỗng sau khi xóa\n" +
                    "mobileKeyword.tap(clearAllButton);\n" +
                    "mobileKeyword.assertElementCount(listItems, 0);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Locator của phần tử phải hợp lệ và có thể tìm thấy nhiều phần tử. " +
                    "Nếu mong đợi không tìm thấy phần tử nào (count = 0), locator vẫn phải hợp lệ. " +
                    "Có thể throw AssertionError nếu số lượng phần tử tìm thấy không khớp với số lượng mong đợi, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình điều khiển, hoặc InvalidSelectorException nếu locator không hợp lệ."
    )
    @Step("Check (Hard) if element {0.name} has {1} items")
    public void assertElementCount(ObjectUI uiObject, int expectedCount) {
        execute(() -> {
            List<WebElement> elements = findElements(uiObject);
            Assert.assertEquals(elements.size(), expectedCount,
                    "HARD ASSERT FAILED: Expected to find " + expectedCount + " items, but found " + elements.size() + ".");
            return null;
        }, uiObject, expectedCount);
    }

    @NetatKeyword(
            name = "assertTextWithOptions",
            description = "So sánh văn bản của phần tử với nhiều tùy chọn linh hoạt: có thể bỏ qua sự khác biệt giữa chữ hoa/thường và/hoặc cắt khoảng trắng ở đầu/cuối. " +
                    "Hữu ích khi cần kiểm tra nội dung mà không quan tâm đến định dạng chính xác. " +
                    "Nếu văn bản không khớp theo các tùy chọn đã chọn, một AssertionError sẽ được ném ra.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra văn bản",
                    "expectedText: String - Chuỗi văn bản mong đợi",
                    "ignoreCase: boolean - true để bỏ qua sự khác biệt giữa chữ hoa/thường, false để phân biệt",
                    "trimText: boolean - true để cắt khoảng trắng ở đầu/cuối trước khi so sánh, false để giữ nguyên"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra nội dung chào mừng, bỏ qua chữ hoa/thường và khoảng trắng\n" +
                    "mobileKeyword.assertTextWithOptions(welcomeMessage, \"  xin chào, Người Dùng \", true, true);\n\n" +
                    "// Kiểm tra địa chỉ email không phân biệt chữ hoa/thường\n" +
                    "mobileKeyword.assertTextWithOptions(emailField, \"User@Example.com\", true, true);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính văn bản (text), và phải hiển thị trên màn hình để có thể đọc văn bản. " +
                    "Có thể throw AssertionError nếu văn bản không khớp theo các tùy chọn đã chọn, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu không thể lấy văn bản của phần tử, hoặc NullPointerException nếu văn bản của phần tử là null và expectedText không phải null."
    )
    @Step("Check (Hard) if text of {0.name} is '{1}' (ignoreCase={2}, trim={3})")
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
                Assert.assertTrue(areEqual, "HARD ASSERT FAILED: Text does not match (ignoring case). Expected: '" + realExpectedText + "', Actual: '" + actualText + "'");
            } else {
                Assert.assertEquals(actualText, realExpectedText, "HARD ASSERT FAILED: Text does not match.");
            }
            return null;
        }, uiObject, expectedText, ignoreCase, trimText);
    }

    @NetatKeyword(
            name = "getElementHeight",
            description = "Lấy và trả về chiều cao của một phần tử (tính bằng pixel). " +
                    "Hữu ích khi cần tính toán vị trí tương đối hoặc kiểm tra kích thước hiển thị của phần tử. " +
                    "Phương thức này trả về giá trị số nguyên đại diện cho chiều cao theo pixel của phần tử. " +
                    "Lưu ý: Phần tử phải hiển thị trên màn hình để có thể lấy được kích thước chính xác.",
            category = "Mobile",
            subCategory = "Getter",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần lấy chiều cao"
            },
            returnValue = "int - Chiều cao của phần tử tính bằng pixel",
            example = "// Lấy chiều cao của một hình ảnh\n" +
                    "int imageHeight = mobileKeyword.getElementHeight(productImage);\n\n" +
                    "// Kiểm tra xem phần tử có kích thước đúng không\n" +
                    "int cardHeight = mobileKeyword.getElementHeight(cardElement);\n" +
                    "assert cardHeight > 200 : \"Card height is too small\";",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần đo phải tồn tại và hiển thị trên màn hình, và phải có kích thước xác định (không phải phần tử ẩn hoặc có kích thước 0). " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, hoặc WebDriverException nếu không thể lấy kích thước của phần tử."
    )
    @Step("Get height of element: {0.name}")
    public int getElementHeight(ObjectUI uiObject) {
        return execute(() -> findElement(uiObject).getSize().getHeight(), uiObject);
    }

    @NetatKeyword(
            name = "getElementWidth",
            description = "Lấy và trả về chiều rộng của một phần tử (tính bằng pixel). " +
                    "Hữu ích khi cần tính toán vị trí tương đối hoặc kiểm tra kích thước hiển thị của phần tử. " +
                    "Phương thức này trả về giá trị số nguyên đại diện cho chiều rộng theo pixel của phần tử. " +
                    "Lưu ý: Phần tử phải hiển thị trên màn hình để có thể lấy được kích thước chính xác.",
            category = "Mobile",
            subCategory = "Getter",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần lấy chiều rộng"
            },
            returnValue = "int - Chiều rộng của phần tử tính bằng pixel",
            example = "// Lấy chiều rộng của một nút\n" +
                    "int buttonWidth = mobileKeyword.getElementWidth(submitButton);\n\n" +
                    "// Kiểm tra xem banner có chiều rộng toàn màn hình không\n" +
                    "int bannerWidth = mobileKeyword.getElementWidth(promotionBanner);\n" +
                    "assert bannerWidth == DriverManager.getDriver().manage().window().getSize().width;",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần đo phải tồn tại và hiển thị trên màn hình, và phải có kích thước xác định (không phải phần tử ẩn hoặc có kích thước 0). " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy phần tử, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, hoặc WebDriverException nếu không thể lấy kích thước của phần tử."
    )
    @Step("Get width of element: {0.name}")
    public int getElementWidth(ObjectUI uiObject) {
        return execute(() -> findElement(uiObject).getSize().getWidth(), uiObject);
    }

    @NetatKeyword(
            name = "pressKeyCode",
            description = "Mô phỏng hành động nhấn các phím vật lý của thiết bị Android như HOME, BACK, VOLUME_UP, v.v. " +
                    "Chỉ hoạt động trên Android, sẽ hiển thị cảnh báo nếu được gọi trên iOS. " +
                    "Tham số keyName phải là một giá trị hợp lệ từ enum AndroidKey. " +
                    "Hữu ích khi cần tương tác với các phím vật lý hoặc phím ảo của thiết bị.",
            category = "Mobile",
            subCategory = "System",
            parameters = {
                    "keyName: String - Tên phím trong AndroidKey enum (ví dụ: 'HOME', 'BACK', 'VOLUME_UP')"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Nhấn nút Back để quay lại màn hình trước\n" +
                    "mobileKeyword.pressKeyCode(\"BACK\");\n\n" +
                    "// Nhấn phím Home để quay về màn hình chính\n" +
                    "mobileKeyword.pressKeyCode(\"HOME\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị Android đã được kết nối và cấu hình đúng với Appium. " +
                    "Đang sử dụng AndroidDriver (phương thức không hoạt động trên iOS), " +
                    "và tham số keyName phải là một giá trị hợp lệ từ enum AndroidKey. " +
                    "Có thể throw IllegalArgumentException nếu tên phím không hợp lệ, " +
                    "WebDriverException nếu không thể thực hiện hành động nhấn phím, " +
                    "hoặc UnsupportedCommandException nếu lệnh không được hỗ trợ trên thiết bị hiện tại."
    )
    @Step("Press Android system key: {0}")
    public void pressKeyCode(String keyName) {
        execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            if (driver instanceof AndroidDriver) {
                // Chuyển đổi chuỗi thành hằng số enum AndroidKey
                AndroidKey key = AndroidKey.valueOf(keyName.toUpperCase());
                ((AndroidDriver) driver).pressKey(new KeyEvent(key));
            } else {
                logger.warn("Keyword 'pressKeyCode' is only supported on Android.");
            }
            return null;
        }, keyName);
    }

    @NetatKeyword(
            name = "verifyOrientation",
            description = "Khẳng định rằng màn hình thiết bị đang ở hướng dọc (PORTRAIT) hoặc ngang (LANDSCAPE). " +
                    "Hữu ích để đảm bảo ứng dụng hiển thị đúng hướng trước khi thực hiện các thao tác tiếp theo. " +
                    "Phương thức hoạt động trên cả Android và iOS. " +
                    "Nếu hướng màn hình không khớp với giá trị mong đợi, một AssertionError sẽ được ném ra.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "expectedOrientation: String - Hướng màn hình mong đợi, phải là 'PORTRAIT' hoặc 'LANDSCAPE' (không phân biệt hoa/thường)"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra rằng ứng dụng đang ở chế độ dọc trước khi tiếp tục\n" +
                    "mobileKeyword.verifyOrientation(\"PORTRAIT\");\n\n" +
                    "// Xoay thiết bị và xác minh hướng ngang\n" +
                    "rotateDevice();\n" +
                    "mobileKeyword.verifyOrientation(\"LANDSCAPE\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Đang sử dụng AndroidDriver hoặc IOSDriver, và tham số expectedOrientation phải là 'PORTRAIT' hoặc 'LANDSCAPE'. " +
                    "Có thể throw AssertionError nếu hướng màn hình không khớp với giá trị mong đợi, " +
                    "IllegalArgumentException nếu giá trị expectedOrientation không hợp lệ, " +
                    "UnsupportedOperationException nếu loại driver không được hỗ trợ, " +
                    "hoặc WebDriverException nếu không thể lấy thông tin hướng màn hình."
    )
    @Step("Check (Hard) if device is in {0} orientation")
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

            Assert.assertEquals(actual, expected, "HARD ASSERT FAILED: Device orientation does not match.");
            return null;
        }, expectedOrientation);
    }

    @NetatKeyword(
            name = "setSliderValue",
            description = "Thiết lập giá trị cho một thanh trượt (slider) bằng cách chạm vào vị trí tương ứng. " +
                    "Giá trị từ 0.0 (bên trái) đến 1.0 (bên phải). " +
                    "Phương thức này tự động tính toán tọa độ cần chạm dựa trên kích thước và vị trí của slider. " +
                    "Hữu ích khi cần điều chỉnh các điều khiển như âm lượng, độ sáng, hoặc các giá trị số trong khoảng. " +
                    "Lưu ý: Giá trị phải nằm trong khoảng từ 0.0 đến 1.0, nếu không sẽ gây ra ngoại lệ.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử slider cần điều chỉnh",
                    "value: double - Giá trị cần thiết lập, từ 0.0 (nhỏ nhất/trái) đến 1.0 (lớn nhất/phải)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Thiết lập thanh âm lượng ở mức 75%\n" +
                    "mobileKeyword.setSliderValue(volumeSlider, 0.75);\n\n" +
                    "// Thiết lập độ sáng màn hình ở mức thấp nhất\n" +
                    "mobileKeyword.setSliderValue(brightnessSlider, 0.0);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử slider cần điều chỉnh phải tồn tại và hiển thị trên màn hình, " +
                    "phải là loại có thể điều chỉnh bằng cách chạm vào vị trí khác nhau, " +
                    "và phải có chiều ngang đủ lớn để có thể chạm chính xác vào các vị trí khác nhau. " +
                    "Có thể throw IllegalArgumentException nếu giá trị nằm ngoài khoảng từ 0.0 đến 1.0, " +
                    "NoSuchElementException nếu không tìm thấy phần tử slider, " +
                    "StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu không thể lấy vị trí hoặc kích thước của phần tử, " +
                    "hoặc ElementNotInteractableException nếu không thể tương tác với slider."
    )
    @Step("Set slider {0.name} to {1}")
    public void setSliderValue(ObjectUI uiObject, double value) {
        execute(() -> {
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("Slider value must be between 0.0 and 1.0");
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
            description = "Thực thi một lệnh Appium tùy chỉnh không có sẵn trong các keyword tiêu chuẩn. " +
                    "Cung cấp sự linh hoạt tối đa cho các tình huống đặc thù hoặc các tính năng mới của Appium chưa được bao gồm trong framework. " +
                    "Phương thức này cho phép truyền các tham số phức tạp dưới dạng Map. " +
                    "Lưu ý: Cần hiểu rõ về lệnh Appium cụ thể trước khi sử dụng.",
            category = "Mobile",
            subCategory = "System",
            parameters = {
                    "commandName: String - Tên lệnh Appium cần thực thi (ví dụ: 'mobile: clearApp', 'mobile: shell')",
                    "commandArgs: Map<String, Object> - Các tham số của lệnh dưới dạng key-value"
            },
            returnValue = "Object - Kết quả trả về từ lệnh Appium, kiểu dữ liệu phụ thuộc vào lệnh được thực thi",
            example = "// Xóa dữ liệu của một ứng dụng trên Android\n" +
                    "Map<String, Object> args = new HashMap<>();\n" +
                    "args.put(\"appId\", \"com.example.app\");\n" +
                    "mobileKeyword.executeMobileCommand(\"mobile: clearApp\", args);\n\n" +
                    "// Thực hiện lệnh shell trên Android\n" +
                    "Map<String, Object> shellArgs = new HashMap<>();\n" +
                    "shellArgs.put(\"command\", \"dumpsys battery\");\n" +
                    "Object result = mobileKeyword.executeMobileCommand(\"mobile: shell\", shellArgs);",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Đang sử dụng AppiumDriver (AndroidDriver hoặc IOSDriver), " +
                    "cần hiểu rõ về lệnh Appium cụ thể và các tham số của nó, " +
                    "lệnh phải được hỗ trợ bởi phiên bản Appium và driver đang sử dụng, " +
                    "và một số lệnh có thể yêu cầu quyền đặc biệt hoặc cấu hình bổ sung. " +
                    "Có thể throw WebDriverException nếu lệnh không được hỗ trợ hoặc không thể thực thi, " +
                    "InvalidArgumentException nếu tham số không đúng định dạng hoặc thiếu tham số bắt buộc, " +
                    "UnsupportedCommandException nếu lệnh không được hỗ trợ trên nền tảng hiện tại, " +
                    "SessionNotCreatedException nếu phiên Appium không còn hoạt động, " +
                    "hoặc NoSuchContextException nếu lệnh yêu cầu context không tồn tại."
    )
    @Step("Execute mobile command: {0}")
    public Object executeMobileCommand(String commandName, Map<String, Object> commandArgs) {
        return execute(() -> {
            return ((AppiumDriver) DriverManager.getDriver()).executeScript(commandName, commandArgs);
        }, commandName, commandArgs);
    }

    @NetatKeyword(
            name = "pause",
            description = "Tạm dừng việc thực thi kịch bản trong một khoảng thời gian tĩnh. (Lưu ý: Chỉ nên dùng khi thực sự cần thiết, ưu tiên các keyword chờ động).",
            category = "Mobile",
            subCategory = "Utility",
            parameters = {
                    "milliseconds: int - Thời gian cần tạm dừng (tính bằng mili giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Tạm dừng để đợi animation hoàn thành\n" +
                    "mobileKeyword.pause(3000); // Đợi 3 giây cho quá trình xử lý\n",
            note = "Áp dụng cho nền tảng Mobile. Không có điều kiện tiên quyết đặc biệt. " +
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

    @NetatKeyword(
            name = "Get Current App Package",
            description = "Lấy appPackage (Android) hoặc bundleId (iOS) của ứng dụng hiện tại đang được test",
            category = "Mobile",
            subCategory = "Applifecycle",
            parameters = {},
            returnValue = "String - Package name của ứng dụng Android hoặc bundle ID của ứng dụng iOS",
            example = "String packageName = getCurrentAppPackage();",
            note = "Keyword này hoạt động trên cả Android và iOS. Trên Android sẽ trả về package name, trên iOS sẽ trả về bundle ID từ capabilities."
    )
    @Step("Get current app package")
    public String getCurrentAppPackage() {
        return execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            if (driver instanceof AndroidDriver) {
                return ((AndroidDriver) driver).getCurrentPackage();
            } else { // Giả định là IOSDriver hoặc các driver khác
                // Lấy từ capabilities, là một cách phổ biến
                return (String) driver.getCapabilities().getCapability("bundleId");
            }
        });
    }

    @NetatKeyword(
            name = "getCurrentActivity",
            description = "Lấy tên Activity hiện tại đang được hiển thị trên màn hình Android",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {},
            returnValue = "String - Tên của Activity hiện tại, null nếu không phải Android",
            example = "String activityName = getCurrentActivity();",
            note = "Keyword này chỉ hoạt động trên Android. Trên iOS sẽ trả về null và ghi log cảnh báo. Activity name thường có định dạng như 'com.example.MainActivity'."
    )
    @Step("Get current Activity (Android)")
    public String getCurrentActivity() {
        return execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            if (driver instanceof AndroidDriver) {
                return ((AndroidDriver) driver).currentActivity();
            } else {
                logger.warn("Keyword 'getCurrentActivity' is only supported on Android.");
                return null; // Trả về null nếu không phải Android
            }
        });
    }


    @NetatKeyword(
            name = "Is App Installed",
            description = "Kiểm tra xem một ứng dụng có được cài đặt trên thiết bị hay không",
            category = "Mobile",
            subCategory = "Applifecycle",
            parameters = {
                    "appId: String - Package name (Android) hoặc bundle ID (iOS) của ứng dụng cần kiểm tra"
            },
            returnValue = "boolean - true nếu ứng dụng đã được cài đặt, false nếu chưa được cài đặt",
            example = "boolean isInstalled = isAppInstalled(\"com.example.myapp\");",
            note = "Keyword này hoạt động trên cả Android và iOS. Trên Android sử dụng package name (vd: com.android.chrome), trên iOS sử dụng bundle ID (vd: com.apple.mobilesafari)."
    )
    @Step("Check if app '{0}' is installed")
    public boolean isAppInstalled(String appId) {
        return execute(() -> {
            // Ép kiểu driver sang InteractsWithApps để gọi phương thức isAppInstalled
            return ((InteractsWithApps) DriverManager.getDriver()).isAppInstalled(appId);
        }, appId);
    }

    @NetatKeyword(
            name = "findElements",
            description = "Tìm kiếm và trả về danh sách tất cả các phần tử WebElement khớp với locator được chỉ định. " +
                    "Trả về danh sách rỗng nếu không tìm thấy phần tử nào, không ném exception.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {"ObjectUI uiObject - Đối tượng UI đại diện cho các phần tử cần tìm kiếm"},
            example = "List<WebElement> productList = mobileKeyword.findElements(productListItemObject);"
    )
    @Step("Find list of elements: {0.name}")
    public List<WebElement> findElements(ObjectUI uiObject) {
        return execute(() -> {
            // Sử dụng locator đầu tiên được kích hoạt để tìm kiếm
            By by = uiObject.getActiveLocators().get(0).convertToBy();
            return DriverManager.getDriver().findElements(by);
        }, uiObject);
    }

    @NetatKeyword(
            name = "getElementCount",
            description = "Đếm và trả về số lượng phần tử trên màn hình khớp với locator được chỉ định. " +
                    "Hữu ích để kiểm tra số lượng item trong danh sách hoặc grid.",
            category = "Mobile",
            subCategory = "Getter",
            parameters = {"ObjectUI uiObject - Đối tượng UI đại diện cho các phần tử cần đếm"},
            example = "int numberOfItems = mobileKeyword.getElementCount(listItemObject);"
    )
    @Step("Count the number of elements of: {0.name}")
    public int getElementCount(ObjectUI uiObject) {
        return execute(() -> {
            // Tái sử dụng keyword findElements để tối ưu code
            return findElements(uiObject).size();
        }, uiObject);
    }

    @NetatKeyword(
            name = "getTextFromElements",
            description = "Trích xuất và trả về danh sách các chuỗi văn bản từ tất cả phần tử khớp với locator. " +
                    "Mỗi phần tử trong danh sách sẽ được lấy text và thêm vào kết quả trả về.",
            category = "Mobile",
            subCategory = "Getter",
            parameters = {"ObjectUI uiObject - Đối tượng UI đại diện cho các phần tử cần lấy văn bản"},
            example = "List<String> itemNames = mobileKeyword.getTextFromElements(itemNameObject);"
    )
    @Step("Get text from list element: {0.name}")
    public List<String> getTextFromElements(ObjectUI uiObject) {
        return execute(() -> {
            List<WebElement> elements = findElements(uiObject);
            // Sử dụng Stream API để xử lý một cách hiệu quả
            return elements.stream()
                    .map(WebElement::getText)
                    .collect(Collectors.toList());
        }, uiObject);
    }

    @NetatKeyword(
            name = "tapElementByIndex",
            description = "Thực hiện thao tác tap vào một phần tử cụ thể trong danh sách dựa trên chỉ số (index). " +
                    "Chỉ số bắt đầu từ 0. Ném IndexOutOfBoundsException nếu index không hợp lệ.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI đại diện cho danh sách phần tử",
                    "int index - Vị trí của phần tử cần tap (bắt đầu từ 0)"
            },
            example = "mobileKeyword.tapElementByIndex(menuItems, 2); // Tap vào phần tử thứ 3 trong danh sách"
    )
    @Step("Tap on the element at position {1} in the list {0.name}")
    public void tapElementByIndex(ObjectUI uiObject, int index) {
        execute(() -> {
            List<WebElement> elements = findElements(uiObject);
            if (index >= 0 && index < elements.size()) {
                elements.get(index).click(); // .click() được Appium tự động chuyển đổi thành .tap()
            } else {
                throw new IndexOutOfBoundsException(
                        String.format("Invalid index: %d. List has only %d elements.", index, elements.size())
                );
            }
            return null;
        }, uiObject, index);
    }


}