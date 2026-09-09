package io.jingproject.bench.common;

import io.jingproject.bench.AbstractBench;
import io.jingproject.common.DualLock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DualLockBenchmark extends AbstractBench {
    private static final int BATCH = 10000;

    private static void test(Runnable op) {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        Runnable wrapped = () -> {
            try {
                startLatch.await();
                for (int i = 0; i < BATCH; i++) {
                    op.run();
                }
                endLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        Thread t1 = Thread.ofPlatform().unstarted(wrapped);
        Thread t2 = Thread.ofPlatform().unstarted(wrapped);
        t1.start();
        t2.start();
        startLatch.countDown();
        try {
            endLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testDualLock(Blackhole blackhole) throws InterruptedException {
        DualLock<IntHolder> dualLock = new DualLock<>(new IntHolder(0));
        test(() -> {
            IntHolder intHolder = dualLock.lock();
            int value = intHolder.value();
            blackhole.consume(value);
            dualLock.unlock(new IntHolder(value + 1));
        });
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testLock(Blackhole blackhole) throws InterruptedException {
        MutableIntHolder intHolder = new MutableIntHolder();
        test(() -> {
            intHolder.lock();
            int value = intHolder.value();
            blackhole.consume(value);
            intHolder.setValue(value + 1);
            intHolder.unlock();
        });
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testAtomic(Blackhole blackhole) throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        test(() -> blackhole.consume(atomicInteger.getAndIncrement()));
    }

    record IntHolder(int value) {

    }

    static final class MutableIntHolder {
        private final Lock lock = new ReentrantLock();
        private int value = 0;

        public void lock() {
            lock.lock();
        }

        public void unlock() {
            lock.unlock();
        }

        public int value() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
