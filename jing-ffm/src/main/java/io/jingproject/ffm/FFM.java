package io.jingproject.ffm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface FFM {
    String VM = "vm";

    /**
     *   Shared library name, using jvm internal lookup by default, you shouldn't name your dynamic library as vm
     */
    String libraryName() default VM;
}
