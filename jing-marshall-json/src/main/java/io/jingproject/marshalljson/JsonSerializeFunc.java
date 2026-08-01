package io.jingproject.marshalljson;

@FunctionalInterface
public interface JsonSerializeFunc {
    JsonSerializeResult serialize(Object instance, int indent, JsonSerializerContext context);
}
