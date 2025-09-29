package com.vtnet.netat.core.utils;

import com.vtnet.netat.core.logging.NetatLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lớp Singleton quản lý toàn bộ cấu hình cho việc thực thi kiểm thử.
 * Nó nạp cấu hình theo một kiến trúc phân lớp có thứ tự ưu tiên rõ ràng.
 */
public final class ConfigurationManager {
    private static final Properties properties = new Properties();
    private static final NetatLogger logger = NetatLogger.getInstance(ConfigurationManager.class);
    static {
        loadPropertiesFromFile("src/main/resources/config/default.properties");
        String env = System.getProperty("env", "test");
        loadPropertiesFromFile("src/main/resources/config/config." + env + ".properties");
        properties.putAll(System.getProperties());
    }

    /**
     * Phương thức nội bộ để đọc một file .properties và hợp nhất vào đối tượng properties chính.
     * @param filePath Đường dẫn đến file properties.
     */
    private static void loadPropertiesFromFile(String filePath) {
        try (InputStream input = new FileInputStream(filePath)) {
            logger.info("Loading configuration from file: {}", filePath);
            properties.load(input);
        } catch (IOException ex) {
            logger.info("Configuration file not found or could not be read: " + filePath);
        }
    }


    public static String getProperty(String key) {
        logger.debug("Getting property '{}'", key);
        return properties.getProperty(key);
    }


    public static String getProperty(String key, String defaultValue) {
        logger.debug("Getting property '{}' with default value '{}'", key, defaultValue);
        return properties.getProperty(key, defaultValue);
    }

    // Constructor private để đảm bảo đây là lớp Singleton
    private ConfigurationManager() {}
}