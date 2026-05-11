package io.jingproject.marshall.json;

import io.jingproject.common.WriteBuffer;

public final class JsonMarshaller {

    private static final JsonMarshaller INSTANCE = new JsonMarshaller();

    public JsonMarshaller() {

    }

    public JsonMarshaller defaultMarshaller() {
        return INSTANCE;
    }

    public void writeObject(Object target, WriteBuffer writeBuffer) {
        // TODO
    }

    public void writeObjectPretty(Object target, WriteBuffer writeBuffer, int indentionStep) {

    }

}
