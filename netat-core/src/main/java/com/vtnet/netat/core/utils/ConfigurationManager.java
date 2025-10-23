package com.vtnet.netat.core.utils;

import com.vtnet.netat.core.logging.NetatLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lớp Singleton quản lý toàn bộ cấu hình cho việc thực thi kiểm thử.
 * Nó nạp cấu hình theo một kiến trúc phân lớp có thứ tự ưu tiên rõ ràng.
 *
 * THỨ TỰ ƯU TIÊN (TỪ CAO ĐẾN THẤP):
 * 1. System Properties (cung cấp qua tham số -D khi chạy, ví dụ: -Denv=staging -Dapp.url=...)
 * 2. File cấu hình theo môi trường (ví dụ: config/config.staging.properties)
 * 3. File cấu hình mặc định (config/default.properties)
 */
public final class ConfigurationManager {
    private static final Properties properties = new Properties();
    private static final NetatLogger logger = NetatLogger.getInstance(ConfigurationManager.class);

    // Đổi tên thư mục/tiền tố file cho rõ ràng hơn
    private static final String CONFIG_DIR = "config/";
    private static final String DEFAULT_CONFIG_FILE = "default.properties";
    private static final String ENV_CONFIG_PREFIX = "config.";
    private static final String ENV_CONFIG_SUFFIX = ".properties";

    static {
        // 1. Load cấu hình MẶC ĐỊNH (default.properties) từ classpath
        // Đây là cấu hình nền, có độ ưu tiên thấp nhất
        loadPropertiesFromClasspath(CONFIG_DIR + DEFAULT_CONFIG_FILE);

        // 2. Load cấu hình MÔI TRƯỜNG (config.{env}.properties)
        // Lấy biến môi trường 'env' từ System Property, mặc định là "local"
        // (Đổi "test" thành "local" cho trực quan hơn)
        String env = System.getProperty("env", "local");
        String envConfigFile = CONFIG_DIR + ENV_CONFIG_PREFIX + env + ENV_CONFIG_SUFFIX;
        loadPropertiesFromClasspath(envConfigFile);

        // 3. KHÔNG load System.getProperties() vào 'properties' nữa.
        // Việc này sẽ được xử lý trong hàm getProperty() để đảm bảo đúng thứ tự ưu tiên.
    }

    /**
     * Phương thức nội bộ để đọc một file .properties từ CLASSPATH và hợp nhất.
     * @param resourcePath Đường dẫn tài nguyên trong classpath (ví dụ: "config/default.properties").
     */
    private static void loadPropertiesFromClasspath(String resourcePath) {

        try (InputStream input = ConfigurationManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                logger.info("Configuration file not found in classpath (skip): " + resourcePath);
                return;
            }
            logger.info("Loading configuration from classpath: {}", resourcePath);

            properties.load(input);
        } catch (IOException ex) {
            logger.warn("Could not read configuration file: " + resourcePath, ex);
        }
    }


    public static String getProperty(String key) {

        String value = System.getProperty(key);
        if (value != null) {
            logger.debug("Getting property '{}' from System Properties (Override)", key);
            return value;
        }

        value = properties.getProperty(key);
        if (value == null) {
            logger.debug("Property '{}' not found in any configuration.", key);
            return null;
        }

        logger.debug("Getting property '{}' from config files", key);

        if (value.startsWith("ENC(") && value.endsWith(")")) {
            logger.debug("Decrypting value for key '{}'", key);
            String encryptedValue = value.substring(4, value.length() - 1);
            // Code mã hoá dữ liệu nhạy cảm
            logger.warn("Property '{}' is encrypted, but no decryption logic is implemented. Returning raw value.", key);
            return encryptedValue;
        }

        return value;
    }

    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return (value != null) ? value : defaultValue;
    }

    private ConfigurationManager() {}
}