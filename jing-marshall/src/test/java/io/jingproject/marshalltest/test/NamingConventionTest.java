package io.jingproject.marshalltest.test;

import io.jingproject.marshall.NamingConvention;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NamingConventionTest {

    @Test
    public void testCamelCase() {
        String base = "helloWorld";
        String shortBase = "hW";
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.UPPER_SNAKE_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.UPPER_KEBAB_CASE, base));
        Assertions.assertEquals("h_w", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.SNAKE_CASE, shortBase));
        Assertions.assertEquals("h-w", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.KEBAB_CASE, shortBase));
        Assertions.assertEquals("HW", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.PASCAL_CASE, shortBase));
        Assertions.assertEquals("H_W", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.UPPER_SNAKE_CASE, shortBase));
        Assertions.assertEquals("H-W", NamingConvention.cast(NamingConvention.CAMEL_CASE, NamingConvention.UPPER_KEBAB_CASE, shortBase));
    }

    @Test
    public void testSnakeCase() {
        String base = "hello_world";
        String shortBase = "h_w";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.UPPER_SNAKE_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.UPPER_KEBAB_CASE, base));
        Assertions.assertEquals("hW", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.CAMEL_CASE, shortBase));
        Assertions.assertEquals("h-w", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.KEBAB_CASE, shortBase));
        Assertions.assertEquals("HW", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.PASCAL_CASE, shortBase));
        Assertions.assertEquals("H_W", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.UPPER_SNAKE_CASE, shortBase));
        Assertions.assertEquals("H-W", NamingConvention.cast(NamingConvention.SNAKE_CASE, NamingConvention.UPPER_KEBAB_CASE, shortBase));
    }

    @Test
    public void testKebabCase() {
        String base = "hello-world";
        String shortBase = "h-w";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.UPPER_SNAKE_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.UPPER_KEBAB_CASE, base));
        Assertions.assertEquals("hW", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.CAMEL_CASE, shortBase));
        Assertions.assertEquals("h_w", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.SNAKE_CASE, shortBase));
        Assertions.assertEquals("HW", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.PASCAL_CASE, shortBase));
        Assertions.assertEquals("H_W", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.UPPER_SNAKE_CASE, shortBase));
        Assertions.assertEquals("H-W", NamingConvention.cast(NamingConvention.KEBAB_CASE, NamingConvention.UPPER_KEBAB_CASE, shortBase));
    }

    @Test
    public void testPascalCase() {
        String base = "HelloWorld";
        String shortBase = "HW";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.UPPER_SNAKE_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.UPPER_KEBAB_CASE, base));
        Assertions.assertEquals("hW", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.CAMEL_CASE, shortBase));
        Assertions.assertEquals("h_w", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.SNAKE_CASE, shortBase));
        Assertions.assertEquals("h-w", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.KEBAB_CASE, shortBase));
        Assertions.assertEquals("H_W", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.UPPER_SNAKE_CASE, shortBase));
        Assertions.assertEquals("H-W", NamingConvention.cast(NamingConvention.PASCAL_CASE, NamingConvention.UPPER_KEBAB_CASE, shortBase));
    }

    @Test
    public void testUpperSnakeCase() {
        String base = "HELLO_WORLD";
        String shortBase = "H_W";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO-WORLD", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.UPPER_KEBAB_CASE, base));
        Assertions.assertEquals("hW", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.CAMEL_CASE, shortBase));
        Assertions.assertEquals("h_w", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.SNAKE_CASE, shortBase));
        Assertions.assertEquals("h-w", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.KEBAB_CASE, shortBase));
        Assertions.assertEquals("HW", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.PASCAL_CASE, shortBase));
        Assertions.assertEquals("H-W", NamingConvention.cast(NamingConvention.UPPER_SNAKE_CASE, NamingConvention.UPPER_KEBAB_CASE, shortBase));
    }

    @Test
    public void testUpperKebabCase() {
        String base = "HELLO-WORLD";
        String shortBase = "H-W";
        Assertions.assertEquals("helloWorld", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.CAMEL_CASE, base));
        Assertions.assertEquals("hello_world", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.SNAKE_CASE, base));
        Assertions.assertEquals("hello-world", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.KEBAB_CASE, base));
        Assertions.assertEquals("HelloWorld", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.PASCAL_CASE, base));
        Assertions.assertEquals("HELLO_WORLD", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.UPPER_SNAKE_CASE, base));
        Assertions.assertEquals("hW", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.CAMEL_CASE, shortBase));
        Assertions.assertEquals("h_w", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.SNAKE_CASE, shortBase));
        Assertions.assertEquals("h-w", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.KEBAB_CASE, shortBase));
        Assertions.assertEquals("HW", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.PASCAL_CASE, shortBase));
        Assertions.assertEquals("H_W", NamingConvention.cast(NamingConvention.UPPER_KEBAB_CASE, NamingConvention.UPPER_SNAKE_CASE, shortBase));
    }
}
