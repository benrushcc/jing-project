package io.jingproject.marshalltest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.marshall.MarshallUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// 这个测试用来对比uscale算法向WriteBuffer中写入浮点数和使用jdk内置方法通过String转化写入浮点数之间的性能差异
// 这个对比并不是完全公平的，jdk有构造String的中间产物的相关开销，因此结果只能作为简单参考

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class WriteFloatBench {
    private static final int BATCH_SIZE = 10000;
    private static final int BUFFER_SIZE = 32;
    private float[] floats;
    private double[] doubles;
    private Arena arena;
    private HeapWriteBuffer heapWriteBuffer;
    private SegmentWriteBuffer segmentWriteBuffer;

    @Setup(Level.Iteration)
    public void setup() {
        floats = new float[BATCH_SIZE];
        doubles = new double[BATCH_SIZE];
        arena = Arena.ofConfined();
        heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
        segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BATCH_SIZE; ) {
            float f = Float.intBitsToFloat(random.nextInt());
            if (Float.isFinite(f)) {
                floats[i] = f;
                i++;
            }
        }
        for (int i = 0; i < BATCH_SIZE; ) {
            double f = Double.longBitsToDouble(random.nextLong());
            if (Double.isFinite(f)) {
                doubles[i] = f;
                i++;
            }
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        floats = null;
        doubles = null;
        arena.close();
        heapWriteBuffer = null;
        segmentWriteBuffer = null;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteHeapFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            float f = floats[index];
            byte[] bytes = Float.toString(f).getBytes(StandardCharsets.US_ASCII);
            heapWriteBuffer.writeBytes(bytes);
            blackhole.consume(heapWriteBuffer);
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteSegmentFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            float f = floats[index];
            byte[] bytes = Float.toString(f).getBytes(StandardCharsets.US_ASCII);
            segmentWriteBuffer.writeBytes(bytes);
            blackhole.consume(segmentWriteBuffer);
            segmentWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteHeapDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            double f = doubles[index];
            byte[] bytes = Double.toString(f).getBytes(StandardCharsets.US_ASCII);
            heapWriteBuffer.writeBytes(bytes);
            blackhole.consume(heapWriteBuffer);
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteSegmentDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            double f = doubles[index];
            byte[] bytes = Double.toString(f).getBytes(StandardCharsets.US_ASCII);
            segmentWriteBuffer.writeBytes(bytes);
            blackhole.consume(segmentWriteBuffer);
            segmentWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleWriteHeapFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            float f = floats[index];
            MarshallUtil.writeFloat(f, heapWriteBuffer);
            blackhole.consume(heapWriteBuffer);
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleWriteSegmentFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            float f = floats[index];
            MarshallUtil.writeFloat(f, segmentWriteBuffer);
            blackhole.consume(segmentWriteBuffer);
            segmentWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleWriteHeapDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            double f = doubles[index];
            MarshallUtil.writeDouble(f, heapWriteBuffer);
            blackhole.consume(heapWriteBuffer);
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleWriteSegmentDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            double f = doubles[index];
            MarshallUtil.writeDouble(f, segmentWriteBuffer);
            blackhole.consume(segmentWriteBuffer);
            segmentWriteBuffer.reset();
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(WriteFloatBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
