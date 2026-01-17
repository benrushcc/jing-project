package io.jingproject.log;

import io.jingproject.common.*;

public final class LoggerFacadeImpl implements LoggerFacade, LifeCycle {
    private final LogLevel level;
    private final BatchQueue<LogEvent> queue;
    private final Thread logThread;

    public LoggerFacadeImpl() {
        this.level = LogLevel.fromString(ConfigurationFactory.conf("jing.log.level", "INFO"));
        this.queue = new BatchQueue<>(ConfigurationFactory.confAsInt("jing.log.batchsize", BatchQueue.defaultBatchSize()));
        this.logThread = Thread.ofPlatform().unstarted(() -> {

        });
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public Logger getLogger(Class<?> clazz) {
        return new LoggerImpl(clazz, level, queue);
    }
}
