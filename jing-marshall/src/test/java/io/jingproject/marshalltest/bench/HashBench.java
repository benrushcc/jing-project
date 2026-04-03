package io.jingproject.marshalltest.bench;

import io.jingproject.marshall.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class HashBench {

    @Param({"4", "8", "16", "32", "64", "128", "256"})
    private int size;

    private byte[] buffer;

    private static final CRC32 crc32 = new CRC32();
    private static final Hasher LENGTH_HASHER = new LengthHasher();
    private static final Hasher ONEBYTE_HASHER = new OneByteHasher();
    private static final Hasher TWOBYTE_HASHER = new TwoByteHasher();
    private static final Hasher THREEBYTE_HASHER = new ThreeByteHasher();
    private static final Hasher FOURBYTE_HASHER = new FourByteHasher();
    private static final Hasher FNV_MUL_HASHER= new FnvHasher();

    @Setup(Level.Iteration)
    public void setup() {
        buffer = new byte[size];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        random.nextBytes(buffer);
        crc32.reset();
    }

    @Benchmark
    public void arrayHash(Blackhole bh) {
        bh.consume(Arrays.hashCode(buffer));
    }

    @Benchmark
    public void manuallyArrayHash(Blackhole bh) {
        int hash = 0;
        for (byte b : buffer) {
            hash = hash * 31 + b;
        }
        bh.consume(hash);
    }

    @Benchmark
    public void crc32Hash(Blackhole bh) {
        crc32.update(buffer);
        bh.consume(crc32.getValue());
        crc32.reset();
    }

    @Benchmark
    public void lengthHash(Blackhole bh) {
        bh.consume(LENGTH_HASHER.hash(buffer));
    }

    @Benchmark
    public void oneByteHash(Blackhole bh) {
        bh.consume(ONEBYTE_HASHER.hash(buffer));
    }

    @Benchmark
    public void twoByteHash(Blackhole bh) {
        bh.consume(TWOBYTE_HASHER.hash(buffer));
    }

    @Benchmark
    public void threeByteHash(Blackhole bh) {
        bh.consume(THREEBYTE_HASHER.hash(buffer));
    }

    @Benchmark
    public void fourByteHash(Blackhole bh) {
        bh.consume(FOURBYTE_HASHER.hash(buffer));
    }

    @Benchmark
    public void fnvMulHash(Blackhole bh) {
        bh.consume(FNV_MUL_HASHER.hash(buffer));
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(HashBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
