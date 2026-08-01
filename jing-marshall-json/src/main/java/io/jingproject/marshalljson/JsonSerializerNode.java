package io.jingproject.marshalljson;

public sealed abstract class JsonSerializerNode
        permits JsonSerializerObjNode, JsonSerializerArrNode, JsonSerializerColNode,
        JsonSerializerListNode, JsonSerializerMapNode {
    protected int indent = 0;
    protected int index = 0;
    protected boolean written = false;

    protected abstract JsonSerializeResult process(JsonSerializerContext c);

    protected final void serializeSep(JsonSerializerContext c) {
        if(written) {
            c.writeBuffer().writeByte((byte) ',');;
        }
        written = true;
        c.serializeIndent(indent);
    }
}
