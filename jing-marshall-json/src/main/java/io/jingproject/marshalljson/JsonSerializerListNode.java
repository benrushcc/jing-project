package io.jingproject.marshalljson;

import java.util.List;

public final class JsonSerializerListNode extends JsonSerializerNode {
    private List<?> list;
    private JsonSerializeFunc func;

    public void init(List<?> list, Class<?> elementType, int indent, JsonSerializerContext c) {
        this.list = list;
        this.func = c.valueSerializeFunc(elementType);
        this.indent = indent;
        this.index = 0;
        this.written = false;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerContext c) {
        final int size = list.size();
        for(int i = index; i < size; i++) {
            Object instance = list.get(i);
            if(instance == null) {
                c.serializeNull();
                continue ;
            }
            serializeSep(c);
            JsonSerializeResult r = func.serialize(instance, indent, c);
            if(r == JsonSerializeResult.Continue) {
                continue ;
            }
            index = i + 1;
            return r;
        }
        c.writeBuffer().writeByte((byte) ']');
        return JsonSerializeResult.Finished;
    }
}
