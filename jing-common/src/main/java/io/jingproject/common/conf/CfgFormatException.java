package io.jingproject.common.conf;

public final class CfgFormatException extends RuntimeException {
    public CfgFormatException(String message) {
        super(message);
    }

    public CfgFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
