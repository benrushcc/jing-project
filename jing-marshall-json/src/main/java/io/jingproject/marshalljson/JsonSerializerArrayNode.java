package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class JsonSerializerArrayNode extends JsonSerializerNode {
    private final List<Object> values;
    private final JsonValueSerializer valueSerializer;

    public JsonSerializerArrayNode(JsonSerializerOption option, WriteBuffer writeBuffer, int indent,
                                   Object[] arr, JsonValueSerializer valueSerializer) {
        super(option, writeBuffer, indent);
        this.values = Arrays.asList(arr);
        this.valueSerializer = valueSerializer;
    }

    public JsonSerializerArrayNode(JsonSerializerOption option, WriteBuffer writeBuffer, int indent,
                                   Collection<?> col, JsonValueSerializer valueSerializer) {
        super(option, writeBuffer, indent);
        this.values = new ArrayList<>(col);
        this.valueSerializer = valueSerializer;
    }

    @Override
    protected int capacity() {
        return values.size();
    }

    @Override
    protected void init() {
        JsonSerializeUtil.serializeArrayStart(writeBuffer);
    }

    @Override
    protected JsonSerializerNode process(int index) {
        Object o = values.get(index);
        if(o == null) {
            JsonSerializeUtil.serializeNull(writeBuffer);
            return null;
        }
        serializeSep();
        return valueSerializer.serialize(o, option, writeBuffer, indent);
    }

    @Override
    protected void end() {
        JsonSerializeUtil.serializeArrayEnd(writeBuffer);
    }
}
