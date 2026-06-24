package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

public final class JsonSerializerArrNode extends JsonSerializerNode {
    private Object[] arr;
    private JsonSerializeFunc func;

    public void setArr(Object[] arr) {
        this.arr = arr;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer) {
        final Object[] array = arr;
        final JsonSerializeFunc fn = fn(option);
        for( ; ; ) {
            int index = incIndex();
            if(index == 0) {
                JsonSerializeUtil.serializeArrayStart(writeBuffer);
            }
            if(index == array.length) {
                JsonSerializeUtil.serializeArrayEnd(writeBuffer);
                return JsonSerializeResult.FINISHED;
            }
            Object instance = array[index];
            if(instance == null) {
                JsonSerializeUtil.serializeNull(writeBuffer);
                continue ;
            }
            serializeSep(option, writeBuffer);
            JsonSerializeResult r = fn.serialize(option, writeBuffer, instance, indent());
            if(r == JsonSerializeResult.CONTINUE) {
                continue ;
            }
            return r;
        }
    }

    private JsonSerializeFunc fn(JsonSerializerOption option) {
        if(func == null) {
            func = JsonSerializeUtil.valueSerializeFunc(option, arr.getClass().getComponentType());
        }
        return func;
    }

    @Override
    protected void reset() {
        super.reset();
        arr = null;
        func = null;
    }
}
