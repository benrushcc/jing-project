package io.jingproject.log;

import java.util.List;

// log event handler interface, consumed on the single consumer thread.
// each handler is free to decide its own formatting and write strategy.
public interface LogEventHandler {
    // handle a batch of events, best effort processed in one go
    void handle(List<LogEvent> events);

    // the configured handler id, matched against jing.log.handlers
    String id();

    // flush any buffered output, invoked by the consumer on shutdown
    default void flush() {
    }
}