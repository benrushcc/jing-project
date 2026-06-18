package io.jingproject.marshalljson;

/**
 * A JSON string value.
 *
 * @param data the string content
 */
public record JsonStrType(String data) implements JsonPrimitiveType {

}
