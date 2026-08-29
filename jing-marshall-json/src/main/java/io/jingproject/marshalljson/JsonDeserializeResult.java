package io.jingproject.marshalljson;

public enum JsonDeserializeResult {
    Continue,

    Finish,

    NewMarshallable,

    NewArr,

    NewCol,

    NewMap,

    NewDummyObj,

    NewDummyCol
}
