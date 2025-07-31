package com.vtnet.netat.web.elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vtnet.netat.core.exceptions.NetatException;
import com.vtnet.netat.core.logging.NetatLogger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository Layer cho UI Objects (NetatUIObject).
 * Chịu trách nhiệm tải, cache, và hợp nhất các NetatUIObject từ các nguồn dữ liệu.
 * Hiện tại hỗ trợ tải từ file JSON.
 */
public class UIObjectRepository {

    private static final NetatLogger logger = NetatLogger.getInstance(UIObjectRepository.class);
    // Base path cho thư mục chứa các file JSON UI Objects trong resources
    private static final String UI_OBJECTS_BASE_PATH = "ui_objects";
    // L1 Cache (In-Memory) để lưu trữ các UIObject đã tải và hợp nhất
    private static final Map<String, NetatUIObject> cache = new ConcurrentHashMap<>();
    // ObjectMapper của Jackson để chuyển đổi JSON sang/từ Java Object
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Đăng ký JavaTimeModule để Jackson có thể xử lý LocalDateTime trong NetatUIObject
        objectMapper.registerModule(new JavaTimeModule());
    }

    // Private constructor để triển khai Singleton Pattern
    private UIObjectRepository() {
        // Có thể thêm logic khởi tạo ban đầu ở đây nếu cần (ví dụ: eager loading)
    }

    /**
     * Phương thức để lấy instance của UIObjectRepository (Singleton).
     * @return Instance của UIObjectRepository
     */
    public static UIObjectRepository getInstance() {
        return Holder.INSTANCE;
    }

    // Holder class cho Singleton Pattern (lazy initialization và thread-safe)
    private static class Holder {
        private static final UIObjectRepository INSTANCE = new UIObjectRepository();
    }

    /**
     * Tải NetatUIObject từ đường dẫn logic được chỉ định, bao gồm cả logic kế thừa và caching.
     * Đây là phương thức chính mà người dùng (trong WebKeywords hoặc Page Objects) sẽ gọi.
     *
     * @param objectPath Đường dẫn logic của UIObject (ví dụ: "ecommerce/customer/LoginPage/loginButton")
     * @return NetatUIObject đã được hợp nhất hoàn chỉnh
     * @throws NetatException nếu không tìm thấy UIObject hoặc lỗi trong quá trình tải/hợp nhất
     */
    public NetatUIObject getUIObjectByPath(String objectPath) {
        // 1. Kiểm tra L1 Cache (In-Memory) trước
        if (cache.containsKey(objectPath)) {
            logger.debug("Hit L1 Cache for UIObject: {}", objectPath);
            return cache.get(objectPath);
        }

        logger.info("Loading UIObject: {} from file system...", objectPath);
        // 2. Tải UIObject cơ bản từ file JSON
        NetatUIObject uiObject = loadUIObjectFromFile(objectPath);

        if (uiObject == null) {
            throw new NetatException("UIObject not found for path: " + objectPath);
        }

        // 3. Xử lý logic kế thừa nếu UIObject có thuộc tính 'parentPath'
        if (uiObject.getParentPath() != null && !uiObject.getParentPath().isEmpty()) {
            logger.debug("UIObject {} inherits from {}", objectPath, uiObject.getParentPath());
            // Đệ quy tải UIObject cha
            NetatUIObject parentUIObject = getUIObjectByPath(uiObject.getParentPath());
            // Hợp nhất các thuộc tính từ cha vào con
            uiObject = mergeUIObjects(uiObject, parentUIObject);
            logger.debug("Merged UIObject {} with parent {}", objectPath, uiObject.getParentPath());
        }

        // 4. Lưu UIObject đã hợp nhất vào L1 Cache để tăng tốc độ cho các lần truy cập sau
        cache.put(objectPath, uiObject);
        return uiObject;
    }

    /**
     * Phương thức nội bộ để tải dữ liệu của một UIObject từ file JSON.
     * Nó tìm kiếm file JSON trong thư mục resources của ứng dụng.
     *
     * @param objectPath Đường dẫn logic của UIObject
     * @return NetatUIObject được đọc từ JSON hoặc null nếu file không tồn tại
     * @throws NetatException nếu có lỗi khi đọc/parse file JSON
     */
    private NetatUIObject loadUIObjectFromFile(String objectPath) {
        // Chuyển đổi đường dẫn logic thành đường dẫn file hệ thống và thêm phần mở rộng .json
        String filePath = UI_OBJECTS_BASE_PATH + "/" + objectPath.replace("/", System.getProperty("file.separator")) + ".json";

        // Cố gắng tải file từ classpath (tốt cho packaged JAR)
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);

        // Nếu không tìm thấy trên classpath, thử tải từ đường dẫn file hệ thống tuyệt đối (tốt cho môi trường dev)
        if (inputStream == null) {
            Path absPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", filePath);
            try {
                if (Files.exists(absPath)) {
                    inputStream = Files.newInputStream(absPath);
                }
            } catch (Exception e) {
                logger.warn("Could not load UIObject from absolute path {}: {}", absPath, e.getMessage());
            }
        }

        if (inputStream == null) {
            // Nếu vẫn không tìm thấy, log cảnh báo và trả về null
            logger.warn("UIObject JSON file not found: {}", filePath);
            return null;
        }

        try (InputStream is = inputStream) {
            // Đọc và parse JSON thành NetatUIObject
            return objectMapper.readValue(is, NetatUIObject.class);
        } catch (Exception e) {
            // Ném ngoại lệ NetatException nếu có lỗi đọc/parse JSON
            throw new NetatException("Error loading UIObject from JSON file: " + filePath, e);
        }
    }

    /**
     * Hợp nhất các thuộc tính từ UIObject cha vào UIObject con.
     * Các thuộc tính của UIObject con sẽ ghi đè thuộc tính của cha nếu có xung đột.
     *
     * @param child UIObject con (sẽ được sửa đổi)
     * @param parent UIObject cha (chỉ đọc)
     * @return UIObject con đã được hợp nhất
     */
    private NetatUIObject mergeUIObjects(NetatUIObject child, NetatUIObject parent) {
        // Hợp nhất Locators:
        // Thêm các locator từ cha vào con nếu con chưa có locator cùng 'strategy' (loại) đó.
        List<Locator> mergedLocators = new ArrayList<>(child.getLocators());
        if (parent.getLocators() != null) {
            for (Locator parentLocator : parent.getLocators()) {
                boolean foundInChild = mergedLocators.stream()
                        .anyMatch(cl -> cl.getStrategy().equalsIgnoreCase(parentLocator.getStrategy()));
                if (!foundInChild) {
                    mergedLocators.add(parentLocator);
                }
            }
        }
        child.setLocators(mergedLocators);

        // Hợp nhất Properties (Map<String, Object>):
        // Properties của con sẽ ghi đè properties của cha.
        if (parent.getProperties() != null) {
            Map<String, Object> mergedProperties = new HashMap<>(parent.getProperties());
            if (child.getProperties() != null) {
                mergedProperties.putAll(child.getProperties());
            }
            child.setProperties(mergedProperties);
        }

        // Hợp nhất Custom Attributes (Map<String, Object>): Tương tự như Properties.
        if (parent.getCustomAttributes() != null) {
            Map<String, Object> mergedCustomAttributes = new HashMap<>(parent.getCustomAttributes());
            if (child.getCustomAttributes() != null) {
                mergedCustomAttributes.putAll(child.getCustomAttributes());
            }
            child.setCustomAttributes(mergedCustomAttributes);
        }

        // Các thuộc tính cơ bản:
        // Chỉ ghi đè nếu UIObject con chưa có giá trị hoặc giá trị rỗng (string).
        if (child.getName() == null || child.getName().isEmpty()) {
            child.setName(parent.getName());
        }
        if (child.getDescription() == null || child.getDescription().isEmpty()) {
            child.setDescription(parent.getDescription());
        }
        if (child.getType() == null || child.getType().isEmpty()) {
            child.setType(parent.getType());
        }
        if (child.getVersion() == null || child.getVersion().isEmpty()) {
            child.setVersion(parent.getVersion());
        }
        if (child.getAuthor() == null || child.getAuthor().isEmpty()) {
            child.setAuthor(parent.getAuthor());
        }
        // lastModified của con thường là thời gian của nó, không hợp nhất từ cha.
        // udid và path cũng là duy nhất của con.

        // inShadowRoot: Nếu con chưa định nghĩa và cha là inShadowRoot, con cũng sẽ là inShadowRoot.
        // Cần đảm bảo rằng isInShadowRoot() trả về boolean wrapper hoặc có giá trị mặc định false.
        // Giả sử NetatUIObject.isInShadowRoot() trả về giá trị hoặc false nếu không được đặt.
        if (!child.isInShadowRoot() && parent.isInShadowRoot()) {
            child.setInShadowRoot(true);
        }

        // Relationships: Việc hợp nhất relationships có thể phức tạp tùy thuộc vào yêu cầu.
        // Ở đây, chúng ta bỏ qua việc hợp nhất relationships từ cha để giữ đơn giản, chỉ giữ lại của con.
        // Nếu cần, bạn sẽ phải định nghĩa logic merge cho Map<String, List<String>>.

        // Children: children đại diện cho Page Object Model/Component Model, không phải kế thừa thuộc tính.
        // Children của con sẽ là danh sách cuối cùng.

        return child;
    }

    /**
     * Xóa toàn bộ L1 Cache của UI Objects.
     * Hữu ích cho việc đảm bảo tải lại dữ liệu mới nhất hoặc dọn dẹp sau khi chạy test suite.
     */
    public static void clearCache() {
        cache.clear();
        logger.info("UIObjectRepository cache cleared.");
    }
}