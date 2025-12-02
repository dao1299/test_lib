package com.vtnet.netat.api.curl;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser để chuyển đổi cURL command thành các thành phần request
 * Hỗ trợ các options phổ biến: -X, -H, -d, --data, -u, -k, --insecure, -b, --cookie
 *
 * @author NETAT Framework
 * @version 1.0
 */
public class CurlParser {

    /**
     * Kết quả parse từ cURL command
     */
    public static class ParsedCurl {
        private String method = "GET";
        private String url;
        private Map<String, String> headers = new LinkedHashMap<>();
        private String body;
        private String basicAuthUser;
        private String basicAuthPassword;
        private boolean insecure = false;
        private Map<String, String> cookies = new LinkedHashMap<>();
        private Map<String, String> formData = new LinkedHashMap<>();
        private boolean isFormData = false;

        // Getters and Setters
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public Map<String, String> getHeaders() { return headers; }
        public void addHeader(String name, String value) { headers.put(name, value); }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }

        public String getBasicAuthUser() { return basicAuthUser; }
        public String getBasicAuthPassword() { return basicAuthPassword; }
        public void setBasicAuth(String user, String password) {
            this.basicAuthUser = user;
            this.basicAuthPassword = password;
        }
        public boolean hasBasicAuth() { return basicAuthUser != null; }

        public boolean isInsecure() { return insecure; }
        public void setInsecure(boolean insecure) { this.insecure = insecure; }

        public Map<String, String> getCookies() { return cookies; }
        public void addCookie(String name, String value) { cookies.put(name, value); }

