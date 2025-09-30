package com.vtnet.netat.db.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.utils.DataFileHelper;
import com.vtnet.netat.db.config.DatabaseProfile;
import com.vtnet.netat.db.connection.ConnectionManager;
import com.vtnet.netat.db.utils.DatabaseHelper;
import com.vtnet.netat.db.utils.JdbcUrlBuilder;
import com.vtnet.netat.db.utils.QueryHelper;
import io.qameta.allure.Step;
import org.testng.Assert;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a set of keywords for interacting with and testing databases.
 * Inherits from BaseKeyword to have logging, retry, and reporting features.
 */
public class DatabaseKeyword extends BaseKeyword {

    // =================================================================================================
    // CONNECTION MANAGEMENT KEYWORDS
    // =================================================================================================

    @NetatKeyword(
            name = "connectDatabase",
            description = "Khởi tạo một connection pool dựa trên file profile. " +
                    "File profile chứa các thông tin cấu hình kết nối như URL, username, password, driver class, và các thuộc tính kết nối khác.",
            category = "DB",
subCategory="Connection",
            parameters = {
                    "profilePath: String - Đường dẫn đến file profile chứa thông tin cấu hình kết nối CSDL"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Khởi tạo kết nối đến cơ sở dữ liệu từ file cấu hình\n" +
                    "databaseKeyword.connectDatabase(\"profiles/mysql_dev.properties\");",
            note = "Áp dụng cho tất cả nền tảng. File profile phải tồn tại và chứa thông tin cấu hình hợp lệ. " +
                    "Có thể throw SQLException nếu không thể thiết lập kết nối đến CSDL, " +
                    "FileNotFoundException nếu không tìm thấy file profile, " +
                    "hoặc ConfigurationException nếu thông tin cấu hình trong profile không hợp lệ."
    )
    @Step("Initialize database connection from profile: {0}")
    public void connectDatabase(String profilePath) {
        execute(() -> {
            DatabaseProfile profile = DatabaseHelper.getProfile(profilePath);
            ConnectionManager.createConnectionPool(profile);
            return null;
        }, profilePath);
    }

    @NetatKeyword(
            name = "connectDatabase",
            description = "Khởi tạo kết nối CSDL bằng cách cung cấp các thông tin riêng lẻ. Thư viện sẽ tự động xây dựng URL kết nối.",
            category = "DB",
            subCategory = "Connection",
            parameters = {
                    "profileName: String - Một tên định danh duy nhất cho kết nối này",
                    "dbType: String - Loại cơ sở dữ liệu (mariadb, postgresql, mysql, sqlserver, oracle, clickhouse)",
                    "host: String - Địa chỉ IP hoặc hostname của server",
                    "port: int - Cổng kết nối",
                    "databaseName: String - Tên của database (hoặc SID/Service Name cho Oracle)",
                    "username: String - Tên người dùng",
                    "password: String - Mật khẩu"
            },
            returnValue = "void - Không trả về giá trị",
            example = "// Kết nối đến PostgreSQL mà không cần biết cấu trúc URL\n" +
                    "databaseKeyword.connectDatabase(\"pg_server\", \"postgresql\", \"10.0.1.5\", 5432, \"app_db\", \"user\", \"pass\");",
            note = "Đây là cách kết nối được khuyên dùng cho người dùng low-code để tránh lỗi cú pháp URL."
    )
    @Step("Initialize database connection for profile: {0} ({1} on {2})")
    public void connectDatabase(String profileName, String dbType, String host, int port, String databaseName, String username, String password) {
        execute(() -> {
            String jdbcUrl = JdbcUrlBuilder.buildUrl(dbType, host, port, databaseName);

            DatabaseProfile profile = new DatabaseProfile();
            profile.setProfileName(profileName);
            profile.setJdbcUrl(jdbcUrl);
            profile.setUsername(username);
            profile.setPassword(password);
            ConnectionManager.createConnectionPool(profile);
            return null;
        }, profileName, dbType, host, port, databaseName, username, password);
    }

    @NetatKeyword(
            name = "disconnectAllDatabases",
            description = "Đóng tất cả các connection pool đang hoạt động và giải phóng tài nguyên. " +
                    "Nên gọi phương thức này ở cuối mỗi test case hoặc test suite để đảm bảo tất cả kết nối được đóng đúng cách.",
            category = "DB",
subCategory="Connection",
            parameters = {},
            returnValue = "void - Không trả về giá trị",
            example = "// Đóng tất cả kết nối CSDL sau khi hoàn thành test\n" +
                    "databaseKeyword.disconnectAllDatabases();",
            note = "Áp dụng cho tất cả nền tảng. Đã khởi tạo ít nhất một kết nối CSDL trước đó. " +
                    "Có thể throw SQLException nếu có lỗi khi đóng kết nối."
    )
    @Step("Close all database connections")
    public void disconnectAllDatabases() {
        execute(() -> {
            ConnectionManager.closeAll();
            return null;
        });
    }

    // =================================================================================================
    // EXECUTION KEYWORDS (PRO-CODE)
    // =================================================================================================

    @NetatKeyword(
            name = "executeQuery",
            description = "Thực thi câu lệnh SELECT và trả về kết quả dưới dạng danh sách các bản ghi. " +
                    "Mỗi bản ghi là một Map với key là tên cột và value là giá trị của cột đó. " +
                    "Hỗ trợ truyền tham số vào câu truy vấn để tránh SQL injection.",
            category = "DB",
subCategory="Execution",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL đã được khởi tạo trước đó",
                    "query: String - Câu lệnh SQL SELECT cần thực thi, có thể chứa các placeholder '?' cho tham số",
                    "params: Object... - Các tham số cần truyền vào câu truy vấn, theo thứ tự xuất hiện của các placeholder '?'"
            },
            returnValue = "List<Map<String, Object>> - Danh sách các bản ghi, mỗi bản ghi là một Map với key là tên cột và value là giá trị",
            example = "// Thực thi câu lệnh SELECT với tham số\n" +
                    "List<Map<String, Object>> users = databaseKeyword.executeQuery(\n" +
                    "    \"mysql_dev\", \n" +
                    "    \"SELECT id, username, email FROM users WHERE status = ? AND created_date > ?\", \n" +
                    "    \"active\", \"2023-01-01\"\n" +
                    ");\n\n" +
                    "// Truy cập dữ liệu từ kết quả\n" +
                    "String username = users.get(0).get(\"username\").toString();",
            note = "Áp dụng cho tất cả nền tảng. Đã khởi tạo kết nối CSDL với profileName tương ứng. " +
                    "Có thể throw SQLException nếu có lỗi khi thực thi câu truy vấn, " +
                    "hoặc IllegalArgumentException nếu profileName không tồn tại."
    )
    @Step("Execute SELECT statement on [{0}]: {1}")
    public List<Map<String, Object>> executeQuery(String profileName, String query, Object... params) {
        return execute(() -> executeQueryWithParams(profileName, query, params), profileName, query, params);
    }

