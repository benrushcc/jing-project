package io.jingproject.marshall;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * customizes field marshalling when used on a field inside a type annotated with @marshallable;
 * allows overriding the field name and skipping serialization or deserialization.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.SOURCE)
public @interface MarshallAttr {
    /**
     * the mapped name to replace the original field name during marshalling/unmarshalling.
     * if empty, the original field name would be used.
     */
    String mappedName() default "";

    /**
     * whether to skip this field during serialization.
     * note: this flag is ignored when the annotated field is an enum constant.
     * for enum-typed fields, the decision is based on the annotation on the field itself,
     * not on the individual enum constants, to ensure consistent behavior.
     */
    boolean skipSerializing() default false;

    /**
     * whether to skip this field during deserialization.
     * note: this flag is ignored when the annotated field is an enum constant.
     * for enum-typed fields, the decision is based on the annotation on the field itself,
     * not on the individual enum constants, to ensure consistent behavior.
     */
    boolean skipDeserializing() default false;
}
