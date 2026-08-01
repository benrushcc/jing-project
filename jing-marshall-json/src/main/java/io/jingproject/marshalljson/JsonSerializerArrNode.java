package io.jingproject.marshalljson;

public final class JsonSerializerArrNode extends JsonSerializerNode {
    private Object[] arr;
    private JsonSerializeFunc func;

    public void init(Object[] arr, int indent, JsonSerializerContext c) {
        this.arr = arr;
        this.func = c.valueSerializeFunc(arr.getClass().getComponentType());
        this.indent = indent;
        this.index = 0;
        this.written = false;
    }

    @Override
    protected JsonSerializeResult process(JsonSerializerContext c) {
        for(int i = index; i < arr.length; i++) {
            Object instance = arr[i];
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
