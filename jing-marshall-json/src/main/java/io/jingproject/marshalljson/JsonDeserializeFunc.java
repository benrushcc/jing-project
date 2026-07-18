package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

@FunctionalInterface
public interface JsonDeserializeFunc {
    JsonDeserializeResult deserialize(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, byte firstByte);
}
