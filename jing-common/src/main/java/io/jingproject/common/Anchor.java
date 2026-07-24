package io.jingproject.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Application anchor point for component management with lifecycle support.
 * Provides simplified dependency management without complex DI frameworks.
 */
public final class Anchor {

    /**
     * Registry mapping class to singleton instance
     */
    private static final Map<Class<?>, Object> m = new HashMap<>();

    /**
     * Registered lifecycle components
     */
    private static final List<LifeCycle> lcs = new ArrayList<>();

    /**
     * Lock for thread-safe lifecycle operations
     */
    private static final Lock lock = new ReentrantLock();

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private Anchor() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Computes and registers a component if it doesn't exist (lazy initialization).
     * This is the primary method for component registration and retrieval.
     *
     * @param <T>      The type of component
     * @param clazz    The class object for the component type
     * @param supplier The factory function to create the component if not present
     * @return The component instance (existing or newly created)
     * @throws LifecycleError if supplier returns null or component creation fails
     */
    public static <T> T compute(Class<T> clazz, Supplier<T> supplier) {
        lock.lock();
        try {
            Object current = m.get(clazz);
            if (current == null) {
                T result = supplier.get();
                m.put(clazz, result);
                if(result instanceof LifeCycle lc) {
                    if(lcs.isEmpty()) {
                        addHook();
                    }
                    lcs.add(lc);
                }
                return clazz.cast(result);
            }
            return clazz.cast(current);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Starts all registered lifecycle components in registration order.
     * This should be called only once after all components are registered.
     *
     * @throws LifecycleError if any component fails during startup
     */
    public static void startLifeCycle() {
        lock.lock();
        try {
            for (LifeCycle lc : lcs) {
                try {
                    lc.start();
                } catch (Throwable e) {
                    throw new LifecycleError(e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Register a lifecycle component for automatic management.
     * Components are stopped in reverse order during shutdown.
     *
     * @param lc Lifecycle component to register
     * @throws LifecycleError if called after shutdown started
     */
    public static void addLifeCycle(LifeCycle lc) {
        lock.lock();
        try {
            if (lcs.isEmpty()) {
                addHook();
            }
            lcs.add(lc);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add JVM shutdown hook for graceful cleanup.
     * Hook stops components in reverse registration order.
     */
    private static void addHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            lock.lock();
            try {
                for (LifeCycle lc : lcs.reversed()) {
                    try {
                        lc.stop();
                    } catch (Throwable e) {
                        throw new LifecycleError(e);
                    }
                }
            } finally {
                lock.unlock();
            }
        }));
    }
}
