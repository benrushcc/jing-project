package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.Iterator;
import java.util.Map;

public final class JsonSerializerMapNode extends JsonSerializerNode {
    private int size;
    private Iterator<? extends Map.Entry<?, ?>> iter;
    private JsonSerializeFunc func;

    public void init(int size, Iterator<? extends Map.Entry<?, ?>> iter, Class<?> valueType, int indent, JsonSerializerContext c) {
        this.size = size;
        this.iter = iter;
        this.func = c.valueSerializeFunc(valueType);
        this.indent = indent;
        this.index = 0;
        this.written = false;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerContext c) {
        for(int i = index; i < size; i++) {
            Map.Entry<?, ?> entry = iter.next();
            Object key = entry.getKey();
            if(key == null) {
                continue ;
            }
            Object value = entry.getValue();
            if(value == null) {
                if(c.option().serializeNullInObjOrMap()) {
                    serializeKey(key, c);
                    c.serializeNull();
                }
                continue ;
            }
            serializeKey(key, c);
            JsonSerializeResult r = func.serialize(value, indent, c);
            if(r == JsonSerializeResult.Continue) {
                continue ;
            }
            index = i + 1;
            return r;
        }
        c.writeBuffer().writeByte((byte) '}');
        return JsonSerializeResult.Finished;
    }

    private void serializeKey(Object key, JsonSerializerContext c) {
        serializeSep(c);
        if (key instanceof String str) {
            c.serializeEscapedString(str);
        } else if(key instanceof CharSequence charSequence) {
            c.serializeEscapedCharSequence(charSequence);
        } else {
            throw new JsonSerializerException("unsupported json key : " + key);
        }
        c.writeBuffer().writeBytes((byte) ':', (byte) ' ');
    }
}
