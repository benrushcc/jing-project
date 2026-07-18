package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

public sealed abstract class JsonSerializerNode
        permits JsonSerializerObjNode, JsonSerializerArrNode, JsonSerializerColNode,
        JsonSerializerListNode, JsonSerializerMapNode {
    protected int indent = 0;
    protected int index = 0;
    protected boolean written = false;

    protected abstract JsonSerializeResult process(JsonSerializerOption option, WriteBuffer writeBuffer, JsonSerializerContext context);

    protected final void serializeSep(JsonSerializerOption option, WriteBuffer writeBuffer) {
        if(written) {
            JsonSerializeUtil.serializeComma(writeBuffer);
        }
        written = true;
        JsonSerializeUtil.serializeIndent(indent, option.indentationLevel(), writeBuffer);
    }
}
