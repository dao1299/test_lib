package com.vtnet.netat.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {
    private static final Logger log = LoggerFactory.getLogger(ConfigReader.class);
    private static final Properties properties = new Properties();
    private static boolean isLoaded = false;

    private ConfigReader() {}

    public static synchronized void loadProperties() {
        if (!isLoaded) {
            // Thay đổi cốt lõi nằm ở đây: sử dụng getResourceAsStream
            // để đọc file từ classpath, thay vì FileInputStream.
            try (InputStream defaultConfigStream = getResourceAsStream("config/default.properties")) {
                if (defaultConfigStream == null) {
                    // Nếu người dùng không cung cấp file default, có thể bỏ qua hoặc báo lỗi.
                    // Trong trường hợp này, chúng ta báo lỗi để đảm bảo tính toàn vẹn.
                    throw new IOException("Không tìm thấy file 'config/default.properties' trên classpath.");
                }
                properties.load(defaultConfigStream);
                log.info("Đã tải cấu hình mặc định (default.properties).");
            } catch (IOException e) {
                throw new RuntimeException("Không thể đọc file cấu hình mặc định.", e);
            }

            String profile = System.getProperty("profile");
            if (profile != null && !profile.trim().isEmpty()) {
                String profileFileName = "config/" + profile.trim() + ".properties";
                try (InputStream profileConfigStream = getResourceAsStream(profileFileName)) {
                    if (profileConfigStream != null) {
                        Properties profileProps = new Properties();
                        profileProps.load(profileConfigStream);
                        properties.putAll(profileProps); // Ghi đè lên default
                        log.info("Đã tải và ghi đè cấu hình từ profile: {}", profile);
                    } else {
                        log.warn("Đã chỉ định profile '{}' nhưng không tìm thấy file {}.", profile, profileFileName);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Không thể đọc file cấu hình của profile: " + profile, e);
                }
            }
            isLoaded = true;
        }
    }

    private static InputStream getResourceAsStream(String resourceName) {
        // Lấy class loader của luồng hiện tại để tìm kiếm tài nguyên
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
    }

    public static String getProperty(String key) {
        if (!isLoaded) {
            loadProperties();
        }
        return properties.getProperty(key);
    }

    public static Properties getProperties() {
        if (!isLoaded) {
            loadProperties();
        }
        return properties;
    }
}