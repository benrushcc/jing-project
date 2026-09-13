package io.jingproject.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// indicates that the annotated class will be used by an annotation processor.
// classes marked with @ProcessorApi are referenced directly by the annotation
// processor, and their class names must remain stable.
// renaming or refactoring such classes may break the code generation process.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ProcessorApi {
}
