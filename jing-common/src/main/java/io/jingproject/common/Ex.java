package io.jingproject.common;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;


public final class Ex<T extends Record> {
    private static final VarHandle handle;

    record ExValue<T extends Record>(
            T value,
            Thread owner
    ) {

    }

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Ex.class, MethodHandles.lookup());
            handle = lookup.findVarHandle(Ex.class, "value", ExValue.class);
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private volatile ExValue<T> value;
    private final int spinCount;

    public Ex(T element) {
        this(element, 0);
    }

    public Ex(T element, int spinCount) {
        this.value = new ExValue<>(element, null);
        this.spinCount = Math.clamp(spinCount, 0, 1024);
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        for ( ; ; ) {
            ExValue<T> current = (ExValue<T>) handle.getVolatile(this);
            T currentValue = current.value();
            if(currentValue != null) {
                return currentValue;
            }
            Thread.onSpinWait();
        }
    }


    @SuppressWarnings("unchecked")
    public T lock() {
        Thread currentThread = Thread.currentThread();
        int spin = spinCount;
        ExValue<T> newEx = new ExValue<>(null, currentThread);
        for( ; ; ) {
            ExValue<T> current = (ExValue<T>) handle.getVolatile(this);
            T currentValue = current.value();
            Thread ownerThread = current.owner();
            if(currentValue == null) {
                if(currentThread == ownerThread) {
                    // Spurious wakeup
                    LockSupport.park(this);
                } else if(spin-- > 0) {
                    Thread.onSpinWait();
                } else {
                    if(handle.compareAndSet(this, current, newEx)) {
                        LockSupport.park(this);
                    }
                }
            } else {
                if(handle.compareAndSet(this, current, newEx)) {
                    return currentValue;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void unlock(T value) {
        Objects.requireNonNull(value, "value must not be null");
        Thread currentThread = Thread.currentThread();
        ExValue<T> newEx = new ExValue<>(value, null);
        for( ; ;) {
            ExValue<T> current = (ExValue<T>) handle.getVolatile(this);
            Thread ownerThread = current.owner();
            if(currentThread == ownerThread) {
                if(handle.compareAndSet(this, current, newEx)) {
                    return ;
                }
            } else {
                if(handle.compareAndSet(this, current, newEx)) {
                    LockSupport.unpark(ownerThread);
                    return ;
                }
            }
        }
    }
}
