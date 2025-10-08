// File: netat-web/src/main/java/com/vtnet/netat/web/ai/HtmlSanitizer.java
package com.vtnet.netat.web.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Sanitize HTML trước khi gửi cho AI
 */
public class HtmlSanitizer {

    private static final Logger log = LoggerFactory.getLogger(HtmlSanitizer.class);

    // Patterns cho sensitive data
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}"
    );

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(token|api[_-]?key|auth|session|jwt)[\"']?\\s*[:=]\\s*[\"']([^\"'\\s]+)[\"']?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"
    );

    /**
     * Sanitize HTML - remove sensitive data
     */
    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        String sanitized = html;

        // 1. Remove emails
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("***@***.***");

        // 2. Remove phone numbers
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("***-***-****");

        // 3. Remove tokens/API keys
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("$1=\"***\"");

        // 4. Remove credit cards
        sanitized = CREDIT_CARD_PATTERN.matcher(sanitized).replaceAll("****-****-****-****");

        // 5. Remove common sensitive attributes
        sanitized = sanitizeSensitiveAttributes(sanitized);

        log.debug("Sanitized HTML: removed {} sensitive data instances",
                countSensitiveData(html) - countSensitiveData(sanitized));

        return sanitized;
    }

    /**
     * Sanitize sensitive HTML attributes
     */
    private static String sanitizeSensitiveAttributes(String html) {
        // Remove values của sensitive attributes
        String[] sensitiveAttrs = {
                "password", "secret", "token", "api-key", "auth", "session"
        };

        String result = html;
        for (String attr : sensitiveAttrs) {
            result = result.replaceAll(
                    "(?i)" + attr + "\\s*=\\s*[\"']([^\"']+)[\"']",
                    attr + "=\"***\""
            );
        }

        return result;
    }

    /**
     * Count sensitive data instances (for logging)
     */
    private static int countSensitiveData(String html) {
        int count = 0;
        count += countMatches(EMAIL_PATTERN, html);
        count += countMatches(PHONE_PATTERN, html);
        count += countMatches(TOKEN_PATTERN, html);
        count += countMatches(CREDIT_CARD_PATTERN, html);
        return count;
    }

    private static int countMatches(Pattern pattern, String text) {
        return (int) pattern.matcher(text).results().count();
    }
}