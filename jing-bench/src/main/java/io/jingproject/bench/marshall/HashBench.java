package io.jingproject.bench.marshall;

import io.jingproject.bench.AbstractBench;
import io.jingproject.marshall.hash.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HashBench extends AbstractBench {
    private static final int BATCH = 1000;
    private final Hasher lengthHasher = new LengthHasher();
    private final Hasher oneByteHasher = new OneByteHasher();
    private final Hasher twoByteHasher = new TwoByteHasher();
    private final Hasher threeByteHasher = new ThreeByteHasher();
    private final Hasher fourByteHasher = new FourByteHasher();
    private final Hasher sumHasher = new SumHasher();
    private final Hasher fnvHasher = new FnvHasher();
    @SuppressWarnings("unused")
    @Param({"4", "32", "256"})
    private int size;
    private List<byte[]> buffers;

    @Setup(Level.Iteration)
    public void setup() {
        buffers = new ArrayList<>(BATCH);
        for (int i = 0; i < BATCH; i++) {
            byte[] bytes = new byte[size];
            ThreadLocalRandom random = ThreadLocalRandom.current();
            random.nextBytes(bytes);
            buffers.add(bytes);
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        buffers = null;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void lengthHash(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            bh.consume(lengthHasher.hash(buffers.get(i)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void oneByteHash(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            bh.consume(oneByteHasher.hash(buffers.get(i)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void twoByteHash(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            bh.consume(twoByteHasher.hash(buffers.get(i)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void threeByteHash(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            bh.consume(threeByteHasher.hash(buffers.get(i)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void fourByteHash(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            bh.consume(fourByteHasher.hash(buffers.get(i)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void sumHash(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            bh.consume(sumHasher.hash(buffers.get(i)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void fnvHash(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            bh.consume(fnvHasher.hash(buffers.get(i)));
        }
    }
}
