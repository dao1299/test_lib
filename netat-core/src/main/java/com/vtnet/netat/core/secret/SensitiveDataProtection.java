package com.vtnet.netat.core.secret;

import com.vtnet.netat.core.ui.ObjectUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auto-protection cho dữ liệu nhạy cảm.
 * Zero-config - tự động detect và mask dựa trên tên ObjectUI hoặc locator.
 *
 * <p>Cách hoạt động:</p>
 * <ul>
 *   <li>Auto-detect: Kiểm tra tên object/locator có chứa keyword nhạy cảm</li>
 *   <li>Mask format: "Secret123" → "S*****3" (ký tự đầu + 5 sao + ký tự cuối)</li>
 *   <li>Không tiết lộ độ dài thực tế của dữ liệu</li>
 * </ul>
 *
 * <p>Sử dụng:</p>
 * <pre>
 * // Không cần config - tự động hoạt động
 * webKeyword.sendKeys(passwordInput, "MySecret"); // Auto mask trong log
 *
 * // Hoặc dùng keyword sensitive cho field không có keyword nhạy cảm
 * webKeyword.sendKeysSensitive(customField, "data"); // Luôn mask
 * </pre>
 */
public final class SensitiveDataProtection {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataProtection.class);

    private static final SensitiveDataProtection INSTANCE = new SensitiveDataProtection();

    /** Mask format: luôn 5 dấu sao để không lộ độ dài */
    private static final String MASK_PATTERN = "*****";

    /** Keywords để auto-detect field nhạy cảm */
    private final Set<String> sensitiveKeywords = ConcurrentHashMap.newKeySet();

    /** Cache ObjectUI đã xác định là nhạy cảm */
    private final Set<String> sensitiveObjectCache = ConcurrentHashMap.newKeySet();

    /** Cache giá trị nhạy cảm đã nhập (để mask trong logs khác) */
    private final Set<String> sensitiveValuesCache = ConcurrentHashMap.newKeySet();

    /** Enable/disable flag */
    private volatile boolean enabled = true;

    private SensitiveDataProtection() {
        initDefaultKeywords();
        log.info("SensitiveDataProtection initialized with {} keywords", sensitiveKeywords.size());
    }

    /**
     * Khởi tạo danh sách keywords mặc định
     */
    private void initDefaultKeywords() {
        sensitiveKeywords.addAll(Arrays.asList(
                // Password related
                "password", "passwd", "pwd", "pass", "mat_khau", "matkhau", "mk",

                // Security tokens
                "secret", "token", "apikey", "api_key", "api-key", "key",
                "credential", "auth", "authorization", "bearer", "jwt",
                "access_token", "refresh_token", "session",

                // Financial
                "credit", "card", "creditcard", "card_number", "cardnumber",
                "cvv", "cvc", "ccv", "security_code",
                "pin", "otp", "verification_code", "verify_code", "ma_xac_nhan",
                "account", "bank", "routing", "swift", "iban",
                "so_tai_khoan", "sotaikhoan", "stk",

                // Personal identity
                "ssn", "social_security", "socialsecurity",
                "tax_id", "taxid", "ma_so_thue", "mst",
                "cmnd", "cccd", "cmtnd", "so_cmnd", "socmnd",
                "passport", "hochieu",

                // Other sensitive
                "private", "secure", "sensitive", "encrypted", "cipher"
        ));
    }

    public static SensitiveDataProtection getInstance() {
        return INSTANCE;
    }

    // ==================== CONFIGURATION ====================

    /**
     * Enable/disable protection
     * @param enabled true để bật, false để tắt
     * @return this instance for chaining
     */
    public SensitiveDataProtection setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("SensitiveDataProtection {}", enabled ? "ENABLED" : "DISABLED");
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Thêm keyword nhạy cảm custom
     * @param keywords các keyword cần thêm
     * @return this instance for chaining
     */
    public SensitiveDataProtection addKeyword(String... keywords) {
        for (String kw : keywords) {
            if (kw != null && !kw.trim().isEmpty()) {
                sensitiveKeywords.add(kw.toLowerCase().trim());
            }
        }
        return this;
    }

    /**
     * Đánh dấu một ObjectUI là nhạy cảm (manual override)
     * @param objectName tên của ObjectUI
     * @return this instance for chaining
     */
    public SensitiveDataProtection markAsSensitive(String objectName) {
        if (objectName != null) {
            sensitiveObjectCache.add(objectName);
        }
        return this;
    }

    // ==================== CORE METHODS ====================

    /**
     * Kiểm tra ObjectUI có nhạy cảm không
     * @param uiObject đối tượng cần kiểm tra
     * @return true nếu là field nhạy cảm
     */
    public boolean isSensitive(ObjectUI uiObject) {
        if (!enabled || uiObject == null) {
            return false;
        }

        String objectName = uiObject.getName();

        // Check cache first
        if (objectName != null && sensitiveObjectCache.contains(objectName)) {
            return true;
        }

        // Check by object name
        if (containsSensitiveKeyword(objectName)) {
            if (objectName != null) {
                sensitiveObjectCache.add(objectName);
            }
            return true;
        }

        // Check by description
        if (containsSensitiveKeyword(uiObject.getDescription())) {
            if (objectName != null) {
                sensitiveObjectCache.add(objectName);
            }
            return true;
        }

        // Check by locator value
        if (uiObject.getActiveLocators() != null && !uiObject.getActiveLocators().isEmpty()) {
            String locatorValue = uiObject.getActiveLocators().get(0).getValue();
            if (containsSensitiveKeyword(locatorValue)) {
                if (objectName != null) {
                    sensitiveObjectCache.add(objectName);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Mask giá trị nhạy cảm
     * Format: "Secret123" → "S*****3"
     * Luôn hiện 5 dấu * để không tiết lộ độ dài thực tế
     *
     * @param value giá trị cần mask
     * @return giá trị đã được mask
     */
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int length = value.length();

        if (length == 1) {
            return "*";
        }

        if (length == 2) {
            return value.charAt(0) + "*";
        }

        // Format: ký_tự_đầu + ***** + ký_tự_cuối
        return String.valueOf(value.charAt(0)) + MASK_PATTERN + value.charAt(length - 1);
    }

    /**
     * Mask nếu ObjectUI là nhạy cảm, ngược lại trả về nguyên bản
     * @param uiObject đối tượng UI
     * @param value giá trị cần xử lý
     * @return giá trị đã mask hoặc nguyên bản
     */
    public String maskIfSensitive(ObjectUI uiObject, String value) {
        if (isSensitive(uiObject)) {
            registerSensitiveValue(value);
            return mask(value);
        }
        return value;
    }

    /**
     * Đăng ký giá trị nhạy cảm vào cache
     * Giá trị này sẽ được mask khi xuất hiện trong các log khác
     * @param value giá trị cần đăng ký
     */
    public void registerSensitiveValue(String value) {
        if (value != null && value.length() >= 3) {
            sensitiveValuesCache.add(value);
        }
    }

    /**
     * Mask tất cả giá trị nhạy cảm đã biết trong một chuỗi text
     * Dùng cho log messages, HTML context, etc.
     * @param text chuỗi text cần xử lý
     * @return text đã được mask các giá trị nhạy cảm
     */
    public String maskAllKnownValues(String text) {
        if (!enabled || text == null || sensitiveValuesCache.isEmpty()) {
            return text;
        }

        String result = text;
        for (String sensitiveValue : sensitiveValuesCache) {
            if (result.contains(sensitiveValue)) {
                result = result.replace(sensitiveValue, mask(sensitiveValue));
            }
        }
        return result;
    }

    /**
     * Clear cache giá trị nhạy cảm (gọi sau mỗi test)
     */
    public void clearValuesCache() {
        sensitiveValuesCache.clear();
        log.debug("Sensitive values cache cleared");
    }

    /**
     * Full reset - clear tất cả cache
     */
    public void reset() {
        sensitiveValuesCache.clear();
        sensitiveObjectCache.clear();
        log.info("SensitiveDataProtection fully reset");
    }

    // ==================== HELPER ====================

    /**
     * Kiểm tra text có chứa keyword nhạy cảm không
     */
    private boolean containsSensitiveKeyword(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Normalize: lowercase và thay special chars bằng space
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9_]", " ");

        for (String keyword : sensitiveKeywords) {
            // Check exact word hoặc là phần của compound word (với underscore)
            if (normalized.contains(keyword) ||
                    normalized.contains(keyword.replace("_", ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lấy số lượng keywords đã đăng ký
     */
    public int getKeywordCount() {
        return sensitiveKeywords.size();
    }

    /**
     * Lấy số lượng giá trị nhạy cảm trong cache
     */
    public int getCachedValuesCount() {
        return sensitiveValuesCache.size();
    }
}
