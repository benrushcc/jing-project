package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallWriter;

public final class JsonDeserializerObjNode extends JsonDeserializerNode {
    private MarshallFacade fc;
    private MarshallWriter writer;

    public void setFc(MarshallFacade fc) {
        this.fc = fc;
    }

    private MarshallWriter writer() {
        if(writer == null) {
            writer = fc.newWriter();
        }
        return writer;
    }

    @Override
    protected void reset() {
        super.reset();
        fc = null;
        writer = null;
    }

    @Override
    protected JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer, JsonDeserializerContext context) {
        final MarshallFacade fc = this.fc;
        byte b;
        // TODO 这一块状态的保存和记录需要再仔细的想一想
        if(init()) {
            b = JsonDeserializeUtil.nextFirstValuableByte(readBuffer, option);
            if(b != (byte) '{') {
                throw new JsonDeserializerException("object start not found, got : " + b);
            }
        }
        // parsing key
        b = JsonDeserializeUtil.nextFirstValuableByte(readBuffer, option);
        if(b != (byte) '"') {
            throw new JsonDeserializerException("key start not found, got : " + b);
        }
        JsonDeserializeUtil.parseString(readBuffer, option, context);
        MarshallInfo marshallInfo = context.asMarshallInfo(fc);
        if(marshallInfo == null) {
            if(option.ensureAllFieldsPresent() && option.ignoreUnknownFields()) {

            }
        }
        return null;
    }
}
