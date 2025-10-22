package io.jingproject.ffm;

public final class ForeignException extends RuntimeException {
    public ForeignException(String message) {
        super(message);
    }

    public ForeignException(String message, Throwable cause) {
        super(message, cause);
    }
}
