// File: netat-web/src/main/java/com/vtnet/netat/web/ai/ContextExtractionStrategy.java
package com.vtnet.netat.web.ai;

public enum ContextExtractionStrategy {
    /**
     * Quick extraction - 500-1000 chars
     * Fast, cheap, but may miss context
     */
    COMPACT(1000, 5),

    /**
     * Medium extraction - 2000-3000 chars
     * Balanced approach
     */
    EXPANDED(3000, 10),

    /**
     * Full DOM - no limit
     * Slowest but most accurate
     */
    FULL(Integer.MAX_VALUE, 30);

    private final int maxChars;
    private final int timeoutSeconds;

    ContextExtractionStrategy(int maxChars, int timeoutSeconds) {
        this.maxChars = maxChars;
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxChars() { return maxChars; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
}