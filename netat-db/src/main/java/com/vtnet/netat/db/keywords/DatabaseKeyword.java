package com.vtnet.netat.db.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.utils.DataFileHelper;
import com.vtnet.netat.db.connection.ConnectionManager;
import com.vtnet.netat.db.config.DatabaseProfile;
import com.vtnet.netat.db.utils.DatabaseHelper;
import com.vtnet.netat.db.utils.QueryHelper;
import io.qameta.allure.Step;
import org.testng.Assert;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cung cấp một bộ các keyword để tương tác và kiểm thử cơ sở dữ liệu.
 * Kế thừa từ BaseKeyword để có các tính năng logging, retry, và reporting.
 */
public class DatabaseKeyword extends BaseKeyword {

    // =================================================================================================
    // KEYWORDS QUẢN LÝ KẾT NỐI
    // =================================================================================================

    @NetatKeyword(name = "connectDatabase", description = "Khởi tạo một connection pool dựa trên file profile.", category = "DB")
    @Step("Khởi tạo kết nối CSDL từ profile: {0}")
    public void connectDatabase(String profilePath) {
        execute(() -> {
            DatabaseProfile profile = DatabaseHelper.getProfile(profilePath);
            ConnectionManager.createConnectionPool(profile);
            return null;
        }, profilePath);
    }

    @NetatKeyword(name = "disconnectAllDatabases", description = "Đóng tất cả các connection pool đang hoạt động.", category = "DB")
    @Step("Đóng tất cả các kết nối CSDL")
    public void disconnectAllDatabases() {
        execute(() -> {
            ConnectionManager.closeAll();
            return null;
        });
    }

    // =================================================================================================
    // KEYWORDS THỰC THI (PRO-CODE)
    // =================================================================================================

    @NetatKeyword(name = "executeQuery", description = "Thực thi câu lệnh SELECT và trả về kết quả.", category = "DB")
    @Step("Thực thi câu lệnh SELECT trên [{0}]: {1}")
    public List<Map<String, Object>> executeQuery(String profileName, String query, Object... params) {
        return execute(() -> executeQueryWithParams(profileName, query, params), profileName, query, params);
    }

    @NetatKeyword(name = "executeUpdate", description = "Thực thi câu lệnh INSERT, UPDATE, DELETE.", category = "DB")
    @Step("Thực thi câu lệnh UPDATE trên [{0}]: {1}")
    public int executeUpdate(String profileName, String query, Object... params) {
        return execute(() -> {
            try (Connection conn = ConnectionManager.getConnection(profileName);
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
                return pstmt.executeUpdate();
            }
        }, profileName, query, params);
    }

    // =================================================================================================
    // KEYWORDS KIỂM CHỨNG (PRO-CODE & LOW-CODE)
    // =================================================================================================

    @NetatKeyword(name = "verifyRecordExists", description = "Kiểm tra sự tồn tại của ít nhất một bản ghi.", category = "DB")
    @Step("Kiểm tra sự tồn tại của bản ghi trên [{0}] khớp với điều kiện: {1}")
    public void verifyRecordExists(String profileName, String query, boolean expectedExists, Object... params) {
        execute(() -> {
            List<Map<String, Object>> result = executeQueryWithParams(profileName, query, params);
            Assert.assertEquals(!result.isEmpty(), expectedExists, "Kiểm tra sự tồn tại của bản ghi thất bại.");
            return null;
        }, profileName, query, expectedExists, params);
    }

    @NetatKeyword(name = "verifyDataSQL", description = "Thực thi và kiểm chứng kết quả với dữ liệu mong đợi.", category = "DB")
    @Step("Kiểm chứng dữ liệu trên [{0}] bằng câu lệnh SQL trực tiếp")
    public void verifyDataSQL(String profileName, String query, String[] expectedColumnNames, Object[][] expectedData, Object... queryParams) {
        execute(() -> {
            List<Map<String, Object>> actualResult = executeQueryWithParams(profileName, query, queryParams);
            Assert.assertEquals(actualResult.size(), expectedData.length, "Lỗi kiểm chứng: Số lượng hàng trả về không khớp.");

            for (int i = 0; i < expectedData.length; i++) {
                Map<String, Object> actualRow = actualResult.get(i);
                Object[] expectedRow = expectedData[i];
                Assert.assertEquals(expectedRow.length, expectedColumnNames.length, "Lỗi cấu hình: Số cột trong dữ liệu mong đợi không khớp với số cột header ở hàng " + (i + 1));

                for (int j = 0; j < expectedColumnNames.length; j++) {
                    String columnName = expectedColumnNames[j];
                    Object expectedValue = expectedRow[j];
                    Assert.assertTrue(actualRow.containsKey(columnName), "Cột '" + columnName + "' không tồn tại trong kết quả truy vấn.");
                    Object actualValue = actualRow.get(columnName);
                    Assert.assertEquals(String.valueOf(actualValue), String.valueOf(expectedValue), "Lỗi kiểm chứng ở hàng " + (i + 1) + ", cột '" + columnName + "' không khớp.");
                }
            }
            return null;
        }, profileName, query, expectedColumnNames, expectedData, queryParams);
    }

    @NetatKeyword(name = "verifyDataByQueryFile", description = "Thực thi và kiểm chứng kết quả bằng file SQL và file dữ liệu.", category = "DB")
    @Step("Kiểm chứng dữ liệu trên [{0}] bằng file query [{1}] và file data [{2}]")
    public void verifyDataByQueryFile(String profileName, String queryPath, String dataFilePath) {
        execute(() -> {
            String baseQuery = QueryHelper.getQuery(queryPath);
            Object[][] data = DataFileHelper.getTestData(dataFilePath);
            Assert.assertTrue(data.length > 0, "File dữ liệu kiểm chứng không có dữ liệu: " + dataFilePath);
            Map<String, String> expectedDataMap = (Map<String, String>) data[0][0];

            List<Object> queryParams = new ArrayList<>();
            Map<String, Object> verificationData = new HashMap<>();
            for (Map.Entry<String, String> entry : expectedDataMap.entrySet()) {
                if (entry.getKey().startsWith("param_")) {
                    queryParams.add(entry.getValue());
                } else {
                    verificationData.put(entry.getKey(), entry.getValue());
                }
            }

            List<Map<String, Object>> result = executeQueryWithParams(profileName, baseQuery, queryParams.toArray());
            Assert.assertFalse(result.isEmpty(), "Câu lệnh SELECT không trả về bản ghi nào.");
            Map<String, Object> actualDataRow = result.get(0);

            for (Map.Entry<String, Object> entry : verificationData.entrySet()) {
                String columnName = entry.getKey();
                Object expectedValue = entry.getValue();
                Assert.assertTrue(actualDataRow.containsKey(columnName), "Cột '" + columnName + "' không tồn tại.");
                Assert.assertEquals(String.valueOf(actualDataRow.get(columnName)), String.valueOf(expectedValue));
            }
            return null;
        }, profileName, queryPath, dataFilePath);
    }

    // =================================================================================================
    // PHƯƠNG THỨC HỖ TRỢ (PRIVATE)
    // =================================================================================================

    private List<Map<String, Object>> executeQueryWithParams(String profileName, String query, Object... params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection(profileName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int columns = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>(columns);
                    for (int i = 1; i <= columns; ++i) {
                        row.put(md.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }
}