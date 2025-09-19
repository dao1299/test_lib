package com.vtnet.netat.core.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import io.qameta.allure.Step;
import org.testng.Assert;

import java.math.BigDecimal;

public class AssertionKeyword extends BaseKeyword {

    @NetatKeyword(
            name = "assertAll",
            description = "Thực hiện kiểm tra tất cả các soft assertion đã được thu thập trước đó. Nếu có bất kỳ assertion nào thất bại, phương thức sẽ ném ra ngoại lệ với thông tin chi tiết về các lỗi. Sau khi gọi phương thức này, soft assert sẽ được reset về null.",
            category = "Assertion",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Sau khi thực hiện nhiều soft assertion\n" +
                    "softAssert.assertEquals(actualTitle, expectedTitle, \"Title không khớp\");\n" +
                    "softAssert.assertTrue(isElementDisplayed, \"Element không hiển thị\");\n" +
                    "// Thực hiện kiểm tra tất cả các assertion\n" +
                    "assertAll();"
    )
    @Step("Aggregate Soft Assert results")
    public void assertAll() {
        execute(() -> {
            ExecutionContext context = ExecutionContext.getInstance();
            if (context.getSoftAssert() != null) {
                context.getSoftAssert().assertAll();
            }
            // Reset lại sau khi assert
            context.setSoftAssert(null);
            return null;
        });
    }

