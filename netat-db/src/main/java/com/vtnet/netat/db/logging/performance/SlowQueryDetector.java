package com.vtnet.netat.db.logging.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Detects and tracks slow queries.
 * Maintains statistics for query patterns.
 */
public class SlowQueryDetector {

    private static final Logger log = LoggerFactory.getLogger(SlowQueryDetector.class);

    // Configuration
    private long warningThreshold = 1000;    // 1 second
    private long criticalThreshold = 5000;   // 5 seconds
    private boolean autoLog = true;

    // Statistics storage
    private final Map<String, QueryStats> queryStatsMap = new ConcurrentHashMap<>();

    // Pattern for normalizing queries
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
    private static final Pattern STRING_PATTERN = Pattern.compile("'[^']*'");

    /**
     * Records a query execution and checks for slowness.
     */
    public void recordQuery(String query, long durationMs) {
        // Normalize query for grouping
        String normalized = normalizeQuery(query);

        // Update statistics
        QueryStats stats = queryStatsMap.computeIfAbsent(
                normalized,
                QueryStats::new
        );
        stats.addExecution(durationMs);

        // Check thresholds
        if (durationMs >= criticalThreshold) {
            onCriticalSlowQuery(query, durationMs, stats);
        } else if (durationMs >= warningThreshold) {
            onSlowQueryWarning(query, durationMs, stats);
        }

        // Check if consistently slow
        if (stats.isConsistentlySlow(warningThreshold)) {
            onConsistentlySlowQuery(query, stats);
        }
    }

    /**
     * Normalizes query by replacing literals with placeholders.
     * Groups similar queries together.
     */
    public String normalizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }

        String normalized = query.trim();

        // Replace numbers: 123 → ?
        normalized = NUMBER_PATTERN.matcher(normalized).replaceAll("?");

        // Replace strings: 'John' → ?
        normalized = STRING_PATTERN.matcher(normalized).replaceAll("?");

        // Collapse multiple spaces
        normalized = normalized.replaceAll("\\s+", " ");

        return normalized;
    }

    /**
     * Gets top N slowest queries by average duration.
     */
    public List<QueryStats> getTopSlowQueries(int limit) {
        return queryStatsMap.values().stream()
                .sorted(Comparator.comparing(QueryStats::getAverageDuration).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets all queries exceeding a threshold.
     */
    public List<QueryStats> getQueriesExceedingThreshold(long thresholdMs) {
        return queryStatsMap.values().stream()
                .filter(stats -> stats.getAverageDuration() > thresholdMs)
                .sorted(Comparator.comparing(QueryStats::getAverageDuration).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Gets statistics for a specific query pattern.
     */
    public QueryStats getStats(String query) {
        String normalized = normalizeQuery(query);
        return queryStatsMap.get(normalized);
    }

    /**
     * Clears all statistics.
     */
    public void clearStats() {
        queryStatsMap.clear();
    }

    /**
     * Gets total number of tracked query patterns.
     */
    public int getTrackedQueryCount() {
        return queryStatsMap.size();
    }

    // Event handlers

    private void onSlowQueryWarning(String query, long durationMs, QueryStats stats) {
        if (autoLog) {
            log.warn("⚠️  SLOW QUERY detected ({}ms > {}ms threshold)\n" +
                            "Query: {}\n" +
                            "Average duration: {:.1f}ms (based on {} executions)",
                    durationMs, warningThreshold, query,
                    stats.getAverageDuration(), stats.getExecutionCount());
        }
    }

    private void onCriticalSlowQuery(String query, long durationMs, QueryStats stats) {
        if (autoLog) {
            log.error("🔥 CRITICAL SLOW QUERY detected ({}ms > {}ms threshold)\n" +
                            "Query: {}\n" +
                            "Average duration: {:.1f}ms (based on {} executions)\n" +
                            "Min: {}ms, Max: {}ms",
                    durationMs, criticalThreshold, query,
                    stats.getAverageDuration(), stats.getExecutionCount(),
                    stats.getMinDuration(), stats.getMaxDuration());
        }
    }

    private void onConsistentlySlowQuery(String query, QueryStats stats) {
        if (autoLog) {
            log.warn("📊 Query is CONSISTENTLY SLOW\n" +
                            "Query: {}\n" +
                            "Average: {:.1f}ms over {} executions\n" +
                            "Recent durations: {}",
                    query, stats.getAverageDuration(), stats.getExecutionCount(),
                    stats.getRecentDurations());
        }
    }

    // Configuration

    public void setWarningThreshold(long warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public long getWarningThreshold() {
        return warningThreshold;
    }

    public void setCriticalThreshold(long criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public long getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setAutoLog(boolean autoLog) {
        this.autoLog = autoLog;
    }

    public boolean isAutoLog() {
        return autoLog;
    }
}