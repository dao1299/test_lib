package com.vtnet.netat.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetatKeyword {

    String name();

    String description();

    String category();

    String subCategory() default "";

    String[] parameters() default {};

    String returnValue() default "void - Không trả về giá trị";

    String example() default "";

    String note() default "";

    String explainer() default "";
}
