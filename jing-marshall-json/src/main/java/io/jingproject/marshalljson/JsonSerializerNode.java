package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

public sealed abstract class JsonSerializerNode
        permits JsonSerializerObjNode, JsonSerializerArrNode, JsonSerializerColNode, JsonSerializerMapNode {
    private int indent = 0;
    private int index = 0;
    private boolean written = false;

    protected abstract JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer);

    protected void reset() {
        indent = 0;
        index = 0;
        written = false;
    }

    public final void setIndent(int indent) {
        assert indent > 0;
        this.indent = indent;
    }

    public final int indent() {
        return indent;
    }

    public final void setIndex(int index) {
        assert index > 0;
        this.index = index;
    }

    protected final int index() {
        return index;
    }

    protected final void serializeSep(JsonSerializerOption option, WriteBuffer writeBuffer) {
        if(written) {
            JsonSerializeUtil.serializeComma(writeBuffer);
        }
        written = true;
        JsonSerializeUtil.serializeIndent(indent, option.indentationLevel(), writeBuffer);
    }
}
