package com.vtnet.netat.api.core;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.logging.NetatLogger;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Base API Keyword - Class cha cho tất cả API keywords
 * Cung cấp các helper methods để execute HTTP requests
 *
 * @author NETAT Framework
 * @version 2.0
 */
public abstract class BaseApiKeyword extends BaseKeyword {

    protected static final NetatLogger logger = NetatLogger.getInstance(BaseApiKeyword.class);

    // ========================================================================
    // CONTEXT (Thread-safe)
    // ========================================================================

    /**
     * Thread-local context cho mỗi test thread
     * Đảm bảo thread-safe khi chạy parallel tests
     */
    protected static ThreadLocal<ApiContext> contextThreadLocal = ThreadLocal.withInitial(ApiContext::new);

    /**
     * Get current thread's API context
     */
    protected ApiContext getContext() {
        return contextThreadLocal.get();
    }

    /**
     * Shorthand property để access context trong subclasses
     */
    protected ApiContext context = getContext();

    /**
     * Reset context cho thread hiện tại
     */
    protected void resetContext() {
        contextThreadLocal.get().reset();
    }

    /**
     * Remove context của thread hiện tại (cleanup)
     */
    protected void removeContext() {
        contextThreadLocal.remove();
    }

    // ========================================================================
    // CORE HTTP REQUEST METHODS
    // ========================================================================

