package io.jingproject.marshalljson;

/**
 * results of a serialization step that guide the subsequent processing flow.
 * <p>
 * each constant indicates what action the caller should take next. some results
 * carry additional data stored in specific fields of {@link JsonSerializerContext}.
 * </p>
 */
public enum JsonSerializeResult {

    /**
     * the current field has been processed normally
     */
    Continue,

    /**
     * all fields of the current object have been processed
     */
    Finished,

    /**
     * the current field holds an object that requires further processing.
     * the new object instance should be placed in {@link JsonSerializerContext#obj()}
     */
    NewMarshallable,

    /**
     * the current field holds an array that requires further processing.
     * the new array should be placed in {@link JsonSerializerContext#obj()}
     */
    NewArray,

    /**
     * the current field holds a collection that requires further processing.
     * the new collection should be placed in {@link JsonSerializerContext#obj()}
     * the element type should be placed in {@link JsonSerializerContext#type()}
     */
    NewCollection,

    /**
     * the current field holds a map that requires further processing.
     * the new map should be placed in {@link JsonSerializerContext#obj()}
     * the value type should be placed in {@link JsonSerializerContext#type()}
     */
    NewMap
}
