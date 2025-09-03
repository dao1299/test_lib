package com.vtnet.netat.core.data;

import java.util.List;
import java.util.Map;

public interface IDataReader {
    List<Map<String, String>> readData(String filePath);
    List<Map<String, String>> readData(String filePath, String... options);
}
