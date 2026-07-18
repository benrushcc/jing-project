package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallWriter;

public final class JsonDeserializerObjNode extends JsonDeserializerNode {
    private MarshallFacade fc;
    private MarshallWriter writer;
    private int count;
    private int offset;
    private int bitmapSize;
    private int bitmapIndex;

    public void init(MarshallFacade fc, JsonDeserializerOption option, JsonDeserializerContext context) {
        this.fc = fc;
        this.writer = fc.newWriter();
        this.count = 0;
        this.offset = Integer.MIN_VALUE;
        if(option.ensureAllFieldsPresent()) {
            this.bitmapSize = JsonDeserializerContext.bitmapBytes(fc.totalElements());
            this.bitmapIndex = context.bitmapIndex(bitmapSize);
        } else {
            this.bitmapSize = Integer.MIN_VALUE;
            this.bitmapIndex = Integer.MIN_VALUE;
        }
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context, Object v) {
        // TODO

        return null;
    }
}
