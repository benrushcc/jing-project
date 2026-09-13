package io.jingproject.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// marks a class as fragile and prone to misuse.
// by design, improper use of a @Fragile class may crash the JVM,
// so no excessive defensive programming is required inside it.
// just make sure it produces the correct output for correct input.
// use at your own risk, the library provides no safety guarantees here.
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Fragile {

}
