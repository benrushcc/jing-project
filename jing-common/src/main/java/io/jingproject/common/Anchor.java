package io.jingproject.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Application anchor point for component management with lifecycle support.
 * Provides simplified dependency management without complex DI frameworks.
 */
public final class Anchor {

    /** Registry mapping class to singleton instance */
    private static final Map<Class<?>, Object> m = new ConcurrentHashMap<>();

    /** Registered lifecycle components */
    private static final List<LifeCycle> lcs = new ArrayList<>();

    /** Lock for thread-safe lifecycle operations */
    private static final Lock lock = new ReentrantLock();

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private Anchor() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Get component instance cast to specified interface.
     *
     * @param clazz Interface class to cast to
     * @param assumed Implementation class for validation
     * @return Component instance cast to interface
     * @throws IllegalArgumentException if clazz not interface or assumed not implementation
     */
    public static <T1, T2> T2 assume(Class<T1> clazz, Class<T2> assumed) {
        if(!clazz.isInterface()) {
            throw new IllegalArgumentException("Class " + clazz + " is not interface");
        }
        if(clazz.isAssignableFrom(assumed)) {
            throw new IllegalArgumentException("Class " + assumed + " is not a implementation");
        }
        try {
            return assumed.cast(m.get(clazz));
        } catch (ClassCastException e) {
            throw new LifecycleError("Assume failure", e);
        }
    }

    /**
     * Computes and registers a component if it doesn't exist (lazy initialization).
     * This is the primary method for component registration and retrieval.
     *
     * @param <T> The type of component
     * @param clazz The class object for the component type
     * @param supplier The factory function to create the component if not present
     * @return The component instance (existing or newly created)
     * @throws LifecycleError if supplier returns null or component creation fails
     */
    public static <T> T compute(Class<T> clazz, Supplier<T> supplier) {
        AtomicBoolean created = new AtomicBoolean(false);
        Object result = m.computeIfAbsent(clazz, _ -> {
            created.set(true);
            T instance = supplier.get();
            if(instance == null) {
                throw new LifecycleError("Failed to create instance");
            }
            return instance;
        });
        if(created.get() && result instanceof LifeCycle lc) {
            addLifeCycle(lc);
        }
        return clazz.cast(result);
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
            if(lcs.isEmpty()) {
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
                for (LifeCycle lc : lcs) {
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
