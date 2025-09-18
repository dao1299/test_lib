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
                    throw new IOException("'config/default.properties' not found on the classpath.");
                }
                properties.load(defaultConfigStream);
                log.info("Loaded default configuration (default.properties).");
            } catch (IOException e) {
                throw new RuntimeException("Unable to read default configuration file.", e);
            }

            String profile = System.getProperty("profile");
            if (profile != null && !profile.trim().isEmpty()) {
                String profileFileName = "config/" + profile.trim() + ".properties";
                try (InputStream profileConfigStream = getResourceAsStream(profileFileName)) {
                    if (profileConfigStream != null) {
                        Properties profileProps = new Properties();
                        profileProps.load(profileConfigStream);
                        properties.putAll(profileProps); // Ghi đè lên default
                        log.info("Loaded and overrode configuration from profile: {}", profile);
                    } else {
                        log.warn("Profile '{}' specified but file {} was not found.", profile, profileFileName);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read configuration file for profile: " + profile, e);
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

    public static String getProperty(String key,String defaultValue) {
        if (!isLoaded) {
            loadProperties();
        }
        return properties.getProperty(key) != null ? properties.getProperty(key) : defaultValue;
    }

    public static Properties getProperties() {
        if (!isLoaded) {
            loadProperties();
        }
        return properties;
    }
}