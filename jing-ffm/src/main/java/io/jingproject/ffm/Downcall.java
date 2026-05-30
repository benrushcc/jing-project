package io.jingproject.ffm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * downcall marked annotations for calling into shared library functions at runtime
 * the JVM could support variadic functions, heap segment arguments, or VM-level error handling, but doing so would make the system overly complex.
 * keeping downcalls straightforward and lightweight is our first concern.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Downcall {
    /**
     * linked methodName in shared library, it's recommended to use a snake-case string as C function names
     */
    String methodName();

    /**
     * whether the return value is a constant, if true, the returned value would be cached and constant folded, this option is usually used when returning a MACRO value from native
     */
    boolean constant() default false;

    /**
     * whether the function could return immediately without extra checking, this option is dangerous because it removes safepoint check
     */
    boolean critical() default false;
}