        public Map<String, String> getFormData() { return formData; }
        public void addFormData(String name, String value) {
            formData.put(name, value);
            isFormData = true;
        }
        public boolean isFormData() { return isFormData; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ParsedCurl {\n");
            sb.append("  method: ").append(method).append("\n");
            sb.append("  url: ").append(url).append("\n");
            sb.append("  headers: ").append(headers).append("\n");
            if (body != null) {
                sb.append("  body: ").append(body.length() > 100 ? body.substring(0, 100) + "..." : body).append("\n");
            }
            if (hasBasicAuth()) {
                sb.append("  basicAuth: ").append(basicAuthUser).append(":***\n");
            }
            if (insecure) {
                sb.append("  insecure: true\n");
            }
            if (!cookies.isEmpty()) {
                sb.append("  cookies: ").append(cookies).append("\n");
            }
            if (!formData.isEmpty()) {
                sb.append("  formData: ").append(formData).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Parse cURL command thành ParsedCurl object
     *
     * @param curlCommand cURL command string
     * @return ParsedCurl object chứa các thành phần đã parse
     * @throws IllegalArgumentException nếu command không hợp lệ
     */
    public static ParsedCurl parse(String curlCommand) {
        if (curlCommand == null || curlCommand.trim().isEmpty()) {
            throw new IllegalArgumentException("cURL command cannot be null or empty");
        }

        ParsedCurl result = new ParsedCurl();

        // Normalize command: remove line continuations và extra whitespace
        String normalized = normalizeCommand(curlCommand);

        // Tokenize command
        List<String> tokens = tokenize(normalized);

        if (tokens.isEmpty() || !tokens.get(0).equalsIgnoreCase("curl")) {
            throw new IllegalArgumentException("Command must start with 'curl'");
        }

        // Parse tokens
        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i);

            switch (token) {
                case "-X":
                case "--request":
                    if (i + 1 < tokens.size()) {
                        result.setMethod(tokens.get(++i).toUpperCase());
                    }
                    break;

                case "-H":
                case "--header":
                    if (i + 1 < tokens.size()) {
                        parseHeader(tokens.get(++i), result);
                    }
                    break;

                case "-d":
                case "--data":
                case "--data-raw":
                case "--data-binary":
                    if (i + 1 < tokens.size()) {
                        result.setBody(tokens.get(++i));
                        // Nếu có body và method vẫn là GET, đổi sang POST
                        if ("GET".equals(result.getMethod())) {
                            result.setMethod("POST");
                        }
                    }
                    break;

                case "--data-urlencode":
                    if (i + 1 < tokens.size()) {
                        parseFormDataItem(tokens.get(++i), result);
                        if ("GET".equals(result.getMethod())) {
                            result.setMethod("POST");
                        }
                    }
                    break;

                case "-F":
                case "--form":
                    if (i + 1 < tokens.size()) {
                        parseFormDataItem(tokens.get(++i), result);
                        if ("GET".equals(result.getMethod())) {
                            result.setMethod("POST");
                        }
                    }
                    break;

                case "-u":
                case "--user":
                    if (i + 1 < tokens.size()) {
                        parseBasicAuth(tokens.get(++i), result);
                    }
                    break;

                case "-k":
                case "--insecure":
                    result.setInsecure(true);
                    break;

                case "-b":
                case "--cookie":
                    if (i + 1 < tokens.size()) {
                        parseCookies(tokens.get(++i), result);
                    }
                    break;

                case "-A":
                case "--user-agent":
                    if (i + 1 < tokens.size()) {
                        result.addHeader("User-Agent", tokens.get(++i));
                    }
                    break;

                case "-e":
                case "--referer":
                    if (i + 1 < tokens.size()) {
                        result.addHeader("Referer", tokens.get(++i));
                    }
                    break;

                case "--compressed":
                    result.addHeader("Accept-Encoding", "gzip, deflate");
                    break;

                case "-L":
                case "--location":
                    // Follow redirects - REST Assured handles this by default
                    break;

                case "-v":
                case "--verbose":
                case "-s":
                case "--silent":
                case "-S":
                case "--show-error":
                case "-o":
                case "--output":
                case "-O":
                case "--remote-name":
                    // Skip these options (and their values if applicable)
                    if ((token.equals("-o") || token.equals("--output")) && i + 1 < tokens.size()) {
                        i++; // Skip the output filename
                    }
                    break;

                default:
                    // Nếu không phải option (không bắt đầu bằng -), có thể là URL
                    if (!token.startsWith("-") && result.getUrl() == null) {
                        result.setUrl(cleanUrl(token));
                    }
                    break;
            }
        }

        // Validate
        if (result.getUrl() == null || result.getUrl().isEmpty()) {
            throw new IllegalArgumentException("No URL found in cURL command");
        }

        return result;
    }

    /**
     * Normalize cURL command - xử lý line continuation và whitespace
     */
    private static String normalizeCommand(String command) {
        // Remove line continuations (backslash + newline)
        String normalized = command.replaceAll("\\\\\\s*\\n\\s*", " ");
        // Remove multiple spaces
        normalized = normalized.replaceAll("\\s+", " ");
        // Trim
        return normalized.trim();
    }

    /**
     * Tokenize command - tách thành các tokens, xử lý quotes
     */
    private static List<String> tokenize(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && !inSingleQuote) {
                escaped = true;
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (c == ' ' && !inSingleQuote && !inDoubleQuote) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    /**
     * Parse header string "Name: Value"
     */
    private static void parseHeader(String headerStr, ParsedCurl result) {
        int colonIndex = headerStr.indexOf(':');
        if (colonIndex > 0) {
            String name = headerStr.substring(0, colonIndex).trim();
            String value = headerStr.substring(colonIndex + 1).trim();
            result.addHeader(name, value);
        }
    }

    /**
     * Parse basic auth "user:password"
     */
    private static void parseBasicAuth(String authStr, ParsedCurl result) {
        int colonIndex = authStr.indexOf(':');
        if (colonIndex > 0) {
            String user = authStr.substring(0, colonIndex);
            String password = authStr.substring(colonIndex + 1);
            result.setBasicAuth(user, password);
        } else {
            result.setBasicAuth(authStr, "");
        }
    }

    /**
     * Parse cookies "name=value; name2=value2"
     */
    private static void parseCookies(String cookieStr, ParsedCurl result) {
        String[] cookies = cookieStr.split(";");
        for (String cookie : cookies) {
            cookie = cookie.trim();
            int eqIndex = cookie.indexOf('=');
            if (eqIndex > 0) {
                String name = cookie.substring(0, eqIndex).trim();
                String value = cookie.substring(eqIndex + 1).trim();
                result.addCookie(name, value);
            }
        }
    }

    /**
     * Parse form data item "name=value"
     */
    private static void parseFormDataItem(String formStr, ParsedCurl result) {
        int eqIndex = formStr.indexOf('=');
        if (eqIndex > 0) {
            String name = formStr.substring(0, eqIndex).trim();
            String value = formStr.substring(eqIndex + 1).trim();
            result.addFormData(name, value);
        }
    }

    /**
     * Clean URL - remove surrounding quotes if present
     */
    private static String cleanUrl(String url) {
        if (url == null) return null;
        url = url.trim();
        // Remove surrounding quotes
        if ((url.startsWith("'") && url.endsWith("'")) ||
                (url.startsWith("\"") && url.endsWith("\""))) {
            url = url.substring(1, url.length() - 1);
        }
        return url;
    }

    /**
     * Utility method để kiểm tra xem một cURL command có hợp lệ không
     */
    public static boolean isValidCurl(String command) {
        try {
            parse(command);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}