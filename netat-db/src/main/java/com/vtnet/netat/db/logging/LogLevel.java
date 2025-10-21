package com.vtnet.netat.db.logging;

/**
 * Log levels for database operations.
 * Controls verbosity of database logging.
 */
public enum LogLevel {
    TRACE(0, "TRACE"),
    DEBUG(1, "DEBUG"),
    INFO(2, "INFO"),
    WARN(3, "WARN"),
    ERROR(4, "ERROR"),
    OFF(5, "OFF");

    private final int value;
    private final String name;

    LogLevel(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled(LogLevel currentLevel) {
        return this.value >= currentLevel.value;
    }
}