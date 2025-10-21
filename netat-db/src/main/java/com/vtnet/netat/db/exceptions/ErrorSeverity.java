package com.vtnet.netat.db.exceptions;

public enum ErrorSeverity {

    /**
     * Critical error requiring immediate attention.
     * System cannot continue operation. Typically indicates:
     * - Complete database unavailability
     * - Catastrophic data corruption
     * - Security breach detected
     */
    CRITICAL(1, "Critical", "Immediate attention required. System cannot continue."),

    /**
     * Error that prevents operation completion.
     * The specific operation failed but system can continue. Typically indicates:
     * - Query syntax errors
     * - Constraint violations
     * - Permission denied
     */
    ERROR(2, "Error", "Operation failed. Manual intervention may be required."),

    /**
     * Warning that operation may not work as expected.
     * Operation completed but with issues. Typically indicates:
     * - Query timeout (may succeed on retry)
     * - Deadlock detected (victim can retry)
     * - Connection pool near exhaustion
     */
    WARNING(3, "Warning", "Operation completed with issues. May need attention."),

    /**
     * Informational message about database operations.
     * No error occurred, but useful diagnostic information. Typically indicates:
     * - Slow query detected
     * - Connection pool statistics
     * - Configuration changes
     */
    INFO(4, "Info", "Informational message. No action required.");

    private final int level;
    private final String displayName;
    private final String description;

    ErrorSeverity(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the numeric severity level.
     * Lower numbers indicate higher severity.
     *
     * @return severity level (1=CRITICAL, 4=INFO)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets human-readable display name.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets detailed description of this severity level.
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this severity is more severe than another.
     *
     * @param other the other severity to compare
     * @return true if this is more severe (lower level number)
     */
    public boolean isMoreSevereThan(ErrorSeverity other) {
        return this.level < other.level;
    }

    /**
     * Checks if this severity is at least as severe as another.
     *
     * @param other the other severity to compare
     * @return true if this is at least as severe
     */
    public boolean isAtLeastAsSevereAs(ErrorSeverity other) {
        return this.level <= other.level;
    }

    /**
     * Gets appropriate logging level for this severity.
     *
     * @return SLF4J logging level name
     */
    public String getLoggingLevel() {
        switch (this) {
            case CRITICAL:
            case ERROR:
                return "ERROR";
            case WARNING:
                return "WARN";
            case INFO:
                return "INFO";
            default:
                return "DEBUG";
        }
    }

    /**
     * Determines if operations should be retried for this severity.
     *
     * @return true if retry is generally recommended
     */
    public boolean shouldRetry() {
        return this == WARNING || this == INFO;
    }

    @Override
    public String toString() {
        return String.format("%s (Level %d): %s", displayName, level, description);
    }
}
