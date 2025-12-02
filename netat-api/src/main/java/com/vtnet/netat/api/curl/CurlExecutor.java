package com.vtnet.netat.api.curl;

import com.vtnet.netat.api.core.ApiContext;
import com.vtnet.netat.api.core.ApiResponse;
import com.vtnet.netat.core.logging.NetatLogger;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * Executor để thực thi ParsedCurl thành HTTP request
 *
 * @author NETAT Framework
 * @version 1.0
 */
public class CurlExecutor {

    private static final NetatLogger logger = NetatLogger.getInstance(CurlExecutor.class);

    /**
     * Execute một ParsedCurl object
     *
     * @param parsedCurl ParsedCurl đã được parse từ cURL command
     * @return ApiResponse chứa kết quả response
     */
    public static ApiResponse execute(CurlParser.ParsedCurl parsedCurl) {
        return execute(parsedCurl, null);
    }

    /**
     * Execute một ParsedCurl object với ApiContext bổ sung
     *
     * @param parsedCurl ParsedCurl đã được parse từ cURL command
     * @param additionalContext ApiContext chứa các cấu hình bổ sung (có thể null)
     * @return ApiResponse chứa kết quả response
     */
    public static ApiResponse execute(CurlParser.ParsedCurl parsedCurl, ApiContext additionalContext) {
        logger.info("Executing cURL: {} {}", parsedCurl.getMethod(), parsedCurl.getUrl());

        RequestSpecification spec = RestAssured.given();

        // Apply SSL config nếu insecure
        if (parsedCurl.isInsecure()) {
            spec.config(RestAssuredConfig.config()
                    .sslConfig(SSLConfig.sslConfig()
                            .relaxedHTTPSValidation()
                            .allowAllHostnames()));
            logger.warn("⚠️ SSL verification DISABLED (--insecure flag)");
        }

        // Apply additional context SSL config nếu có
        if (additionalContext != null && !additionalContext.isSslVerificationEnabled()) {
            spec.config(RestAssuredConfig.config()
                    .sslConfig(SSLConfig.sslConfig()
                            .relaxedHTTPSValidation()
                            .allowAllHostnames()));
        }

        // Apply headers
        for (Map.Entry<String, String> header : parsedCurl.getHeaders().entrySet()) {
            spec.header(header.getKey(), header.getValue());
            logger.debug("Header: {} = {}", header.getKey(),
                    header.getKey().toLowerCase().contains("authorization") ? "***" : header.getValue());
        }

        // Apply basic auth
        if (parsedCurl.hasBasicAuth()) {
            spec.auth().basic(parsedCurl.getBasicAuthUser(), parsedCurl.getBasicAuthPassword());
            logger.debug("Basic Auth: {} : ***", parsedCurl.getBasicAuthUser());
        }

        // Apply cookies
        if (!parsedCurl.getCookies().isEmpty()) {
            spec.cookies(parsedCurl.getCookies());
            logger.debug("Cookies: {}", parsedCurl.getCookies().keySet());
        }

        // Apply body hoặc form data
        if (parsedCurl.isFormData() && !parsedCurl.getFormData().isEmpty()) {
            spec.formParams(parsedCurl.getFormData());
            logger.debug("Form data fields: {}", parsedCurl.getFormData().keySet());
        } else if (parsedCurl.getBody() != null && !parsedCurl.getBody().isEmpty()) {
            spec.body(parsedCurl.getBody());
            logger.debug("Body length: {} chars", parsedCurl.getBody().length());
        }

        // Apply additional context settings
        if (additionalContext != null) {
            // Apply timeout
            if (additionalContext.getTimeout() > 0) {
                spec.config(RestAssuredConfig.config()
                        .connectionConfig(io.restassured.config.ConnectionConfig.connectionConfig()
                                .closeIdleConnectionsAfterEachResponseAfter(
                                        additionalContext.getTimeout(),
                                        java.util.concurrent.TimeUnit.SECONDS)));
            }

            // Apply additional headers từ context (không override từ cURL)
            if (additionalContext.getHeaders() != null) {
                for (Map.Entry<String, String> header : additionalContext.getHeaders().entrySet()) {
                    if (!parsedCurl.getHeaders().containsKey(header.getKey())) {
                        spec.header(header.getKey(), header.getValue());
                    }
                }
            }

            // Apply logging
            if (additionalContext.isLogRequests()) {
                spec.log().all();
            }
        }

        // Execute request based on method
        Response response = executeMethod(spec, parsedCurl.getMethod(), parsedCurl.getUrl());

        logger.info("Response: {} {} ({}ms)",
                response.getStatusCode(),
                response.getStatusLine(),
                response.getTime());

        return new ApiResponse(response);
    }

    /**
     * Execute HTTP method
     */
    private static Response executeMethod(RequestSpecification spec, String method, String url) {
        switch (method.toUpperCase()) {
            case "GET":
                return spec.when().get(url);
            case "POST":
                return spec.when().post(url);
            case "PUT":
                return spec.when().put(url);
            case "PATCH":
                return spec.when().patch(url);
            case "DELETE":
                return spec.when().delete(url);
            case "HEAD":
                return spec.when().head(url);
            case "OPTIONS":
                return spec.when().options(url);
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    /**
     * Execute cURL command trực tiếp từ string
     *
     * @param curlCommand cURL command string
     * @return ApiResponse
     */
    public static ApiResponse execute(String curlCommand) {
        CurlParser.ParsedCurl parsed = CurlParser.parse(curlCommand);
        return execute(parsed);
    }

    /**
     * Execute cURL command với context bổ sung
     *
     * @param curlCommand cURL command string
     * @param additionalContext ApiContext bổ sung
     * @return ApiResponse
     */
    public static ApiResponse execute(String curlCommand, ApiContext additionalContext) {
        CurlParser.ParsedCurl parsed = CurlParser.parse(curlCommand);
        return execute(parsed, additionalContext);
    }
}