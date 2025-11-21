package com.vtnet.netat.db.config;

/**
 * Database profile configuration.
 * Can be loaded from JSON file or created programmatically.
 */
public class DatabaseProfile {

    private final String name;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final int poolSize;
    private final long connectionTimeout;

    private DatabaseProfile(Builder builder) {
        this.name = builder.name;
        this.jdbcUrl = builder.jdbcUrl;
        this.username = builder.username;
        this.password = builder.password;
        this.driverClassName = builder.driverClassName;
        this.poolSize = builder.poolSize;
        this.connectionTimeout = builder.connectionTimeout;
    }

    // Getters
    public String getName() { return name; }
    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDriverClassName() { return driverClassName; }
    public int getPoolSize() { return poolSize; }
    public long getConnectionTimeout() { return connectionTimeout; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String jdbcUrl;
        private String username;
        private String password;
        private String driverClassName;
        private int poolSize = 10;
        private long connectionTimeout = 30000;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public Builder connectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public DatabaseProfile build() {
            if (name == null || jdbcUrl == null || username == null) {
                throw new IllegalStateException(
                        "name, jdbcUrl, and username are required"
                );
            }
            return new DatabaseProfile(this);
        }
    }


}