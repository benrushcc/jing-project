package io.jingproject.bindingstest.bench;

import io.jingproject.bindingstest.entity.DemoBinding;
import io.jingproject.bindingstest.entity.DemoBindingImpl;
import io.jingproject.ffm.Libs;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 4000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class DemoBench {
    private static final int BATCH_SIZE = 10000;
    private static final DemoBinding NATIVE_IMPL = Objects.requireNonNull(Libs.getImpl(DemoBinding.class), "Failed to load jing_demo library");
    private static final DemoBinding JAVA_IMPL = new DemoBindingImpl();
    private int[] a;
    private int[] b;
    private Arena arena;
    private MemorySegment m1;
    private MemorySegment m2;

    @Setup(Level.Iteration)
    public void setup() {
        a = new int[BATCH_SIZE];
        b = new int[BATCH_SIZE];
        arena = Arena.ofConfined();
        m1 = arena.allocate(ValueLayout.JAVA_INT, BATCH_SIZE);
        m2 = arena.allocate(ValueLayout.JAVA_INT, BATCH_SIZE);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BATCH_SIZE; i++) {
            int i1 = random.nextInt(0, Integer.MAX_VALUE);
            int i2 = random.nextInt(0, Integer.MAX_VALUE);
            a[i] = i1;
            b[i] = -i2;
            m1.set(ValueLayout.JAVA_INT, i * ValueLayout.JAVA_INT.byteSize(), i1);
            m2.set(ValueLayout.JAVA_INT, i * ValueLayout.JAVA_INT.byteSize(), i2);
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        a = null;
        b = null;
        arena.close();
        m1 = null;
        m2 = null;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testJavaSingleInt(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            blackhole.consume(JAVA_IMPL.singleInt());
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testJavaComputeAdd(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            int i1 = a[i];
            int i2 = b[i];
            blackhole.consume(JAVA_IMPL.computeAdd(i1, i2));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testJavaComputePointer(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            MemorySegment s1 = m1.asSlice(i * ValueLayout.JAVA_INT.byteSize(), ValueLayout.JAVA_INT.byteSize());
            MemorySegment s2 = m2.asSlice(i * ValueLayout.JAVA_INT.byteSize(), ValueLayout.JAVA_INT.byteSize());
            blackhole.consume(JAVA_IMPL.computePointer(s1, s2));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testNativeSingleInt(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            blackhole.consume(NATIVE_IMPL.singleInt());
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testNativeComputeAdd(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            int i1 = a[i];
            int i2 = b[i];
            blackhole.consume(NATIVE_IMPL.computeAdd(i1, i2));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testNativeComputePointer(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            MemorySegment s1 = m1.asSlice(i * ValueLayout.JAVA_INT.byteSize(), ValueLayout.JAVA_INT.byteSize());
            MemorySegment s2 = m2.asSlice(i * ValueLayout.JAVA_INT.byteSize(), ValueLayout.JAVA_INT.byteSize());
            blackhole.consume(NATIVE_IMPL.computePointer(s1, s2));
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(DemoBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
