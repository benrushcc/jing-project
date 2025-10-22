package io.jingproject.log;

import io.jingproject.common.LogLevel;
import io.jingproject.common.Logger;

public final class LoggerImpl implements Logger {

    private final Class<?> clazz;

    public LoggerImpl(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setLevel(LogLevel level) {

    }

    @Override
    public boolean enabled(LogLevel level) {
        return false;
    }

    @Override
    public void log(LogLevel level, String msg, Throwable throwable) {

    }
}
