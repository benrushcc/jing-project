package io.jingproject.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method is used by an annotation processor.
 *
 * <p>Methods marked with {@code @ProcessorMethod} are directly referenced
 * by the annotation processor and hard-coded into the generated source code.
 * Therefore, the method name and parameter list must remain unchanged.
 * Changing them may cause compilation errors or runtime issues in generated code.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface ProcessorMethod {
}
