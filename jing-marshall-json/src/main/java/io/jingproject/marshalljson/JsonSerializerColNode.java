package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

import java.util.Iterator;

public final class JsonSerializerColNode extends JsonSerializerNode {
    private int size;
    private Iterator<?> iter;
    private Class<?> elementType;
    private JsonSerializeFunc func;

    public void setSize(int size) {
        this.size = size;
    }

    public void setIter(Iterator<?> iter) {
        this.iter = iter;
    }

    public void setElementType(Class<?> elementType) {
        this.elementType = elementType;
    }

    private JsonSerializeFunc func(JsonSerializerOption option) {
        if(func == null) {
            func = JsonSerializeUtil.valueSerializeFunc(option, elementType);
        }
        return func;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer) {
        final int size = this.size;
        final Iterator<?> iter = this.iter;
        final JsonSerializeFunc func = func(option);
        int index = index();
        if(index == 0) {
            JsonSerializeUtil.serializeArrayStart(writeBuffer);
        }
        for( ; index < size; index++) {
            Object instance = iter.next();
            if(instance == null) {
                JsonSerializeUtil.serializeNull(writeBuffer);
                continue ;
            }
            serializeSep(option, writeBuffer);
            JsonSerializeResult r = func.serialize(option, writeBuffer, instance, indent());
            if(r == JsonSerializeResult.CONTINUE) {
                continue ;
            }
            setIndex(index + 1);
            return r;
        }
        JsonSerializeUtil.serializeArrayEnd(writeBuffer);
        return JsonSerializeResult.FINISHED;
    }

    @Override
    protected void reset() {
        super.reset();
        this.size = Integer.MIN_VALUE;
        this.iter = null;
        this.elementType = null;
        this.func = null;
    }
}
