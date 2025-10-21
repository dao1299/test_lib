package com.vtnet.netat.db.keywords;

import com.vtnet.netat.core.BaseKeyword;
import com.vtnet.netat.core.annotations.NetatKeyword;
import com.vtnet.netat.core.context.ExecutionContext;
import com.vtnet.netat.db.connection.ConnectionManager;
import com.vtnet.netat.db.exceptions.DatabaseException;
import com.vtnet.netat.db.exceptions.ErrorSeverity;
import com.vtnet.netat.db.exceptions.GenericDatabaseException;
import com.vtnet.netat.db.exceptions.SqlStateMapper;
import com.vtnet.netat.db.exceptions.query.QueryExecutionException;
import com.vtnet.netat.db.logging.DatabaseLogger;
import com.vtnet.netat.db.logging.LogContext;
import com.vtnet.netat.db.logging.model.PoolStats;

import java.sql.*;
import java.util.*;

/**
 * Database keyword với đầy đủ logging support.
 * Mọi operation đều được log với context, duration, và error details.
 *
 * @author NETAT Team
 * @version 1.1.0
 */
public class DatabaseKeyword extends BaseKeyword {

    private static final DatabaseLogger dbLogger = DatabaseLogger.getInstance();

    // ========================================================================
    // QUERY EXECUTION KEYWORDS
    // ========================================================================

    @NetatKeyword(
            name = "executeQuery",
            description = "Thực thi câu lệnh SELECT và trả về kết quả dưới dạng List các Map (mỗi Map đại diện cho 1 row)",
            category = "Database",
            subCategory = "Query Execution",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu lệnh SQL SELECT (có thể chứa ? placeholder)",
                    "params: Object... - Các tham số để thay thế cho ? trong query (tùy chọn)"
            },
            returnValue = "List<Map<String, Object>> - Danh sách các row, mỗi row là Map với key=column name, value=column value",
            example =
                    "// Lấy thông tin user theo email\n" +
                            "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "List<Map<String, Object>> users = db.executeQuery(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM users WHERE email = ?\",\n" +
                            "    \"john@test.com\"\n" +
                            ");\n" +
                            "System.out.println(\"User ID: \" + users.get(0).get(\"id\"));\n" +
                            "System.out.println(\"User Name: \" + users.get(0).get(\"name\"));\n" +
                            "\n" +
                            "// Query không có parameter\n" +
                            "List<Map<String, Object>> activeUsers = db.executeQuery(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM users WHERE status = 'active'\"\n" +
                            ");\n" +
                            "\n" +
                            "// Query với nhiều parameters\n" +
                            "List<Map<String, Object>> orders = db.executeQuery(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM orders WHERE user_id = ? AND status = ?\",\n" +
                            "    123,\n" +
                            "    \"completed\"\n" +
                            ");\n" +
                            "\n" +
                            "// Iterate qua tất cả results\n" +
                            "for (Map<String, Object> order : orders) {\n" +
                            "    System.out.println(\"Order ID: \" + order.get(\"id\"));\n" +
                            "    System.out.println(\"Amount: \" + order.get(\"amount\"));\n" +
                            "}",
            note = "- Query sẽ được log tự động với thời gian thực thi và số lượng rows trả về\n" +
                    "- Dữ liệu nhạy cảm (password, email) sẽ được mask trong log\n" +
                    "- Nếu query chậm hơn threshold (mặc định 1000ms), sẽ có warning log\n" +
                    "- Trả về empty list [] nếu không tìm thấy kết quả\n" +
                    "- Throw DatabaseException nếu có lỗi SQL"
    )
    public List<Map<String, Object>> executeQuery(String profileName, String query, Object... params) {
        return executeWithLogging(
                "executeQuery",
                profileName,
                query,
                params,
                () -> executeQueryInternal(profileName, query, params)
        );
    }

