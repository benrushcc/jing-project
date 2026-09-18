package io.jingproject.log;

import io.jingproject.common.*;
import io.jingproject.common.log.LogLevel;
import io.jingproject.common.log.Logger;
import io.jingproject.common.log.LoggerFacade;

import java.util.Map;

public final class LoggerFacadeImpl implements LoggerFacade, LifeCycle {
    public static final ScopedValue<Map<String, Object>> LOGGING_CONTEXT = ScopedValue.newInstance();

    private final LogLevel level;
    private final MpscLinkedQueue<LogEvent> queue;
    private final Thread thread;

    public LoggerFacadeImpl() {
        this.level = LogLevel.fromString(ConfigurationFactory.conf("jing.log.level", "INFO"));
        this.queue = new MpscLinkedQueue<>();
        this.thread = Thread.ofPlatform().unstarted(() -> {

        });
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public Logger getLogger(Class<?> clazz) {
        return new LoggerImpl(clazz, level, queue, thread);
    }
}
