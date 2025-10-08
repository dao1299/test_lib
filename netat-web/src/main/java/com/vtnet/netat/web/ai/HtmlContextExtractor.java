// File: netat-web/src/main/java/com/vtnet/netat/web/ai/HtmlContextExtractor.java
package com.vtnet.netat.web.ai;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlContextExtractor {

    private static final Logger log = LoggerFactory.getLogger(HtmlContextExtractor.class);

    /**
     * ✅ NEW: Extract với strategy escalation
     */
    public static String extractContext(String fullHtml,
                                        String elementHint,
                                        ContextExtractionStrategy strategy) {

        if (strategy == ContextExtractionStrategy.FULL) {
            // ✅ Full DOM - no processing
            log.info("Using FULL DOM strategy: {} chars", fullHtml.length());
            return fullHtml;
        }

        try {
            Document doc = Jsoup.parse(fullHtml);

            if (strategy == ContextExtractionStrategy.EXPANDED) {
                // ✅ Expanded: lấy form/main container
                return extractExpandedContext(doc, elementHint, strategy.getMaxChars());
            } else {
                // ✅ Compact: chỉ lấy relevant area
                return extractCompactContext(doc, elementHint, strategy.getMaxChars());
            }

        } catch (Exception e) {
            log.error("Error extracting context, falling back to truncated HTML", e);
            int maxChars = strategy.getMaxChars();
            return fullHtml.length() > maxChars
                    ? fullHtml.substring(0, maxChars)
                    : fullHtml;
        }
    }

    /**
     * ✅ COMPACT: Tìm specific element context
     */
    private static String extractCompactContext(Document doc, String hint, int maxChars) {
        Elements candidates = findByTextContent(doc, hint);

        if (candidates.isEmpty()) {
            candidates = findByAttributes(doc, hint);
        }

        if (candidates.isEmpty()) {
            candidates = findMainContentArea(doc);
        }

        StringBuilder context = new StringBuilder();
        for (Element element : candidates) {
            String snippet = buildCompactSnippet(element);
            if (context.length() + snippet.length() < maxChars) {
                context.append(snippet);
            } else {
                break;
            }
        }

        return context.toString();
    }

    /**
     * ✅ EXPANDED: Lấy toàn bộ form/container chứa element
     */
    private static String extractExpandedContext(Document doc, String hint, int maxChars) {
        // 1. Tìm element
        Elements candidates = findByTextContent(doc, hint);
        if (candidates.isEmpty()) {
            candidates = findByAttributes(doc, hint);
        }

        if (!candidates.isEmpty()) {
            // 2. Lấy parent form/container
            Element element = candidates.first();
            Element container = findContainer(element);

            if (container != null) {
                String html = container.outerHtml();
                log.debug("Found container <{}> with {} chars",
                        container.tagName(), html.length());

                return html.length() > maxChars
                        ? html.substring(0, maxChars)
                        : html;
            }
        }

        // Fallback: main content
        Elements mainContent = findMainContentArea(doc);
        if (!mainContent.isEmpty()) {
            String html = mainContent.first().outerHtml();
            return html.length() > maxChars
                    ? html.substring(0, maxChars)
                    : html;
        }

        return doc.body().html();
    }

    /**
     * Tìm container (form, section, main) chứa element
     */
    private static Element findContainer(Element element) {
        Element current = element;

        // Traverse up to find semantic container
        while (current != null) {
            String tag = current.tagName();

            // Priority containers
            if (tag.equals("form") ||
                    tag.equals("section") ||
                    tag.equals("article") ||
                    tag.equals("main") ||
                    current.hasClass("container") ||
                    current.hasClass("content")) {
                return current;
            }

            current = current.parent();
        }

        return null;
    }

    /**
     * Build compact snippet (existing code)
     */
    private static String buildCompactSnippet(Element element) {
        StringBuilder snippet = new StringBuilder();

        Element parent = element.parent();
        if (parent != null && !parent.tagName().equals("body")) {
            snippet.append("<").append(parent.tagName());
            appendRelevantAttributes(snippet, parent);
            snippet.append(">\n");
        }

        snippet.append("  ").append(element.outerHtml()).append("\n");

        Elements siblings = element.siblingElements();
        int count = 0;
        for (Element sibling : siblings) {
            if (count++ >= 2) break;
            snippet.append("  ").append(sibling.outerHtml()).append("\n");
        }

        if (parent != null) {
            snippet.append("</").append(parent.tagName()).append(">\n");
        }

        return snippet.toString();
    }

    // ... rest of helper methods (findByTextContent, etc.) remain same

    private static Elements findByTextContent(Document doc, String hint) {
        Elements results = new Elements();
        String normalizedHint = normalizeText(hint);
        String[] keywords = extractKeywords(normalizedHint);

        for (String keyword : keywords) {
            Elements found = doc.getElementsContainingOwnText(keyword);
            results.addAll(found);
            if (results.size() >= 5) break;
        }
        return results;
    }

    private static Elements findByAttributes(Document doc, String hint) {
        Elements results = new Elements();
        String[] attrs = {"id", "name", "class", "data-test-id", "aria-label"};
        String normalizedHint = normalizeText(hint);

        for (String attr : attrs) {
            Elements found = doc.select("[" + attr + "*=" + normalizedHint + "]");
            results.addAll(found);
            if (results.size() >= 5) break;
        }
        return results;
    }

    private static Elements findMainContentArea(Document doc) {
        Elements main = doc.select("main, article, [role=main]");
        if (!main.isEmpty()) return main;
        return doc.select("form, .container, .content, #content");
    }

    private static void appendRelevantAttributes(StringBuilder sb, Element element) {
        String[] relevantAttrs = {"id", "class", "name", "data-test-id", "role", "type"};
        for (String attr : relevantAttrs) {
            String value = element.attr(attr);
            if (!value.isEmpty()) {
                sb.append(" ").append(attr).append("=\"").append(value).append("\"");
            }
        }
    }

    private static String normalizeText(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();
    }

    private static String[] extractKeywords(String normalizedHint) {
        String withoutStopwords = normalizedHint.replaceAll(
                "\\b(the|a|an|and|or|but|in|on|at|to|for)\\b", " "
        );
        return withoutStopwords.trim().split("\\s+");
    }
}