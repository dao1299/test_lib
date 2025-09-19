package com.vtnet.netat.core.exceptions;

public class StepFailException extends RuntimeException {

    private String keywordName;
    private String screenshotPath;

    public StepFailException(String message, Throwable cause, String keywordName) {
        super(message, cause);
        this.keywordName = keywordName;
    }

    public String getKeywordName() {
        return keywordName;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    /**
     * Ghi đè phương thức getMessage để cung cấp một báo cáo lỗi chi tiết.
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("STEP FAILED: ").append(super.getMessage());
        sb.append("\n\tKeyword: ").append(keywordName);

        if (screenshotPath != null) {
            sb.append("\n\tScreenshot: ").append(screenshotPath);
        }

        if (getCause() != null) {
            sb.append("\n\tRoot Cause: ").append(getCause().getMessage());
        }

        return sb.toString();
    }
}