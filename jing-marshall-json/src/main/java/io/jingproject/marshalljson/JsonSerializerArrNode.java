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
        int index = incIndex();
        if(index == 0) {
            JsonSerializeUtil.serializeArrayStart(writeBuffer);
        }
        if(index == arr.length) {
            JsonSerializeUtil.serializeArrayEnd(writeBuffer);
            return JsonSerializeResult.FINISHED;
        }
        Object instance = arr[index];
        if(instance == null) {
            JsonSerializeUtil.serializeNull(writeBuffer);
            return JsonSerializeResult.CONTINUE;
        }
        serializeSep(option, writeBuffer);
        if(func == null) {
            func = JsonSerializeUtil.valueSerializeFunc(option, arr.getClass().getComponentType());
        }
        return func.serialize(option, writeBuffer, instance, indent());
    }

    @Override
    protected void reset() {
        super.reset();
        arr = null;
        func = null;
    }
}
