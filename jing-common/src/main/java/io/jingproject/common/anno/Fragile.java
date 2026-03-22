package io.jingproject.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class is fragile and prone to misuse.
 *
 * <p>This annotation is intended primarily for internal use within the Jing library.
 * Classes marked with {@code @Fragile} may lack stable fallback mechanisms and
 * could lead to unexpected behavior if used incorrectly.</p>
 *
 * <p>External developers should exercise extreme caution when using such classes.
 * Use at your own risk, as the library provides no guarantees of safety or stability
 * for these components.</p>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Fragile {

}
