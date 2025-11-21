package com.vtnet.netat.api.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.response.Response;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper class cho REST Assured Response
 * Cung cấp các phương thức tiện lợi để truy xuất dữ liệu
 */
public class ApiResponse {

    private final Response restAssuredResponse;
    private final long responseTime;
    private final int statusCode;
    private final String statusLine;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ApiResponse(Response response) {
        this.restAssuredResponse = response;
        this.responseTime = response.getTime();
        this.statusCode = response.getStatusCode();
        this.statusLine = response.getStatusLine();

        // Tự động attach vào Allure Report
        attachToAllure();
    }

    // === BASIC INFO ===

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusLine() {
        return statusLine;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public String getContentType() {
        return restAssuredResponse.getContentType();
    }

    // === BODY ===

    public String getBody() {
        return restAssuredResponse.getBody().asString();
    }

    public String getPrettyBody() {
        return restAssuredResponse.getBody().asPrettyString();
    }

    public byte[] getBodyAsByteArray() {
        return restAssuredResponse.getBody().asByteArray();
    }

    public <T> T getBodyAs(Class<T> clazz) {
        return restAssuredResponse.as(clazz);
    }

    // === JSON ===

    public String getJsonPath(String path) {
        String normalizedPath = (path != null && !path.startsWith("$")) ? "$." + path : path;

        try {
            Object result = JsonPath.read(getBody(), normalizedPath);
            return result != null ? result.toString() : null;
        } catch (PathNotFoundException e) {
            return null;
        } catch (Exception e) {
            try {
                String restPath = path.startsWith("$.") ? path.substring(2) : path;
                return restAssuredResponse.jsonPath().getString(restPath);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public Integer getJsonPathAsInt(String path) {
        String normalizedPath = (path != null && !path.startsWith("$")) ? "$." + path : path;
        try {
            return JsonPath.read(getBody(), normalizedPath);
        } catch (Exception e) {
            try {
                String restPath = path.startsWith("$.") ? path.substring(2) : path;
                return restAssuredResponse.jsonPath().getInt(restPath);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public Double getJsonPathAsDouble(String path) {
        String normalizedPath = (path != null && !path.startsWith("$")) ? "$." + path : path;
        try {
            return JsonPath.read(getBody(), normalizedPath);
        } catch (Exception e) {
            try {
                String restPath = path.startsWith("$.") ? path.substring(2) : path;
                return restAssuredResponse.jsonPath().getDouble(restPath);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public Boolean getJsonPathAsBoolean(String path) {
        String normalizedPath = (path != null && !path.startsWith("$")) ? "$." + path : path;
        try {
            return JsonPath.read(getBody(), normalizedPath);
        } catch (Exception e) {
            try {
                String restPath = path.startsWith("$.") ? path.substring(2) : path;
                return restAssuredResponse.jsonPath().getBoolean(restPath);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public <T> List<T> getJsonPathAsList(String path) {
        String normalizedPath = (path != null && !path.startsWith("$")) ? "$." + path : path;
        try {
            return JsonPath.read(getBody(), normalizedPath);
        } catch (Exception e) {
            try {
                String restPath = path.startsWith("$.") ? path.substring(2) : path;
                return restAssuredResponse.jsonPath().getList(restPath);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public Map<String, Object> getBodyAsMap() {
        try {
            return objectMapper.readValue(getBody(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot parse response body as Map", e);
        }
    }

    // === XML ===

    public String getXPath(String xpath) {
        try {
            return restAssuredResponse.xmlPath().getString(xpath);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getXPathAsList(String xpath) {
        try {
            return restAssuredResponse.xmlPath().getList(xpath);
        } catch (Exception e) {
            return null;
        }
    }

    // === HEADERS ===

    public String getHeader(String headerName) {
        return restAssuredResponse.getHeader(headerName);
    }

    public Map<String, String> getHeaders() {
        return restAssuredResponse.getHeaders().asList().stream()
                .collect(Collectors.toMap(
                        Header::getName,
                        Header::getValue,
                        (existing, replacement) -> existing // Keep first value if duplicate
                ));
    }

    public boolean hasHeader(String headerName) {
        return restAssuredResponse.getHeader(headerName) != null;
    }

    // === COOKIES ===

    public String getCookie(String cookieName) {
        Cookie cookie = restAssuredResponse.getDetailedCookie(cookieName);
        return cookie != null ? cookie.getValue() : null;
    }

    public Map<String, String> getCookies() {
        return restAssuredResponse.getCookies();
    }

    public boolean hasCookie(String cookieName) {
        return restAssuredResponse.getCookie(cookieName) != null;
    }

    // === ALLURE REPORTING ===

    @Attachment(value = "API Response", type = "application/json")
    private String attachToAllure() {
        try {
            String body = getBody();
            // Try to pretty print JSON
            try {
                Object json = objectMapper.readValue(body, Object.class);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            } catch (Exception e) {
                // Not JSON, return as is
                return body;
            }
        } catch (Exception e) {
            return "Unable to attach response body: " + e.getMessage();
        }
    }

    // === INTERNAL ===

    public Response getRestAssuredResponse() {
        return restAssuredResponse;
    }

    @Override
    public String toString() {
        return String.format("ApiResponse{statusCode=%d, time=%dms, contentType='%s'}",
                statusCode, responseTime, getContentType());
    }
}