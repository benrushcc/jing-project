package io.jingproject.common;

public enum LogLevel {
    DEBUG(-7355608),

    INFO(0),

    ERROR(7355608);

    private final int value;

    LogLevel(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static LogLevel fromString(String s) {
        return switch (s) {
            case "DEBUG" -> DEBUG;
            case "INFO" -> INFO;
            case "ERROR" -> ERROR;
            default -> throw new IllegalArgumentException("Unsupported loglevel : " + s);
        };
    }
}
