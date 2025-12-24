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

    private final DataFormatter formatter = new DataFormatter();

    public List<Map<String, String>> readData(String filePath, String sheetName, boolean containsHeaders) {
        List<Map<String, String>> data = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(file)) {

            Sheet sheet = (sheetName != null && !sheetName.isEmpty())
                    ? workbook.getSheet(sheetName)
                    : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new RuntimeException("Sheet not found in file: " + filePath);
            }

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            List<String> headers = new ArrayList<>();
            int firstDataRowIndex;

            if (containsHeaders) {
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    throw new RuntimeException("File is configured to have headers but header row is missing.");
                }
                for (Cell cell : headerRow) {
                    headers.add(getCellValue(cell, evaluator, sheet));
                }
                firstDataRowIndex = 1;
            } else {
                Row firstRow = sheet.getRow(0);
                if (firstRow == null) {
                    throw new RuntimeException("File has no data rows.");
                }
                int columnCount = firstRow.getLastCellNum();
                for (int i = 0; i < columnCount; i++) {
                    headers.add("Column " + (i + 1));
                }
                firstDataRowIndex = 0;
            }

            for (int i = firstDataRowIndex; i <= sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);

                if (isRowCompletelyEmpty(currentRow, headers.size())) {
                    break;
                }

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

    private boolean isRowCompletelyEmpty(Row row, int columnCount) {
        if (row == null) return true;

        for (int i = 0; i < columnCount; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = cell.toString().trim();
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellValue(Cell cell, FormulaEvaluator evaluator, Sheet sheet) {
        Cell effectiveCell = (cell != null) ? getMergedCell(cell, sheet) : null;
        if (effectiveCell == null) return "";
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
                    return firstRow.getCell(mergedRegion.getFirstColumn());
                }
            }
        }

        return cell;
    }
}