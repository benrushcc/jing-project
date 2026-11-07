package io.jingproject.common;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class BatchQueue<T> {
    private final Object[] items;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;
    private int pIndex;
    private int cIndex;

    public BatchQueue(int size) {
        this.items = new Object[size];
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.notFull = lock.newCondition();
    }

    public void produce(T t) {
        lock.lock();
        try {
            while (pIndex == items.length) {
                notFull.awaitUninterruptibly();
            }
            items[pIndex++] = t;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public int consume(T[] data, int batch) {
        lock.lock();
        try {
            while (cIndex == pIndex) {
                notEmpty.awaitUninterruptibly();
            }
            int count = pIndex - cIndex;
            boolean full = pIndex == items.length;
            if(count <= batch) {
                System.arraycopy(items, cIndex, data, 0, count);
                cIndex = pIndex = 0;
                if(full) {
                    notFull.signalAll();
                }
                return count;
            }
            System.arraycopy(items, cIndex, data, 0, batch);
            if(full) {
                int remaining = count - batch;
                System.arraycopy(items, cIndex + batch, items, 0, remaining);
                cIndex = 0;
                pIndex = remaining;
                notFull.signalAll();
            } else {
                cIndex += batch;
            }
            return batch;
        }  finally {
            lock.unlock();
        }
    }
}
