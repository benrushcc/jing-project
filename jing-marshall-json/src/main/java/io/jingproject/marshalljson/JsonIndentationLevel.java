package io.jingproject.marshalljson;

/**
 * represents the indentation level for json serialization.
 */
public enum JsonIndentationLevel {
    /**
     * no indentation, the json output will be compact without any extra spaces.
     */
    NONE,

    /**
     * indentation with two spaces.
     */
    TWO,

    /**
     * indentation with four spaces.
     */
    FOUR
}
