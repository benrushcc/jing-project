package io.jingproject.log;

import io.jingproject.common.MpscLinkedQueue;
import io.jingproject.common.log.LogLevel;
import io.jingproject.common.log.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;
import java.time.Instant;
import java.util.Map;

// producer side of the log pipeline: builds LogEvent instances and hands them
// to the queue, never blocking the calling thread. the current level is held
// in a MutableCallSite so the enabled() fast path can be constant folded by
// the JIT and setLevel() can switch it dynamically.
public final class LoggerImpl implements Logger {
    private final String className;
    private final MutableCallSite site;
    private final MethodHandle mh;
    private final MpscLinkedQueue<LogEvent> queue;
    private final LoggerFacadeImpl facade;

    public LoggerImpl(Class<?> clazz, LogLevel level, MpscLinkedQueue<LogEvent> queue, LoggerFacadeImpl facade) {
        this.className = clazz.getCanonicalName();
        this.site = new MutableCallSite(MethodHandles.constant(LogLevel.class, level));
        this.mh = site.dynamicInvoker();
        this.queue = queue;
        this.facade = facade;
    }

    @Override
    public void setLevel(LogLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        site.setTarget(MethodHandles.constant(LogLevel.class, level));
        MutableCallSite.syncAll(new MutableCallSite[]{site});
    }

    @Override
    public boolean enabled(LogLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        LogLevel current;
        try {
            current = (LogLevel) mh.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError("unexpected error in MutableCallSite invocation", t);
        }
        return current.value() <= level.value();
    }

    @Override
    public void log(LogLevel level, String msg, Throwable throwable) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        if (!enabled(level)) {
            return;
        }
        Map<String, Object> ctx = LoggerFacadeImpl.LOGGING_CONTEXT.isBound()
                ? LoggerFacadeImpl.LOGGING_CONTEXT.get()
                : Map.of();
        LogEvent event = new LogEvent(
                level,
                Instant.now(),
                className,
                Thread.currentThread().getName(),
                msg,
                throwable,
                ctx);
        queue.offer(event);
        facade.unparkConsumer();
    }
}