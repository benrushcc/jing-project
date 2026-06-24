package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

@FunctionalInterface
public interface JsonSerializeFunc {
    JsonSerializeResult serialize(JsonSerializerOption option, WriteBuffer writeBuffer, Object instance, int indent);
}
