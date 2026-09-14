package io.jingproject.marshallcbor;

// results of a deserialization step that guide the subsequent processing flow.
// each constant indicates what action the caller should take next, mirroring
// the JsonDeserializeResult contract.
public enum CborDeserializeResult {

    // the current data item has been consumed normally
    Continue,

    // the current object or container has been fully consumed
    Finish,

    // the current data item holds a marshallable object
    NewMarshallable,

    // the current data item holds a fixed or indefinite-length array
    NewArr,

    // the current data item holds a collection
    NewCol,

    // the current data item holds a map
    NewMap,

    // the current data item holds an unknown object to be skipped
    NewDummyObj,

    // the current data item holds an unknown container to be skipped
    NewDummyCol
}