package io.jingproject.ffm;

/**
 * exception for FFM (Foreign Function & Memory) related errors
 */
public class ForeignException extends RuntimeException {
    public ForeignException(String message) {
        super(message);
    }

    public ForeignException(String message, Throwable cause) {
        super(message, cause);
    }
}
