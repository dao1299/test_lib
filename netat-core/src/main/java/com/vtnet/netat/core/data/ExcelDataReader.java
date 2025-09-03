package com.vtnet.netat.core.data;

import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelDataReader { // Không cần implement IDataReader nữa để linh hoạt hơn

    public List<Map<String, String>> readData(String filePath, String sheetName, boolean containsHeaders) {
        List<Map<String, String>> data = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(file)) {

            Sheet sheet = (sheetName != null && !sheetName.isEmpty()) ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);
            if (sheet == null) throw new RuntimeException("Không tìm thấy sheet trong file: " + filePath);

            List<String> headers = new ArrayList<>();
            int firstDataRowIndex = 0;

            if (containsHeaders) {
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new RuntimeException("File được cấu hình có header nhưng không tìm thấy hàng header.");
                for (Cell cell : headerRow) {
                    headers.add(getCellValue(cell));
                }
                firstDataRowIndex = 1; // Dữ liệu bắt đầu từ hàng thứ hai
            } else {
                // Tự động sinh header nếu không có
                int columnCount = sheet.getRow(0).getLastCellNum();
                for (int i = 0; i < columnCount; i++) {
                    headers.add("Column " + (i + 1));
                }
                firstDataRowIndex = 0; // Dữ liệu bắt đầu ngay từ hàng đầu tiên
            }

            for (int i = firstDataRowIndex; i <= sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);
                if (currentRow == null) continue;
                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = currentRow.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    rowData.put(headers.get(j), getCellValue(cell));
                }
                data.add(rowData);
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file Excel: " + filePath, e);
        }
        return data;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }
}