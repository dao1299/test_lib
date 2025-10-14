package com.vtnet.netat.api.keywords;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vtnet.netat.api.core.ApiContext;
import com.vtnet.netat.api.core.ApiResponse;
import com.vtnet.netat.api.core.BaseApiKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import io.qameta.allure.Step;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * ⭐ Main API Keyword Class - Hybrid Approach
 *
 * Chứa tất cả operations: config, auth, headers, params, body, requests, response extraction
 *
 * @author NETAT Framework
 * @version 2.0
 *
 * Usage:
 * <pre>
 * ApiKeyword api = new ApiKeyword();
 * api.setApiBaseUrl("https://api.example.com");
 * api.setBearerToken("token123");
 * ApiResponse response = api.sendGetRequest("/users/1");
 * String name = api.extractJsonValue(response, "$.name");
 * </pre>
 */
public class ApiKeyword extends BaseApiKeyword {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ========================================================================
    //  SECTION 1: CONFIGURATION (5 methods)
    // Từ RestConfigKeyword.java
    // ========================================================================

    @NetatKeyword(
            name = "setApiBaseUrl",
            description = "Thiết lập URL gốc cho API",
            category = "API",
            subCategory = "Configuration",
            parameters = {"baseUrl: String - URL gốc (VD: https://api.example.com)"},
            example = "api.setApiBaseUrl(\"https://api.example.com\");",
            explainer = "Set API Base URL: {0}"
    )
    @Step("Set API Base URL: {0}")
    public void setApiBaseUrl(String baseUrl) {
        execute(() -> {
            context.setBaseUri(baseUrl);
            logger.info(" API Base URL set to: {}", baseUrl);
            return null;
        }, baseUrl);
    }

    @NetatKeyword(
            name = "setRequestTimeout",
            description = "Thiết lập thời gian chờ tối đa (seconds)",
            category = "API",
            subCategory = "Configuration",
            parameters = {"timeoutSeconds: int - Số giây chờ tối đa"},
            example = "api.setRequestTimeout(30);",
            explainer = "Set timeout: {0}s"
    )
    @Step("Set timeout: {0}s")
    public void setRequestTimeout(int timeoutSeconds) {
        execute(() -> {
            context.setTimeout(timeoutSeconds);
            logger.info(" Timeout set to: {}s", timeoutSeconds);
            return null;
        }, timeoutSeconds);
    }

    @NetatKeyword(
            name = "enableRequestLogging",
            description = "Bật/tắt log chi tiết cho requests",
            category = "API",
            subCategory = "Configuration",
            parameters = {"enabled: boolean - true = bật, false = tắt"},
            example = "api.enableRequestLogging(true);",
            explainer = "Enable logging: {0}"
    )
    @Step("Enable logging: {0}")
    public void enableRequestLogging(boolean enabled) {
        execute(() -> {
            context.setLogRequests(enabled);
            logger.info(" Request logging: {}", enabled ? "enabled" : "disabled");
            return null;
        }, enabled);
    }

    @NetatKeyword(
            name = "clearAllRequestSettings",
            description = "Xóa toàn bộ cấu hình request (headers, params, body...)",
            category = "API",
            subCategory = "Configuration",
            example = "api.clearAllRequestSettings();",
            explainer = "Clear all request settings"
    )
    @Step("Clear all request settings")
    public void clearAllRequestSettings() {
        execute(() -> {
            context.clearAllRequestSettings();
            logger.info(" All request settings cleared");
            return null;
        });
    }

    @NetatKeyword(
            name = "resetApiContext",
            description = "Reset toàn bộ API context về trạng thái ban đầu",
            category = "API",
            subCategory = "Configuration",
            example = "api.resetApiContext();",
            explainer = "Reset API context"
    )
    @Step("Reset API context")
    public void resetApiContext() {
        execute(() -> {
            context.reset();
            logger.info(" API context reset");
            return null;
        });
    }

    // ========================================================================
    //  SECTION 2: AUTHENTICATION (7 methods)
    // Từ RestAuthKeyword.java
    // ========================================================================

    @NetatKeyword(
            name = "setBearerToken",
            description = "Thiết lập Bearer token (JWT)",
            category = "API",
            subCategory = "Authentication",
            parameters = {"token: String - Access token"},
            example = "api.setBearerToken(\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\");",
            explainer = "Set Bearer Token"
    )
    @Step("Set Bearer Token")
    public void setBearerToken(String token) {
        execute(() -> {
            context.setBearerToken(token);
            logger.info(" Bearer token set");
            return null;
        }, "***"); // Hide token in logs
    }

