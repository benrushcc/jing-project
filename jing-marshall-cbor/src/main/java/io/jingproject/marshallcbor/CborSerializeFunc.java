package io.jingproject.marshallcbor;

@FunctionalInterface
public interface CborSerializeFunc {
    CborSerializeResult serialize(Object instance, CborSerializerContext context);
}