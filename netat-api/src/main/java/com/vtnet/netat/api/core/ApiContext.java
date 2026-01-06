package com.vtnet.netat.api.core;

import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;


public class ApiContext {

    private String baseUri;
    private int timeout = 30; // seconds (default)
    private boolean logRequests = false;
    private boolean sslVerificationEnabled = true;

    private Map<String, String> headers;
    private Map<String, Object> queryParams;
    private Map<String, Object> pathParams;
    private Map<String, Object> formParams;
    private Object requestBody;
    private String contentType;

    private AuthType authType = AuthType.NONE;
    private String bearerToken;
    private String basicUsername;
    private String basicPassword;
    private String apiKeyName;
    private String apiKeyValue;
    private ApiKeyLocation apiKeyLocation;
    private String oauth2Token;

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


    public ApiContext() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.pathParams = new HashMap<>();
        this.formParams = new HashMap<>();
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getBaseUri() {
        return baseUri;
    }


    public void setTimeout(int timeoutSeconds) {
        this.timeout = timeoutSeconds;
    }

    public int getTimeout() {
        return timeout;
    }


    public void setLogRequests(boolean enabled) {
        this.logRequests = enabled;
    }

    public boolean isLogRequests() {
        return logRequests;
    }


    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void addHeader(String name, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
    }


    public void addHeaders(Map<String, String> newHeaders) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        if (newHeaders != null) {
            headers.putAll(newHeaders);
        }
    }

    public void removeHeader(String headerName) {
        if (headers != null) {
            headers.remove(headerName);
        }
    }

    public void clearHeaders() {
        if (headers != null) {
            headers.clear();
        }
    }


    public Map<String, String> getHeaders() {
        return headers;
    }


    public boolean hasHeader(String headerName) {
        return headers != null && headers.containsKey(headerName);
    }

    public String getHeader(String headerName) {
        return headers != null ? headers.get(headerName) : null;
    }


    public void addQueryParam(String name, Object value) {
        if (queryParams == null) {
            queryParams = new HashMap<>();
        }
        queryParams.put(name, value);
    }


    public void addQueryParams(Map<String, Object> newParams) {
        if (queryParams == null) {
            queryParams = new HashMap<>();
        }
        if (newParams != null) {
            queryParams.putAll(newParams);
        }
    }

    public void removeQueryParam(String paramName) {
        if (queryParams != null) {
            queryParams.remove(paramName);
        }
    }


    public void clearQueryParams() {
        if (queryParams != null) {
            queryParams.clear();
        }
    }


    public Map<String, Object> getQueryParams() {
        return queryParams;
    }

    public void addPathParam(String name, Object value) {
        if (pathParams == null) {
            pathParams = new HashMap<>();
        }
        pathParams.put(name, value);
    }


    public void addPathParams(Map<String, Object> newParams) {
        if (pathParams == null) {
            pathParams = new HashMap<>();
        }
        if (newParams != null) {
            pathParams.putAll(newParams);
        }
    }


    public void clearPathParams() {
        if (pathParams != null) {
            pathParams.clear();
        }
    }


    public Map<String, Object> getPathParams() {
        return pathParams;
    }


    public void addFormParam(String name, Object value) {
        if (formParams == null) {
            formParams = new HashMap<>();
        }
        formParams.put(name, value);
    }

    public void addFormParams(Map<String, Object> newParams) {
        if (formParams == null) {
            formParams = new HashMap<>();
        }
        if (newParams != null) {
            formParams.putAll(newParams);
        }
    }


    public void clearFormParams() {
        if (formParams != null) {
            formParams.clear();
        }
    }


    public Map<String, Object> getFormParams() {
        return formParams;
    }

    public void setRequestBody(Object body) {
        this.requestBody = body;
    }


    public Object getRequestBody() {
        return requestBody;
    }


    public void clearRequestBody() {
        this.requestBody = null;
    }


    public boolean hasRequestBody() {
        return requestBody != null;
    }


    public void setBearerToken(String token) {
        this.authType = AuthType.BEARER;
        this.bearerToken = token;
    }

    public String getBearerToken() {
        return bearerToken;
    }


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

    public void setOAuth2Token(String token) {
        this.authType = AuthType.OAUTH2;
        this.oauth2Token = token;
    }

    public String getOAuth2Token() {
        return oauth2Token;
    }


    public AuthType getAuthType() {
        return authType;
    }

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

    public void clearAllRequestSettings() {
        clearHeaders();
        clearQueryParams();
        clearPathParams();
        clearFormParams();
        clearRequestBody();
        this.contentType = null;
    }

    public void reset() {
        clearAllRequestSettings();

        removeAuth();

        // Reset configuration
        this.baseUri = null;
        this.timeout = 30;
        this.logRequests = false;
    }


    public RequestSpecification applyToRequestSpec(RequestSpecification spec) {

        if (baseUri != null && !baseUri.isEmpty()) {
            spec.baseUri(baseUri);
        }

        if (timeout > 0) {
            spec.config(io.restassured.config.RestAssuredConfig.config()
                    .connectionConfig(io.restassured.config.ConnectionConfig.connectionConfig()
                            .closeIdleConnectionsAfterEachResponseAfter(timeout, java.util.concurrent.TimeUnit.SECONDS)));
        }

        if (headers != null && !headers.isEmpty()) {
            spec.headers(headers);
        }

        if (contentType != null && !contentType.isEmpty()) {
            spec.contentType(contentType);
        }

        if (queryParams != null && !queryParams.isEmpty()) {
            spec.queryParams(queryParams);
        }

        if (pathParams != null && !pathParams.isEmpty()) {
            spec.pathParams(pathParams);
        }

        if (formParams != null && !formParams.isEmpty()) {
            spec.formParams(formParams);
        }

        // Apply request body
        if (requestBody != null) {
            spec.body(requestBody);
        }

        applyAuthentication(spec);

        if (logRequests) {
            spec.log().all();
        }

        return spec;
    }

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


    public boolean isEmpty() {
        return baseUri == null &&
                (headers == null || headers.isEmpty()) &&
                (queryParams == null || queryParams.isEmpty()) &&
                (pathParams == null || pathParams.isEmpty()) &&
                (formParams == null || formParams.isEmpty()) &&
                requestBody == null &&
                authType == AuthType.NONE;
    }

    public ApiContext copy() {
        ApiContext newContext = new ApiContext();

        newContext.baseUri = this.baseUri;
        newContext.timeout = this.timeout;
        newContext.logRequests = this.logRequests;
        newContext.contentType = this.contentType;

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

    public void setSslVerificationEnabled(boolean enabled) {
        this.sslVerificationEnabled = enabled;
    }

    public boolean isSslVerificationEnabled() {
        return sslVerificationEnabled;
    }
}