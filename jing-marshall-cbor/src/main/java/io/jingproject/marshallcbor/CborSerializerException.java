package io.jingproject.marshallcbor;

import java.io.Serial;

public final class CborSerializerException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public CborSerializerException(String message) {
        super(message);
    }
}