package io.jingproject.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public final class Anchor {

    private static final Map<Class<?>, Object> m = new ConcurrentHashMap<>();

    private static final List<LifeCycle> lcs = new ArrayList<>();

    private static final Lock lock = new ReentrantLock();

    private static boolean shutdown = false;

    private Anchor() {
        throw new UnsupportedOperationException("utility class");
    }

    public static <T1, T2> T2 assume(Class<T1> clazz, Class<T2> clazz2) {
        if(!clazz.isInterface()) {
            throw new IllegalArgumentException("Class " + clazz + " is not interface");
        }
        if(clazz.isAssignableFrom(clazz2)) {
            throw new IllegalArgumentException("Class " + clazz2 + " is not a implementation");
        }
        try {
            return clazz2.cast(m.get(clazz));
        } catch (ClassCastException e) {
            throw new RuntimeException("Assume failure", e);
        }
    }

    public static <T> T compute(Class<T> clazz, Supplier<T> supplier) {
        AtomicBoolean created = new AtomicBoolean(false);
        Object result = m.computeIfAbsent(clazz, _ -> {
            created.set(true);
            T instance = supplier.get();
            if(instance == null) {
                throw new RuntimeException("Failed to create instance");
            }
            return instance;
        });
        if(created.get() && result instanceof LifeCycle lc) {
            addLifeCycle(lc);
        }
        return clazz.cast(result);
    }

    public static void startLifeCycle() {
        lock.lock();
        try {
            for (LifeCycle lc : lcs) {
                try {
                    lc.start();
                } catch (Exception e) {
                    throw new LifecycleError(e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static void addLifeCycle(LifeCycle lc) {
        lock.lock();
        try {
            if(shutdown) {
                throw new RuntimeException("Already shutdown");
            }
            if(lcs.isEmpty()) {
                addHook();
            }
            lcs.add(lc);
        } finally {
            lock.unlock();
        }
    }

    private static void addHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            lock.lock();
            try {
                if(shutdown) {
                    throw new RuntimeException("Already shutdown");
                }
                shutdown = true;
                for (LifeCycle lc : lcs) {
                    try {
                        lc.stop();
                    } catch (Exception e) {
                        throw new LifecycleError(e);
                    }
                }
            } finally {
                lock.unlock();
            }
        }));
    }
}
