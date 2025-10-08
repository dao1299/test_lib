// File: netat-web/src/main/java/com/vtnet/netat/web/ai/AiLocatorCache.java
package com.vtnet.netat.web.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache cho AI-generated locators
 */
public class AiLocatorCache {

    private static final Logger log = LoggerFactory.getLogger(AiLocatorCache.class);

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int MAX_CACHE_SIZE = 1000;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Get locator from cache
     */
    public String get(String elementName, String contextHash) {
        String key = buildKey(elementName, contextHash);
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return null;
        }

        // Check TTL
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("Cache expired for key: {}", key);
            return null;
        }

        log.debug("Cache HIT for key: {}", key);
        return entry.locator;
    }

    /**
     * Put locator into cache
     */
    public void put(String elementName, String contextHash, String locator) {
        // Check cache size limit
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldest();
        }

        String key = buildKey(elementName, contextHash);
        cache.put(key, new CacheEntry(locator, Instant.now().plus(CACHE_TTL)));

        log.debug("Cache PUT for key: {} (size: {})", key, cache.size());
    }

    /**
     * Clear all cache
     */
    public void clear() {
        cache.clear();
        log.info("AI locator cache cleared");
    }

    /**
     * Build cache key
     */
    private String buildKey(String elementName, String contextHash) {
        return elementName + "_" + contextHash;
    }

    /**
     * Evict oldest entries
     */
    private void evictOldest() {
        // Remove expired entries first
        cache.entrySet().removeIf(e -> e.getValue().isExpired());

        // If still full, remove oldest
        if (cache.size() >= MAX_CACHE_SIZE) {
            String oldestKey = cache.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (oldestKey != null) {
                cache.remove(oldestKey);
                log.debug("Evicted oldest cache entry: {}", oldestKey);
            }
        }
    }

    /**
     * Hash HTML context for cache key
     */
    public static String hashContext(String context) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(context.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            // Fallback: use hashCode
            return String.valueOf(context.hashCode());
        }
    }

    /**
     * Cache entry với TTL
     */
    private static class CacheEntry implements Comparable<CacheEntry> {
        final String locator;
        final Instant expiresAt;

        CacheEntry(String locator, Instant expiresAt) {
            this.locator = locator;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        @Override
        public int compareTo(CacheEntry other) {
            return this.expiresAt.compareTo(other.expiresAt);
        }
    }
}