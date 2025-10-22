package io.jingproject.log;

import io.jingproject.common.LifeCycle;
import io.jingproject.common.Logger;
import io.jingproject.common.LoggerFacade;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

public final class LoggerFacadeImpl implements LoggerFacade, LifeCycle {
    private static final BlockingQueue<LogEvent> QUEUE = new LinkedTransferQueue<>();

    public static BlockingQueue<LogEvent> getQueue() {
        return QUEUE;
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public Logger getLogger(Class<?> clazz) {
        return new LoggerImpl(clazz);
    }
}
