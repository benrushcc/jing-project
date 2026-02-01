package io.jingproject.marshalltest.bench;

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
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 800, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 800, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(8)
public class MarshallUtilBench {

    @Param({"1024", "4096", "16384"})
    private int CAPACITY;
    private Arena arena;
    private MemorySegment seg;

    @Setup
    public void setup() {
        arena = Arena.ofConfined();
        List<MemorySegment> l = new ArrayList<>(CAPACITY);
        int len = 0;
        for(int i = 0; i < CAPACITY; i++) {
            byte[] bytes = String.valueOf(i).getBytes(StandardCharsets.UTF_8);
            l.add(MemorySegment.ofArray(bytes));
            len = Math.addExact(len, bytes.length);
        }
        seg = arena.allocate(ValueLayout.JAVA_BYTE, Math.addExact(len, Math.multiplyExact(CAPACITY, Long.BYTES)));
        long offset = 0L;
        for (MemorySegment m : l) {
            long longLen = ValueLayout.JAVA_LONG.byteSize();
            long mLen = m.byteSize();
            seg.set(ValueLayout.JAVA_LONG_UNALIGNED, offset, mLen);
            MemorySegment.copy(m, 0L, seg, Math.addExact(offset, longLen), mLen);
            offset = Math.addExact(Math.addExact(offset, longLen), mLen);
        }
    }

    @TearDown
    public void tearDown() {
        arena.close();
    }

    @Benchmark
    public void jdkParseInt(Blackhole bh) {
        long offset = 0L;
        for(int i = 0; i < CAPACITY; i++) {
            long len = seg.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
            offset = Math.addExact(offset, ValueLayout.JAVA_LONG.byteSize());
            byte[] bytes = seg.asSlice(offset, len).toArray(ValueLayout.JAVA_BYTE);
            String s = new String(bytes, StandardCharsets.UTF_8);
            bh.consume(Integer.parseInt(s));
            offset = Math.addExact(offset, len);
        }
    }

    @Benchmark
    public void jdkParseLong(Blackhole bh) {
        long offset = 0L;
        for(int i = 0; i < CAPACITY; i++) {
            long len = seg.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
            offset = Math.addExact(offset, ValueLayout.JAVA_LONG.byteSize());
            byte[] bytes = seg.asSlice(offset, len).toArray(ValueLayout.JAVA_BYTE);
            String s = new String(bytes, StandardCharsets.UTF_8);
            bh.consume(Long.parseLong(s));
            offset = Math.addExact(offset, len);
        }
    }

    @Benchmark
    public void marshallParseInt(Blackhole bh) {
        long offset = 0L;
        for(int i = 0; i < CAPACITY; i++) {
            long len = seg.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
            offset = Math.addExact(offset, ValueLayout.JAVA_LONG.byteSize());
            bh.consume(MarshallUtil.parseInt(seg, offset, len));
            offset = Math.addExact(offset, len);
        }
    }

    @Benchmark
    public void marshallParseLong(Blackhole bh) {
        long offset = 0L;
        for(int i = 0; i < CAPACITY; i++) {
            long len = seg.get(ValueLayout.JAVA_LONG_UNALIGNED, offset);
            offset = Math.addExact(offset, ValueLayout.JAVA_LONG.byteSize());
            bh.consume(MarshallUtil.parseLong(seg, offset, len));
            offset = Math.addExact(offset, len);
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(MarshallUtilBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
