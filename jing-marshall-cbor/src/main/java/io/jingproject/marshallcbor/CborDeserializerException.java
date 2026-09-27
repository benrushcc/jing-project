package io.jingproject.marshallcbor;

import java.io.Serial;

public final class CborDeserializerException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public CborDeserializerException(String message) {
        super(message);
    }
}