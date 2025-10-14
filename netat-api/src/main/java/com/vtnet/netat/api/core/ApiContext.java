package com.vtnet.netat.api.core;

import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;

/**
 * API Context - Quản lý toàn bộ cấu hình cho API requests
 * Thread-safe context cho mỗi test case
 *
 * @author NETAT Framework
 * @version 2.0
 */
public class ApiContext {

    // ========================================================================
    // FIELDS
    // ========================================================================

    // Configuration
    private String baseUri;
    private int timeout = 30; // seconds (default)
    private boolean logRequests = false;

    // Request components
    private Map<String, String> headers;
    private Map<String, Object> queryParams;
    private Map<String, Object> pathParams;
    private Map<String, Object> formParams;
    private Object requestBody;
    private String contentType;

    // Authentication
    private AuthType authType = AuthType.NONE;
    private String bearerToken;
    private String basicUsername;
    private String basicPassword;
    private String apiKeyName;
    private String apiKeyValue;
    private ApiKeyLocation apiKeyLocation;
    private String oauth2Token;

    // ========================================================================
    // ENUMS
    // ========================================================================

    /**
     * Authentication type
     */
    public enum AuthType {
        NONE,
        BEARER,
        BASIC,
        API_KEY,
        OAUTH2
    }

    /**
     * API Key location (Header hoặc Query parameter)
     */
    public enum ApiKeyLocation {
        HEADER,
        QUERY
    }

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public ApiContext() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.pathParams = new HashMap<>();
        this.formParams = new HashMap<>();
    }

    // ========================================================================
    // CONFIGURATION METHODS
    // ========================================================================

    /**
     * Set base URI for API
     */
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getBaseUri() {
        return baseUri;
    }

    /**
     * Set request timeout (seconds)
     */
    public void setTimeout(int timeoutSeconds) {
        this.timeout = timeoutSeconds;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Enable/disable request logging
     */
    public void setLogRequests(boolean enabled) {
        this.logRequests = enabled;
    }

    public boolean isLogRequests() {
        return logRequests;
    }

    /**
     * Set content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    // ========================================================================
    // HEADER METHODS
    // ========================================================================

    /**
     * Add a single header
     */
    public void addHeader(String name, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
    }

    /**
     * Add multiple headers
     */
    public void addHeaders(Map<String, String> newHeaders) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        if (newHeaders != null) {
            headers.putAll(newHeaders);
        }
    }

    /**
     * Remove a specific header
     */
    public void removeHeader(String headerName) {
        if (headers != null) {
            headers.remove(headerName);
        }
    }

    /**
     * Clear all headers
     */
    public void clearHeaders() {
        if (headers != null) {
            headers.clear();
        }
    }

    /**
     * Get all headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Check if header exists
     */
    public boolean hasHeader(String headerName) {
        return headers != null && headers.containsKey(headerName);
    }

    /**
     * Get specific header value
     */
    public String getHeader(String headerName) {
        return headers != null ? headers.get(headerName) : null;
    }

    // ========================================================================
    // QUERY PARAMETER METHODS
    // ========================================================================

    /**
     * Add a single query parameter
     */
    public void addQueryParam(String name, Object value) {
        if (queryParams == null) {
            queryParams = new HashMap<>();
        }
        queryParams.put(name, value);
    }

    /**
     * Add multiple query parameters
     */
    public void addQueryParams(Map<String, Object> newParams) {
        if (queryParams == null) {
            queryParams = new HashMap<>();
        }
        if (newParams != null) {
            queryParams.putAll(newParams);
        }
    }

    /**
     * Remove a specific query parameter
     */
    public void removeQueryParam(String paramName) {
        if (queryParams != null) {
            queryParams.remove(paramName);
        }
    }

    /**
     * Clear all query parameters
     */
    public void clearQueryParams() {
        if (queryParams != null) {
            queryParams.clear();
        }
    }

    /**
     * Get all query parameters
     */
    public Map<String, Object> getQueryParams() {
        return queryParams;
    }

    // ========================================================================
    // PATH PARAMETER METHODS
    // ========================================================================

    /**
     * Add a single path parameter
     */
    public void addPathParam(String name, Object value) {
        if (pathParams == null) {
            pathParams = new HashMap<>();
        }
        pathParams.put(name, value);
    }

    /**
     * Add multiple path parameters
     */
    public void addPathParams(Map<String, Object> newParams) {
        if (pathParams == null) {
            pathParams = new HashMap<>();
        }
        if (newParams != null) {
            pathParams.putAll(newParams);
        }
    }

    /**
     * Clear all path parameters
     */
    public void clearPathParams() {
        if (pathParams != null) {
            pathParams.clear();
        }
    }

    /**
     * Get all path parameters
     */
    public Map<String, Object> getPathParams() {
        return pathParams;
    }

    // ========================================================================
    // FORM PARAMETER METHODS
    // ========================================================================

    /**
     * Add a single form parameter
     */
    public void addFormParam(String name, Object value) {
        if (formParams == null) {
            formParams = new HashMap<>();
        }
        formParams.put(name, value);
    }

    /**
     * Add multiple form parameters
     */
    public void addFormParams(Map<String, Object> newParams) {
        if (formParams == null) {
            formParams = new HashMap<>();
        }
        if (newParams != null) {
            formParams.putAll(newParams);
        }
    }

    /**
     * Clear all form parameters
     */
    public void clearFormParams() {
        if (formParams != null) {
            formParams.clear();
        }
    }

    /**
     * Get all form parameters
     */
    public Map<String, Object> getFormParams() {
        return formParams;
    }

    // ========================================================================
    // REQUEST BODY METHODS
    // ========================================================================

    /**
     * Set request body
     */
    public void setRequestBody(Object body) {
        this.requestBody = body;
    }

    /**
     * Get request body
     */
    public Object getRequestBody() {
        return requestBody;
    }

    /**
     * Clear request body
     */
    public void clearRequestBody() {
        this.requestBody = null;
    }

    /**
     * Check if request body exists
     */
    public boolean hasRequestBody() {
        return requestBody != null;
    }

    // ========================================================================
    // AUTHENTICATION METHODS
    // ========================================================================

    /**
     * Set Bearer token authentication
     */
    public void setBearerToken(String token) {
        this.authType = AuthType.BEARER;
        this.bearerToken = token;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    /**
     * Set Basic authentication
     */
    public void setBasicAuth(String username, String password) {
        this.authType = AuthType.BASIC;
        this.basicUsername = username;
        this.basicPassword = password;
    }

    public String getBasicUsername() {
        return basicUsername;
    }

    public String getBasicPassword() {
        return basicPassword;
    }

    /**
     * Set API Key authentication
     */
    public void setApiKey(String keyName, String keyValue, ApiKeyLocation location) {
        this.authType = AuthType.API_KEY;
        this.apiKeyName = keyName;
        this.apiKeyValue = keyValue;
        this.apiKeyLocation = location;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public ApiKeyLocation getApiKeyLocation() {
        return apiKeyLocation;
    }

    /**
     * Set OAuth2 token
     */
    public void setOAuth2Token(String token) {
        this.authType = AuthType.OAUTH2;
        this.oauth2Token = token;
    }

    public String getOAuth2Token() {
        return oauth2Token;
    }

    /**
     * Get current auth type
     */
    public AuthType getAuthType() {
        return authType;
    }

    /**
     * Remove all authentication
     */
    public void removeAuth() {
        this.authType = AuthType.NONE;
        this.bearerToken = null;
        this.basicUsername = null;
        this.basicPassword = null;
        this.apiKeyName = null;
        this.apiKeyValue = null;
        this.apiKeyLocation = null;
        this.oauth2Token = null;
    }

    // ========================================================================
    // CLEAR METHODS
    // ========================================================================

    /**
     * Clear all request settings (headers, params, body)
     * Keep configuration (baseUri, timeout, auth)
     */
    public void clearAllRequestSettings() {
        clearHeaders();
        clearQueryParams();
        clearPathParams();
        clearFormParams();
        clearRequestBody();
        this.contentType = null;
    }

    /**
     * Reset entire context to initial state
     */
    public void reset() {
        // Clear all request settings
        clearAllRequestSettings();

        // Clear authentication
        removeAuth();

        // Reset configuration
        this.baseUri = null;
        this.timeout = 30;
        this.logRequests = false;
    }

    // ========================================================================
    // APPLY TO REQUEST SPECIFICATION
    // ========================================================================

    /**
     * Apply all context settings to RestAssured RequestSpecification
     *
     * @param spec RestAssured RequestSpecification
     * @return Modified RequestSpecification
     */
    public RequestSpecification applyToRequestSpec(RequestSpecification spec) {
        // Apply base URI
        if (baseUri != null && !baseUri.isEmpty()) {
            spec.baseUri(baseUri);
        }

        // Apply timeout
        if (timeout > 0) {
            // RestAssured timeout in milliseconds
            spec.config(io.restassured.config.RestAssuredConfig.config()
                    .connectionConfig(io.restassured.config.ConnectionConfig.connectionConfig()
                            .closeIdleConnectionsAfterEachResponseAfter(timeout, java.util.concurrent.TimeUnit.SECONDS)));
        }

        // Apply headers
        if (headers != null && !headers.isEmpty()) {
            spec.headers(headers);
        }

        // Apply content type
        if (contentType != null && !contentType.isEmpty()) {
            spec.contentType(contentType);
        }

        // Apply query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            spec.queryParams(queryParams);
        }

        // Apply path parameters
        if (pathParams != null && !pathParams.isEmpty()) {
            spec.pathParams(pathParams);
        }

        // Apply form parameters
        if (formParams != null && !formParams.isEmpty()) {
            spec.formParams(formParams);
        }

        // Apply request body
        if (requestBody != null) {
            spec.body(requestBody);
        }

        // Apply authentication
        applyAuthentication(spec);

        // Apply logging
        if (logRequests) {
            spec.log().all();
        }

        return spec;
    }

    /**
     * Apply authentication to request specification
     */
    private void applyAuthentication(RequestSpecification spec) {
        if (authType == null || authType == AuthType.NONE) {
            return;
        }

        switch (authType) {
            case BEARER:
                if (bearerToken != null && !bearerToken.isEmpty()) {
                    spec.header("Authorization", "Bearer " + bearerToken);
                }
                break;

            case BASIC:
                if (basicUsername != null && basicPassword != null) {
                    spec.auth().basic(basicUsername, basicPassword);
                }
                break;

            case API_KEY:
                if (apiKeyName != null && apiKeyValue != null) {
                    if (apiKeyLocation == ApiKeyLocation.HEADER) {
                        spec.header(apiKeyName, apiKeyValue);
                    } else if (apiKeyLocation == ApiKeyLocation.QUERY) {
                        spec.queryParam(apiKeyName, apiKeyValue);
                    }
                }
                break;

            case OAUTH2:
                if (oauth2Token != null && !oauth2Token.isEmpty()) {
                    spec.header("Authorization", "Bearer " + oauth2Token);
                }
                break;

            default:
                // No authentication
                break;
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Check if context has any configuration
     */
    public boolean isEmpty() {
        return baseUri == null &&
                (headers == null || headers.isEmpty()) &&
                (queryParams == null || queryParams.isEmpty()) &&
                (pathParams == null || pathParams.isEmpty()) &&
                (formParams == null || formParams.isEmpty()) &&
                requestBody == null &&
                authType == AuthType.NONE;
    }

    /**
     * Create a copy of this context
     */
    public ApiContext copy() {
        ApiContext newContext = new ApiContext();

        // Copy configuration
        newContext.baseUri = this.baseUri;
        newContext.timeout = this.timeout;
        newContext.logRequests = this.logRequests;
        newContext.contentType = this.contentType;

        // Copy collections (deep copy)
        if (this.headers != null) {
            newContext.headers = new HashMap<>(this.headers);
        }
        if (this.queryParams != null) {
            newContext.queryParams = new HashMap<>(this.queryParams);
        }
        if (this.pathParams != null) {
            newContext.pathParams = new HashMap<>(this.pathParams);
        }
        if (this.formParams != null) {
            newContext.formParams = new HashMap<>(this.formParams);
        }

        // Copy body
        newContext.requestBody = this.requestBody;

        // Copy authentication
        newContext.authType = this.authType;
        newContext.bearerToken = this.bearerToken;
        newContext.basicUsername = this.basicUsername;
        newContext.basicPassword = this.basicPassword;
        newContext.apiKeyName = this.apiKeyName;
        newContext.apiKeyValue = this.apiKeyValue;
        newContext.apiKeyLocation = this.apiKeyLocation;
        newContext.oauth2Token = this.oauth2Token;

        return newContext;
    }

    /**
     * Get context summary (for debugging/logging)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ApiContext{");

        if (baseUri != null) {
            sb.append("baseUri='").append(baseUri).append("', ");
        }

        if (authType != AuthType.NONE) {
            sb.append("authType=").append(authType).append(", ");
        }

        if (headers != null && !headers.isEmpty()) {
            sb.append("headers=").append(headers.size()).append(" items, ");
        }

        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append("queryParams=").append(queryParams.size()).append(" items, ");
        }

        if (pathParams != null && !pathParams.isEmpty()) {
            sb.append("pathParams=").append(pathParams.size()).append(" items, ");
        }

        if (requestBody != null) {
            sb.append("hasBody=true, ");
        }

        sb.append("timeout=").append(timeout).append("s");
        sb.append("}");

        return sb.toString();
    }
}