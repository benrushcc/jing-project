package io.jingproject.marshalljson;

import java.util.Map;

public final class JsonDeserializerMapNode extends JsonDeserializerNode {
    private Map<Object, Object> map;
    private JsonDeserializeFunc func;
    private Object key;
    private int index;

    private static boolean finishDeserializing(Map<Object, Object> m, JsonDeserializerContext c) {
        byte firstByte = c.nextFirstValuableByte();
        if (firstByte == (byte) '}') {
            c.setObj(m);
            return true;
        } else if (firstByte == (byte) ',') {
            return false;
        } else {
            throw new JsonDeserializerException("illegal object value end, got : " + firstByte);
        }
    }

    public void init(Map<Object, Object> map, Class<?> valueType, JsonDeserializerContext c) {
        this.map = map;
        this.func = c.valueDeserializeFunc(valueType);
        this.key = null;
        this.index = 0;
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerContext c, JsonDeserializeResult last) {
        final Map<Object, Object> m = map;
        final JsonDeserializeFunc fn = func;
        if (last == JsonDeserializeResult.Finish) {
            m.put(key, c.obj());
        }
        if (last != JsonDeserializeResult.Start && finishDeserializing(m, c)) {
            return JsonDeserializeResult.Finish;
        }
        final int maxMapElements = c.option().maxMapElements();
        for (int i = index; i < maxMapElements; i++) {
            byte b = c.nextFirstValuableByte();
            if (b != (byte) '"') {
                throw new JsonDeserializerException("illegal key start, got : " + b);
            }
            String k = c.deserializeString(b);
            b = c.skipColon();
            JsonDeserializeResult r = fn.deserialize(b, c);
            if (r == JsonDeserializeResult.Continue) {
                m.put(k, c.obj());
                if (finishDeserializing(m, c)) {
                    return JsonDeserializeResult.Finish;
                }
                continue;
            }
            index = i + 1;
            key = k;
            return r;
        }
        throw new JsonDeserializerException("too many map elements");
    }
}
