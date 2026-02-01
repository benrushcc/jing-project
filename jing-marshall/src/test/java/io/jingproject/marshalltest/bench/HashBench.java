package io.jingproject.marshalltest.bench;

import io.jingproject.marshall.MarshallUtil;
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
@Warmup(iterations = 1, time = 800, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 800, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(8)
public class HashBench {

    @Param({"64", "1024", "16384"})
    private int size;

    private byte[] buffer;

    private CRC32 crc32 = new CRC32();

    @Setup
    public void setup() {
        buffer = new byte[size];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        random.nextBytes(buffer);
    }

    @Benchmark
    public void jdkArrayHash(Blackhole bh) {
        bh.consume(Arrays.hashCode(buffer));
    }

    @Benchmark
    public void vectorArrayHash(Blackhole bh) {
        bh.consume(MarshallUtil.hashCodeSIMD(buffer));
    }

    @Benchmark
    public void crc32ArrayHash(Blackhole bh) {
        crc32.update(buffer);
        bh.consume(crc32.getValue());
        crc32.reset();
    }

    @Benchmark
    public void manuallyArrayHash(Blackhole bh) {
        int r = 1;
        for(int i = 0; i < size; i++) {
            r = r * 31 + Byte.toUnsignedInt(buffer[i]);
        }
        bh.consume(r);
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(HashBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
