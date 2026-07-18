package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.SegmentReadBuffer;
import io.jingproject.marshalljson.FpStrRep;
import io.jingproject.marshalljson.JsonNumberUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class ReadFloatBench {
    private static final int INTEGER_SIZE = 2000;
    private static final int FRACTION2_SIZE = 2000;
    private static final int FRACTION4_SIZE = 2000;
    private static final int FRACTION8_SIZE = 2000;
    private static final int RANDOM_SIZE = 2000;
    private static final int BATCH_SIZE = INTEGER_SIZE + FRACTION2_SIZE + FRACTION4_SIZE + FRACTION8_SIZE + RANDOM_SIZE;
    private static final int MAX_FP_SIZE = 256;
    private Arena arena;
    private List<byte[]> floatBytes;
    private List<byte[]> doubleBytes;
    private List<MemorySegment> floatSegments;
    private List<MemorySegment> doubleSegments;

    private void loadFloat(float f) {
        byte[] bytes = String.valueOf(f).getBytes(StandardCharsets.US_ASCII);
        floatBytes.add(bytes);
        MemorySegment segment = arena.allocate(ValueLayout.JAVA_BYTE, bytes.length);
        MemorySegment.copy(bytes, 0, segment, ValueLayout.JAVA_BYTE, 0L, bytes.length);
        floatSegments.add(segment);
    }

    private void loadDouble(double d) {
        byte[] bytes = String.valueOf(d).getBytes(StandardCharsets.US_ASCII);
        doubleBytes.add(bytes);
        MemorySegment segment = arena.allocate(ValueLayout.JAVA_BYTE, bytes.length);
        MemorySegment.copy(bytes, 0, segment, ValueLayout.JAVA_BYTE, 0L, bytes.length);
        doubleSegments.add(segment);
    }

    private static float createFractionFloat(ThreadLocalRandom random, int i, int frac) {
        int bound = Math.powExact(10, frac);
        int i1 = random.nextInt(bound / 10, bound);
        int i2 = random.nextInt(0, bound);
        float f = i1 + i2 / ((float) bound);
        return (i & 1) == 0 ? f : -f;
    }

    private static double createFractionDouble(ThreadLocalRandom random, int i, int frac) {
        int bound = Math.powExact(10, frac);
        int i1 = random.nextInt(bound / 10, bound);
        int i2 = random.nextInt(0, bound);
        double f = i1 + i2 / ((double) bound);
        return (i & 1) == 0 ? f : -f;
    }

    @Setup(Level.Iteration)
    public void setup() {
        arena = Arena.ofConfined();
        floatBytes = new ArrayList<>(BATCH_SIZE);
        doubleBytes = new ArrayList<>(BATCH_SIZE);
        floatSegments = new ArrayList<>(BATCH_SIZE);
        doubleSegments = new ArrayList<>(BATCH_SIZE);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < INTEGER_SIZE; i++) {
            float f = (float) random.nextInt();
            loadFloat(f);
            double v = random.nextLong();
            loadDouble(v);
        }
        for(int i = 0; i < FRACTION2_SIZE; i++) {
            float f = createFractionFloat(random, i, 2);
            loadFloat(f);
            double v = createFractionDouble(random, i, 2);
            loadDouble(v);
        }
        for(int i = 0; i < FRACTION4_SIZE; i++) {
            float f = createFractionFloat(random, i, 4);
            loadFloat(f);
            double v = createFractionDouble(random, i, 4);
            loadDouble(v);
        }
        for(int i = 0; i < FRACTION8_SIZE; i++) {
            float f = createFractionFloat(random, i, 8);
            loadFloat(f);
            double v = createFractionDouble(random, i, 8);
            loadDouble(v);
        }
        for(int i = 0; i < RANDOM_SIZE; ) {
            float f = Float.intBitsToFloat(random.nextInt());
            if(Float.isFinite(f)) {
                loadFloat(f);
                i++;
            }
        }
        for (int i = 0; i < RANDOM_SIZE; ) {
            double f = Double.longBitsToDouble(random.nextLong());
            if (Double.isFinite(f)) {
                loadDouble(f);
                i++;
            }
        }
        Collections.shuffle(floatBytes);
        Collections.shuffle(doubleBytes);
        Collections.shuffle(floatSegments);
        Collections.shuffle(doubleSegments);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        arena.close();
        floatBytes = null;
        doubleBytes = null;
        floatSegments = null;
        doubleSegments = null;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkReadHeapFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = floatBytes.get(index);
            float f = Float.parseFloat(new String(bytes, StandardCharsets.US_ASCII));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkReadHeapDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = doubleBytes.get(index);
            double f = Double.parseDouble(new String(bytes, StandardCharsets.US_ASCII));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkReadSegmentFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = floatSegments.get(index);
            float f = Float.parseFloat(new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.US_ASCII));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkReadSegmentDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = doubleSegments.get(index);
            double f = Double.parseDouble(new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.US_ASCII));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleReadHeapFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = floatBytes.get(index);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte firstByte = heapReadBuffer.readByte();
            float f = JsonNumberUtil.readFloat(heapReadBuffer, MAX_FP_SIZE, firstByte);
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleReadHeapDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = doubleBytes.get(index);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte firstByte = heapReadBuffer.readByte();
            double d = JsonNumberUtil.readDouble(heapReadBuffer, MAX_FP_SIZE, firstByte);
            blackhole.consume(d);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleReadSegmentFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = floatSegments.get(index);
            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(segment);
            byte firstByte = segmentReadBuffer.readByte();
            float f = JsonNumberUtil.readFloat(segmentReadBuffer, MAX_FP_SIZE, firstByte);
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleReadSegmentDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = doubleSegments.get(index);
            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(segment);
            byte firstByte = segmentReadBuffer.readByte();
            double d = JsonNumberUtil.readDouble(segmentReadBuffer, MAX_FP_SIZE, firstByte);
            blackhole.consume(d);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleParseHeapFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = floatBytes.get(index);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte firstByte = heapReadBuffer.readByte();
            FpStrRep rep = JsonNumberUtil.parseFpStrRep(heapReadBuffer, MAX_FP_SIZE, firstByte);
            blackhole.consume(rep);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleParseSegmentFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = floatSegments.get(index);
            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(segment);
            byte firstByte = segmentReadBuffer.readByte();
            FpStrRep rep = JsonNumberUtil.parseFpStrRep(segmentReadBuffer, MAX_FP_SIZE, firstByte);
            blackhole.consume(rep);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleParseHeapDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = doubleBytes.get(index);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            byte firstByte = heapReadBuffer.readByte();
            FpStrRep rep = JsonNumberUtil.parseFpStrRep(heapReadBuffer, MAX_FP_SIZE, firstByte);
            blackhole.consume(rep);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleParseSegmentDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = doubleSegments.get(index);
            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(segment);
            byte firstByte = segmentReadBuffer.readByte();
            FpStrRep rep = JsonNumberUtil.parseFpStrRep(segmentReadBuffer, MAX_FP_SIZE, firstByte);
            blackhole.consume(rep);
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(ReadFloatBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
