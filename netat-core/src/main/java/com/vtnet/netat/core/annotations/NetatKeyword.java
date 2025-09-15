package com.vtnet.netat.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation để đánh dấu các method là NETAT keywords cho automation testing
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NetatKeyword {

    /**
     * Tên của keyword (unique identifier)
     */
    String name(); // Required

    /**
     * Mô tả chức năng của keyword
     */
    String description(); // Required - không có default

    /**
     * Category/nhóm của keyword (WEB, MOBILE, API, DB, REPORT, etc.)
     */
    String category(); // Required - không có default

    /**
     * Danh sách parameters của keyword
     */
    String[] parameters() default {}; // Required khi có tham số

    /**
     * Giá trị trả về của keyword (kiểu dữ liệu và mô tả)
     */
    String returnValue(); // Required - không có default

    // Các field không bắt buộc bên dưới

    /**
     * Ví dụ sử dụng keyword
     */
    String example() default "";

    /**
     * Các điều kiện tiên quyết phải thỏa mãn trước khi sử dụng keyword
     */
    String[] prerequisites() default {};

    /**
     * Danh sách các exception có thể được ném ra khi sử dụng keyword
     */
    String[] exceptions() default {};

    /**
     * Nền tảng hỗ trợ (ANDROID, IOS, WEB, API, ALL, etc.)
     */
    String platform() default "ALL";

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

    /**
     * Cấp độ ổn định của keyword (STABLE, BETA, EXPERIMENTAL)
     */
    String stability() default "STABLE";

    /**
     * Mức độ tác động đến hệ thống (READ_ONLY, MODIFY, RESET)
     */
    String systemImpact() default "READ_ONLY";
}
