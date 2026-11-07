package io.jingproject.log;

import io.jingproject.bindings.alloc.Allocator;
import io.jingproject.bindings.alloc.ArenaBase;
import io.jingproject.common.ConfigurationFactory;
import io.jingproject.common.LifeCycle;
import io.jingproject.common.MpscLinkedQueue;
import io.jingproject.common.log.LogLevel;
import io.jingproject.common.log.Logger;
import io.jingproject.common.log.LoggerFacade;

import java.util.*;
import java.util.concurrent.locks.LockSupport;

// the logging facade implementation: owns the mpsc queue, the single consumer
// thread and the shared mmap arena. producers only offer events and unpark the
// consumer; all formatting and writing happens on the consumer thread inside a
// ScopedValue binding of the arena so handlers can allocate off-heap regions.
public final class LoggerFacadeImpl implements LoggerFacade, LifeCycle {
    // the mdc style context snapshot read by LoggerImpl.log() on the producer side
    public static final ScopedValue<Map<String, Object>> LOGGING_CONTEXT = ScopedValue.newInstance();

    private static final int DEFAULT_BATCH_MAX = 1024;
    private static final long DEFAULT_ARENA_SIZE = 16777216;
    private static final List<String> DEFAULT_HANDLERS = List.of("console");

    private final LogLevel level;
    private final int batchMax;
    private final long arenaSize;
    private final List<String> handlerIds;
    private final MpscLinkedQueue<LogEvent> queue = new MpscLinkedQueue<>();
    private final List<LogEventHandler> handlers = new ArrayList<>();

    private ArenaBase arena;
    private Thread thread;
    private volatile boolean running;

