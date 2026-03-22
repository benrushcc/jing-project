package io.jingproject.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a provider for automatic SPI (Service Provider Interface) generation.
 *
 * <p>Classes annotated with {@code @Provider} will be processed by an annotation processor,
 * which automatically generates the necessary SPI-related information and imports.
 * This eliminates the need to manually specify the SPI configuration in {@code module-info.java},
 * streamlining the process of declaring services for modular Java applications.</p>
 *
 * <p>Additionally, this annotation can also be used by the annotation processor when generating
 * other classes, ensuring the correct SPI information is included during code generation.</p>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Provider {
    /**
     * The target interface that this provider is associated with.
     */
    Class<?> target();
}
