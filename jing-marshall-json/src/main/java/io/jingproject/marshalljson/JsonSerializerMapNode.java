package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.Iterator;
import java.util.Map;

public final class JsonSerializerMapNode extends JsonSerializerNode {
    private int size;
    private Iterator<? extends Map.Entry<?, ?>> iter;
    private JsonSerializeFunc func;

    public void init(JsonSerializerOption option, int size, Iterator<? extends Map.Entry<?, ?>> iter, Class<?> valueType, int indent) {
        this.size = size;
        this.iter = iter;
        this.func = JsonSerializeUtil.valueSerializeFunc(option, valueType);
        this.indent = indent;
        this.index = 0;
        this.written = false;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer, JsonSerializerContext context) {
        for(int i = index; i < size; i++) {
            Map.Entry<?, ?> entry = iter.next();
            Object key = entry.getKey();
            if(key == null) {
                continue ;
            }
            Object value = entry.getValue();
            if(value == null) {
                if(option.serializeNullInObjOrMap()) {
                    serializeKey(option, writeBuffer, context, key);
                    JsonSerializeUtil.serializeNull(writeBuffer);
                }
                continue ;
            }
            serializeKey(option, writeBuffer, context, key);
            JsonSerializeResult r = func.serialize(option, writeBuffer, context, value, indent);
            if(r == JsonSerializeResult.Continue) {
                continue ;
            }
            index = i + 1;
            return r;
        }
        JsonSerializeUtil.serializeObjEnd(writeBuffer);
        return JsonSerializeResult.Finished;
    }

    private void serializeKey(JsonSerializerOption option, WriteBuffer writeBuffer, JsonSerializerContext context, Object key) {
        serializeSep(option, writeBuffer);
        if (key instanceof String str) {
            JsonSerializeUtil.serializeEscapedString(str, writeBuffer, context);
        } else if(key instanceof CharSequence charSequence) {
            JsonSerializeUtil.serializeEscapedCharSequence(charSequence, writeBuffer, context);
        } else {
            throw new JsonSerializerException("unsupported json key : " + key);
        }
        JsonSerializeUtil.serializeKvSep(writeBuffer);
    }
}
