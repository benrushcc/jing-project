package io.jingproject.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class is used by an annotation processor.
 *
 * <p>Classes marked with {@code @ProcessorApi} are referenced directly
 * by the annotation processor, and their class names must remain stable.
 * Renaming or refactoring such classes may break the code generation process.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ProcessorApi {
}
