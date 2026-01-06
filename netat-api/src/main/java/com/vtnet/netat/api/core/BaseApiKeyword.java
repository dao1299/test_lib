package com.vtnet.netat.api.core;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.core.secret.SecretDecryptor;
import com.vtnet.netat.core.secret.SensitiveDataProtection;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.config.SSLConfig;
import io.restassured.config.RestAssuredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public abstract class BaseApiKeyword extends BaseKeyword {

    protected static final NetatLogger logger = NetatLogger.getInstance(BaseApiKeyword.class);
    protected static final SensitiveDataProtection protection = SensitiveDataProtection.getInstance();

    protected static ThreadLocal<ApiContext> contextThreadLocal = ThreadLocal.withInitial(ApiContext::new);

    protected ApiContext getContext() {
        return contextThreadLocal.get();
    }

    protected void resetContext() {
        contextThreadLocal.get().reset();
    }


    protected void removeContext() {
        contextThreadLocal.remove();
    }


    public static void cleanupThreadLocal() {
        contextThreadLocal.remove();
    }


    protected ApiResponse executeGet(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("GET", endpoint);

        Response response = spec.when().get(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }


    protected ApiResponse executePost(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("POST", endpoint);

        Response response = spec.when().post(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }


    protected ApiResponse executePut(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("PUT", endpoint);

        Response response = spec.when().put(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }


    protected ApiResponse executePatch(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("PATCH", endpoint);

        Response response = spec.when().patch(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }


    protected ApiResponse executeDelete(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("DELETE", endpoint);

        Response response = spec.when().delete(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }


    protected ApiResponse executeHead(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("HEAD", endpoint);

        Response response = spec.when().head(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }


    protected ApiResponse executeOptions(String endpoint) {
        RequestSpecification spec = buildRequestSpec();

        logRequest("OPTIONS", endpoint);

        Response response = spec.when().options(endpoint);

        logResponse(response);

        return new ApiResponse(response);
    }

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


    protected ApiResponse executeMultipartUploadWithMetadata(String endpoint, String filePath,
                                                             String fileFieldName, Map<String, String> metadata) {
        RequestSpecification spec = buildRequestSpec();

        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        spec.multiPart(fileFieldName, file);

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

    private RequestSpecification buildRequestSpec() {
        RequestSpecification spec = RestAssured.given();

        ApiContext ctx = getContext();

        if (!ctx.isSslVerificationEnabled()) {
            spec.config(RestAssuredConfig.config()
                    .sslConfig(SSLConfig.sslConfig()
                            .relaxedHTTPSValidation()
                            .allowAllHostnames()));
            logger.info("SSL verification DISABLED - use only for testing!");
        }

        ctx.applyToRequestSpec(spec);

        return spec;
    }


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
                return shortForm;
        }
    }

    protected void validateEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
    }

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

    protected String buildFullUrl(String endpoint) {
        ApiContext ctx = getContext();
        String baseUri = ctx.getBaseUri();

        if (baseUri == null || baseUri.isEmpty()) {
            return endpoint;
        }

        if (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length() - 1);
        }

        // Add leading slash to endpoint if missing
        if (!endpoint.startsWith("/") && !endpoint.startsWith("http")) {
            endpoint = "/" + endpoint;
        }

        return baseUri + endpoint;
    }

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
            return json;
        }
    }

    protected boolean isJsonResponse(Response response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    protected boolean isXmlResponse(Response response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.toLowerCase().contains("xml");
    }


    protected String extractFilenameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isEmpty()) {
            return null;
        }
        String[] parts = contentDisposition.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("filename=")) {
                String filename = part.substring("filename=".length());
                filename = filename.replaceAll("\"", "");
                return filename;
            }
        }

        return null;
    }

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

    protected void cleanup() {
        // ✅ FIX: Luôn gọi getContext()
        getContext().clearAllRequestSettings();
    }


    protected void beforeRequest() {

    }

    protected void afterRequest(ApiResponse response) {

    }

    /**
     * @deprecated Use getContext() instead
     */
    @Deprecated
    protected ApiContext getApiContext() {
        return getContext();
    }
}