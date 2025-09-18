package com.vtnet.netat.db.utils;

import java.nio.file.Files;
import java.nio.file.Paths;

public final class QueryHelper {
    private static final String QUERY_REPO_PATH = Paths.get(
            System.getProperty("user.dir"), "src", "test", "resources", "sql_queries").toString();

    private QueryHelper() {}

    /**
     * Đọc nội dung của một file SQL từ kho lưu trữ.
     */
    public static String getQuery(String relativeQueryPath) {
        String fullPath = Paths.get(QUERY_REPO_PATH, relativeQueryPath + ".sql").toString();
        try {
            return new String(Files.readAllBytes(Paths.get(fullPath)));
        } catch (Exception e) {
            throw new RuntimeException("Unable to read query file: " + fullPath, e);
        }
    }
}