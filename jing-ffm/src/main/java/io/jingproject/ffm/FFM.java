package io.jingproject.ffm;

import io.jingproject.common.Os;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface FFM {
    String VM = "vm";

    /**
     * Specifies the shared library name. Defaults to JVM's internal lookup.
     * Don't name your library "vm".
     */
    String libraryName() default VM;

    Os[] supportedOS() default {Os.WINDOWS, Os.LINUX, Os.MACOS};
}
