package io.jingproject.marshalljson;

public enum JsonDeserializeResult {

    Continue,

    Finish,

    NewMarshallable,

    NewDummyObj,

    NewDummyArr,

    DummyFinish,
}
