package com.vtnet.netat.db.logging.masking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to mask sensitive data in logs.
 */
public final class SensitiveDataMasker {

    private static final String MASK = "***";

    // Sensitive column names
    private static final Set<String> SENSITIVE_COLUMNS = new HashSet<>(Arrays.asList(
            "password", "pwd", "passwd", "secret", "token", "api_key", "apikey",
            "credit_card", "creditcard", "ssn", "social_security", "cvv", "pin",
            "private_key", "secret_key", "access_token", "refresh_token"
    ));

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
    );

    // Credit card pattern (simple version)
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?(\\d{4})\\b"
    );

    // Phone pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"
    );

    private SensitiveDataMasker() {
        // Utility class
    }

    /**
     * Masks sensitive data in a string.
     */
    public static String maskString(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String masked = value;

        // Mask emails
        masked = maskEmails(masked);

        // Mask credit cards
        masked = maskCreditCards(masked);

        // Mask phones
        masked = maskPhones(masked);

        return masked;
    }

    /**
     * Masks an email address (keep first char and domain).
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (matcher.find()) {
            String username = matcher.group(1);
            String domain = matcher.group(2);

            if (username.length() <= 1) {
                return MASK + "@" + domain;
            }

            return username.charAt(0) + MASK + "@" + domain;
        }

        return email;
    }

    /**
     * Masks all emails in a string.
     */
    private static String maskEmails(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String masked = maskEmail(matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Masks credit card numbers (show only last 4 digits).
     */
    private static String maskCreditCards(String text) {
        Matcher matcher = CREDIT_CARD_PATTERN.matcher(text);
        return matcher.replaceAll("****$1");
    }

    /**
     * Masks phone numbers.
     */
    private static String maskPhones(String text) {
        return PHONE_PATTERN.matcher(text).replaceAll("***-***-****");
    }

    /**
     * Checks if a column name is sensitive.
     */
    public static boolean isSensitiveColumn(String columnName) {
        if (columnName == null) {
            return false;
        }
        return SENSITIVE_COLUMNS.contains(columnName.toLowerCase());
    }

    /**
     * Masks a value if the column is sensitive.
     */
    public static String maskIfSensitive(String columnName, String value) {
        if (isSensitiveColumn(columnName)) {
            return MASK;
        }
        return value;
    }

    /**
     * Masks query parameters.
     */
    public static Object[] maskParameters(String query, Object[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return parameters;
        }

        // Check if query contains sensitive keywords
        boolean hasSensitiveKeyword = false;
        String lowerQuery = query != null ? query.toLowerCase() : "";

        for (String sensitive : SENSITIVE_COLUMNS) {
            if (lowerQuery.contains(sensitive)) {
                hasSensitiveKeyword = true;
                break;
            }
        }

        Object[] masked = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
                masked[i] = null;
            } else if (hasSensitiveKeyword && parameters[i] instanceof String) {
                // If query has sensitive keywords, mask string parameters
                masked[i] = MASK;
            } else if (parameters[i] instanceof String) {
                // Mask sensitive patterns in strings
                masked[i] = maskString((String) parameters[i]);
            } else {
                masked[i] = parameters[i];
            }
        }

        return masked;
    }
}