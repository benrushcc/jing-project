package io.jingproject.ffm;

/**
 * Exception for FFM (Foreign Function & Memory) related errors
 */
public final class ForeignException extends RuntimeException {
    public ForeignException(String message) {
        super(message);
    }

    public ForeignException(String message, Throwable cause) {
        super(message, cause);
    }
}
