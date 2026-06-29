package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

public abstract class JsonDeserializerNode {
    protected abstract JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer);
}