    @NetatKeyword(
            name = "setBasicAuth",
            description = "Thiết lập Basic Authentication",
            category = "API",
            subCategory = "Authentication",
            parameters = {
                    "username: String - Username",
                    "password: String - Password"
            },
            example = "api.setBasicAuth(\"admin\", \"password123\");",
            explainer = "Set Basic Auth: {0}"
    )
    @Step("Set Basic Auth: {0}")
    public void setBasicAuth(String username, String password) {
        execute(() -> {
            context.setBasicAuth(username, password);
            logger.info(" Basic auth set for user: {}", username);
            return null;
        }, username, "***");
    }

    @NetatKeyword(
            name = "setApiKey",
            description = "Thiết lập API Key (trong header hoặc query)",
            category = "API",
            subCategory = "Authentication",
            parameters = {
                    "keyName: String - Tên key (VD: X-API-Key, api_key)",
                    "keyValue: String - Giá trị key",
                    "location: String - HEADER hoặc QUERY"
            },
            example = "api.setApiKey(\"X-API-Key\", \"abc123xyz\", \"HEADER\");",
            explainer = "Set API Key: {0} in {2}"
    )
    @Step("Set API Key: {0} in {2}")
    public void setApiKey(String keyName, String keyValue, String location) {
        execute(() -> {
            ApiContext.ApiKeyLocation loc = ApiContext.ApiKeyLocation.valueOf(location.toUpperCase());
            context.setApiKey(keyName, keyValue, loc);
            logger.info(" API Key '{}' set in {}", keyName, location);
            return null;
        }, keyName, "***", location);
    }

    @NetatKeyword(
            name = "removeAuthentication",
            description = "Xóa toàn bộ authentication đã setup",
            category = "API",
            subCategory = "Authentication",
            example = "api.removeAuthentication();",
            explainer = "Remove authentication"
    )
    @Step("Remove authentication")
    public void removeAuthentication() {
        execute(() -> {
            context.removeAuth();
            logger.info(" Authentication removed");
            return null;
        });
    }

    // TODO: Copy thêm obtainOAuth2TokenByPassword, obtainOAuth2TokenByClientCredentials từ RestAuthKeyword

    // ========================================================================
    //  SECTION 3: HEADERS (6 methods)
    // Từ RestHeaderKeyword.java
    // ========================================================================

    @NetatKeyword(
            name = "addHeader",
            description = "Thêm HTTP header vào request",
            category = "API",
            subCategory = "Headers",
            parameters = {
                    "headerName: String - Tên header",
                    "headerValue: String - Giá trị header"
            },
            example = "api.addHeader(\"X-Request-ID\", \"12345\");",
            explainer = "Add header: {0} = {1}"
    )
    @Step("Add header: {0} = {1}")
    public void addHeader(String headerName, String headerValue) {
        execute(() -> {
            context.addHeader(headerName, headerValue);
            logger.info(" Header added: {} = {}", headerName, headerValue);
            return null;
        }, headerName, headerValue);
    }

    @NetatKeyword(
            name = "setContentType",
            description = "Thiết lập Content-Type header (shortcut)",
            category = "API",
            subCategory = "Headers",
            parameters = {"contentType: String - JSON, XML, FORM, TEXT, hoặc full MIME type"},
            example = "api.setContentType(\"JSON\");",
            explainer = "Set Content-Type: {0}"
    )
    @Step("Set Content-Type: {0}")
    public void setContentType(String contentType) {
        execute(() -> {
            String mimeType = convertToMimeType(contentType);
            context.setContentType(mimeType);
            context.addHeader("Content-Type", mimeType);
            logger.info(" Content-Type set to: {}", mimeType);
            return null;
        }, contentType);
    }

    @NetatKeyword(
            name = "setAcceptHeader",
            description = "Thiết lập Accept header",
            category = "API",
            subCategory = "Headers",
            parameters = {"acceptType: String - JSON, XML, TEXT, hoặc full MIME type"},
            example = "api.setAcceptHeader(\"JSON\");",
            explainer = "Set Accept: {0}"
    )
    @Step("Set Accept: {0}")
    public void setAcceptHeader(String acceptType) {
        execute(() -> {
            String mimeType = convertToMimeType(acceptType);
            context.addHeader("Accept", mimeType);
            logger.info(" Accept header set to: {}", mimeType);
            return null;
        }, acceptType);
    }

