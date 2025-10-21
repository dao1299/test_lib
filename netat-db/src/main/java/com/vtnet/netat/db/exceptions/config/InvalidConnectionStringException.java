package com.vtnet.netat.db.exceptions.config;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when JDBC URL format is invalid.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class InvalidConnectionStringException extends ConfigurationException {

    private static final long serialVersionUID = 1L;

    // Standard JDBC URL patterns for different databases
    private static final Map<String, String> URL_PATTERNS = new HashMap<>();

    static {
        URL_PATTERNS.put("mysql", "jdbc:mysql://host:port/database");
        URL_PATTERNS.put("mariadb", "jdbc:mariadb://host:port/database");
        URL_PATTERNS.put("postgresql", "jdbc:postgresql://host:port/database");
        URL_PATTERNS.put("sqlserver", "jdbc:sqlserver://host:port;databaseName=database");
        URL_PATTERNS.put("oracle", "jdbc:oracle:thin:@host:port:sid");
        URL_PATTERNS.put("h2", "jdbc:h2:mem:database or jdbc:h2:file:/path/to/db");
    }

    private final String providedUrl;
    private final String detectedDbType;

    protected InvalidConnectionStringException(Builder builder) {
        super(builder);
        this.providedUrl = builder.providedUrl;
        this.detectedDbType = builder.detectedDbType;
    }

    /**
     * Creates a new builder for InvalidConnectionStringException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the invalid URL that was provided.
     *
     * @return provided URL
     */
    public String getProvidedUrl() {
        return providedUrl;
    }

    /**
     * Gets the detected database type (if any).
     *
     * @return detected DB type, or null if unknown
     */
    public String getDetectedDbType() {
        return detectedDbType;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("Invalid JDBC connection string format.\n\n");

        if (providedUrl != null) {
            suggestion.append(String.format("Provided URL: %s\n\n", providedUrl));
        }

        if (detectedDbType != null && URL_PATTERNS.containsKey(detectedDbType.toLowerCase())) {
            suggestion.append(String.format("Expected format for %s:\n", detectedDbType));
            suggestion.append(String.format("  %s\n\n", URL_PATTERNS.get(detectedDbType.toLowerCase())));

            suggestion.append("Example:\n");
            suggestion.append(getExampleUrl(detectedDbType)).append("\n\n");
        } else {
            suggestion.append("Common JDBC URL formats:\n\n");
            URL_PATTERNS.forEach((db, pattern) ->
                    suggestion.append(String.format("  %s:\n    %s\n\n",
                            db.toUpperCase(), pattern)));
        }

        suggestion.append("Common Issues:\n");
        suggestion.append("1. Missing 'jdbc:' prefix\n");
        suggestion.append("2. Incorrect database type (e.g., 'mysql' vs 'mariadb')\n");
        suggestion.append("3. Wrong port number format\n");
        suggestion.append("4. Missing or extra slashes\n");
        suggestion.append("5. Special characters not URL-encoded\n\n");

        suggestion.append("Checklist:\n");
        suggestion.append("✓ Starts with 'jdbc:'\n");
        suggestion.append("✓ Database type is correct\n");
        suggestion.append("✓ Host and port are specified\n");
        suggestion.append("✓ Database name is included\n");
        suggestion.append("✓ Special characters are escaped\n");

        return suggestion.toString();
    }

    /**
     * Gets an example URL for the given database type.
     *
     * @param dbType database type
     * @return example URL
     */
    private String getExampleUrl(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "mariadb":
                return "jdbc:mysql://localhost:3306/testdb?useSSL=false";
            case "postgresql":
                return "jdbc:postgresql://localhost:5432/testdb";
            case "sqlserver":
                return "jdbc:sqlserver://localhost:1433;databaseName=testdb";
            case "oracle":
                return "jdbc:oracle:thin:@localhost:1521:orcl";
            case "h2":
                return "jdbc:h2:mem:testdb";
            default:
                return "jdbc:database://host:port/dbname";
        }
    }

    /**
     * Builder for InvalidConnectionStringException.
     */
    public static class Builder extends ConfigurationException.Builder {
        private String providedUrl;
        private String detectedDbType;

        public Builder() {
            severity(ErrorSeverity.ERROR);
        }

        public Builder providedUrl(String providedUrl) {
            this.providedUrl = providedUrl;

            // Try to detect database type from URL
            if (providedUrl != null) {
                detectDbType(providedUrl);
            }

            return this;
        }

        public Builder detectedDbType(String detectedDbType) {
            this.detectedDbType = detectedDbType;
            if (detectedDbType != null) {
                addContext("detectedDbType", detectedDbType);
            }
            return this;
        }

        /**
         * Attempts to detect database type from URL.
         *
         * @param url the JDBC URL
         */
        private void detectDbType(String url) {
            if (url == null || !url.startsWith("jdbc:")) {
                return;
            }

            String lowerUrl = url.toLowerCase();
            for (String dbType : URL_PATTERNS.keySet()) {
                if (lowerUrl.contains("jdbc:" + dbType)) {
                    this.detectedDbType = dbType;
                    break;
                }
            }
        }

        @Override
        public InvalidConnectionStringException build() {
            message("Invalid JDBC connection string format");
            return new InvalidConnectionStringException(this);
        }
    }
}