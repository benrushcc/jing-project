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

    // specifies the shared library name.
    // the default value is the JVM's internal lookup.
    // you should never name your library "jvm".
    String libraryName() default VM;

    // the default value supports windows, linux and macos.
    // more operating systems may be added as the jing project grows.
    // this value must stay strictly consistent with the Os enum values.
    Os[] supportedOS() default {Os.WINDOWS, Os.LINUX, Os.MACOS};
}
