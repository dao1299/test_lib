package com.vtnet.netat.db.connection;

import com.vtnet.netat.db.config.DatabaseProfile;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private static final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    public static void createConnectionPool(DatabaseProfile profile) {
        if (dataSources.containsKey(profile.getProfileName())) return;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(profile.getJdbcUrl());
        config.setUsername(profile.getUsername());
        config.setPassword(profile.getPassword());
        config.setMaximumPoolSize(profile.getPoolSize());
        dataSources.put(profile.getProfileName(), new HikariDataSource(config));
    }

    public static Connection getConnection(String profileName) throws SQLException {
        HikariDataSource ds = dataSources.get(profileName);
        if (ds == null) throw new SQLException("Pool cho profile '" + profileName + "' chưa được khởi tạo.");
        return ds.getConnection();
    }

    public static void closeAll() {
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
    }
}
