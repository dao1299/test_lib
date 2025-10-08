package com.vtnet.netat.web.ai;

import java.util.Optional;

/**
 * Interface cho AI Self-Healing Service
 */
public interface IAiSelfHealingService {

    /**
     * Tìm locator mới cho element bằng AI
     *
     * @param elementName Tên/mô tả element
     * @param contextHtml HTML context xung quanh element (đã được sanitize)
     * @param previousLocator Locator cũ đã fail (optional)
     * @return Locator mới (CSS selector) hoặc empty nếu không tìm thấy
     */
    Optional<String> findNewLocator(String elementName, String contextHtml, String previousLocator);

    /**
     * Check xem service có available không
     */
    boolean isAvailable();

    /**
     * Clear cache (dùng khi cần refresh)
     */
    void clearCache();
}