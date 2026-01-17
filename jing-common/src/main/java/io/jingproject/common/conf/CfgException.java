package io.jingproject.common.conf;

public final class CfgException extends RuntimeException {
    public CfgException(String message) {
        super(message);
    }

    public CfgException(String message, Throwable cause) {
        super(message, cause);
    }
}
