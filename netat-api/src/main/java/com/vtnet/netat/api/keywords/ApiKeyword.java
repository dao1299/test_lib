package com.vtnet.netat.api.keywords;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vtnet.netat.api.core.ApiContext;
import com.vtnet.netat.api.core.ApiResponse;
import com.vtnet.netat.api.core.BaseApiKeyword;
import com.vtnet.netat.api.curl.CurlExecutor;
import com.vtnet.netat.api.curl.CurlParser;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.secret.SecretDecryptor;
import com.vtnet.netat.core.secret.SensitiveDataProtection;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;


public class ApiKeyword extends BaseApiKeyword {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @NetatKeyword(
            name = "setApiBaseUrl",
            description = "Thiết lập URL gốc cho API",
            category = "API",
            subCategory = "Configuration",
            parameters = {"baseUrl: String - URL gốc (VD: https://api.example.com)"},
            example = "api.setApiBaseUrl(\"https://api.example.com\");",
            explainer = "Set API Base URL: {baseUrl}"
    )
    public void setApiBaseUrl(String baseUrl) {
        execute(() -> {
            getContext().setBaseUri(baseUrl);  // : context → getContext()
            logger.info("API Base URL set to: {}", baseUrl);
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
            explainer = "Set timeout: {timeoutSeconds}s"
    )
    public void setRequestTimeout(int timeoutSeconds) {
        execute(() -> {
            getContext().setTimeout(timeoutSeconds);  // 
            logger.info("Timeout set to: {}s", timeoutSeconds);
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
            explainer = "Enable logging: {enabled}"
    )
    public void enableRequestLogging(boolean enabled) {
        execute(() -> {
            getContext().setLogRequests(enabled);  // 
            logger.info("Request logging: {}", enabled ? "enabled" : "disabled");
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
    public void clearAllRequestSettings() {
        execute(() -> {
            getContext().clearAllRequestSettings();  // 
            logger.info("All request settings cleared");
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
    public void resetApiContext() {
        execute(() -> {
            getContext().reset();  // 
            logger.info("API context reset");
            return null;
        });
    }

    @NetatKeyword(
            name = "disableSslVerification",
            description = "Tắt SSL certificate verification. CHỈ DÙNG CHO MÔI TRƯỜNG TEST!",
            category = "API",
            subCategory = "Configuration",
            example = "api.disableSslVerification();",
            explainer = "Disable SSL verification (TEST ONLY)",
            note = "WARNING: Không sử dụng trong production!"
    )
    public void disableSslVerification() {
        execute(() -> {
            getContext().setSslVerificationEnabled(false);  // 
            logger.warn("SSL verification DISABLED - Only use in test environment!");
            return null;
        });
    }

    @NetatKeyword(
            name = "enableSslVerification",
            description = "Bật lại SSL certificate verification (mặc định đã bật)",
            category = "API",
            subCategory = "Configuration",
            example = "api.enableSslVerification();",
            explainer = "Enable SSL verification"
    )
    public void enableSslVerification() {
        execute(() -> {
            getContext().setSslVerificationEnabled(true);  // 
            logger.info("✓ SSL verification enabled");
            return null;
        });
    }

    @NetatKeyword(
            name = "executeCurl",
            description = "Thực thi một cURL command trực tiếp. Hỗ trợ các options: " +
                    "-X (method), -H (header), -d/--data (body), -u (basic auth), " +
                    "-k/--insecure (skip SSL), -b/--cookie (cookies), -F/--form (form data).",
            category = "API",
            subCategory = "cURL",
            parameters = {"curlCommand: String - cURL command đầy đủ"},
            returnValue = "ApiResponse - Response object",
            example = "String curl = \"curl -X POST 'https://api.example.com/users' \" +\n" +
                    "              \"-H 'Content-Type: application/json' \" +\n" +
                    "              \"-H 'Authorization: Bearer token123' \" +\n" +
                    "              \"-d '{\\\"name\\\": \\\"John\\\"}' \";\n" +
                    "ApiResponse response = api.executeCurl(curl);",
            explainer = "Execute cURL command",
            note = "Copy cURL từ browser DevTools hoặc Postman và paste trực tiếp. " +
                    "Hỗ trợ multiline với backslash (\\)."
    )
    public ApiResponse executeCurl(String curlCommand) {
        return execute(() -> {
            logger.info("Parsing and executing cURL command...");
            CurlParser.ParsedCurl parsed = CurlParser.parse(curlCommand);
            logger.info("Parsed: {} {}", parsed.getMethod(), parsed.getUrl());
            ApiResponse response = CurlExecutor.execute(parsed, getContext());  // 
            logger.info("cURL executed - Status: {}", response.getStatusCode());
            return response;
        }, truncateForLog(curlCommand));
    }

    @NetatKeyword(
            name = "executeCurlFromFile",
            description = "Thực thi cURL command từ file. Hữu ích khi cURL command quá dài hoặc cần reuse.",
            category = "API",
            subCategory = "cURL",
            parameters = {"filePath: String - Đường dẫn đến file chứa cURL command"},
            returnValue = "ApiResponse - Response object",
            example = "// File: src/test/resources/curl/create_user.sh\n" +
                    "// curl -X POST 'https://api.example.com/users' -H 'Content-Type: application/json' -d '{...}'\n\n" +
                    "ApiResponse response = api.executeCurlFromFile(\"src/test/resources/curl/create_user.sh\");",
            explainer = "Execute cURL from file: {filePath}"
    )
    public ApiResponse executeCurlFromFile(String filePath) {
        return execute(() -> {
            logger.info("Reading cURL command from file: {}", filePath);
            String curlCommand = new String(Files.readAllBytes(Paths.get(filePath)));
            CurlParser.ParsedCurl parsed = CurlParser.parse(curlCommand);
            logger.info("Parsed from file: {} {}", parsed.getMethod(), parsed.getUrl());
            ApiResponse response = CurlExecutor.execute(parsed, getContext());  // 
            logger.info("cURL from file executed - Status: {}", response.getStatusCode());
            return response;
        }, filePath);
    }

    @NetatKeyword(
            name = "parseCurl",
            description = "Parse cURL command để xem các thành phần (debug/verify). " +
                    "Không thực thi request, chỉ trả về thông tin đã parse.",
            category = "API",
            subCategory = "cURL",
            parameters = {"curlCommand: String - cURL command cần parse"},
            returnValue = "String - Thông tin các thành phần đã parse (method, url, headers, body...)",
            example = "String info = api.parseCurl(curlCommand);\n" +
                    "System.out.println(info);",
            explainer = "Parse cURL command (debug)"
    )
    public String parseCurl(String curlCommand) {
        return execute(() -> {
            CurlParser.ParsedCurl parsed = CurlParser.parse(curlCommand);
            String info = parsed.toString();
            logger.info("Parsed cURL:\n{}", info);
            return info;
        }, truncateForLog(curlCommand));
    }

    @NetatKeyword(
            name = "setBearerToken",
            description = "Thiết lập Bearer token (JWT)",
            category = "API",
            subCategory = "Authentication",
            parameters = {"token: String - Access token"},
            example = "api.setBearerToken(\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\");",
            explainer = "Set Bearer Token"
    )
    public void setBearerToken(String token) {
        execute(() -> {
            getContext().setBearerToken(token);  // 
            logger.info("Bearer token set");
            return null;
        }, "***");
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
            explainer = "Set Basic Auth: {username}"
    )
    public void setBasicAuth(String username, String password) {
        execute(() -> {
            getContext().setBasicAuth(username, password);  // 
            logger.info("Basic auth set for user: {}", username);
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
            explainer = "Set API Key: {keyName} in {location}"
    )
    public void setApiKey(String keyName, String keyValue, String location) {
        execute(() -> {
            ApiContext.ApiKeyLocation loc = ApiContext.ApiKeyLocation.valueOf(location.toUpperCase());
            getContext().setApiKey(keyName, keyValue, loc);  // 
            logger.info("API Key '{}' set in {}", keyName, location);
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
    public void removeAuthentication() {
        execute(() -> {
            getContext().removeAuth();  // 
            logger.info("Authentication removed");
            return null;
        });
    }

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
            explainer = "Add header: {headerName} = {headerValue}"
    )
    public void addHeader(String headerName, String headerValue) {
        execute(() -> {
            getContext().addHeader(headerName, headerValue); 
            logger.info("Header added: {} = {}", headerName, headerValue);
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
            explainer = "Set Content-Type: {contentType}"
    )
    public void setContentType(String contentType) {
        execute(() -> {
            String mimeType = convertToMimeType(contentType);
            getContext().setContentType(mimeType);
            getContext().addHeader("Content-Type", mimeType);
            logger.info("Content-Type set to: {}", mimeType);
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
            explainer = "Set Accept: {acceptType}"
    )
    public void setAcceptHeader(String acceptType) {
        execute(() -> {
            String mimeType = convertToMimeType(acceptType);
            getContext().addHeader("Accept", mimeType);  // 
            logger.info("Accept header set to: {}", mimeType);
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
            explainer = "Remove header: {headerName}"
    )
    public void removeHeader(String headerName) {
        execute(() -> {
            getContext().removeHeader(headerName);  // 
            logger.info("Header removed: {}", headerName);
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
    public void clearAllHeaders() {
        execute(() -> {
            getContext().clearHeaders();  // 
            logger.info("All headers cleared");
            return null;
        });
    }

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
            explainer = "Add query param: {paramName} = {paramValue}"
    )
    public void addQueryParam(String paramName, Object paramValue) {
        execute(() -> {
            getContext().addQueryParam(paramName, paramValue);  // 
            logger.info("Query param added: {} = {}", paramName, paramValue);
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
            explainer = "Add path param: {paramName} = {paramValue}"
    )
    public void addPathParam(String paramName, Object paramValue) {
        execute(() -> {
            getContext().addPathParam(paramName, paramValue);  // 
            logger.info("Path param added: {} = {}", paramName, paramValue);
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
    public void clearQueryParams() {
        execute(() -> {
            getContext().clearQueryParams();  // 
            logger.info("All query params cleared");
            return null;
        });
    }


    @NetatKeyword(
            name = "setRequestBody",
            description = "Thiết lập request body từ string",
            category = "API",
            subCategory = "Body",
            parameters = {"bodyContent: String - Nội dung body (JSON, XML, text...)"},
            example = "api.setRequestBody(\"{\\\"name\\\":\\\"John\\\"}\");",
            explainer = "Set request body"
    )
    public void setRequestBody(String bodyContent) {
        execute(() -> {
            getContext().setRequestBody(bodyContent);  // 
            logger.info("Request body set ({} bytes)", bodyContent.length());
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
            explainer = "Set body from file: {filePath}"
    )
    public void setRequestBodyFromFile(String filePath) {
        execute(() -> {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            getContext().setRequestBody(content);  // 
            logger.info("Request body set from file: {}", filePath);
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
    public void clearRequestBody() {
        execute(() -> {
            getContext().clearRequestBody();  // 
            logger.info("Request body cleared");
            return null;
        });
    }

    @NetatKeyword(
            name = "sendGetRequest",
            description = "Gửi GET request",
            category = "API",
            subCategory = "Requests",
            parameters = {"endpoint: String - Endpoint URL"},
            returnValue = "ApiResponse - Response object",
            example = "ApiResponse response = api.sendGetRequest(\"/users/1\");",
            explainer = "GET {endpoint}"
    )
    public ApiResponse sendGetRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executeGet(endpoint);
            logger.info("GET {} - Status: {}", endpoint, response.getStatusCode());
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
            explainer = "POST {endpoint}"
    )
    public ApiResponse sendPostRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executePost(endpoint);
            logger.info("POST {} - Status: {}", endpoint, response.getStatusCode());
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
            explainer = "PUT {endpoint}"
    )
    public ApiResponse sendPutRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executePut(endpoint);
            logger.info("PUT {} - Status: {}", endpoint, response.getStatusCode());
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
            explainer = "DELETE {endpoint}"
    )
    public ApiResponse sendDeleteRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executeDelete(endpoint);
            logger.info("DELETE {} - Status: {}", endpoint, response.getStatusCode());
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
            explainer = "PATCH {endpoint}"
    )
    public ApiResponse sendPatchRequest(String endpoint) {
        return execute(() -> {
            ApiResponse response = executePatch(endpoint);
            logger.info("PATCH {} - Status: {}", endpoint, response.getStatusCode());
            return response;
        }, endpoint);
    }

    @NetatKeyword(
            name = "sendPostWithJson",
            description = "Gửi POST với JSON body (shortcut - 1 step)",
            category = "API",
            subCategory = "Requests",
            parameters = {
                    "endpoint: String - Endpoint URL",
                    "jsonBody: String - JSON string"
            },
            returnValue = "ApiResponse",
            example = "ApiResponse response = api.sendPostWithJson(\"/users\", \"{\\\"name\\\":\\\"John\\\"}\");",
            explainer = "POST {endpoint} with JSON"
    )
    public ApiResponse sendPostWithJson(String endpoint, String jsonBody) {
        return execute(() -> {
            ApiContext ctx = getContext();  // : Lấy context một lần, dùng local variable
            ctx.setRequestBody(jsonBody);
            ctx.setContentType("application/json");
            try {
                ApiResponse response = executePost(endpoint);
                logger.info("POST {} with JSON - Status: {}", endpoint, response.getStatusCode());
                return response;
            } finally {
                ctx.clearRequestBody();
            }
        }, endpoint, jsonBody);
    }

    @NetatKeyword(
            name = "sendPutWithJson",
            description = "Gửi PUT với JSON body (shortcut)",
            category = "API",
            subCategory = "Requests",
            parameters = {
                    "endpoint: String",
                    "jsonBody: String"
            },
            returnValue = "ApiResponse",
            example = "ApiResponse response = api.sendPutWithJson(\"/users/1\", \"{\\\"name\\\":\\\"Jane\\\"}\");",
            explainer = "PUT {endpoint} with JSON"
    )
    public ApiResponse sendPutWithJson(String endpoint, String jsonBody) {
        return execute(() -> {
            ApiContext ctx = getContext();  // 
            ctx.setRequestBody(jsonBody);
            ctx.setContentType("application/json");
            try {
                return executePut(endpoint);
            } finally {
                ctx.clearRequestBody();
            }
        }, endpoint, jsonBody);
    }

    @NetatKeyword(
            name = "sendGetWithParams",
            description = "Gửi GET với query params từ JSON string (shortcut)",
            category = "API",
            subCategory = "Requests",
            parameters = {
                    "endpoint: String",
                    "paramsJson: String - VD: {\"page\":1,\"limit\":10}"
            },
            returnValue = "ApiResponse",
            example = "ApiResponse response = api.sendGetWithParams(\"/users\", \"{\\\"page\\\":1,\\\"limit\\\":10}\");",
            explainer = "GET {endpoint} with params"
    )
    public ApiResponse sendGetWithParams(String endpoint, String paramsJson) {
        return execute(() -> {
            ApiContext ctx = getContext();  // 
            try {
                Map<String, Object> params = objectMapper.readValue(paramsJson, Map.class);
                ctx.addQueryParams(params);
                ApiResponse response = executeGet(endpoint);
                logger.info("GET {} with params - Status: {}", endpoint, response.getStatusCode());
                return response;
            } catch (Exception e) {
                throw new RuntimeException("Invalid JSON params: " + paramsJson, e);
            } finally {
                ctx.clearQueryParams();
            }
        }, endpoint, paramsJson);
    }

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
            subCategory = "Response",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String - JSON Path expression (VD: $.user.name)"
            },
            returnValue = "String - Extracted value",
            example = "String name = api.extractJsonValue(response, \"$.name\");",
            explainer = "Extract JSON: {jsonPath}"
    )
    public String extractJsonValue(ApiResponse response, String jsonPath) {
        return execute(() -> {
            String value = response.getJsonPath(jsonPath);
            logger.info("Extracted '{}': {}", jsonPath, value);
            return value;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "extractJsonInt",
            description = "Trích xuất số nguyên từ JSON",
            category = "API",
            subCategory = "Response",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            returnValue = "int",
            example = "int userId = api.extractJsonInt(response, \"$.id\");",
            explainer = "Extract int from {jsonPath}"
    )
    public int extractJsonInt(ApiResponse response, String jsonPath) {
        return execute(() -> {
            Integer value = response.getJsonPathAsInt(jsonPath);
            if (value == null) {
                throw new RuntimeException("Cannot extract int from path: " + jsonPath);
            }
            logger.info("Extracted int '{}': {}", jsonPath, value);
            return value;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "extractJsonDouble",
            description = "Trích xuất số thực từ JSON",
            category = "API",
            subCategory = "Response",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            returnValue = "double",
            example = "double price = api.extractJsonDouble(response, \"$.price\");",
            explainer = "Extract double from {jsonPath}"
    )
    public double extractJsonDouble(ApiResponse response, String jsonPath) {
        return execute(() -> {
            Double value = response.getJsonPathAsDouble(jsonPath);
            if (value == null) {
                throw new RuntimeException("Cannot extract double from path: " + jsonPath);
            }
            logger.info("Extracted double '{}': {}", jsonPath, value);
            return value;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "extractJsonBoolean",
            description = "Trích xuất boolean từ JSON",
            category = "API",
            subCategory = "Response",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String"
            },
            returnValue = "boolean",
            example = "boolean isActive = api.extractJsonBoolean(response, \"$.active\");",
            explainer = "Extract boolean from {jsonPath}"
    )
    public boolean extractJsonBoolean(ApiResponse response, String jsonPath) {
        return execute(() -> {
            Boolean value = response.getJsonPathAsBoolean(jsonPath);
            if (value == null) {
                throw new RuntimeException("Cannot extract boolean from path: " + jsonPath);
            }
            logger.info("Extracted boolean '{}': {}", jsonPath, value);
            return value;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "getArraySize",
            description = "Lấy số lượng phần tử trong JSON array",
            category = "API",
            subCategory = "Response",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String - Path đến array"
            },
            returnValue = "int - Array size",
            example = "int count = api.getArraySize(response, \"$.users\");",
            explainer = "Get array size: {jsonPath}"
    )
    public int getArraySize(ApiResponse response, String jsonPath) {
        return execute(() -> {
            List<Object> list = response.getJsonPathAsList(jsonPath);
            int size = list != null ? list.size() : 0;
            logger.info("Array '{}' size: {}", jsonPath, size);
            return size;
        }, response, jsonPath);
    }

    @NetatKeyword(
            name = "getHeader",
            description = "Lấy giá trị header từ response",
            category = "API",
            subCategory = "Response",
            parameters = {
                    "response: ApiResponse",
                    "headerName: String"
            },
            returnValue = "String - Header value",
            example = "String contentType = api.getHeader(response, \"Content-Type\");",
            explainer = "Get header: {headerName}"
    )
    public String getHeader(ApiResponse response, String headerName) {
        return execute(() -> {
            String value = response.getHeader(headerName);
            logger.info("Header '{}': {}", headerName, value);
            return value;
        }, response, headerName);
    }

    @NetatKeyword(
            name = "setHeaderSensitive",
            description = "Thiết lập HTTP header với giá trị đã mã hóa. Giá trị sẽ được giải mã và che dấu trong log/report.",
            category = "API",
            subCategory = "Security",
            parameters = {
                    "headerName: String - Tên header (VD: Authorization, X-API-Key)",
                    "encryptedValue: String - Giá trị đã mã hóa"
            },
            example = "api.setHeaderSensitive(\"Authorization\", encryptedToken);",
            explainer = "Set secure header: {headerName}"
    )
    public void setHeaderSensitive(String headerName, String encryptedValue) {
        execute(() -> {
            String plainValue = SecretDecryptor.decrypt(encryptedValue);
            protection.registerSensitiveValue(plainValue);
            getContext().addHeader(headerName, plainValue);
            logger.info("Secure header set: {} = *****", headerName);
            return null;
        }, headerName, "*****");
    }

    @NetatKeyword(
            name = "setBearerTokenSensitive",
            description = "Thiết lập Bearer token đã mã hóa. Token sẽ được giải mã và che dấu trong log.",
            category = "API",
            subCategory = "Security",
            parameters = {"encryptedToken: String - Bearer token đã mã hóa"},
            example = "api.setBearerTokenSensitive(encryptedToken);",
            explainer = "Set secure Bearer Token"
    )
    public void setBearerTokenSensitive(String encryptedToken) {
        execute(() -> {
            String plainToken = SecretDecryptor.decrypt(encryptedToken);
            protection.registerSensitiveValue(plainToken);
            getContext().setBearerToken(plainToken);
            logger.info("Secure Bearer token set: *****");
            return null;
        }, "*****");
    }

    @NetatKeyword(
            name = "setBasicAuthSensitive",
            description = "Thiết lập Basic Authentication với password đã mã hóa",
            category = "API",
            subCategory = "Security",
            parameters = {
                    "username: String - Username (plain text)",
                    "encryptedPassword: String - Password đã mã hóa"
            },
            example = "api.setBasicAuthSensitive(\"admin\", encryptedPassword);",
            explainer = "Set secure Basic Auth: {username}"
    )
    public void setBasicAuthSensitive(String username, String encryptedPassword) {
        execute(() -> {
            String plainPassword = SecretDecryptor.decrypt(encryptedPassword);
            protection.registerSensitiveValue(plainPassword);
            getContext().setBasicAuth(username, plainPassword);
            logger.info("Secure Basic auth set for user: {}", username);
            return null;
        }, username, "*****");
    }

    @NetatKeyword(
            name = "setApiKeySensitive",
            description = "Thiết lập API Key đã mã hóa (trong header hoặc query)",
            category = "API",
            subCategory = "Security",
            parameters = {
                    "keyName: String - Tên key (VD: X-API-Key, api_key)",
                    "encryptedKeyValue: String - Giá trị key đã mã hóa",
                    "location: String - HEADER hoặc QUERY"
            },
            example = "api.setApiKeySensitive(\"X-API-Key\", encryptedApiKey, \"HEADER\");",
            explainer = "Set secure API Key: {keyName} in {location}"
    )
    public void setApiKeySensitive(String keyName, String encryptedKeyValue, String location) {
        execute(() -> {
            String plainKeyValue = SecretDecryptor.decrypt(encryptedKeyValue);
            protection.registerSensitiveValue(plainKeyValue);
            ApiContext.ApiKeyLocation loc = ApiContext.ApiKeyLocation.valueOf(location.toUpperCase());
            getContext().setApiKey(keyName, plainKeyValue, loc);
            logger.info("Secure API Key '{}' set in {}", keyName, location);
            return null;
        }, keyName, "*****", location);
    }

    @NetatKeyword(
            name = "addQueryParamSensitive",
            description = "Thêm query parameter nhạy cảm đã mã hóa (VD: access_token, api_key trong URL)",
            category = "API",
            subCategory = "Security",
            parameters = {
                    "paramName: String - Tên parameter",
                    "encryptedValue: String - Giá trị đã mã hóa"
            },
            example = "api.addQueryParamSensitive(\"access_token\", encryptedToken);",
            explainer = "Add secure query param: {paramName}"
    )
    public void addQueryParamSensitive(String paramName, String encryptedValue) {
        execute(() -> {
            String plainValue = SecretDecryptor.decrypt(encryptedValue);
            protection.registerSensitiveValue(plainValue);
            getContext().addQueryParam(paramName, plainValue);
            logger.info("Secure query param added: {} = *****", paramName);
            return null;
        }, paramName, "*****");
    }

    @NetatKeyword(
            name = "setRequestBodySensitive",
            description = "Thiết lập request body có chứa dữ liệu nhạy cảm đã mã hóa. " +
                    "Body sẽ được giải mã và các giá trị nhạy cảm được che dấu trong log.",
            category = "API",
            subCategory = "Security",
            parameters = {"encryptedBody: String - Body đã mã hóa (JSON, XML, text...)"},
            example = "api.setRequestBodySensitive(encryptedJsonBody);",
            explainer = "Set secure request body"
    )
    public void setRequestBodySensitive(String encryptedBody) {
        execute(() -> {
            String plainBody = SecretDecryptor.decrypt(encryptedBody);
            protection.registerSensitiveValue(plainBody);
            getContext().setRequestBody(plainBody);
            logger.info("Secure request body set ({} bytes)", plainBody.length());
            return null;
        }, "*****");
    }

    @NetatKeyword(
            name = "sendPostWithJsonSensitive",
            description = "Gửi POST với JSON body nhạy cảm đã mã hóa. " +
                    "JSON sẽ được giải mã và các giá trị nhạy cảm được che dấu trong log.",
            category = "API",
            subCategory = "Security",
            parameters = {
                    "endpoint: String - Endpoint URL",
                    "encryptedJsonBody: String - JSON body đã mã hóa"
            },
            returnValue = "ApiResponse - Response object",
            example = "ApiResponse response = api.sendPostWithJsonSensitive(\"/auth/login\", encryptedLoginJson);",
            explainer = "POST {endpoint} with secure JSON"
    )
    public ApiResponse sendPostWithJsonSensitive(String endpoint, String encryptedJsonBody) {
        return execute(() -> {
            String plainJson = SecretDecryptor.decrypt(encryptedJsonBody);
            protection.registerSensitiveValue(plainJson);

            ApiContext ctx = getContext();
            ctx.setRequestBody(plainJson);
            ctx.setContentType("application/json");
            try {
                ApiResponse response = executePost(endpoint);
                logger.info("POST {} with secure JSON - Status: {}", endpoint, response.getStatusCode());
                return response;
            } finally {
                ctx.clearRequestBody();
            }
        }, endpoint, "*****");
    }

    @NetatKeyword(
            name = "sendPutWithJsonSensitive",
            description = "Gửi PUT với JSON body nhạy cảm đã mã hóa",
            category = "API",
            subCategory = "Security",
            parameters = {
                    "endpoint: String - Endpoint URL",
                    "encryptedJsonBody: String - JSON body đã mã hóa"
            },
            returnValue = "ApiResponse",
            example = "ApiResponse response = api.sendPutWithJsonSensitive(\"/users/1\", encryptedUserJson);",
            explainer = "PUT {endpoint} with secure JSON"
    )
    public ApiResponse sendPutWithJsonSensitive(String endpoint, String encryptedJsonBody) {
        return execute(() -> {
            String plainJson = SecretDecryptor.decrypt(encryptedJsonBody);
            protection.registerSensitiveValue(plainJson);

            ApiContext ctx = getContext();
            ctx.setRequestBody(plainJson);
            ctx.setContentType("application/json");
            try {
                ApiResponse response = executePut(endpoint);
                logger.info("PUT {} with secure JSON - Status: {}", endpoint, response.getStatusCode());
                return response;
            } finally {
                ctx.clearRequestBody();
            }
        }, endpoint, "*****");
    }

    @NetatKeyword(
            name = "extractJsonValueSensitive",
            description = "Trích xuất giá trị nhạy cảm từ JSON response và che dấu trong log. " +
                    "Giá trị thật vẫn được trả về cho test sử dụng.",
            category = "API",
            subCategory = "Security",
            parameters = {
                    "response: ApiResponse",
                    "jsonPath: String - JSON Path expression (VD: $.data.token)"
            },
            returnValue = "String - Giá trị thật (không mask)",
            example = "String token = api.extractJsonValueSensitive(response, \"$.access_token\");",
            explainer = "Extract secure value from: {jsonPath}"
    )
    public String extractJsonValueSensitive(ApiResponse response, String jsonPath) {
        return execute(() -> {
            String value = response.getJsonPath(jsonPath);
            protection.registerSensitiveValue(value);
            String masked = protection.mask(value);
            logger.info("Extracted secure value from '{}': {}", jsonPath, masked);
            return value;
        }, response, jsonPath);
    }

    @Override
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
                return shortForm;
        }
    }

    private String truncateForLog(String str) {
        if (str == null) return "null";
        if (str.length() <= 100) return str;
        return str.substring(0, 100) + "... (truncated)";
    }
}