package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.List;

public final class JsonSerializerListNode extends JsonSerializerNode {
    private List<?> list;
    private JsonSerializeFunc func;

    public void init(JsonSerializerOption option, List<?> list, Class<?> elementType, int indent) {
        this.list = list;
        this.func = JsonSerializeUtil.valueSerializeFunc(option, elementType);
        this.indent = indent;
        this.index = 0;
        this.written = false;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer, JsonSerializerContext context) {
        final int size = list.size();
        for(int i = index; i < size; i++) {
            Object instance = list.get(i);
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
