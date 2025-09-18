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
        log.info("Reading data source definition file at: {}", fullJsonPath);
        try {
            String jsonConfig = new String(Files.readAllBytes(Paths.get(fullJsonPath)));
            return DataUtils.getTestDataFromJson(jsonConfig);
        } catch (Exception e) {
            log.error("Critical error reading or processing data source definition file at '{}'.", fullJsonPath, e);
            throw new RuntimeException("Cannot retrieve test data from: " + relativeDataSourcePath, e);
        }
    }
}