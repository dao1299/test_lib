package com.vtnet.netat.api.keywords;

import com.jayway.jsonpath.JsonPath;
import com.vtnet.netat.api.core.ApiResponse;
import com.vtnet.netat.api.core.BaseApiKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import io.qameta.allure.Step;
import org.testng.Assert;

import java.util.List;

/**
 * ⭐ API Assertion Keyword Class - Hybrid Approach
 * <p>
 * Chứa tất cả assertions và verifications cho API testing
 *
 * @author NETAT Framework
 * @version 2.0
 * <p>
 * Usage:
 * <pre>
 * ApiAssert verify = new ApiAssert();
 * verify.statusCode(response, 200);
 * verify.jsonPathEquals(response, "$.name", "John");
 * verify.responseTimeLessThan(response, 2000);
 * </pre>
 */
public class ApiAssert extends BaseApiKeyword {

    // ========================================================================
    //  SECTION 1: STATUS CODE ASSERTIONS (6 methods)
    // ========================================================================

    @NetatKeyword(
            name = "statusCode",
            description = "Kiểm tra status code bằng giá trị cụ thể",
            category = "API",
            subCategory = "Assertion - Status",
            parameters = {
                    "response: ApiResponse",
                    "expectedCode: int - Expected status code"
            },
            example = "verify.statusCode(response, 200);",
            explainer = "Assert status = {1}"
    )
    @Step("Assert status = {1}")
    public void statusCode(ApiResponse response, int expectedCode) {
        execute(() -> {
            int actual = response.getStatusCode();
            Assert.assertEquals(actual, expectedCode,
                    String.format("Expected status %d but got %d", expectedCode, actual));
            logger.info("Status code {} verified", expectedCode);
            return null;
        }, response, expectedCode);
    }

    @NetatKeyword(
            name = "statusSuccess",
            description = "Kiểm tra status code là 2xx (success)",
            category = "API",
            subCategory = "Assertion - Status",
            parameters = {"response: ApiResponse"},
            example = "verify.statusSuccess(response);",
            explainer = "Assert status is 2xx"
    )
    @Step("Assert status is 2xx")
    public void statusSuccess(ApiResponse response) {
        execute(() -> {
            int status = response.getStatusCode();
            boolean isSuccess = status >= 200 && status < 300;
            Assert.assertTrue(isSuccess,
                    String.format("Expected 2xx status but got %d", status));
            logger.info("Status {} is successful (2xx)", status);
            return null;
        }, response);
    }

    @NetatKeyword(
            name = "statusClientError",
            description = "Kiểm tra status code là 4xx (client error)",
            category = "API",
            subCategory = "Assertion - Status",
            parameters = {"response: ApiResponse"},
            example = "verify.statusClientError(response);",
            explainer = "Assert status is 4xx"
    )
    @Step("Assert status is 4xx")
    public void statusClientError(ApiResponse response) {
        execute(() -> {
            int status = response.getStatusCode();
            boolean isClientError = status >= 400 && status < 500;
            Assert.assertTrue(isClientError,
                    String.format("Expected 4xx status but got %d", status));
            logger.info("Status {} is client error (4xx)", status);
            return null;
        }, response);
    }

    @NetatKeyword(
            name = "statusServerError",
            description = "Kiểm tra status code là 5xx (server error)",
            category = "API",
            subCategory = "Assertion - Status",
            parameters = {"response: ApiResponse"},
            example = "verify.statusServerError(response);",
            explainer = "Assert status is 5xx"
    )
    @Step("Assert status is 5xx")
    public void statusServerError(ApiResponse response) {
        execute(() -> {
            int status = response.getStatusCode();
            boolean isServerError = status >= 500 && status < 600;
            Assert.assertTrue(isServerError,
                    String.format("Expected 5xx status but got %d", status));
            logger.info("Status {} is server error (5xx)", status);
            return null;
        }, response);
    }

    @NetatKeyword(
            name = "statusIn",
            description = "Kiểm tra status code nằm trong danh sách cho phép",
            category = "API",
            subCategory = "Assertion - Status",
            parameters = {
                    "response: ApiResponse",
                    "allowedCodes: String - Danh sách codes cách nhau bởi dấu phẩy (VD: 200,201,204)"
            },
            example = "verify.statusIn(response, \"200,201,204\");",
            explainer = "Assert status in [{1}]"
    )
    @Step("Assert status in [{1}]")
    public void statusIn(ApiResponse response, String allowedCodes) {
        execute(() -> {
            int actual = response.getStatusCode();
            String[] codes = allowedCodes.split(",");
            boolean found = false;

            for (String code : codes) {
                if (actual == Integer.parseInt(code.trim())) {
                    found = true;
                    break;
                }
            }

            Assert.assertTrue(found,
                    String.format("Status %d not in allowed list: [%s]", actual, allowedCodes));
            logger.info("Status {} is in allowed list", actual);
            return null;
        }, response, allowedCodes);
    }

