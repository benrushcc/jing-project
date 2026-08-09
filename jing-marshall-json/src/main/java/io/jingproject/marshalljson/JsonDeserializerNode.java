package io.jingproject.marshalljson;

public sealed abstract class JsonDeserializerNode
        permits JsonDeserializerObjNode, JsonDeserializerListNode, JsonDeserializerMapNode, JsonDeserializerDummyObjNode, JsonDeserializerDummyArrNode {

    protected abstract JsonDeserializeResult process(JsonDeserializerContext c, JsonDeserializeResult last);

}
