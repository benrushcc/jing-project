package io.jingproject.marshall;

public enum NamingConvention {
    ORIGINAL,

    CAMEL_CASE, // helloWorld

    SNAKE_CASE, // hello_world

    KEBAB_CASE, // hello-world

    PASCAL_CASE, // HelloWorld

    UPPER_SNAKE_CASE, // HELLO_WORLD

    UPPER_KEBAB_CASE; // HELLO-WORLD

    public static String cast(NamingConvention from, NamingConvention to, String name) {
        // TODO
        return null;
    }
}