    @NetatKeyword(
            name = "executeUpdate",
            description = "Thực thi câu lệnh INSERT, UPDATE, DELETE và trả về số bản ghi bị ảnh hưởng. " +
                    "Hỗ trợ truyền tham số vào câu truy vấn để tránh SQL injection.",
            category = "DB",
subCategory="Execution",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL đã được khởi tạo trước đó",
                    "query: String - Câu lệnh SQL INSERT, UPDATE hoặc DELETE cần thực thi, có thể chứa các placeholder '?' cho tham số",
                    "params: Object... - Các tham số cần truyền vào câu truy vấn, theo thứ tự xuất hiện của các placeholder '?'"
            },
            returnValue = "int - Số bản ghi bị ảnh hưởng bởi câu lệnh",
            example = "// Thêm một bản ghi mới vào bảng users\n" +
                    "int rowsAffected = databaseKeyword.executeUpdate(\n" +
                    "    \"mysql_dev\", \n" +
                    "    \"INSERT INTO users (username, email, status) VALUES (?, ?, ?)\", \n" +
                    "    \"john.doe\", \"john.doe@example.com\", \"active\"\n" +
                    ");\n\n" +
                    "// Kiểm tra xem có đúng một bản ghi được thêm vào không\n" +
                    "Assert.assertEquals(rowsAffected, 1);",
            note = "Áp dụng cho tất cả nền tảng. Đã khởi tạo kết nối CSDL với profileName tương ứng. " +
                    "Có thể throw SQLException nếu có lỗi khi thực thi câu lệnh, " +
                    "hoặc IllegalArgumentException nếu profileName không tồn tại."
    )
    @Step("Execute UPDATE statement on [{0}]: {1}")
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
    // VERIFICATION KEYWORDS (PRO-CODE & LOW-CODE)
    // =================================================================================================

    @NetatKeyword(
            name = "verifyRecordExists",
            description = "Kiểm tra sự tồn tại của ít nhất một bản ghi thỏa mãn điều kiện trong câu truy vấn. " +
                    "Phương thức này thực thi câu lệnh SELECT và kiểm tra xem kết quả có trống không, " +
                    "sau đó so sánh với giá trị mong đợi.",
            category = "DB",
subCategory="Verification",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL đã được khởi tạo trước đó",
                    "query: String - Câu lệnh SQL SELECT cần thực thi để kiểm tra sự tồn tại của bản ghi",
                    "expectedExists: boolean - Giá trị mong đợi: true nếu mong đợi bản ghi tồn tại, false nếu mong đợi không tồn tại",
                    "params: Object... - Các tham số cần truyền vào câu truy vấn, theo thứ tự xuất hiện của các placeholder '?'"
            },
            returnValue = "void - Không trả về giá trị, nhưng sẽ ném AssertionError nếu kiểm chứng thất bại",
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
            note = "Áp dụng cho tất cả nền tảng. Đã khởi tạo kết nối CSDL với profileName tương ứng. " +
                    "Có thể throw SQLException nếu có lỗi khi thực thi câu truy vấn, " +
                    "hoặc AssertionError nếu kết quả kiểm chứng không khớp với giá trị mong đợi."
    )
    @Step("Check record existence on [{0}] matching condition: {1}")
    public void verifyRecordExists(String profileName, String query, boolean expectedExists, Object... params) {
        execute(() -> {
            List<Map<String, Object>> result = executeQueryWithParams(profileName, query, params);
            Assert.assertEquals(!result.isEmpty(), expectedExists, "Record existence verification failed.");
            return null;
        }, profileName, query, expectedExists, params);
    }

    @NetatKeyword(
            name = "verifyDataSQL",
            description = "Thực thi câu lệnh SQL và kiểm chứng kết quả với dữ liệu mong đợi. " +
                    "Phương thức này so sánh từng cột trong từng hàng của kết quả với dữ liệu mong đợi được cung cấp.",
            category = "DB",
subCategory="Verification",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL đã được khởi tạo trước đó",
                    "query: String - Câu lệnh SQL SELECT cần thực thi để lấy dữ liệu cần kiểm chứng",
                    "expectedColumnNames: String[] - Mảng các tên cột cần kiểm chứng",
                    "expectedData: Object[][] - Mảng 2 chiều chứa dữ liệu mong đợi, mỗi hàng tương ứng với một bản ghi và các cột tương ứng với expectedColumnNames",
                    "queryParams: Object... - Các tham số cần truyền vào câu truy vấn, theo thứ tự xuất hiện của các placeholder '?'"
            },
            returnValue = "void - Không trả về giá trị, nhưng sẽ ném AssertionError nếu kiểm chứng thất bại",
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
            note = "Áp dụng cho tất cả nền tảng. Đã khởi tạo kết nối CSDL với profileName tương ứng. " +
                    "Có thể throw SQLException nếu có lỗi khi thực thi câu truy vấn, " +
                    "AssertionError nếu kết quả kiểm chứng không khớp với dữ liệu mong đợi, " +
                    "hoặc IllegalArgumentException nếu cấu trúc dữ liệu mong đợi không hợp lệ."
    )
    @Step("Verify data on [{0}] using direct SQL statement")
    public void verifyDataSQL(String profileName, String query, String[] expectedColumnNames, Object[][] expectedData, Object... queryParams) {
        execute(() -> {
            List<Map<String, Object>> actualResult = executeQueryWithParams(profileName, query, queryParams);
            Assert.assertEquals(actualResult.size(), expectedData.length, "Verification error: Number of returned rows does not match.");

            for (int i = 0; i < expectedData.length; i++) {
                Map<String, Object> actualRow = actualResult.get(i);
                Object[] expectedRow = expectedData[i];
                Assert.assertEquals(expectedRow.length, expectedColumnNames.length, "Configuration error: Number of columns in expected data does not match number of header columns at row " + (i + 1));

                for (int j = 0; j < expectedColumnNames.length; j++) {
                    String columnName = expectedColumnNames[j];
                    Object expectedValue = expectedRow[j];
                    Assert.assertTrue(actualRow.containsKey(columnName), "Column '" + columnName + "' does not exist in query result.");
                    Object actualValue = actualRow.get(columnName);
                    Assert.assertEquals(String.valueOf(actualValue), String.valueOf(expectedValue), "Verification error at row " + (i + 1) + ", column '" + columnName + "' does not match.");
                }
            }
            return null;
        }, profileName, query, expectedColumnNames, expectedData, queryParams);
    }

    @NetatKeyword(
            name = "verifyDataByQueryFile",
            description = "Thực thi câu lệnh SQL từ file và kiểm chứng kết quả với dữ liệu từ file dữ liệu. " +
                    "Phương thức này tách biệt câu truy vấn và dữ liệu kiểm chứng vào các file riêng biệt, " +
                    "giúp quản lý test case dễ dàng hơn.",
            category = "DB",
subCategory="Verification",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL đã được khởi tạo trước đó",
                    "queryPath: String - Đường dẫn đến file chứa câu lệnh SQL cần thực thi",
                    "dataFilePath: String - Đường dẫn đến file dữ liệu chứa các tham số truy vấn và dữ liệu mong đợi"
            },
            returnValue = "void - Không trả về giá trị, nhưng sẽ ném AssertionError nếu kiểm chứng thất bại",
            example = "// Kiểm tra dữ liệu người dùng sử dụng file\n" +
                    "databaseKeyword.verifyDataByQueryFile(\n" +
                    "    \"mysql_dev\",\n" +
                    "    \"sql/get_user_by_department.sql\",\n" +
                    "    \"testdata/user_verification_data.xlsx\"\n" +
                    ");\n\n" +
                    "// File SQL có thể chứa: SELECT username, email, status FROM users WHERE department = ?\n" +
                    "// File dữ liệu có thể chứa: {param_1: \"IT\", username: \"john.doe\", email: \"john.doe@example.com\", status: \"active\"}",
            note = "Áp dụng cho tất cả nền tảng. Đã khởi tạo kết nối CSDL với profileName tương ứng, " +
                    "File SQL và file dữ liệu phải tồn tại và có định dạng hợp lệ. " +
                    "Có thể throw SQLException nếu có lỗi khi thực thi câu truy vấn, " +
                    "AssertionError nếu kết quả kiểm chứng không khớp với dữ liệu mong đợi, " +
                    "FileNotFoundException nếu không tìm thấy file SQL hoặc file dữ liệu, " +
                    "hoặc IOException nếu có lỗi khi đọc file."
    )
    @Step("Verify data on [{0}] using query file [{1}] and data file [{2}]")
    public void verifyDataByQueryFile(String profileName, String queryPath, String dataFilePath) {
        execute(() -> {
            String baseQuery = QueryHelper.getQuery(queryPath);
            Object[][] data = DataFileHelper.getTestData(dataFilePath);
            Assert.assertTrue(data.length > 0, "Verification data file has no data: " + dataFilePath);
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
            Assert.assertFalse(result.isEmpty(), "SELECT statement returned no records.");
            Map<String, Object> actualDataRow = result.get(0);

            for (Map.Entry<String, Object> entry : verificationData.entrySet()) {
                String columnName = entry.getKey();
                Object expectedValue = entry.getValue();
                Assert.assertTrue(actualDataRow.containsKey(columnName), "Column '" + columnName + "' does not exist.");
                Assert.assertEquals(String.valueOf(actualDataRow.get(columnName)), String.valueOf(expectedValue));
            }
            return null;
        }, profileName, queryPath, dataFilePath);
    }

    // =================================================================================================
    // SUPPORT METHODS (PRIVATE)
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

    // =================================================================================================
