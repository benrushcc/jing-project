package io.jingproject.commontest.bench;

import io.jingproject.common.DualLock;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 800, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ExBenchmark {
    
    record IntHolder(int value) {

    }

    static final class MutableIntHolder {
        private int value;

        public int value() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
    
    @Benchmark
    @OperationsPerInvocation(10000)
    public void testEx(Blackhole blackhole) throws InterruptedException {
        DualLock<IntHolder> dualLock = new DualLock<>(new IntHolder(0));
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        Thread t1 = Thread.ofPlatform().unstarted(() -> {
            try {
                startLatch.await();
                for(int i = 0; i < 5; i++) {
                    IntHolder intHolder = dualLock.lock();
                    int value = intHolder.value();
                    blackhole.consume(value);
                    dualLock.unlock(new IntHolder(value + 1));
                }
                endLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2 = Thread.ofPlatform().unstarted(() -> {
            try {
                startLatch.await();
                for(int i = 0; i < 5; i++) {
                    IntHolder intHolder = dualLock.lock();
                    int value = intHolder.value();
                    blackhole.consume(value);
                    dualLock.unlock(new IntHolder(value + 1));
                }
                endLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();
        t2.start();
        startLatch.countDown();
        endLatch.await();
    }

    @Benchmark
    @OperationsPerInvocation(10000)
    public void testLock(Blackhole blackhole) throws InterruptedException {
        MutableIntHolder intHolder = new MutableIntHolder();
        intHolder.setValue(0);
        Lock lock = new ReentrantLock();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        Thread t1 = Thread.ofPlatform().unstarted(() -> {
            try {
                startLatch.await();
                for(int i = 0; i < 10000; i++) {
                    lock.lock();
                    try {
                        int value = intHolder.value();
                        blackhole.consume(value);
                        intHolder.setValue(value + 1);
                    } finally {
                        lock.unlock();
                    }
                }
                endLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2 = Thread.ofPlatform().unstarted(() -> {
            try {
                startLatch.await();
                for(int i = 0; i < 10000; i++) {
                    lock.lock();
                    try {
                        int value = intHolder.value();
                        blackhole.consume(value);
                        intHolder.setValue(value + 1);
                    } finally {
                        lock.unlock();
                    }
                }
                endLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();
        t2.start();
        startLatch.countDown();
        endLatch.await();
    }

    @Benchmark
    @OperationsPerInvocation(10000)
    public void testAtomic(Blackhole blackhole) throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        Thread t1 = Thread.ofPlatform().unstarted(() -> {
            try {
                startLatch.await();
                for(int i = 0; i < 10000; i++) {
                    blackhole.consume(atomicInteger.getAndIncrement());
                }
                endLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2 = Thread.ofPlatform().unstarted(() -> {
            try {
                startLatch.await();
                for(int i = 0; i < 10000; i++) {
                    blackhole.consume(atomicInteger.getAndIncrement());
                }
                endLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();
        t2.start();
        startLatch.countDown();
        endLatch.await();
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(ExBenchmark.class.getSimpleName()).addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
