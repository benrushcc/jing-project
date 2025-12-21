package io.jingproject.common;

public final class LifecycleError extends Error {
    public LifecycleError(String message) {
        super(message);
    }

    public LifecycleError(Throwable cause) {
        super(cause);
    }

    public LifecycleError(String message, Throwable cause) {
        super(message, cause);
    }
}
