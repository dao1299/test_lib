package com.vtnet.netat.db.utils;

/**
 * Lớp tiện ích để xây dựng các chuỗi JDBC Connection URL từ các thành phần riêng lẻ.
 */
public final class JdbcUrlBuilder {

    private JdbcUrlBuilder() {} // Ngăn không cho tạo instance

    /**
     * Xây dựng một chuỗi JDBC URL hoàn chỉnh.
     * @param dbType Loại cơ sở dữ liệu (ví dụ: "postgresql", "mariadb", "sqlserver").
     * @param host   Địa chỉ IP hoặc hostname.
     * @param port   Cổng kết nối.
     * @param databaseName Tên cơ sở dữ liệu (hoặc SID/Service Name cho Oracle).
     * @return Một chuỗi JDBC URL hợp lệ.
     */
    public static String buildUrl(String dbType, String host, int port, String databaseName) {
        switch (dbType.toLowerCase()) {
            case "mariadb":
                return String.format("jdbc:mariadb://%s:%d/%s", host, port, databaseName);
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s", host, port, databaseName);
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, databaseName);
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
            case "clickhouse":
                return String.format("jdbc:clickhouse://%s:%d/%s", host, port, databaseName);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType + ". Supported types: mariadb, mysql, postgresql, sqlserver, oracle, clickhouse.");
        }
    }
}