package io.jingproject.log;

import io.jingproject.common.log.LogLevel;

import java.time.OffsetDateTime;
import java.util.Map;

public record LogEvent (
        LogLevel level,
        OffsetDateTime offsetDateTime,
        String className,
        String threadName,
        String msg,
        Throwable throwable,
        Map<String, Object> logContext
) {

}
