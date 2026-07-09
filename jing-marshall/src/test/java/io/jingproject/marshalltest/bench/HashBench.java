package io.jingproject.marshalltest.bench;

import io.jingproject.marshall.hash.*;
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

// 这个测试是用来探索不同哈希算法之间的性能差距，各个算法实现其实都很快，但直接取byte不用计算的开销还是最小的

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class HashBench {

    @SuppressWarnings("unused")
    @Param({"4", "32", "256"})
    private int size;

    private byte[] buffer;

    private CRC32 crc32;
    private Hasher lengthHasher;
    private Hasher oneByteHasher;
    private Hasher twoByteHasher;
    private Hasher threeByteHasher;
    private Hasher fourByteHasher;
    private Hasher sumHasher;
    private Hasher fnvHasher;

    @Setup(Level.Iteration)
    public void setup() {
        buffer = new byte[size];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        random.nextBytes(buffer);
        crc32 = new CRC32();
        lengthHasher = new LengthHasher();
        oneByteHasher = new OneByteHasher();
        twoByteHasher = new TwoByteHasher();
        threeByteHasher = new ThreeByteHasher();
        fourByteHasher = new FourByteHasher();
        sumHasher = new SumHasher();
        fnvHasher = new FnvHasher();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        buffer = null;
        crc32 = null;
        lengthHasher = null;
        oneByteHasher = null;
        twoByteHasher = null;
        threeByteHasher = null;
        fourByteHasher = null;
        sumHasher = null;
        fnvHasher = null;
    }

    @Benchmark
    public void jdkArrayHash(Blackhole bh) {
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
        bh.consume(lengthHasher.hash(buffer));
    }

    @Benchmark
    public void oneByteHash(Blackhole bh) {
        bh.consume(oneByteHasher.hash(buffer));
    }

    @Benchmark
    public void twoByteHash(Blackhole bh) {
        bh.consume(twoByteHasher.hash(buffer));
    }

    @Benchmark
    public void threeByteHash(Blackhole bh) {
        bh.consume(threeByteHasher.hash(buffer));
    }

    @Benchmark
    public void fourByteHash(Blackhole bh) {
        bh.consume(fourByteHasher.hash(buffer));
    }

    @Benchmark
    public void sumHash(Blackhole bh) {
        bh.consume(sumHasher.hash(buffer));
    }

    @Benchmark
    public void fnvMulHash(Blackhole bh) {
        bh.consume(fnvHasher.hash(buffer));
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(HashBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