    // ========================================================================
    //  SECTION 2: JSON PATH ASSERTIONS (15 methods)
    // ========================================================================

    @NetatKeyword(
            name = "jsonPathEquals",
            description = "Kiểm tra giá trị JSON path bằng với expected value",
            category = "API",
            subCategory = "Assertion - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String - JSON Path (VD: $.user.name)",
                    "expectedValue: Object - Giá trị mong đợi"
            },
            example = "verify.jsonPathEquals(response, \"$.name\", \"John Doe\");",
            explainer = "Assert {1} = {2}"
    )
    @Step("Assert {1} = {2}")
    public void jsonPathEquals(ApiResponse response, String jsonPath, Object expectedValue) {
        execute(() -> {
            String actual = response.getJsonPath(jsonPath);
            String expected = String.valueOf(expectedValue);
            Assert.assertEquals(actual, expected,
                    String.format("JSON path '%s': expected '%s' but got '%s'", jsonPath, expected, actual));
            logger.info("JSON path '{}' = '{}' verified", jsonPath, expected);
            return null;
        }, response, jsonPath, expectedValue);
    }

    @NetatKeyword(
            name = "jsonPathNotEquals",
            description = "Kiểm tra giá trị JSON path KHÔNG bằng với unexpected value",
            category = "API",
            subCategory = "Assertion - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String",
                    "unexpectedValue: Object"
            },
            example = "verify.jsonPathNotEquals(response, \"$.status\", \"error\");",
            explainer = "Assert {1} ≠ {2}"
    )
    @Step("Assert {1} ≠ {2}")
    public void jsonPathNotEquals(ApiResponse response, String jsonPath, Object unexpectedValue) {
        execute(() -> {
            String actual = response.getJsonPath(jsonPath);
            String unexpected = String.valueOf(unexpectedValue);
            Assert.assertNotEquals(actual, unexpected,
                    String.format("JSON path '%s' should NOT be '%s'", jsonPath, unexpected));
            logger.info("JSON path '{}' ≠ '{}' verified", jsonPath, unexpected);
            return null;
        }, response, jsonPath, unexpectedValue);
    }

    @NetatKeyword(
            name = "jsonPathExists",
            description = "Kiểm tra JSON path TỒN TẠI trong response",
            category = "API",
            subCategory = "Assertion - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            example = "verify.jsonPathExists(response, \"$.user.email\");",
            explainer = "Assert {1} exists"
    )
    @Step("Assert {1} exists")
    public void jsonPathExists(ApiResponse response, String jsonPath) {
        execute(() -> {
            try {
                JsonPath.read(response.getBody(), jsonPath);
                logger.info("JSON path '{}' exists", jsonPath);
            } catch (Exception e) {
                Assert.fail(String.format("JSON path '%s' should exist but was not found", jsonPath));
            }
            return null;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "jsonPathNotExists",
            description = "Kiểm tra JSON path KHÔNG TỒN TẠI (dùng cho security testing)",
            category = "API",
            subCategory = "Assertion - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            example = "verify.jsonPathNotExists(response, \"$.user.password\");",
            explainer = "Assert {1} NOT exists"
    )
    @Step("Assert {1} NOT exists")
    public void jsonPathNotExists(ApiResponse response, String jsonPath) {
        execute(() -> {
            try {
                Object value = JsonPath.read(response.getBody(), jsonPath);
                if (value != null) {
                    Assert.fail(String.format("JSON path '%s' should NOT exist but it does", jsonPath));
                }
            } catch (Exception e) {
                // Path not found - this is expected
                logger.info("JSON path '{}' does not exist (as expected)", jsonPath);
            }
            return null;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "jsonPathIsNull",
            description = "Kiểm tra giá trị tại JSON path là NULL",
            category = "API",
            subCategory = "Assertion - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            example = "verify.jsonPathIsNull(response, \"$.deletedAt\");",
            explainer = "Assert {1} is null"
    )
    @Step("Assert {1} is null")
    public void jsonPathIsNull(ApiResponse response, String jsonPath) {
        execute(() -> {
            try {
                Object value = JsonPath.read(response.getBody(), jsonPath);
                Assert.assertNull(value,
                        String.format("JSON path '%s' should be null but got '%s'", jsonPath, value));
                logger.info("JSON path '{}' is null", jsonPath);
            } catch (Exception e) {
                Assert.fail(String.format("Cannot find JSON path '%s'", jsonPath));
            }
            return null;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "jsonPathNotNull",
            description = "Kiểm tra giá trị tại JSON path KHÔNG NULL",
            category = "API",
            subCategory = "Assertion - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            example = "verify.jsonPathNotNull(response, \"$.user.id\");",
            explainer = "Assert {1} is NOT null"
    )
    @Step("Assert {1} is NOT null")
    public void jsonPathNotNull(ApiResponse response, String jsonPath) {
        execute(() -> {
            try {
                Object value = JsonPath.read(response.getBody(), jsonPath);
                Assert.assertNotNull(value,
                        String.format("JSON path '%s' should NOT be null", jsonPath));
                logger.info("JSON path '{}' is not null", jsonPath);
            } catch (Exception e) {
                Assert.fail(String.format("Cannot find JSON path '%s'", jsonPath));
            }
            return null;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "jsonPathContains",
            description = "Kiểm tra giá trị string chứa substring",
            category = "API",
            subCategory = "Assertion - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String",
                    "substring: String"
            },
            example = "verify.jsonPathContains(response, \"$.message\", \"success\");",
            explainer = "Assert {1} contains '{2}'"
    )
    @Step("Assert {1} contains '{2}'")
    public void jsonPathContains(ApiResponse response, String jsonPath, String substring) {
        execute(() -> {
            String actual = response.getJsonPath(jsonPath);
            Assert.assertTrue(actual != null && actual.contains(substring),
                    String.format("JSON path '%s' should contain '%s' but got '%s'", jsonPath, substring, actual));
            logger.info("JSON path '{}' contains '{}'", jsonPath, substring);
            return null;
        }, response, jsonPath, substring);
    }

    // TODO: Copy thêm jsonPathGreaterThan, jsonPathLessThan, jsonPathMatches (regex), etc.

    // ========================================================================
    //  SECTION 3: ARRAY ASSERTIONS (5 methods)
    // ========================================================================

    @NetatKeyword(
            name = "arraySize",
            description = "Kiểm tra kích thước array",
            category = "API",
            subCategory = "Assertion - Arrays",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String - Path đến array",
                    "expectedSize: int - Số lượng phần tử mong đợi"
            },
            example = "verify.arraySize(response, \"$.users\", 10);",
            explainer = "Assert array {1} size = {2}"
    )
    @Step("Assert array {1} size = {2}")
    public void arraySize(ApiResponse response, String jsonPath, int expectedSize) {
        execute(() -> {
            List<Object> list = response.getJsonPathAsList(jsonPath);
            int actual = list != null ? list.size() : 0;
            Assert.assertEquals(actual, expectedSize,
                    String.format("Array at '%s': expected size %d but got %d", jsonPath, expectedSize, actual));
            logger.info("Array '{}' size = {} verified", jsonPath, expectedSize);
            return null;
        }, response, jsonPath, expectedSize);
    }

    @NetatKeyword(
            name = "arrayNotEmpty",
            description = "Kiểm tra array KHÔNG RỖNG",
            category = "API",
            subCategory = "Assertion - Arrays",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            example = "verify.arrayNotEmpty(response, \"$.users\");",
            explainer = "Assert array {1} is not empty"
    )
    @Step("Assert array {1} is not empty")
    public void arrayNotEmpty(ApiResponse response, String jsonPath) {
        execute(() -> {
            List<Object> list = response.getJsonPathAsList(jsonPath);
            int size = list != null ? list.size() : 0;
            Assert.assertTrue(size > 0,
                    String.format("Array at '%s' should NOT be empty", jsonPath));
            logger.info("Array '{}' is not empty ({} items)", jsonPath, size);
            return null;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "arrayIsEmpty",
            description = "Kiểm tra array RỖNG",
            category = "API",
            subCategory = "Assertion - Arrays",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            example = "verify.arrayIsEmpty(response, \"$.errors\");",
            explainer = "Assert array {1} is empty"
    )
    @Step("Assert array {1} is empty")
    public void arrayIsEmpty(ApiResponse response, String jsonPath) {
        execute(() -> {
            List<Object> list = response.getJsonPathAsList(jsonPath);
            int size = list != null ? list.size() : 0;
            Assert.assertEquals(size, 0,
                    String.format("Array at '%s' should be empty but has %d items", jsonPath, size));
            logger.info("Array '{}' is empty", jsonPath);
            return null;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "arrayContains",
            description = "Kiểm tra array chứa giá trị cụ thể",
            category = "API",
            subCategory = "Assertion - Arrays",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String",
                    "expectedValue: Object"
            },
            example = "verify.arrayContains(response, \"$.roles\", \"admin\");",
            explainer = "Assert array {1} contains {2}"
    )
    @Step("Assert array {1} contains {2}")
    public void arrayContains(ApiResponse response, String jsonPath, Object expectedValue) {
        execute(() -> {
            List<Object> list = response.getJsonPathAsList(jsonPath);
            boolean contains = list != null && list.contains(expectedValue);
            Assert.assertTrue(contains,
                    String.format("Array at '%s' should contain '%s'", jsonPath, expectedValue));
            logger.info("Array '{}' contains '{}'", jsonPath, expectedValue);
            return null;
        }, response, jsonPath, expectedValue);
    }

    // ========================================================================
    //  SECTION 4: BODY ASSERTIONS (5 methods)
    // ========================================================================

    @NetatKeyword(
            name = "bodyEquals",
            description = "Kiểm tra toàn bộ response body bằng với expected string",
            category = "API",
            subCategory = "Assertion - Body",
            parameters = {
                    "response: ApiResponse",
                    "expectedBody: String"
            },
            example = "verify.bodyEquals(response, \"{\\\"status\\\":\\\"ok\\\"}\");",
            explainer = "Assert body equals expected"
    )
    @Step("Assert body equals expected")
    public void bodyEquals(ApiResponse response, String expectedBody) {
        execute(() -> {
            String actual = response.getBody();
            Assert.assertEquals(actual, expectedBody,
                    "Response body does not match expected");
            logger.info("Body equals expected value");
            return null;
        }, response, expectedBody);
    }

    @NetatKeyword(
            name = "bodyContains",
            description = "Kiểm tra body CHỨA substring",
            category = "API",
            subCategory = "Assertion - Body",
            parameters = {
                    "response: ApiResponse",
                    "substring: String"
            },
            example = "verify.bodyContains(response, \"success\");",
            explainer = "Assert body contains '{1}'"
    )
    @Step("Assert body contains '{1}'")
    public void bodyContains(ApiResponse response, String substring) {
        execute(() -> {
            String body = response.getBody();
            Assert.assertTrue(body.contains(substring),
                    String.format("Body should contain '%s'", substring));
            logger.info("Body contains '{}'", substring);
            return null;
        }, response, substring);
    }

    @NetatKeyword(
            name = "bodyNotContains",
            description = "Kiểm tra body KHÔNG CHỨA substring",
            category = "API",
            subCategory = "Assertion - Body",
            parameters = {
                    "response: ApiResponse",
                    "substring: String"
            },
            example = "verify.bodyNotContains(response, \"error\");",
            explainer = "Assert body NOT contains '{1}'"
    )
    @Step("Assert body NOT contains '{1}'")
    public void bodyNotContains(ApiResponse response, String substring) {
        execute(() -> {
            String body = response.getBody();
            Assert.assertFalse(body.contains(substring),
                    String.format("Body should NOT contain '%s'", substring));
            logger.info("Body does not contain '{}'", substring);
            return null;
        }, response, substring);
    }

    @NetatKeyword(
            name = "bodyIsEmpty",
            description = "Kiểm tra body RỖNG",
            category = "API",
            subCategory = "Assertion - Body",
            parameters = {"response: ApiResponse"},
            example = "verify.bodyIsEmpty(response);",
            explainer = "Assert body is empty"
    )
    @Step("Assert body is empty")
    public void bodyIsEmpty(ApiResponse response) {
        execute(() -> {
            String body = response.getBody();
            Assert.assertTrue(body == null || body.trim().isEmpty(),
                    "Body should be empty");
            logger.info("Body is empty");
            return null;
        }, response);
    }

    @NetatKeyword(
            name = "bodyNotEmpty",
            description = "Kiểm tra body KHÔNG RỖNG",
            category = "API",
            subCategory = "Assertion - Body",
            parameters = {"response: ApiResponse"},
            example = "verify.bodyNotEmpty(response);",
            explainer = "Assert body is not empty"
    )
    @Step("Assert body is not empty")
    public void bodyNotEmpty(ApiResponse response) {
        execute(() -> {
            String body = response.getBody();
            Assert.assertTrue(body != null && !body.trim().isEmpty(),
                    "Body should NOT be empty");
            logger.info("Body is not empty ({} chars)", body.length());
            return null;
        }, response);
    }

    // ========================================================================
    //  SECTION 5: PERFORMANCE ASSERTIONS (2 methods)
    // ========================================================================

    @NetatKeyword(
            name = "responseTimeLessThan",
            description = "Kiểm tra response time nhỏ hơn ngưỡng (ms)",
            category = "API",
            subCategory = "Assertion - Performance",
            parameters = {
                    "response: ApiResponse",
                    "maxTimeMs: long - Thời gian tối đa cho phép (milliseconds)"
            },
            example = "verify.responseTimeLessThan(response, 2000);",
            explainer = "Assert response time < {1}ms"
    )
    @Step("Assert response time < {1}ms")
    public void responseTimeLessThan(ApiResponse response, long maxTimeMs) {
        execute(() -> {
            long actual = response.getResponseTime();
            Assert.assertTrue(actual < maxTimeMs,
                    String.format("Expected response time < %dms but got %dms", maxTimeMs, actual));
            logger.info("Response time {}ms < {}ms verified", actual, maxTimeMs);
            return null;
        }, response, maxTimeMs);
    }

    // ========================================================================
    //  SECTION 6: HEADER ASSERTIONS (3 methods)
    // ========================================================================

    @NetatKeyword(
            name = "headerEquals",
            description = "Kiểm tra giá trị header",
            category = "API",
            subCategory = "Assertion - Headers",
            parameters = {
                    "response: ApiResponse",
                    "headerName: String",
                    "expectedValue: String"
            },
            example = "verify.headerEquals(response, \"Content-Type\", \"application/json\");",
            explainer = "Assert header {1} = {2}"
    )
    @Step("Assert header {1} = {2}")
    public void headerEquals(ApiResponse response, String headerName, String expectedValue) {
        execute(() -> {
            String actual = response.getHeader(headerName);
            Assert.assertEquals(actual, expectedValue,
                    String.format("Header '%s': expected '%s' but got '%s'", headerName, expectedValue, actual));
            logger.info("Header '{}' = '{}' verified", headerName, expectedValue);
            return null;
        }, response, headerName, expectedValue);
    }

    @NetatKeyword(
            name = "headerExists",
            description = "Kiểm tra header TỒN TẠI",
            category = "API",
            subCategory = "Assertion - Headers",
            parameters = {
                    "response: ApiResponse",
                    "headerName: String"
            },
            example = "verify.headerExists(response, \"Authorization\");",
            explainer = "Assert header {1} exists"
    )
    @Step("Assert header {1} exists")
    public void headerExists(ApiResponse response, String headerName) {
        execute(() -> {
            boolean exists = response.hasHeader(headerName);
            Assert.assertTrue(exists,
                    String.format("Header '%s' should exist", headerName));
            logger.info("Header '{}' exists", headerName);
            return null;
        }, response, headerName);
    }

    @NetatKeyword(
            name = "headerContains",
            description = "Kiểm tra giá trị header CHỨA substring",
            category = "API",
            subCategory = "Assertion - Headers",
            parameters = {
                    "response: ApiResponse",
                    "headerName: String",
                    "substring: String"
            },
            example = "verify.headerContains(response, \"Content-Type\", \"json\");",
            explainer = "Assert header {1} contains '{2}'"
    )
    @Step("Assert header {1} contains '{2}'")
    public void headerContains(ApiResponse response, String headerName, String substring) {
        execute(() -> {
            String headerValue = response.getHeader(headerName);
            boolean contains = headerValue != null && headerValue.contains(substring);
            Assert.assertTrue(contains,
                    String.format("Header '%s' should contain '%s' but got '%s'", headerName, substring, headerValue));
            logger.info("Header '{}' contains '{}'", headerName, substring);
            return null;
        }, response, headerName, substring);
    }

// ========================================================================
//  SECTION 7: GENERAL ASSERTIONS (5 methods)
// ========================================================================

    @NetatKeyword(
            name = "assertTrue",
            description = "Kiểm tra điều kiện là TRUE",
            category = "API",
            subCategory = "Assertion - General",
            parameters = {
                    "condition: boolean - Điều kiện cần kiểm tra",
                    "message: String (optional) - Thông báo khi fail"
            },
            example = "verify.assertTrue(isValid, \"Should be valid\");",
            explainer = "Assert true"
    )
    @Step("Assert true: {1}")
    public void assertTrue(boolean condition, String... message) {
        execute(() -> {
            String msg = message.length > 0 ? message[0] : "Expected condition to be true but was false";
            Assert.assertTrue(condition, msg);
            logger.info("assertTrue passed");
            return null;
        }, condition, message);
    }

    @NetatKeyword(
            name = "assertFalse",
            description = "Kiểm tra điều kiện là FALSE",
            category = "API",
            subCategory = "Assertion - General",
            parameters = {
                    "condition: boolean",
                    "message: String (optional)"
            },
            example = "verify.assertFalse(hasError, \"Should not have error\");",
            explainer = "Assert false"
    )
    @Step("Assert false: {1}")
    public void assertFalse(boolean condition, String... message) {
        execute(() -> {
            String msg = message.length > 0 ? message[0] : "Expected condition to be false but was true";
            Assert.assertFalse(condition, msg);
            logger.info("assertFalse passed");
            return null;
        }, condition, message);
    }

    @NetatKeyword(
            name = "assertEquals",
            description = "Kiểm tra 2 giá trị BẰNG NHAU",
            category = "API",
            subCategory = "Assertion - General",
            parameters = {
                    "actual: Object - Giá trị thực tế",
                    "expected: Object - Giá trị mong đợi"
            },
            example = "verify.assertEquals(actualValue, expectedValue);",
            explainer = "Assert {0} = {1}"
    )
    @Step("Assert {0} = {1}")
    public void assertEquals(Object actual, Object expected) {
        execute(() -> {
            Assert.assertEquals(actual, expected,
                    String.format("Expected '%s' but got '%s'", expected, actual));
            logger.info("assertEquals passed: {} = {}", actual, expected);
            return null;
        }, actual, expected);
    }

    @NetatKeyword(
            name = "assertNotEquals",
            description = "Kiểm tra 2 giá trị KHÁC NHAU",
            category = "API",
            subCategory = "Assertion - General",
            parameters = {
                    "actual: Object",
                    "unexpected: Object"
            },
            example = "verify.assertNotEquals(actualValue, forbiddenValue);",
            explainer = "Assert {0} ≠ {1}"
    )
    @Step("Assert {0} ≠ {1}")
    public void assertNotEquals(Object actual, Object unexpected) {
        execute(() -> {
            Assert.assertNotEquals(actual, unexpected,
                    String.format("Value should NOT be '%s'", unexpected));
            logger.info("assertNotEquals passed: {} ≠ {}", actual, unexpected);
            return null;
        }, actual, unexpected);
    }

    @NetatKeyword(
            name = "assertNull",
            description = "Kiểm tra giá trị là NULL",
            category = "API",
            subCategory = "Assertion - General",
            parameters = {"object: Object"},
            example = "verify.assertNull(deletedValue);",
            explainer = "Assert null"
    )
    @Step("Assert null")
    public void assertNull(Object object) {
        execute(() -> {
            Assert.assertNull(object, "Expected null but got: " + object);
            logger.info("assertNull passed");
            return null;
        }, object);
    }

    @NetatKeyword(
            name = "assertNotNull",
            description = "Kiểm tra giá trị KHÔNG NULL",
            category = "API",
            subCategory = "Assertion - General",
            parameters = {"object: Object"},
            example = "verify.assertNotNull(userId);",
            explainer = "Assert not null"
    )
    @Step("Assert not null")
    public void assertNotNull(Object object) {
        execute(() -> {
            Assert.assertNotNull(object, "Expected value should NOT be null");
            logger.info("assertNotNull passed");
            return null;
        }, object);
    }

    @NetatKeyword(
            name = "fail",
            description = "Force fail test với message (dùng cho conditional logic)",
            category = "API",
            subCategory = "Assertion - General",
            parameters = {"message: String - Thông báo lỗi"},
            example = "if (someCondition) { verify.fail(\"This should not happen\"); }",
            explainer = "Force fail: {0}"
    )
    @Step("Force fail: {0}")
    public void fail(String message) {
        execute(() -> {
            Assert.fail(message);
            return null;
        }, message);
    }
}