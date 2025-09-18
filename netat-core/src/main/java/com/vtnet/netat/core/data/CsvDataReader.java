package com.vtnet.netat.core.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvDataReader { // Không cần implement IDataReader nữa để linh hoạt hơn

    public List<Map<String, String>> readData(String filePath, String separator, boolean containsHeaders) {
        List<Map<String, String>> data = new ArrayList<>();
        String effectiveSeparator = (separator != null && !separator.isEmpty()) ? separator : ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            List<String> headers = new ArrayList<>();
            String firstLine = br.readLine();
            if (firstLine == null) {
                return data; // Trả về danh sách trống nếu file rỗng
            }

            if (containsHeaders) {
                headers.addAll(List.of(firstLine.split(effectiveSeparator)));
            } else {
                // Tự động sinh header nếu không có
                String[] firstLineData = firstLine.split(effectiveSeparator);
                for (int i = 0; i < firstLineData.length; i++) {
                    headers.add("Column " + (i + 1));
                }
                // Xử lý dòng đầu tiên như là dữ liệu
                processLine(firstLine, headers, effectiveSeparator, data);
            }

            // Đọc các dòng dữ liệu còn lại
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line, headers, effectiveSeparator, data);
            }

        } catch (IOException e) {
            throw new RuntimeException("Unable to read CSV file: " + filePath, e);
        }
        return data;
    }

    private void processLine(String line, List<String> headers, String separator, List<Map<String, String>> data) {
        String[] values = line.split(separator);
        Map<String, String> rowData = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            rowData.put(headers.get(i), i < values.length ? values[i] : "");
        }
        data.add(rowData);
    }
}