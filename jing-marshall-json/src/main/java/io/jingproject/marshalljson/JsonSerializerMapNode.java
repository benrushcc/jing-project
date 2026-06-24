package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.Iterator;
import java.util.Map;

public final class JsonSerializerMapNode extends JsonSerializerNode {
    private int size;
    private Iterator<? extends Map.Entry<?, ?>> iter;
    private Class<?> valueType;
    private JsonSerializeFunc func;

    public void setSize(int size) {
        this.size = size;
    }

    public void setIter(Iterator<? extends Map.Entry<?, ?>> iter) {
        this.iter = iter;
    }

    public void setValueType(Class<?> valueType) {
        this.valueType = valueType;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer) {
        int index = incIndex();
        if(index == 0) {
            JsonSerializeUtil.serializeObjStart(writeBuffer);
        }
        if(index == size) {
            JsonSerializeUtil.serializeObjEnd(writeBuffer);
            return JsonSerializeResult.FINISHED;
        }
        Map.Entry<?, ?> entry = iter.next();
        Object key = entry.getKey();
        if(key == null) {
            return JsonSerializeResult.CONTINUE;
        }
        Object value = entry.getValue();
        if(value == null) {
            if(option.serializeNullInObjOrMap()) {
                serializeKey(option, writeBuffer, key);
                JsonSerializeUtil.serializeNull(writeBuffer);
            }
            return null;
        }
        if(func == null) {
            func = JsonSerializeUtil.valueSerializeFunc(option, valueType);
        }
        return func.serialize(option, writeBuffer, value, indent());
    }

    private void serializeKey(JsonSerializerOption option, WriteBuffer writeBuffer, Object key) {
        serializeSep(option, writeBuffer);
        if (key instanceof CharSequence charSequence) {
            JsonSerializeUtil.serializeEscapedCharSequence(charSequence, writeBuffer);
        } else {
            throw new JsonSerializerException("unsupported json key : " + key);
        }
        JsonSerializeUtil.serializeKvSep(writeBuffer);
    }

    @Override
    protected void reset() {
        super.reset();
        this.size = Integer.MIN_VALUE;
        this.iter = null;
        this.valueType = null;
        this.func = null;
    }
}
