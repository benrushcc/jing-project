package io.jingproject.marshalljson;

import java.util.List;

public final class JsonDeserializerListNode extends JsonDeserializerNode {
    private List<Object> list;
    private JsonDeserializeFunc func;
    private boolean asArray;
    private int index;

    private static boolean finishDeserializing(List<Object> l, boolean asArray, JsonDeserializerContext c) {
        byte firstByte = c.nextFirstValuableByte();
        if (firstByte == (byte) ']') {
            c.setObj(asArray ? l.toArray() : l);
            return true;
        } else if (firstByte == (byte) ',') {
            return false;
        } else {
            throw new JsonDeserializerException("illegal array value end, got : " + firstByte);
        }
    }

    public void init(List<Object> list, Class<?> valueType, JsonDeserializerContext c) {
        this.list = list;
        this.func = c.valueDeserializeFunc(valueType);
        this.asArray = c.asArray();
        this.index = 0;
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerContext c, JsonDeserializeResult last) {
        final List<Object> l = list;
        final JsonDeserializeFunc fn = func;
        final boolean asArr = asArray;
        if (last == JsonDeserializeResult.Finish) {
            list.add(c.obj());
        }
        if (last != JsonDeserializeResult.Finish && finishDeserializing(l, asArr, c)) {
            return JsonDeserializeResult.Finish;
        }
        final int maxArrayElements = c.option().maxArrayElements();
        for (int i = index; i < maxArrayElements; i++) {
            byte firstByte = c.nextFirstValuableByte();
            JsonDeserializeResult r = fn.deserialize(firstByte, c);
            if (r == JsonDeserializeResult.Continue) {
                l.add(c.obj());
                if (finishDeserializing(l, asArr, c)) {
                    return JsonDeserializeResult.Finish;
                }
                continue;
            }
            index = i + 1;
            return r;
        }
        throw new JsonDeserializerException("too many map elements");
    }
}
