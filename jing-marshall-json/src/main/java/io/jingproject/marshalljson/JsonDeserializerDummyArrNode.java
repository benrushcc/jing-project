package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

public final class JsonDeserializerDummyArrNode extends JsonDeserializerNode {
    @Override
    protected JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, Object v) {
        return null;
    }
}
