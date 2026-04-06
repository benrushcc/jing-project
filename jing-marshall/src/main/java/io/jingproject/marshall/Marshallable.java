package io.jingproject.marshall;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Marshallable {
    String name() default "";

    NamingConvention from() default NamingConvention.ORIGINAL;

    NamingConvention to() default NamingConvention.ORIGINAL;
}
