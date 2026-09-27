package io.jingproject.marshalljson;

import java.io.Serial;

public final class JsonDeserializerException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public JsonDeserializerException(String message) {
        super(message);
    }
}
