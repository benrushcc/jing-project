package io.jingproject.marshalljson;

public enum JsonSerializeResult {
    Continue,
    Finished,
    NewMarshallable,
    NewArray,
    NewCollection,
    NewMap,

//    JsonSerializeResult CONTINUE = new JsonSerializeContinue();
//
//    JsonSerializeResult FINISHED = new JsonSerializeFinished();
//
//    record JsonSerializeContinue() implements JsonSerializeResult {
//
//    }
//
//    record JsonSerializeFinished() implements JsonSerializeResult {
//
//    }
//
//    record JsonSerializeNewMarshallable(Object instance) implements JsonSerializeResult {
//
//    }
//
//    record JsonSerializeNewArray(Object[] arr) implements JsonSerializeResult {
//
//    }
//
//    record JsonSerializeNewCollection(Collection<?> col, Class<?> elementType) implements JsonSerializeResult {
//
//    }
//
//    record JsonSerializeNewMap(Map<?, ?> map, Class<?> keyType, Class<?> valueType) implements JsonSerializeResult {
//
//    }
}
