package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JsonSerializerMapNode extends JsonSerializerNode {
    private final int cap;
    private final List<Object> kvs;
    private final JsonValueSerializer valueSerializer;

    public JsonSerializerMapNode(JsonSerializerOption option, WriteBuffer writeBuffer, int indent,
                                 Map<?, ?> map, JsonValueSerializer valueSerializer) {
        super(option, writeBuffer, indent);
        this.cap = map.size();
        this.kvs = new ArrayList<>(Math.multiplyExact(cap, 2));
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            kvs.add(entry.getKey());
            kvs.add(entry.getValue());
        }
        this.valueSerializer = valueSerializer;
    }

    @Override
    protected int capacity() {
        return cap;
    }

    @Override
    protected void init() {
        JsonSerializeUtil.serializeObjStart(writeBuffer);
    }

    @Override
    protected JsonSerializerNode process(int index) {
        int kIdx = index << 1;
        Object key = kvs.get(kIdx);
        if(key == null) {
            return null;
        }
        Object value = kvs.get(kIdx + 1);
        if(value == null) {
            if(option.serializeNullInObjOrMap()) {
                serializeKey(key);
                JsonSerializeUtil.serializeNull(writeBuffer);
            }
            return null;
        }
        serializeKey(key);
        return valueSerializer.serialize(value, option, writeBuffer, indent);
    }

    private void serializeKey(Object key) {
        serializeSep();
        if (key instanceof CharSequence charSequence) {
            JsonSerializeUtil.serializeEscapedCharSequence(charSequence, writeBuffer);
        } else {
            throw new JsonSerializerException("unsupported json key : " + key);
        }
        JsonSerializeUtil.serializeKvSep(writeBuffer);
    }

    @Override
    protected void end() {
        JsonSerializeUtil.serializeObjEnd(writeBuffer);
    }

}
