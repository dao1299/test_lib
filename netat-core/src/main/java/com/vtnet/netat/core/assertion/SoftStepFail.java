package com.vtnet.netat.core.assertion;

public class SoftStepFail extends RuntimeException {
    public SoftStepFail(String message) { super(message); }
    public SoftStepFail(String message, Throwable cause) { super(message, cause); }
}
