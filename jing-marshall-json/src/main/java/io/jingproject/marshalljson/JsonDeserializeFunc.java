package io.jingproject.marshalljson;

@FunctionalInterface
public interface JsonDeserializeFunc {
    JsonDeserializeResult deserialize(byte firstByte, JsonDeserializerContext context);
}
