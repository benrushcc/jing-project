package io.jingproject.marshall;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MarshallAttr {
    String mappedName() default "";

    boolean skipSerializing() default false;

    boolean skipDeserializing() default false;
}
