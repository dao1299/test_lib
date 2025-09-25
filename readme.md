# NETAT Libraries

NETAT Libraries là một bộ thư viện tự động kiểm thử đa nền tảng được xây dựng trên hệ sinh thái Java và Maven. Mục tiêu của dự án là cung cấp cho kỹ sư QA và nhà phát triển một nền tảng low-code linh hoạt, giúp chuẩn hóa, tự động hóa và mở rộng các kịch bản kiểm thử web, mobile, API và cơ sở dữ liệu trong doanh nghiệp.

## Chức năng & Nhiệm vụ chính
- **Tách biệt dữ liệu và logic kiểm thử**: Locator, dữ liệu thử nghiệm và cấu hình môi trường được quản lý riêng biệt để tăng khả năng tái sử dụng và dễ bảo trì.
- **Quản lý driver thống nhất**: Hỗ trợ Selenium WebDriver và Appium cho cả môi trường local và remote (Selenium Grid/Appium Server) thông qua cơ chế profile cấu hình.
- **Kho từ khóa phong phú**: Cung cấp sẵn tập hợp keyword cho Web, Mobile, Database và các kênh mở rộng giúp tập trung vào luồng nghiệp vụ thay vì chi tiết kỹ thuật.
- **Hỗ trợ kiểm thử data-driven**: Đọc dữ liệu từ Excel, CSV, JSON để chạy lặp kịch bản theo nhiều bộ dữ liệu.
- **Báo cáo chi tiết**: Tích hợp Allure và cơ chế logging giúp truy vết từng bước kiểm thử kèm ảnh chụp màn hình khi lỗi.
- **Tự chữa lỗi AI (tùy chọn)**: Tự động đề xuất locator dự phòng thông qua các mô hình ngôn ngữ khi giao diện thay đổi.

### Kiến trúc module
- `netat-core`: Runtime lõi, annotation `@NetatKeyword`, util cho logging, cấu hình và đọc dữ liệu.
- `netat-driver`: Quản lý vòng đời WebDriver/AppiumDriver với khả năng chạy song song.
- `netat-web`: Bộ keyword tự động hóa web và kho đối tượng UI JSON.
- `netat-mobile`: Keyword cho ứng dụng di động native/hybrid dựa trên Appium.
- `netat-db`: Kết nối cơ sở dữ liệu, thực thi truy vấn và xác minh dữ liệu.
- `netat-api`, `netat-tools`, `netat-report`: Các module đang phát triển cho kiểm thử API, công cụ hỗ trợ và báo cáo nâng cao.

## Hướng dẫn cài đặt
1. **Chuẩn bị môi trường**
   - Cài đặt [Java 11+](https://adoptium.net/) và [Apache Maven 3.8+](https://maven.apache.org/download.cgi).
   - Đảm bảo đã cấu hình biến môi trường `JAVA_HOME` và `MAVEN_HOME`.
2. **Lấy mã nguồn**
   ```bash
   git clone https://github.com/<organisation>/netat-libraries.git
   cd netat-libraries
   ```
3. **Cài đặt dependencies và build**
   ```bash
   mvn clean install
   ```
   Lệnh trên sẽ build toàn bộ multi-module và cài đặt các artifact vào local Maven repository.

## Hướng dẫn sử dụng
1. **Cấu hình môi trường chạy**
   - Sao chép file mẫu trong `helper/config-template` (nếu có) sang `src/test/resources/config/default.properties` hoặc thư mục tương ứng của module.
   - Khai báo các khóa phổ biến: `platform.name`, `run.mode`, cấu hình trình duyệt/thiết bị, thông tin Selenium Grid/Appium, thông tin AI provider.
2. **Định nghĩa kho đối tượng và dữ liệu**
   - Tạo các file JSON trong `resources/ui_objects` mô tả locator cho từng màn hình.
   - Tạo dữ liệu kiểm thử trong `resources/data_sources` (Excel/CSV/JSON) và đăng ký trong file nguồn dữ liệu.
3. **Viết kịch bản kiểm thử**
   - Sử dụng TestNG/JUnit để tạo test class và gọi các keyword từ module tương ứng (`WebKeyword`, `MobileKeyword`, `DatabaseKeyword`, ...).
   - Ví dụ (TestNG + data-driven):
     ```java
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
     ```
4. **Thực thi và báo cáo**
   - Chạy test bằng Maven hoặc IDE:
     ```bash
     mvn test -pl netat-web
     ```
   - Sinh và xem báo cáo Allure:
     ```bash
     allure serve target/allure-results
     ```
5. **Mở rộng & Tùy biến**
   - Tạo keyword mới bằng cách kế thừa `BaseKeyword` hoặc sử dụng annotation `@NetatKeyword`.
   - Thêm module/phụ thuộc mới vào `pom.xml` khi cần mở rộng phạm vi kiểm thử.

## Tham khảo
- Tài liệu Maven: <https://maven.apache.org/guides/>
- Selenium: <https://www.selenium.dev/documentation/>
- Appium: <https://appium.io/docs/en/about-appium/intro/>

