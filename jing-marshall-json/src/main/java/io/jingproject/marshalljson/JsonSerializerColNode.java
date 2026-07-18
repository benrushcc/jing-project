package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.Iterator;

public final class JsonSerializerColNode extends JsonSerializerNode {
    private int size;
    private Iterator<?> iter;
    private JsonSerializeFunc func;

    public void init(JsonSerializerOption option, int size, Iterator<?> iter, Class<?> elementType, int indent) {
        this.size = size;
        this.iter = iter;
        this.func = JsonSerializeUtil.valueSerializeFunc(option, elementType);
        this.indent = indent;
        this.index = 0;
        this.written = false;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer, JsonSerializerContext context) {
        for(int i = index; i < size; i++) {
            Object instance = iter.next();
            if(instance == null) {
                JsonSerializeUtil.serializeNull(writeBuffer);
                continue ;
            }
            serializeSep(option, writeBuffer);
            JsonSerializeResult r = func.serialize(option, writeBuffer, context, instance, indent);
            if(r == JsonSerializeResult.Continue) {
                continue ;
            }
            index = i + 1;
            return r;
        }
        JsonSerializeUtil.serializeArrayEnd(writeBuffer);
        return JsonSerializeResult.Finished;
    }
}