    @NetatKeyword(
            name = "assertEquals",
            description = "Khẳng định rằng hai giá trị bằng nhau. Tự động xử lý so sánh giữa các kiểu dữ liệu khác nhau (ví dụ: số 1 và chuỗi \"1\"). Nếu không, kịch bản sẽ DỪNG LẠI.",
            category = "Assertion",
            parameters = {
                    "actualValue: Object - Giá trị thực tế (có thể là một biến).",
                    "expectedValue: Object - Giá trị mong đợi."
            },
            note = "Keyword này so sánh giá trị sau khi đã chuyển đổi chúng sang dạng chuỗi ký tự."
    )
    @Step("Assert that '{0}' equals '{1}'")
    public void assertEquals(Object actualValue, Object expectedValue) {
        execute(() -> {
            // Chuyển đổi cả hai giá trị sang String để so sánh một cách linh hoạt
            String actualStr = String.valueOf(actualValue);
            String expectedStr = String.valueOf(expectedValue);

            Assert.assertEquals(actualStr, expectedStr,
                    "ASSERT FAILED: The actual value '" + actualStr + "' was not equal to the expected value '" + expectedStr + "'.");
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertNotEquals",
            description = "Khẳng định rằng hai giá trị không bằng nhau. Tự động xử lý so sánh giữa các kiểu dữ liệu khác nhau. Nếu chúng bằng nhau, kịch bản sẽ DỪNG LẠI.",
            category = "Assertion",
            parameters = {
                    "actualValue: Object - Giá trị thực tế.",
                    "unexpectedValue: Object - Giá trị không mong muốn."
            },
            note = "Keyword này so sánh giá trị sau khi đã chuyển đổi chúng sang dạng chuỗi ký tự."
    )
    @Step("Assert that '{0}' does not equal '{1}'")
    public void assertNotEquals(Object actualValue, Object unexpectedValue) {
        execute(() -> {
            // Chuyển đổi cả hai giá trị sang String để so sánh một cách linh hoạt
            String actualStr = String.valueOf(actualValue);
            String unexpectedStr = String.valueOf(unexpectedValue);

            Assert.assertNotEquals(actualStr, unexpectedStr,
                    "ASSERT FAILED: The actual value '" + actualStr + "' was equal to the unexpected value '" + unexpectedStr + "'.");
            return null;
        }, actualValue, unexpectedValue);
    }

    @NetatKeyword(
            name = "assertContains",
            description = "Khẳng định rằng một chuỗi văn bản chứa chuỗi con được chỉ định. Nếu không chứa, test case sẽ dừng lại và báo lỗi",
            category = "Assertion",
            parameters = {
                    "sourceText: String - Chuỗi văn bản nguồn cần kiểm tra",
                    "substring: String - Chuỗi con cần tìm trong văn bản nguồn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertContains(\"Hello World\", \"World\");",
            note = "Assertion này kiểm tra xem chuỗi nguồn có chứa chuỗi con hay không. Phân biệt chữ hoa chữ thường. Sẽ dừng test nếu không tìm thấy chuỗi con."
    )
    @Step("Assert that '{0}' contains '{1}'")
    public void assertContains(String sourceText, String substring) {
        execute(() -> {
            Assert.assertTrue(sourceText != null && sourceText.contains(substring), "ASSERT FAILED: The source text did not contain the expected substring '" + substring + "'.");
            return null;
        }, sourceText, substring);
    }

    @NetatKeyword(
            name = "assertTrue",
            description = "Khẳng định rằng một điều kiện có giá trị true. Nếu điều kiện là false, test case sẽ dừng lại và báo lỗi",
            category = "Assertion",
            parameters = {
                    "condition: boolean - Điều kiện boolean cần kiểm tra (phải là true)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertTrue(isElementVisible);",
            note = "Assertion này kiểm tra điều kiện boolean phải là true. Thường được sử dụng để kiểm tra trạng thái hoặc kết quả của các phép so sánh."
    )
    @Step("Assert that condition '{0}' is true")
    public void assertTrue(boolean condition) {
        execute(() -> {
            Assert.assertTrue(condition, "ASSERT FAILED: The condition was not true.");
            return null;
        }, condition);
    }

    @NetatKeyword(
            name = "assertFalse",
            description = "Khẳng định rằng một điều kiện có giá trị false. Nếu điều kiện là true, test case sẽ dừng lại và báo lỗi",
            category = "Assertion",
            parameters = {
                    "condition: boolean - Điều kiện boolean cần kiểm tra (phải là false)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertFalse(isElementHidden);",
            note = "Assertion này kiểm tra điều kiện boolean phải là false. Thường được sử dụng để kiểm tra trạng thái phủ định hoặc kết quả của các phép so sánh."
    )
    @Step("Assert that condition '{0}' is false")
    public void assertFalse(boolean condition) {
        execute(() -> {
            Assert.assertFalse(condition, "ASSERT FAILED: The condition was not false.");
            return null;
        }, condition);
    }

    @NetatKeyword(
            name = "assertGreaterThan",
            description = "Khẳng định rằng giá trị thực tế lớn hơn giá trị so sánh. Nếu không thỏa mãn, test case sẽ dừng lại và báo lỗi",
            category = "Assertion",
            parameters = {
                    "actualValue: Object - Giá trị thực tế cần so sánh (số hoặc chuỗi số)",
                    "expectedValue: Object - Giá trị mong đợi để so sánh (actualValue phải lớn hơn giá trị này)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertGreaterThan(15, 10); // 15 > 10",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Assertion sẽ fail nếu actualValue <= expectedValue."
    )
    @Step("Assert that '{0}' is greater than '{1}'")
    public void assertGreaterThan(Object actualValue, Object expectedValue) {
        execute(() -> {
            performNumericComparison(actualValue, expectedValue, Comparison.GREATER_THAN);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertGreaterThanOrEqual",
            description = "Khẳng định rằng giá trị thực tế lớn hơn hoặc bằng giá trị so sánh. Nếu không thỏa mãn, test case sẽ dừng lại và báo lỗi",
            category = "Assertion",
            parameters = {
                    "actualValue: Object - Giá trị thực tế cần so sánh (số hoặc chuỗi số)",
                    "expectedValue: Object - Giá trị mong đợi để so sánh (actualValue phải lớn hơn hoặc bằng giá trị này)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertGreaterThanOrEqual(10, 10); // 10 >= 10",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Assertion sẽ fail nếu actualValue < expectedValue."
    )
    @Step("Assert that '{0}' is greater than or equal to '{1}'")
    public void assertGreaterThanOrEqual(Object actualValue, Object expectedValue) {
        execute(() -> {
            performNumericComparison(actualValue, expectedValue, Comparison.GREATER_THAN_OR_EQUAL);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertLessThan",
            description = "Khẳng định rằng giá trị thực tế nhỏ hơn giá trị so sánh. Nếu không thỏa mãn, test case sẽ dừng lại và báo lỗi",
            category = "Assertion",
            parameters = {
                    "actualValue: Object - Giá trị thực tế cần so sánh (số hoặc chuỗi số)",
                    "expectedValue: Object - Giá trị mong đợi để so sánh (actualValue phải nhỏ hơn giá trị này)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertLessThan(5, 10); // 5 < 10",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Assertion sẽ fail nếu actualValue >= expectedValue."
    )
    @Step("Assert that '{0}' is less than '{1}'")
    public void assertLessThan(Object actualValue, Object expectedValue) {
        execute(() -> {
            performNumericComparison(actualValue, expectedValue, Comparison.LESS_THAN);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertLessThanOrEqual",
            description = "Khẳng định rằng giá trị thực tế nhỏ hơn hoặc bằng giá trị so sánh. Nếu không thỏa mãn, test case sẽ dừng lại và báo lỗi",
            category = "Assertion",
            parameters = {
                    "actualValue: Object - Giá trị thực tế cần so sánh (số hoặc chuỗi số)",
                    "expectedValue: Object - Giá trị mong đợi để so sánh (actualValue phải nhỏ hơn hoặc bằng giá trị này)"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertLessThanOrEqual(10, 10); // 10 <= 10",
            note = "Tự động xử lý so sánh giữa số và chuỗi số. Assertion sẽ fail nếu actualValue > expectedValue."
    )
    @Step("Assert that '{0}' is less than or equal to '{1}'")
    public void assertLessThanOrEqual(Object actualValue, Object expectedValue) {
        execute(() -> {
            performNumericComparison(actualValue, expectedValue, Comparison.LESS_THAN_OR_EQUAL);
            return null;
        }, actualValue, expectedValue);
    }

    @NetatKeyword(
            name = "assertNotContains",
            description = "Khẳng định rằng một chuỗi văn bản không chứa chuỗi con được chỉ định. Nếu có chứa, test case sẽ dừng lại và báo lỗi",
            category = "Assertion",
            parameters = {
                    "sourceText: String - Chuỗi văn bản nguồn cần kiểm tra",
                    "substring: String - Chuỗi con không được phép có trong văn bản nguồn"
            },
            returnValue = "void - Không trả về giá trị",
            example = "assertNotContains(\"Hello World\", \"Error\");",
            note = "Assertion này kiểm tra xem chuỗi nguồn không được chứa chuỗi con. Phân biệt chữ hoa chữ thường. Sẽ dừng test nếu tìm thấy chuỗi con."
    )
    @Step("Assert that '{0}' does not contain '{1}'")
    public void assertNotContains(String sourceText, String substring) {
        execute(() -> {
            // Sử dụng Assert.assertFalse để kiểm tra điều kiện phủ định
            Assert.assertFalse(sourceText != null && sourceText.contains(substring),
                    "ASSERT FAILED: The source text '" + sourceText + "' was found to contain the unexpected substring '" + substring + "'.");
            return null;
        }, sourceText, substring);
    }



    private enum Comparison {
        GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL
    }

    private void performNumericComparison(Object actual, Object expected, Comparison type) {
        try {
            // Sử dụng BigDecimal để xử lý cả số nguyên và số thực một cách chính xác
            BigDecimal actualNumber = new BigDecimal(String.valueOf(actual));
            BigDecimal expectedNumber = new BigDecimal(String.valueOf(expected));
            int compareResult = actualNumber.compareTo(expectedNumber);

            switch (type) {
                case GREATER_THAN:
                    Assert.assertTrue(compareResult > 0, "ASSERT FAILED: Expected '" + actual + "' to be greater than '" + expected + "'.");
                    break;
                case GREATER_THAN_OR_EQUAL:
                    Assert.assertTrue(compareResult >= 0, "ASSERT FAILED: Expected '" + actual + "' to be greater than or equal to '" + expected + "'.");
                    break;
                case LESS_THAN:
                    Assert.assertTrue(compareResult < 0, "ASSERT FAILED: Expected '" + actual + "' to be less than '" + expected + "'.");
                    break;
                case LESS_THAN_OR_EQUAL:
                    Assert.assertTrue(compareResult <= 0, "ASSERT FAILED: Expected '" + actual + "' to be less than or equal to '" + expected + "'.");
                    break;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot compare values. Both actual ('" + actual + "') and expected ('" + expected + "') must be convertible to numbers.", e);
        }
    }
}