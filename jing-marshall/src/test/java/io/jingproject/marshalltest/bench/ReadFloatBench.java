package io.jingproject.marshalltest.bench;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.SegmentReadBuffer;
import io.jingproject.marshall.MarshallUtil;
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
    private static final int BATCH_SIZE = 10000;
    private Arena arena;
    private List<byte[]> floatBytes;
    private List<byte[]> doubleBytes;
    private List<MemorySegment> floatSegments;
    private List<MemorySegment> doubleSegments;

    @Setup(Level.Iteration)
    public void setup() {
        arena = Arena.ofConfined();
        floatBytes = new ArrayList<>(BATCH_SIZE);
        doubleBytes = new ArrayList<>(BATCH_SIZE);
        floatSegments = new ArrayList<>(BATCH_SIZE);
        doubleSegments = new ArrayList<>(BATCH_SIZE);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < BATCH_SIZE; ) {
            float f = Float.intBitsToFloat(random.nextInt());
            if(Float.isFinite(f)) {
                byte[] bytes = String.valueOf(f).getBytes(StandardCharsets.US_ASCII);
                floatBytes.add(bytes);
                MemorySegment segment = arena.allocate(ValueLayout.JAVA_BYTE, bytes.length);
                MemorySegment.copy(bytes, 0, segment, ValueLayout.JAVA_BYTE, 0L, bytes.length);
                floatSegments.add(segment);
                i++;
            }
        }
        for(int i = 0; i < BATCH_SIZE; ) {
            double f = Double.longBitsToDouble(random.nextLong());
            if(Double.isFinite(f)) {
                byte[] bytes = String.valueOf(f).getBytes(StandardCharsets.US_ASCII);
                doubleBytes.add(bytes);
                MemorySegment segment = arena.allocate(ValueLayout.JAVA_BYTE, bytes.length);
                MemorySegment.copy(bytes, 0, segment, ValueLayout.JAVA_BYTE, 0L, bytes.length);
                doubleSegments.add(segment);
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
    public void jdkHeapReadFloat(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = floatBytes.get(index);
            float f = Float.parseFloat(new String(bytes, StandardCharsets.US_ASCII));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkHeapReadDouble(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = doubleBytes.get(index);
            double f = Double.parseDouble(new String(bytes, StandardCharsets.US_ASCII));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkSegmentReadFloat(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = floatSegments.get(index);
            float f = Float.parseFloat(new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.US_ASCII));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkSegmentReadDouble(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = doubleSegments.get(index);
            double f = Double.parseDouble(new String(segment.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.US_ASCII));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleHeapReadFloat(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = floatBytes.get(index);
            float f = MarshallUtil.readFloat(new HeapReadBuffer(bytes));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleHeapReadDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = doubleBytes.get(index);
            double d = MarshallUtil.readDouble(new HeapReadBuffer(bytes));
            blackhole.consume(d);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleSegmentReadFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = floatSegments.get(index);
            float f = MarshallUtil.readFloat(new SegmentReadBuffer(segment));
            blackhole.consume(f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleSegmentReadDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = doubleSegments.get(index);
            double d = MarshallUtil.readDouble(new SegmentReadBuffer(segment));
            blackhole.consume(d);
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(ReadFloatBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
