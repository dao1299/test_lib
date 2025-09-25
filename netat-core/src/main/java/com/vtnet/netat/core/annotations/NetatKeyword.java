package com.vtnet.netat.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetatKeyword {

    /**
     * Tên của keyword
     */
    String name();

    /**
     * Mô tả chức năng của keyword
     */
    String description();

    /**
     * Danh mục của keyword (ví dụ: "Web UI", "Mobile", "Database", "API", "Utility")
     */
    String category();

    String subCategory() default "";

    /**
     * Danh sách các tham số đầu vào
     * Định dạng: "parameterName|parameterType|required|description"
     * Ví dụ: "to|ObjectUI|Yes|Represents a web element."
     */
    String[] parameters() default {};

    /**
     * Thông tin về giá trị trả về
     * Định dạng: "returnType|description"
     * Ví dụ: "void|No return value" hoặc "boolean|Returns true if element is visible, false otherwise"
     */
    String returnValue() default "void - Không trả về giá trị";

    /**
     * Ví dụ sử dụng keyword
     */
    String example() default "";

    /**
     * Ghi chú quan trọng về keyword (điều kiện áp dụng, hạn chế, lưu ý đặc biệt)
     */
    String note() default "";
}
