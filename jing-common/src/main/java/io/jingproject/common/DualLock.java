package io.jingproject.common;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

/**
 * A specialized synchronization wrapper for Record objects that supports
 * mutual exclusion between exactly two threads.
 * <p>
 * IMPORTANT: This implementation is designed for exactly two threads.
 * Behavior is undefined if more than two threads attempt to access the resource.
 * <p>
 * The synchronization state is represented by the 'value' field:
 * - When it's a Record instance: The resource is unlocked/available
 * - When it's a Thread instance: Either:
 *   1. The thread that currently holds the lock (owner), OR
 *   2. A contending thread that is spinning or parked waiting for the lock
 * <p>
 * This design relies on the fact that T extends Record, ensuring T cannot be
 * a Thread type, thus allowing clean type-based state discrimination.
 *
 * @param <T> The type of Record to be wrapped
 */
public final class DualLock<T extends Record> {
    /**
     * Maximum spin count before parking a contending thread.
     * Excessive spinning can harm performance.
     */
    private static final int MAX_SPIN_COUNT = 1024;

    /**
     * VarHandle for atomic operations on the value field.
     */
    private static final VarHandle handle;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(DualLock.class, MethodHandles.lookup());
            handle = lookup.findVarHandle(DualLock.class, "value", Object.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * The synchronization state:
     * - Record instance: Resource is available (unlocked state)
     * - Thread instance: Either the lock owner or a contending thread
     * <p>
     * Because T extends Record, we can safely assume Thread and T are distinct types.
     */
    @SuppressWarnings("FieldMayBeFinal")
    private volatile Object value;

    /**
     * Number of spin iterations a contending thread will perform before parking.
     * A negative value means immediate parking (no spinning).
     */
    private final int spinCount;

    /**
     * Creates an Ex wrapper with the specified Record element.
     * Uses default spin behavior (-1 means immediate parking when lock is contended).
     *
     * @param element the Record to wrap
     */
    public DualLock(T element) {
        this(element, -1);
    }

    /**
     * Creates an Ex wrapper with the specified Record element and spin count.
     *
     * @param element the Record to wrap
     * @param spinCount number of spin attempts before parking (clamped to 0-{@link #MAX_SPIN_COUNT})
     *                  Use -1 for immediate parking (no spinning)
     */
    public DualLock(T element, int spinCount) {
        this.value = element;
        this.spinCount = Math.clamp(spinCount, 0, MAX_SPIN_COUNT);
    }

    /**
     * Non-blocking peek at the current value.
     * If the value field contains a Thread (indicating the resource is locked
     * or being contended), this method spins until a Record value appears.
     *
     * @return the current Record value (never null)
     */
    @SuppressWarnings("unchecked")
    public T peek() {
        for( ; ; ) {
            Object current = handle.getVolatile(this);
            if(current instanceof Thread) {
                Thread.onSpinWait();
            } else {
                return (T) current;
            }
        }
    }

    /**
     * Attempts to acquire exclusive access to the wrapped Record.
     * <p>
     * When the value field contains a Thread:
     * - If it's the current thread: Already holds the lock (spurious wakeup scenario)
     * - If it's another thread: The current thread becomes a contender
     *   - Spins for spinCount iterations (if configured)
     *   - Then parks to wait for the owner to unlock
     * <p>
     * IMPORTANT: This method assumes only two threads total. If a third thread
     * calls this method while two threads are already involved (owner + contender),
     * behavior is undefined.
     *
     * @return the Record value at the time of successful lock acquisition
     */
    @SuppressWarnings("unchecked")
    public T lock() {
        Thread currentThread = Thread.currentThread();
        int spin = spinCount;
        for( ; ; ) {
            Object current = handle.getVolatile(this);
            if(current instanceof Thread t) {
                if(currentThread == t) {
                    // Could be spurious wakeup
                    LockSupport.park(this);
                } else if(spin-- > 0) {
                    Thread.onSpinWait();
                } else {
                    if(handle.compareAndSet(this, current, currentThread)) {
                        LockSupport.park(this);
                    }
                }
            } else {
                if(handle.compareAndSet(this, current, currentThread)) {
                    return (T) current;
                }
            }
        }
    }

    /**
     * Releases exclusive access and updates the wrapped value.
     * <p>
     * Three possible scenarios:
     * 1. Current thread is the owner: Releases lock and sets new value
     * 2. Current thread is a contender: "Steals" the release by setting new value
     *    and unparking the parked thread (could be owner or another contender)
     * 3. Unexpected state: Neither Thread nor expected state (shouldn't happen)
     * <p>
     *
     * @param value the new Record value (must not be null)
     * @throws IllegalStateException if the value field doesn't contain a Thread
     * @throws NullPointerException if value is null
     */
    public void unlock(T value) {
        Objects.requireNonNull(value, "value must not be null");
        Thread currentThread = Thread.currentThread();
        for( ; ;) {
            Object current = handle.getVolatile(this);
            if(current instanceof Thread t) {
                if(currentThread == t) {
                    if(handle.compareAndSet(this, current, value)) {
                        return ;
                    }
                } else {
                    if(handle.compareAndSet(this, current, value)) {
                        LockSupport.unpark(t);
                        return ;
                    }
                }
            } else {
                throw new IllegalStateException("Corrupted state");
            }
        }
    }
}
