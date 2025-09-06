package com.vtnet.netat.web.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vtnet.netat.core.ui.Locator;
import com.vtnet.netat.core.ui.ObjectUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.MessageFormat;

public final class UiObjectHelper {

    private static final Logger log = LoggerFactory.getLogger(UiObjectHelper.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // === THAY ĐỔI QUAN TRỌNG: ĐỊNH NGHĨA ĐƯỜNG DẪN GỐC CỐ ĐỊNH ===
    // Sử dụng src/main/resources/ui_objects là một chuẩn mực tốt hơn
    // vì nó tương thích với cách Maven đóng gói tài nguyên.

    private static final String OBJECT_REPO_PATH = Paths.get(
            System.getProperty("user.dir"),
            "src", "test", "java", "automationtest", "object").toString();

    /**
     * Phương thức chính và duy nhất người dùng sẽ sử dụng.
     * Tự động tìm đối tượng từ kho lưu trữ đã được định nghĩa sẵn.
     *
     * @param relativeObjectPath Đường dẫn tương đối đến file object (không có .json).
     * Ví dụ: "LoginPage/email_input"
     * @param params             Các tham số động cho locator.
     * @return Một đối tượng ObjectUI.
     */
    public static ObjectUI getObject(String relativeObjectPath, String... params) {
        // Xây dựng đường dẫn đầy đủ từ đường dẫn gốc và đường dẫn tương đối
        String fullPath = Paths.get(OBJECT_REPO_PATH, relativeObjectPath + ".json").toString();

        // Gọi lại phương thức xử lý nội bộ
        return readObjectFromFile(fullPath, params);
    }

    /**
     * Phương thức nội bộ để đọc và xử lý file.
     * Giữ nó ở private để người dùng không gọi trực tiếp.
     */
    private static ObjectUI readObjectFromFile(String jsonPath, String... params) {
        log.debug("Đang xử lý ObjectUI từ path: '{}' với params: {}", jsonPath, (Object) params);

        try (InputStream inputStream = new FileInputStream(jsonPath)) {
            ObjectUI uiObject = MAPPER.readValue(inputStream, ObjectUI.class);

            if (params != null && params.length > 0 && uiObject.getLocators() != null) {
                for (Locator locator : uiObject.getLocators()) {
                    String formattedValue = locator.getValue();
                    for (int i = 0; i < params.length; i++) {
                        String placeholder = "{" + i + "}";
                        formattedValue = formattedValue.replace(placeholder, params[i]);
                    }
                    locator.setValue(formattedValue);
                }
            }

            log.info("Lấy thành công ObjectUI '{}' từ path: {}", uiObject.getName(), jsonPath);
            return uiObject;

        } catch (IOException e) {
            log.error("Lỗi nghiêm trọng khi đọc hoặc xử lý file UI Object tại '{}'.", jsonPath, e);
            throw new RuntimeException("Không thể lấy ObjectUI từ path: " + jsonPath, e);
        }
    }
}