    @NetatKeyword(
            name = "executeUpdate",
            description = "Thực thi câu lệnh INSERT, UPDATE, DELETE và trả về số lượng rows bị ảnh hưởng",
            category = "Database",
            subCategory = "Query Execution",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu lệnh SQL INSERT/UPDATE/DELETE (có thể chứa ? placeholder)",
                    "params: Object... - Các tham số để thay thế cho ? trong query (tùy chọn)"
            },
            returnValue = "int - Số lượng rows đã được insert/update/delete",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Insert một user mới\n" +
                            "int inserted = db.executeUpdate(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"INSERT INTO users (name, email, status) VALUES (?, ?, ?)\",\n" +
                            "    \"John Doe\",\n" +
                            "    \"john@test.com\",\n" +
                            "    \"active\"\n" +
                            ");\n" +
                            "System.out.println(\"Inserted \" + inserted + \" row(s)\");\n" +
                            "\n" +
                            "// Update user status\n" +
                            "int updated = db.executeUpdate(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"UPDATE users SET status = ? WHERE email = ?\",\n" +
                            "    \"inactive\",\n" +
                            "    \"john@test.com\"\n" +
                            ");\n" +
                            "System.out.println(\"Updated \" + updated + \" row(s)\");\n" +
                            "\n" +
                            "// Delete user\n" +
                            "int deleted = db.executeUpdate(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"DELETE FROM users WHERE id = ?\",\n" +
                            "    123\n" +
                            ");\n" +
                            "System.out.println(\"Deleted \" + deleted + \" row(s)\");\n" +
                            "\n" +
                            "// Update multiple rows\n" +
                            "int batchUpdated = db.executeUpdate(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"UPDATE users SET last_login = NOW() WHERE status = ?\",\n" +
                            "    \"active\"\n" +
                            ");\n" +
                            "System.out.println(\"Updated \" + batchUpdated + \" active users\");",
            note = "- Trả về 0 nếu không có row nào bị ảnh hưởng (ví dụ: WHERE condition không match)\n" +
                    "- INSERT thành công thường trả về 1\n" +
                    "- UPDATE/DELETE có thể trả về nhiều hơn 1 nếu WHERE condition match nhiều rows\n" +
                    "- Query sẽ được log với số rows affected\n" +
                    "- Throw DatabaseException nếu có lỗi (duplicate key, foreign key violation, etc.)"
    )
    public int executeUpdate(String profileName, String query, Object... params) {
        return executeWithLogging(
                "executeUpdate",
                profileName,
                query,
                params,
                () -> executeUpdateInternal(profileName, query, params)
        );
    }

    @NetatKeyword(
            name = "executeBatch",
            description = "Thực thi batch operations - insert/update nhiều records cùng lúc cho hiệu suất tốt hơn",
            category = "Database",
            subCategory = "Query Execution",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu lệnh SQL với ? placeholders",
                    "batchParams: List<Object[]> - List các mảng parameters, mỗi mảng tương ứng với 1 execution"
            },
            returnValue = "int[] - Mảng số lượng rows affected cho mỗi batch operation",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Tạo list parameters cho batch insert\n" +
                            "List<Object[]> batchParams = new ArrayList<>();\n" +
                            "batchParams.add(new Object[]{\"John Doe\", \"john@test.com\", \"active\"});\n" +
                            "batchParams.add(new Object[]{\"Jane Smith\", \"jane@test.com\", \"active\"});\n" +
                            "batchParams.add(new Object[]{\"Bob Johnson\", \"bob@test.com\", \"inactive\"});\n" +
                            "\n" +
                            "// Execute batch insert\n" +
                            "int[] results = db.executeBatch(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"INSERT INTO users (name, email, status) VALUES (?, ?, ?)\",\n" +
                            "    batchParams\n" +
                            ");\n" +
                            "System.out.println(\"Inserted \" + results.length + \" users\");\n" +
                            "\n" +
                            "// Batch update example\n" +
                            "List<Object[]> updates = new ArrayList<>();\n" +
                            "updates.add(new Object[]{\"active\", 123});\n" +
                            "updates.add(new Object[]{\"active\", 456});\n" +
                            "updates.add(new Object[]{\"inactive\", 789});\n" +
                            "\n" +
                            "int[] updated = db.executeBatch(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"UPDATE users SET status = ? WHERE id = ?\",\n" +
                            "    updates\n" +
                            ");\n" +
                            "\n" +
                            "// Check individual results\n" +
                            "for (int i = 0; i < updated.length; i++) {\n" +
                            "    System.out.println(\"Batch \" + i + \": \" + updated[i] + \" row(s) affected\");\n" +
                            "}",
            note = "- Hiệu suất tốt hơn nhiều so với loop executeUpdate cho từng record\n" +
                    "- Nên dùng khi cần insert/update >= 10 records\n" +
                    "- Tất cả operations trong batch dùng chung 1 connection\n" +
                    "- Nếu 1 operation fail, các operation khác vẫn có thể thành công (tùy database)\n" +
                    "- Trả về mảng với length = số lượng batch operations"
    )
    public int[] executeBatch(String profileName, String query, List<Object[]> batchParams) {
        return executeWithLogging(
                "executeBatch",
                profileName,
                query,
                new Object[]{batchParams.size() + " batches"},
                () -> executeBatchInternal(profileName, query, batchParams)
        );
    }

    @NetatKeyword(
            name = "executeScript",
            description = "Thực thi một SQL script chứa nhiều statements (phân cách bằng dấu ;)",
            category = "Database",
            subCategory = "Query Execution",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "script: String - SQL script với nhiều statements, phân cách bằng dấu ;"
            },
            returnValue = "void - Không trả về giá trị",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Execute script để tạo và populate table\n" +
                            "String script = \n" +
                            "    \"CREATE TABLE IF NOT EXISTS test_users (\" +\n" +
                            "    \"    id INT PRIMARY KEY AUTO_INCREMENT,\" +\n" +
                            "    \"    name VARCHAR(100),\" +\n" +
                            "    \"    email VARCHAR(100)\" +\n" +
                            "    \");\" +\n" +
                            "    \"INSERT INTO test_users (name, email) VALUES ('John', 'john@test.com');\" +\n" +
                            "    \"INSERT INTO test_users (name, email) VALUES ('Jane', 'jane@test.com');\";\n" +
                            "\n" +
                            "db.executeScript(\"mysql-dev\", script);\n" +
                            "\n" +
                            "// Execute setup script từ file\n" +
                            "String setupSql = new String(Files.readAllBytes(\n" +
                            "    Paths.get(\"src/test/resources/setup.sql\")\n" +
                            "));\n" +
                            "db.executeScript(\"mysql-dev\", setupSql);\n" +
                            "\n" +
                            "// Multi-line script với StringBuilder\n" +
                            "StringBuilder sb = new StringBuilder();\n" +
                            "sb.append(\"DROP TABLE IF EXISTS temp_table;\");\n" +
                            "sb.append(\"CREATE TABLE temp_table (id INT, value VARCHAR(50));\");\n" +
                            "sb.append(\"INSERT INTO temp_table VALUES (1, 'test');\");\n" +
                            "db.executeScript(\"mysql-dev\", sb.toString());",
            note = "- Script sẽ được split theo dấu ; và execute từng statement riêng biệt\n" +
                    "- Nếu 1 statement fail, các statement sau sẽ không được execute\n" +
                    "- Thích hợp cho database setup/teardown scripts\n" +
                    "- Empty statements (chỉ có whitespace) sẽ bị bỏ qua\n" +
                    "- Throw DatabaseException nếu có bất kỳ statement nào fail"
    )
    public void executeScript(String profileName, String script) {
        executeWithLogging(
                "executeScript",
                profileName,
                script,
                new Object[0],
                () -> {
                    executeScriptInternal(profileName, script);
                    return null;
                }
        );
    }

    // ========================================================================
    // VERIFICATION KEYWORDS
    // ========================================================================

    @NetatKeyword(
            name = "verifyRecordExists",
            description = "Verify rằng ít nhất một record thỏa mãn điều kiện query tồn tại trong database",
            category = "Database",
            subCategory = "Verification",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu lệnh SELECT để tìm record",
                    "params: Object... - Parameters cho query (tùy chọn)"
            },
            returnValue = "void - Không trả về giá trị (throw AssertionError nếu không tìm thấy)",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Verify user với email cụ thể tồn tại\n" +
                            "db.verifyRecordExists(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM users WHERE email = ?\",\n" +
                            "    \"john@test.com\"\n" +
                            ");\n" +
                            "\n" +
                            "// Verify order đã được tạo\n" +
                            "db.verifyRecordExists(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM orders WHERE user_id = ? AND status = ?\",\n" +
                            "    123,\n" +
                            "    \"completed\"\n" +
                            ");\n" +
                            "\n" +
                            "// Verify session còn active\n" +
                            "String sessionToken = \"abc123xyz\";\n" +
                            "db.verifyRecordExists(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM sessions WHERE token = ? AND expires_at > NOW()\",\n" +
                            "    sessionToken\n" +
                            ");\n" +
                            "\n" +
                            "// Trong test case với try-catch\n" +
                            "try {\n" +
                            "    db.verifyRecordExists(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"SELECT * FROM users WHERE id = ?\",\n" +
                            "        999\n" +
                            "    );\n" +
                            "    System.out.println(\"✓ User exists\");\n" +
                            "} catch (AssertionError e) {\n" +
                            "    System.out.println(\"✗ User not found: \" + e.getMessage());\n" +
                            "}",
            note = "- Throw AssertionError với message rõ ràng nếu không tìm thấy record\n" +
                    "- Pass nếu tìm thấy >= 1 record matching query\n" +
                    "- Thích hợp cho verification sau khi create/update data\n" +
                    "- Query nên được design để return đúng records cần verify"
    )
    public void verifyRecordExists(String profileName, String query, Object... params) {
        executeWithLogging(
                "verifyRecordExists",
                profileName,
                query,
                params,
                () -> {
                    List<Map<String, Object>> results = executeQueryInternal(profileName, query, params);
                    if (results.isEmpty()) {
                        throw new AssertionError("Expected record to exist but none found");
                    }
                    return null;
                }
        );
    }

    @NetatKeyword(
            name = "verifyRecordNotExists",
            description = "Verify rằng KHÔNG có record nào thỏa mãn điều kiện query trong database",
            category = "Database",
            subCategory = "Verification",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu lệnh SELECT để tìm record",
                    "params: Object... - Parameters cho query (tùy chọn)"
            },
            returnValue = "void - Không trả về giá trị (throw AssertionError nếu tìm thấy record)",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Verify user đã bị delete\n" +
                            "db.verifyRecordNotExists(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM users WHERE email = ?\",\n" +
                            "    \"deleted@test.com\"\n" +
                            ");\n" +
                            "\n" +
                            "// Verify không có pending orders\n" +
                            "db.verifyRecordNotExists(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM orders WHERE user_id = ? AND status = ?\",\n" +
                            "    123,\n" +
                            "    \"pending\"\n" +
                            ");\n" +
                            "\n" +
                            "// Verify session đã expire\n" +
                            "String oldToken = \"expired_token_123\";\n" +
                            "db.verifyRecordNotExists(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM sessions WHERE token = ? AND expires_at > NOW()\",\n" +
                            "    oldToken\n" +
                            ");\n" +
                            "\n" +
                            "// Test cleanup verification\n" +
                            "try {\n" +
                            "    db.verifyRecordNotExists(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"SELECT * FROM temp_data WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)\"\n" +
                            "    );\n" +
                            "    System.out.println(\"✓ Old temp data cleaned up\");\n" +
                            "} catch (AssertionError e) {\n" +
                            "    System.out.println(\"✗ Cleanup incomplete: \" + e.getMessage());\n" +
                            "}",
            note = "- Throw AssertionError nếu tìm thấy bất kỳ record nào\n" +
                    "- Pass nếu query trả về 0 results\n" +
                    "- Thích hợp cho verification sau khi delete hoặc cleanup\n" +
                    "- Hữu ích để đảm bảo data đã được remove hoàn toàn"
    )
    public void verifyRecordNotExists(String profileName, String query, Object... params) {
        executeWithLogging(
                "verifyRecordNotExists",
                profileName,
                query,
                params,
                () -> {
                    List<Map<String, Object>> results = executeQueryInternal(profileName, query, params);
                    if (!results.isEmpty()) {
                        throw new AssertionError("Expected no records but found " + results.size());
                    }
                    return null;
                }
        );
    }

    @NetatKeyword(
            name = "verifyRowCount",
            description = "Verify số lượng rows trả về từ query bằng đúng expected count",
            category = "Database",
            subCategory = "Verification",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu lệnh SELECT",
                    "expectedCount: int - Số lượng rows mong đợi",
                    "params: Object... - Parameters cho query (tùy chọn)"
            },
            returnValue = "void - Không trả về giá trị (throw AssertionError nếu count không khớp)",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Verify có đúng 5 active users\n" +
                            "db.verifyRowCount(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM users WHERE status = ?\",\n" +
                            "    5,\n" +
                            "    \"active\"\n" +
                            ");\n" +
                            "\n" +
                            "// Verify user có đúng 3 orders\n" +
                            "db.verifyRowCount(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM orders WHERE user_id = ?\",\n" +
                            "    3,\n" +
                            "    123\n" +
                            ");\n" +
                            "\n" +
                            "// Verify không có records (count = 0)\n" +
                            "db.verifyRowCount(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM temp_table\",\n" +
                            "    0\n" +
                            ");\n" +
                            "\n" +
                            "// Verify batch insert count\n" +
                            "int insertedCount = 10;\n" +
                            "db.verifyRowCount(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM users WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 MINUTE)\",\n" +
                            "    insertedCount\n" +
                            ");\n" +
                            "System.out.println(\"✓ All \" + insertedCount + \" records inserted successfully\");",
            note = "- Throw AssertionError với actual vs expected count nếu không khớp\n" +
                    "- Pass nếu số lượng rows = expectedCount\n" +
                    "- Có thể dùng expectedCount = 0 để verify empty result\n" +
                    "- Hữu ích cho batch operation verification"
    )
    public void verifyRowCount(String profileName, String query, int expectedCount, Object... params) {
        executeWithLogging(
                "verifyRowCount",
                profileName,
                query,
                params,
                () -> {
                    List<Map<String, Object>> results = executeQueryInternal(profileName, query, params);
                    if (results.size() != expectedCount) {
                        throw new AssertionError(
                                String.format("Expected %d rows but got %d", expectedCount, results.size())
                        );
                    }
                    return null;
                }
        );
    }

    @NetatKeyword(
            name = "verifyColumnValue",
            description = "Verify giá trị của một column cụ thể trong first row của query result",
            category = "Database",
            subCategory = "Verification",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu lệnh SELECT",
                    "params: Object[] - Parameters cho query",
                    "columnName: String - Tên column cần verify",
                    "expectedValue: Object - Giá trị mong đợi của column"
            },
            returnValue = "void - Không trả về giá trị (throw AssertionError nếu value không khớp)",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Verify user status\n" +
                            "Object[] params1 = {123};\n" +
                            "db.verifyColumnValue(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT status FROM users WHERE id = ?\",\n" +
                            "    params1,\n" +
                            "    \"status\",\n" +
                            "    \"active\"\n" +
                            ");\n" +
                            "\n" +
                            "// Verify order amount\n" +
                            "int orderId = 456;\n" +
                            "double expectedAmount = 99.99;\n" +
                            "db.verifyColumnValue(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT total_amount FROM orders WHERE id = ?\",\n" +
                            "    new Object[]{orderId},\n" +
                            "    \"total_amount\",\n" +
                            "    expectedAmount\n" +
                            ");\n" +
                            "\n" +
                            "// Verify counter value\n" +
                            "db.verifyColumnValue(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT COUNT(*) as cnt FROM users\",\n" +
                            "    new Object[]{},\n" +
                            "    \"cnt\",\n" +
                            "    10\n" +
                            ");\n" +
                            "\n" +
                            "// Verify email after update\n" +
                            "String newEmail = \"newemail@test.com\";\n" +
                            "db.verifyColumnValue(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT email FROM users WHERE id = ?\",\n" +
                            "    new Object[]{123},\n" +
                            "    \"email\",\n" +
                            "    newEmail\n" +
                            ");\n" +
                            "System.out.println(\"✓ Email updated successfully to: \" + newEmail);",
            note = "- Throw AssertionError nếu query không trả về row nào\n" +
                    "- Throw AssertionError nếu column value không khớp với expectedValue\n" +
                    "- Chỉ verify first row nếu query trả về nhiều rows\n" +
                    "- Comparison sử dụng Objects.equals() nên type phải match\n" +
                    "- Nếu cần verify nhiều rows, nên dùng executeQuery và verify manually"
    )
    public void verifyColumnValue(String profileName, String query, Object[] params,
                                  String columnName, Object expectedValue) {
        executeWithLogging(
                "verifyColumnValue",
                profileName,
                query,
                params,
                () -> {
                    List<Map<String, Object>> results = executeQueryInternal(profileName, query, params);
                    if (results.isEmpty()) {
                        throw new AssertionError("No records found");
                    }

                    Object actualValue = results.get(0).get(columnName);
                    if (!Objects.equals(actualValue, expectedValue)) {
                        throw new AssertionError(
                                String.format("Expected column '%s' to be '%s' but was '%s'",
                                        columnName, expectedValue, actualValue)
                        );
                    }
                    return null;
                }
        );
    }

    // ========================================================================
    // DATA RETRIEVAL KEYWORDS
    // ========================================================================

    @NetatKeyword(
            name = "getScalarValue",
            description = "Lấy một giá trị scalar (single value) từ query - thường dùng cho COUNT, MAX, MIN, SUM, AVG",
            category = "Database",
            subCategory = "Data Retrieval",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu lệnh SELECT trả về 1 giá trị",
                    "params: Object... - Parameters cho query (tùy chọn)"
            },
            returnValue = "Object - Giá trị scalar (có thể là Integer, String, Date, etc. tùy query). Trả về null nếu không có result",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Đếm số lượng users\n" +
                            "Object countObj = db.getScalarValue(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT COUNT(*) FROM users\"\n" +
                            ");\n" +
                            "int totalUsers = ((Number) countObj).intValue();\n" +
                            "System.out.println(\"Total users: \" + totalUsers);\n" +
                            "\n" +
                            "// Lấy tên user theo ID\n" +
                            "String userName = (String) db.getScalarValue(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT name FROM users WHERE id = ?\",\n" +
                            "    123\n" +
                            ");\n" +
                            "System.out.println(\"User name: \" + userName);\n" +
                            "\n" +
                            "// Lấy max order amount\n" +
                            "Object maxAmountObj = db.getScalarValue(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT MAX(amount) FROM orders WHERE user_id = ?\",\n" +
                            "    123\n" +
                            ");\n" +
                            "double maxAmount = ((Number) maxAmountObj).doubleValue();\n" +
                            "System.out.println(\"Max order amount: $\" + maxAmount);\n" +
                            "\n" +
                            "// Lấy average rating\n" +
                            "Object avgObj = db.getScalarValue(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT AVG(rating) FROM reviews WHERE product_id = ?\",\n" +
                            "    456\n" +
                            ");\n" +
                            "if (avgObj != null) {\n" +
                            "    double avgRating = ((Number) avgObj).doubleValue();\n" +
                            "    System.out.println(\"Average rating: \" + String.format(\"%.2f\", avgRating));\n" +
                            "}",
            note = "- Trả về giá trị của column đầu tiên trong row đầu tiên\n" +
                    "- Trả về null nếu query không có result\n" +
                    "- Thích hợp cho aggregate functions (COUNT, SUM, AVG, MAX, MIN)\n" +
                    "- Nếu query trả về nhiều columns, chỉ lấy column đầu tiên\n" +
                    "- Type của return value tùy thuộc vào column type trong database\n" +
                    "- Cần cast về đúng type khi sử dụng (Integer, String, Double, etc.)"
    )
    public Object getScalarValue(String profileName, String query, Object... params) {
        return executeWithLogging(
                "getScalarValue",
                profileName,
                query,
                params,
                () -> {
                    List<Map<String, Object>> results = executeQueryInternal(profileName, query, params);
                    if (results.isEmpty()) {
                        return null;
                    }

                    Map<String, Object> firstRow = results.get(0);
                    if (firstRow.isEmpty()) {
                        return null;
                    }

                    return firstRow.values().iterator().next();
                }
        );
    }

    @NetatKeyword(
            name = "getColumnValues",
            description = "Lấy tất cả giá trị của một column cụ thể dưới dạng List",
            category = "Database",
            subCategory = "Data Retrieval",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu lệnh SELECT",
                    "columnName: String - Tên column cần lấy giá trị",
                    "params: Object... - Parameters cho query (tùy chọn)"
            },
            returnValue = "List<Object> - List các giá trị của column (có thể chứa null). Empty list nếu không có results",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Lấy danh sách email của active users\n" +
                            "List<Object> emails = db.getColumnValues(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT email FROM users WHERE status = ?\",\n" +
                            "    \"email\",\n" +
                            "    \"active\"\n" +
                            ");\n" +
                            "for (Object email : emails) {\n" +
                            "    System.out.println(\"Email: \" + email);\n" +
                            "}\n" +
                            "\n" +
                            "// Lấy danh sách IDs\n" +
                            "List<Object> orderIds = db.getColumnValues(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT id FROM orders WHERE user_id = ?\",\n" +
                            "    \"id\",\n" +
                            "    123\n" +
                            ");\n" +
                            "System.out.println(\"Found \" + orderIds.size() + \" orders\");\n" +
                            "\n" +
                            "// Lấy danh sách product names\n" +
                            "List<Object> productNames = db.getColumnValues(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT name FROM products WHERE category = ?\",\n" +
                            "    \"name\",\n" +
                            "    \"Electronics\"\n" +
                            ");\n" +
                            "\n" +
                            "// Check if list contains specific value\n" +
                            "if (emails.contains(\"john@test.com\")) {\n" +
                            "    System.out.println(\"✓ John's email found in active users\");\n" +
                            "}",
            note = "- Trả về List chứa giá trị của column từ tất cả rows\n" +
                    "- Trả về empty list [] nếu query không có result\n" +
                    "- List có thể chứa null values nếu column cho phép NULL\n" +
                    "- List có thể chứa duplicate values\n" +
                    "- Thứ tự trong list giống thứ tự rows trả về từ query"
    )
    public List<Object> getColumnValues(String profileName, String query, String columnName, Object... params) {
        return executeWithLogging(
                "getColumnValues",
                profileName,
                query,
                params,
                () -> {
                    List<Map<String, Object>> results = executeQueryInternal(profileName, query, params);
                    List<Object> values = new ArrayList<>();

                    for (Map<String, Object> row : results) {
                        values.add(row.get(columnName));
                    }

                    return values;
                }
        );
    }

    @NetatKeyword(
            name = "getRowCount",
            description = "Đếm số lượng rows từ query hoặc table. Tự động wrap với COUNT(*) nếu input là table name",
            category = "Database",
            subCategory = "Data Retrieval",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "queryOrTable: String - Câu SELECT query hoặc table name",
                    "params: Object... - Parameters cho query nếu có (tùy chọn)"
            },
            returnValue = "int - Số lượng rows. Trả về 0 nếu không có data",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Đếm rows từ query\n" +
                            "int activeCount = db.getRowCount(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM users WHERE status = ?\",\n" +
                            "    \"active\"\n" +
                            ");\n" +
                            "System.out.println(\"Active users: \" + activeCount);\n" +
                            "\n" +
                            "// Đếm rows từ table name (không cần viết SELECT COUNT)\n" +
                            "int totalUsers = db.getRowCount(\"mysql-dev\", \"users\");\n" +
                            "System.out.println(\"Total users in table: \" + totalUsers);\n" +
                            "\n" +
                            "// Đếm với nhiều conditions\n" +
                            "int completedOrders = db.getRowCount(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM orders WHERE user_id = ? AND status = ?\",\n" +
                            "    123,\n" +
                            "    \"completed\"\n" +
                            ");\n" +
                            "\n" +
                            "// So sánh counts\n" +
                            "int before = db.getRowCount(\"mysql-dev\", \"temp_table\");\n" +
                            "// ... perform some operations ...\n" +
                            "int after = db.getRowCount(\"mysql-dev\", \"temp_table\");\n" +
                            "System.out.println(\"Added \" + (after - before) + \" rows\");",
            note = "- Nếu input không bắt đầu bằng SELECT, sẽ tự động tạo query COUNT(*) cho table\n" +
                    "- Trả về 0 nếu table/query không có data\n" +
                    "- Hiệu suất tốt hơn so với executeQuery rồi check size\n" +
                    "- Có thể dùng với complex queries có WHERE, JOIN, etc."
    )
    public int getRowCount(String profileName, String queryOrTable, Object... params) {
        return executeWithLogging(
                "getRowCount",
                profileName,
                queryOrTable,
                params,
                () -> {
                    String query = queryOrTable;

                    // If it's just a table name, create COUNT query
                    if (!query.trim().toUpperCase().startsWith("SELECT")) {
                        query = "SELECT COUNT(*) FROM " + query;
                    }

                    Object count = getScalarValue(profileName, query, params);
                    return count != null ? ((Number) count).intValue() : 0;
                }
        );
    }

    // ========================================================================
    // TRANSACTION KEYWORDS
    // ========================================================================

    @NetatKeyword(
            name = "beginTransaction",
            description = "Bắt đầu một transaction với isolation level tùy chọn",
            category = "Database",
            subCategory = "Transaction",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "isolationLevel: String... - Isolation level (tùy chọn): READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE. Default: READ_COMMITTED"
            },
            returnValue = "void - Không trả về giá trị",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Begin transaction với default isolation (READ_COMMITTED)\n" +
                            "db.beginTransaction(\"mysql-dev\");\n" +
                            "try {\n" +
                            "    db.executeUpdate(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"UPDATE accounts SET balance = balance - ? WHERE id = ?\",\n" +
                            "        100.0,\n" +
                            "        1\n" +
                            "    );\n" +
                            "    db.executeUpdate(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"UPDATE accounts SET balance = balance + ? WHERE id = ?\",\n" +
                            "        100.0,\n" +
                            "        2\n" +
                            "    );\n" +
                            "    db.commitTransaction(\"mysql-dev\");\n" +
                            "    System.out.println(\"✓ Transaction committed successfully\");\n" +
                            "} catch (Exception e) {\n" +
                            "    db.rollbackTransaction(\"mysql-dev\", \"Error: \" + e.getMessage());\n" +
                            "    System.out.println(\"✗ Transaction rolled back\");\n" +
                            "}\n" +
                            "\n" +
                            "// Begin transaction với specific isolation level\n" +
                            "db.beginTransaction(\"mysql-dev\", \"SERIALIZABLE\");\n" +
                            "try {\n" +
                            "    // Critical operations requiring highest isolation\n" +
                            "    db.executeUpdate(\"mysql-dev\", \"UPDATE inventory SET stock = stock - 1 WHERE id = ?\", 123);\n" +
                            "    db.commitTransaction(\"mysql-dev\");\n" +
                            "} catch (Exception e) {\n" +
                            "    db.rollbackTransaction(\"mysql-dev\");\n" +
                            "}",
            note = "- Connection sẽ được lưu trong ThreadLocal cho các operations tiếp theo\n" +
                    "- Auto-commit sẽ bị tắt cho đến khi commit hoặc rollback\n" +
                    "- Phải gọi commitTransaction hoặc rollbackTransaction để kết thúc transaction\n" +
                    "- Isolation levels: READ_UNCOMMITTED (lowest), READ_COMMITTED (default), REPEATABLE_READ, SERIALIZABLE (highest)\n" +
                    "- Nên sử dụng try-catch-finally để đảm bảo transaction được close"
    )
    public void beginTransaction(String profileName, String... isolationLevel) {
        String isolation = isolationLevel.length > 0 ? isolationLevel[0] : "READ_COMMITTED";

        executeWithLogging(
                "beginTransaction",
                profileName,
                "BEGIN TRANSACTION",
                new Object[]{isolation},
                () -> {
                    // ✅ Log transaction begin
                    dbLogger.logTransactionBegin(profileName, isolation);

                    Connection conn = ConnectionManager.getConnection(profileName);
                    conn.setAutoCommit(false);

                    // Set isolation level if specified
                    if (isolationLevel.length > 0) {
                        int level = mapIsolationLevel(isolationLevel[0]);
                        conn.setTransactionIsolation(level);
                    }

                    // Store connection in context for subsequent operations
                    storeConnection(profileName, conn);

                    return null;
                }
        );
    }

    @NetatKeyword(
            name = "commitTransaction",
            description = "Commit transaction hiện tại, lưu tất cả thay đổi vào database",
            category = "Database",
            subCategory = "Transaction",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình"
            },
            returnValue = "void - Không trả về giá trị",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Simple transaction\n" +
                            "db.beginTransaction(\"mysql-dev\");\n" +
                            "db.executeUpdate(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"INSERT INTO orders (user_id, amount) VALUES (?, ?)\",\n" +
                            "    123,\n" +
                            "    99.99\n" +
                            ");\n" +
                            "db.commitTransaction(\"mysql-dev\");\n" +
                            "System.out.println(\"✓ Order created\");\n" +
                            "\n" +
                            "// Transaction with multiple operations\n" +
                            "db.beginTransaction(\"mysql-dev\");\n" +
                            "try {\n" +
                            "    // Insert order\n" +
                            "    int orderId = db.executeUpdate(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"INSERT INTO orders (user_id, total) VALUES (?, ?)\",\n" +
                            "        123, 199.99\n" +
                            "    );\n" +
                            "    \n" +
                            "    // Insert order items\n" +
                            "    db.executeUpdate(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"INSERT INTO order_items (order_id, product_id, quantity) VALUES (?, ?, ?)\",\n" +
                            "        orderId, 456, 2\n" +
                            "    );\n" +
                            "    \n" +
                            "    // Update inventory\n" +
                            "    db.executeUpdate(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"UPDATE products SET stock = stock - ? WHERE id = ?\",\n" +
                            "        2, 456\n" +
                            "    );\n" +
                            "    \n" +
                            "    db.commitTransaction(\"mysql-dev\");\n" +
                            "    System.out.println(\"✓ Order and inventory updated\");\n" +
                            "} catch (Exception e) {\n" +
                            "    db.rollbackTransaction(\"mysql-dev\");\n" +
                            "    System.err.println(\"✗ Transaction failed: \" + e.getMessage());\n" +
                            "    throw e;\n" +
                            "}",
            note = "- Tất cả thay đổi trong transaction sẽ được persist vào database\n" +
                    "- Connection sẽ được đóng và remove khỏi ThreadLocal\n" +
                    "- Auto-commit sẽ được bật lại sau khi commit\n" +
                    "- Throw IllegalStateException nếu không có active transaction\n" +
                    "- Transaction duration và operation count sẽ được log"
    )
    public void commitTransaction(String profileName) {
        long startTime = System.currentTimeMillis();

        executeWithLogging(
                "commitTransaction",
                profileName,
                "COMMIT",
                new Object[0],
                () -> {
                    Connection conn = getStoredConnection(profileName);
                    if (conn == null) {
                        throw new IllegalStateException("No active transaction for profile: " + profileName);
                    }

                    conn.commit();
                    conn.setAutoCommit(true);

                    // ✅ Log transaction commit
                    long duration = System.currentTimeMillis() - startTime;
                    dbLogger.logTransactionCommit(profileName, duration, getOperationCount(profileName));

                    // Cleanup
                    removeConnection(profileName);
                    conn.close();

                    return null;
                }
        );
    }

    @NetatKeyword(
            name = "rollbackTransaction",
            description = "Rollback transaction hiện tại, hủy bỏ tất cả thay đổi",
            category = "Database",
            subCategory = "Transaction",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "reason: String... - Lý do rollback (tùy chọn, dùng cho logging)"
            },
            returnValue = "void - Không trả về giá trị",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Rollback on error\n" +
                            "db.beginTransaction(\"mysql-dev\");\n" +
                            "try {\n" +
                            "    db.executeUpdate(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"UPDATE accounts SET balance = balance - ? WHERE id = ?\",\n" +
                            "        1000.0,\n" +
                            "        1\n" +
                            "    );\n" +
                            "    \n" +
                            "    // Check balance\n" +
                            "    Object balance = db.getScalarValue(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"SELECT balance FROM accounts WHERE id = ?\",\n" +
                            "        1\n" +
                            "    );\n" +
                            "    \n" +
                            "    if (((Number) balance).doubleValue() < 0) {\n" +
                            "        db.rollbackTransaction(\"mysql-dev\", \"Insufficient balance\");\n" +
                            "        System.out.println(\"✗ Transaction cancelled: Insufficient funds\");\n" +
                            "        return;\n" +
                            "    }\n" +
                            "    \n" +
                            "    db.executeUpdate(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"UPDATE accounts SET balance = balance + ? WHERE id = ?\",\n" +
                            "        1000.0,\n" +
                            "        2\n" +
                            "    );\n" +
                            "    \n" +
                            "    db.commitTransaction(\"mysql-dev\");\n" +
                            "} catch (Exception e) {\n" +
                            "    db.rollbackTransaction(\"mysql-dev\", \"Error: \" + e.getMessage());\n" +
                            "    System.err.println(\"✗ Transaction rolled back due to error\");\n" +
                            "}\n" +
                            "\n" +
                            "// Manual rollback for business logic\n" +
                            "db.beginTransaction(\"mysql-dev\");\n" +
                            "db.executeUpdate(\"mysql-dev\", \"INSERT INTO temp_data VALUES (?)\", \"test\");\n" +
                            "// Decide to cancel\n" +
                            "db.rollbackTransaction(\"mysql-dev\", \"Manual cancellation\");",
            note = "- Tất cả thay đổi trong transaction sẽ bị hủy bỏ\n" +
                    "- Connection sẽ được đóng và remove khỏi ThreadLocal\n" +
                    "- Auto-commit sẽ được bật lại sau khi rollback\n" +
                    "- Throw IllegalStateException nếu không có active transaction\n" +
                    "- Reason parameter sẽ được log để track lý do rollback"
    )
    public void rollbackTransaction(String profileName, String... reason) {
        String rollbackReason = reason.length > 0 ? reason[0] : "Manual rollback";

        executeWithLogging(
                "rollbackTransaction",
                profileName,
                "ROLLBACK",
                new Object[]{rollbackReason},
                () -> {
                    Connection conn = getStoredConnection(profileName);
                    if (conn == null) {
                        throw new IllegalStateException("No active transaction for profile: " + profileName);
                    }

                    conn.rollback();
                    conn.setAutoCommit(true);

                    // ✅ Log transaction rollback
                    dbLogger.logTransactionRollback(profileName, rollbackReason);

                    // Cleanup
                    removeConnection(profileName);
                    conn.close();

                    return null;
                }
        );
    }

    // ========================================================================
    // CONNECTION MANAGEMENT KEYWORDS
    // ========================================================================

    @NetatKeyword(
            name = "checkConnection",
            description = "Kiểm tra connection có hoạt động không bằng cách thực thi simple query",
            category = "Database",
            subCategory = "Connection Management",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình"
            },
            returnValue = "boolean - true nếu connection hoạt động, false nếu không",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Check connection trước khi execute queries\n" +
                            "if (db.checkConnection(\"mysql-dev\")) {\n" +
                            "    System.out.println(\"✓ Database connection is healthy\");\n" +
                            "    // Proceed with queries\n" +
                            "} else {\n" +
                            "    System.out.println(\"✗ Database connection failed\");\n" +
                            "    // Handle connection error\n" +
                            "}\n" +
                            "\n" +
                            "// Health check trong loop\n" +
                            "for (int i = 0; i < 3; i++) {\n" +
                            "    boolean isConnected = db.checkConnection(\"mysql-dev\");\n" +
                            "    System.out.println(\"Attempt \" + (i + 1) + \": \" + (isConnected ? \"OK\" : \"FAILED\"));\n" +
                            "    if (isConnected) break;\n" +
                            "    Thread.sleep(1000); // Wait before retry\n" +
                            "}\n" +
                            "\n" +
                            "// Check multiple profiles\n" +
                            "String[] profiles = {\"mysql-dev\", \"postgres-prod\", \"oracle-test\"};\n" +
                            "for (String profile : profiles) {\n" +
                            "    boolean status = db.checkConnection(profile);\n" +
                            "    System.out.println(profile + \": \" + (status ? \"✓\" : \"✗\"));\n" +
                            "}",
            note = "- Thực thi simple query (SELECT 1) để test connection\n" +
                    "- Trả về false nếu có SQLException, không throw exception\n" +
                    "- Hữu ích cho health checks và pre-flight validations\n" +
                    "- Có thể dùng để verify connection sau khi reconnect"
    )
    public boolean checkConnection(String profileName) {
        return executeWithLogging(
                "checkConnection",
                profileName,
                "SELECT 1",
                new Object[0],
                () -> {
                    try (Connection conn = ConnectionManager.getConnection(profileName);
                         Statement stmt = conn.createStatement()) {

                        ResultSet rs = stmt.executeQuery("SELECT 1");
                        return rs.next();

                    } catch (SQLException e) {
                        return false;
                    }
                }
        );
    }

    @NetatKeyword(
            name = "getConnectionPoolStats",
            description = "Lấy thống kê connection pool (size, active, idle, waiting threads)",
            category = "Database",
            subCategory = "Connection Management",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình"
            },
            returnValue = "Map<String, Object> - Map chứa pool statistics (poolSize, activeConnections, idleConnections, waitingThreads, utilizationPercent)",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Lấy và hiển thị pool stats\n" +
                            "Map<String, Object> stats = db.getConnectionPoolStats(\"mysql-dev\");\n" +
                            "System.out.println(\"Pool Size: \" + stats.get(\"poolSize\"));\n" +
                            "System.out.println(\"Active: \" + stats.get(\"activeConnections\"));\n" +
                            "System.out.println(\"Idle: \" + stats.get(\"idleConnections\"));\n" +
                            "System.out.println(\"Waiting: \" + stats.get(\"waitingThreads\"));\n" +
                            "System.out.println(\"Utilization: \" + stats.get(\"utilizationPercent\") + \"%\");\n" +
                            "\n" +
                            "// Check if pool is healthy\n" +
                            "double utilization = (Double) stats.get(\"utilizationPercent\");\n" +
                            "if (utilization > 80) {\n" +
                            "    System.out.println(\"⚠️  Warning: High pool utilization!\");\n" +
                            "} else if (utilization > 90) {\n" +
                            "    System.out.println(\"🔥 Critical: Pool nearly exhausted!\");\n" +
                            "}\n" +
                            "\n" +
                            "// Monitor pool during load test\n" +
                            "for (int i = 0; i < 100; i++) {\n" +
                            "    db.executeQuery(\"mysql-dev\", \"SELECT * FROM users LIMIT 10\");\n" +
                            "    \n" +
                            "    if (i % 10 == 0) {\n" +
                            "        Map<String, Object> currentStats = db.getConnectionPoolStats(\"mysql-dev\");\n" +
                            "        System.out.println(\"Iteration \" + i + \": \" + \n" +
                            "            currentStats.get(\"activeConnections\") + \"/\" + \n" +
                            "            currentStats.get(\"poolSize\") + \" active\");\n" +
                            "    }\n" +
                            "}",
            note = "- Pool statistics sẽ được log tự động\n" +
                    "- Warning log nếu utilization > 80%\n" +
                    "- Hữu ích cho monitoring và capacity planning\n" +
                    "- Có thể integrate với monitoring tools (Prometheus, Grafana)"
    )
    public Map<String, Object> getConnectionPoolStats(String profileName) {
        return executeWithLogging(
                "getConnectionPoolStats",
                profileName,
                "GET_POOL_STATS",
                new Object[0],
                () -> {
                    PoolStats stats = ConnectionManager.getPoolStats(profileName);

                    // ✅ Log pool stats
                    dbLogger.logConnectionPoolStats(profileName, stats);

                    Map<String, Object> result = new HashMap<>();
                    result.put("poolSize", stats.getPoolSize());
                    result.put("activeConnections", stats.getActiveConnections());
                    result.put("idleConnections", stats.getIdleConnections());
                    result.put("waitingThreads", stats.getWaitingThreads());
                    result.put("utilizationPercent", stats.getUtilizationPercent());

                    return result;
                }
        );
    }

    @NetatKeyword(
            name = "waitForConnectionAvailable",
            description = "Đợi cho đến khi có connection available trong pool (polling với timeout)",
            category = "Database",
            subCategory = "Connection Management",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "timeoutMs: long - Timeout trong milliseconds"
            },
            returnValue = "void - Không trả về giá trị (throw DatabaseException nếu timeout)",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Wait for connection với 5 second timeout\n" +
                            "try {\n" +
                            "    db.waitForConnectionAvailable(\"mysql-dev\", 5000);\n" +
                            "    System.out.println(\"✓ Connection available\");\n" +
                            "    // Proceed với operations\n" +
                            "} catch (DatabaseException e) {\n" +
                            "    System.out.println(\"✗ Timeout: No connections available\");\n" +
                            "}\n" +
                            "\n" +
                            "// Trong high load scenario\n" +
                            "for (int i = 0; i < 1000; i++) {\n" +
                            "    // Wait before each operation\n" +
                            "    db.waitForConnectionAvailable(\"mysql-dev\", 10000);\n" +
                            "    db.executeQuery(\"mysql-dev\", \"SELECT * FROM users WHERE id = ?\", i);\n" +
                            "}\n" +
                            "\n" +
                            "// Retry logic với exponential backoff\n" +
                            "int maxRetries = 3;\n" +
                            "for (int retry = 0; retry < maxRetries; retry++) {\n" +
                            "    try {\n" +
                            "        long timeout = (long) (1000 * Math.pow(2, retry)); // 1s, 2s, 4s\n" +
                            "        db.waitForConnectionAvailable(\"mysql-dev\", timeout);\n" +
                            "        // Execute operation\n" +
                            "        break;\n" +
                            "    } catch (DatabaseException e) {\n" +
                            "        if (retry == maxRetries - 1) throw e;\n" +
                            "        System.out.println(\"Retry \" + (retry + 1) + \" failed, waiting...\");\n" +
                            "    }\n" +
                            "}",
            note = "- Polling mỗi 100ms để check idle connections\n" +
                    "- Throw DatabaseException nếu không có connection available sau timeout\n" +
                    "- Hữu ích để tránh connection timeout errors trong high load\n" +
                    "- Có thể combine với retry logic cho reliability"
    )
    public void waitForConnectionAvailable(String profileName, long timeoutMs) {
        executeWithLogging(
                "waitForConnectionAvailable",
                profileName,
                "WAIT_FOR_CONNECTION",
                new Object[]{timeoutMs},
                () -> {
                    long startTime = System.currentTimeMillis();

                    while (System.currentTimeMillis() - startTime < timeoutMs) {
                        PoolStats stats = ConnectionManager.getPoolStats(profileName);
                        if (stats.getIdleConnections() > 0) {
                            return null;
                        }

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                    throw GenericDatabaseException.builder()
                            .message("Timeout waiting for available connection after " + timeoutMs + "ms")
                            .profileName(profileName)
                            .severity(ErrorSeverity.ERROR)
                            .retryable(true)
                            .build();
                }
        );
    }

    // ========================================================================
    // TABLE MANAGEMENT KEYWORDS
    // ========================================================================

    @NetatKeyword(
            name = "truncateTable",
            description = "Xóa tất cả data trong table (TRUNCATE) - nhanh hơn DELETE và reset auto-increment",
            category = "Database",
            subCategory = "Table Management",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "tableName: String - Tên table cần truncate"
            },
            returnValue = "void - Không trả về giá trị",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Truncate table trước test\n" +
                            "db.truncateTable(\"mysql-dev\", \"temp_users\");\n" +
                            "System.out.println(\"✓ Table truncated\");\n" +
                            "\n" +
                            "// Cleanup multiple tables\n" +
                            "String[] tables = {\"sessions\", \"temp_data\", \"cache\"};\n" +
                            "for (String table : tables) {\n" +
                            "    db.truncateTable(\"mysql-dev\", table);\n" +
                            "    System.out.println(\"Truncated: \" + table);\n" +
                            "}\n" +
                            "\n" +
                            "// Truncate và verify\n" +
                            "db.truncateTable(\"mysql-dev\", \"test_orders\");\n" +
                            "int count = db.getRowCount(\"mysql-dev\", \"test_orders\");\n" +
                            "if (count == 0) {\n" +
                            "    System.out.println(\"✓ Table successfully truncated\");\n" +
                            "}\n" +
                            "\n" +
                            "// Trong test teardown\n" +
                            "try {\n" +
                            "    db.truncateTable(\"mysql-dev\", \"test_results\");\n" +
                            "} catch (DatabaseException e) {\n" +
                            "    System.err.println(\"Warning: Could not truncate table - \" + e.getMessage());\n" +
                            "}",
            note = "- TRUNCATE nhanh hơn DELETE vì không log từng row\n" +
                    "- Auto-increment counter sẽ được reset về 1\n" +
                    "- Không thể rollback trong transaction (DDL statement)\n" +
                    "- Throw exception nếu table có foreign key constraints\n" +
                    "- Thích hợp cho cleanup test data"
    )
    public void truncateTable(String profileName, String tableName) {
        String query = "TRUNCATE TABLE " + tableName;
        executeWithLogging(
                "truncateTable",
                profileName,
                query,
                new Object[0],
                () -> {
                    executeUpdateInternal(profileName, query);
                    return null;
                }
        );
    }

    @NetatKeyword(
            name = "dropTable",
            description = "Xóa table khỏi database (DROP TABLE IF EXISTS)",
            category = "Database",
            subCategory = "Table Management",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "tableName: String - Tên table cần drop"
            },
            returnValue = "void - Không trả về giá trị",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Drop table an toàn (không throw error nếu không tồn tại)\n" +
                            "db.dropTable(\"mysql-dev\", \"temp_users\");\n" +
                            "System.out.println(\"✓ Table dropped\");\n" +
                            "\n" +
                            "// Drop multiple tables\n" +
                            "String[] tables = {\"test_table1\", \"test_table2\", \"test_table3\"};\n" +
                            "for (String table : tables) {\n" +
                            "    db.dropTable(\"mysql-dev\", table);\n" +
                            "    System.out.println(\"Dropped: \" + table);\n" +
                            "}\n" +
                            "\n" +
                            "// Drop và verify\n" +
                            "db.dropTable(\"mysql-dev\", \"old_data\");\n" +
                            "boolean exists = db.tableExists(\"mysql-dev\", \"old_data\");\n" +
                            "if (!exists) {\n" +
                            "    System.out.println(\"✓ Table successfully dropped\");\n" +
                            "}\n" +
                            "\n" +
                            "// Test cleanup - drop all temp tables\n" +
                            "List<Object> tempTables = db.getColumnValues(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT table_name FROM information_schema.tables WHERE table_name LIKE 'temp_%'\",\n" +
                            "    \"table_name\"\n" +
                            ");\n" +
                            "for (Object tableName : tempTables) {\n" +
                            "    db.dropTable(\"mysql-dev\", tableName.toString());\n" +
                            "}",
            note = "- Sử dụng IF EXISTS nên không throw error nếu table không tồn tại\n" +
                    "- Xóa cả structure và data của table\n" +
                    "- Không thể rollback (DDL statement)\n" +
                    "- Cẩn thận khi drop table có foreign key references\n" +
                    "- Thích hợp cho cleanup sau integration tests"
    )
    public void dropTable(String profileName, String tableName) {
        String query = "DROP TABLE IF EXISTS " + tableName;
        executeWithLogging(
                "dropTable",
                profileName,
                query,
                new Object[0],
                () -> {
                    executeUpdateInternal(profileName, query);
                    return null;
                }
        );
    }

    @NetatKeyword(
            name = "tableExists",
            description = "Kiểm tra table có tồn tại trong database không",
            category = "Database",
            subCategory = "Table Management",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "tableName: String - Tên table cần check"
            },
            returnValue = "boolean - true nếu table tồn tại, false nếu không",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Check table existence\n" +
                            "if (db.tableExists(\"mysql-dev\", \"users\")) {\n" +
                            "    System.out.println(\"✓ Table 'users' exists\");\n" +
                            "} else {\n" +
                            "    System.out.println(\"✗ Table 'users' does not exist\");\n" +
                            "}\n" +
                            "\n" +
                            "// Conditional table creation\n" +
                            "if (!db.tableExists(\"mysql-dev\", \"test_data\")) {\n" +
                            "    String createTable = \n" +
                            "        \"CREATE TABLE test_data (\" +\n" +
                            "        \"    id INT PRIMARY KEY AUTO_INCREMENT,\" +\n" +
                            "        \"    value VARCHAR(100)\" +\n" +
                            "        \")\";\n" +
                            "    db.executeUpdate(\"mysql-dev\", createTable);\n" +
                            "    System.out.println(\"✓ Table created\");\n" +
                            "}\n" +
                            "\n" +
                            "// Verify table after creation\n" +
                            "db.executeScript(\"mysql-dev\", \"CREATE TABLE IF NOT EXISTS new_table (id INT)\");\n" +
                            "boolean created = db.tableExists(\"mysql-dev\", \"new_table\");\n" +
                            "System.out.println(\"Table created: \" + created);\n" +
                            "\n" +
                            "// Check multiple tables\n" +
                            "String[] requiredTables = {\"users\", \"orders\", \"products\"};\n" +
                            "boolean allExist = true;\n" +
                            "for (String table : requiredTables) {\n" +
                            "    if (!db.tableExists(\"mysql-dev\", table)) {\n" +
                            "        System.out.println(\"Missing table: \" + table);\n" +
                            "        allExist = false;\n" +
                            "    }\n" +
                            "}\n" +
                            "if (allExist) {\n" +
                            "    System.out.println(\"✓ All required tables exist\");\n" +
                            "}",
            note = "- Sử dụng DatabaseMetaData để check table existence\n" +
                    "- Case-sensitive trên một số databases (Linux MySQL)\n" +
                    "- Không throw exception, chỉ return true/false\n" +
                    "- Hữu ích cho pre-flight checks và conditional logic"
    )
    public boolean tableExists(String profileName, String tableName) {
        return executeWithLogging(
                "tableExists",
                profileName,
                "CHECK_TABLE_EXISTS",
                new Object[]{tableName},
                () -> {
                    try (Connection conn = ConnectionManager.getConnection(profileName)) {
                        DatabaseMetaData metadata = conn.getMetaData();
                        ResultSet rs = metadata.getTables(null, null, tableName, new String[]{"TABLE"});
                        return rs.next();
                    } catch (SQLException e) {
                        throw SqlStateMapper.mapException(e, null, null, profileName);
                    }
                }
        );
    }

    @NetatKeyword(
            name = "getTableColumns",
            description = "Lấy danh sách tất cả columns của table",
            category = "Database",
            subCategory = "Table Management",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "tableName: String - Tên table"
            },
            returnValue = "List<String> - List tên các columns trong table",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Lấy danh sách columns\n" +
                            "List<String> columns = db.getTableColumns(\"mysql-dev\", \"users\");\n" +
                            "System.out.println(\"Columns in users table:\");\n" +
                            "for (String column : columns) {\n" +
                            "    System.out.println(\"  - \" + column);\n" +
                            "}\n" +
                            "\n" +
                            "// Check if column exists\n" +
                            "List<String> userColumns = db.getTableColumns(\"mysql-dev\", \"users\");\n" +
                            "if (userColumns.contains(\"email\")) {\n" +
                            "    System.out.println(\"✓ Email column exists\");\n" +
                            "}\n" +
                            "\n" +
                            "// Dynamic query building\n" +
                            "List<String> cols = db.getTableColumns(\"mysql-dev\", \"products\");\n" +
                            "String query = \"SELECT \" + String.join(\", \", cols) + \" FROM products\";\n" +
                            "List<Map<String, Object>> results = db.executeQuery(\"mysql-dev\", query);\n" +
                            "\n" +
                            "// Validate table structure\n" +
                            "List<String> expectedColumns = Arrays.asList(\"id\", \"name\", \"email\", \"created_at\");\n" +
                            "List<String> actualColumns = db.getTableColumns(\"mysql-dev\", \"users\");\n" +
                            "if (actualColumns.containsAll(expectedColumns)) {\n" +
                            "    System.out.println(\"✓ Table structure is correct\");\n" +
                            "} else {\n" +
                            "    System.out.println(\"✗ Missing columns\");\n" +
                            "}",
            note = "- Sử dụng DatabaseMetaData để lấy column information\n" +
                    "- Trả về empty list nếu table không tồn tại\n" +
                    "- Column names được return theo thứ tự định nghĩa trong table\n" +
                    "- Hữu ích cho schema validation và dynamic query building"
    )
    public List<String> getTableColumns(String profileName, String tableName) {
        return executeWithLogging(
                "getTableColumns",
                profileName,
                "GET_TABLE_COLUMNS",
                new Object[]{tableName},
                () -> {
                    List<String> columns = new ArrayList<>();

                    try (Connection conn = ConnectionManager.getConnection(profileName)) {
                        DatabaseMetaData metadata = conn.getMetaData();
                        ResultSet rs = metadata.getColumns(null, null, tableName, null);

                        while (rs.next()) {
                            columns.add(rs.getString("COLUMN_NAME"));
                        }

                        return columns;
                    } catch (SQLException e) {
                        throw SqlStateMapper.mapException(e, null, null, profileName);
                    }
                }
        );
    }

    // ========================================================================
    // UTILITY KEYWORDS
    // ========================================================================

    @NetatKeyword(
            name = "waitForRowCount",
            description = "Đợi cho đến khi row count đạt expected value (polling với timeout) - hữu ích cho async operations",
            category = "Database",
            subCategory = "Utility",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu SELECT query",
                    "expectedCount: int - Số lượng rows mong đợi",
                    "timeoutMs: long - Timeout trong milliseconds",
                    "params: Object... - Parameters cho query (tùy chọn)"
            },
            returnValue = "void - Không trả về giá trị (throw AssertionError nếu timeout)",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Đợi async insert hoàn thành\n" +
                            "// Trigger async operation\n" +
                            "triggerAsyncDataImport();\n" +
                            "\n" +
                            "// Wait for 100 records to be inserted\n" +
                            "db.waitForRowCount(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM imported_data\",\n" +
                            "    100,\n" +
                            "    30000 // 30 second timeout\n" +
                            ");\n" +
                            "System.out.println(\"✓ All records imported\");\n" +
                            "\n" +
                            "// Wait for queue to be processed\n" +
                            "db.waitForRowCount(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM queue WHERE status = ?\",\n" +
                            "    0, // Wait until queue is empty\n" +
                            "    60000,\n" +
                            "    \"pending\"\n" +
                            ");\n" +
                            "System.out.println(\"✓ Queue processed\");\n" +
                            "\n" +
                            "// Wait for specific user's orders\n" +
                            "int userId = 123;\n" +
                            "db.waitForRowCount(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM orders WHERE user_id = ? AND status = ?\",\n" +
                            "    5,\n" +
                            "    10000,\n" +
                            "    userId,\n" +
                            "    \"completed\"\n" +
                            ");\n" +
                            "\n" +
                            "// With error handling\n" +
                            "try {\n" +
                            "    db.waitForRowCount(\n" +
                            "        \"mysql-dev\",\n" +
                            "        \"SELECT * FROM async_results\",\n" +
                            "        10,\n" +
                            "        5000\n" +
                            "    );\n" +
                            "} catch (AssertionError e) {\n" +
                            "    System.out.println(\"Timeout: Expected count not reached\");\n" +
                            "}",
            note = "- Polling mỗi 500ms để check row count\n" +
                    "- Throw AssertionError nếu không đạt expected count sau timeout\n" +
                    "- Hữu ích cho testing async operations, message queues, background jobs\n" +
                    "- Có thể dùng expectedCount = 0 để wait until empty"
    )
    public void waitForRowCount(String profileName, String query, int expectedCount,
                                long timeoutMs, Object... params) {
        executeWithLogging(
                "waitForRowCount",
                profileName,
                query,
                params,
                () -> {
                    long startTime = System.currentTimeMillis();

                    while (System.currentTimeMillis() - startTime < timeoutMs) {
                        List<Map<String, Object>> results = executeQueryInternal(profileName, query, params);
                        if (results.size() == expectedCount) {
                            return null;
                        }

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                    throw new AssertionError(
                            String.format("Timeout waiting for row count %d", expectedCount)
                    );
                }
        );
    }

    @NetatKeyword(
            name = "compareQueryResults",
            description = "So sánh kết quả của 2 queries - kiểm tra cả số lượng rows và content",
            category = "Database",
            subCategory = "Utility",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query1: String - Query thứ nhất",
                    "query2: String - Query thứ hai"
            },
            returnValue = "boolean - true nếu kết quả giống nhau, false nếu khác",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// So sánh data giữa 2 tables\n" +
                            "boolean isSame = db.compareQueryResults(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM users ORDER BY id\",\n" +
                            "    \"SELECT * FROM users_backup ORDER BY id\"\n" +
                            ");\n" +
                            "if (isSame) {\n" +
                            "    System.out.println(\"✓ Tables are identical\");\n" +
                            "} else {\n" +
                            "    System.out.println(\"✗ Tables differ\");\n" +
                            "}\n" +
                            "\n" +
                            "// Verify data migration\n" +
                            "boolean migrated = db.compareQueryResults(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT id, name, email FROM old_schema.users\",\n" +
                            "    \"SELECT id, name, email FROM new_schema.users\"\n" +
                            ");\n" +
                            "System.out.println(\"Migration successful: \" + migrated);\n" +
                            "\n" +
                            "// Compare aggregations\n" +
                            "boolean countsMatch = db.compareQueryResults(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT status, COUNT(*) as cnt FROM orders GROUP BY status\",\n" +
                            "    \"SELECT status, COUNT(*) as cnt FROM orders_archive GROUP BY status\"\n" +
                            ");\n" +
                            "\n" +
                            "// Assert results match\n" +
                            "if (!db.compareQueryResults(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM expected_results\",\n" +
                            "    \"SELECT * FROM actual_results\"\n" +
                            ")) {\n" +
                            "    throw new AssertionError(\"Query results do not match!\");\n" +
                            "}",
            note = "- So sánh cả row count và content của từng row\n" +
                    "- Trả về false nếu số lượng rows khác nhau\n" +
                    "- Trả về false nếu bất kỳ row nào khác nhau\n" +
                    "- Queries nên có ORDER BY để đảm bảo consistent ordering\n" +
                    "- Hữu ích cho data migration verification và regression testing"
    )
    public boolean compareQueryResults(String profileName, String query1, String query2) {
        return executeWithLogging(
                "compareQueryResults",
                profileName,
                query1 + " vs " + query2,
                new Object[0],
                () -> {
                    List<Map<String, Object>> results1 = executeQueryInternal(profileName, query1);
                    List<Map<String, Object>> results2 = executeQueryInternal(profileName, query2);

                    if (results1.size() != results2.size()) {
                        return false;
                    }

                    return results1.equals(results2);
                }
        );
    }

    @NetatKeyword(
            name = "exportQueryToCSV",
            description = "Export kết quả query ra file CSV - hữu ích cho data extraction và reporting",
            category = "Database",
            subCategory = "Utility",
            parameters = {
                    "profileName: String - Tên database profile đã cấu hình",
                    "query: String - Câu SELECT query",
                    "filePath: String - Đường dẫn file CSV output",
                    "params: Object... - Parameters cho query (tùy chọn)"
            },
            returnValue = "void - Không trả về giá trị",
            example =
                    "DatabaseKeyword db = new DatabaseKeyword();\n" +
                            "\n" +
                            "// Export tất cả users ra CSV\n" +
                            "db.exportQueryToCSV(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT id, name, email, created_at FROM users\",\n" +
                            "    \"/tmp/users_export.csv\"\n" +
                            ");\n" +
                            "System.out.println(\"✓ Users exported to CSV\");\n" +
                            "\n" +
                            "// Export với filtering\n" +
                            "db.exportQueryToCSV(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM orders WHERE created_at >= ?\",\n" +
                            "    \"./reports/orders_2025.csv\",\n" +
                            "    \"2025-01-01\"\n" +
                            ");\n" +
                            "\n" +
                            "// Export aggregated data\n" +
                            "db.exportQueryToCSV(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT category, COUNT(*) as total, AVG(price) as avg_price \" +\n" +
                            "    \"FROM products GROUP BY category\",\n" +
                            "    \"./reports/product_summary.csv\"\n" +
                            ");\n" +
                            "\n" +
                            "// Generate report với timestamp\n" +
                            "String timestamp = new SimpleDateFormat(\"yyyyMMdd_HHmmss\").format(new Date());\n" +
                            "String reportPath = \"./reports/daily_stats_\" + timestamp + \".csv\";\n" +
                            "db.exportQueryToCSV(\n" +
                            "    \"mysql-dev\",\n" +
                            "    \"SELECT * FROM daily_statistics WHERE date = CURDATE()\",\n" +
                            "    reportPath\n" +
                            ");\n" +
                            "System.out.println(\"Report generated: \" + reportPath);",
            note = "- File CSV sẽ có header row với column names\n" +
                    "- Values được comma-separated, null values hiển thị là empty string\n" +
                    "- File sẽ bị overwrite nếu đã tồn tại\n" +
                    "- Throw exception nếu không thể write file\n" +
                    "- Hữu ích cho data export, reporting, và data analysis"
    )
    public void exportQueryToCSV(String profileName, String query, String filePath, Object... params) {
        executeWithLogging(
                "exportQueryToCSV",
                profileName,
                query,
                params,
                () -> {
                    List<Map<String, Object>> results = executeQueryInternal(profileName, query, params);

                    try (java.io.PrintWriter writer = new java.io.PrintWriter(filePath)) {
                        if (!results.isEmpty()) {
                            // Write header
                            String header = String.join(",", results.get(0).keySet());
                            writer.println(header);

                            // Write rows
                            for (Map<String, Object> row : results) {
                                String line = String.join(",",
                                        row.values().stream()
                                                .map(v -> v != null ? v.toString() : "")
                                                .toArray(String[]::new)
                                );
                                writer.println(line);
                            }
                        }
                    }

                    return null;
                }
        );
    }

    // ========================================================================
    // INTERNAL HELPER METHODS
    // ========================================================================

    /**
     * Generic method to execute operations with logging.
     */
    /**
     * Generic method to execute operations with logging.
     */
    private <T> T executeWithLogging(String keywordName, String profileName, String query,
                                     Object[] params, DatabaseOperation<T> operation) {
        LogContext.setDatabaseContext(profileName, getCurrentTestCase(), keywordName);

        long startTime = System.currentTimeMillis();

        dbLogger.logQueryStart(profileName, query, params);

        try {
            T result = operation.execute();

            long duration = System.currentTimeMillis() - startTime;
            int rowsAffected = calculateRowsAffected(result);
            dbLogger.logQuerySuccess(profileName, query, params, duration, rowsAffected);

            return result;

        } catch (DatabaseException e) {
            long duration = System.currentTimeMillis() - startTime;
            dbLogger.logQueryFailure(profileName, query, params, duration, e);
            throw e;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            DatabaseException dbEx = GenericDatabaseException.builder()
                    .message("Unexpected error: " + e.getMessage())
                    .cause(e)
                    .profileName(profileName)
                    .query(query)
                    .parameters(params)
                    .severity(ErrorSeverity.ERROR)
                    .retryable(false)
                    .build();

            dbLogger.logQueryFailure(profileName, query, params, duration, dbEx);
            throw dbEx;

        } finally {
            LogContext.clear();
        }
    }

    /**
     * Internal query execution without logging (used by logged methods).
     */
    private List<Map<String, Object>> executeQueryInternal(String profileName, String query, Object... params) {
        try (Connection conn = ConnectionManager.getConnection(profileName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Set parameters
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }

            // Execute query
            ResultSet rs = pstmt.executeQuery();

            // Convert to List<Map>
            return resultSetToList(rs);

        } catch (SQLException e) {
            throw SqlStateMapper.mapException(e, query, params, profileName);
        }
    }

    /**
     * Internal update execution without logging.
     */
    private int executeUpdateInternal(String profileName, String query, Object... params) {
        try (Connection conn = ConnectionManager.getConnection(profileName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Set parameters
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }

            // Execute update
            return pstmt.executeUpdate();

        } catch (SQLException e) {
            throw SqlStateMapper.mapException(e, query, params, profileName);
        }
    }

    /**
     * Internal batch execution without logging.
     */
    private int[] executeBatchInternal(String profileName, String query, List<Object[]> batchParams) {
        try (Connection conn = ConnectionManager.getConnection(profileName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            for (Object[] params : batchParams) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
                pstmt.addBatch();
            }

            return pstmt.executeBatch();

        } catch (SQLException e) {
            throw SqlStateMapper.mapException(e, query, null, profileName);
        }
    }

    /**
     * Internal script execution without logging.
     */
    private void executeScriptInternal(String profileName, String script) {
        try (Connection conn = ConnectionManager.getConnection(profileName);
             Statement stmt = conn.createStatement()) {

            // Split by semicolon and execute each statement
            String[] statements = script.split(";");
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }

        } catch (SQLException e) {
            throw SqlStateMapper.mapException(e, script, null, profileName);
        }
    }

    /**
     * Converts ResultSet to List<Map<String, Object>>.
     */
    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metadata = rs.getMetaData();
        int columnCount = metadata.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metadata.getColumnName(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            results.add(row);
        }

        return results;
    }

    /**
     * Calculates rows affected from operation result.
     */
    private int calculateRowsAffected(Object result) {
        if (result == null) {
            return 0;
        }
        if (result instanceof Integer) {
            return (Integer) result;
        }
        if (result instanceof List) {
            return ((List<?>) result).size();
        }
        if (result instanceof int[]) {
            int[] arr = (int[]) result;
            int total = 0;
            for (int count : arr) {
                total += count;
            }
            return total;
        }
        return 1; // Default for other operations
    }

    /**
     * Gets current test case name from ExecutionContext.
     */
    private String getCurrentTestCase() {
        try {
            return ExecutionContext.getInstance().getCurrentTestCase();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ========================================================================
    // TRANSACTION CONNECTION STORAGE
    // ========================================================================

    // ThreadLocal storage for transaction connections
    private static final ThreadLocal<Map<String, Connection>> TRANSACTION_CONNECTIONS =
            ThreadLocal.withInitial(HashMap::new);

    // ThreadLocal storage for operation count per transaction
    private static final ThreadLocal<Map<String, Integer>> OPERATION_COUNTS =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * Stores connection for transaction scope.
     */
    private void storeConnection(String profileName, Connection conn) {
        TRANSACTION_CONNECTIONS.get().put(profileName, conn);
        OPERATION_COUNTS.get().put(profileName, 0);
    }

    /**
     * Gets stored connection for transaction.
     */
    private Connection getStoredConnection(String profileName) {
        return TRANSACTION_CONNECTIONS.get().get(profileName);
    }

    /**
     * Removes connection after transaction ends.
     */
    private void removeConnection(String profileName) {
        TRANSACTION_CONNECTIONS.get().remove(profileName);
        OPERATION_COUNTS.get().remove(profileName);
    }

    /**
     * Increments operation count for transaction.
     */
    private void incrementOperationCount(String profileName) {
        Map<String, Integer> counts = OPERATION_COUNTS.get();
        counts.put(profileName, counts.getOrDefault(profileName, 0) + 1);
    }

    /**
     * Gets operation count for transaction.
     */
    private int getOperationCount(String profileName) {
        return OPERATION_COUNTS.get().getOrDefault(profileName, 0);
    }

    /**
     * Maps isolation level string to JDBC constant.
     */
    private int mapIsolationLevel(String level) {
        switch (level.toUpperCase()) {
            case "READ_UNCOMMITTED":
                return Connection.TRANSACTION_READ_UNCOMMITTED;
            case "READ_COMMITTED":
                return Connection.TRANSACTION_READ_COMMITTED;
            case "REPEATABLE_READ":
                return Connection.TRANSACTION_REPEATABLE_READ;
            case "SERIALIZABLE":
                return Connection.TRANSACTION_SERIALIZABLE;
            default:
                return Connection.TRANSACTION_READ_COMMITTED;
        }
    }

    // ========================================================================
    // FUNCTIONAL INTERFACE FOR OPERATIONS
    // ========================================================================

    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T execute() throws Exception;
    }
}