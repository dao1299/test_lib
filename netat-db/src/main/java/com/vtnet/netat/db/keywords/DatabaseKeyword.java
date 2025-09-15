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

    @NetatKeyword(
            name = "connectDatabase",
            description = "Khởi tạo một connection pool dựa trên file profile. File profile chứa các thông tin cấu hình kết nối như URL, username, password, driver class, và các thuộc tính kết nối khác.",
            category = "DB/Connection",
            parameters = {"String: profilePath - Đường dẫn đến file profile chứa thông tin cấu hình kết nối CSDL."},
            returnValue = "void: Không trả về giá trị",
            example = "// Khởi tạo kết nối đến cơ sở dữ liệu từ file cấu hình\n" +
                    "databaseKeyword.connectDatabase(\"profiles/mysql_dev.properties\");",
            prerequisites = {"File profile phải tồn tại và chứa thông tin cấu hình hợp lệ"},
            exceptions = {"SQLException: Nếu không thể thiết lập kết nối đến CSDL",
                    "FileNotFoundException: Nếu không tìm thấy file profile",
                    "ConfigurationException: Nếu thông tin cấu hình trong profile không hợp lệ"},
            platform = "ALL",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"database", "connection", "setup"}
    )
    @Step("Khởi tạo kết nối CSDL từ profile: {0}")
    public void connectDatabase(String profilePath) {
        execute(() -> {
            DatabaseProfile profile = DatabaseHelper.getProfile(profilePath);
            ConnectionManager.createConnectionPool(profile);
            return null;
        }, profilePath);
    }

    @NetatKeyword(
            name = "disconnectAllDatabases",
            description = "Đóng tất cả các connection pool đang hoạt động và giải phóng tài nguyên. Nên gọi phương thức này ở cuối mỗi test case hoặc test suite để đảm bảo tất cả kết nối được đóng đúng cách.",
            category = "DB/Connection",
            parameters = {},
            returnValue = "void: Không trả về giá trị",
            example = "// Đóng tất cả kết nối CSDL sau khi hoàn thành test\n" +
                    "databaseKeyword.disconnectAllDatabases();",
            prerequisites = {"Đã khởi tạo ít nhất một kết nối CSDL trước đó"},
            exceptions = {"SQLException: Nếu có lỗi khi đóng kết nối"},
            platform = "ALL",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"database", "connection", "cleanup"}
    )
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

    @NetatKeyword(
            name = "executeQuery",
            description = "Thực thi câu lệnh SELECT và trả về kết quả dưới dạng danh sách các bản ghi. Mỗi bản ghi là một Map với key là tên cột và value là giá trị của cột đó. Hỗ trợ truyền tham số vào câu truy vấn để tránh SQL injection.",
            category = "DB/Execution",
            parameters = {
                    "String: profileName - Tên của profile kết nối CSDL đã được khởi tạo trước đó.",
                    "String: query - Câu lệnh SQL SELECT cần thực thi, có thể chứa các placeholder '?' cho tham số.",
                    "Object...: params - Các tham số cần truyền vào câu truy vấn, theo thứ tự xuất hiện của các placeholder '?'."
            },
            returnValue = "List<Map<String, Object>>: Danh sách các bản ghi, mỗi bản ghi là một Map với key là tên cột và value là giá trị",
            example = "// Thực thi câu lệnh SELECT với tham số\n" +
                    "List<Map<String, Object>> users = databaseKeyword.executeQuery(\n" +
                    "    \"mysql_dev\", \n" +
                    "    \"SELECT id, username, email FROM users WHERE status = ? AND created_date > ?\", \n" +
                    "    \"active\", \"2023-01-01\"\n" +
                    ");\n\n" +
                    "// Truy cập dữ liệu từ kết quả\n" +
                    "String username = users.get(0).get(\"username\").toString();",
            prerequisites = {"Đã khởi tạo kết nối CSDL với profileName tương ứng"},
            exceptions = {"SQLException: Nếu có lỗi khi thực thi câu truy vấn",
                    "IllegalArgumentException: Nếu profileName không tồn tại"},
            platform = "ALL",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"database", "query", "select"}
    )
    @Step("Thực thi câu lệnh SELECT trên [{0}]: {1}")
    public List<Map<String, Object>> executeQuery(String profileName, String query, Object... params) {
        return execute(() -> executeQueryWithParams(profileName, query, params), profileName, query, params);
    }

    @NetatKeyword(
            name = "executeUpdate",
            description = "Thực thi câu lệnh INSERT, UPDATE, DELETE và trả về số bản ghi bị ảnh hưởng. Hỗ trợ truyền tham số vào câu truy vấn để tránh SQL injection.",
            category = "DB/Execution",
            parameters = {
                    "String: profileName - Tên của profile kết nối CSDL đã được khởi tạo trước đó.",
                    "String: query - Câu lệnh SQL INSERT, UPDATE hoặc DELETE cần thực thi, có thể chứa các placeholder '?' cho tham số.",
                    "Object...: params - Các tham số cần truyền vào câu truy vấn, theo thứ tự xuất hiện của các placeholder '?'."
            },
            returnValue = "int: Số bản ghi bị ảnh hưởng bởi câu lệnh",
            example = "// Thêm một bản ghi mới vào bảng users\n" +
                    "int rowsAffected = databaseKeyword.executeUpdate(\n" +
                    "    \"mysql_dev\", \n" +
                    "    \"INSERT INTO users (username, email, status) VALUES (?, ?, ?)\", \n" +
                    "    \"john.doe\", \"john.doe@example.com\", \"active\"\n" +
                    ");\n\n" +
                    "// Kiểm tra xem có đúng một bản ghi được thêm vào không\n" +
                    "Assert.assertEquals(rowsAffected, 1);",
            prerequisites = {"Đã khởi tạo kết nối CSDL với profileName tương ứng"},
            exceptions = {"SQLException: Nếu có lỗi khi thực thi câu lệnh",
                    "IllegalArgumentException: Nếu profileName không tồn tại"},
            platform = "ALL",
            systemImpact = "MODIFY",
            stability = "STABLE",
            tags = {"database", "update", "insert", "delete"}
    )
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

    @NetatKeyword(
            name = "verifyRecordExists",
            description = "Kiểm tra sự tồn tại của ít nhất một bản ghi thỏa mãn điều kiện trong câu truy vấn. Phương thức này thực thi câu lệnh SELECT và kiểm tra xem kết quả có trống không, sau đó so sánh với giá trị mong đợi.",
            category = "DB/Verification",
            parameters = {
                    "String: profileName - Tên của profile kết nối CSDL đã được khởi tạo trước đó.",
                    "String: query - Câu lệnh SQL SELECT cần thực thi để kiểm tra sự tồn tại của bản ghi.",
                    "boolean: expectedExists - Giá trị mong đợi: true nếu mong đợi bản ghi tồn tại, false nếu mong đợi không tồn tại.",
                    "Object...: params - Các tham số cần truyền vào câu truy vấn, theo thứ tự xuất hiện của các placeholder '?'."
            },
            returnValue = "void: Không trả về giá trị, nhưng sẽ ném AssertionError nếu kiểm chứng thất bại",
            example = "// Kiểm tra xem người dùng với email cụ thể có tồn tại không\n" +
                    "databaseKeyword.verifyRecordExists(\n" +
                    "    \"mysql_dev\", \n" +
                    "    \"SELECT * FROM users WHERE email = ?\", \n" +
                    "    true, // Mong đợi bản ghi tồn tại\n" +
                    "    \"john.doe@example.com\"\n" +
                    ");\n\n" +
                    "// Kiểm tra xem không có đơn hàng nào có trạng thái 'cancelled'\n" +
                    "databaseKeyword.verifyRecordExists(\n" +
                    "    \"mysql_dev\", \n" +
                    "    \"SELECT * FROM orders WHERE status = ?\", \n" +
                    "    false, // Mong đợi không có bản ghi nào\n" +
                    "    \"cancelled\"\n" +
                    ");",
            prerequisites = {"Đã khởi tạo kết nối CSDL với profileName tương ứng"},
            exceptions = {"SQLException: Nếu có lỗi khi thực thi câu truy vấn",
                    "AssertionError: Nếu kết quả kiểm chứng không khớp với giá trị mong đợi"},
            platform = "ALL",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"database", "verification", "assertion"}
    )
    @Step("Kiểm tra sự tồn tại của bản ghi trên [{0}] khớp với điều kiện: {1}")
    public void verifyRecordExists(String profileName, String query, boolean expectedExists, Object... params) {
        execute(() -> {
            List<Map<String, Object>> result = executeQueryWithParams(profileName, query, params);
            Assert.assertEquals(!result.isEmpty(), expectedExists, "Kiểm tra sự tồn tại của bản ghi thất bại.");
            return null;
        }, profileName, query, expectedExists, params);
    }

    @NetatKeyword(
            name = "verifyDataSQL",
            description = "Thực thi câu lệnh SQL và kiểm chứng kết quả với dữ liệu mong đợi. Phương thức này so sánh từng cột trong từng hàng của kết quả với dữ liệu mong đợi được cung cấp.",
            category = "DB/Verification",
            parameters = {
                    "String: profileName - Tên của profile kết nối CSDL đã được khởi tạo trước đó.",
                    "String: query - Câu lệnh SQL SELECT cần thực thi để lấy dữ liệu cần kiểm chứng.",
                    "String[]: expectedColumnNames - Mảng các tên cột cần kiểm chứng.",
                    "Object[][]: expectedData - Mảng 2 chiều chứa dữ liệu mong đợi, mỗi hàng tương ứng với một bản ghi và các cột tương ứng với expectedColumnNames.",
                    "Object...: queryParams - Các tham số cần truyền vào câu truy vấn, theo thứ tự xuất hiện của các placeholder '?'."
            },
            returnValue = "void: Không trả về giá trị, nhưng sẽ ném AssertionError nếu kiểm chứng thất bại",
            example = "// Kiểm tra dữ liệu người dùng\n" +
                    "String[] columns = {\"username\", \"email\", \"status\"};\n" +
                    "Object[][] expectedData = {\n" +
                    "    {\"john.doe\", \"john.doe@example.com\", \"active\"},\n" +
                    "    {\"jane.smith\", \"jane.smith@example.com\", \"inactive\"}\n" +
                    "};\n\n" +
                    "databaseKeyword.verifyDataSQL(\n" +
                    "    \"mysql_dev\",\n" +
                    "    \"SELECT username, email, status FROM users WHERE department = ? ORDER BY username\",\n" +
                    "    columns,\n" +
                    "    expectedData,\n" +
                    "    \"IT\"\n" +
                    ");",
            prerequisites = {"Đã khởi tạo kết nối CSDL với profileName tương ứng"},
            exceptions = {"SQLException: Nếu có lỗi khi thực thi câu truy vấn",
                    "AssertionError: Nếu kết quả kiểm chứng không khớp với dữ liệu mong đợi",
                    "IllegalArgumentException: Nếu cấu trúc dữ liệu mong đợi không hợp lệ"},
            platform = "ALL",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"database", "verification", "assertion", "data-comparison"}
    )
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

    @NetatKeyword(
            name = "verifyDataByQueryFile",
            description = "Thực thi câu lệnh SQL từ file và kiểm chứng kết quả với dữ liệu từ file dữ liệu. Phương thức này tách biệt câu truy vấn và dữ liệu kiểm chứng vào các file riêng biệt, giúp quản lý test case dễ dàng hơn.",
            category = "DB/Verification",
            parameters = {
                    "String: profileName - Tên của profile kết nối CSDL đã được khởi tạo trước đó.",
                    "String: queryPath - Đường dẫn đến file chứa câu lệnh SQL cần thực thi.",
                    "String: dataFilePath - Đường dẫn đến file dữ liệu chứa các tham số truy vấn và dữ liệu mong đợi."
            },
            returnValue = "void: Không trả về giá trị, nhưng sẽ ném AssertionError nếu kiểm chứng thất bại",
            example = "// Kiểm tra dữ liệu người dùng sử dụng file\n" +
                    "databaseKeyword.verifyDataByQueryFile(\n" +
                    "    \"mysql_dev\",\n" +
                    "    \"sql/get_user_by_department.sql\",\n" +
                    "    \"testdata/user_verification_data.xlsx\"\n" +
                    ");\n\n" +
                    "// File SQL có thể chứa: SELECT username, email, status FROM users WHERE department = ?\n" +
                    "// File dữ liệu có thể chứa: {param_1: \"IT\", username: \"john.doe\", email: \"john.doe@example.com\", status: \"active\"}",
            prerequisites = {
                    "Đã khởi tạo kết nối CSDL với profileName tương ứng",
                    "File SQL và file dữ liệu phải tồn tại và có định dạng hợp lệ"
            },
            exceptions = {
                    "SQLException: Nếu có lỗi khi thực thi câu truy vấn",
                    "AssertionError: Nếu kết quả kiểm chứng không khớp với dữ liệu mong đợi",
                    "FileNotFoundException: Nếu không tìm thấy file SQL hoặc file dữ liệu",
                    "IOException: Nếu có lỗi khi đọc file"
            },
            platform = "ALL",
            systemImpact = "READ_ONLY",
            stability = "STABLE",
            tags = {"database", "verification", "file-based", "data-driven"}
    )
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
