package io.jingproject.common;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 *   BatchQueue acts like a MPSC queue, it uses lock internally, but optimized for batch consuming
 *   the consumer could just fetch all the elements at once without busy polling mechanism
 */
public final class BatchQueue<T> {
    private static final int DEFAULT_BATCH_SIZE = Integer.getInteger("jing.common.defaultbatchsize", 256);
    private final Lock lock = new ReentrantLock();
    private final int batchSize;
    private Batch<T> head;
    private Batch<T> tail;

    public BatchQueue(int batchSize) {
        this.batchSize = batchSize;
        this.head = this.tail = new Batch<>(batchSize);
    }

    public BatchQueue() {
        this(DEFAULT_BATCH_SIZE);
    }

    private static final class Batch<T> {
        private final Object[] elements;
        private int index = 0;
        private Batch<T> next = null;

        Batch(int batchSize) {
            this.elements = new Object[batchSize];
        }
    }

    public static int defaultBatchSize() {
        return DEFAULT_BATCH_SIZE;
    }

    public void offer(T element) {
        lock.lock();
        try {
            Batch<T> t = tail;
            int index = t.index;
            if(index == batchSize) {
                Batch<T> next = new Batch<>(batchSize);
                t.next = next;
                t = next;
                tail = t;
            }
            t.elements[index] = element;
            t.index = Math.addExact(index, 1);
        } finally {
            lock.unlock();
        }
    }

    public Stream<T> stream() {
        Batch<T> first;
        lock.lock();
        try {
            if(head == tail && head.index == 0) {
                return Stream.empty();
            }
            first = head;
            Batch<T> b = new Batch<>(batchSize);
            head = b;
            tail = b;
        } finally {
            lock.unlock();
        }
        return Stream.iterate(first, Objects::nonNull, batch -> batch.next)
                .mapMulti((batch, consumer) -> {
                    for (int i = 0; i < batch.index; i++) {
                        @SuppressWarnings("unchecked")
                        T element = (T) batch.elements[i];
                        consumer.accept(element);
                    }
                });
    }
}
