package io.jingproject.marshall;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * marks a class as marshallable, triggers annotation processing to generate metadata and access methods.
 * from and to specify default field naming conversion; both ORIGINAL means no conversion.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Marshallable {
    NamingConvention from() default NamingConvention.ORIGINAL;

    NamingConvention to() default NamingConvention.ORIGINAL;
}
