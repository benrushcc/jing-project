package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

import java.util.Map;

public final class JsonDeserializerMapNode extends JsonDeserializerNode {
    private Map<?, ?> map;

    public void init(Map<?, ?> map) {
        this.map = map;
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, Object v) {
        return null;
    }
}
