package io.jingproject.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// marks a class as a provider for automatic SPI (Service Provider Interface) generation.
// classes annotated with @Provider are processed by an annotation processor,
// which automatically generates the necessary SPI-related information and imports.
// this eliminates the need to manually specify the SPI configuration in module-info.java,
// streamlining the process of declaring services for modular Java applications.
// additionally, this annotation can also be used by the annotation processor when
// generating other classes, ensuring the correct SPI information is included.
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Provider {
    // the target interface that this provider is associated with.
    Class<?> target();
}
