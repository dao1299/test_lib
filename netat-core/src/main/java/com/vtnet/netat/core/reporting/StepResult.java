package com.vtnet.netat.core.reporting;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Class để lưu trữ kết quả execution của một keyword/step
 * Được sử dụng cho reporting và logging
 */
public class StepResult {

    public enum Status {
        PASSED,
        FAILED,
        SKIPPED,
        WARNING,
        INFO
    }

    // Basic information
    private String keywordName;
    private String description;
    private String parameters;
    private Status status;
    private Object result;

    // Timing information
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;

    // Error information
    private Exception error;
    private String errorMessage;
    private String stackTrace;

    // Media attachments
    private String screenshotPath;
    private String videoPath;
    private List<String> attachments = new ArrayList<>();

    // Additional metadata
    private String category;
    private int stepNumber;
    private String testCaseName;
    private String testSuiteName;

    // Constructors
    public StepResult() {
    }

    public StepResult(String keywordName, String description) {
        this.keywordName = keywordName;
        this.description = description;
        this.startTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getKeywordName() {
        return keywordName;
    }

    public void setKeywordName(String keywordName) {
        this.keywordName = keywordName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if (startTime != null && endTime != null) {
            this.durationMs = Duration.between(startTime, endTime).toMillis();
        }
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * Get duration in human readable format
     */
    public String getDurationFormatted() {
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return String.format("%.2fs", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
        if (error != null) {
            this.errorMessage = error.getMessage();
            this.stackTrace = getStackTraceAsString(error);
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
        if (screenshotPath != null && !attachments.contains(screenshotPath)) {
            attachments.add(screenshotPath);
        }
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
        if (videoPath != null && !attachments.contains(videoPath)) {
            attachments.add(videoPath);
        }
    }

    public List<String> getAttachments() {
        return new ArrayList<>(attachments);
    }

    public void addAttachment(String attachmentPath) {
        if (attachmentPath != null && !attachments.contains(attachmentPath)) {
            attachments.add(attachmentPath);
        }
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getTestSuiteName() {
        return testSuiteName;
    }

    public void setTestSuiteName(String testSuiteName) {
        this.testSuiteName = testSuiteName;
    }

    // Utility methods
    public boolean isPassed() {
        return Status.PASSED.equals(status);
    }

    public boolean isFailed() {
        return Status.FAILED.equals(status);
    }

    public boolean isSkipped() {
        return Status.SKIPPED.equals(status);
    }

    /**
     * Convert exception stack trace to string
     */
    private String getStackTraceAsString(Exception e) {
        if (e == null) return null;

        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    @Override
    public String toString() {
        return String.format("StepResult{keyword='%s', status=%s, duration=%s, error='%s'}",
                keywordName, status, getDurationFormatted(), errorMessage);
    }
}