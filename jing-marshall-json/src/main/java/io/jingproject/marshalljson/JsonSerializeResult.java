package io.jingproject.marshalljson;

import java.util.Collection;
import java.util.Map;

public sealed interface JsonSerializeResult {

    JsonSerializeResult CONTINUE = new JsonSerializeContinue();

    JsonSerializeResult FINISHED = new JsonSerializeFinished();

    record JsonSerializeContinue() implements JsonSerializeResult {

    }

    record JsonSerializeFinished() implements JsonSerializeResult {

    }

    record JsonSerializeNewMarshallable(Object instance) implements JsonSerializeResult {

    }

    record JsonSerializeNewArray(Object[] instance) implements JsonSerializeResult {

    }

    record JsonSerializeNewCollection(Collection<?> instance, Class<?> elementType) implements JsonSerializeResult {

    }

    record JsonSerializeNewMap(Map<?, ?> instance, Class<?> keyType, Class<?> valueType) implements JsonSerializeResult {

    }
}
