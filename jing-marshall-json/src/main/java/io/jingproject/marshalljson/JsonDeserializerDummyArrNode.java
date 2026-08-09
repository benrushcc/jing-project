package io.jingproject.marshalljson;

public final class JsonDeserializerDummyArrNode extends JsonDeserializerNode {
    private int index;

    public void init() {
        this.index = 0;
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerContext c, JsonDeserializeResult last) {
        byte firstByte = c.nextFirstValuableByte();
        if (firstByte == (byte) ']') {
            return JsonDeserializeResult.Finish;
        } else if (firstByte != (byte) ',') {
            throw new JsonDeserializerException("illegal value end, got : " + firstByte);
        }
        // since the skip* functions directly invokes readByte() to perform reads, an IndexOutOfBoundsException can occur at any moment, necessitating explicit catch handling
        try {
            final int maxDummyArrayElements = c.option().maxDummyArrayElements();
            int i = index;
            while (i < maxDummyArrayElements) {
                firstByte = c.nextFirstValuableByte();
                if (c.skipAnyValue(firstByte)) {
                    i++;
                } else if (firstByte == (byte) '{') {
                    index = i + 1;
                    return JsonDeserializeResult.NewDummyObj;
                } else if (firstByte == (byte) '[') {
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
