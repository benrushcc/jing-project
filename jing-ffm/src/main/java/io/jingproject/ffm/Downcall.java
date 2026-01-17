package io.jingproject.ffm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Downcall marked annotations for calling into shared library functions at runtime
 * The JVM could support variadic functions, heap segment arguments, or VM-level error handling, but doing so would make the system overly complex.
 * Keeping downcalls straightforward and lightweight is our first concern.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Downcall {
    /**
     * Linked methodName in shared library, it's recommended to use a snake-case string as C function names
     */
    String methodName();

    /**
     * Whether the return value is a constant, if true, the returned value would be cached and constant folded, this option is usually used when returning a MACRO value from native
     */
    boolean constant() default false;

    /**
     * Whether the function could return immediately without extra checking, this option is dangerous because it completely removes safepoint check
     */
    boolean critical() default false;
}
