package com.vtnet.netat.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class DataFileHelper {
    private static final Logger log = LoggerFactory.getLogger(DataFileHelper.class);
    private static final String DATA_SOURCE_REPO_PATH = Paths.get(
            System.getProperty("user.dir"), "src", "test", "java","automationtest", "datafile").toString();

    private DataFileHelper() {}

    public static Object[][] getTestData(String relativeDataSourcePath) {
        String fullJsonPath = Paths.get(DATA_SOURCE_REPO_PATH, relativeDataSourcePath + ".json").toString();
        log.info("Đang đọc file định nghĩa nguồn dữ liệu tại: {}", fullJsonPath);
        try {
            String jsonConfig = new String(Files.readAllBytes(Paths.get(fullJsonPath)));
            return DataUtils.getTestDataFromJson(jsonConfig);
        } catch (Exception e) {
            log.error("Lỗi khi đọc hoặc xử lý file định nghĩa nguồn dữ liệu tại '{}'.", fullJsonPath, e);
            throw new RuntimeException("Không thể lấy dữ liệu test từ: " + relativeDataSourcePath, e);
        }
    }
}