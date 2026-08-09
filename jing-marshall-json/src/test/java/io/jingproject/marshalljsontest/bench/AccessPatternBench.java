package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.ArrayAccess;
import io.jingproject.common.Utils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 2, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class AccessPatternBench {
    private static final byte b = (byte) '0';
    private static final short SHORT_CONSTANT = Utils.compact((byte) (b + 1), (byte) (b + 2));
    private static final int INT_CONSTANT = Utils.compact(Utils.compact((byte) (b + 2), (byte) (b + 3)), Utils.compact((byte) (b + 1), (byte) (b + 2)));
    private static final long LONG_CONSTANT = Utils.compact(Utils.compact(Utils.compact(b, (byte) (b + 1)), Utils.compact((byte) (b + 2), (byte) (b + 3))),
            Utils.compact(Utils.compact((byte) (b + 4), (byte) (b + 5)), Utils.compact((byte) (b + 6), (byte) (b + 7))));
    private Random random;
    private byte[] bytes;

    @Setup
    public void setup() {
        random = ThreadLocalRandom.current();
        bytes = new byte[16000];
    }

    @TearDown
    public void tearDown() {
        random = null;
        bytes = null;
    }

    @Benchmark
    public void directAccess(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 4000; i++) {
            int cp = random.nextInt(0x110000);
            bytes[position++] = (byte) (0xF0 | (cp >> 18));
            bytes[position++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
            bytes[position++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
            bytes[position++] = (byte) (0x80 | (cp & 0x3F));
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void directAccessConstant(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 4000; i++) {
            bytes[position++] = b;
            bytes[position++] = b + 1;
            bytes[position++] = b + 2;
            bytes[position++] = b + 3;
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void mixedShortAccess(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 4000; i++) {
            int cp = random.nextInt(0x110000);
            bytes[position++] = (byte) (0xF0 | (cp >> 18));
            ArrayAccess.setShort(bytes, position, Utils.compact((byte) (0x80 | ((cp >> 12) & 0x3F)), (byte) (0x80 | ((cp >> 6) & 0x3F))));
            position += 2;
            bytes[position++] = (byte) (0x80 | (cp & 0x3F));
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void mixedShortAccessConstant(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 4000; i++) {
            bytes[position++] = b;
            ArrayAccess.setShort(bytes, position, SHORT_CONSTANT);
            position += 2;
            bytes[position++] = b + 3;
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void mixedIntAccess(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 2000; i++) {
            int cp = random.nextInt(0x110000);
            int cp2 = random.nextInt(0x110000);
            bytes[position++] = (byte) (0xF0 | (cp >> 18));
            bytes[position++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
            short s1 = Utils.compact((byte) (0x80 | ((cp >> 6) & 0x3F)), (byte) (0x80 | (cp & 0x3F)));
            short s2 = Utils.compact((byte) (0xF0 | (cp2 >> 18)), (byte) (0x80 | ((cp2 >> 12) & 0x3F)));
            ArrayAccess.setInt(bytes, position, Utils.compact(s1, s2));
            bytes[position++] = (byte) (0x80 | ((cp2 >> 6) & 0x3F));
            bytes[position++] = (byte) (0x80 | (cp2 & 0x3F));
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void mixedIntAccessConstant(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 2000; i++) {
            bytes[position++] = b;
            bytes[position++] = b + 1;
            ArrayAccess.setInt(bytes, position, INT_CONSTANT);
            bytes[position++] = b + 2;
            bytes[position++] = b + 3;
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void longAccess(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 2000; i++) {
            int cp = random.nextInt(0x110000);
            int cp2 = random.nextInt(0x110000);
            short s1 = Utils.compact((byte) (0xF0 | (cp >> 18)), (byte) (0x80 | ((cp >> 12) & 0x3F)));
            short s2 = Utils.compact((byte) (0x80 | ((cp >> 6) & 0x3F)), (byte) (0x80 | (cp & 0x3F)));
            short s3 = Utils.compact((byte) (0xF0 | (cp2 >> 18)), (byte) (0x80 | ((cp2 >> 12) & 0x3F)));
            short s4 = Utils.compact((byte) (0x80 | ((cp2 >> 6) & 0x3F)), (byte) (0x80 | (cp2 & 0x3F)));
            long l = Utils.compact(Utils.compact(s1, s2), Utils.compact(s3, s4));
            ArrayAccess.setLong(bytes, position, l);
            position += 8;
        }
        blackhole.consume(position);
    }

    @Benchmark
    public void longAccessConstant(Blackhole blackhole) {
        int position = 0;
        for (int i = 0; i < 2000; i++) {
            ArrayAccess.setLong(bytes, position, LONG_CONSTANT);
            position += 8;
        }
        blackhole.consume(position);
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(AccessPatternBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
