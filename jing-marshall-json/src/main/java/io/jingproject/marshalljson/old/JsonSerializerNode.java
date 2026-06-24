package io.jingproject.marshalljson.old;

import io.jingproject.common.WriteBuffer;

public sealed abstract class JsonSerializerNode permits JsonSerializerObjNode, JsonSerializerMapNode, JsonSerializerArrayNode {
    protected final JsonSerializerOption option;
    protected final WriteBuffer writeBuffer;
    protected final int indent;
    private int index = -1;
    private boolean written = false;

    protected JsonSerializerNode(JsonSerializerOption option, WriteBuffer writeBuffer, int indent) {
        assert option != null && writeBuffer != null && indent >= 1 && indent <= JsonSerializerOption.HARD_MAX_SIZE;
        this.option = option;
        this.writeBuffer = writeBuffer;
        this.indent = indent;
    }

    public final JsonSerializerNodeResult step() {
        int cap = capacity();
        int idx = ++index; // This value is bounded by the element count of arrays, collections, maps, and objects; thus overflow cannot occur.
        if(idx == 0) {
            init();
        }
        if(idx == cap) {
            end();
            return JsonSerializeUtil.serializerFinished();
        }
        JsonSerializerNode n = process(idx);
        return n == null ? JsonSerializeUtil.serializerContinue() : new JsonSerializerNodeResult.JsonSerializerNodeTransfer(n);
    }

    protected abstract int capacity();

    protected abstract void init();

    protected abstract JsonSerializerNode process(int index);

    protected abstract void end();

    protected final void serializeSep() {
        if(written) {
            JsonSerializeUtil.serializeComma(writeBuffer);
        }
        written = true;
        JsonSerializeUtil.serializeIndent(indent, option.indentationLevel(), writeBuffer);
    }
}