    /**
     * Execute GET request
     *
     * @param endpoint Endpoint URL (relative hoặc absolute)
     * @return ApiResponse object
     */
    protected ApiResponse executeGet(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("GET", endpoint);

        Response response = spec.when().get(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

    /**
     * Execute POST request
     *
     * @param endpoint Endpoint URL
     * @return ApiResponse object
     */
    protected ApiResponse executePost(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("POST", endpoint);

        Response response = spec.when().post(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

    /**
     * Execute PUT request
     *
     * @param endpoint Endpoint URL
     * @return ApiResponse object
     */
    protected ApiResponse executePut(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("PUT", endpoint);

        Response response = spec.when().put(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

    /**
     * Execute PATCH request
     *
     * @param endpoint Endpoint URL
     * @return ApiResponse object
     */
    protected ApiResponse executePatch(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("PATCH", endpoint);

        Response response = spec.when().patch(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

    /**
     * Execute DELETE request
     *
     * @param endpoint Endpoint URL
     * @return ApiResponse object
     */
    protected ApiResponse executeDelete(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("DELETE", endpoint);

        Response response = spec.when().delete(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

    /**
     * Execute HEAD request
     *
     * @param endpoint Endpoint URL
     * @return ApiResponse object
     */
    protected ApiResponse executeHead(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("HEAD", endpoint);

        Response response = spec.when().head(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

    /**
     * Execute OPTIONS request
     *
     * @param endpoint Endpoint URL
     * @return ApiResponse object
     */
    protected ApiResponse executeOptions(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("OPTIONS", endpoint);

        Response response = spec.when().options(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

    // ========================================================================
    // MULTIPART/FILE UPLOAD METHODS
    // ========================================================================

    /**
     * Execute multipart request (file upload)
     *
     * @param endpoint Endpoint URL
     * @param filePath Path to file
     * @param fileFieldName Field name trong form (thường là "file")
     * @return ApiResponse object
     */
    protected ApiResponse executeMultipartUpload(String endpoint, String filePath, String fileFieldName) {
        RequestSpecification spec = buildRequestSpec();

        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        spec.multiPart(fileFieldName, file);

        logRequest("POST (Multipart)", endpoint);
        logger.info("Uploading file: {} ({})", file.getName(), file.length() + " bytes");

        Response response = spec.when().post(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

    /**
     * Execute multipart request với metadata
     *
     * @param endpoint Endpoint URL
     * @param filePath Path to file
     * @param fileFieldName Field name cho file
     * @param metadata Additional form fields (key-value pairs)
     * @return ApiResponse object
     */
    protected ApiResponse executeMultipartUploadWithMetadata(String endpoint, String filePath,
                                                             String fileFieldName, Map<String, String> metadata) {
        RequestSpecification spec = buildRequestSpec();

        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        // Add file
        spec.multiPart(fileFieldName, file);

        // Add metadata fields
        if (metadata != null && !metadata.isEmpty()) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                spec.multiPart(entry.getKey(), entry.getValue());
            }
        }

        logRequest("POST (Multipart with metadata)", endpoint);
        logger.info("Uploading file: {} with {} metadata fields", file.getName(),
                metadata != null ? metadata.size() : 0);

        Response response = spec.when().post(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Build RequestSpecification từ context hiện tại
     */
    private RequestSpecification buildRequestSpec() {
        RequestSpecification spec = RestAssured.given();

        // Apply context settings
        ApiContext ctx = getContext();
        ctx.applyToRequestSpec(spec);

        return spec;
    }

    /**
     * Log request information (nếu logging enabled)
     */
    private void logRequest(String method, String endpoint) {
        ApiContext ctx = getContext();

        if (ctx.isLogRequests()) {
            logger.info("═══════════════════════════════════════════════════");
            logger.info("{} {}", method, endpoint);

            if (ctx.getBaseUri() != null) {
                logger.info("Base URI: {}", ctx.getBaseUri());
            }

            if (ctx.getHeaders() != null && !ctx.getHeaders().isEmpty()) {
                logger.info("Headers: {}", ctx.getHeaders());
            }

            if (ctx.getQueryParams() != null && !ctx.getQueryParams().isEmpty()) {
                logger.info("Query Params: {}", ctx.getQueryParams());
            }

            if (ctx.getPathParams() != null && !ctx.getPathParams().isEmpty()) {
                logger.info("Path Params: {}", ctx.getPathParams());
            }

            if (ctx.hasRequestBody()) {
                logger.info("Body: {}", ctx.getRequestBody());
            }

            if (ctx.getAuthType() != ApiContext.AuthType.NONE) {
                logger.info("Auth Type: {}", ctx.getAuthType());
            }
        }
    }

    /**
     * Log response information (nếu logging enabled)
     */
    private void logResponse(Response response) {
        ApiContext ctx = getContext();

        if (ctx.isLogRequests()) {
            logger.info("───────────────────────────────────────────────────");
            logger.info("Response Status: {} {} ({}ms)",
                    response.getStatusCode(),
                    response.getStatusLine(),
                    response.getTime());

            logger.info("Response Headers: {}", response.getHeaders());

            String body = response.getBody().asString();
            if (body != null && !body.isEmpty()) {
                if (body.length() > 500) {
                    logger.info("Response Body: {} ... (truncated, total {} chars)",
                            body.substring(0, 500), body.length());
                } else {
                    logger.info("Response Body: {}", body);
                }
            }

            logger.info("═══════════════════════════════════════════════════");
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Convert content type shorthand to full MIME type
     *
     * @param shortForm Shorthand (JSON, XML, FORM, TEXT, HTML)
     * @return Full MIME type
     */
    protected String convertToMimeType(String shortForm) {
        if (shortForm == null || shortForm.isEmpty()) {
            return null;
        }

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
            case "OCTET":
            case "BINARY":
                return "application/octet-stream";
            default:
                // Already a MIME type or custom type
                return shortForm;
        }
    }

    /**
     * Validate endpoint URL
     */
    protected void validateEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
    }

    /**
     * Validate file path
     */
    protected void validateFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + filePath);
        }
    }

    /**
     * Build full URL from base URI and endpoint
     */
    protected String buildFullUrl(String endpoint) {
        ApiContext ctx = getContext();
        String baseUri = ctx.getBaseUri();

        if (baseUri == null || baseUri.isEmpty()) {
            return endpoint;
        }

        // Remove trailing slash from base URI
        if (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length() - 1);
        }

        // Add leading slash to endpoint if missing
        if (!endpoint.startsWith("/") && !endpoint.startsWith("http")) {
            endpoint = "/" + endpoint;
        }

        return baseUri + endpoint;
    }

    /**
     * Pretty print JSON string
     */
    protected String prettyPrintJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            Object jsonObject = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (Exception e) {
            // If not valid JSON, return as-is
            return json;
        }
    }

    /**
     * Check if response is JSON
     */
    protected boolean isJsonResponse(Response response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    /**
     * Check if response is XML
     */
    protected boolean isXmlResponse(Response response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.toLowerCase().contains("xml");
    }

    /**
     * Extract filename from Content-Disposition header
     */
    protected String extractFilenameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isEmpty()) {
            return null;
        }

        // Pattern: filename="something.pdf" hoặc filename=something.pdf
        String[] parts = contentDisposition.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("filename=")) {
                String filename = part.substring("filename=".length());
                // Remove quotes if present
                filename = filename.replaceAll("\"", "");
                return filename;
            }
        }

        return null;
    }

    /**
     * Wait for a condition (polling)
     *
     * @param condition Lambda condition to check
     * @param timeoutSeconds Maximum wait time
     * @param pollIntervalMs Interval between checks
     * @return true if condition met, false if timeout
     */
    protected boolean waitForCondition(java.util.function.Supplier<Boolean> condition,
                                       int timeoutSeconds, int pollIntervalMs) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.get()) {
                return true;
            }

            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    // ========================================================================
    // CLEANUP METHODS
    // ========================================================================

    /**
     * Cleanup method - được gọi sau mỗi test (nếu cần)
     * Override method này trong subclass nếu cần custom cleanup
     */
    protected void cleanup() {
        // Default: clear request settings nhưng giữ config
        getContext().clearAllRequestSettings();
    }

    /**
     * Hook method - được gọi trước mỗi request
     * Override để thêm custom logic (VD: refresh token)
     */
    protected void beforeRequest() {
        // Default: do nothing
        // Subclasses có thể override
    }

    /**
     * Hook method - được gọi sau mỗi request
     * Override để thêm custom logic (VD: logging, metrics)
     */
    protected void afterRequest(ApiResponse response) {
        // Default: do nothing
        // Subclasses có thể override
    }

    // ========================================================================
    // DEPRECATED METHODS (For backward compatibility)
    // ========================================================================

    /**
     * @deprecated Use getContext() instead
     */
    @Deprecated
    protected ApiContext getApiContext() {
        return getContext();
    }
}