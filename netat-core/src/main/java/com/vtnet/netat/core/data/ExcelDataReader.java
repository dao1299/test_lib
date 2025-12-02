package com.vtnet.netat.core.data;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelDataReader {

    public List<Map<String, String>> readData(String filePath, String sheetName, boolean containsHeaders) {
        List<Map<String, String>> data = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(file)) {

            Sheet sheet = (sheetName != null && !sheetName.isEmpty()) ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);
            if (sheet == null) throw new RuntimeException("Sheet not found in file: " + filePath);

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            List<String> headers = new ArrayList<>();
            int firstDataRowIndex = 0;

            if (containsHeaders) {
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) throw new RuntimeException("File is configured to have headers but header row is missing.");
                for (Cell cell : headerRow) {
                    headers.add(getCellValue(cell, evaluator, sheet));
                }
                firstDataRowIndex = 1;
            } else {
                int columnCount = sheet.getRow(0).getLastCellNum();
                for (int i = 0; i < columnCount; i++) {
                    headers.add("Column " + (i + 1));
                }
                firstDataRowIndex = 0;
            }

            for (int i = firstDataRowIndex; i <= sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);
                if (currentRow == null) continue;
                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = currentRow.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    rowData.put(headers.get(j), getCellValue(cell, evaluator, sheet));
                }
                data.add(rowData);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read Excel file: " + filePath, e);
        }
        return data;
    }

    private String getCellValue(Cell cell, FormulaEvaluator evaluator, Sheet sheet) {
        // Nếu cell là null, kiểm tra xem vị trí này có nằm trong merge region không
        Cell effectiveCell = (cell != null) ? getMergedCell(cell, sheet) : null;

        if (effectiveCell == null) return "";

        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(effectiveCell, evaluator);
    }

    private Cell getMergedCell(Cell cell, Sheet sheet) {
        if (cell == null) return null;

        int rowIndex = cell.getRowIndex();
        int columnIndex = cell.getColumnIndex();

        for (CellRangeAddress mergedRegion : sheet.getMergedRegions()) {
            if (mergedRegion.isInRange(rowIndex, columnIndex)) {
                Row firstRow = sheet.getRow(mergedRegion.getFirstRow());
                if (firstRow != null) {
                    Cell firstCell = firstRow.getCell(mergedRegion.getFirstColumn());
                    return firstCell;
                }
            }
        }

        return cell;
    }
}