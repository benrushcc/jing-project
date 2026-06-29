package io.jingproject.marshalljson;

import io.jingproject.common.ReadBuffer;
import io.jingproject.marshall.MarshallFacade;
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
    protected JsonDeserializeResult process(JsonDeserializerOption option, ReadBuffer readBuffer) {
        final MarshallFacade fc = this.fc;

        return null;
    }
}
