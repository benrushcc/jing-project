package io.jingproject.marshalljson;

public sealed interface JsonDeserializeResult {

    JsonDeserializeResult CONTINUE = new JsonDeserializeContinue();

    JsonDeserializeResult DUMMY_FINISH = new JsonDeserializeFinished(JsonDeserializeUtil.dummyValue());

    JsonDeserializeResult DUMMY_OBJ = new JsonDeserializeNewDummyObj();

    JsonDeserializeResult DUMMY_ARR = new JsonDeserializeNewDummyArr();

    record JsonDeserializeContinue() implements JsonDeserializeResult {

    }

    record JsonDeserializeFinished(Object result) implements JsonDeserializeResult {

    }

    record JsonDeserializeNewMarshallable(Class<?> marshallableType) implements JsonDeserializeResult {

    }

    record JsonDeserializeNewDummyObj() implements JsonDeserializeResult {

    }

    record JsonDeserializeNewDummyArr() implements JsonDeserializeResult {

    }
}
