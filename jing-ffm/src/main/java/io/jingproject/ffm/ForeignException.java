package io.jingproject.ffm;

import java.io.Serial;

// exception for FFM (Foreign Function & Memory) related errors.
public final class ForeignException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ForeignException(String message) {
        super(message);
    }

    public ForeignException(String message, Throwable cause) {
        super(message, cause);
    }
}
