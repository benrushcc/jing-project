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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class ReadIntegerBench {
    private static final int SMALL_SIZE = 8000;
    private static final int MEDIUM_SIZE = 1000;
    private static final int HUGE_SIZE = 1000;
    private static final int BATCH_SIZE = SMALL_SIZE + MEDIUM_SIZE + HUGE_SIZE;
    private Arena arena;
    private List<byte[]> intBytes;
    private List<byte[]> longBytes;
    private List<MemorySegment> intSegments;
    private List<MemorySegment> longSegments;

    @Setup
    public void setup() {
        arena = Arena.ofConfined();
        intBytes = new ArrayList<>(BATCH_SIZE);
        longBytes = new ArrayList<>(BATCH_SIZE);
        intSegments = new ArrayList<>(BATCH_SIZE);
        longSegments = new ArrayList<>(BATCH_SIZE);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < SMALL_SIZE; i++) {
            intBytes.add(String.valueOf(random.nextInt(-1000, 1000)).getBytes(StandardCharsets.UTF_8));
            longBytes.add(String.valueOf(random.nextLong(-1000L, 1000L)).getBytes(StandardCharsets.UTF_8));
            intSegments.add(arena.allocateFrom(String.valueOf(random.nextInt(-1000, 1000)), StandardCharsets.UTF_8));
            longSegments.add(arena.allocateFrom(String.valueOf(random.nextLong(-1000L, 1000L)), StandardCharsets.UTF_8));
        }
        for (int i = 0; i < MEDIUM_SIZE; i++) {
            intBytes.add(String.valueOf(random.nextInt(-100000, 100000)).getBytes(StandardCharsets.UTF_8));
            longBytes.add(String.valueOf(random.nextLong(-100000L, 100000L)).getBytes(StandardCharsets.UTF_8));
            intSegments.add(arena.allocateFrom(String.valueOf(random.nextInt(-100000, 100000)), StandardCharsets.UTF_8));
            longSegments.add(arena.allocateFrom(String.valueOf(random.nextLong(-100000L, 100000L)), StandardCharsets.UTF_8));
        }
        for (int i = 0; i < HUGE_SIZE; i++) {
            intBytes.add(String.valueOf(random.nextInt()).getBytes(StandardCharsets.UTF_8));
            longBytes.add(String.valueOf(random.nextLong()).getBytes(StandardCharsets.UTF_8));
            intSegments.add(arena.allocateFrom(String.valueOf(random.nextInt()), StandardCharsets.UTF_8));
            longSegments.add(arena.allocateFrom(String.valueOf(random.nextLong()), StandardCharsets.UTF_8));
        }
        Collections.shuffle(intBytes);
        Collections.shuffle(longBytes);
        Collections.shuffle(intSegments);
        Collections.shuffle(longSegments);
    }

    @TearDown
    public void tearDown() {
        arena.close();
        intBytes = null;
        longBytes = null;
        intSegments = null;
        longSegments = null;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkReadInteger(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = intBytes.get(index);
            String s = new String(bytes, StandardCharsets.UTF_8);
            int value = Integer.parseInt(s);
            blackhole.consume(value);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkReadLong(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = longBytes.get(index);
            String s = new String(bytes, StandardCharsets.UTF_8);
            long value = Long.parseLong(s);
            blackhole.consume(value);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void heapReadInteger(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = intBytes.get(index);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            int value = MarshallUtil.readInt(heapReadBuffer);
            blackhole.consume(value);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void heapReadLong(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            byte[] bytes = longBytes.get(index);
            HeapReadBuffer heapReadBuffer = new HeapReadBuffer(bytes);
            long value = MarshallUtil.readLong(heapReadBuffer);
            blackhole.consume(value);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void segmentReadInteger(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = intSegments.get(index);
            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(segment);
            int value = MarshallUtil.readInt(segmentReadBuffer);
            blackhole.consume(value);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void segmentReadLong(Blackhole blackhole) {
        for(int index = 0; index < BATCH_SIZE; index++) {
            MemorySegment segment = longSegments.get(index);
            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(segment);
            long value = MarshallUtil.readLong(segmentReadBuffer);
            blackhole.consume(value);
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(ReadIntegerBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
