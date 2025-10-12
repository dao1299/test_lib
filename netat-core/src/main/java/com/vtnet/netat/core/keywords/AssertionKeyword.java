package com.vtnet.netat.core.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import io.qameta.allure.Step;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;

public class AssertionKeyword extends BaseKeyword {

    @NetatKeyword(
            name = "assertAll",
            description = "Thực hiện kiểm tra tất cả các soft assertion đã được thu thập trước đó. Nếu có bất kỳ assertion nào thất bại, phương thức sẽ ném ra ngoại lệ với thông tin chi tiết về các lỗi. Sau khi gọi phương thức này, soft assert sẽ được reset về null. Hãy gọi phương thức này bên trong @Test có sử dụng soft assert",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi có assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Sau khi thực hiện nhiều soft assertion\n" +
                    "softAssert.assertEquals(actualTitle, expectedTitle, \"Title không khớp\");\n" +
                    "softAssert.assertTrue(isElementDisplayed, \"Element không hiển thị\");\n" +
                    "// Thực hiện kiểm tra tất cả các assertion\n" +
                    "assertAll();\n\n" +
                    "// Với custom message\n" +
                    "assertAll(\"Kiểm tra tất cả các assertion trong test case đăng nhập\");"
    )
    @Step("Aggregate Soft Assert results")
    public void assertAll(String... customMessage) {
        execute(() -> {
            try {
                com.vtnet.netat.core.context.ExecutionContext.getInstance().assertAllSoftAndReset();
            } catch (AssertionError e) {
                if (customMessage.length > 0) {
                    throw new AssertionError(customMessage[0] + " - " + e.getMessage(), e);
                }
                throw e;
            }
            return null;
        });
    }

