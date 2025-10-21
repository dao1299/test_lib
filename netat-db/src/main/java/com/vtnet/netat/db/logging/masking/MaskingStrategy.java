package com.vtnet.netat.db.logging.masking;

public enum MaskingStrategy {
    FULL("***"),
    PARTIAL_START("***xyz"),
    PARTIAL_END("abc***"),
    PARTIAL_EMAIL("u***@domain.com"),
    LAST_4_DIGITS("****1234"),
    HASH("sha256:hash");

    private final String example;

    MaskingStrategy(String example) {
        this.example = example;
    }

    public String getExample() {
        return example;
    }
}