    @NetatKeyword(
            name = "removeHeader",
            description = "Xóa một header",
            category = "API",
            subCategory = "Headers",
            parameters = {"headerName: String"},
            example = "api.removeHeader(\"X-Custom-Header\");",
            explainer = "Remove header: {0}"
    )
    @Step("Remove header: {0}")
    public void removeHeader(String headerName) {
        execute(() -> {
            context.removeHeader(headerName);
            logger.info(" Header removed: {}", headerName);
            return null;
        }, headerName);
    }

    @NetatKeyword(
            name = "clearAllHeaders",
            description = "Xóa tất cả headers",
            category = "API",
            subCategory = "Headers",
            example = "api.clearAllHeaders();",
            explainer = "Clear all headers"
    )
    @Step("Clear all headers")
    public void clearAllHeaders() {
        execute(() -> {
            context.clearHeaders();
            logger.info(" All headers cleared");
            return null;
        });
    }

    // ========================================================================
    //  SECTION 4: PARAMETERS (6 methods)
    // Từ RestParameterKeyword.java
    // ========================================================================

    @NetatKeyword(
            name = "addQueryParam",
            description = "Thêm query parameter",
            category = "API",
            subCategory = "Parameters",
            parameters = {
                    "paramName: String - Tên parameter",
                    "paramValue: Object - Giá trị parameter"
            },
            example = "api.addQueryParam(\"page\", 1);",
            explainer = "Add query param: {0} = {1}"
    )
    @Step("Add query param: {0} = {1}")
    public void addQueryParam(String paramName, Object paramValue) {
        execute(() -> {
            context.addQueryParam(paramName, paramValue);
            logger.info(" Query param added: {} = {}", paramName, paramValue);
            return null;
        }, paramName, paramValue);
    }

    @NetatKeyword(
            name = "addPathParam",
            description = "Thêm path parameter",
            category = "API",
            subCategory = "Parameters",
            parameters = {
                    "paramName: String - Tên parameter",
                    "paramValue: Object - Giá trị parameter"
            },
            example = "api.addPathParam(\"userId\", 123);",
            explainer = "Add path param: {0} = {1}"
    )
    @Step("Add path param: {0} = {1}")
    public void addPathParam(String paramName, Object paramValue) {
        execute(() -> {
            context.addPathParam(paramName, paramValue);
            logger.info(" Path param added: {} = {}", paramName, paramValue);
            return null;
        }, paramName, paramValue);
    }

    @NetatKeyword(
            name = "clearQueryParams",
            description = "Xóa tất cả query parameters",
            category = "API",
            subCategory = "Parameters",
            example = "api.clearQueryParams();",
            explainer = "Clear all query params"
    )
    @Step("Clear all query params")
    public void clearQueryParams() {
        execute(() -> {
            context.clearQueryParams();
            logger.info(" All query params cleared");
            return null;
        });
    }

    // TODO: Copy thêm addMultipleQueryParams, removeQueryParam, etc.

    // ========================================================================
    //  SECTION 5: REQUEST BODY (7 methods)
    // Từ RestBodyKeyword.java
    // ========================================================================

    @NetatKeyword(
            name = "setRequestBody",
            description = "Thiết lập request body từ string",
            category = "API",
            subCategory = "Body",
            parameters = {"bodyContent: String - Nội dung body (JSON, XML, text...)"},
            example = "api.setRequestBody(\"{\\\"name\\\":\\\"John\\\"}\");",
            explainer = "Set request body"
    )
    @Step("Set request body")
    public void setRequestBody(String bodyContent) {
        execute(() -> {
            context.setRequestBody(bodyContent);
            logger.info(" Request body set ({} bytes)", bodyContent.length());
            return null;
        }, bodyContent);
    }

    @NetatKeyword(
            name = "setRequestBodyFromFile",
            description = "Thiết lập body từ file",
            category = "API",
            subCategory = "Body",
            parameters = {"filePath: String - Đường dẫn file"},
            example = "api.setRequestBodyFromFile(\"data/user.json\");",
            explainer = "Set body from file: {0}"
    )
    @Step("Set body from file: {0}")
    public void setRequestBodyFromFile(String filePath) {
        execute(() -> {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            context.setRequestBody(content);
            logger.info(" Request body set from file: {}", filePath);
            return null;
        }, filePath);
    }