    @NetatKeyword(
            name = "assertEquals",
            description = "Khẳng định rằng hai giá trị bằng nhau. Tự động xử lý so sánh giữa các kiểu dữ liệu khác nhau (ví dụ: số 1 và chuỗi \"1\"). Nếu không, kịch bản sẽ DỪNG LẠI.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "actualValue: Object - Giá trị thực tế (có thể là một biến)",
                    "expectedValue: Object - Giá trị mong đợi",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertEquals(actualTitle, expectedTitle);\n" +
                    "assertEquals(userAge, 25, \"Tuổi người dùng phải là 25\");\n" +
                    "assertEquals(loginStatus, \"success\", \"Trạng thái đăng nhập phải là thành công\");",
            note = "Keyword này so sánh giá trị sau khi đã chuyển đổi chúng sang dạng chuỗi ký tự."
    )
    @Step("Assert that '{0}' equals '{1}'")
    public void assertEquals(Object actualValue, Object expectedValue, String... customMessage) {
        execute(() -> {
            String actualStr = String.valueOf(actualValue);
            String expectedStr = String.valueOf(expectedValue);

            String message = customMessage.length > 0 ? customMessage[0] :
                    "ASSERT FAILED: The actual value '" + actualStr + "' was not equal to the expected value '" + expectedStr + "'.";

            Assert.assertEquals(actualStr, expectedStr, message);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertNotEquals",
            description = "Khẳng định rằng hai giá trị không bằng nhau. Tự động xử lý so sánh giữa các kiểu dữ liệu khác nhau. Nếu chúng bằng nhau, kịch bản sẽ DỪNG LẠI.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "actualValue: Object - Giá trị thực tế",
                    "unexpectedValue: Object - Giá trị không mong muốn",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertNotEquals(actualPassword, oldPassword);\n" +
                    "assertNotEquals(currentStatus, \"error\", \"Trạng thái không được là lỗi\");\n" +
                    "assertNotEquals(userRole, \"guest\", \"Vai trò người dùng không được là khách\");",
            note = "Keyword này so sánh giá trị sau khi đã chuyển đổi chúng sang dạng chuỗi ký tự."
    )
    @Step("Assert that '{0}' does not equal '{1}'")
    public void assertNotEquals(Object actualValue, Object unexpectedValue, String... customMessage) {
        execute(() -> {
            String actualStr = String.valueOf(actualValue);
            String unexpectedStr = String.valueOf(unexpectedValue);

            String message = customMessage.length > 0 ? customMessage[0] :
                    "ASSERT FAILED: The actual value '" + actualStr + "' was equal to the unexpected value '" + unexpectedStr + "'.";

            Assert.assertNotEquals(actualStr, unexpectedStr, message);
            return null;
        }, actualValue, unexpectedValue);
    }

    @NetatKeyword(
            name = "assertContains",
            description = "Khẳng định rằng một chuỗi văn bản chứa chuỗi con được chỉ định. Nếu không chứa, test case sẽ dừng lại và báo lỗi.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "sourceText: String - Chuỗi văn bản nguồn cần kiểm tra",
                    "substring: String - Chuỗi con cần tìm trong văn bản nguồn",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertContains(\"Hello World\", \"World\");\n" +
                    "assertContains(pageTitle, \"Dashboard\", \"Tiêu đề trang phải chứa từ Dashboard\");\n" +
                    "assertContains(errorMessage, \"required\", \"Thông báo lỗi phải chứa từ required\");",
            note = "Assertion này kiểm tra xem chuỗi nguồn có chứa chuỗi con hay không. Phân biệt chữ hoa chữ thường. Sẽ dừng test nếu không tìm thấy chuỗi con."
    )
    @Step("Assert that '{0}' contains '{1}'")
    public void assertContains(String sourceText, String substring, String... customMessage) {
        execute(() -> {
            String message = customMessage.length > 0 ? customMessage[0] :
                    "ASSERT FAILED: The source text did not contain the expected substring '" + substring + "'.";

            Assert.assertTrue(sourceText != null && sourceText.contains(substring), message);
            return null;
        }, sourceText, substring);
    }

    @NetatKeyword(
            name = "assertTrue",
            description = "Khẳng định rằng một điều kiện có giá trị true. Nếu điều kiện là false, test case sẽ dừng lại và báo lỗi.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "condition: boolean - Điều kiện boolean cần kiểm tra (phải là true)",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertTrue(isElementVisible);\n" +
                    "assertTrue(isLoggedIn, \"Người dùng phải đã đăng nhập\");\n" +
                    "assertTrue(isFormValid, \"Form phải hợp lệ trước khi submit\");",
            note = "Assertion này kiểm tra điều kiện boolean phải là true. Thường được sử dụng để kiểm tra trạng thái hoặc kết quả của các phép so sánh."
    )
    @Step("Assert that condition '{0}' is true")
    public void assertTrue(boolean condition, String... customMessage) {
        execute(() -> {
            String message = customMessage.length > 0 ? customMessage[0] :
                    "ASSERT FAILED: The condition was not true.";

            Assert.assertTrue(condition, message);
            return null;
        }, condition);
    }

    @NetatKeyword(
            name = "assertFalse",
            description = "Khẳng định rằng một điều kiện có giá trị false. Nếu điều kiện là true, test case sẽ dừng lại và báo lỗi.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "condition: boolean - Điều kiện boolean cần kiểm tra (phải là false)",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertFalse(isElementHidden);\n" +
                    "assertFalse(hasError, \"Không được có lỗi trong quá trình xử lý\");\n" +
                    "assertFalse(isFormSubmitted, \"Form không được submit khi có lỗi validation\");",
            note = "Assertion này kiểm tra điều kiện boolean phải là false. Thường được sử dụng để kiểm tra trạng thái phủ định hoặc kết quả của các phép so sánh."
    )
    @Step("Assert that condition '{0}' is false")
    public void assertFalse(boolean condition, String... customMessage) {
        execute(() -> {
            String message = customMessage.length > 0 ? customMessage[0] :
                    "ASSERT FAILED: The condition was not false.";

            Assert.assertFalse(condition, message);
            return null;
        }, condition);
    }

    @NetatKeyword(
            name = "assertGreaterThan",
            description = "Khẳng định rằng giá trị thực tế lớn hơn giá trị so sánh. Nếu không thỏa mãn, test case sẽ dừng lại và báo lỗi.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "actualValue: Object - Giá trị thực tế cần so sánh (số hoặc chuỗi số)",
                    "expectedValue: Object - Giá trị mong đợi để so sánh (actualValue phải lớn hơn giá trị này)",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertGreaterThan(15, 10);\n" +
                    "assertGreaterThan(userAge, 18, \"Tuổi người dùng phải lớn hơn 18\");\n" +
                    "assertGreaterThan(totalAmount, 0, \"Tổng số tiền phải lớn hơn 0\");",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Assertion sẽ fail nếu actualValue <= expectedValue."
    )
    @Step("Assert that '{0}' is greater than '{1}'")
    public void assertGreaterThan(Object actualValue, Object expectedValue, String... customMessage) {
        execute(() -> {
            performNumericComparison(actualValue, expectedValue, Comparison.GREATER_THAN, false, customMessage);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertGreaterThanOrEqual",
            description = "Khẳng định rằng giá trị thực tế lớn hơn hoặc bằng giá trị so sánh. Nếu không thỏa mãn, test case sẽ dừng lại và báo lỗi.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "actualValue: Object - Giá trị thực tế cần so sánh (số hoặc chuỗi số)",
                    "expectedValue: Object - Giá trị mong đợi để so sánh (actualValue phải lớn hơn hoặc bằng giá trị này)",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertGreaterThanOrEqual(10, 10);\n" +
                    "assertGreaterThanOrEqual(score, 60, \"Điểm số phải từ 60 trở lên\");\n" +
                    "assertGreaterThanOrEqual(quantity, 1, \"Số lượng phải ít nhất là 1\");",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Assertion sẽ fail nếu actualValue < expectedValue."
    )
    @Step("Assert that '{0}' is greater than or equal to '{1}'")
    public void assertGreaterThanOrEqual(Object actualValue, Object expectedValue, String... customMessage) {
        execute(() -> {
            performNumericComparison(actualValue, expectedValue, Comparison.GREATER_THAN_OR_EQUAL, false, customMessage);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertLessThan",
            description = "Khẳng định rằng giá trị thực tế nhỏ hơn giá trị so sánh. Nếu không thỏa mãn, test case sẽ dừng lại và báo lỗi.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "actualValue: Object - Giá trị thực tế cần so sánh (số hoặc chuỗi số)",
                    "expectedValue: Object - Giá trị mong đợi để so sánh (actualValue phải nhỏ hơn giá trị này)",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertLessThan(5, 10);\n" +
                    "assertLessThan(responseTime, 3000, \"Thời gian phản hồi phải dưới 3 giây\");\n" +
                    "assertLessThan(errorCount, 5, \"Số lỗi phải ít hơn 5\");",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Assertion sẽ fail nếu actualValue >= expectedValue."
    )
    @Step("Assert that '{0}' is less than '{1}'")
    public void assertLessThan(Object actualValue, Object expectedValue, String... customMessage) {
        execute(() -> {
            performNumericComparison(actualValue, expectedValue, Comparison.LESS_THAN, false, customMessage);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertLessThanOrEqual",
            description = "Khẳng định rằng giá trị thực tế nhỏ hơn hoặc bằng giá trị so sánh. Nếu không thỏa mãn, test case sẽ dừng lại và báo lỗi.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "actualValue: Object - Giá trị thực tế cần so sánh (số hoặc chuỗi số)",
                    "expectedValue: Object - Giá trị mong đợi để so sánh (actualValue phải nhỏ hơn hoặc bằng giá trị này)",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertLessThanOrEqual(10, 10);\n" +
                    "assertLessThanOrEqual(fileSize, maxSize, \"Kích thước file không được vượt quá giới hạn\");\n" +
                    "assertLessThanOrEqual(attempts, 3, \"Số lần thử không được quá 3\");",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Assertion sẽ fail nếu actualValue > expectedValue."
    )
    @Step("Assert that '{0}' is less than or equal to '{1}'")
    public void assertLessThanOrEqual(Object actualValue, Object expectedValue, String... customMessage) {
        execute(() -> {
            performNumericComparison(actualValue, expectedValue, Comparison.LESS_THAN_OR_EQUAL, false, customMessage);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertNotContains",
            description = "Khẳng định rằng một chuỗi văn bản không chứa chuỗi con được chỉ định. Nếu có chứa, test case sẽ dừng lại và báo lỗi.",
            category = "Assertion",
            subCategory = "HardAssert",
            parameters = {
                    "sourceText: String - Chuỗi văn bản nguồn cần kiểm tra",
                    "substring: String - Chuỗi con không được phép có trong văn bản nguồn",
                    "customMessage: String (optional) - Thông báo tùy chỉnh khi assertion thất bại"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertNotContains(\"Hello World\", \"Error\");\n" +
                    "assertNotContains(successMessage, \"failed\", \"Thông báo thành công không được chứa từ failed\");\n" +
                    "assertNotContains(userEmail, \"@temp\", \"Email người dùng không được là email tạm thời\");",
            note = "Assertion này kiểm tra xem chuỗi nguồn không được chứa chuỗi con. Phân biệt chữ hoa chữ thường. Sẽ dừng test nếu tìm thấy chuỗi con."
    )
    @Step("Assert that '{0}' does not contain '{1}'")
    public void assertNotContains(String sourceText, String substring, String... customMessage) {
        execute(() -> {
            String message = customMessage.length > 0 ? customMessage[0] :
                    "ASSERT FAILED: The source text '" + sourceText + "' was found to contain the unexpected substring '" + substring + "'.";

            Assert.assertFalse(sourceText != null && sourceText.contains(substring), message);
            return null;
        }, sourceText, substring);
    }



    private enum Comparison {
        GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL
    }

    private void performNumericComparison(Object actual, Object expected, Comparison type, boolean isSoft, String... customMessage) {
        try {
            BigDecimal actualNumber = new BigDecimal(String.valueOf(actual));
            BigDecimal expectedNumber = new BigDecimal(String.valueOf(expected));
            int compareResult = actualNumber.compareTo(expectedNumber);
            boolean condition = false;
            String comparisonString = "";

            switch (type) {
                case GREATER_THAN:
                    condition = compareResult > 0;
                    comparisonString = "to be greater than";
                    break;
                case GREATER_THAN_OR_EQUAL:
                    condition = compareResult >= 0;
                    comparisonString = "to be greater than or equal to";
                    break;
                case LESS_THAN:
                    condition = compareResult < 0;
                    comparisonString = "to be less than";
                    break;
                case LESS_THAN_OR_EQUAL:
                    condition = compareResult <= 0;
                    comparisonString = "to be less than or equal to";
                    break;
            }

            // Tạo message cơ bản
            String baseMessage = String.format("Expected '%s' %s '%s'.", actual, comparisonString, expected);

            // Thêm custom message nếu có
            String finalMessage = baseMessage;
            if (customMessage.length > 0 && customMessage[0] != null && !customMessage[0].trim().isEmpty()) {
                finalMessage = baseMessage + "\nCustom message: " + customMessage[0];
            }

            if (isSoft) {
                getSoftAssert().assertTrue(condition, "SOFT ASSERT FAILED: " + finalMessage);
            } else {
                Assert.assertTrue(condition, "HARD ASSERT FAILED: " + finalMessage);
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot compare values. Both actual ('" + actual + "') and expected ('" + expected + "') must be convertible to numbers.", e);
        }
    }


    private SoftAssert getSoftAssert() {
        return ExecutionContext.getInstance().getSoftAssert();
    }

    @NetatKeyword(
            name = "softAssertEquals",
            description = "Kiểm tra hai giá trị có bằng nhau không. Nếu không, ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "actualValue: Object - Giá trị thực tế",
                    "expectedValue: Object - Giá trị mong đợi",
                    "message: String (tùy chọn) - Thông điệp tùy chỉnh khi assertion fail"
            },
            example = "String pageTitle = web.getPageTitle();\n" +
                    "assertion.softAssertEquals(pageTitle, \"Trang chủ\", \"Tiêu đề trang không đúng\");",
            note = "So sánh giá trị sau khi chuyển sang dạng chuỗi. Lỗi sẽ được ghi nhận và test vẫn tiếp tục. **Phải gọi `assertAll()` ở cuối** để tổng hợp kết quả.",
            explainer = "Assert (Soft) that '{0}' equals '{1}'"
    )
    public void softAssertEquals(Object actualValue, Object expectedValue, String... message) {
        execute(() -> {
            String customMessage = (message.length > 0 && message[0] != null && !message[0].trim().isEmpty())
                    ? "\nCustom message: " + message[0] : "";
            getSoftAssert().assertEquals(String.valueOf(actualValue), String.valueOf(expectedValue),
                    "SOFT ASSERT FAILED: The actual value '" + actualValue + "' was not equal to the expected value '" + expectedValue + "'." + customMessage);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "softAssertNotEquals",
            description = "Kiểm tra hai giá trị có khác nhau không. Nếu chúng bằng nhau, ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "actualValue: Object - Giá trị thực tế",
                    "unexpectedValue: Object - Giá trị không mong muốn",
                    "message: String (tùy chọn) - Thông điệp tùy chỉnh khi assertion fail"
            },
            example = "String userStatus = db.getCellValue(\"SELECT status FROM users\");\n" +
                    "assertion.softAssertNotEquals(userStatus, \"INACTIVE\", \"Trạng thái user không được là INACTIVE\");",
            note = "So sánh giá trị sau khi chuyển sang dạng chuỗi. Lỗi sẽ được ghi nhận và test vẫn tiếp tục. **Phải gọi `assertAll()` ở cuối** để tổng hợp kết quả.",
            explainer = "Assert (Soft) that '{0}' is not equal to '{1}'"
    )
    public void softAssertNotEquals(Object actualValue, Object unexpectedValue, String... message) {
        execute(() -> {
            String customMessage = (message.length > 0 && message[0] != null && !message[0].trim().isEmpty())
                    ? "\nCustom message: " + message[0] : "";
            getSoftAssert().assertNotEquals(String.valueOf(actualValue), String.valueOf(unexpectedValue),
                    "SOFT ASSERT FAILED: The actual value '" + actualValue + "' was equal to the unexpected value '" + unexpectedValue + "'." + customMessage);
            return null;
        }, actualValue, unexpectedValue);
    }

    @NetatKeyword(
            name = "softAssertTrue",
            description = "Kiểm tra một điều kiện là true. Nếu là false, ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "condition: boolean - Điều kiện cần kiểm tra",
                    "message: String (tùy chọn) - Thông điệp tùy chỉnh khi assertion fail"
            },
            example = "boolean isVisible = web.isElementPresent(\"myElement\", 5);\n" +
                    "assertion.softAssertTrue(isVisible, \"Element 'myElement' phải hiển thị.\");",
            note = "Lỗi sẽ được ghi nhận và test vẫn tiếp tục. **Phải gọi `assertAll()` ở cuối** để tổng hợp kết quả.",
            explainer = "Assert (Soft) that '{0}' is true"
    )
    public void softAssertTrue(boolean condition, String... message) {
        execute(() -> {
            String customMessage = (message.length > 0 && message[0] != null && !message[0].trim().isEmpty())
                    ? "\nCustom message: " + message[0] : "";
            getSoftAssert().assertTrue(condition, "SOFT ASSERT FAILED: The condition was not true." + customMessage);
            return null;
        }, condition);
    }

    @NetatKeyword(
            name = "softAssertFalse",
            description = "Kiểm tra một điều kiện là false. Nếu là true, ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "condition: boolean - Điều kiện cần kiểm tra",
                    "message: String (tùy chọn) - Thông điệp tùy chỉnh khi assertion fail"
            },
            example = "boolean isDisabled = web.getAttribute(\"myButton\", \"disabled\").equals(\"true\");\n" +
                    "assertion.softAssertFalse(isDisabled, \"Nút bấm không được phép bị vô hiệu hóa.\");",
            note = "Lỗi sẽ được ghi nhận và test vẫn tiếp tục. **Phải gọi `assertAll()` ở cuối** để tổng hợp kết quả.",
            explainer = "Assert (Soft) that '{0}' is false"
    )
    public void softAssertFalse(boolean condition, String... message) {
        execute(() -> {
            String customMessage = (message.length > 0 && message[0] != null && !message[0].trim().isEmpty())
                    ? "\nCustom message: " + message[0] : "";
            getSoftAssert().assertFalse(condition, "SOFT ASSERT FAILED: The condition was not false." + customMessage);
            return null;
        }, condition);
    }

    @NetatKeyword(
            name = "softAssertContains",
            description = "Kiểm tra một chuỗi có chứa một chuỗi con hay không. Nếu không, ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "sourceText: String - Chuỗi nguồn",
                    "substring: String - Chuỗi con cần tìm",
                    "message: String (tùy chọn) - Thông điệp tùy chỉnh khi assertion fail"
            },
            example = "String welcomeMsg = web.getText(\"welcomeLabel\");\n" +
                    "assertion.softAssertContains(welcomeMsg, \"Xin chào\", \"Thông điệp chào mừng không chứa text mong đợi\");",
            note = "Kiểm tra có phân biệt chữ hoa/thường. Lỗi sẽ được ghi nhận và test vẫn tiếp tục. **Phải gọi `assertAll()` ở cuối** để tổng hợp kết quả.",
            explainer = "Assert (Soft) that '{0}' contains '{1}'"
    )
    public void softAssertContains(String sourceText, String substring, String... message) {
        execute(() -> {
            String customMessage = (message.length > 0 && message[0] != null && !message[0].trim().isEmpty())
                    ? "\nCustom message: " + message[0] : "";
            boolean contains = sourceText != null && sourceText.contains(substring);
            getSoftAssert().assertTrue(contains, "SOFT ASSERT FAILED: The source text did not contain the expected substring '" + substring + "'." + customMessage);
            return null;
        }, sourceText, substring);
    }

    @NetatKeyword(
            name = "softAssertGreaterThan",
            description = "Kiểm tra giá trị thực tế có lớn hơn giá trị so sánh không. Nếu không, ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "actualValue: Object - Giá trị thực tế (số hoặc chuỗi số)",
                    "expectedValue: Object - Giá trị so sánh",
                    "message: String (tùy chọn) - Thông điệp tùy chỉnh khi assertion fail"
            },
            example = "int productCount = web.getElementCount(\"productList\");\n" +
                    "assertion.softAssertGreaterThan(productCount, 0, \"Số lượng sản phẩm phải lớn hơn 0\");",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Lỗi sẽ được ghi nhận và test vẫn tiếp tục. **Phải gọi `assertAll()` ở cuối** để tổng hợp kết quả.",
            explainer = "Assert (Soft) that '{actualValue}' is greater than '{expectedValue}'"
    )
    public void softAssertGreaterThan(Object actualValue, Object expectedValue, String... message) {
        execute(() -> {
            String customMessage = (message.length > 0 && message[0] != null && !message[0].trim().isEmpty())
                    ? message[0] : null;
            performNumericComparison(actualValue, expectedValue, Comparison.GREATER_THAN, true, customMessage);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "softAssertGreaterThanOrEqual",
            description = "Kiểm tra giá trị thực tế có lớn hơn hoặc bằng giá trị so sánh không. Nếu không, ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "actualValue: Object - Giá trị thực tế",
                    "expectedValue: Object - Giá trị so sánh",
                    "message: String (tùy chọn) - Thông điệp tùy chỉnh khi assertion fail"
            },
            example = "int stockQuantity = Integer.parseInt(web.getText(\"stockLabel\"));\n" +
                    "assertion.softAssertGreaterThanOrEqual(stockQuantity, 1, \"Số lượng tồn kho phải >= 1\");",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Lỗi sẽ được ghi nhận và test vẫn tiếp tục. **Phải gọi `assertAll()` ở cuối** để tổng hợp kết quả.",
            explainer = "Assert (Soft) that '{actualValue}' is greater than or equal to '{expectedValue}'"
    )
    public void softAssertGreaterThanOrEqual(Object actualValue, Object expectedValue, String... message) {
        execute(() -> {
            String customMessage = (message.length > 0 && message[0] != null && !message[0].trim().isEmpty())
                    ? message[0] : null;
            performNumericComparison(actualValue, expectedValue, Comparison.GREATER_THAN_OR_EQUAL, true, customMessage);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "softAssertLessThan",
            description = "Kiểm tra giá trị thực tế có nhỏ hơn giá trị so sánh không. Nếu không, ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "actualValue: Object - Giá trị thực tế",
                    "expectedValue: Object - Giá trị so sánh",
                    "message: String (tùy chọn) - Thông điệp tùy chỉnh khi assertion fail"
            },
            example = "double discount = Double.parseDouble(web.getText(\"discountValue\"));\n" +
                    "assertion.softAssertLessThan(discount, 50.0, \"Giảm giá không được vượt quá 50%\");",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Lỗi sẽ được ghi nhận và test vẫn tiếp tục. **Phải gọi `assertAll()` ở cuối** để tổng hợp kết quả.",
            explainer = "Assert (Soft) that '{actualValue}' is less than '{expectedValue}'"
    )
    public void softAssertLessThan(Object actualValue, Object expectedValue, String... message) {
        execute(() -> {
            String customMessage = (message.length > 0 && message[0] != null && !message[0].trim().isEmpty())
                    ? message[0] : null;
            performNumericComparison(actualValue, expectedValue, Comparison.LESS_THAN, true, customMessage);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "softAssertLessThanOrEqual",
            description = "Kiểm tra giá trị thực tế có nhỏ hơn hoặc bằng giá trị so sánh không. Nếu không, ghi nhận lỗi và TIẾP TỤC chạy.",
            category = "Assertion",
            subCategory = "Soft",
            parameters = {
                    "actualValue: Object - Giá trị thực tế",
                    "expectedValue: Object - Giá trị so sánh",
                    "message: String (tùy chọn) - Thông điệp tùy chỉnh khi assertion fail"
            },
            example = "int itemsInCart = Integer.parseInt(web.getText(\"cartCount\"));\n" +
                    "assertion.softAssertLessThanOrEqual(itemsInCart, 10, \"Giỏ hàng không được chứa quá 10 sản phẩm\");",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Lỗi sẽ được ghi nhận và test vẫn tiếp tục. **Phải gọi `assertAll()` ở cuối** để tổng hợp kết quả.",
            explainer = "Assert (Soft) that '{actualValue}' is less than or equal to '{expectedValue}'"
    )
    @Step("Assert (Soft) that '{0}' is less than or equal to '{1}'")
    public void softAssertLessThanOrEqual(Object actualValue, Object expectedValue, String... message) {
        execute(() -> {
            String customMessage = (message.length > 0 && message[0] != null && !message[0].trim().isEmpty())
                    ? message[0] : null;
            performNumericComparison(actualValue, expectedValue, Comparison.LESS_THAN_OR_EQUAL, true, customMessage);
            return null;
        }, actualValue, expectedValue);
    }
}