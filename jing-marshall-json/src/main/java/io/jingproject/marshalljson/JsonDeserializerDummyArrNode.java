package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;

public final class JsonDeserializerDummyArrNode extends JsonDeserializerNode {
    private int index;

    public void init() {
        this.index = 0;
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, Object v) {
        byte firstByte;
        if(v != null) {
            assert v == JsonDeserializeUtil.dummyValue(); // dummy node could only receive dummy value
            firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
            if(firstByte == (byte) ']') {
                context.setObj(JsonDeserializeUtil.dummyValue());
                return JsonDeserializeResult.Finish;
            } else if(firstByte != (byte) ',') {
                throw new JsonDeserializerException("illegal value end, got : " + firstByte);
            }
        }
        try {
            int i = index;
            while (i < option.maxDummyArrayElements()) {
                firstByte = JsonDeserializeUtil.nextFirstValuableByte(option, readBuffer);
                if(JsonDeserializeUtil.skipAnyValue(option, readBuffer, firstByte)) {
                    i++;
                } else if(firstByte == (byte) '{') {
                    index = i + 1;
                    return JsonDeserializeResult.NewDummyObj;
                } else if(firstByte == (byte) '[') {
                    index = i + 1;
                    return JsonDeserializeResult.NewDummyArr;
                } else {
                    throw new JsonDeserializerException("illegal value start, got : " + firstByte);
                }
            }
            throw new JsonDeserializerException("too many elements in dummy array");
        } catch (IndexOutOfBoundsException e) {
            throw new JsonDeserializerException("eof reached while skipping elements in dummy array");
        }
    }
}
