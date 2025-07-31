package com.vtnet.netat.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation để đánh dấu các method là NETAT keywords
 * Được sử dụng bởi Keyword Compiler để generate documentation và validation
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NetatKeyword {

    /**
     * Tên của keyword (unique identifier)
     */
    String name();

    /**
     * Mô tả chức năng của keyword
     */
    String description() default "";

    /**
     * Category/nhóm của keyword (WEB, MOBILE, API, DB, REPORT, etc.)
     */
    String category() default "GENERAL";

    /**
     * Danh sách parameters của keyword
     */
    String[] parameters() default {};

    /**
     * Ví dụ sử dụng keyword
     */
    String example() default "";

    /**
     * Version của keyword (để track changes)
     */
    String version() default "1.0.0";

    /**
     * Keyword có deprecated không
     */
    boolean deprecated() default false;

    /**
     * Message khi keyword deprecated
     */
    String deprecatedMessage() default "";

    /**
     * Tags để group keywords
     */
    String[] tags() default {};

    /**
     * Keyword có cần screenshot không (mặc định true cho UI keywords)
     */
    boolean screenshot() default true;

    /**
     * Timeout mặc định cho keyword (seconds)
     */
    int timeout() default 30;

    /**
     * Keyword có retry khi fail không
     */
    boolean retryOnFailure() default false;

    /**
     * Số lần retry tối đa
     */
    int maxRetries() default 3;
}
