package io.jingproject.marshalltest.test;

import io.jingproject.marshall.NamingConvention;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class NamingConventionTest {

    @Test
    public void testTargetIllegal() {
        for (String s : List.of("Ab", "aB", "ABb", "AB", "JSONTest", "HelloWORLD", "someA", "SomeA")) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.UPPER_SNAKE_CASE, s));
        }
        for (String s : List.of("a_b", "a_bc", "ab_c", "some_a", "a_bcd")) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.UPPER_SNAKE_CASE, s));
        }
        for (String s : List.of("AB", "ABc", "AbC", "AbcdE", "ABcde")) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.UPPER_SNAKE_CASE, s));
        }
    }

    @Test
    public void testIllegalCase() {
        List<String> illegalStrs = List.of(
                "",
                "_",
                "-abc",
                "abc-",
                "-abc-",
                "a_b",
                "a-b",
                "ab_c",
                "a-bc",
                "hello__world",
                "hello_world-test"
        );
        for (String illegalStr : illegalStrs) {
            for (NamingConvention f : NamingConvention.values()) {
                if(f != NamingConvention.ORIGINAL) {
                    for (NamingConvention t : NamingConvention.values()) {
                        if(t != NamingConvention.ORIGINAL && f != t) {
                            Assertions.assertThrows(IllegalArgumentException.class, () -> NamingConvention.cast(f, t, illegalStr),
                                    "Failed, from : " + f + ", to : " + t + ", str : " + illegalStr);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testCamelCase() {
        String base = "helloWorld";
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.UPPER_SNAKE_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.UPPER_KEBAB_CASE, base));

        String single = "hello";
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.SNAKE_CASE, single));
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.KEBAB_CASE, single));
        Assertions.assertEquals("Hello", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.PASCAL_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.UPPER_SNAKE_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.UPPER_KEBAB_CASE, single));
    }

    @Test
    public void testSnakeCase() {
        String base = "hello_world";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.UPPER_SNAKE_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.UPPER_KEBAB_CASE, base));

        String single = "hello";
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.CAMEL_CASE, single));
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.KEBAB_CASE, single));
        Assertions.assertEquals("Hello", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.PASCAL_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.UPPER_SNAKE_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.UPPER_KEBAB_CASE, single));
    }

    @Test
    public void testKebabCase() {
        String base = "hello-world";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.UPPER_SNAKE_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.UPPER_KEBAB_CASE, base));

        String single = "hello";
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.CAMEL_CASE, single));
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.SNAKE_CASE, single));
        Assertions.assertEquals("Hello", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.PASCAL_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.UPPER_SNAKE_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.UPPER_KEBAB_CASE, single));
    }

    @Test
    public void testPascalCase() {
        String base = "HelloWorld";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.UPPER_SNAKE_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.UPPER_KEBAB_CASE, base));

        String single = "Hello";
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.CAMEL_CASE, single));
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.SNAKE_CASE, single));
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.KEBAB_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.UPPER_SNAKE_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.UPPER_KEBAB_CASE, single));
    }

    @Test
    public void testUpperSnakeCase() {
        String base = "HELLO_WORLD";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.UPPER_KEBAB_CASE, base));

        String single = "HELLO";
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.CAMEL_CASE, single));
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.SNAKE_CASE, single));
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.KEBAB_CASE, single));
        Assertions.assertEquals("Hello", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.PASCAL_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.UPPER_KEBAB_CASE, single));
    }

    @Test
    public void testUpperKebabCase() {
        String base = "HELLO-WORLD";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.UPPER_SNAKE_CASE, base));

        String single = "HELLO";
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.CAMEL_CASE, single));
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.SNAKE_CASE, single));
        Assertions.assertEquals("hello", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.KEBAB_CASE, single));
        Assertions.assertEquals("Hello", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.PASCAL_CASE, single));
        Assertions.assertEquals("HELLO", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.UPPER_SNAKE_CASE, single));
    }
}
