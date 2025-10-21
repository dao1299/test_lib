package com.vtnet.netat.db.connection;

import com.vtnet.netat.db.config.DatabaseProfile;
import com.vtnet.netat.db.exceptions.connection.ConnectionException;
import com.vtnet.netat.db.logging.DatabaseLogger;
import com.vtnet.netat.db.logging.model.PoolStats;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private static final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private static final DatabaseLogger logger = DatabaseLogger.getInstance();

    public static void createConnectionPool(DatabaseProfile profile) {
        if (dataSources.containsKey(profile.getProfileName())) {
            return;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(profile.getJdbcUrl());
        config.setUsername(profile.getUsername());
        config.setPassword(profile.getPassword());
        config.setMaximumPoolSize(profile.getPoolSize());

        logger.logConnectionOpen(profile.getProfileName(), profile.getJdbcUrl());

        dataSources.put(profile.getProfileName(), new HikariDataSource(config));
    }

    public static Connection getConnection(String profileName) throws SQLException {
        HikariDataSource ds = dataSources.get(profileName);
        if (ds == null) {
            throw new SQLException("Pool for profile '" + profileName + "' not found");
        }

        long startTime = System.currentTimeMillis();

        try {
            Connection conn = ds.getConnection();

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 1000) {
                logger.logConnectionTimeout(profileName, 30000, duration);
            }

            return conn;

        } catch (SQLException e) {
            // ✅ Log connection timeout
            long duration = System.currentTimeMillis() - startTime;
            logger.logConnectionTimeout(profileName, 30000, duration);
            throw e;
        }
    }

    public static PoolStats getPoolStats(String profileName) {
        HikariDataSource ds = dataSources.get(profileName);
        if (ds == null) {
            return null;
        }

        HikariPoolMXBean poolBean = ds.getHikariPoolMXBean();

        return new PoolStats(
                ds.getMaximumPoolSize(),
                poolBean.getActiveConnections(),
                poolBean.getIdleConnections(),
                poolBean.getThreadsAwaitingConnection()
        );
    }

    /**
     * Logs pool statistics.
     */
    public static void logPoolStats(String profileName) {
        PoolStats stats = getPoolStats(profileName);
        if (stats != null) {
            logger.logConnectionPoolStats(profileName, stats);
        }
    }

    public static void closeAll() {
        dataSources.forEach((profileName, ds) -> {
            logger.logConnectionClose(profileName, 0);
            ds.close();
        });
        dataSources.clear();
    }
}