    @NetatKeyword(
            name = "clearRequestBody",
            description = "Xóa request body",
            category = "API",
            subCategory = "Body",
            example = "api.clearRequestBody();",
            explainer = "Clear request body"
    )
    @Step("Clear request body")
    public void clearRequestBody() {
        execute(() -> {
            context.clearRequestBody();
            logger.info(" Request body cleared");
            return null;
        });
    }

    // TODO: Copy thêm setRequestBodyFromMap, addFormParameter, etc.

    // ========================================================================
    //  SECTION 6: SEND REQUESTS (15 methods)
    // Từ RestRequestKeyword.java
    // ========================================================================

    @NetatKeyword(
            name = "sendGetRequest",
            description = "Gửi GET request",
            category = "API",
            subCategory = "Requests",
            parameters = {"endpoint: String - Endpoint URL"},
            returnValue = "ApiResponse - Response object",
            example = "ApiResponse response = api.sendGetRequest(\"/users/1\");",
            explainer = "GET {0}"
    )
    @Step("GET {0}")
    public ApiResponse sendGetRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executeGet(endpoint);
            logger.info(" GET {} - Status: {}", endpoint, response.getStatusCode());
            return response;
        }, endpoint);
    }

    @NetatKeyword(
            name = "sendPostRequest",
            description = "Gửi POST request",
            category = "API",
            subCategory = "Requests",
            parameters = {"endpoint: String - Endpoint URL"},
            returnValue = "ApiResponse - Response object",
            example = "ApiResponse response = api.sendPostRequest(\"/users\");",
            explainer = "POST {0}"
    )
    @Step("POST {0}")
    public ApiResponse sendPostRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executePost(endpoint);
            logger.info(" POST {} - Status: {}", endpoint, response.getStatusCode());
            return response;
        }, endpoint);
    }

    @NetatKeyword(
            name = "sendPutRequest",
            description = "Gửi PUT request",
            category = "API",
            subCategory = "Requests",
            parameters = {"endpoint: String - Endpoint URL"},
            returnValue = "ApiResponse - Response object",
            example = "ApiResponse response = api.sendPutRequest(\"/users/1\");",
            explainer = "PUT {0}"
    )
    @Step("PUT {0}")
    public ApiResponse sendPutRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executePut(endpoint);
            logger.info(" PUT {} - Status: {}", endpoint, response.getStatusCode());
            return response;
        }, endpoint);
    }

    @NetatKeyword(
            name = "sendDeleteRequest",
            description = "Gửi DELETE request",
            category = "API",
            subCategory = "Requests",
            parameters = {"endpoint: String - Endpoint URL"},
            returnValue = "ApiResponse - Response object",
            example = "ApiResponse response = api.sendDeleteRequest(\"/users/1\");",
            explainer = "DELETE {0}"
    )
    @Step("DELETE {0}")
    public ApiResponse sendDeleteRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executeDelete(endpoint);
            logger.info(" DELETE {} - Status: {}", endpoint, response.getStatusCode());
            return response;
        }, endpoint);
    }

    @NetatKeyword(
            name = "sendPatchRequest",
            description = "Gửi PATCH request",
            category = "API",
            subCategory = "Requests",
            parameters = {"endpoint: String - Endpoint URL"},
            returnValue = "ApiResponse - Response object",
            example = "ApiResponse response = api.sendPatchRequest(\"/users/1\");",
            explainer = "PATCH {0}"
    )
    @Step("PATCH {0}")
    public ApiResponse sendPatchRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executePatch(endpoint);
            logger.info(" PATCH {} - Status: {}", endpoint, response.getStatusCode());
            return response;
        }, endpoint);
    }

    // ========================================================================
    //  SECTION 6B: SHORTCUT REQUESTS (5 methods) - Quan trọng cho Low-Code!
    // ========================================================================

    @NetatKeyword(
            name = "sendPostWithJson",
            description = "Gửi POST với JSON body (shortcut - 1 step)",
            category = "API",
            subCategory = "Requests - Shortcuts",
            parameters = {
                    "endpoint: String - Endpoint URL",
                    "jsonBody: String - JSON string"
            },
            returnValue = "ApiResponse",
            example = "ApiResponse response = api.sendPostWithJson(\"/users\", \"{\\\"name\\\":\\\"John\\\"}\");",
            explainer = "POST {0} with JSON"
    )
    @Step("POST {0} with JSON")
    public ApiResponse sendPostWithJson(String endpoint, String jsonBody) {
        return execute(() -> {
            context.setRequestBody(jsonBody);
            context.setContentType("application/json");
            try {
                ApiResponse response = executePost(endpoint);
                logger.info(" POST {} with JSON - Status: {}", endpoint, response.getStatusCode());
                return response;
            } finally {
                context.clearRequestBody();
            }
        }, endpoint, jsonBody);
    }

    @NetatKeyword(
            name = "sendPutWithJson",
            description = "Gửi PUT với JSON body (shortcut)",
            category = "API",
            subCategory = "Requests - Shortcuts",
            parameters = {
                    "endpoint: String",
                    "jsonBody: String"
            },
            returnValue = "ApiResponse",
            example = "ApiResponse response = api.sendPutWithJson(\"/users/1\", \"{\\\"name\\\":\\\"Jane\\\"}\");",
            explainer = "PUT {0} with JSON"
    )
    @Step("PUT {0} with JSON")
    public ApiResponse sendPutWithJson(String endpoint, String jsonBody) {
        return execute(() -> {
            context.setRequestBody(jsonBody);
            context.setContentType("application/json");
            try {
                return executePut(endpoint);
            } finally {
                context.clearRequestBody();
            }
        }, endpoint, jsonBody);
    }

    @NetatKeyword(
            name = "sendGetWithParams",
            description = "Gửi GET với query params từ JSON string (shortcut)",
            category = "API",
            subCategory = "Requests - Shortcuts",
            parameters = {
                    "endpoint: String",
                    "paramsJson: String - VD: {\"page\":1,\"limit\":10}"
            },
            returnValue = "ApiResponse",
            example = "ApiResponse response = api.sendGetWithParams(\"/users\", \"{\\\"page\\\":1,\\\"limit\\\":10}\");",
            explainer = "GET {0} with params"
    )
    @Step("GET {0} with params")
    public ApiResponse sendGetWithParams(String endpoint, String paramsJson) {
        return execute(() -> {
            try {
                Map<String, Object> params = objectMapper.readValue(paramsJson, Map.class);
                context.addQueryParams(params);
                ApiResponse response = executeGet(endpoint);
                logger.info(" GET {} with params - Status: {}", endpoint, response.getStatusCode());
                return response;
            } catch (Exception e) {
                throw new RuntimeException("Invalid JSON params: " + paramsJson, e);
            } finally {
                context.clearQueryParams();
            }
        }, endpoint, paramsJson);
    }


    // ========================================================================
    //  SECTION 7: RESPONSE EXTRACTION (20 methods)
    // Từ RestResponseKeyword.java
    // ========================================================================

    @NetatKeyword(
            name = "getStatusCode",
            description = "Lấy HTTP status code từ response",
            category = "API",
            subCategory = "Response",
            parameters = {"response: ApiResponse"},
            returnValue = "int - Status code",
            example = "int code = api.getStatusCode(response);",
            explainer = "Get status code"
    )
    @Step("Get status code")
    public int getStatusCode(ApiResponse response) {
        return execute(() -> {
            int code = response.getStatusCode();
            logger.info("Status code: {}", code);
            return code;
        }, response);
    }

    @NetatKeyword(
            name = "getResponseTime",
            description = "Lấy response time (milliseconds)",
            category = "API",
            subCategory = "Response",
            parameters = {"response: ApiResponse"},
            returnValue = "long - Response time in ms",
            example = "long time = api.getResponseTime(response);",
            explainer = "Get response time"
    )
    @Step("Get response time")
    public long getResponseTime(ApiResponse response) {
        return execute(() -> {
            long time = response.getResponseTime();
            logger.info("Response time: {}ms", time);
            return time;
        }, response);
    }

    @NetatKeyword(
            name = "getBody",
            description = "Lấy response body as string",
            category = "API",
            subCategory = "Response",
            parameters = {"response: ApiResponse"},
            returnValue = "String - Response body",
            example = "String body = api.getBody(response);",
            explainer = "Get response body"
    )
    @Step("Get response body")
    public String getBody(ApiResponse response) {
        return execute(() -> {
            String body = response.getBody();
            logger.info("Response body retrieved ({} chars)", body.length());
            return body;
        }, response);
    }

    @NetatKeyword(
            name = "extractJsonValue",
            description = "Trích xuất giá trị từ JSON response (JSON Path)",
            category = "API",
            subCategory = "Response - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String - JSON Path expression (VD: $.user.name)"
            },
            returnValue = "String - Extracted value",
            example = "String name = api.extractJsonValue(response, \"$.name\");",
            explainer = "Extract JSON: {1}"
    )
    @Step("Extract JSON: {1}")
    public String extractJsonValue(ApiResponse response, String jsonPath) {
        return execute(() -> {
            String value = response.getJsonPath(jsonPath);
            logger.info(" Extracted '{}': {}", jsonPath, value);
            return value;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "extractJsonInt",
            description = "Trích xuất số nguyên từ JSON",
            category = "API",
            subCategory = "Response - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            returnValue = "int",
            example = "int userId = api.extractJsonInt(response, \"$.id\");",
            explainer = "Extract int from {1}"
    )
    @Step("Extract int from {1}")
    public int extractJsonInt(ApiResponse response, String jsonPath) {
        return execute(() -> {
            Integer value = response.getJsonPathAsInt(jsonPath);
            if (value == null) {
                throw new RuntimeException("Cannot extract int from path: " + jsonPath);
            }
            logger.info(" Extracted int '{}': {}", jsonPath, value);
            return value;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "extractJsonDouble",
            description = "Trích xuất số thực từ JSON",
            category = "API",
            subCategory = "Response - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            returnValue = "double",
            example = "double price = api.extractJsonDouble(response, \"$.price\");",
            explainer = "Extract double from {1}"
    )
    @Step("Extract double from {1}")
    public double extractJsonDouble(ApiResponse response, String jsonPath) {
        return execute(() -> {
            Double value = response.getJsonPathAsDouble(jsonPath);
            if (value == null) {
                throw new RuntimeException("Cannot extract double from path: " + jsonPath);
            }
            logger.info(" Extracted double '{}': {}", jsonPath, value);
            return value;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "extractJsonBoolean",
            description = "Trích xuất boolean từ JSON",
            category = "API",
            subCategory = "Response - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            returnValue = "boolean",
            example = "boolean isActive = api.extractJsonBoolean(response, \"$.active\");",
            explainer = "Extract boolean from {1}"
    )
    @Step("Extract boolean from {1}")
    public boolean extractJsonBoolean(ApiResponse response, String jsonPath) {
        return execute(() -> {
            Boolean value = response.getJsonPathAsBoolean(jsonPath);
            if (value == null) {
                throw new RuntimeException("Cannot extract boolean from path: " + jsonPath);
            }
            logger.info(" Extracted boolean '{}': {}", jsonPath, value);
            return value;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "getArraySize",
            description = "Lấy số lượng phần tử trong JSON array",
            category = "API",
            subCategory = "Response - JSON",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String - Path đến array"
            },
            returnValue = "int - Array size",
            example = "int count = api.getArraySize(response, \"$.users\");",
            explainer = "Get array size: {1}"
    )
    @Step("Get array size: {1}")
    public int getArraySize(ApiResponse response, String jsonPath) {
        return execute(() -> {
            List<Object> list = response.getJsonPathAsList(jsonPath);
            int size = list != null ? list.size() : 0;
            logger.info(" Array '{}' size: {}", jsonPath, size);
            return size;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "getHeader",
            description = "Lấy giá trị header từ response",
            category = "API",
            subCategory = "Response - Headers",
            parameters = {
                    "response: ApiResponse",
                    "headerName: String"
            },
            returnValue = "String - Header value",
            example = "String contentType = api.getHeader(response, \"Content-Type\");",
            explainer = "Get header: {1}"
    )
    @Step("Get header: {1}")
    public String getHeader(ApiResponse response, String headerName) {
        return execute(() -> {
            String value = response.getHeader(headerName);
            logger.info(" Header '{}': {}", headerName, value);
            return value;
        }, response, headerName);
    }

    // TODO: Copy thêm extractJsonArray, checkJsonPathExists, etc.

    // ========================================================================
    //  UTILITY METHODS (Private helpers)
    // ========================================================================
    
    protected String convertToMimeType(String shortForm) {
        switch (shortForm.toUpperCase()) {
            case "JSON":
                return "application/json";
            case "XML":
                return "application/xml";
            case "FORM":
                return "application/x-www-form-urlencoded";
            case "TEXT":
                return "text/plain";
            case "HTML":
                return "text/html";
            case "MULTIPART":
                return "multipart/form-data";
            default:
                return shortForm; // Already a MIME type
        }
    }
}
