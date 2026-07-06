package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

public abstract class JsonDeserializerNode {
    private boolean initialized = false;

    protected final boolean init() {
        boolean r = initialized;
        initialized = true;
        return r;
    }

    protected void reset() {
        initialized = false;
    }

    protected abstract JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context);
}
