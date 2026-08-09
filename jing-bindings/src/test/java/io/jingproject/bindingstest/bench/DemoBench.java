package io.jingproject.bindingstest.bench;

import io.jingproject.bindingstest.entity.DemoBinding;
import io.jingproject.bindingstest.entity.DemoBindingImpl;
import io.jingproject.ffm.Libs;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// 这个测试用来对比java原生实现和native调用实现简单函数之间的性能差异，从而衡量跨越native边界的具体开销有多大
// 在x64上，使用critical的单次调用耗时大概是2ns，没有启用critical的单次调用耗时大概是4ns

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
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
    private long[] l;
    private double[] d;
    private List<MemorySegment> lm;
    private List<MemorySegment> dm;

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(DemoBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setup() {
        a = new int[BATCH_SIZE];
        b = new int[BATCH_SIZE];
        arena = Arena.ofConfined();
        m1 = arena.allocate(ValueLayout.JAVA_INT, BATCH_SIZE);
        m2 = arena.allocate(ValueLayout.JAVA_INT, BATCH_SIZE);
        l = new long[BATCH_SIZE];
        d = new double[BATCH_SIZE];
        lm = new ArrayList<>(BATCH_SIZE);
        dm = new ArrayList<>(BATCH_SIZE);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BATCH_SIZE; i++) {
            int i1 = random.nextInt(0, Integer.MAX_VALUE);
            int i2 = random.nextInt(0, Integer.MAX_VALUE);
            a[i] = i1;
            b[i] = -i2;
            m1.set(ValueLayout.JAVA_INT, i * ValueLayout.JAVA_INT.byteSize(), i1);
            m2.set(ValueLayout.JAVA_INT, i * ValueLayout.JAVA_INT.byteSize(), i2);
            l[i] = random.nextLong();
            d[i] = random.nextDouble();
            lm.add(arena.allocateFrom(String.valueOf(l[i])));
            dm.add(arena.allocateFrom(String.valueOf(d[i])));
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        a = null;
        b = null;
        arena.close();
        m1 = null;
        m2 = null;
        l = null;
        d = null;
        lm = null;
        dm = null;
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
    public void testJavaStrToLong(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            MemorySegment segment = lm.get(i);
            blackhole.consume(JAVA_IMPL.strToLong(segment));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testJavaStrToDouble(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            MemorySegment segment = lm.get(i);
            blackhole.consume(JAVA_IMPL.strToDouble(segment));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testJavaLongToStr(Blackhole blackhole) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment s = a.allocate(ValueLayout.JAVA_BYTE, 64);
            for (int i = 0; i < BATCH_SIZE; i++) {
                blackhole.consume(JAVA_IMPL.longToStr(l[i], s, 64));
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testJavaDoubleToStr(Blackhole blackhole) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment s = a.allocate(ValueLayout.JAVA_BYTE, 64);
            for (int i = 0; i < BATCH_SIZE; i++) {
                blackhole.consume(JAVA_IMPL.doubleToStr(d[i], s, 64));
            }
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

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testNativeStrToLong(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            MemorySegment segment = lm.get(i);
            blackhole.consume(NATIVE_IMPL.strToLong(segment));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testNativeStrToDouble(Blackhole blackhole) {
        for (int i = 0; i < BATCH_SIZE; i++) {
            MemorySegment segment = lm.get(i);
            blackhole.consume(NATIVE_IMPL.strToDouble(segment));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testNativeLongToStr(Blackhole blackhole) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment s = a.allocate(ValueLayout.JAVA_BYTE, 64);
            for (int i = 0; i < BATCH_SIZE; i++) {
                blackhole.consume(NATIVE_IMPL.longToStr(l[i], s, 64));
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void testNativeDoubleToStr(Blackhole blackhole) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment s = a.allocate(ValueLayout.JAVA_BYTE, 64);
            for (int i = 0; i < BATCH_SIZE; i++) {
                blackhole.consume(NATIVE_IMPL.doubleToStr(d[i], s, 64));
            }
        }
    }
}