// DATA RETRIEVAL KEYWORDS (NEW UTILITY KEYWORDS)
// =================================================================================================

    @NetatKeyword(
            name = "getCellValue",
            description = "Thực thi câu lệnh SELECT và trả về giá trị của ô đầu tiên (dòng 1, cột 1). Rất hữu ích để lấy một giá trị duy nhất để xác thực hoặc sử dụng trong các bước tiếp theo.",
            category = "DB",
            subCategory = "Data Retrieval",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL",
                    "query: String - Câu lệnh SQL SELECT, thường chỉ chọn một cột",
                    "params: Object... - Các tham số cần truyền vào câu truy vấn"
            },
            returnValue = "String - Giá trị của ô dưới dạng chuỗi, hoặc null nếu không có kết quả.",
            example = "// Lấy email của một người dùng cụ thể\n" +
                    "String email = databaseKeyword.getCellValue(\"mysql_dev\", \"SELECT email FROM users WHERE id = ?\", 123);",
            note = "Nếu câu truy vấn trả về nhiều dòng hoặc nhiều cột, keyword này sẽ chỉ lấy giá trị của cột đầu tiên ở dòng đầu tiên."
    )
    @Step("Get single cell value from [{0}] with query: {1}")
    public String getCellValue(String profileName, String query, Object... params) {
        return execute(() -> {
            List<Map<String, Object>> result = executeQueryWithParams(profileName, query, params);
            if (result == null || result.isEmpty()) {
                return null; // Không có bản ghi nào được tìm thấy
            }
            Map<String, Object> firstRow = result.get(0);
            if (firstRow == null || firstRow.isEmpty()) {
                return null; // Dòng đầu tiên rỗng
            }
            // Lấy giá trị của cột đầu tiên, bất kể tên cột là gì
            Object value = firstRow.values().iterator().next();
            return value != null ? value.toString() : null;
        });
    }

    @NetatKeyword(
            name = "getRow",
            description = "Thực thi câu lệnh SELECT và trả về bản ghi đầu tiên dưới dạng một Map. Hữu ích khi bạn cần kiểm tra nhiều trường của một đối tượng duy nhất.",
            category = "DB",
            subCategory = "Data Retrieval",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL",
                    "query: String - Câu lệnh SQL SELECT",
                    "params: Object... - Các tham số cần truyền vào câu truy vấn"
            },
            returnValue = "Map<String, Object> - Một Map biểu diễn bản ghi đầu tiên, với key là tên cột và value là giá trị.",
            example = "// Lấy toàn bộ thông tin của một sản phẩm\n" +
                    "Map<String, Object> product = databaseKeyword.getRow(\"mysql_dev\", \"SELECT * FROM products WHERE id = ?\", \"PROD-001\");\n" +
                    "String productName = product.get(\"name\").toString();\n" +
                    "double price = (Double) product.get(\"price\");",
            note = "Nếu câu truy vấn trả về nhiều bản ghi, chỉ có bản ghi đầu tiên được trả về. Trả về null nếu không có kết quả."
    )
    @Step("Get first row from [{0}] with query: {1}")
    public Map<String, Object> getRow(String profileName, String query, Object... params) {
        return execute(() -> {
            List<Map<String, Object>> result = executeQueryWithParams(profileName, query, params);
            return result.isEmpty() ? null : result.get(0);
        });
    }

    @NetatKeyword(
            name = "getColumnValues",
            description = "Thực thi câu lệnh SELECT và trả về tất cả giá trị của một cột cụ thể dưới dạng một danh sách.",
            category = "DB",
            subCategory = "Data Retrieval",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL",
                    "columnName: String - Tên của cột mà bạn muốn lấy dữ liệu",
                    "query: String - Câu lệnh SQL SELECT",
                    "params: Object... - Các tham số cần truyền vào câu truy vấn"
            },
            returnValue = "List<Object> - Một danh sách chứa tất cả các giá trị từ cột được chỉ định.",
            example = "// Lấy danh sách email của tất cả người dùng đang hoạt động\n" +
                    "List<Object> emails = databaseKeyword.getColumnValues(\"mysql_dev\", \"email\", \"SELECT email FROM users WHERE status = ?\", \"active\");",
            note = "Đảm bảo rằng tên cột bạn cung cấp khớp chính xác với tên cột trong kết quả trả về của câu lệnh SELECT."
    )
    @Step("Get values from column [{1}] on [{0}] with query: {2}")
    public List<Object> getColumnValues(String profileName, String columnName, String query, Object... params) {
        return execute(() -> {
            List<Map<String, Object>> result = executeQueryWithParams(profileName, query, params);
            List<Object> columnValues = new ArrayList<>();
            if (result.isEmpty()) {
                return columnValues; // Trả về danh sách rỗng
            }
            for (Map<String, Object> row : result) {
                columnValues.add(row.get(columnName));
            }
            return columnValues;
        });
    }

    @NetatKeyword(
            name = "getRowCount",
            description = "Thực thi một câu lệnh SELECT và trả về tổng số bản ghi (dòng) trong kết quả.",
            category = "DB",
            subCategory = "Data Retrieval",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL",
                    "query: String - Câu lệnh SQL SELECT",
                    "params: Object... - Các tham số cần truyền vào câu truy vấn"
            },
            returnValue = "int - Số lượng bản ghi trong kết quả.",
            example = "// Đếm số lượng đơn hàng đã được giao thành công\n" +
                    "int deliveredOrders = databaseKeyword.getRowCount(\"mysql_dev\", \"SELECT * FROM orders WHERE status = ?\", \"DELIVERED\");",
            note = "Keyword này là một cách khác để kiểm tra sự tồn tại của dữ liệu. Khác với executeUpdate, nó không thay đổi dữ liệu."
    )
    @Step("Get row count from [{0}] with query: {1}")
    public int getRowCount(String profileName, String query, Object... params) {
        return execute(() -> {
            List<Map<String, Object>> result = executeQueryWithParams(profileName, query, params);
            return result.size();
        });
    }

    @NetatKeyword(
            name = "getQueryResultsAsString",
            description = "Thực thi câu lệnh SELECT và trả về toàn bộ kết quả dưới dạng một chuỗi String duy nhất, được định dạng sẵn. Rất hữu ích để kiểm tra nhanh hoặc xác thực một phần dữ liệu.",
            category = "DB",
            subCategory = "Data Retrieval",
            parameters = {
                    "profileName: String - Tên của profile kết nối CSDL",
                    "query: String - Câu lệnh SQL SELECT",
                    "params: Object... - Các tham số cần truyền vào câu truy vấn"
            },
            returnValue = "String - Toàn bộ bảng kết quả đã được định dạng.",
            example = "// Lấy thông tin sinh viên và kiểm tra xem có chứa tên 'Nguyen Van A' không\n" +
                    "String result = databaseKeyword.getQueryResultsAsString(\"dbMariaLocal\", \"SELECT ma_sv, ten_sv FROM sinhvien\");\n" +
                    "assertion.assertContains(result, \"Nguyen Van A\");",
            note = "Chuỗi trả về sẽ bao gồm cả tiêu đề cột và tất cả các dòng dữ liệu, mỗi dòng trên một hàng mới."
    )
    @Step("Get query results as formatted string from [{0}] with query: {1}")
    public String getQueryResultsAsString(String profileName, String query, Object... params) {
        return execute(() -> {
            List<Map<String, Object>> result = executeQueryWithParams(profileName, query, params);

            if (result == null || result.isEmpty()) {
                return "Query returned no results.";
            }

            StringBuilder sb = new StringBuilder();

            // Lấy và thêm tiêu đề cột
            String headers = String.join(" | ", result.get(0).keySet());
            sb.append(headers).append("\n");
            sb.append("---------------------------------\n");

            // Thêm từng dòng dữ liệu
            for (Map<String, Object> row : result) {
                List<String> values = new ArrayList<>();
                for (Object value : row.values()) {
                    values.add(String.valueOf(value));
                }
                sb.append(String.join(" | ", values)).append("\n");
            }

            return sb.toString();
        });
    }
}
