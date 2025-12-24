package com.vtnet.netat.core.secret;

import com.vtnet.netat.core.logging.NetatLogger;
import com.vtnet.netat.core.ui.ObjectUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public final class SensitiveDataProtection {

    private static final NetatLogger log = NetatLogger.getInstance(SensitiveDataProtection.class);
    private static final SensitiveDataProtection INSTANCE = new SensitiveDataProtection();


    private static final String MASK_PATTERN = "*****";

    private final Set<String> sensitiveKeywords = ConcurrentHashMap.newKeySet();

    private final Set<String> sensitiveObjectCache = ConcurrentHashMap.newKeySet();

    private final Set<String> sensitiveValuesCache = ConcurrentHashMap.newKeySet();

    private volatile boolean enabled = true;

    private SensitiveDataProtection() {
        initDefaultKeywords();
        log.info("SensitiveDataProtection initialized with {} keywords", sensitiveKeywords.size());
    }

    private void initDefaultKeywords() {
        sensitiveKeywords.addAll(Arrays.asList(
                "password", "passwd", "pwd", "pass", "mat_khau", "matkhau", "mk", "pw" ,
                "secret", "token", "apikey", "api_key", "api-key", "key",
                "credential", "auth", "authorization", "bearer", "jwt",
                "access_token", "refresh_token", "session",
                "credit", "card", "creditcard", "card_number", "cardnumber",
                "cvv", "cvc", "ccv", "security_code",
                "pin", "otp", "verification_code", "verify_code", "ma_xac_nhan",
                "account", "bank", "routing", "swift", "iban",
                "so_tai_khoan", "sotaikhoan", "stk",
                "ssn", "social_security", "socialsecurity",
                "tax_id", "taxid", "ma_so_thue", "mst",
                "cmnd", "cccd", "cmtnd", "so_cmnd", "socmnd",
                "passport", "hochieu",
                "private", "secure", "sensitive", "encrypted", "cipher"
        ));
    }

    public static SensitiveDataProtection getInstance() {
        return INSTANCE;
    }

    public SensitiveDataProtection setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("SensitiveDataProtection {}", enabled ? "ENABLED" : "DISABLED");
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }


    public SensitiveDataProtection addKeyword(String... keywords) {
        for (String kw : keywords) {
            if (kw != null && !kw.trim().isEmpty()) {
                sensitiveKeywords.add(kw.toLowerCase().trim());
            }
        }
        return this;
    }


    public SensitiveDataProtection markAsSensitive(String objectName) {
        if (objectName != null) {
            sensitiveObjectCache.add(objectName);
        }
        return this;
    }


    public boolean isSensitive(ObjectUI uiObject) {
        if (!enabled || uiObject == null) {
            return false;
        }

        String objectName = uiObject.getName();

        if (objectName != null && sensitiveObjectCache.contains(objectName)) {
            return true;
        }

        if (containsSensitiveKeyword(objectName)) {
            if (objectName != null) {
                sensitiveObjectCache.add(objectName);
            }
            return true;
        }

        if (containsSensitiveKeyword(uiObject.getDescription())) {
            if (objectName != null) {
                sensitiveObjectCache.add(objectName);
            }
            return true;
        }

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

    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return MASK_PATTERN;
    }

    public String maskIfSensitive(ObjectUI uiObject, String value) {
        if (isSensitive(uiObject)) {
            registerSensitiveValue(value);
            return mask(value);
        }
        return value;
    }


    public void registerSensitiveValue(String value) {
        if (value != null && !value.isEmpty()) {
            sensitiveValuesCache.add(value);
        }
    }

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


    public void clearValuesCache() {
        sensitiveValuesCache.clear();
        log.debug("Sensitive values cache cleared");
    }

    public void reset() {
        sensitiveValuesCache.clear();
        sensitiveObjectCache.clear();
        log.info("SensitiveDataProtection fully reset");
    }


    private boolean containsSensitiveKeyword(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String normalized = text.toLowerCase().replaceAll("[^a-z0-9_]", " ");

        for (String keyword : sensitiveKeywords) {
            if (normalized.contains(keyword) ||
                    normalized.contains(keyword.replace("_", ""))) {
                return true;
            }
        }
        return false;
    }

    public int getKeywordCount() {
        return sensitiveKeywords.size();
    }


    public int getCachedValuesCount() {
        return sensitiveValuesCache.size();
    }
}
