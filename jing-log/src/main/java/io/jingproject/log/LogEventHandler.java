package io.jingproject.log;

@FunctionalInterface
public interface LogEventHandler {
    void handle(LogEvent logEvent);
}
