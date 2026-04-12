package io.jingproject.commonprocess;

public final class AnnotationProcessorException extends RuntimeException {
    public AnnotationProcessorException(String message) {
        super(message);
    }

    public AnnotationProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
