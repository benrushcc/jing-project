package io.jingproject.marshall;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.SOURCE)
public @interface MarshallAttr {
    /**
     * The mapped name to replace the original field name during marshalling/unmarshalling.
     * If empty, the original field name would be used.
     */
    String mappedName() default "";

    /**
     * Whether to skip this field during serialization.
     * Note: This flag is ignored when the annotated field is an enum constant.
     * For enum-typed fields, the decision is based on the annotation on the field itself,
     * not on the individual enum constants, to ensure consistent behavior.
     */
    boolean skipSerializing() default false;

    /**
     * Whether to skip this field during deserialization.
     * Note: This flag is ignored when the annotated field is an enum constant.
     * For enum-typed fields, the decision is based on the annotation on the field itself,
     * not on the individual enum constants, to ensure consistent behavior.
     */
    boolean skipDeserializing() default false;
}
