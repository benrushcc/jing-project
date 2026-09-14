package io.jingproject.marshallcbor;

@FunctionalInterface
public interface CborDeserializeFunc {
    CborDeserializeResult deserialize(byte firstByte, CborDeserializerContext context);
}