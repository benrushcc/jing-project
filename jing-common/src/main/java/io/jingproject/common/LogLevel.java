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
        for (LogLevel v : LogLevel.values()) {
            if (v.name().equalsIgnoreCase(s)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unsupported logging level : " + s);
    }
}
