package io.jingproject.marshalltest.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// 这个测试只是用来简单的探索一下biginteger的计算性能和long之间的差距有多大，事实证明在x64和arm64上，biginteger需要大概20ns的时间来执行乘法，这个耗时在高性能解析的场景中已经完全不能接受了
@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class BigIntegerBench {

    private static final int BATCH = 100000;
    private BigInteger[] bi1;
    private BigInteger[] bi2;
    private long[] long1;
    private long[] long2;

    @Setup(Level.Iteration)
    public void setup() {
        bi1 = new BigInteger[BATCH];
        bi2 = new BigInteger[BATCH];
        long1 = new long[BATCH];
        long2 = new long[BATCH];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BATCH; i++) {
            long l1 = random.nextLong();
            long l2 = random.nextLong();
            long high = Math.multiplyHigh(l1, l2);
            long low = l1 * l2;
            BigInteger v = BigInteger.valueOf(high).shiftLeft(64).add(BigInteger.valueOf(low));
            bi1[i] = v;
            long1[i] = low;
        }
        for (int i = 0; i < BATCH; i++) {
            long l1 = random.nextLong();
            long l2 = random.nextLong();
            long high = Math.multiplyHigh(l1, l2);
            long low = l1 * l2;
            BigInteger v = BigInteger.valueOf(high).shiftLeft(64).add(BigInteger.valueOf(low));
            bi2[i] = v;
            long2[i] = low;
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void longMultiply(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            long l1 = long1[i];
            long l2 = long2[i];
            bh.consume(l1 * l2);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void bigIntegerMultiply(Blackhole bh) {
        for (int i = 0; i < BATCH; i++) {
            BigInteger b1 = bi1[i];
            BigInteger b2 = bi2[i];
            bh.consume(b1.multiply(b2));
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(BigInteger.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
