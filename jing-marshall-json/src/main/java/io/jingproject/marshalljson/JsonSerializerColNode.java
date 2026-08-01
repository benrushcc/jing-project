package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.Iterator;

public final class JsonSerializerColNode extends JsonSerializerNode {
    private int size;
    private Iterator<?> iter;
    private JsonSerializeFunc func;

    public void init(int size, Iterator<?> iter, Class<?> elementType, int indent, JsonSerializerContext c) {
        this.size = size;
        this.iter = iter;
        this.func = c.valueSerializeFunc(elementType);
        this.indent = indent;
        this.index = 0;
        this.written = false;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerContext c) {
        for(int i = index; i < size; i++) {
            Object instance = iter.next();
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
