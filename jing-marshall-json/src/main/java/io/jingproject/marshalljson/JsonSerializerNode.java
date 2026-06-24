package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

public sealed abstract class JsonSerializerNode
        permits JsonSerializerObjNode, JsonSerializerArrNode, JsonSerializerColNode, JsonSerializerMapNode {
    private int indent = 0;
    private int index = -1;
    private boolean written = false;

    protected abstract JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer);

    protected void reset() {
        indent = 0;
        index = -1;
        written = false;
    }

    public final void setIndent(int indent) {
        this.indent = indent;
    }

    public final int indent() {
        return indent;
    }

    protected final int incIndex() {
        return ++index;
    }

    protected final void serializeSep(JsonSerializerOption option, WriteBuffer writeBuffer) {
        if(written) {
            JsonSerializeUtil.serializeComma(writeBuffer);
        }
        written = true;
        JsonSerializeUtil.serializeIndent(indent, option.indentationLevel(), writeBuffer);
    }
}
