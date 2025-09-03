package com.vtnet.netat.core.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSource {
    private String name;
    private String driver;
    private String csvSeparator;
    private String sheetName;
    private String filePath;
    private boolean containsHeaders = true; // <-- THÊM TRƯỜNG MỚI (mặc định là true)

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDriver() { return driver; }
    public void setDriver(String driver) { this.driver = driver; }
    public String getCsvSeparator() { return csvSeparator; }
    public void setCsvSeparator(String csvSeparator) { this.csvSeparator = csvSeparator; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public boolean isContainsHeaders() { return containsHeaders; } // <-- THÊM GETTER/SETTER
    public void setContainsHeaders(boolean containsHeaders) { this.containsHeaders = containsHeaders; }

    public static DataSource fromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, DataSource.class);
    }
}