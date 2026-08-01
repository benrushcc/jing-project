package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

import java.util.List;

public final class JsonDeserializerListNode extends JsonDeserializerNode {
    private List<?> list;
    private Class<?> type;

    public void init(List<?> list, Class<?> type) {
        this.list = list;
        this.type = type;
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, Object v) {
        return null;
    }
}
