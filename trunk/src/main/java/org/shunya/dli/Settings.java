package org.shunya.dli;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Settings {
    boolean ignore() default false;
    boolean editable() default true;
    String description() default "";
}
