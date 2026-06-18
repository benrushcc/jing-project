package io.jingproject.marshalljson;

/**
 * A JSON number value.
 * The number is stored as a byte array. The content may represent either an
 * integer or a floating-point number.
 * During serialization, the internal logic does not validate the format of the
 * byte array; it is trusted as-is. It is the responsibility of the implementor
 * to ensure the correctness of the byte array content.
 *
 * @param data the byte array representing the number
 */
public record JsonNumberType(byte[] data) implements JsonPrimitiveType {

}
