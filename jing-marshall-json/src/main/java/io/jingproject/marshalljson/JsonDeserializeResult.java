package io.jingproject.marshalljson;

public enum JsonDeserializeResult {
    Start,

    Trivial,

    Continue,

    Finish,

    NewMarshallable,

    NewList,

    NewMap,

    NewDummyObj,

    NewDummyArr;

    public boolean isNested() {
        return ordinal() > Finish.ordinal();
    }
}
