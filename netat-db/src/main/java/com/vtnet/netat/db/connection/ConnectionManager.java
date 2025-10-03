package com.vtnet.netat.db.connection;

import com.vtnet.netat.core.keywords.UtilityKeyword;
import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.db.config.DatabaseProfile;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private static final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private static final NetatLogger logger = NetatLogger.getInstance(ConnectionManager.class);

    public static void createConnectionPool(DatabaseProfile profile) {
        if (dataSources.containsKey(profile.getProfileName())) return;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(profile.getJdbcUrl());
        config.setUsername(profile.getUsername());
        config.setPassword(profile.getPassword());
        config.setMaximumPoolSize(profile.getPoolSize());
        logger.info("Creating connection pool for profile '{}' with JDBC URL: {} and pool size: {}", profile.getProfileName(), profile.getJdbcUrl(), profile.getPoolSize());
        dataSources.put(profile.getProfileName(), new HikariDataSource(config));
    }

    public static Connection getConnection(String profileName) throws SQLException {
        HikariDataSource ds = dataSources.get(profileName);
        if (ds == null) throw new SQLException("Pool cho profile '" + profileName + "' with JDBC URL: not found. Please create pool before getting connection.");
        return ds.getConnection();
    }

    public static void closeAll() {
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
    }
}
