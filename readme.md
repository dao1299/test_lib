NETAT Libraries

NETAT Libraries là một framework tự động kiểm thử đa nền tảng được xây dựng trên Java và Maven. Nó cung cấp một giải pháp mạnh mẽ, linh hoạt và thân thiện với môi trường low-code cho các kỹ sư QA và nhà phát triển để xây dựng các kịch bản kiểm thử tự động nhanh chóng và hiệu quả.

Triết lý cốt lõi

Tách biệt dữ liệu và logic – Các định vị đối tượng giao diện (locator), dữ liệu thử nghiệm và cấu hình môi trường được định nghĩa riêng biệt khỏi logic của kịch bản thử nghiệm.

Tái sử dụng tối đa – Một tập hợp phong phú các “keyword” được định nghĩa trước bao gồm các hành động chung, giúp kịch bản tập trung vào luồng nghiệp vụ thay vì chi tiết kỹ thuật.

Hướng đến low-code/no-code – Kiến trúc được thiết kế để hỗ trợ các nền tảng no-code trong tương lai, nơi người dùng có thể lắp ráp bài test qua giao diện kéo thả.

Kiến trúc & Các module

NETAT Libraries được tổ chức thành một dự án Maven đa module. Mỗi module có trách nhiệm rõ ràng.

netat-core

Module này chứa runtime lõi được sử dụng bởi tất cả các module khác:

BaseKeyword và annotation @NetatKeyword điều khiển thực thi, retry, logging và báo cáo.

Hỗ trợ kiểm thử data‑driven qua DataFileHelper, DataUtils và các trình đọc cho file CSV và Excel.

Các tiện ích chung cho logging và cấu hình.

netat-driver

Chịu trách nhiệm quản lý vòng đời của WebDriver và AppiumDriver:

Hỗ trợ chạy local và remote (Selenium Grid) thông qua cấu hình profile.

Xử lý nhiều nền tảng – Web (Chrome, Firefox, v.v.) và Mobile (Android, iOS).

Sử dụng ThreadLocal để cô lập session cho chạy song song.

Tự động tải và thiết lập driver hoặc cho phép chỉ định đường dẫn driver tùy chỉnh.

netat-web

Cung cấp bộ từ khóa đầy đủ để tự động hóa ứng dụng web:

Lớp WebKeyword bao gồm các hành động như openUrl, click, sendKeys, scrollToElement, verifyElementText và hơn thế nữa.

Kho đối tượng UI dưới dạng file JSON định nghĩa các locator riêng biệt với hỗ trợ nhiều locator dự phòng.

UiObjectHelper cache và truy xuất định nghĩa ObjectUI.

Cơ chế tự chữa lỗi AI gửi HTML trang đến mô hình ngôn ngữ được cấu hình (ví dụ Gemini, Qwen) để sinh CSS selector mới khi tất cả locator đã định nghĩa thất bại.

Tích hợp với Allure để báo cáo chi tiết theo bước và tự động chụp ảnh màn hình khi lỗi.

netat-mobile

Mở rộng framework cho ứng dụng di động native và hybrid:

MobileKeyword cung cấp các hành động như tap, swipe, pinchToZoom và sendKeys.

Chia sẻ cùng kiến trúc data‑driven và kho đối tượng như web.

Xây dựng trên Appium để hỗ trợ cả Android và iOS.

netat-db

Cho phép bài test tương tác trực tiếp với cơ sở dữ liệu quan hệ:

ConnectionManager sử dụng pool kết nối HikariCP khóa bằng tên profile.

DatabaseKeyword cung cấp executeQuery, executeUpdate, verifyDataSQL và verifyDataByQueryFile cho kiểm thử linh hoạt và tái sử dụng.

Hỗ trợ đọc câu lệnh SQL từ file .sql bên ngoài và dữ liệu mong đợi từ Excel hoặc CSV.

netat-api, netat-tools & netat-report (đang phát triển)

Các module trong tương lai sẽ bổ sung thử nghiệm API (sử dụng các framework như RestAssured), công cụ hỗ trợ (ví dụ sinh metadata keyword) và tổng hợp báo cáo tùy chỉnh.

Tính năng chính

Quản lý driver thông minh – dễ dàng chuyển đổi giữa trình duyệt/local và remote bằng cách thay đổi một dòng trong cấu hình; hỗ trợ nhiều driver và profile.

Kho đối tượng – định nghĩa tập trung các locator với nhiều dự phòng, dễ dàng bảo trì khi giao diện thay đổi.

Kiểm thử dữ liệu – trình đọc tích hợp cho Excel và CSV giúp bài test chạy lặp qua các tập dữ liệu lớn.

Tự chữa lỗi AI – tích hợp tùy chọn với mô hình ngôn ngữ để tạo locator mới khi tất cả locator định nghĩa thất bại, tăng khả năng chịu đựng thay đổi giao diện.

Báo cáo toàn diện – tích hợp Allure ghi lại mỗi lần gọi keyword cùng tham số và đính kèm ảnh khi lỗi.

Bắt đầu nhanh

Thêm NETAT Libraries vào dự án Maven của bạn bằng cách đưa các module làm phụ thuộc.

Định nghĩa cấu hình trong resources/config/default.properties – chỉ định platform.name, run.mode, cấu hình trình duyệt hoặc thiết bị, URL grid hoặc Appium, profile và nhà cung cấp AI.

Tạo định nghĩa đối tượng UI trong resources/ui_objects – ví dụ LoginPage/login_button.json với một mảng locator.

Cung cấp dữ liệu test trong resources/data_sources – định nghĩa nguồn dữ liệu JSON trỏ tới file Excel hoặc CSV.

Viết bài test của bạn sử dụng TestNG hoặc JUnit:

@DataProvider(name = "loginProvider")
public Object[][] loginProvider() {
return DataFileHelper.getTestData("login/credentials");
}

@Test(dataProvider = "loginProvider")
public void testLogin(Map<String, String> data) {
DriverManager.initDriver();
WebKeyword web = new WebKeyword();
try {
ObjectUI emailInput = UiObjectHelper.getObject("LoginPage/email_input");
ObjectUI loginButton = UiObjectHelper.getObject("LoginPage/login_button");

        web.openUrl("https://example.com/login");
        web.sendKeys(emailInput, data.get("username"));
        web.click(loginButton);

        ObjectUI welcomeMsg = UiObjectHelper.getObject("Dashboard/welcome_message");
        web.verifyElementText(welcomeMsg, data.get("expected_message"));
    } finally {
        DriverManager.quitDriver();
    }
}


Chạy các bài test bằng runner ưa thích (TestNG) và xem báo cáo Allure chi tiết được tạo sau khi chạy.