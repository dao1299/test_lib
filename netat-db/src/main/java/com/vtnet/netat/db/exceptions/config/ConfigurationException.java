package com.vtnet.netat.db.exceptions.config;

import com.vtnet.netat.db.exceptions.DatabaseException;
import com.vtnet.netat.db.exceptions.ErrorSeverity;

/**
 * Base exception for database configuration errors.
 * Thrown when there are issues with database profiles, connection strings,
 * or other configuration problems.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class ConfigurationException extends DatabaseException {

    private static final long serialVersionUID = 1L;

    protected ConfigurationException(Builder builder) {
        super(builder);
    }

    @Override
    public String getSuggestion() {
        return "Review database configuration settings and profiles.";
    }

    /**
     * Creates a new builder for ConfigurationException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ConfigurationException.
     */
    public static class Builder extends DatabaseException.Builder<Builder> {

        public Builder() {
            severity(ErrorSeverity.ERROR);
            retryable(false); // Configuration errors need manual fix
        }

        @Override
        public ConfigurationException build() {
            return new ConfigurationException(this);
        }
    }
}