    public LoggerFacadeImpl() {
        String levelRaw = ConfigurationFactory.conf("jing.log.level", "INFO");
        try {
            this.level = LogLevel.fromString(levelRaw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("jing.log.level : unsupported value '" + levelRaw + "'");
        }
        this.batchMax = intConfig("jing.log.batch.max", DEFAULT_BATCH_MAX, 1, Integer.MAX_VALUE);
        this.arenaSize = intConfig("jing.log.arena.size", (int) DEFAULT_ARENA_SIZE, 4096, Integer.MAX_VALUE);
        this.handlerIds = handlerConfig();
    }

    // internal test seam: wires pre-built handlers and settings directly,
    // bypassing the configuration and ServiceLoader paths
    public LoggerFacadeImpl(List<LogEventHandler> handlers, int batchMax, long arenaSize) {
        if (handlers == null || handlers.isEmpty()) {
            throw new IllegalArgumentException("handlers must not be null or empty");
        }
        if (batchMax < 1) {
            throw new IllegalArgumentException("batchMax must be >= 1");
        }
        if (arenaSize < 4096) {
            throw new IllegalArgumentException("arenaSize must be >= 4096");
        }
        this.level = LogLevel.INFO;
        this.batchMax = batchMax;
        this.arenaSize = arenaSize;
        this.handlerIds = List.of();
        for (LogEventHandler handler : handlers) {
            if (handler == null) {
                throw new IllegalArgumentException("handlers must not contain null");
            }
            this.handlers.add(handler);
        }
    }

    @Override
    public synchronized void start() {
        if (running) {
            throw new IllegalStateException("jing-log facade already started");
        }
        if (handlerIds.isEmpty()) {
            // the 3-arg test seam constructor already wired the handlers
            for (LogEventHandler handler : handlers) {
                if (handler == null) {
                    throw new IllegalStateException("jing-log facade started with a disabled handler");
                }
            }
        } else {
            Map<String, LogEventHandler> byId = new HashMap<>();
            for (LogEventHandler handler : ServiceLoader.load(LogEventHandler.class)) {
                byId.put(handler.id(), handler);
            }
            List<LogEventHandler> active = new ArrayList<>();
            for (String id : handlerIds) {
                LogEventHandler handler = byId.get(id);
                if (handler == null) {
                    throw new IllegalArgumentException("jing.log.handlers : unknown handler id '" + id + "'");
                }
                if (!active.contains(handler)) {
                    active.add(handler);
                }
            }
            handlers.clear();
            handlers.addAll(active);
        }
        ArenaBase arena = ArenaBase.newInstance(arenaSize, 8, 1);
        this.arena = arena;
        running = true;
        Thread t = Thread.ofPlatform().daemon().name("jing-log-consumer").unstarted(
                () -> ScopedValue.where(Allocator.MMAP_SCOPE, arena).run(this::consume));
        thread = t;
        t.start();
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        Thread t = thread;
        if (t != null) {
            LockSupport.unpark(t);
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        thread = null;
        arena = null;
    }

    // wakes the consumer thread, safe to call from any producer thread
    void unparkConsumer() {
        Thread t = thread;
        if (t != null) {
            LockSupport.unpark(t);
        }
    }

    @Override
    public Logger getLogger(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        return new LoggerImpl(clazz, level, queue, this);
    }

    // the single consumer thread body. loops draining the queue into batches
    // of at most batchMax events, delivering each batch to every active
    // handler; parks only when the queue is empty. on shutdown it drains the
    // remaining events, flushes and closes every handler, then releases the
    // shared arena.
    private void consume() {
        List<LogEvent> batch = new ArrayList<>(Math.min(batchMax, 1024));
        while (running) {
            while (running && batch.size() < batchMax) {
                LogEvent e = queue.poll();
                if (e == null) {
                    break;
                }
                batch.add(e);
            }
            if (!batch.isEmpty()) {
                deliver(batch);
                batch.clear();
                continue;
            }
            LockSupport.park();
        }
        while (true) {
            LogEvent e = queue.poll();
            if (e == null) {
                break;
            }
            batch.add(e);
            if (batch.size() >= batchMax) {
                deliver(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            deliver(batch);
        }
        for (int i = 0; i < handlers.size(); i++) {
            LogEventHandler handler = handlers.get(i);
            if (handler == null) {
                continue;
            }
            try {
                handler.flush();
            } catch (Throwable t) {
                System.err.println("jing-log: handler flush failed, disabled, reason : " + t);
                closeHandler(handler);
                handlers.set(i, null);
            }
        }
        for (int i = 0; i < handlers.size(); i++) {
            LogEventHandler handler = handlers.get(i);
            if (handler instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Throwable t) {
                    System.err.println("jing-log: handler close failed, reason : " + t);
                }
            }
        }
        ArenaBase a = arena;
        arena = null;
        if (a != null) {
            a.close();
        }
    }

    private void deliver(List<LogEvent> batch) {
        for (int i = 0; i < handlers.size(); i++) {
            LogEventHandler handler = handlers.get(i);
            if (handler == null) {
                continue;
            }
            try {
                handler.handle(batch);
            } catch (Throwable t) {
                System.err.println("jing-log: handler failed, disabled, reason : " + t);
                closeHandler(handler);
                handlers.set(i, null);
            }
        }
    }

    // closes a handler that is being disabled by a delivery/flush failure so
    // its owned resources (file handles, native state) are released promptly;
    // double-close must be safe for every AutoCloseable handler
    private static void closeHandler(LogEventHandler handler) {
        if (handler instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Throwable t) {
                System.err.println("jing-log: handler close after failure failed, reason : " + t);
            }
        }
    }

    private static List<String> handlerConfig() {
        List<String> ids = ConfigurationFactory.confList("jing.log.handlers");
        if (ids == null || ids.isEmpty()) {
            return DEFAULT_HANDLERS;
        }
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("jing.log.handlers : blank handler id");
            }
        }
        return List.copyOf(ids);
    }

    private static int intConfig(String key, int defaultValue, int min, int max) {
        String raw = ConfigurationFactory.conf(key);
        int value;
        try {
            value = ConfigurationFactory.valueAsInt(raw, defaultValue);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(key + " : invalid integer value '" + raw + "'");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(key + " : value out of range [" + min + ", " + max + "], actual : "
                    + value);
        }
        return value;
    }
}