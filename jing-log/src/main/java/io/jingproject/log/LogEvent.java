package io.jingproject.log;

import io.jingproject.common.LogLevel;

public record LogEvent(
        LogLevel level,
        long timestamp,
        String className,
        String threadName,
        Throwable throwable,
        String msg
) {

}
