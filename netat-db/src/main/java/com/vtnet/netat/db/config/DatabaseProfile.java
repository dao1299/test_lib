package com.vtnet.netat.db.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseProfile {
    private String profileName;
    private String jdbcUrl;
    private String username;
    private String password;
    private int poolSize = 5; // Mặc định

    // Getters and Setters...

    public DatabaseProfile(String profileName, String jdbcUrl, String username, String password, int poolSize) {
        this.profileName = profileName;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
    }

    public DatabaseProfile() {
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }


}