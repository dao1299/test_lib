package com.vtnet.netat.core.exceptions;

/**
 * Custom exception class cho NETAT platform
 * Extends RuntimeException để không cần declare throws
 */
public class NetatException extends RuntimeException {

    private String keywordName;
    private String stepDescription;
    private String screenshotPath;

    public NetatException(String message) {
        super(message);
    }

    public NetatException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetatException(String message, String keywordName) {
        super(message);
        this.keywordName = keywordName;
    }

    public NetatException(String message, Throwable cause, String keywordName) {
        super(message, cause);
        this.keywordName = keywordName;
    }

    public NetatException(String message, String keywordName, String stepDescription) {
        super(message);
        this.keywordName = keywordName;
        this.stepDescription = stepDescription;
    }

    public NetatException(String message, Throwable cause, String keywordName, String stepDescription) {
        super(message, cause);
        this.keywordName = keywordName;
        this.stepDescription = stepDescription;
    }

    // Getters and Setters
    public String getKeywordName() {
        return keywordName;
    }

    public void setKeywordName(String keywordName) {
        this.keywordName = keywordName;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    /**
     * Get detailed error message with context
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("NETAT Execution Error: ").append(getMessage());

        if (keywordName != null) {
            sb.append("\nKeyword: ").append(keywordName);
        }

        if (stepDescription != null) {
            sb.append("\nStep: ").append(stepDescription);
        }

        if (screenshotPath != null) {
            sb.append("\nScreenshot: ").append(screenshotPath);
        }

        if (getCause() != null) {
            sb.append("\nRoot Cause: ").append(getCause().getMessage());
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getDetailedMessage();
    }
}