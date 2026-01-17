package com.vtnet.netat.mobile.keywords;

import com.vtnet.netat.core.BaseUiKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.ui.ObjectUI;
import com.vtnet.netat.core.utils.ScreenshotUtils;
import com.vtnet.netat.driver.DriverManager;
import io.appium.java_client.*;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.connection.ConnectionState;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.clipboard.HasClipboard;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.html5.Location;
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
import com.vtnet.netat.core.secret.SecretDecryptor;


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
    public void backgroundApp(int seconds) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).runAppInBackground(Duration.ofSeconds(seconds));
            return null;
        }, seconds);
    }

    @NetatKeyword(
            name = "launchApp",
            description = "Khởi động ứng dụng từ đầu (fresh launch) như lần đầu tiên mở. " +
                    "Khác với activateApp, keyword này luôn khởi động ứng dụng ở trạng thái sạch, " +
                    "xóa state/session cũ và bắt đầu lại từ màn hình đầu tiên. " +
                    "Hữu ích cho các test case cần kiểm tra flow từ đầu như login, onboarding, hoặc first-time setup. " +
                    "Tương đương với việc: terminate app (nếu đang chạy) -> activate app.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Khởi động app từ đầu để test login flow\n" +
                    "mobileKeyword.launchApp();\n" +
                    "mobileKeyword.sendText(usernameField, \"testuser\");\n" +
                    "mobileKeyword.sendText(passwordField, \"password123\");\n" +
                    "mobileKeyword.tap(loginButton);\n\n" +
                    "// Test onboarding screen cho user mới\n" +
                    "mobileKeyword.launchApp();\n" +
                    "mobileKeyword.assertElementVisible(welcomeScreen);\n\n" +
                    "// Reset app về trạng thái ban đầu giữa các test case\n" +
                    "mobileKeyword.launchApp();",
            note = "Áp dụng cho nền tảng Mobile. Ứng dụng đã được cài đặt và cấu hình trong Desired Capabilities. " +
                    "Keyword này sẽ terminate app nếu đang chạy, sau đó activate lại. " +
                    "Tất cả session data, cache, và trạng thái trước đó sẽ bị xóa. " +
                    "Có thể throw WebDriverException nếu không thể khởi động ứng dụng."
    )
    public void launchApp() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            // Lấy appId từ capabilities
            String appId = null;
            if (driver instanceof AndroidDriver) {
                appId = (String) driver.getCapabilities().getCapability("appPackage");
            } else if (driver instanceof IOSDriver) {
                appId = (String) driver.getCapabilities().getCapability("bundleId");
            }

            if (appId != null) {
                // Terminate app nếu đang chạy
                try {
                    ((InteractsWithApps) driver).terminateApp(appId);
                } catch (Exception e) {
                    logger.debug("App was not running or could not be terminated: " + e.getMessage());
                }
                // Activate app để khởi động lại
                ((InteractsWithApps) driver).activateApp(appId);
            } else {
                throw new IllegalStateException("Cannot determine app ID. Please ensure 'appPackage' (Android) or 'bundleId' (iOS) is set in capabilities.");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "closeApp",
            description = "Đóng ứng dụng hiện tại đang được kiểm soát bởi Appium driver. " +
                    "Khác với terminateApp, keyword này đóng ứng dụng một cách graceful, " +
                    "cho phép ứng dụng lưu trạng thái và thực hiện cleanup trước khi đóng. " +
                    "Session của Appium vẫn được giữ lại, có thể khởi động lại app khác trong cùng session. " +
                    "Hữu ích khi cần chuyển đổi giữa nhiều ứng dụng hoặc kết thúc test một cách clean.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Đóng app sau khi hoàn thành test\n" +
                    "mobileKeyword.tap(logoutButton);\n" +
                    "mobileKeyword.closeApp();\n\n" +
                    "// Đóng app hiện tại để chuyển sang app khác\n" +
                    "mobileKeyword.closeApp();\n" +
                    "mobileKeyword.activateApp(\"com.android.settings\");\n\n" +
                    "// Đóng app để kiểm tra launch behavior\n" +
                    "mobileKeyword.closeApp();\n" +
                    "mobileKeyword.launchApp();",
            note = "Áp dụng cho nền tảng Mobile. Ứng dụng đang chạy và được kiểm soát bởi Appium. " +
                    "Session Appium vẫn được giữ lại sau khi đóng app. " +
                    "Để mở lại app, sử dụng activateApp() hoặc launchApp(). " +
                    "Có thể throw WebDriverException nếu không thể đóng ứng dụng."
    )
    public void closeApp() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            // Lấy appId từ capabilities để terminate app
            String appId = null;
            if (driver instanceof AndroidDriver) {
                appId = (String) driver.getCapabilities().getCapability("appPackage");
            } else if (driver instanceof IOSDriver) {
                appId = (String) driver.getCapabilities().getCapability("bundleId");
            }

            if (appId != null) {
                ((InteractsWithApps) driver).terminateApp(appId);
            } else {
                throw new IllegalStateException("Cannot determine app ID to close. Please ensure 'appPackage' (Android) or 'bundleId' (iOS) is set in capabilities.");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "removeApp",
            description = "Gỡ cài đặt ứng dụng và xóa toàn bộ dữ liệu liên quan khỏi thiết bị. " +
                    "Keyword này tương đương với uninstallApp nhưng đảm bảo xóa sạch tất cả app data, cache, và files. " +
                    "Trên Android: xóa package và data directory. " +
                    "Trên iOS: xóa bundle và documents directory. " +
                    "Hữu ích cho test cleanup hoặc đảm bảo môi trường test hoàn toàn sạch.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {
                    "appId: String - Package name (Android) hoặc Bundle ID (iOS) của ứng dụng cần xóa"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Xóa hoàn toàn ứng dụng để chuẩn bị test cài đặt mới\n" +
                    "mobileKeyword.removeApp(\"com.example.myapp\");\n" +
                    "mobileKeyword.installApp(\"C:/apps/myapp-v2.apk\");\n\n" +
                    "// Cleanup sau khi test xong\n" +
                    "mobileKeyword.removeApp(\"com.test.tempapp\");\n\n" +
                    "// Xóa app và data để test fresh install experience\n" +
                    "mobileKeyword.removeApp(\"com.example.myapp\");\n" +
                    "mobileKeyword.installApp(\"C:/apps/myapp.apk\");\n" +
                    "mobileKeyword.launchApp();",
            note = "Áp dụng cho nền tảng Mobile. Có quyền gỡ cài đặt ứng dụng trên thiết bị. " +
                    "Keyword này xóa hoàn toàn app và data, không thể khôi phục. " +
                    "Một số ứng dụng hệ thống không thể xóa. " +
                    "Có thể throw WebDriverException nếu không thể xóa ứng dụng."
    )
    public void removeApp(String appId) {
        execute(() -> {
            ((InteractsWithApps) DriverManager.getDriver()).removeApp(appId);
            return null;
        }, appId);
    }

    @NetatKeyword(
            name = "queryAppState",
            description = "Truy vấn trạng thái hiện tại của một ứng dụng trên thiết bị. " +
                    "Trả về số nguyên đại diện cho trạng thái của app: " +
                    "0 = NOT_INSTALLED (chưa cài đặt), " +
                    "1 = NOT_RUNNING (đã cài đặt nhưng không chạy), " +
                    "2 = RUNNING_IN_BACKGROUND_SUSPENDED (chạy nền bị tạm dừng), " +
                    "3 = RUNNING_IN_BACKGROUND (chạy nền đang hoạt động), " +
                    "4 = RUNNING_IN_FOREGROUND (chạy ở foreground). " +
                    "Hữu ích để verify app state trước/sau khi thực hiện các thao tác lifecycle.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {
                    "appId: String - Package name (Android) hoặc Bundle ID (iOS) của ứng dụng cần kiểm tra"
            },
            returnValue = "int - Trạng thái của app (0-4)",
            example = "// Kiểm tra app có đang chạy không trước khi activate\n" +
                    "int state = mobileKeyword.queryAppState(\"com.example.myapp\");\n" +
                    "if (state < 3) {\n" +
                    "    mobileKeyword.activateApp(\"com.example.myapp\");\n" +
                    "}\n\n" +
                    "// Verify app chuyển sang background sau khi nhấn Home\n" +
                    "mobileKeyword.backgroundApp(5);\n" +
                    "int bgState = mobileKeyword.queryAppState(\"com.example.myapp\");\n" +
                    "// bgState sẽ là 2 hoặc 3\n\n" +
                    "// Kiểm tra app đã được cài đặt chưa\n" +
                    "int installState = mobileKeyword.queryAppState(\"com.example.newapp\");\n" +
                    "if (installState == 0) {\n" +
                    "    mobileKeyword.installApp(\"C:/apps/newapp.apk\");\n" +
                    "}",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Giá trị trả về: 0=NOT_INSTALLED, 1=NOT_RUNNING, 2=RUNNING_IN_BACKGROUND_SUSPENDED, " +
                    "3=RUNNING_IN_BACKGROUND, 4=RUNNING_IN_FOREGROUND. " +
                    "Có thể throw WebDriverException nếu không thể truy vấn trạng thái."
    )
    public int queryAppState(String appId) {
        return execute(() -> {
            return ((InteractsWithApps) DriverManager.getDriver()).queryAppState(appId).ordinal();
        }, appId);
    }

    @NetatKeyword(
            name = "getCurrentAppPackage",
            description = "Lấy định danh (identifier) của ứng dụng hiện đang chạy ở foreground. " +
                    "Trên Android: trả về package name (ví dụ: com.example.myapp). " +
                    "Trên iOS: trả về bundle ID (ví dụ: com.example.MyApp). " +
                    "Hữu ích để verify đang ở đúng ứng dụng hoặc theo dõi việc chuyển đổi giữa các app.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {},
            returnValue = "String - Package name (Android) hoặc Bundle ID (iOS) của ứng dụng hiện tại",
            example = "// Verify đang ở đúng ứng dụng sau khi activate\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\");\n" +
                    "String currentApp = mobileKeyword.getCurrentAppPackage();\n" +
                    "Assert.assertEquals(currentApp, \"com.example.myapp\");\n\n" +
                    "// Kiểm tra app có chuyển sang Settings không\n" +
                    "mobileKeyword.tap(settingsLink);\n" +
                    "String currentPackage = mobileKeyword.getCurrentAppPackage();\n" +
                    "// Trên Android: com.android.settings\n" +
                    "// Trên iOS: com.apple.Preferences\n\n" +
                    "// Log app hiện tại để debug\n" +
                    "System.out.println(\"Current app: \" + mobileKeyword.getCurrentAppPackage());",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Trên Android: lấy từ currentPackage() method. " +
                    "Trên iOS: lấy từ capabilities bundleId. " +
                    "Trả về null nếu không thể xác định app hiện tại."
    )
    public String getCurrentAppPackage() {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                return ((AndroidDriver) driver).getCurrentPackage();
            } else if (driver instanceof IOSDriver) {
                // iOS: lấy từ capabilities
                return (String) driver.getCapabilities().getCapability("bundleId");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "getAppVersion",
            description = "Lấy phiên bản (version) của ứng dụng đang được test. " +
                    "Trả về version name/string của app (ví dụ: \"1.2.3\", \"2.0.0-beta\"). " +
                    "Hữu ích để verify đúng version đang test, ghi log trong test report, " +
                    "hoặc thực hiện logic khác nhau dựa trên version của app.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {},
            returnValue = "String - Version string của ứng dụng (ví dụ: \"1.2.3\")",
            example = "// Log version của app trong test report\n" +
                    "String appVersion = mobileKeyword.getAppVersion();\n" +
                    "System.out.println(\"Testing app version: \" + appVersion);\n\n" +
                    "// Verify đúng version trước khi test\n" +
                    "String version = mobileKeyword.getAppVersion();\n" +
                    "Assert.assertEquals(version, \"2.0.0\");\n\n" +
                    "// Thực hiện logic khác nhau dựa trên version\n" +
                    "String currentVersion = mobileKeyword.getAppVersion();\n" +
                    "if (currentVersion.startsWith(\"2.\")) {\n" +
                    "    // Test features của version 2.x\n" +
                    "}",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Trên Android: lấy từ versionName trong AndroidManifest. " +
                    "Trên iOS: lấy từ CFBundleShortVersionString trong Info.plist. " +
                    "Trả về null nếu không thể lấy version information."
    )
    public String getAppVersion() {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                return (String) driver.getCapabilities().getCapability("appVersion");
            } else if (driver instanceof IOSDriver) {
                return (String) driver.getCapabilities().getCapability("CFBundleShortVersionString");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "getAppBuildNumber",
            description = "Lấy build number của ứng dụng đang được test. " +
                    "Build number thường là số nguyên tăng dần theo mỗi lần build (ví dụ: 42, 1523). " +
                    "Khác với version name, build number được dùng để phân biệt các build khác nhau của cùng một version. " +
                    "Hữu ích để verify đúng build đang test hoặc ghi log chi tiết trong test report.",
            category = "Mobile",
            subCategory = "AppLifecycle",
            parameters = {},
            returnValue = "String - Build number của ứng dụng (ví dụ: \"42\")",
            example = "// Log build number trong test report\n" +
                    "String buildNumber = mobileKeyword.getAppBuildNumber();\n" +
                    "System.out.println(\"Testing build: \" + buildNumber);\n\n" +
                    "// Verify đúng build trước khi test\n" +
                    "String build = mobileKeyword.getAppBuildNumber();\n" +
                    "Assert.assertEquals(build, \"1523\");\n\n" +
                    "// Log đầy đủ app info\n" +
                    "String version = mobileKeyword.getAppVersion();\n" +
                    "String build = mobileKeyword.getAppBuildNumber();\n" +
                    "System.out.println(\"Testing: v\" + version + \" build \" + build);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Trên Android: lấy từ versionCode trong AndroidManifest. " +
                    "Trên iOS: lấy từ CFBundleVersion trong Info.plist. " +
                    "Trả về null nếu không thể lấy build information."
    )
    public String getAppBuildNumber() {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                Object versionCode = driver.getCapabilities().getCapability("versionCode");
                return versionCode != null ? versionCode.toString() : null;
            } else if (driver instanceof IOSDriver) {
                return (String) driver.getCapabilities().getCapability("CFBundleVersion");
            }
            return null;
        });
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
    public void sendText(ObjectUI uiObject, String text) {
        super.sendKeys(uiObject, text);
    }

    @NetatKeyword(
            name = "sendTextSensitive",
            description = "Nhập dữ liệu nhạy cảm đã được mã hóa vào một ô input. " +
                    "Tự động giải mã giá trị với master key từ ENV/.env trước khi nhập. " +
                    "Giá trị thật sẽ được mask trong log/report để bảo mật (ví dụ: 'S*****3'). " +
                    "Chỉ hoạt động với các phần tử có thuộc tính 'editable' là true.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng đầu vào có thể chỉnh sửa (như TextField, EditText)",
                    "encryptedText: String - Văn bản ĐÃ MÃ HÓA cần nhập vào phần tử"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Nhập mật khẩu đã mã hóa\n" +
                    "mobileKeyword.sendTextSensitive(passwordInput, \"U2FsdGVkX1+abc123...\");\n\n" +
                    "// Nhập API token đã mã hóa\n" +
                    "mobileKeyword.sendTextSensitive(tokenInput, config.get(\"api_token\"));\n\n" +
                    "// Log sẽ hiển thị: \"Enter sensitive text '*****' into element: passwordInput\"",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Master key phải được cấu hình qua: ENV NETAT_MASTER_KEY, System Property netat.master.key, " +
                    "hoặc file .env/.netat-master-key. Giá trị đầu vào phải là chuỗi đã được mã hóa bằng tool NETAT Secret. " +
                    "Có thể throw SecretDecryptionException nếu giải mã thất bại, " +
                    "MasterKeyNotFoundException nếu không tìm thấy master key, " +
                    "hoặc ElementNotInteractableException nếu phần tử không thể tương tác."
    )
    public void sendTextSensitive(ObjectUI uiObject, String encryptedText) {
        execute(() -> {
            String plainText = SecretDecryptor.decrypt(encryptedText);

            String maskedText = maskSensitiveValue(plainText);
            logger.info("Sending sensitive text '{}' to element '{}'", maskedText, uiObject.getName());

            super.sendKeys(uiObject, plainText);

            return null;
        }, uiObject, maskSensitiveValue("***"));
    }


    private String maskSensitiveValue(String value) {
        if (value == null || value.isEmpty()) return "";
        return "*****";
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
                    "uiObject: ObjectUI - Phần tử cần kiểm tra sự tồn tại",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra nút đăng nhập tồn tại\n" +
                    "mobileKeyword.assertElementPresent(loginButton);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertElementPresent(loginButton, \"Nút đăng nhập phải tồn tại trên màn hình\");\n\n" +
                    "// Kiểm tra nhiều element với custom message\n" +
                    "mobileKeyword.assertElementPresent(usernameField, \"Trường username phải có trong form\");\n" +
                    "mobileKeyword.assertElementPresent(passwordField, \"Trường password phải có trong form\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Đã xác định chính xác phần tử UI cần kiểm tra. " +
                    "Có thể throw AssertionError nếu phần tử không tồn tại trong DOM, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình điều khiển."
    )
    public void assertElementPresent(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            try {
                super.verifyElementPresent(uiObject, 0); // timeout 0 for immediate check
            } catch (AssertionError e) {
                if (customMessage.length > 0) {
                    throw new AssertionError(customMessage[0], e);
                }
                throw e;
            }
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertElementVisible",
            description = "Khẳng định rằng một phần tử đang được hiển thị trên màn hình và người dùng có thể nhìn thấy. " +
                    "Khác với assertElementPresent, phương thức này kiểm tra cả sự tồn tại và tính hiển thị của phần tử. " +
                    "Nếu phần tử không tồn tại hoặc không hiển thị, một AssertionError sẽ được ném ra.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần kiểm tra tính hiển thị",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra thông báo thành công hiển thị\n" +
                    "mobileKeyword.assertElementVisible(successMessage);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertElementVisible(successMessage, \"Thông báo thành công phải hiển thị sau khi đăng nhập\");\n\n" +
                    "// Kiểm tra các element UI hiển thị\n" +
                    "mobileKeyword.assertElementVisible(menuButton, \"Nút menu phải hiển thị trên header\");\n" +
                    "mobileKeyword.assertElementVisible(profileIcon, \"Icon profile phải hiển thị khi đã đăng nhập\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Đã xác định chính xác phần tử UI cần kiểm tra. " +
                    "Có thể throw AssertionError nếu phần tử không tồn tại hoặc không hiển thị, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình điều khiển, " +
                    "hoặc StaleElementReferenceException nếu phần tử không còn gắn với DOM."
    )
    public void assertElementVisible(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            try {
                super.verifyElementVisibleHard(uiObject, true);
            } catch (AssertionError e) {
                if (customMessage.length > 0) {
                    throw new AssertionError(customMessage[0], e);
                }
                throw e;
            }
            return null;
        }, uiObject);
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
                    "expectedText: String - Chuỗi văn bản mong đợi để so sánh",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra tiêu đề màn hình\n" +
                    "mobileKeyword.assertTextEquals(title, \"Đăng nhập\");\n\n" +
                    "// Xác minh thông báo lỗi với custom message\n" +
                    "mobileKeyword.assertTextEquals(errorMessage, \"Email không hợp lệ\", \n" +
                    "    \"Thông báo lỗi phải hiển thị đúng nội dung khi email sai format\");\n\n" +
                    "// Kiểm tra label của các field\n" +
                    "mobileKeyword.assertTextEquals(usernameLabel, \"Tên đăng nhập\", \n" +
                    "    \"Label của trường username phải hiển thị đúng\");\n" +
                    "mobileKeyword.assertTextEquals(passwordLabel, \"Mật khẩu\", \n" +
                    "    \"Label của trường password phải hiển thị đúng\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử UI cần kiểm tra phải hiển thị và chứa văn bản. " +
                    "Có thể throw AssertionError nếu văn bản của phần tử không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "hoặc StaleElementReferenceException nếu phần tử không còn gắn với DOM."
    )
    public void assertTextEquals(ObjectUI uiObject, String expectedText, String... customMessage) {
        execute(() -> {
            try {
                super.verifyTextHard(uiObject, expectedText);
            } catch (AssertionError e) {
                if (customMessage.length > 0) {
                    throw new AssertionError(customMessage[0], e);
                }
                throw e;
            }
            return null;
        }, uiObject, expectedText);
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
                    "uiObject: ObjectUI - Phần tử cần kiểm tra trạng thái",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kiểm tra checkbox đã được chọn\n" +
                    "mobileKeyword.tap(agreeToTermsCheckbox);\n" +
                    "mobileKeyword.assertChecked(agreeToTermsCheckbox);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertChecked(agreeToTermsCheckbox, \n" +
                    "    \"Checkbox đồng ý điều khoản phải được chọn trước khi đăng ký\");\n\n" +
                    "// Kiểm tra các toggle switches\n" +
                    "mobileKeyword.assertChecked(notificationSwitch, \n" +
                    "    \"Switch thông báo phải được bật theo mặc định\");\n" +
                    "mobileKeyword.assertChecked(darkModeSwitch, \n" +
                    "    \"Chế độ tối phải được kích hoạt sau khi user chọn\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử UI cần kiểm tra phải là loại có thể chọn/bỏ chọn và hỗ trợ thuộc tính 'checked'. " +
                    "Có thể throw AssertionError nếu phần tử không ở trạng thái được chọn/bật, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, " +
                    "hoặc WebDriverException nếu không thể truy cập thuộc tính 'checked'."
    )
    public void assertChecked(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            boolean isChecked = Boolean.parseBoolean(element.getAttribute("checked"));

            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' is not in checked/enabled state.";

            Assert.assertTrue(isChecked, message);
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
    public WebElement scrollToText(String textToFind) {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            String platform = String.valueOf(driver.getCapabilities().getPlatformName());

            if (textToFind == null || textToFind.isEmpty()) {
                throw new IllegalArgumentException("textToFind must not be null or empty");
            }

            // Escape dấu " để không vỡ chuỗi trong UiAutomator/NSPredicate
            final String qAndroid = textToFind.replace("\"", "\\\"");
            final String qiOS    = textToFind.replace("'", "\\'");

            if ("android".equalsIgnoreCase(platform)) {
                // 1) Thử scroll + find theo TEXT chứa chuỗi
                String uiText = "new UiScrollable(new UiSelector().scrollable(true))"
                        + ".scrollIntoView(new UiSelector().textContains(\"" + qAndroid + "\"))";
                By byText = AppiumBy.androidUIAutomator(uiText);
                logger.info("Android locator (textContains): " + uiText);
                List<WebElement> found = driver.findElements(byText);
                if (!found.isEmpty()) return found.get(0);

                // 2) Nếu không thấy, thử scroll + find theo DESCRIPTION chứa chuỗi
                String uiDesc = "new UiScrollable(new UiSelector().scrollable(true))"
                        + ".scrollIntoView(new UiSelector().descriptionContains(\"" + qAndroid + "\"))";
                By byDesc = AppiumBy.androidUIAutomator(uiDesc);
                logger.info("Android locator (descriptionContains): " + uiDesc);
                found = driver.findElements(byDesc);
                if (!found.isEmpty()) return found.get(0);

                // 3) Fallback: nếu layout không đánh dấu scrollable(true) chuẩn → tự swipe và tìm lại
                int maxScrolls = 10;
                By quickText = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + qAndroid + "\")");
                By quickDesc = AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"" + qAndroid + "\")");

                for (int i = 0; i < maxScrolls; i++) {
                    found = driver.findElements(quickText);
                    if (!found.isEmpty()) return found.get(0);
                    found = driver.findElements(quickDesc);
                    if (!found.isEmpty()) return found.get(0);

                    swipeUp(500); // bạn đã có sẵn method này
                }
                throw new NoSuchElementException("Android: Not found element with text/desc contains: " + textToFind);

            } else { // iOS
                // Kiểm cả label / name / value, không phân biệt hoa thường (CONTAINS[c])
                String predicate = "(label CONTAINS[c] '" + qiOS + "' OR name CONTAINS[c] '" + qiOS + "' OR value CONTAINS[c] '" + qiOS + "')";
                By byAny = AppiumBy.iOSNsPredicateString(predicate);

                // 1) Thử tìm trước khi cuộn
                List<WebElement> elements = driver.findElements(byAny);
                if (!elements.isEmpty()) return elements.get(0);

                // 2) Cố gắng dùng 'mobile: scroll' theo predicate (nếu đang ở trong một ScrollView/ListView chuẩn)
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> args = new java.util.HashMap<>();
                    args.put("direction", "down");
                    args.put("predicateString", predicate);
                    // Một số bản Appium yêu cầu 'toVisible': true; thêm vào nếu cần
                    // args.put("toVisible", true);
                    driver.executeScript("mobile: scroll", args);

                    elements = driver.findElements(byAny);
                    if (!elements.isEmpty()) return elements.get(0);
                } catch (Exception ignored) {
                    // Nếu môi trường không hỗ trợ mobile: scroll theo predicate → fallback swipe
                }

                // 3) Fallback: tự swipe và tìm lại
                int maxScrolls = 10;
                for (int i = 0; i < maxScrolls; i++) {
                    swipeUp(500);
                    elements = driver.findElements(byAny);
                    if (!elements.isEmpty()) return elements.get(0);
                }
                throw new NoSuchElementException("iOS: Could not find element containing: " + textToFind + " after " + 10 + " scrolls");
            }
        }, textToFind);
    }

    @NetatKeyword(
            name = "swipeLeft",
            description = "Thực hiện hành động vuốt sang trái trên màn hình. " +
                    "Tương đương với thao tác chuyển sang mục tiếp theo trong carousel, gallery, hoặc swipe navigation. " +
                    "Phương thức này tự động tính toán các tọa độ dựa trên kích thước màn hình thiết bị.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "durationInMs: Integer... - (Tùy chọn) Thời gian thực hiện vuốt (ms). Mặc định là 500ms nếu không được chỉ định"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Vuốt trái với tốc độ mặc định để xem ảnh tiếp theo\n" +
                    "mobileKeyword.swipeLeft();\n\n" +
                    "// Vuốt trái nhanh hơn\n" +
                    "mobileKeyword.swipeLeft(300);\n\n" +
                    "// Vuốt trái trong carousel\n" +
                    "mobileKeyword.swipeLeft();\n" +
                    "mobileKeyword.assertElementVisible(nextImage);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Vuốt từ 80% sang 20% chiều rộng màn hình theo chiều ngang. " +
                    "Có thể throw WebDriverException nếu không thể thực hiện hành động vuốt."
    )
    public void swipeLeft(Integer... durationInMs) {
        int duration = (durationInMs != null && durationInMs.length > 0 && durationInMs[0] != null)
                ? durationInMs[0]
                : 500;
        execute(() -> {
            Dimension size = DriverManager.getDriver().manage().window().getSize();
            int startX = (int) (size.getWidth() * 0.8);
            int endX = (int) (size.getWidth() * 0.2);
            int y = size.getHeight() / 2;
            swipe(startX, y, endX, y, duration);
            return null;
        }, (Object[]) durationInMs);
    }

    @NetatKeyword(
            name = "swipeRight",
            description = "Thực hiện hành động vuốt sang phải trên màn hình. " +
                    "Tương đương với thao tác quay lại mục trước trong carousel, gallery, hoặc swipe navigation. " +
                    "Phương thức này tự động tính toán các tọa độ dựa trên kích thước màn hình thiết bị.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "durationInMs: Integer... - (Tùy chọn) Thời gian thực hiện vuốt (ms). Mặc định là 500ms nếu không được chỉ định"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Vuốt phải với tốc độ mặc định để xem ảnh trước\n" +
                    "mobileKeyword.swipeRight();\n\n" +
                    "// Vuốt phải chậm hơn\n" +
                    "mobileKeyword.swipeRight(800);\n\n" +
                    "// Vuốt phải để quay lại trong carousel\n" +
                    "mobileKeyword.swipeRight();\n" +
                    "mobileKeyword.assertElementVisible(previousImage);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Vuốt từ 20% sang 80% chiều rộng màn hình theo chiều ngang. " +
                    "Có thể throw WebDriverException nếu không thể thực hiện hành động vuốt."
    )
    public void swipeRight(Integer... durationInMs) {
        int duration = (durationInMs != null && durationInMs.length > 0 && durationInMs[0] != null)
                ? durationInMs[0]
                : 500;
        execute(() -> {
            Dimension size = DriverManager.getDriver().manage().window().getSize();
            int startX = (int) (size.getWidth() * 0.2);
            int endX = (int) (size.getWidth() * 0.8);
            int y = size.getHeight() / 2;
            swipe(startX, y, endX, y, duration);
            return null;
        }, (Object[]) durationInMs);
    }

    @NetatKeyword(
            name = "scrollToElement",
            description = "Tự động cuộn màn hình theo hướng chỉ định cho đến khi tìm thấy element mong muốn. " +
                    "Phương thức này sẽ thực hiện tối đa 10 lần scroll để tìm kiếm. " +
                    "Hỗ trợ 4 hướng: up, down, left, right. " +
                    "Trả về WebElement nếu tìm thấy, hoặc ném NoSuchElementException nếu không tìm thấy.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "uiObject: ObjectUI - Element cần tìm và scroll đến",
                    "direction: String - Hướng scroll (\"up\", \"down\", \"left\", \"right\")"
            },
            returnValue = "WebElement - Element được tìm thấy",
            example = "// Scroll xuống để tìm button\n" +
                    "WebElement btn = mobileKeyword.scrollToElement(submitButton, \"down\");\n" +
                    "btn.click();\n\n" +
                    "// Scroll lên để tìm header\n" +
                    "mobileKeyword.scrollToElement(pageHeader, \"up\");\n\n" +
                    "// Scroll sang phải để tìm item trong horizontal list\n" +
                    "mobileKeyword.scrollToElement(menuItem, \"right\");",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Direction không phân biệt hoa thường. " +
                    "Nếu element đã hiển thị, trả về ngay không cần scroll. " +
                    "Có thể throw NoSuchElementException nếu không tìm thấy sau 10 lần scroll, " +
                    "hoặc IllegalArgumentException nếu direction không hợp lệ."
    )
    public WebElement scrollToElement(ObjectUI uiObject, String direction) {
        return execute(() -> {
            int maxScrolls = 10;
            String dir = direction.toLowerCase();

            // Check if element is already visible
            try {
                WebElement element = findElement(uiObject);
                if (element.isDisplayed()) {
                    return element;
                }
            } catch (Exception e) {
                // Element not found yet, will scroll
            }

            // Scroll until element is found
            for (int i = 0; i < maxScrolls; i++) {
                try {
                    WebElement element = findElement(uiObject);
                    if (element.isDisplayed()) {
                        return element;
                    }
                } catch (Exception e) {
                    // Element not found, continue scrolling
                }

                // Perform scroll based on direction
                switch (dir) {
                    case "up":
                        swipeDown(); // Swipe down to scroll up content
                        break;
                    case "down":
                        swipeUp(); // Swipe up to scroll down content
                        break;
                    case "left":
                        swipeRight(); // Swipe right to scroll left content
                        break;
                    case "right":
                        swipeLeft(); // Swipe left to scroll right content
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid direction: " + direction + ". Must be one of: up, down, left, right");
                }
            }

            throw new NoSuchElementException("Could not find element after scrolling " + maxScrolls + " times in direction: " + direction);
        }, uiObject, direction);
    }

    @NetatKeyword(
            name = "scrollToTop",
            description = "Cuộn nhanh về đầu trang/màn hình. " +
                    "Thực hiện nhiều lần swipe down liên tiếp để đảm bảo về đến vị trí đầu tiên. " +
                    "Hữu ích khi cần reset vị trí scroll về đầu danh sách hoặc trang.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Scroll về đầu danh sách\n" +
                    "mobileKeyword.scrollToTop();\n" +
                    "mobileKeyword.assertElementVisible(firstItem);\n\n" +
                    "// Reset scroll position trước test\n" +
                    "mobileKeyword.scrollToTop();\n\n" +
                    "// Quay về đầu feed\n" +
                    "mobileKeyword.scrollToTop();\n" +
                    "mobileKeyword.tap(refreshButton);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Thực hiện 5 lần swipe down nhanh (200ms mỗi lần). " +
                    "Có thể không về chính xác vị trí đầu nếu danh sách có header động. " +
                    "Có thể throw WebDriverException nếu không thể thực hiện hành động scroll."
    )
    public void scrollToTop() {
        execute(() -> {
            // Perform multiple fast swipes down to reach top
            for (int i = 0; i < 5; i++) {
                swipeDown(200);
                try {
                    Thread.sleep(100); // Small delay between swipes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "scrollToBottom",
            description = "Cuộn nhanh về cuối trang/màn hình. " +
                    "Thực hiện nhiều lần swipe up liên tiếp để đảm bảo về đến vị trí cuối cùng. " +
                    "Hữu ích khi cần kiểm tra footer hoặc item cuối cùng trong danh sách.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Scroll về cuối danh sách\n" +
                    "mobileKeyword.scrollToBottom();\n" +
                    "mobileKeyword.assertElementVisible(loadMoreButton);\n\n" +
                    "// Kiểm tra footer\n" +
                    "mobileKeyword.scrollToBottom();\n" +
                    "mobileKeyword.assertElementVisible(footerText);\n\n" +
                    "// Load all content\n" +
                    "mobileKeyword.scrollToBottom();\n" +
                    "mobileKeyword.tap(lastItem);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Thực hiện 5 lần swipe up nhanh (200ms mỗi lần). " +
                    "Với infinite scroll, có thể không về được cuối thật sự. " +
                    "Có thể throw WebDriverException nếu không thể thực hiện hành động scroll."
    )
    public void scrollToBottom() {
        execute(() -> {
            // Perform multiple fast swipes up to reach bottom
            for (int i = 0; i < 5; i++) {
                swipeUp(200);
                try {
                    Thread.sleep(100); // Small delay between swipes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "fling",
            description = "Thực hiện gesture fling (vuốt nhanh mạnh) theo hướng chỉ định với vận tốc cao. " +
                    "Khác với swipe thông thường, fling tạo hiệu ứng cuộn quán tính (momentum scrolling). " +
                    "Velocity càng cao thì cuộn càng nhanh và xa (1-10, khuyến nghị 3-7). " +
                    "Hữu ích cho việc cuộn nhanh qua danh sách dài.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "direction: String - Hướng fling (\"up\", \"down\", \"left\", \"right\")",
                    "velocity: int - Vận tốc fling (1-10), giá trị cao = nhanh hơn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Fling nhanh xuống dưới để cuộn danh sách dài\n" +
                    "mobileKeyword.fling(\"down\", 7);\n\n" +
                    "// Fling nhẹ lên trên\n" +
                    "mobileKeyword.fling(\"up\", 3);\n\n" +
                    "// Fling sang trái trong gallery\n" +
                    "mobileKeyword.fling(\"left\", 5);\n\n" +
                    "// Fling mạnh nhất\n" +
                    "mobileKeyword.fling(\"down\", 10);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Velocity được map sang duration: velocity càng cao, duration càng ngắn (fling càng nhanh). " +
                    "Direction không phân biệt hoa thường. " +
                    "Có thể throw IllegalArgumentException nếu direction hoặc velocity không hợp lệ, " +
                    "hoặc WebDriverException nếu không thể thực hiện gesture."
    )
    public void fling(String direction, int velocity) {
        execute(() -> {
            // Validate velocity
            if (velocity < 1 || velocity > 10) {
                throw new IllegalArgumentException("Velocity must be between 1 and 10, got: " + velocity);
            }

            // Calculate duration based on velocity (higher velocity = shorter duration)
            int duration = 600 - (velocity * 50); // velocity 1 = 550ms, velocity 10 = 100ms

            String dir = direction.toLowerCase();
            switch (dir) {
                case "up":
                    swipeDown(duration);
                    break;
                case "down":
                    swipeUp(duration);
                    break;
                case "left":
                    swipeRight(duration);
                    break;
                case "right":
                    swipeLeft(duration);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid direction: " + direction + ". Must be one of: up, down, left, right");
            }
            return null;
        }, direction, velocity);
    }

    @NetatKeyword(
            name = "swipeOnElement",
            description = "Thực hiện swipe trên một element cụ thể theo hướng chỉ định. " +
                    "Khác với swipe toàn màn hình, keyword này chỉ swipe trong phạm vi element. " +
                    "Hữu ích cho việc swipe trong ScrollView, ListView, hoặc element có scroll riêng. " +
                    "Swipe sẽ diễn ra từ trung tâm element theo hướng chỉ định.",
            category = "Mobile",
            subCategory = "Gesture",
            parameters = {
                    "uiObject: ObjectUI - Element cần swipe trên đó",
                    "direction: String - Hướng swipe (\"up\", \"down\", \"left\", \"right\")",
                    "durationInMs: int - Thời gian thực hiện swipe (ms)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Swipe down trong một ScrollView cụ thể\n" +
                    "mobileKeyword.swipeOnElement(scrollView, \"down\", 500);\n\n" +
                    "// Swipe left trong horizontal RecyclerView\n" +
                    "mobileKeyword.swipeOnElement(horizontalList, \"left\", 400);\n\n" +
                    "// Swipe up nhanh trong container\n" +
                    "mobileKeyword.swipeOnElement(container, \"up\", 300);\n\n" +
                    "// Swipe right chậm trong gallery\n" +
                    "mobileKeyword.swipeOnElement(gallery, \"right\", 800);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Element phải hiển thị và có kích thước đủ lớn để swipe. " +
                    "Swipe diễn ra trong phạm vi 80%-20% của element. " +
                    "Direction không phân biệt hoa thường. " +
                    "Có thể throw ElementNotVisibleException nếu element không hiển thị, " +
                    "IllegalArgumentException nếu direction không hợp lệ, " +
                    "hoặc ElementNotInteractableException nếu element quá nhỏ để swipe."
    )
    public void swipeOnElement(ObjectUI uiObject, String direction, int durationInMs) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            Point location = element.getLocation();
            Dimension size = element.getSize();

            int centerX = location.getX() + size.getWidth() / 2;
            int centerY = location.getY() + size.getHeight() / 2;

            int startX, startY, endX, endY;
            String dir = direction.toLowerCase();

            switch (dir) {
                case "up":
                    startX = centerX;
                    startY = location.getY() + (int) (size.getHeight() * 0.8);
                    endX = centerX;
                    endY = location.getY() + (int) (size.getHeight() * 0.2);
                    break;
                case "down":
                    startX = centerX;
                    startY = location.getY() + (int) (size.getHeight() * 0.2);
                    endX = centerX;
                    endY = location.getY() + (int) (size.getHeight() * 0.8);
                    break;
                case "left":
                    startX = location.getX() + (int) (size.getWidth() * 0.8);
                    startY = centerY;
                    endX = location.getX() + (int) (size.getWidth() * 0.2);
                    endY = centerY;
                    break;
                case "right":
                    startX = location.getX() + (int) (size.getWidth() * 0.2);
                    startY = centerY;
                    endX = location.getX() + (int) (size.getWidth() * 0.8);
                    endY = centerY;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid direction: " + direction + ". Must be one of: up, down, left, right");
            }

            swipe(startX, startY, endX, endY, durationInMs);
            return null;
        }, uiObject, direction, durationInMs);
    }



        /**
         * Scrolls to find an element by its text and then taps on it.
         * This method leverages the powerful `scrollToText` which sequentially searches
         * through `text`, `content-desc`, and `resource-id` attributes.
         *
         * @param textToFind The text to locate within the element's attributes.
         */
        @NetatKeyword(
                name = "tapElementWithText",
                description = "Cuộn để tìm và chạm vào một phần tử dựa trên văn bản hiển thị. Keyword này tự động tìm kiếm qua các thuộc tính `text`, `content-desc`, và `resource-id` để xác định vị trí phần tử.",
                category = "Mobile",
                subCategory = "Gesture",
                parameters = {"String: textToFind - Văn bản cần tìm trong các thuộc tính của phần tử."},
                example = "mobileKeyword.tapElementWithText(\"Thanh toán\");"
        )
        public void tapElementWithText(String textToFind) {
            execute(() -> {
                WebElement element = scrollToText(textToFind); // Assumes scrollToText is defined in the class
                element.click();
                return null;
            }, textToFind);
        }

        @NetatKeyword(
                name = "getText",
                description = "Lấy văn bản hiển thị từ một phần tử. Tự động kiểm tra và trả về giá trị từ các thuộc tính theo thứ tự ưu tiên: `text`, `content-desc`, `label`.",
                category = "Mobile",
                subCategory = "Getter",
                parameters = {"ObjectUI: uiObject - Phần tử UI cần lấy văn bản."},
                returnValue = "String: Văn bản của phần tử. Trả về chuỗi rỗng nếu không có thuộc tính nào chứa văn bản.",
                example = "String welcomeText = mobileKeyword.getText(welcomeMessageObject);"
        )
        public String getText(ObjectUI uiObject) {
            return super.getText(uiObject);
        }

        @NetatKeyword(
                name = "verifyElementVisibleWithText",
                description = "Xác minh một phần tử chứa văn bản chỉ định đang hiển thị trên màn hình. Tự động cuộn để tìm kiếm qua các thuộc tính `text`, `content-desc`, và `resource-id`.",
                category = "Mobile",
                subCategory = "Assertion",
                parameters = {"String: textToFind - Văn bản cần xác minh sự tồn tại trên màn hình."},
                example = "mobileKeyword.verifyElementVisibleWithText(\"Đăng nhập thành công\");"
        )
        public void verifyElementVisibleWithText(String textToFind) {
            execute(() -> {
                scrollToText(textToFind); // The act of finding the element serves as verification
                return null;
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

    @NetatKeyword(
            name = "doubleTap",
            description = "Thực hiện hành động double tap (chạm nhanh 2 lần) vào một phần tử trên màn hình. " +
                    "Hữu ích cho các thao tác như zoom in/out trên ảnh, chọn text, hoặc các tương tác đặc biệt yêu cầu double tap. " +
                    "Phương thức sẽ tự động đợi phần tử hiển thị và có thể tương tác trước khi thực hiện.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần double tap"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Double tap vào ảnh để zoom in\n" +
                    "mobileKeyword.doubleTap(imageView);\n\n" +
                    "// Double tap vào text để chọn từ\n" +
                    "mobileKeyword.doubleTap(textElement);\n\n" +
                    "// Double tap vào map để zoom\n" +
                    "mobileKeyword.doubleTap(mapView);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Phần tử UI cần tương tác phải hiển thị trên màn hình. " +
                    "Khoảng thời gian giữa 2 lần tap được tối ưu tự động (khoảng 100ms). " +
                    "Có thể throw ElementNotVisibleException nếu phần tử không hiển thị, " +
                    "hoặc NoSuchElementException nếu không tìm thấy phần tử."
    )
    public void doubleTap(ObjectUI uiObject) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            Point location = element.getLocation();
            Dimension size = element.getSize();
            int centerX = location.getX() + size.getWidth() / 2;
            int centerY = location.getY() + size.getHeight() / 2;

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence doubleTapSequence = new Sequence(finger, 1);

            // First tap
            doubleTapSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY));
            doubleTapSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            doubleTapSequence.addAction(new Pause(finger, Duration.ofMillis(50)));
            doubleTapSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            // Short pause between taps
            doubleTapSequence.addAction(new Pause(finger, Duration.ofMillis(100)));

            // Second tap
            doubleTapSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            doubleTapSequence.addAction(new Pause(finger, Duration.ofMillis(50)));
            doubleTapSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            getAppiumDriver().perform(Collections.singletonList(doubleTapSequence));
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "doubleTapByCoordinates",
            description = "Thực hiện hành động double tap (chạm nhanh 2 lần) tại tọa độ cụ thể trên màn hình. " +
                    "Hữu ích khi cần double tap tại vị trí chính xác mà không cần locator của element. " +
                    "Tọa độ được tính từ góc trên bên trái của màn hình (0,0).",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "x: int - Tọa độ X (pixel từ trái sang phải)",
                    "y: int - Tọa độ Y (pixel từ trên xuống dưới)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Double tap tại trung tâm màn hình để zoom\n" +
                    "mobileKeyword.doubleTapByCoordinates(540, 960);\n\n" +
                    "// Double tap tại vị trí cụ thể trên map\n" +
                    "mobileKeyword.doubleTapByCoordinates(200, 400);\n\n" +
                    "// Double tap sau khi tính toán tọa độ động\n" +
                    "int centerX = screenWidth / 2;\n" +
                    "int centerY = screenHeight / 2;\n" +
                    "mobileKeyword.doubleTapByCoordinates(centerX, centerY);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Tọa độ phải nằm trong phạm vi màn hình thiết bị. " +
                    "Khoảng thời gian giữa 2 lần tap được tối ưu tự động (khoảng 100ms). " +
                    "Có thể throw InvalidCoordinatesException nếu tọa độ nằm ngoài màn hình."
    )
    public void doubleTapByCoordinates(int x, int y) {
        execute(() -> {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence doubleTapSequence = new Sequence(finger, 1);

            // First tap
            doubleTapSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            doubleTapSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            doubleTapSequence.addAction(new Pause(finger, Duration.ofMillis(50)));
            doubleTapSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            // Short pause between taps
            doubleTapSequence.addAction(new Pause(finger, Duration.ofMillis(100)));

            // Second tap
            doubleTapSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            doubleTapSequence.addAction(new Pause(finger, Duration.ofMillis(50)));
            doubleTapSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            getAppiumDriver().perform(Collections.singletonList(doubleTapSequence));
            return null;
        }, x, y);
    }

    @NetatKeyword(
            name = "pinch",
            description = "Thực hiện gesture pinch (thu nhỏ/zoom out) trên một phần tử. " +
                    "Mô phỏng hành động 2 ngón tay chụm lại với nhau để zoom out ảnh, map, hoặc nội dung có thể phóng to/thu nhỏ. " +
                    "Scale càng nhỏ thì zoom out càng mạnh (0.1 = zoom out rất mạnh, 0.9 = zoom out nhẹ).",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần thực hiện pinch",
                    "scale: double - Tỷ lệ pinch (0.1 đến 0.9), giá trị nhỏ = zoom out mạnh hơn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Pinch để zoom out ảnh mạnh (scale = 0.3)\n" +
                    "mobileKeyword.pinch(imageView, 0.3);\n\n" +
                    "// Pinch nhẹ trên map (scale = 0.7)\n" +
                    "mobileKeyword.pinch(mapView, 0.7);\n\n" +
                    "// Pinch rất mạnh để zoom out tối đa\n" +
                    "mobileKeyword.pinch(imageView, 0.1);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Phần tử phải hỗ trợ gesture pinch/zoom. " +
                    "Scale phải nằm trong khoảng 0.1 đến 0.9 (ngoài khoảng này sẽ được điều chỉnh). " +
                    "Gesture sẽ thực hiện từ trung tâm phần tử. " +
                    "Có thể throw ElementNotInteractableException nếu phần tử không hỗ trợ pinch."
    )
    public void pinch(ObjectUI uiObject, double scale) {
        // Ensure scale is within valid range
        final double finalScale = (scale < 0.1) ? 0.1 : (scale > 0.9) ? 0.9 : scale;

        execute(() -> {
            WebElement element = findElement(uiObject);
            Point location = element.getLocation();
            Dimension size = element.getSize();
            int centerX = location.getX() + size.getWidth() / 2;
            int centerY = location.getY() + size.getHeight() / 2;

            // Calculate start and end positions for two fingers
            int offset = (int) (Math.min(size.getWidth(), size.getHeight()) / 2 * 0.8);
            int endOffset = (int) (offset * finalScale);

            // First finger (top)
            PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
            Sequence pinchSequence1 = new Sequence(finger1, 1);
            pinchSequence1.addAction(finger1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY - offset));
            pinchSequence1.addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinchSequence1.addAction(finger1.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), centerX, centerY - endOffset));
            pinchSequence1.addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            // Second finger (bottom)
            PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");
            Sequence pinchSequence2 = new Sequence(finger2, 1);
            pinchSequence2.addAction(finger2.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY + offset));
            pinchSequence2.addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinchSequence2.addAction(finger2.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), centerX, centerY + endOffset));
            pinchSequence2.addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            getAppiumDriver().perform(Arrays.asList(pinchSequence1, pinchSequence2));
            return null;
        }, uiObject, finalScale);
    }

    @NetatKeyword(
            name = "zoom",
            description = "Thực hiện gesture zoom in (phóng to) trên một phần tử. " +
                    "Mô phỏng hành động 2 ngón tay dạng ra để zoom in ảnh, map, hoặc nội dung có thể phóng to/thu nhỏ. " +
                    "Scale càng lớn thì zoom in càng mạnh (1.1 = zoom in nhẹ, 3.0 = zoom in rất mạnh).",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần thực hiện zoom",
                    "scale: double - Tỷ lệ zoom (1.1 đến 3.0), giá trị lớn = zoom in mạnh hơn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Zoom in ảnh mạnh (scale = 2.5)\n" +
                    "mobileKeyword.zoom(imageView, 2.5);\n\n" +
                    "// Zoom in nhẹ trên map (scale = 1.3)\n" +
                    "mobileKeyword.zoom(mapView, 1.3);\n\n" +
                    "// Zoom in tối đa\n" +
                    "mobileKeyword.zoom(imageView, 3.0);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Phần tử phải hỗ trợ gesture zoom. " +
                    "Scale phải nằm trong khoảng 1.1 đến 3.0 (ngoài khoảng này sẽ được điều chỉnh). " +
                    "Gesture sẽ thực hiện từ trung tâm phần tử. " +
                    "Có thể throw ElementNotInteractableException nếu phần tử không hỗ trợ zoom."
    )
    public void zoom(ObjectUI uiObject, double scale) {
        // Ensure scale is within valid range
        final double finalScale = (scale < 1.1) ? 1.1 : (scale > 3.0) ? 3.0 : scale;

        execute(() -> {
            WebElement element = findElement(uiObject);
            Point location = element.getLocation();
            Dimension size = element.getSize();
            int centerX = location.getX() + size.getWidth() / 2;
            int centerY = location.getY() + size.getHeight() / 2;

            // Calculate start and end positions for two fingers
            int startOffset = (int) (Math.min(size.getWidth(), size.getHeight()) / 2 * 0.2);
            int endOffset = (int) (startOffset * finalScale);

            // First finger (top)
            PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
            Sequence zoomSequence1 = new Sequence(finger1, 1);
            zoomSequence1.addAction(finger1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY - startOffset));
            zoomSequence1.addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            zoomSequence1.addAction(finger1.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), centerX, centerY - endOffset));
            zoomSequence1.addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            // Second finger (bottom)
            PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");
            Sequence zoomSequence2 = new Sequence(finger2, 1);
            zoomSequence2.addAction(finger2.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY + startOffset));
            zoomSequence2.addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            zoomSequence2.addAction(finger2.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), centerX, centerY + endOffset));
            zoomSequence2.addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            getAppiumDriver().perform(Arrays.asList(zoomSequence1, zoomSequence2));
            return null;
        }, uiObject, finalScale);
    }

    @NetatKeyword(
            name = "tapAndHold",
            description = "Thực hiện hành động tap và giữ tại một phần tử, sau đó kéo đến tọa độ đích. " +
                    "Hữu ích cho các thao tác drag and drop tùy chỉnh, kéo slider, hoặc các gesture phức tạp yêu cầu tap-hold-drag. " +
                    "Khác với dragAndDrop, keyword này cho phép chỉ định tọa độ đích chính xác thay vì element đích.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần tap và giữ",
                    "x: int - Tọa độ X đích (pixel từ trái sang phải)",
                    "y: int - Tọa độ Y đích (pixel từ trên xuống dưới)",
                    "durationInSeconds: int - Thời gian giữ trước khi kéo (giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kéo slider đến vị trí cụ thể\n" +
                    "mobileKeyword.tapAndHold(sliderThumb, 800, 500, 1);\n\n" +
                    "// Kéo item trong list đến vị trí mới\n" +
                    "mobileKeyword.tapAndHold(listItem, 400, 800, 2);\n\n" +
                    "// Drag element đến góc màn hình\n" +
                    "mobileKeyword.tapAndHold(draggableElement, 100, 100, 1);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Phần tử phải hỗ trợ drag gesture. " +
                    "Tọa độ đích phải nằm trong phạm vi màn hình. " +
                    "Duration khuyến nghị từ 1-3 giây để đảm bảo gesture được nhận diện đúng. " +
                    "Có thể throw ElementNotInteractableException nếu phần tử không hỗ trợ drag."
    )
    public void tapAndHold(ObjectUI uiObject, int x, int y, int durationInSeconds) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            Point location = element.getLocation();
            Dimension size = element.getSize();
            int startX = location.getX() + size.getWidth() / 2;
            int startY = location.getY() + size.getHeight() / 2;

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence dragSequence = new Sequence(finger, 1);

            // Move to element center
            dragSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
            // Tap and hold
            dragSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            dragSequence.addAction(new Pause(finger, Duration.ofSeconds(durationInSeconds)));
            // Drag to destination
            dragSequence.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), x, y));
            // Release
            dragSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            getAppiumDriver().perform(Collections.singletonList(dragSequence));
            return null;
        }, uiObject, x, y, durationInSeconds);
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
        public void waitForText(ObjectUI uiObject, String expectedText, int timeoutInSeconds) {
            execute(() -> {
                WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
                wait.until(ExpectedConditions.textToBePresentInElement(findElement(uiObject), expectedText));
                return null;
            }, uiObject, expectedText, timeoutInSeconds);
        }

    @NetatKeyword(
            name = "waitForElementCount",
            description = "Chờ đợi cho đến khi số lượng phần tử khớp với locator đạt đến số lượng mong đợi. " +
                    "Hữu ích khi cần đợi danh sách load đủ số item, hoặc verify số lượng element sau khi thao tác. " +
                    "Nếu timeout, sẽ ném TimeoutException.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - Đối tượng UI đại diện cho các element cần đếm",
                    "count: int - Số lượng element mong đợi",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Đợi danh sách có đúng 10 items\n" +
                    "mobileKeyword.waitForElementCount(listItems, 10, 15);\n\n" +
                    "// Đợi sau khi xóa item\n" +
                    "int currentCount = mobileKeyword.getElementCount(items);\n" +
                    "mobileKeyword.tap(deleteButton);\n" +
                    "mobileKeyword.waitForElementCount(items, currentCount - 1, 10);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Có thể throw TimeoutException nếu số lượng không đạt được trong thời gian chờ."
    )
    public void waitForElementCount(ObjectUI uiObject, int count, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                List<WebElement> elements = findElements(uiObject);
                return elements.size() == count;
            });
            return null;
        }, uiObject, count, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForAttributeValue",
            description = "Chờ đợi cho đến khi attribute của element có giá trị mong đợi. " +
                    "Hữu ích khi cần đợi trạng thái thay đổi (enabled/disabled), class thay đổi, hoặc attribute động khác. " +
                    "Nếu timeout, sẽ ném TimeoutException.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - Element cần kiểm tra attribute",
                    "attributeName: String - Tên attribute cần kiểm tra",
                    "expectedValue: String - Giá trị mong đợi của attribute",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Đợi button enabled\n" +
                    "mobileKeyword.waitForAttributeValue(submitButton, \"enabled\", \"true\", 10);\n\n" +
                    "// Đợi element có class cụ thể\n" +
                    "mobileKeyword.waitForAttributeValue(statusLabel, \"class\", \"success\", 5);\n\n" +
                    "// Đợi progress bar complete\n" +
                    "mobileKeyword.waitForAttributeValue(progressBar, \"value\", \"100\", 30);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "ExpectedValue so sánh chính xác (case-sensitive). " +
                    "Có thể throw TimeoutException nếu attribute không đạt giá trị trong thời gian chờ."
    )
    public void waitForAttributeValue(ObjectUI uiObject, String attributeName, String expectedValue, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                WebElement element = findElement(uiObject);
                String actualValue = element.getAttribute(attributeName);
                return expectedValue.equals(actualValue);
            });
            return null;
        }, uiObject, attributeName, expectedValue, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForEnabled",
            description = "Chờ đợi cho đến khi element được enabled (có thể tương tác). " +
                    "Hữu ích khi button/field bị disabled và cần đợi enable sau khi điều kiện đáp ứng. " +
                    "Nếu timeout, sẽ ném TimeoutException.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - Element cần chờ enabled",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Đợi submit button enabled sau khi validate form\n" +
                    "mobileKeyword.sendText(emailField, \"test@example.com\");\n" +
                    "mobileKeyword.sendText(passwordField, \"Password123\");\n" +
                    "mobileKeyword.waitForEnabled(submitButton, 10);\n" +
                    "mobileKeyword.tap(submitButton);\n\n" +
                    "// Đợi next button enabled trong wizard\n" +
                    "mobileKeyword.tap(agreeCheckbox);\n" +
                    "mobileKeyword.waitForEnabled(nextButton, 5);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Kiểm tra thuộc tính 'enabled' của element. " +
                    "Có thể throw TimeoutException nếu element không enabled trong thời gian chờ."
    )
    public void waitForEnabled(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(ExpectedConditions.elementToBeClickable(findElement(uiObject)));
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForDisabled",
            description = "Chờ đợi cho đến khi element bị disabled (không thể tương tác). " +
                    "Hữu ích khi cần verify button bị disable sau khi submit, hoặc field bị disable sau khi chọn option. " +
                    "Nếu timeout, sẽ ném TimeoutException.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "uiObject: ObjectUI - Element cần chờ disabled",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Đợi submit button disabled sau khi click\n" +
                    "mobileKeyword.tap(submitButton);\n" +
                    "mobileKeyword.waitForDisabled(submitButton, 5);\n\n" +
                    "// Đợi input field disabled sau khi lock\n" +
                    "mobileKeyword.tap(lockButton);\n" +
                    "mobileKeyword.waitForDisabled(inputField, 3);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Kiểm tra thuộc tính 'enabled' = false của element. " +
                    "Có thể throw TimeoutException nếu element không disabled trong thời gian chờ."
    )
    public void waitForDisabled(ObjectUI uiObject, int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            wait.until(driver -> {
                WebElement element = findElement(uiObject);
                return !element.isEnabled();
            });
            return null;
        }, uiObject, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "waitForAppToLoad",
            description = "Chờ đợi app load hoàn tất bằng cách đợi activity stable (Android) hoặc check app state (iOS). " +
                    "Hữu ích sau khi launch app, navigate sang screen mới, hoặc sau khi background/foreground. " +
                    "Timeout mặc định là thời gian chờ được chỉ định.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "timeoutInSeconds: int - Thời gian chờ tối đa (giây)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Đợi app load sau launch\n" +
                    "mobileKeyword.launchApp();\n" +
                    "mobileKeyword.waitForAppToLoad(15);\n\n" +
                    "// Đợi app load sau khi back từ background\n" +
                    "mobileKeyword.backgroundApp(5);\n" +
                    "mobileKeyword.waitForAppToLoad(10);\n\n" +
                    "// Đợi sau navigate\n" +
                    "mobileKeyword.tap(menuItem);\n" +
                    "mobileKeyword.waitForAppToLoad(8);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Android: Đợi activity name stable. iOS: Đợi app ở foreground state. " +
                    "Có thể throw TimeoutException nếu app không load trong thời gian chờ."
    )
    public void waitForAppToLoad(int timeoutInSeconds) {
        execute(() -> {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(timeoutInSeconds));
            AppiumDriver driver = getAppiumDriver();

            if (driver instanceof AndroidDriver) {
                // Wait for activity to be stable
                wait.until(d -> {
                    try {
                        String currentActivity = ((AndroidDriver) d).currentActivity();
                        Thread.sleep(500);
                        String afterActivity = ((AndroidDriver) d).currentActivity();
                        return currentActivity != null && currentActivity.equals(afterActivity);
                    } catch (Exception e) {
                        return false;
                    }
                });
            } else if (driver instanceof IOSDriver) {
                // Wait for app to be in foreground
                wait.until(d -> {
                    try {
                        String bundleId = (String) driver.getCapabilities().getCapability("bundleId");
                        if (bundleId != null) {
                            int state = ((InteractsWithApps) driver).queryAppState(bundleId).ordinal();
                            return state == 4; // RUNNING_IN_FOREGROUND
                        }
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
            }
            return null;
        }, timeoutInSeconds);
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
                        "partialText: String - Chuỗi con cần tìm trong văn bản của phần tử",
                        "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
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
        public void assertTextContains(ObjectUI uiObject, String partialText,String... customMessages) {
            super.performTextContainsAssertion(uiObject, partialText, false,customMessages);
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
                    "uiObject: ObjectUI - Phần tử cần kiểm tra trạng thái",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra rằng tùy chọn chưa được chọn ban đầu\n" +
                    "mobileKeyword.assertNotChecked(optionalFeatureCheckbox);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertNotChecked(optionalFeatureCheckbox, \n" +
                    "    \"Checkbox tính năng tùy chọn phải ở trạng thái chưa chọn khi khởi tạo\");\n\n" +
                    "// Xác minh các radio button khác không được chọn\n" +
                    "mobileKeyword.tap(option1RadioButton);\n" +
                    "mobileKeyword.assertChecked(option1RadioButton);\n" +
                    "mobileKeyword.assertNotChecked(option2RadioButton, \n" +
                    "    \"Option 2 phải không được chọn khi Option 1 đã được chọn\");\n" +
                    "mobileKeyword.assertNotChecked(option3RadioButton, \n" +
                    "    \"Option 3 phải không được chọn khi Option 1 đã được chọn\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính 'checked', và phải là loại có thể chọn/bỏ chọn (checkbox, radio button, switch). " +
                    "Có thể throw AssertionError nếu phần tử đang ở trạng thái được chọn/bật, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu không thể lấy thuộc tính 'checked' của phần tử."
    )
    public void assertNotChecked(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            boolean isChecked = Boolean.parseBoolean(element.getAttribute("checked"));

            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' is in checked/enabled state.";

            Assert.assertFalse(isChecked, message);
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
                    "uiObject: ObjectUI - Phần tử cần kiểm tra trạng thái",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra nút đăng nhập được kích hoạt sau khi nhập thông tin\n" +
                    "mobileKeyword.sendText(usernameInput, \"user@example.com\");\n" +
                    "mobileKeyword.sendText(passwordInput, \"password123\");\n" +
                    "mobileKeyword.assertEnabled(loginButton);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertEnabled(loginButton, \n" +
                    "    \"Nút đăng nhập phải được kích hoạt sau khi nhập đầy đủ thông tin\");\n\n" +
                    "// Xác minh nút tiếp tục được kích hoạt sau khi đồng ý điều khoản\n" +
                    "mobileKeyword.tap(agreeToTermsCheckbox);\n" +
                    "mobileKeyword.assertEnabled(continueButton, \n" +
                    "    \"Nút tiếp tục phải được kích hoạt sau khi đồng ý điều khoản\");\n\n" +
                    "// Kiểm tra các input field có thể nhập liệu\n" +
                    "mobileKeyword.assertEnabled(emailInput, \n" +
                    "    \"Trường email phải cho phép nhập liệu\");\n" +
                    "mobileKeyword.assertEnabled(phoneInput, \n" +
                    "    \"Trường số điện thoại phải cho phép nhập liệu\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính 'enabled', và phải là loại có thể được kích hoạt/vô hiệu hóa (button, input, etc.). " +
                    "Có thể throw AssertionError nếu phần tử đang ở trạng thái bị vô hiệu hóa (disabled), " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu không thể lấy thuộc tính 'enabled' của phần tử."
    )
    public void assertEnabled(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            try {
                // Gọi phương thức logic từ lớp cha
                super.performStateAssertion(uiObject, true, false);
            } catch (AssertionError e) {
                if (customMessage.length > 0) {
                    throw new AssertionError(customMessage[0], e);
                }
                throw e;
            }
            return null;
        }, uiObject);
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
                    "timeoutInSeconds: int - Thời gian tối đa (giây) để đợi và xác nhận phần tử không xuất hiện",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thông báo lỗi không xuất hiện sau khi nhập đúng thông tin\n" +
                    "mobileKeyword.assertElementNotPresent(errorMessage, 3);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertElementNotPresent(errorMessage, 3, \n" +
                    "    \"Thông báo lỗi không được xuất hiện khi nhập thông tin hợp lệ\");\n\n" +
                    "// Xác minh màn hình loading đã biến mất sau khi tải xong\n" +
                    "mobileKeyword.assertElementNotPresent(loadingSpinner, 10, \n" +
                    "    \"Loading spinner phải biến mất sau khi tải dữ liệu hoàn tất\");\n\n" +
                    "// Kiểm tra popup không xuất hiện\n" +
                    "mobileKeyword.assertElementNotPresent(adPopup, 5, \n" +
                    "    \"Popup quảng cáo không được hiển thị trên màn hình chính\");\n" +
                    "mobileKeyword.assertElementNotPresent(updateDialog, 3, \n" +
                    "    \"Dialog cập nhật không được hiển thị khi app đã là phiên bản mới nhất\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Locator của phần tử cần kiểm tra phải hợp lệ. " +
                    "Có thể throw AssertionError nếu phần tử xuất hiện trong khoảng thời gian chờ, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với trình điều khiển."
    )
    public void assertElementNotPresent(ObjectUI uiObject, int timeoutInSeconds, String... customMessage) {
        execute(() -> {
            boolean isPresent = isElementPresent(uiObject, timeoutInSeconds);

            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' is still present after " + timeoutInSeconds + " seconds.";

            Assert.assertFalse(isPresent, message);
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
                    "}\n\n" +
                    "// Xử lý điều kiện phức tạp với nhiều element\n" +
                    "boolean hasWelcomeScreen = mobileKeyword.isElementPresent(welcomeTitle, 2);\n" +
                    "boolean hasLoginForm = mobileKeyword.isElementPresent(loginForm, 2);\n" +
                    "if (hasWelcomeScreen) {\n" +
                    "    mobileKeyword.tap(getStartedButton);\n" +
                    "} else if (hasLoginForm) {\n" +
                    "    // User đã qua welcome screen, tiến hành đăng nhập\n" +
                    "    mobileKeyword.sendText(usernameInput, \"testuser\");\n" +
                    "}\n\n" +
                    "// Kiểm tra optional elements\n" +
                    "if (mobileKeyword.isElementPresent(tutorialOverlay, 1)) {\n" +
                    "    mobileKeyword.tap(skipTutorialButton);\n" +
                    "}",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Locator của phần tử cần kiểm tra phải hợp lệ. " +
                    "Có thể throw WebDriverException nếu có lỗi khi tương tác với trình điều khiển. " +
                    "Method này không ném AssertionError, chỉ trả về boolean để xử lý logic."
    )
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
                    "uiObject: ObjectUI - Phần tử cần kiểm tra trạng thái",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra nút đăng nhập bị vô hiệu hóa khi chưa nhập thông tin\n" +
                    "mobileKeyword.assertDisabled(loginButton);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertDisabled(loginButton, \n" +
                    "    \"Nút đăng nhập phải bị vô hiệu hóa khi chưa nhập đầy đủ thông tin\");\n\n" +
                    "// Xác minh trường nhập số tiền bị vô hiệu hóa khi chọn số tiền cố định\n" +
                    "mobileKeyword.tap(fixedAmountOption);\n" +
                    "mobileKeyword.assertDisabled(amountInput, \n" +
                    "    \"Trường nhập số tiền phải bị vô hiệu hóa khi chọn số tiền cố định\");\n\n" +
                    "// Kiểm tra các button bị disable trong form\n" +
                    "mobileKeyword.assertDisabled(submitButton, \n" +
                    "    \"Nút gửi phải bị vô hiệu hóa khi form chưa hợp lệ\");\n" +
                    "mobileKeyword.assertDisabled(saveButton, \n" +
                    "    \"Nút lưu phải bị vô hiệu hóa khi không có thay đổi nào\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính 'enabled', và phải là loại có thể được kích hoạt/vô hiệu hóa (button, input, etc.). " +
                    "Có thể throw AssertionError nếu phần tử đang ở trạng thái được kích hoạt (enabled), " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu không thể lấy thuộc tính 'enabled' của phần tử."
    )
    public void assertDisabled(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            try {
                super.performStateAssertion(uiObject, false, false);
            } catch (AssertionError e) {
                if (customMessage.length > 0) {
                    throw new AssertionError(customMessage[0], e);
                }
                throw e;
            }
            return null;
        }, uiObject);
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
                    "expectedValue: String - Giá trị mong đợi của thuộc tính",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thuộc tính content-desc của nút\n" +
                    "mobileKeyword.assertAttributeEquals(menuButton, \"content-desc\", \"Menu chính\");\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertAttributeEquals(menuButton, \"content-desc\", \"Menu chính\", \n" +
                    "    \"Content description của nút menu phải là 'Menu chính'\");\n\n" +
                    "// Xác minh resource-id của một phần tử\n" +
                    "mobileKeyword.assertAttributeEquals(loginButton, \"resource-id\", \n" +
                    "    \"com.example.myapp:id/login_button\", \n" +
                    "    \"Resource ID của nút đăng nhập phải đúng theo thiết kế\");\n\n" +
                    "// Kiểm tra thuộc tính text\n" +
                    "mobileKeyword.assertAttributeEquals(titleLabel, \"text\", \"Đăng nhập\", \n" +
                    "    \"Tiêu đề màn hình phải hiển thị 'Đăng nhập'\");\n" +
                    "mobileKeyword.assertAttributeEquals(submitButton, \"text\", \"Gửi\", \n" +
                    "    \"Text của nút submit phải là 'Gửi'\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại, thuộc tính cần kiểm tra phải tồn tại trên phần tử, " +
                    "và cần biết chính xác tên thuộc tính theo nền tảng (Android/iOS có thể khác nhau). " +
                    "Có thể throw AssertionError nếu giá trị thuộc tính không khớp với giá trị mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "hoặc WebDriverException nếu không thể lấy thuộc tính của phần tử."
    )
    public void assertAttributeEquals(ObjectUI uiObject, String attributeName, String expectedValue, String... customMessage) {
        execute(() -> {
            try {
                super.performAttributeAssertion(uiObject, attributeName, expectedValue, false);
            } catch (AssertionError e) {
                if (customMessage.length > 0) {
                    throw new AssertionError(customMessage[0], e);
                }
                throw e;
            }
            return null;
        }, uiObject, attributeName, expectedValue);
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
                    "partialValue: String - Chuỗi con cần tìm trong giá trị thuộc tính",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thuộc tính content-desc có chứa từ khóa\n" +
                    "mobileKeyword.assertAttributeContains(productItem, \"content-desc\", \"iPhone\");\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertAttributeContains(productItem, \"content-desc\", \"iPhone\", \n" +
                    "    \"Content description của sản phẩm phải chứa từ 'iPhone'\");\n\n" +
                    "// Xác minh resource-id có chứa phần nhất định\n" +
                    "mobileKeyword.assertAttributeContains(anyElement, \"resource-id\", \"button_\", \n" +
                    "    \"Resource ID phải chứa prefix 'button_' theo quy ước đặt tên\");\n\n" +
                    "// Kiểm tra class name chứa thông tin cần thiết\n" +
                    "mobileKeyword.assertAttributeContains(inputField, \"class\", \"EditText\", \n" +
                    "    \"Class name phải chứa 'EditText' để xác nhận đây là trường nhập liệu\");\n" +
                    "mobileKeyword.assertAttributeContains(imageView, \"class\", \"ImageView\", \n" +
                    "    \"Class name phải chứa 'ImageView' để xác nhận đây là thành phần hiển thị hình ảnh\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại, thuộc tính cần kiểm tra phải tồn tại trên phần tử, " +
                    "và cần biết chính xác tên thuộc tính theo nền tảng (Android/iOS có thể khác nhau). " +
                    "Có thể throw AssertionError nếu giá trị thuộc tính không chứa chuỗi con mong đợi, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu không thể lấy thuộc tính của phần tử, hoặc NullPointerException nếu giá trị thuộc tính là null."
    )
    public void assertAttributeContains(ObjectUI uiObject, String attributeName, String partialValue, String... customMessage) {
        execute(() -> {
            try {
                super.performAttributeContainsAssertion(uiObject, attributeName, partialValue, false);
            } catch (AssertionError e) {
                if (customMessage.length > 0) {
                    throw new AssertionError(customMessage[0], e);
                }
                throw e;
            }
            return null;
        }, uiObject, attributeName, partialValue);
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
                    "expectedCount: int - Số lượng phần tử mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra số lượng sản phẩm trong giỏ hàng\n" +
                    "mobileKeyword.assertElementCount(cartItems, 3);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertElementCount(cartItems, 3, \n" +
                    "    \"Giỏ hàng phải chứa đúng 3 sản phẩm sau khi thêm\");\n\n" +
                    "// Xác minh danh sách rỗng sau khi xóa\n" +
                    "mobileKeyword.tap(clearAllButton);\n" +
                    "mobileKeyword.assertElementCount(listItems, 0, \n" +
                    "    \"Danh sách phải rỗng sau khi nhấn nút xóa tất cả\");\n\n" +
                    "// Kiểm tra số lượng menu items\n" +
                    "mobileKeyword.assertElementCount(menuItems, 5, \n" +
                    "    \"Menu chính phải có đúng 5 mục theo thiết kế\");\n" +
                    "// Kiểm tra số lượng notification\n" +
                    "mobileKeyword.assertElementCount(notifications, 2, \n" +
                    "    \"Phải có đúng 2 thông báo chưa đọc\");\n" +
                    "// Kiểm tra số lượng tabs\n" +
                    "mobileKeyword.assertElementCount(tabButtons, 4, \n" +
                    "    \"Bottom navigation phải có đúng 4 tab\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Locator của phần tử phải hợp lệ và có thể tìm thấy nhiều phần tử. " +
                    "Nếu mong đợi không tìm thấy phần tử nào (count = 0), locator vẫn phải hợp lệ. " +
                    "Có thể throw AssertionError nếu số lượng phần tử tìm thấy không khớp với số lượng mong đợi, " +
                    "WebDriverException nếu có lỗi khi tương tác với trình điều khiển, hoặc InvalidSelectorException nếu locator không hợp lệ."
    )
    public void assertElementCount(ObjectUI uiObject, int expectedCount, String... customMessage) {
        execute(() -> {
            List<WebElement> elements = findElements(uiObject);

            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: Expected to find " + expectedCount + " items, but found " + elements.size() + ".";

            Assert.assertEquals(elements.size(), expectedCount, message);
            return null;
        }, uiObject, expectedCount);
    }

    @NetatKeyword(
            name = "assertTextNotEquals",
            description = "Khẳng định rằng văn bản của element KHÔNG bằng giá trị mong đợi. " +
                    "Hữu ích để verify text đã thay đổi sau thao tác, hoặc đảm bảo không hiển thị giá trị cũ. " +
                    "So sánh chính xác và phân biệt hoa thường.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Element cần kiểm tra text",
                    "unexpectedText: String - Text không mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify text đã thay đổi sau update\n" +
                    "String oldText = mobileKeyword.getText(statusLabel);\n" +
                    "mobileKeyword.tap(updateButton);\n" +
                    "mobileKeyword.assertTextNotEquals(statusLabel, oldText, \"Status phải thay đổi sau update\");\n\n" +
                    "// Verify không hiển thị giá trị default\n" +
                    "mobileKeyword.assertTextNotEquals(inputField, \"placeholder text\");\n\n" +
                    "// Verify error message đã clear\n" +
                    "mobileKeyword.assertTextNotEquals(errorMessage, \"Invalid input\");",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "So sánh exact match, case-sensitive. " +
                    "Có thể throw AssertionError nếu text bằng unexpectedText."
    )
    public void assertTextNotEquals(ObjectUI uiObject, String unexpectedText, String... customMessage) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            String actualText = element.getText();

            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' should not have text '" + unexpectedText + "' but it does.";

            Assert.assertNotEquals(actualText, unexpectedText, message);
            return null;
        }, uiObject, unexpectedText);
    }

    @NetatKeyword(
            name = "assertElementNotVisible",
            description = "Khẳng định rằng element KHÔNG hiển thị trên màn hình. " +
                    "Element có thể tồn tại trong DOM nhưng bị ẩn (visibility=hidden, display=none). " +
                    "Hữu ích để verify element đã bị ẩn sau thao tác.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Element cần kiểm tra",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify loading spinner đã ẩn\n" +
                    "mobileKeyword.assertElementNotVisible(loadingSpinner, \"Loading spinner phải ẩn sau khi load xong\");\n\n" +
                    "// Verify error message đã ẩn\n" +
                    "mobileKeyword.tap(closeErrorButton);\n" +
                    "mobileKeyword.assertElementNotVisible(errorMessage);\n\n" +
                    "// Verify modal đã đóng\n" +
                    "mobileKeyword.tap(closeModalButton);\n" +
                    "mobileKeyword.assertElementNotVisible(modal);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Element phải tồn tại trong DOM nhưng không visible. " +
                    "Nếu element không tồn tại, sẽ pass assertion. " +
                    "Có thể throw AssertionError nếu element đang visible."
    )
    public void assertElementNotVisible(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            try {
                WebElement element = findElement(uiObject);
                boolean isVisible = element.isDisplayed();

                String message = customMessage.length > 0 ? customMessage[0] :
                        "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' should not be visible but it is.";

                Assert.assertFalse(isVisible, message);
            } catch (NoSuchElementException e) {
                // Element not found = not visible, assertion passes
                logger.debug("Element not found, considered as not visible: " + uiObject.getName());
            }
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertSelected",
            description = "Khẳng định rằng element đang được selected/chọn. " +
                    "Áp dụng cho dropdown option, list item, hoặc selectable element. " +
                    "Khác với assertChecked (dành cho checkbox/radio), keyword này cho selection state.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Element cần kiểm tra",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify option đã được chọn trong dropdown\n" +
                    "mobileKeyword.tap(dropdown);\n" +
                    "mobileKeyword.tap(option1);\n" +
                    "mobileKeyword.assertSelected(option1, \"Option 1 phải được chọn\");\n\n" +
                    "// Verify tab đang active\n" +
                    "mobileKeyword.tap(settingsTab);\n" +
                    "mobileKeyword.assertSelected(settingsTab);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Element phải hỗ trợ thuộc tính 'selected'. " +
                    "Có thể throw AssertionError nếu element không được selected."
    )
    public void assertSelected(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            boolean isSelected = element.isSelected();

            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' should be selected but it is not.";

            Assert.assertTrue(isSelected, message);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "assertNotSelected",
            description = "Khẳng định rằng element KHÔNG được selected/chọn. " +
                    "Áp dụng cho dropdown option, list item, hoặc selectable element. " +
                    "Hữu ích để verify option khác không được chọn khi chọn option mới.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "uiObject: ObjectUI - Element cần kiểm tra",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Verify option khác không được chọn\n" +
                    "mobileKeyword.tap(dropdown);\n" +
                    "mobileKeyword.tap(option1);\n" +
                    "mobileKeyword.assertSelected(option1);\n" +
                    "mobileKeyword.assertNotSelected(option2, \"Option 2 phải không được chọn\");\n" +
                    "mobileKeyword.assertNotSelected(option3, \"Option 3 phải không được chọn\");",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Element phải hỗ trợ thuộc tính 'selected'. " +
                    "Có thể throw AssertionError nếu element đang được selected."
    )
    public void assertNotSelected(ObjectUI uiObject, String... customMessage) {
        execute(() -> {
            WebElement element = findElement(uiObject);
            boolean isSelected = element.isSelected();

            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: Element '" + uiObject.getName() + "' should not be selected but it is.";

            Assert.assertFalse(isSelected, message);
            return null;
        }, uiObject);
    }

    @NetatKeyword(
            name = "verifyAttributeExists",
            description = "Verify (soft assertion) rằng attribute tồn tại trên element, không quan tâm giá trị. " +
                    "Khác với assert, verify không dừng test khi fail, chỉ log error. " +
                    "Hữu ích để kiểm tra element có attribute hay không.",
            category = "Mobile",
            subCategory = "Verification",
            parameters = {
                    "uiObject: ObjectUI - Element cần kiểm tra",
                    "attributeName: String - Tên attribute cần verify"
            },
            returnValue = "boolean - true nếu attribute tồn tại, false nếu không",
            example = "// Verify element có attribute 'enabled'\n" +
                    "boolean hasEnabled = mobileKeyword.verifyAttributeExists(button, \"enabled\");\n\n" +
                    "// Verify element có attribute 'value'\n" +
                    "boolean hasValue = mobileKeyword.verifyAttributeExists(inputField, \"value\");\n\n" +
                    "// Chain verify để check multiple attributes\n" +
                    "if (mobileKeyword.verifyAttributeExists(element, \"clickable\")) {\n" +
                    "    mobileKeyword.tap(element);\n" +
                    "}",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Đây là soft assertion, không throw exception khi fail. " +
                    "Attribute = null hoặc không tồn tại đều return false."
    )
    public boolean verifyAttributeExists(ObjectUI uiObject, String attributeName) {
        return execute(() -> {
            try {
                WebElement element = findElement(uiObject);
                String attributeValue = element.getAttribute(attributeName);
                boolean exists = attributeValue != null;

                if (!exists) {
                    logger.warn("SOFT VERIFY FAILED: Attribute '{}' does not exist on element '{}'",
                            attributeName, uiObject.getName());
                }
                return exists;
            } catch (Exception e) {
                logger.warn("SOFT VERIFY FAILED: Cannot check attribute '{}' on element '{}': {}",
                        attributeName, uiObject.getName(), e.getMessage());
                return false;
            }
        }, uiObject, attributeName);
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
                    "trimText: boolean - true để cắt khoảng trắng ở đầu/cuối trước khi so sánh, false để giữ nguyên",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra nội dung chào mừng, bỏ qua chữ hoa/thường và khoảng trắng\n" +
                    "mobileKeyword.assertTextWithOptions(welcomeMessage, \"  xin chào, Người Dùng \", true, true);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertTextWithOptions(welcomeMessage, \"  xin chào, Người Dùng \", true, true, \n" +
                    "    \"Thông báo chào mừng phải khớp với nội dung mong đợi (bỏ qua format)\");\n\n" +
                    "// Kiểm tra địa chỉ email không phân biệt chữ hoa/thường\n" +
                    "mobileKeyword.assertTextWithOptions(emailField, \"User@Example.com\", true, true, \n" +
                    "    \"Email hiển thị phải khớp không phân biệt chữ hoa/thường\");\n\n" +
                    "// Kiểm tra title với trim nhưng phân biệt case\n" +
                    "mobileKeyword.assertTextWithOptions(pageTitle, \" Trang Chủ \", false, true, \n" +
                    "    \"Tiêu đề trang phải là 'Trang Chủ' (phân biệt chữ hoa/thường)\");\n" +
                    "// Kiểm tra message với case-sensitive nhưng không trim\n" +
                    "mobileKeyword.assertTextWithOptions(statusMessage, \"  Đang xử lý...  \", false, false, \n" +
                    "    \"Thông báo trạng thái phải khớp chính xác bao gồm cả khoảng trắng\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Phần tử cần kiểm tra phải tồn tại và có thuộc tính văn bản (text), và phải hiển thị trên màn hình để có thể đọc văn bản. " +
                    "Có thể throw AssertionError nếu văn bản không khớp theo các tùy chọn đã chọn, " +
                    "NoSuchElementException nếu không tìm thấy phần tử, StaleElementReferenceException nếu phần tử không còn gắn với DOM, " +
                    "WebDriverException nếu không thể lấy văn bản của phần tử, hoặc NullPointerException nếu văn bản của phần tử là null và expectedText không phải null."
    )
    public void assertTextWithOptions(ObjectUI uiObject, String expectedText, boolean ignoreCase, boolean trimText, String... customMessage) {
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

                String message = customMessage.length > 0 ? customMessage[0] :
                        "HARD ASSERT FAILED: Text does not match (ignoring case). Expected: '" + realExpectedText + "', Actual: '" + actualText + "'";

                Assert.assertTrue(areEqual, message);
            } else {
                String message = customMessage.length > 0 ? customMessage[0] :
                        "HARD ASSERT FAILED: Text does not match.";

                Assert.assertEquals(actualText, realExpectedText, message);
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
                name = "getCurrentActivity",
                description = "Lấy tên Activity hiện tại đang được hiển thị trên màn hình Android",
                category = "Mobile",
                subCategory = "AppLifecycle",
                parameters = {},
                returnValue = "String - Tên của Activity hiện tại, null nếu không phải Android",
                example = "String activityName = getCurrentActivity();",
                note = "Keyword này chỉ hoạt động trên Android. Trên iOS sẽ trả về null và ghi log cảnh báo. Activity name thường có định dạng như 'com.example.MainActivity'."
        )
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
                        "int... index - Các vị trí của phần tử cần tap (bắt đầu từ 0)"
                },
                example = "mobileKeyword.tapElementByIndex(menuItems, 2,3,1); // Tap vào các phần tử thứ 3,4,2 trong danh sách"
        )
        public void tapElementByIndex(ObjectUI uiObject, int... index) {
            execute(() -> {
                List<WebElement> elements = findElements(uiObject);
                for (int i=0; i<index.length; i++) {
                    int indexToTap = index[i];
                    if (indexToTap >= 0 && indexToTap < elements.size()) {
                        elements.get(indexToTap).click(); // .click() được Appium tự động chuyển đổi thành .tap()
                        logger.info("Tap element at position " + indexToTap + " in the list " + uiObject.getName());
                    } else {
                        throw new IndexOutOfBoundsException(
                                String.format("Invalid index: %d. List has only %d elements.", indexToTap, elements.size())
                        );
                    }
                }
                return null;
            }, uiObject, index);
        }

    @NetatKeyword(
            name = "tapAllElement",
            description = "Thực hiện thao tác tap vào mọi phần tử trong danh sách",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "ObjectUI uiObject - Đối tượng UI đại diện cho danh sách phần tử",
            },
            example = "mobileKeyword.tapAllElement(menuItems); // Tap vào mọi phần tử trong danh sách"
    )
        public void tapAllElement(ObjectUI objectUI){
            execute(() -> {
                List<WebElement> elements = findElements(objectUI);
                for (int i=0; i<elements.size(); i++) {
                    elements.get(i).click();
                    logger.info("Tap element at position " + i+ " in the list " + objectUI.getName());
                }
                return null;
            }, objectUI);
        }

    @NetatKeyword(
            name = "openNotifications",
            description = "Mở thanh thông báo (notification shade) của hệ điều hành.",
            category = "Mobile",
            subCategory = "System",
            parameters = {},
            example = "mobileKeyword.openNotifications();"
    )
    public void openNotifications() {
        execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            String platform = driver.getCapabilities().getPlatformName().toString();

            // Logic được phân tách cho từng nền tảng
            if ("android".equalsIgnoreCase(platform)) {
                // Đối với Android, gọi trực tiếp phương thức từ AndroidDriver
                ((AndroidDriver) driver).openNotifications();
                logger.info("Opened notifications on Android.");
            } else { // Giả định là iOS
                // Đối với iOS, mô phỏng hành động vuốt từ trên xuống
                logger.info("Simulating swipe down to open notifications on iOS.");
                swipeDown(500); // Tái sử dụng keyword swipeDown đã có
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "waitForNotification",
            description = "Chờ cho đến khi một thông báo chứa đoạn văn bản cụ thể xuất hiện.",
            category = "Mobile",
            subCategory = "Wait",
            parameters = {
                    "containingText: String - Đoạn văn bản (tiêu đề hoặc nội dung) cần có trong thông báo.",
                    "timeoutInSeconds: int - Thời gian chờ tối đa."
            },
            example = "mobileKeyword.waitForNotification(\"Bạn có tin nhắn mới\", 15);"
    )
    public WebElement waitForNotification(String containingText, int timeoutInSeconds) {
        return execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            String platform = driver.getCapabilities().getPlatformName().toString();
            By locator;

            // Locator để tìm thông báo có thể khác nhau tùy vào phiên bản HĐH
            // Đây là một ví dụ phổ biến
            if ("android".equalsIgnoreCase(platform)) {
                locator = By.xpath("//*[@text='" + containingText + "' or contains(@text, '" + containingText + "')]");
            } else { // iOS
                locator = AppiumBy.iOSNsPredicateString("label CONTAINS '" + containingText + "' OR value CONTAINS '" + containingText + "'");
            }

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        }, containingText, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "assertNotificationText",
            description = "Kiểm tra văn bản của một thông báo. Tự động chờ thông báo xuất hiện trước khi kiểm tra. " +
                    "Phương thức này sẽ tìm kiếm thông báo dựa trên một phần văn bản chứa trong đó, sau đó so sánh toàn bộ nội dung " +
                    "với văn bản mong đợi. Hữu ích để xác minh nội dung thông báo push, toast message, hoặc system notification.",
            category = "Mobile",
            subCategory = "Assertion",
            parameters = {
                    "containingText: String - Đoạn văn bản dùng để tìm thông báo (có thể là một phần của thông báo)",
                    "expectedText: String - Văn bản đầy đủ mong đợi của thông báo",
                    "timeoutInSeconds: int - Thời gian chờ tối đa (tính bằng giây) để thông báo xuất hiện",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị, ném AssertionError nếu kiểm tra thất bại",
            example = "// Kiểm tra thông báo tin nhắn\n" +
                    "mobileKeyword.assertNotificationText(\"Tin nhắn từ A\", \"Bạn có muốn trả lời không?\", 15);\n\n" +
                    "// Với custom message\n" +
                    "mobileKeyword.assertNotificationText(\"Tin nhắn từ A\", \"Bạn có muốn trả lời không?\", 15, \n" +
                    "    \"Nội dung thông báo tin nhắn phải khớp với văn bản mong đợi\");\n\n" +
                    "// Kiểm tra thông báo email\n" +
                    "mobileKeyword.assertNotificationText(\"Email mới\", \"Bạn có 3 email chưa đọc từ work@company.com\", 10, \n" +
                    "    \"Thông báo email phải hiển thị đúng số lượng và người gửi\");\n\n" +
                    "// Kiểm tra thông báo hệ thống\n" +
                    "mobileKeyword.assertNotificationText(\"Cập nhật\", \"Ứng dụng đã được cập nhật thành công lên phiên bản 2.1.0\", 20, \n" +
                    "    \"Thông báo cập nhật phải hiển thị đúng phiên bản mới\");\n" +
                    "// Kiểm tra thông báo lỗi\n" +
                    "mobileKeyword.assertNotificationText(\"Lỗi kết nối\", \"Không thể kết nối đến máy chủ. Vui lòng thử lại sau.\", 5, \n" +
                    "    \"Thông báo lỗi phải hiển thị hướng dẫn rõ ràng cho người dùng\");\n" +
                    "// Kiểm tra thông báo thành công\n" +
                    "mobileKeyword.assertNotificationText(\"Thành công\", \"Giao dịch đã được xử lý thành công. Mã GD: TX123456\", 8, \n" +
                    "    \"Thông báo giao dịch thành công phải bao gồm mã giao dịch\");",
            note = "Áp dụng cho nền tảng Mobile. Thiết bị di động đã được kết nối và cấu hình đúng với Appium. " +
                    "Hệ thống phải có quyền truy cập vào notification panel hoặc thông báo của ứng dụng. " +
                    "containingText được sử dụng để định vị thông báo, sau đó toàn bộ nội dung sẽ được so sánh với expectedText. " +
                    "Có thể throw AssertionError nếu nội dung thông báo không khớp với văn bản mong đợi, " +
                    "TimeoutException nếu không tìm thấy thông báo chứa containingText trong thời gian chờ, " +
                    "NoSuchElementException nếu thông báo biến mất trước khi đọc được nội dung, " +
                    "hoặc WebDriverException nếu có lỗi khi tương tác với notification panel."
    )
    public void assertNotificationText(String containingText, String expectedText, int timeoutInSeconds, String... customMessage) {
        execute(() -> {
            WebElement notification = waitForNotification(containingText, timeoutInSeconds);
            String message = customMessage.length > 0 ? customMessage[0] :
                    "HARD ASSERT FAILED: The message content does not match.";
            Assert.assertEquals(notification.getText(), expectedText, message);
            return null;
        }, containingText, expectedText, timeoutInSeconds);
    }


    @NetatKeyword(
            name = "tapNotificationByText",
            description = "Chờ một thông báo xuất hiện và chạm vào nó.",
            category = "Mobile",
            subCategory = "Interaction",
            parameters = {
                    "containingText: String - Đoạn văn bản dùng để tìm và chạm vào thông báo.",
                    "timeoutInSeconds: int - Thời gian chờ tối đa."
            },
            example = "mobileKeyword.tapNotificationByText(\"Bạn có tin nhắn mới\", 15);"
    )
    public void tapNotificationByText(String containingText, int timeoutInSeconds) {
        execute(() -> {
            WebElement notification = waitForNotification(containingText, timeoutInSeconds);
            notification.click();
            return null;
        }, containingText, timeoutInSeconds);
    }

    @NetatKeyword(
            name = "getAttribute",
            description = "Lấy và trả về giá trị của một thuộc tính (attribute) cụ thể trên một phần tử.",
            category = "Mobile",
            subCategory = "Getter",
            parameters = {
                    "uiObject: ObjectUI - Phần tử cần lấy thuộc tính.",
                    "attributeName: String - Tên của thuộc tính cần lấy (ví dụ: 'content-desc', 'resource-id', 'checked')."
            },
            returnValue = "String|Giá trị của thuộc tính dưới dạng chuỗi.",
            example = "String description = mobileKeyword.getAttribute(menuButton, \"content-desc\");"
    )
    public String getAttribute(ObjectUI uiObject, String attributeName) {
        // Gọi lại phương thức logic đã được định nghĩa ở lớp cha
        return super.getAttribute(uiObject, attributeName);
    }

    /**
     * Locks the device screen for a specified duration.
     * Supports both Android and iOS platforms with automatic platform detection.
     *
     * @param seconds Optional duration in seconds to keep the device locked.
     *                If not provided, defaults to 1 second.
     */
    @NetatKeyword(
            name = "lockDevice",
            description = "Khóa màn hình thiết bị trong khoảng thời gian chỉ định. Tự động phát hiện nền tảng (Android/iOS) và áp dụng phương thức khóa phù hợp.",
            category = "Mobile",
            subCategory = "System",
            parameters = {"Integer... seconds - (Tùy chọn) Số giây giữ trạng thái khóa. Mặc định: 1 giây nếu không chỉ định."},
            note = "⚠️ LƯU Ý: Keyword này sẽ BLOCK thread trong thời gian khóa. Không nên sử dụng thời gian quá dài (>30s). Một số thiết bị có thể yêu cầu quyền đặc biệt để khóa màn hình.",
            example = "mobileKeyword.lockDevice(); // Khóa 1 giây\nmobileKeyword.lockDevice(5); // Khóa 5 giây"
    )
    public void lockDevice(Integer... seconds) {
        execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            Duration duration = (seconds != null && seconds.length > 0) ? Duration.ofSeconds(seconds[0]) : Duration.ofSeconds(1);

            // SỬ DỤNG CƠ CHẾ MỚI: executeScript
            driver.executeScript("mobile: lock");

            // Giữ trạng thái khóa nếu người dùng yêu cầu
            if (seconds != null && seconds.length > 0) {
                try { Thread.sleep(duration.toMillis()); } catch (InterruptedException ignored) {}
            }
            logger.info("Device locked.");
            return null;
        }, (Object[]) seconds);
    }

    /**
     * Unlocks the device screen.
     * Works on both Android and iOS with automatic platform detection.
     */
    @NetatKeyword(
            name = "unlockDevice",
            description = "Mở khóa màn hình thiết bị. Tự động phát hiện nền tảng (Android/iOS) và thực hiện lệnh mở khóa tương ứng.",
            category = "Mobile",
            subCategory = "System",
            parameters = {},
            note = "⚠️ QUAN TRỌNG: Chỉ mở khóa cơ bản (wake up screen). Không thể bypass mật khẩu, vân tay, hoặc Face ID. Thiết bị thực có thể vẫn yêu cầu xác thực bổ sung.",
            example = "mobileKeyword.unlockDevice();"
    )
    public void unlockDevice() {
        execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            // SỬ DỤNG CƠ CHẾ MỚI: executeScript
            driver.executeScript("mobile: unlock");
            logger.info("Device unlocked.");
            return null;
        });
    }

    /**
     * Simulates a device shake gesture.
     * Useful for testing applications that respond to accelerometer events.
     */
    @NetatKeyword(
            name = "shakeDevice",
            description = "Mô phỏng hành động lắc thiết bị để kích hoạt các tính năng sử dụng cảm biến gia tốc (accelerometer).",
            category = "Mobile",
            subCategory = "System",
            parameters = {},
            note = "LƯU Ý: Chỉ hoạt động trên thiết bị thật hoặc simulator hỗ trợ sensor. Emulator Android thông thường không hỗ trợ. Một số app có thể có độ trễ trong việc phản hồi shake event.",
            example = "mobileKeyword.shakeDevice(); // Mô phỏng lắc thiết bị"
    )
    public void shakeDevice() {
        execute(() -> {
            AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
            // SỬ DỤNG CƠ CHẾ MỚI: executeScript
            driver.executeScript("mobile: shake");
            logger.info("Device shaken.");
            return null;
        });
    }

    /**
     * Retrieves the current system time from the device.
     * Uses mobile script execution for cross-platform compatibility.
     *
     * @param format The desired time format pattern (e.g., "dd/MM/yyyy HH:mm:ss")
     * @return Formatted device time as string
     */
    @NetatKeyword(
            name = "getDeviceTime",
            description = "Lấy thời gian hệ thống hiện tại từ thiết bị và định dạng theo mẫu chỉ định. Sử dụng mobile script execution để đảm bảo tương thích đa nền tảng.",
            category = "Mobile",
            subCategory = "Getter",
            parameters = {"String format - Mẫu định dạng thời gian (ví dụ: 'dd/MM/yyyy HH:mm:ss', 'HH:mm:ss'). Sử dụng cú pháp Java SimpleDateFormat."},
            returnValue = "String: Thời gian thiết bị đã được định dạng theo mẫu yêu cầu.",
            note = "DEPRECATED API: AppiumDriver.getDeviceTime() đã bị deprecated. Hiện tại sử dụng executeScript('mobile: getDeviceTime'). Thời gian trả về phụ thuộc vào múi giờ thiết bị, không phải máy chủ test.",
            example = "String currentTime = mobileKeyword.getDeviceTime(\"HH:mm:ss\");\nString fullDate = mobileKeyword.getDeviceTime(\"dd/MM/yyyy HH:mm:ss\");"
    )
    public String getDeviceTime(String format) {
        return execute(() -> {
            try {
                // Sử dụng mobile script thay vì deprecated getDeviceTime()
                String deviceTimeStr = (String) ((AppiumDriver) DriverManager.getDriver())
                        .executeScript("mobile: getDeviceTime");

                return deviceTimeStr != null ? deviceTimeStr : "";
            } catch (Exception e) {
                logger.warn("Failed to get device time: {}", e.getMessage());
                return "";
            }
        }, format);
    }

    /**
     * Checks whether the virtual keyboard is currently displayed on screen.
     * Supports both Android and iOS platforms.
     *
     * @return true if keyboard is shown, false otherwise
     */
    @NetatKeyword(
            name = "isKeyboardShown",
            description = "Kiểm tra trạng thái hiển thị của bàn phím ảo trên màn hình. Tự động phát hiện nền tảng và sử dụng API tương ứng.",
            category = "Mobile",
            subCategory = "Verification",
            parameters = {},
            returnValue = "boolean: Trả về true nếu bàn phím đang hiển thị, false nếu không.",
            note = "PLATFORM DEPENDENT: Độ chính xác có thể khác nhau giữa Android và iOS. Android: dựa vào InputMethodManager. iOS: dựa vào keyboard notification. Có thể có false positive với bàn phím bên thứ 3.",
            example = "boolean keyboardVisible = mobileKeyword.isKeyboardShown();\nif (keyboardVisible) {\n    mobileKeyword.hideKeyboard();\n}"
    )
    public boolean isKeyboardShown() {
        return execute(() -> {
            try {
                AppiumDriver driver = (AppiumDriver) DriverManager.getDriver();
                if (driver instanceof AndroidDriver) {
                    return ((AndroidDriver) driver).isKeyboardShown();
                } else if (driver instanceof IOSDriver) {
                    return ((IOSDriver) driver).isKeyboardShown();
                }
                logger.warn("Could not determine keyboard state for the current platform.");
                return false;
            } catch (Exception e) {
                logger.warn("Failed to check keyboard state: {}", e.getMessage());
                return false;
            }
        });
    }

    // =================================================================================
    // --- DEVICE INTERACTIONS - ADDITIONAL KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "rotateDevice",
            description = "Xoay màn hình thiết bị sang orientation chỉ định. " +
                    "Hỗ trợ PORTRAIT (dọc) và LANDSCAPE (ngang). " +
                    "Hữu ích để test responsive layout, orientation-specific features, hoặc rotation handling.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {
                    "orientation: String - Orientation mong muốn (\"PORTRAIT\" hoặc \"LANDSCAPE\")"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Xoay sang landscape để test full-width layout\n" +
                    "mobileKeyword.rotateDevice(\"LANDSCAPE\");\n" +
                    "mobileKeyword.assertElementVisible(landscapeMenu);\n\n" +
                    "// Xoay về portrait\n" +
                    "mobileKeyword.rotateDevice(\"PORTRAIT\");\n\n" +
                    "// Test rotation handling\n" +
                    "mobileKeyword.sendText(inputField, \"test data\");\n" +
                    "mobileKeyword.rotateDevice(\"LANDSCAPE\");\n" +
                    "mobileKeyword.assertTextEquals(inputField, \"test data\", \"Data phải giữ nguyên sau rotate\");",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Orientation không phân biệt hoa thường. " +
                    "Một số app có thể lock orientation, không thể xoay. " +
                    "Có thể throw IllegalArgumentException nếu orientation không hợp lệ."
    )
    public void rotateDevice(String orientation) {
        execute(() -> {
            String ori = orientation.toUpperCase();
            ScreenOrientation screenOrientation;

            switch (ori) {
                case "PORTRAIT":
                    screenOrientation = ScreenOrientation.PORTRAIT;
                    break;
                case "LANDSCAPE":
                    screenOrientation = ScreenOrientation.LANDSCAPE;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid orientation: " + orientation + ". Must be PORTRAIT or LANDSCAPE");
            }

            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).rotate(screenOrientation);
            } else if (driver instanceof IOSDriver) {
                ((IOSDriver) driver).rotate(screenOrientation);
            }
            return null;
        }, orientation);
    }

    @NetatKeyword(
            name = "pressHome",
            description = "Nhấn nút Home để về màn hình chính của thiết bị. " +
                    "App hiện tại sẽ chuyển sang background. " +
                    "Hữu ích để test app resume behavior hoặc notification handling.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Nhấn Home và kiểm tra app resume\n" +
                    "mobileKeyword.pressHome();\n" +
                    "mobileKeyword.activateApp(\"com.example.myapp\");\n" +
                    "mobileKeyword.assertElementVisible(mainScreen);\n\n" +
                    "// Test notification từ home screen\n" +
                    "mobileKeyword.pressHome();\n" +
                    "mobileKeyword.openNotifications();",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Android: Nhấn KEYCODE_HOME. iOS: Sử dụng mobile:pressButton. " +
                    "Có thể throw WebDriverException nếu không thể thực hiện."
    )
    public void pressHome() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.HOME));
            } else if (driver instanceof IOSDriver) {
                // iOS: Use mobile:pressButton
                Map<String, Object> params = new HashMap<>();
                params.put("name", "home");
                driver.executeScript("mobile:pressButton", params);
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "pressEnter",
            description = "Nhấn phím Enter/Return trên keyboard. " +
                    "Hữu ích để submit form, search, hoặc new line trong textarea.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Submit search query\n" +
                    "mobileKeyword.sendText(searchField, \"test query\");\n" +
                    "mobileKeyword.pressEnter();\n\n" +
                    "// Submit login form\n" +
                    "mobileKeyword.sendText(passwordField, \"password123\");\n" +
                    "mobileKeyword.pressEnter();",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Android: KEYCODE_ENTER. iOS: Return key. " +
                    "Keyboard phải đang hiển thị để press key có hiệu lực."
    )
    public void pressEnter() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.ENTER));
            } else if (driver instanceof IOSDriver) {
                // iOS: Simulate return key
                Map<String, Object> params = new HashMap<>();
                params.put("name", "return");
                driver.executeScript("mobile:pressButton", params);
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "pressSearch",
            description = "Nhấn nút Search trên keyboard hoặc navigation bar (Android). " +
                    "Hữu ích để trigger search action trong app.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Trigger search\n" +
                    "mobileKeyword.sendText(searchField, \"product name\");\n" +
                    "mobileKeyword.pressSearch();\n" +
                    "mobileKeyword.waitForVisible(searchResults, 10);",
            note = "Áp dụng chủ yếu cho Android. " +
                    "iOS không có dedicated search key, có thể sử dụng pressEnter thay thế. " +
                    "Có thể throw UnsupportedOperationException trên iOS."
    )
    public void pressSearch() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.SEARCH));
            } else {
                logger.warn("pressSearch is primarily for Android. For iOS, use pressEnter instead.");
                throw new UnsupportedOperationException("pressSearch is not supported on iOS");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "pressMenu",
            description = "Nhấn nút Menu (Android) để mở menu options. " +
                    "Một số device hiện đại không còn hardware menu button.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Mở menu options\n" +
                    "mobileKeyword.pressMenu();\n" +
                    "mobileKeyword.tap(settingsOption);",
            note = "Chỉ áp dụng cho Android. " +
                    "iOS không có menu button. " +
                    "Nhiều Android devices hiện đại đã loại bỏ hardware menu button. " +
                    "Có thể throw UnsupportedOperationException trên iOS."
    )
    public void pressMenu() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.MENU));
            } else {
                logger.warn("pressMenu is only supported on Android.");
                throw new UnsupportedOperationException("pressMenu is not supported on iOS");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "pressVolumeUp",
            description = "Nhấn nút Volume Up để tăng âm lượng. " +
                    "Hữu ích để test app behavior khi adjust volume.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Tăng âm lượng\n" +
                    "mobileKeyword.pressVolumeUp();\n\n" +
                    "// Test volume control trong media app\n" +
                    "mobileKeyword.tap(playButton);\n" +
                    "mobileKeyword.pressVolumeUp();\n" +
                    "mobileKeyword.pressVolumeUp();",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Android: KEYCODE_VOLUME_UP. iOS: mobile:pressButton volumeUp. " +
                    "Có thể không work trên emulator/simulator."
    )
    public void pressVolumeUp() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.VOLUME_UP));
            } else if (driver instanceof IOSDriver) {
                Map<String, Object> params = new HashMap<>();
                params.put("name", "volumeUp");
                driver.executeScript("mobile:pressButton", params);
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "pressVolumeDown",
            description = "Nhấn nút Volume Down để giảm âm lượng. " +
                    "Hữu ích để test app behavior khi adjust volume.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Giảm âm lượng\n" +
                    "mobileKeyword.pressVolumeDown();\n\n" +
                    "// Test mute behavior\n" +
                    "for (int i = 0; i < 5; i++) {\n" +
                    "    mobileKeyword.pressVolumeDown();\n" +
                    "}",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Android: KEYCODE_VOLUME_DOWN. iOS: mobile:pressButton volumeDown. " +
                    "Có thể không work trên emulator/simulator."
    )
    public void pressVolumeDown() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.VOLUME_DOWN));
            } else if (driver instanceof IOSDriver) {
                Map<String, Object> params = new HashMap<>();
                params.put("name", "volumeDown");
                driver.executeScript("mobile:pressButton", params);
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "getNetworkConnection",
            description = "Lấy trạng thái kết nối mạng hiện tại của thiết bị (Android). " +
                    "Trả về bitmask: 0=None, 1=Airplane, 2=Wifi, 4=Data, 6=All. " +
                    "Hữu ích để verify network state trước test hoặc troubleshooting.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {},
            returnValue = "int - Bitmask của network connection",
            example = "// Check network state\n" +
                    "int networkState = mobileKeyword.getNetworkConnection();\n" +
                    "if (networkState == 0) {\n" +
                    "    logger.warn(\"No network connection\");\n" +
                    "}\n\n" +
                    "// Verify wifi enabled\n" +
                    "int state = mobileKeyword.getNetworkConnection();\n" +
                    "Assert.assertTrue((state & 2) != 0, \"Wifi should be enabled\");",
            note = "Chỉ áp dụng cho Android. " +
                    "iOS không hỗ trợ network connection API. " +
                    "Bitmask: 0=NONE, 1=AIRPLANE, 2=WIFI, 4=DATA, 6=ALL (WIFI+DATA). " +
                    "Có thể throw UnsupportedOperationException trên iOS."
    )
    public int getNetworkConnection() {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ConnectionState state = ((AndroidDriver) driver).getConnection();
                // ConnectionState wraps an int value representing network state
                // Extract the numeric value using bitwise operations
                int value = 0;
                if (state.isAirplaneModeEnabled()) value |= 1;
                if (state.isWiFiEnabled()) value |= 2;
                if (state.isDataEnabled()) value |= 4;
                return value;
            } else {
                logger.warn("getNetworkConnection is only supported on Android.");
                throw new UnsupportedOperationException("getNetworkConnection is not supported on iOS");
            }
        });
    }

    @NetatKeyword(
            name = "setNetworkConnection",
            description = "Thiết lập trạng thái kết nối mạng của thiết bị (Android). " +
                    "ConnectionType bitmask: 0=None, 1=Airplane, 2=Wifi, 4=Data, 6=All. " +
                    "Hữu ích để test offline mode, airplane mode, hoặc specific network scenarios.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {
                    "connectionType: int - Bitmask của connection type"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Enable airplane mode\n" +
                    "mobileKeyword.setNetworkConnection(1);\n" +
                    "mobileKeyword.assertElementVisible(offlineMessage);\n\n" +
                    "// Enable wifi only\n" +
                    "mobileKeyword.setNetworkConnection(2);\n\n" +
                    "// Enable all connections\n" +
                    "mobileKeyword.setNetworkConnection(6);\n\n" +
                    "// Disable all connections\n" +
                    "mobileKeyword.setNetworkConnection(0);",
            note = "Chỉ áp dụng cho Android. " +
                    "iOS không hỗ trợ network connection API. " +
                    "Bitmask: 0=NONE, 1=AIRPLANE, 2=WIFI, 4=DATA, 6=ALL. " +
                    "Cần permission SET_NETWORK để modify network. " +
                    "Có thể throw UnsupportedOperationException trên iOS."
    )
    public void setNetworkConnection(int connectionType) {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).setConnection(new ConnectionState(connectionType));
            } else {
                logger.warn("setNetworkConnection is only supported on Android.");
                throw new UnsupportedOperationException("setNetworkConnection is not supported on iOS");
            }
            return null;
        }, connectionType);
    }

    @NetatKeyword(
            name = "toggleWifi",
            description = "Bật/tắt Wifi (toggle current state) trên Android. " +
                    "Nếu đang bật → tắt, nếu đang tắt → bật. " +
                    "Hữu ích để test offline/online behavior.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Test offline mode\n" +
                    "mobileKeyword.toggleWifi(); // Tắt wifi\n" +
                    "mobileKeyword.assertElementVisible(offlineIndicator);\n" +
                    "mobileKeyword.toggleWifi(); // Bật lại wifi\n" +
                    "mobileKeyword.waitForVisible(onlineIndicator, 10);",
            note = "Chỉ áp dụng cho Android. " +
                    "iOS không hỗ trợ programmatic wifi toggle. " +
                    "Cần permission CHANGE_WIFI_STATE. " +
                    "Có thể throw UnsupportedOperationException trên iOS."
    )
    public void toggleWifi() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ConnectionState currentState = ((AndroidDriver) driver).getConnection();
                boolean wifiEnabled = currentState.isWiFiEnabled();

                // Build new connection state with toggled wifi
                int newValue = 0;
                if (currentState.isAirplaneModeEnabled()) newValue |= 1;
                if (!wifiEnabled) newValue |= 2;  // Toggle: if was enabled, disable it
                if (currentState.isDataEnabled()) newValue |= 4;

                ((AndroidDriver) driver).setConnection(new ConnectionState(newValue));
            } else {
                logger.warn("toggleWifi is only supported on Android.");
                throw new UnsupportedOperationException("toggleWifi is not supported on iOS");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "toggleData",
            description = "Bật/tắt Mobile Data (toggle current state) trên Android. " +
                    "Nếu đang bật → tắt, nếu đang tắt → bật. " +
                    "Hữu ích để test cellular network scenarios.",
            category = "Mobile",
            subCategory = "Device",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Test cellular-only mode\n" +
                    "mobileKeyword.toggleWifi(); // Tắt wifi\n" +
                    "mobileKeyword.toggleData(); // Bật data\n" +
                    "mobileKeyword.assertElementVisible(cellularIndicator);",
            note = "Chỉ áp dụng cho Android. " +
                    "iOS không hỗ trợ programmatic data toggle. " +
                    "Cần permission CHANGE_NETWORK_STATE. " +
                    "Có thể throw UnsupportedOperationException trên iOS."
    )
    public void toggleData() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ConnectionState currentState = ((AndroidDriver) driver).getConnection();
                boolean dataEnabled = currentState.isDataEnabled();

                // Build new connection state with toggled data
                int newValue = 0;
                if (currentState.isAirplaneModeEnabled()) newValue |= 1;
                if (currentState.isWiFiEnabled()) newValue |= 2;
                if (!dataEnabled) newValue |= 4;  // Toggle: if was enabled, disable it

                ((AndroidDriver) driver).setConnection(new ConnectionState(newValue));
            } else {
                logger.warn("toggleData is only supported on Android.");
                throw new UnsupportedOperationException("toggleData is not supported on iOS");
            }
            return null;
        });
    }

    // =================================================================================
    // --- FILE & DATA MANAGEMENT - ADDITIONAL KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "setClipboard",
            description = "Set nội dung clipboard của thiết bị. " +
                    "Hữu ích để test paste functionality hoặc setup test data trong clipboard.",
            category = "Mobile",
            subCategory = "Data",
            parameters = {
                    "text: String - Nội dung cần set vào clipboard"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Set clipboard và paste vào field\n" +
                    "mobileKeyword.setClipboard(\"test@example.com\");\n" +
                    "mobileKeyword.longPress(emailField, 2);\n" +
                    "mobileKeyword.tap(pasteOption);\n\n" +
                    "// Setup test data\n" +
                    "mobileKeyword.setClipboard(\"https://example.com/deep-link\");",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Android: Sử dụng ClipData. iOS: Sử dụng UIPasteboard. " +
                    "Có thể throw WebDriverException nếu không thể set clipboard."
    )
    public void setClipboard(String text) {
        execute(() -> {
            ((HasClipboard) getAppiumDriver()).setClipboardText(text);
            return null;
        }, text);
    }

    @NetatKeyword(
            name = "deleteFile",
            description = "Xóa file trên thiết bị theo đường dẫn chỉ định. " +
                    "Hữu ích để cleanup test data hoặc remove temporary files.",
            category = "Mobile",
            subCategory = "Data",
            parameters = {
                    "devicePath: String - Đường dẫn file cần xóa trên thiết bị"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Xóa file test data\n" +
                    "mobileKeyword.deleteFile(\"/sdcard/Download/test-data.txt\");\n\n" +
                    "// Cleanup sau test\n" +
                    "mobileKeyword.deleteFile(\"/data/local/tmp/test-image.png\");",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Cần permission để xóa file ở location chỉ định. " +
                    "Không throw exception nếu file không tồn tại. " +
                    "Có thể throw WebDriverException nếu không có permission."
    )
    public void deleteFile(String devicePath) {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                // Android: Use adb shell rm
                driver.executeScript("mobile:shell", Map.of("command", "rm", "args", List.of("-f", devicePath)));
            } else if (driver instanceof IOSDriver) {
                // iOS: Use mobile:deleteFile
                driver.executeScript("mobile:deleteFile", Map.of("remotePath", devicePath));
            }
            return null;
        }, devicePath);
    }

    @NetatKeyword(
            name = "recordScreen",
            description = "Bắt đầu record màn hình thiết bị. " +
                    "Recording sẽ tiếp tục cho đến khi gọi stopRecordScreen. " +
                    "Hữu ích để capture test execution video hoặc debugging.",
            category = "Mobile",
            subCategory = "Data",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Record test execution\n" +
                    "mobileKeyword.recordScreen();\n" +
                    "mobileKeyword.tap(loginButton);\n" +
                    "mobileKeyword.sendText(usernameField, \"test\");\n" +
                    "String video = mobileKeyword.stopRecordScreen();\n\n" +
                    "// Record bug reproduction\n" +
                    "mobileKeyword.recordScreen();\n" +
                    "// ... reproduce bug steps\n" +
                    "mobileKeyword.stopRecordScreen();",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Android: Sử dụng screenrecord utility. iOS: Sử dụng xctest framework. " +
                    "Recording có thể bị giới hạn thời gian (Android: 3 phút mặc định). " +
                    "Có thể throw WebDriverException nếu không thể start recording."
    )
    public void recordScreen() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).startRecordingScreen();
            } else if (driver instanceof IOSDriver) {
                ((IOSDriver) driver).startRecordingScreen();
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "stopRecordScreen",
            description = "Dừng recording màn hình và trả về video dưới dạng Base64 string. " +
                    "Video có thể được decode và lưu vào file để review sau.",
            category = "Mobile",
            subCategory = "Data",
            parameters = {},
            returnValue = "String - Base64 encoded video content",
            example = "// Stop recording và save video\n" +
                    "mobileKeyword.recordScreen();\n" +
                    "// ... test steps\n" +
                    "String base64Video = mobileKeyword.stopRecordScreen();\n" +
                    "byte[] videoBytes = Base64.getDecoder().decode(base64Video);\n" +
                    "Files.write(Paths.get(\"test-recording.mp4\"), videoBytes);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Phải gọi recordScreen trước khi gọi stopRecordScreen. " +
                    "Video format: MP4 (Android), MOV (iOS). " +
                    "Có thể throw WebDriverException nếu không có active recording."
    )
    public String stopRecordScreen() {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                return ((AndroidDriver) driver).stopRecordingScreen();
            } else if (driver instanceof IOSDriver) {
                return ((IOSDriver) driver).stopRecordingScreen();
            }
            return null;
        });
    }

    // =================================================================================
    // --- NOTIFICATION MANAGEMENT - ADDITIONAL KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "clearNotifications",
            description = "Xóa tất cả notifications trong notification panel (Android). " +
                    "Hữu ích để cleanup notifications trước test hoặc verify notification cleared.",
            category = "Mobile",
            subCategory = "Notification",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Clear notifications trước test\n" +
                    "mobileKeyword.openNotifications();\n" +
                    "mobileKeyword.clearNotifications();\n\n" +
                    "// Verify notification và clear\n" +
                    "mobileKeyword.openNotifications();\n" +
                    "mobileKeyword.assertNotificationText(\"New Message\", \"You have 1 new message\", 5);\n" +
                    "mobileKeyword.clearNotifications();",
            note = "Chỉ áp dụng cho Android. " +
                    "iOS không cho phép programmatic clear notifications. " +
                    "Cần mở notification panel trước khi clear. " +
                    "Có thể throw UnsupportedOperationException trên iOS."
    )
    public void clearNotifications() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                // Find and tap clear all button
                try {
                    // Different Android versions may have different clear button
                    List<String> clearButtonTexts = Arrays.asList("Clear all", "Clear", "Xóa tất cả");
                    for (String text : clearButtonTexts) {
                        try {
                            WebElement clearButton = driver.findElement(
                                    AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")")
                            );
                            clearButton.click();
                            return null;
                        } catch (NoSuchElementException e) {
                            // Try next text
                        }
                    }
                    logger.warn("Could not find clear notifications button");
                } catch (Exception e) {
                    logger.warn("Failed to clear notifications: {}", e.getMessage());
                }
            } else {
                logger.warn("clearNotifications is only supported on Android.");
                throw new UnsupportedOperationException("clearNotifications is not supported on iOS");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "getNotificationCount",
            description = "Đếm số lượng notifications hiện có trong notification panel. " +
                    "Hữu ích để verify số lượng notifications hoặc check notification state.",
            category = "Mobile",
            subCategory = "Notification",
            parameters = {},
            returnValue = "int - Số lượng notifications",
            example = "// Verify số lượng notifications\n" +
                    "mobileKeyword.openNotifications();\n" +
                    "int count = mobileKeyword.getNotificationCount();\n" +
                    "Assert.assertEquals(count, 3, \"Should have 3 notifications\");\n\n" +
                    "// Check notification state\n" +
                    "mobileKeyword.openNotifications();\n" +
                    "if (mobileKeyword.getNotificationCount() > 0) {\n" +
                    "    mobileKeyword.clearNotifications();\n" +
                    "}",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Cần mở notification panel trước khi count. " +
                    "Android: Count notification items. iOS: Count notification cells. " +
                    "Kết quả có thể không chính xác 100% do UI complexity."
    )
    public int getNotificationCount() {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            int count = 0;

            try {
                if (driver instanceof AndroidDriver) {
                    // Android: Count notification items
                    List<WebElement> notifications = driver.findElements(
                            AppiumBy.androidUIAutomator("new UiSelector().className(\"android.widget.FrameLayout\").descriptionContains(\"notification\")")
                    );
                    count = notifications.size();
                } else if (driver instanceof IOSDriver) {
                    // iOS: Count notification cells
                    List<WebElement> notifications = driver.findElements(
                            AppiumBy.iOSClassChain("**/XCUIElementTypeCell[`label CONTAINS 'notification'`]")
                    );
                    count = notifications.size();
                }
            } catch (Exception e) {
                logger.warn("Failed to count notifications: {}", e.getMessage());
            }

            return count;
        });
    }

    // =================================================================================
    // --- ADVANCED FEATURES - ADDITIONAL KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "getPerformanceData",
            description = "Lấy dữ liệu hiệu suất của ứng dụng Android (CPU, Memory, Network, Battery). " +
                    "DataType: cpuinfo, memoryinfo, batteryinfo, networkinfo. " +
                    "Hữu ích để monitor performance trong quá trình test.",
            category = "Mobile",
            subCategory = "Advanced",
            parameters = {
                    "packageName: String - Package name của app cần monitor",
                    "dataType: String - Loại data: cpuinfo, memoryinfo, batteryinfo, networkinfo",
                    "dataReadTimeout: int - Timeout để đọc data (milliseconds)"
            },
            returnValue = "List<List<Object>> - Performance data dưới dạng table",
            example = "// Monitor memory usage\n" +
                    "List<List<Object>> memData = mobileKeyword.getPerformanceData(\"com.example.app\", \"memoryinfo\", 5000);\n" +
                    "logger.info(\"Memory data: {}\", memData);\n\n" +
                    "// Monitor CPU usage\n" +
                    "List<List<Object>> cpuData = mobileKeyword.getPerformanceData(\"com.example.app\", \"cpuinfo\", 5000);\n\n" +
                    "// Check battery consumption\n" +
                    "List<List<Object>> batteryData = mobileKeyword.getPerformanceData(\"com.example.app\", \"batteryinfo\", 5000);",
            note = "Chỉ áp dụng cho Android. " +
                    "iOS không hỗ trợ performance data API. " +
                    "Cần adb và appium-uiautomator2-driver. " +
                    "Data format phụ thuộc vào Android version. " +
                    "Có thể throw UnsupportedOperationException trên iOS."
    )
    public List<List<Object>> getPerformanceData(String packageName, String dataType, int dataReadTimeout) {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                return ((AndroidDriver) driver).getPerformanceData(packageName, dataType, dataReadTimeout);
            } else {
                logger.warn("getPerformanceData is only supported on Android.");
                throw new UnsupportedOperationException("getPerformanceData is not supported on iOS");
            }
        }, packageName, dataType, dataReadTimeout);
    }

    @NetatKeyword(
            name = "getBatteryInfo",
            description = "Lấy thông tin pin của thiết bị (level, state, temperature). " +
                    "Trả về Map với keys: level (0-100), state (charging/discharging/full). " +
                    "Hữu ích để test battery optimization scenarios.",
            category = "Mobile",
            subCategory = "Advanced",
            parameters = {},
            returnValue = "Map<String, Object> - Battery info (level, state, etc.)",
            example = "// Check battery level before test\n" +
                    "Map<String, Object> battery = mobileKeyword.getBatteryInfo();\n" +
                    "int level = (Integer) battery.get(\"level\");\n" +
                    "if (level < 20) {\n" +
                    "    logger.warn(\"Battery low: {}%\", level);\n" +
                    "}\n\n" +
                    "// Verify charging state\n" +
                    "String state = (String) battery.get(\"state\");\n" +
                    "Assert.assertEquals(state, \"charging\");",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Android: Sử dụng dumpsys battery. iOS: Sử dụng IOKit framework. " +
                    "Battery state có thể là: unknown, charging, discharging, not_charging, full. " +
                    "Một số emulator/simulator có thể trả về mock data."
    )
    public Map<String, Object> getBatteryInfo() {
        return execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            Map<String, Object> batteryInfo = new HashMap<>();

            try {
                if (driver instanceof AndroidDriver) {
                    // Android: Use mobile:batteryInfo command
                    Map<String, Object> result = (Map<String, Object>) driver.executeScript("mobile: batteryInfo");
                    batteryInfo.putAll(result);
                } else if (driver instanceof IOSDriver) {
                    // iOS: Use mobile:batteryInfo command
                    Map<String, Object> result = (Map<String, Object>) driver.executeScript("mobile: batteryInfo");
                    batteryInfo.putAll(result);
                }
            } catch (Exception e) {
                logger.warn("Failed to get battery info: {}", e.getMessage());
                // Return default values
                batteryInfo.put("level", -1);
                batteryInfo.put("state", "unknown");
            }

            return batteryInfo;
        });
    }

    @NetatKeyword(
            name = "setLocation",
            description = "Thiết lập vị trí GPS giả lập (mock location) cho thiết bị. " +
                    "Cho phép test location-based features mà không cần di chuyển thực tế. " +
                    "Latitude: -90 đến 90, Longitude: -180 đến 180.",
            category = "Mobile",
            subCategory = "Advanced",
            parameters = {
                    "latitude: double - Vĩ độ (-90 đến 90)",
                    "longitude: double - Kinh độ (-180 đến 180)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Set location to Hanoi, Vietnam\n" +
                    "mobileKeyword.setLocation(21.0285, 105.8542);\n" +
                    "mobileKeyword.click(refreshLocationButton);\n" +
                    "mobileKeyword.assertTextContains(locationLabel, \"Hanoi\");\n\n" +
                    "// Set location to Ho Chi Minh City\n" +
                    "mobileKeyword.setLocation(10.8231, 106.6297);\n\n" +
                    "// Test location boundary\n" +
                    "mobileKeyword.setLocation(0.0, 0.0); // Null Island",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Android: Cần enable mock location trong Developer Options. " +
                    "iOS: Simulator luôn support, real device cần jailbreak hoặc developer mode. " +
                    "Latitude phải trong range [-90, 90]. Longitude phải trong range [-180, 180]. " +
                    "Có thể throw IllegalArgumentException nếu coordinates không hợp lệ."
    )
    public void setLocation(double latitude, double longitude) {
        execute(() -> {
            if (latitude < -90 || latitude > 90) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90. Got: " + latitude);
            }
            if (longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180. Got: " + longitude);
            }

            AppiumDriver driver = getAppiumDriver();
            Location location = new Location(latitude, longitude, 0.0);

            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).setLocation(location);
            } else if (driver instanceof IOSDriver) {
                ((IOSDriver) driver).setLocation(location);
            }
            return null;
        }, latitude, longitude);
    }

    // =================================================================================
    // --- IOS-SPECIFIC FEATURES - ADDITIONAL KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "shake",
            description = "Mô phỏng hành động lắc thiết bị iOS. " +
                    "Hữu ích để test các tính năng shake-to-undo, shake-to-refresh. " +
                    "Chỉ hoạt động trên iOS.",
            category = "Mobile",
            subCategory = "iOS",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Test shake to undo feature\n" +
                    "mobileKeyword.sendText(textField, \"wrong text\");\n" +
                    "mobileKeyword.shake();\n" +
                    "mobileKeyword.click(undoButton);\n" +
                    "mobileKeyword.assertTextEquals(textField, \"\");\n\n" +
                    "// Test shake to refresh\n" +
                    "mobileKeyword.shake();\n" +
                    "mobileKeyword.waitForVisible(refreshingIndicator, 5);",
            note = "Chỉ áp dụng cho iOS. " +
                    "Android không hỗ trợ shake gesture thông qua Appium. " +
                    "Shake gesture có thể không hoạt động trên một số iOS simulator versions. " +
                    "Có thể throw UnsupportedOperationException trên Android."
    )
    public void shake() {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof IOSDriver) {
                ((IOSDriver) driver).shake();
            } else {
                logger.warn("shake is only supported on iOS.");
                throw new UnsupportedOperationException("shake is not supported on Android");
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "performTouchID",
            description = "Mô phỏng Touch ID authentication trên iOS simulator. " +
                    "Match = true: Authentication thành công. Match = false: Authentication thất bại. " +
                    "Hữu ích để test biometric authentication flows.",
            category = "Mobile",
            subCategory = "iOS",
            parameters = {
                    "match: boolean - true = success, false = failure"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Test successful Touch ID\n" +
                    "mobileKeyword.click(loginWithTouchIDButton);\n" +
                    "mobileKeyword.performTouchID(true);\n" +
                    "mobileKeyword.assertVisible(dashboardScreen);\n\n" +
                    "// Test failed Touch ID\n" +
                    "mobileKeyword.click(loginWithTouchIDButton);\n" +
                    "mobileKeyword.performTouchID(false);\n" +
                    "mobileKeyword.assertVisible(errorMessage);",
            note = "Chỉ áp dụng cho iOS Simulator. " +
                    "Không hoạt động trên real device. " +
                    "Cần iOS version 8+. " +
                    "Face ID sử dụng command tương tự với key 'FaceID'. " +
                    "Có thể throw UnsupportedOperationException trên Android hoặc real iOS device."
    )
    public void performTouchID(boolean match) {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof IOSDriver) {
                Map<String, Object> params = new HashMap<>();
                params.put("match", match);
                ((IOSDriver) driver).executeScript("mobile: enrollBiometric", params);

                // Perform the Touch ID
                Map<String, Object> touchParams = new HashMap<>();
                touchParams.put("match", match);
                ((IOSDriver) driver).executeScript("mobile: sendBiometricMatch", touchParams);
            } else {
                logger.warn("performTouchID is only supported on iOS.");
                throw new UnsupportedOperationException("performTouchID is not supported on Android");
            }
            return null;
        }, match);
    }

    @NetatKeyword(
            name = "scrollIOS",
            description = "Thực hiện scroll theo hướng chỉ định sử dụng iOS native scroll. " +
                    "Direction: up, down, left, right. " +
                    "Hữu ích khi standard scroll không hoạt động với iOS native components.",
            category = "Mobile",
            subCategory = "iOS",
            parameters = {
                    "direction: String - Hướng scroll: up, down, left, right"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Scroll down in iOS native list\n" +
                    "mobileKeyword.scrollIOS(\"down\");\n\n" +
                    "// Scroll to top\n" +
                    "mobileKeyword.scrollIOS(\"up\");\n\n" +
                    "// Horizontal scroll\n" +
                    "mobileKeyword.scrollIOS(\"right\");",
            note = "Chỉ áp dụng cho iOS. " +
                    "Sử dụng mobile:scroll command của XCUITest. " +
                    "Direction không phân biệt hoa thường. " +
                    "Có thể throw UnsupportedOperationException trên Android hoặc IllegalArgumentException nếu direction không hợp lệ."
    )
    public void scrollIOS(String direction) {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof IOSDriver) {
                String dir = direction.toLowerCase();
                if (!Arrays.asList("up", "down", "left", "right").contains(dir)) {
                    throw new IllegalArgumentException("Invalid direction: " + direction + ". Must be: up, down, left, right");
                }

                Map<String, Object> params = new HashMap<>();
                params.put("direction", dir);
                ((IOSDriver) driver).executeScript("mobile: scroll", params);
            } else {
                logger.warn("scrollIOS is only supported on iOS.");
                throw new UnsupportedOperationException("scrollIOS is not supported on Android");
            }
            return null;
        }, direction);
    }

    // =================================================================================
    // --- ANDROID-SPECIFIC FEATURES - ADDITIONAL KEYWORDS ---
    // =================================================================================

    @NetatKeyword(
            name = "startActivity",
            description = "Khởi động một Activity cụ thể của ứng dụng Android. " +
                    "Cho phép deep link vào màn hình cụ thể mà không cần navigate từ đầu. " +
                    "Hữu ích để test individual screens hoặc specific flows.",
            category = "Mobile",
            subCategory = "Android",
            parameters = {
                    "appPackage: String - Package name của app",
                    "appActivity: String - Activity name cần start"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Start main activity\n" +
                    "mobileKeyword.startActivity(\"com.example.app\", \".MainActivity\");\n\n" +
                    "// Start settings activity directly\n" +
                    "mobileKeyword.startActivity(\"com.example.app\", \".SettingsActivity\");\n\n" +
                    "// Start activity with full package path\n" +
                    "mobileKeyword.startActivity(\"com.example.app\", \"com.example.app.ui.LoginActivity\");",
            note = "Chỉ áp dụng cho Android. " +
                    "iOS không có concept của Activity. " +
                    "Activity name có thể bắt đầu với '.' (relative) hoặc full package path. " +
                    "Activity phải được declare trong AndroidManifest.xml. " +
                    "Có thể throw UnsupportedOperationException trên iOS."
    )
    public void startActivity(String appPackage, String appActivity) {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).startActivity(new io.appium.java_client.android.Activity(appPackage, appActivity));
            } else {
                logger.warn("startActivity is only supported on Android.");
                throw new UnsupportedOperationException("startActivity is not supported on iOS");
            }
            return null;
        }, appPackage, appActivity);
    }


    @NetatKeyword(
            name = "performFingerprint",
            description = "Mô phỏng fingerprint authentication trên Android emulator. " +
                    "FingerprintId là số identifier của fingerprint đã enroll (1-10). " +
                    "Hữu ích để test biometric authentication flows trên Android.",
            category = "Mobile",
            subCategory = "Android",
            parameters = {
                    "fingerprintId: int - ID của fingerprint (1-10)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Test fingerprint authentication\n" +
                    "mobileKeyword.click(loginWithFingerprintButton);\n" +
                    "mobileKeyword.performFingerprint(1);\n" +
                    "mobileKeyword.assertVisible(dashboardScreen);\n\n" +
                    "// Test different fingerprints\n" +
                    "mobileKeyword.performFingerprint(2);\n" +
                    "mobileKeyword.assertVisible(errorMessage);",
            note = "Chỉ áp dụng cho Android Emulator. " +
                    "Không hoạt động trên real device. " +
                    "Cần Android 6.0+ và emulator có fingerprint sensor. " +
                    "FingerprintId phải được enroll trước thông qua adb command. " +
                    "Có thể throw UnsupportedOperationException trên iOS hoặc real device."
    )
    public void performFingerprint(int fingerprintId) {
        execute(() -> {
            AppiumDriver driver = getAppiumDriver();
            if (driver instanceof AndroidDriver) {
                Map<String, Object> params = new HashMap<>();
                params.put("fingerprintId", fingerprintId);
                ((AndroidDriver) driver).executeScript("mobile: fingerprint", params);
            } else {
                logger.warn("performFingerprint is only supported on Android.");
                throw new UnsupportedOperationException("performFingerprint is not supported on Android");
            }
            return null;
        }, fingerprintId);
    }

    // =================================================================================
    // --- UTILITY KEYWORDS - ELEMENT INFO & SCRIPT EXECUTION ---
    // =================================================================================

    @NetatKeyword(
            name = "getElementLocation",
            description = "Lấy vị trí của element trên màn hình (tọa độ x, y). " +
                    "Trả về Map với keys: x (tọa độ ngang), y (tọa độ dọc). " +
                    "Hữu ích để verify vị trí element, calculate gestures, hoặc visual testing.",
            category = "Mobile",
            subCategory = "Utility",
            parameters = {
                    "uiObject: ObjectUI - Element cần lấy vị trí"
            },
            returnValue = "Map<String, Integer> - Map với keys 'x' và 'y'",
            example = "// Get element location\n" +
                    "Map<String, Integer> location = mobileKeyword.getElementLocation(loginButton);\n" +
                    "int x = location.get(\"x\");\n" +
                    "int y = location.get(\"y\");\n" +
                    "logger.info(\"Button at: ({}, {})\", x, y);\n\n" +
                    "// Verify element is in viewport\n" +
                    "Map<String, Integer> loc = mobileKeyword.getElementLocation(banner);\n" +
                    "Assert.assertTrue(loc.get(\"y\") >= 0, \"Banner should be visible\");\n\n" +
                    "// Calculate relative position\n" +
                    "Map<String, Integer> loc1 = mobileKeyword.getElementLocation(element1);\n" +
                    "Map<String, Integer> loc2 = mobileKeyword.getElementLocation(element2);\n" +
                    "int distance = Math.abs(loc1.get(\"x\") - loc2.get(\"x\"));",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Tọa độ là absolute position trên màn hình. " +
                    "Origin (0,0) ở góc trên bên trái màn hình. " +
                    "Element phải visible để có location. " +
                    "Có thể throw NoSuchElementException nếu element không tồn tại."
    )
    public Map<String, Integer> getElementLocation(ObjectUI uiObject) {
        return execute(() -> {
            WebElement element = findElement(uiObject);
            Point location = element.getLocation();

            Map<String, Integer> result = new HashMap<>();
            result.put("x", location.getX());
            result.put("y", location.getY());

            return result;
        }, uiObject);
    }

    @NetatKeyword(
            name = "getElementSize",
            description = "Lấy kích thước của element (width, height). " +
                    "Trả về Map với keys: width (chiều rộng), height (chiều cao). " +
                    "Hữu ích để verify layout, responsive design, hoặc calculate touch areas.",
            category = "Mobile",
            subCategory = "Utility",
            parameters = {
                    "uiObject: ObjectUI - Element cần lấy kích thước"
            },
            returnValue = "Map<String, Integer> - Map với keys 'width' và 'height'",
            example = "// Get element size\n" +
                    "Map<String, Integer> size = mobileKeyword.getElementSize(loginButton);\n" +
                    "int width = size.get(\"width\");\n" +
                    "int height = size.get(\"height\");\n" +
                    "logger.info(\"Button size: {}x{}\", width, height);\n\n" +
                    "// Verify minimum touch target size (44x44)\n" +
                    "Map<String, Integer> btnSize = mobileKeyword.getElementSize(button);\n" +
                    "Assert.assertTrue(btnSize.get(\"width\") >= 44, \"Width must be at least 44px\");\n" +
                    "Assert.assertTrue(btnSize.get(\"height\") >= 44, \"Height must be at least 44px\");\n\n" +
                    "// Verify image aspect ratio\n" +
                    "Map<String, Integer> imgSize = mobileKeyword.getElementSize(image);\n" +
                    "double ratio = (double) imgSize.get(\"width\") / imgSize.get(\"height\");\n" +
                    "Assert.assertEquals(ratio, 16.0/9.0, 0.1, \"Should be 16:9 aspect ratio\");",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Kích thước tính bằng pixels (device pixels, không phải CSS pixels). " +
                    "Element phải visible để có size. " +
                    "Có thể throw NoSuchElementException nếu element không tồn tại."
    )
    public Map<String, Integer> getElementSize(ObjectUI uiObject) {
        return execute(() -> {
            WebElement element = findElement(uiObject);
            Dimension size = element.getSize();

            Map<String, Integer> result = new HashMap<>();
            result.put("width", size.getWidth());
            result.put("height", size.getHeight());

            return result;
        }, uiObject);
    }

    @NetatKeyword(
            name = "getPageSource",
            description = "Lấy XML source code của page hierarchy hiện tại. " +
                    "Android: Trả về UI Automator XML. iOS: Trả về XCUITest XML. " +
                    "Hữu ích để debug, logging, verify page structure, hoặc parse element attributes.",
            category = "Mobile",
            subCategory = "Utility",
            parameters = {},
            returnValue = "String - XML source code của page",
            example = "// Get and log page source\n" +
                    "String source = mobileKeyword.getPageSource();\n" +
                    "logger.info(\"Page source: {}\", source);\n\n" +
                    "// Verify element exists in page source\n" +
                    "String pageXml = mobileKeyword.getPageSource();\n" +
                    "Assert.assertTrue(pageXml.contains(\"Login\"), \"Login text should exist\");\n\n" +
                    "// Save page source for debugging\n" +
                    "String source = mobileKeyword.getPageSource();\n" +
                    "Files.write(Paths.get(\"page_source.xml\"), source.getBytes());\n\n" +
                    "// Count elements\n" +
                    "String xml = mobileKeyword.getPageSource();\n" +
                    "int buttonCount = xml.split(\"<.*Button\").length - 1;\n" +
                    "logger.info(\"Found {} buttons\", buttonCount);",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "XML structure khác nhau giữa Android và iOS. " +
                    "Page source có thể rất lớn, nên cẩn thận khi log. " +
                    "Mỗi lần gọi sẽ query lại từ device (có thể chậm). " +
                    "Có thể throw WebDriverException nếu không thể lấy source."
    )
    public String getPageSource() {
        return execute(() -> {
            return getAppiumDriver().getPageSource();
        });
    }

    @NetatKeyword(
            name = "executeScript",
            description = "Thực thi JavaScript hoặc mobile script với arguments. " +
                    "Hỗ trợ cả Appium mobile: commands và custom scripts. " +
                    "Linh hoạt hơn executeMobileCommand với varargs parameters.",
            category = "Mobile",
            subCategory = "Utility",
            parameters = {
                    "script: String - Script cần thực thi",
                    "args: Object... - Arguments cho script (varargs)"
            },
            returnValue = "Object - Kết quả trả về từ script",
            example = "// Execute mobile command\n" +
                    "mobileKeyword.executeScript(\"mobile: scroll\", Map.of(\"direction\", \"down\"));\n\n" +
                    "// Execute with multiple arguments\n" +
                    "Object result = mobileKeyword.executeScript(\"mobile: shell\", \"ls\", \"-la\", \"/sdcard\");\n\n" +
                    "// Get device info\n" +
                    "String deviceTime = (String) mobileKeyword.executeScript(\"mobile: getDeviceTime\");\n" +
                    "logger.info(\"Device time: {}\", deviceTime);\n\n" +
                    "// Execute custom script\n" +
                    "Map<String, Object> params = new HashMap<>();\n" +
                    "params.put(\"x\", 100);\n" +
                    "params.put(\"y\", 200);\n" +
                    "mobileKeyword.executeScript(\"mobile: tap\", params);\n\n" +
                    "// Android: Start activity\n" +
                    "mobileKeyword.executeScript(\"mobile: startActivity\", \n" +
                    "    Map.of(\"appPackage\", \"com.example.app\", \"appActivity\", \".MainActivity\"));",
            note = "Áp dụng cho nền tảng Mobile. Hoạt động trên cả Android và iOS. " +
                    "Script syntax phụ thuộc vào platform (Android vs iOS). " +
                    "Mobile commands phải prefix với 'mobile: '. " +
                    "Có thể throw WebDriverException nếu script không hợp lệ hoặc không được hỗ trợ. " +
                    "Arguments được pass theo thứ tự hoặc dùng Map cho named parameters."
    )
    public Object executeScript(String script, Object... args) {
        return execute(() -> {
            return getAppiumDriver().executeScript(script, args);
        }, script, args);
    }

}