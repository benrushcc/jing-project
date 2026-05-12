package io.jingproject.ffm;

import io.jingproject.common.Os;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface FFM {
    String VM = "jvm";

    /**
     * specifies the shared library name. the default value is JVM's internal lookup.
     * you should never name your library "jvm".
     */
    String libraryName() default VM;

    // 默认情况下支持windows linux macos，以后可能会随着jing项目的发展而引入更多操作系统的支持，该值需要严格与Os中的枚举值保持一致
    Os[] supportedOS() default {Os.WINDOWS, Os.LINUX, Os.MACOS};
}
