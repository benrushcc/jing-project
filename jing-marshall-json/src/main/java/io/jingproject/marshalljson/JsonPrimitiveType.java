package io.jingproject.marshalljson;

/**
 * A sealed interface representing the primitive value types natively supported in JSON serializer.
 * The permitted implementations cover boolean, number, and string values.
 */
public sealed interface JsonPrimitiveType
        permits JsonBoolType, JsonNumberType, JsonStrType {

}
