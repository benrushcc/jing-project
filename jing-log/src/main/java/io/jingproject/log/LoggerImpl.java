package io.jingproject.log;

import io.jingproject.common.BatchQueue;
import io.jingproject.common.LogLevel;
import io.jingproject.common.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;
import java.util.Objects;

public final class LoggerImpl implements Logger {
    private final Class<?> clazz;
    private final MutableCallSite site;
    private final MethodHandle mh;
    private final BatchQueue<LogEvent> queue;

    public LoggerImpl(Class<?> clazz, LogLevel level, BatchQueue<LogEvent> queue) {
        this.clazz = clazz;
        this.site = new MutableCallSite(MethodHandles.constant(LogLevel.class, level));
        this.mh = site.dynamicInvoker();
        this.queue = queue;
    }

    @Override
    public void setLevel(LogLevel level) {
        site.setTarget(MethodHandles.constant(LogLevel.class, level));
        MutableCallSite.syncAll(new MutableCallSite[]{site});
    }

    @Override
    public boolean enabled(LogLevel level) {
        LogLevel current;
        try {
            current = (LogLevel) mh.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError("Unexpected error in MutableCallSite invocation", t);
        }
        return current.value() <= Objects.requireNonNull(level).value();
    }

    @Override
    public void log(LogLevel level, String msg, Throwable throwable) {
        String className = clazz.getCanonicalName();
        String threadName = Thread.currentThread().getName();
        // TODO 做具体实现
    }
}
