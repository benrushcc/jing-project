package io.jingproject.common.conf;

import java.io.Serial;

public final class CfgException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public CfgException(String message) {
        super(message);
    }

    public CfgException(String message, Throwable cause) {
        super(message, cause);
    }
}
