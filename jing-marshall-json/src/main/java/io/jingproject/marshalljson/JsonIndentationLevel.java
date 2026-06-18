package io.jingproject.marshalljson;

/**
 * Represents the indentation level for JSON serialization.
 * Determines how many spaces are used to indent nested fields.
 */
public enum JsonIndentationLevel {
    /**
     * No indentation; the JSON output will be compact without any extra spaces.
     */
    NONE,

    /**
     * Indentation with two spaces.
     */
    TWO,

    /**
     * Indentation with four spaces.
     */
    FOUR
}
