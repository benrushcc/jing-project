package io.jingproject.common.log;

// represents log levels
// trace is used for the most detailed debugging information
// debug is used for debugging information
// info  is used for normal operational messages
// warn  is used for potentially harmful situations
// error is used for serious errors
// the internal value is used to better determine whether a given level is allowed
public enum LogLevel {
    TRACE(-7355608),

    DEBUG(-60937),

    INFO(0),

    WARN(60937),

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
        throw new IllegalArgumentException("unsupported logging level : " + s);
    }
}
