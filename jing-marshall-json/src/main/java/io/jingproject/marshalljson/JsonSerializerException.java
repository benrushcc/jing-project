package io.jingproject.marshalljson;

import java.io.Serial;

public final class JsonSerializerException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public JsonSerializerException(String message) {
        super(message);
    }
}
