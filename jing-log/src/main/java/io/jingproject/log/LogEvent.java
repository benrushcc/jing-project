package io.jingproject.log;

import io.jingproject.common.log.LogLevel;

import java.time.Instant;
import java.util.Map;

public record LogEvent(
        LogLevel level,
        Instant instant,
        String className,
        String threadName,
        String msg,
        Throwable throwable,
        Map<String, Object> logContext
) {

}