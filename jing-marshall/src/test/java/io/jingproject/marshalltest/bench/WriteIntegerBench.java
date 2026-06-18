package io.jingproject.marshalltest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.marshall.MarshallOldUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.foreign.Arena;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// 这个测试用来对比手动实现向WriteBuffer中写入整数和使用jdk内置方法通过String转化写入整数之间的性能差异
// 这个对比并不是完全公平的，jdk有构造String的中间产物的相关开销，因此结果只能作为简单参考

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class WriteIntegerBench {
    private static final int BUFFER_SIZE = 32;
    private static final int SMALL_SIZE = 8000;
    private static final int MEDIUM_SIZE = 1000;
    private static final int HUGE_SIZE = 1000;
    private static final int BATCH_SIZE = SMALL_SIZE + MEDIUM_SIZE + HUGE_SIZE;
    private int[] intNums;
    private long[] longNums;
    private Arena arena;
    private HeapWriteBuffer heapWriteBuffer;
    private SegmentWriteBuffer segmentWriteBuffer;

    @Setup(Level.Iteration)
    public void setup() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        intNums = new int[BATCH_SIZE];
        longNums = new long[BATCH_SIZE];
        arena = Arena.ofConfined();
        heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
        segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
        for (int i = 0; i < SMALL_SIZE; i++) {
            intNums[i] = random.nextInt(-1000, 1000);
            longNums[i] = random.nextLong(-1000L, 1000L);
        }
        for (int i = 0; i < MEDIUM_SIZE; i++) {
            intNums[i] = random.nextInt(-100000, 100000);
            longNums[i] = random.nextLong(-100000L, 100000L);
        }
        for (int i = 0; i < HUGE_SIZE; i++) {
            intNums[i] = random.nextInt();
            longNums[i] = random.nextLong();
        }
        for (int i = intNums.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = intNums[i];
            intNums[i] = intNums[j];
            intNums[j] = temp;
        }
        for (int i = longNums.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            long temp = longNums[i];
            longNums[i] = longNums[j];
            longNums[j] = temp;
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        intNums = null;
        longNums = null;
        arena.close();
        heapWriteBuffer = null;
        segmentWriteBuffer = null;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteHeapInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeInt0(intNums[index], heapWriteBuffer);
            heapWriteBuffer.reset();
            blackhole.consume(heapWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteHeapLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeLong0(longNums[index], heapWriteBuffer);
            heapWriteBuffer.reset();
            blackhole.consume(heapWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteSegmentInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeInt0(intNums[index], segmentWriteBuffer);
            segmentWriteBuffer.reset();
            blackhole.consume(segmentWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteSegmentLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeLong0(longNums[index], segmentWriteBuffer);
            segmentWriteBuffer.reset();
            blackhole.consume(segmentWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void singleWriteHeapInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeInt1(intNums[index], heapWriteBuffer);
            heapWriteBuffer.reset();
            blackhole.consume(heapWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void singleWriteHeapLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeLong1(longNums[index], heapWriteBuffer);
            heapWriteBuffer.reset();
            blackhole.consume(heapWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void singleWriteSegmentInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeInt1(intNums[index], segmentWriteBuffer);
            segmentWriteBuffer.reset();
            blackhole.consume(segmentWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void singleWriteSegmentLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeLong1(longNums[index], segmentWriteBuffer);
            segmentWriteBuffer.reset();
            blackhole.consume(segmentWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void lutWriteHeapInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeInt2(intNums[index], heapWriteBuffer);
            heapWriteBuffer.reset();
            blackhole.consume(heapWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void lutWriteHeapLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeLong2(longNums[index], heapWriteBuffer);
            heapWriteBuffer.reset();
            blackhole.consume(heapWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void lutWriteSegmentInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeInt2(intNums[index], segmentWriteBuffer);
            segmentWriteBuffer.reset();
            blackhole.consume(segmentWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void lutWriteSegmentLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeLong2(longNums[index], segmentWriteBuffer);
            segmentWriteBuffer.reset();
            blackhole.consume(segmentWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void thWriteHeapInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeInt(intNums[index], heapWriteBuffer);
            heapWriteBuffer.reset();
            blackhole.consume(heapWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void thWriteHeapLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeLong(longNums[index], heapWriteBuffer);
            heapWriteBuffer.reset();
            blackhole.consume(heapWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void thWriteSegmentInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeInt(intNums[index], segmentWriteBuffer);
            segmentWriteBuffer.reset();
            blackhole.consume(segmentWriteBuffer);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void thWriteSegmentLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MarshallOldUtil.writeLong(longNums[index], segmentWriteBuffer);
            segmentWriteBuffer.reset();
            blackhole.consume(segmentWriteBuffer);
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(WriteIntegerBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
