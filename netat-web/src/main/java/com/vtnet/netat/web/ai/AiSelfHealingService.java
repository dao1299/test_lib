// File: netat-web/src/main/java/com/vtnet/netat/web/ai/AiSelfHealingService.java
package com.vtnet.netat.web.ai;

import com.vtnet.netat.driver.ConfigReader;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AiSelfHealingService implements IAiSelfHealingService {

    private static final Logger log = LoggerFactory.getLogger(AiSelfHealingService.class);

    private final ChatModel aiModel;
    private final AiLocatorCache cache;
    private final RateLimiter rateLimiter;

    public AiSelfHealingService() {
        this.aiModel = AiModelFactory.createModel();
        this.cache = new AiLocatorCache();
        this.rateLimiter = new RateLimiter(10, java.time.Duration.ofMinutes(1));
    }

    @Override
    public Optional<String> findNewLocator(String elementName,
                                           String contextHtml,
                                           String previousLocator) {
        if (!isAvailable()) {
            log.warn("AI Self-Healing is disabled");
            return Optional.empty();
        }

        // ✅ Try with escalating strategies
        ContextExtractionStrategy[] strategies = {
                ContextExtractionStrategy.COMPACT,
                ContextExtractionStrategy.EXPANDED,
                ContextExtractionStrategy.FULL
        };

        for (ContextExtractionStrategy strategy : strategies) {
            log.info("Trying AI self-healing with strategy: {}", strategy);

            Optional<String> result = tryFindWithStrategy(
                    elementName,
                    contextHtml,
                    previousLocator,
                    strategy
            );

            if (result.isPresent()) {
                log.info("✅ SUCCESS with strategy: {}", strategy);
                return result;
            }

            log.warn("❌ FAILED with strategy: {}, escalating...", strategy);
        }

        log.error("All strategies failed for element: {}", elementName);
        return Optional.empty();
    }

    /**
     * ✅ Try to find locator with specific strategy
     */
    private Optional<String> tryFindWithStrategy(String elementName,
                                                 String fullHtml,
                                                 String previousLocator,
                                                 ContextExtractionStrategy strategy) {
        try {
            // 1. Extract context based on strategy
            String extractedContext = HtmlContextExtractor.extractContext(
                    fullHtml,
                    elementName,
                    strategy
            );

            log.debug("Extracted {} chars for strategy {}",
                    extractedContext.length(), strategy);

            // 2. Sanitize (only if not FULL - keep full dom as-is for max accuracy)
            String contextToSend = (strategy == ContextExtractionStrategy.FULL)
                    ? extractedContext  // Skip sanitization for full DOM
                    : HtmlSanitizer.sanitize(extractedContext);

            // 3. Check cache
            String contextHash = AiLocatorCache.hashContext(contextToSend);
            String cached = cache.get(elementName, contextHash);
            if (cached != null) {
                log.info("Cache HIT for strategy: {}", strategy);
                return Optional.of(cached);
            }

            // 4. Check rate limit
            if (!rateLimiter.tryAcquire()) {
                log.warn("Rate limit exceeded");
                return Optional.empty();
            }

            // 5. Call AI with timeout based on strategy
            String locator = callAiModel(
                    elementName,
                    contextToSend,
                    previousLocator,
                    strategy.getTimeoutSeconds()
            );

            // 6. Validate locator
            if (locator != null && isValidCssSelector(locator)) {
                // 7. Validate against original HTML
                if (validateLocatorInHtml(locator, fullHtml)) {
                    cache.put(elementName, contextHash, locator);
                    return Optional.of(locator);
                } else {
                    log.warn("Locator '{}' validation failed against DOM", locator);
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error with strategy " + strategy, e);
            return Optional.empty();
        }
    }

    /**
     * ✅ Validate locator exists in HTML
     */
    private boolean validateLocatorInHtml(String cssSelector, String html) {
        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
            org.jsoup.select.Elements elements = doc.select(cssSelector);

            boolean found = !elements.isEmpty();
            log.debug("Validation: locator '{}' found {} elements",
                    cssSelector, elements.size());

            return found;

        } catch (Exception e) {
            log.error("Validation error", e);
            return false;
        }
    }

    /**
     * Call AI model with timeout
     */
    private String callAiModel(String elementName,
                               String contextHtml,
                               String previousLocator,
                               int timeoutSeconds) {
        String prompt = buildPrompt(elementName, contextHtml, previousLocator);

        try {
            // Simple call - no retry for now
            String response = aiModel.chat(prompt).trim();
            return extractLocator(response);

        } catch (Exception e) {
            log.error("AI call failed", e);
            return null;
        }
    }

    /**
     * Build improved prompt
     */
    private String buildPrompt(String elementName, String html, String previous) {
        System.out.println("AiSelfHealingService.buildPrompt: "+html);
        return "You are a web automation expert. Find the EXACT CSS selector.\n\n" +
                "ELEMENT: " + elementName + "\n" +
                "PREVIOUS LOCATOR (FAILED): " + (previous != null ? previous : "none") + "\n\n" +
                "HTML:\n```html\n" + html + "\n```\n\n" +
                "RULES:\n" +
                "1. Return ONLY CSS selector that EXISTS in HTML\n" +
                "2. Prefer ID > class > attribute\n" +
                "3. DO NOT invent IDs\n" +
                "4. Verify selector mentally\n\n" +
                "CSS SELECTOR:";
    }

    private String extractLocator(String response) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "```(?:css)?\\s*(.+?)```",
                java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return response.replace("RESPONSE:", "")
                .replace("```", "")
                .trim();
    }

    private boolean isValidCssSelector(String selector) {
        if (selector == null || selector.isEmpty()) return false;
        return selector.matches("^[a-zA-Z0-9#.\\[\\]\\-_:()> +~*=,\\s\"']+$")
                && !selector.contains("javascript:")
                && !selector.contains("<script");
    }

    @Override
    public boolean isAvailable() {
        return Boolean.parseBoolean(
                ConfigReader.getProperty("ai.self.healing.enabled", "false")
        ) && aiModel != null;
    }

    @Override
    public void clearCache() {
        cache.clear();
    }

    // RateLimiter inner class (same as before)
    private static class RateLimiter {
        private final int maxCalls;
        private final java.time.Duration window;
        private final long[] timestamps;
        private int index = 0;

        RateLimiter(int maxCalls, java.time.Duration window) {
            this.maxCalls = maxCalls;
            this.window = window;
            this.timestamps = new long[maxCalls];
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            long windowStart = now - window.toMillis();

            for (int i = 0; i < maxCalls; i++) {
                if (timestamps[i] < windowStart) {
                    timestamps[i] = 0;
                }
            }

            long callsInWindow = 0;
            for (long ts : timestamps) {
                if (ts > 0) callsInWindow++;
            }

            if (callsInWindow < maxCalls) {
                timestamps[index] = now;
                index = (index + 1) % maxCalls;
                return true;
            }

            return false;
        }
    }
}