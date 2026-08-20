package io.jingproject.marshalljson;

public enum JsonDeserializeResult {
    Start,

    Trivial,

    Continue,

    Finish,

    NewMarshallable,

    NewArr,

    NewCol,

    NewMap,

    NewDummyObj,

    NewDummyCol;

    public boolean isNested() {
        return ordinal() > Finish.ordinal();
    }
}
