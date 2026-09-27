package io.jingproject.commonprocess;

import java.io.Serial;

public final class AnnotationProcessorException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public AnnotationProcessorException(String message) {
        super(message);
    }

    public AnnotationProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
