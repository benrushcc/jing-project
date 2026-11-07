package io.jingproject.net;

import java.io.Serial;

public final class NetException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public NetException(String message) {
        super(message);
    }

    public NetException(String message, Throwable cause) {
        super(message, cause);
    }
}
