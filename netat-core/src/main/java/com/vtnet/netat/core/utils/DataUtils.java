package com.vtnet.netat.core.utils;

import com.vtnet.netat.core.data.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class DataUtils {

    private DataUtils() {}

    public static Object[][] getTestDataFromJson(String jsonConfig) {
        try {
            DataSource dataSource = DataSource.fromJson(jsonConfig);
            String filePath = dataSource.getFilePath();
            List<Map<String, String>> testData;

            // Không cần factory nữa, gọi trực tiếp
            if ("ExcelFile".equalsIgnoreCase(dataSource.getDriver())) {
                ExcelDataReader reader = new ExcelDataReader();
                testData = reader.readData(filePath, dataSource.getSheetName(), dataSource.isContainsHeaders());
            } else if ("CSVFile".equalsIgnoreCase(dataSource.getDriver())) {
                CsvDataReader reader = new CsvDataReader(); // Giả sử đã có CsvDataReader tương tự
                testData = reader.readData(filePath, dataSource.getCsvSeparator(), dataSource.isContainsHeaders());
            } else {
                throw new IllegalArgumentException("Unsupported driver: " + dataSource.getDriver());
            }

            return convertToObjectArray(testData);
        } catch (IOException e) {
            throw new RuntimeException("Unable to process JSON configuration for test data.", e);
        }
    }

    private static Object[][] convertToObjectArray(List<Map<String, String>> testData) {
        if (testData == null || testData.isEmpty()) {
            return new Object[0][0];
        }
        Object[][] data = new Object[testData.size()][1];
        for (int i = 0; i < testData.size(); i++) {
            data[i][0] = testData.get(i);
        }
        return data;
    }
}