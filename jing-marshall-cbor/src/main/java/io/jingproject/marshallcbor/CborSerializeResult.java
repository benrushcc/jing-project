package io.jingproject.marshallcbor;

// results of a serialization step that guide the subsequent processing flow.
// each constant indicates what action the caller should take next. some results
// carry additional data stored in specific fields of CborSerializerContext.
public enum CborSerializeResult {

    // the current field has been processed normally
    Continue,

    // all fields of the current object have been processed
    Finished,

    // the current field holds an object that requires further processing.
    // the new object instance should be placed in CborSerializerContext.obj()
    NewMarshallable,

    // the current field holds an array that requires further processing.
    // the new array should be placed in CborSerializerContext.obj()
    NewArray,

    // the current field holds a collection that requires further processing.
    // the new collection should be placed in CborSerializerContext.obj()
    // the element type should be placed in CborSerializerContext.type()
    NewCollection,

    // the current field holds a map that requires further processing.
    // the new map should be placed in CborSerializerContext.obj()
    // the value type should be placed in CborSerializerContext.type()
    NewMap
}