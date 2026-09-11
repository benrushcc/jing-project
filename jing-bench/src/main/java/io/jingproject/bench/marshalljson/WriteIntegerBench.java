package io.jingproject.bench.marshalljson;

import io.jingproject.bench.AbstractBench;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.marshalljson.JsonNumberUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

//@Fork(value = 1, jvmArgsAppend = {
//        "-Xbatch",
//        "-XX:-TieredCompilation",
//        "-XX:CompileCommand=print,io.jingproject.marshalljson.JsonNumberUtil::writeLong",
//        "-XX:CompileCommand=option,io.jingproject.marshalljson.JsonNumberUtil::writeLong,PrintInlining",
//        "-XX:CompileCommand=print,io.jingproject.marshalljsontest.JsonNumberUtil::writeLongToHeap",
//        "-XX:CompileCommand=option,io.jingproject.marshalljsontest.JsonNumberUtil::writeLongToHeap,PrintInlining",
//        "-XX:CompileCommand=print,io.jingproject.marshalljsontest.JsonNumberUtil::writePositiveLongToHeap",
//        "-XX:CompileCommand=option,io.jingproject.marshalljsontest.JsonNumberUtil::writePositiveLongToHeap,PrintInlining",
//        "-XX:CompileCommand=print,io.jingproject.marshalljsontest.NumberUtil::writeLong3",
//        "-XX:CompileCommand=option,io.jingproject.marshalljsontest.NumberUtil::writeLong3,PrintInlining",
//        "-XX:CompileCommand=print,io.jingproject.marshalljsontest.NumberUtil::writeLongToHeap",
//        "-XX:CompileCommand=option,io.jingproject.marshalljsontest.NumberUtil::writeLongToHeap,PrintInlining",
//        "-XX:CompileCommand=print,io.jingproject.marshalljsontest.NumberUtil::writePositiveLongToHeap",
//        "-XX:CompileCommand=option,io.jingproject.marshalljsontest.NumberUtil::writePositiveLongToHeap,PrintInlining",
//        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:PrintAssemblyOptions=intel",
//})
public class WriteIntegerBench extends AbstractBench {
    private static final int BUFFER_SIZE = 32;
    private static final int SMALL_SIZE = 8000;
    private static final int MEDIUM_SIZE = 1000;
    private static final int HUGE_SIZE = 1000;
    private static final int BATCH_SIZE = SMALL_SIZE + MEDIUM_SIZE + HUGE_SIZE;
    private Random random;
    private int[] intNums;
    private long[] longNums;
    private Arena arena;
    private HeapWriteBuffer heapWriteBuffer;
    private SegmentWriteBuffer segmentWriteBuffer;

    @Setup(Level.Iteration)
    public void setup() {
        random = ThreadLocalRandom.current();
        intNums = new int[BATCH_SIZE];
        longNums = new long[BATCH_SIZE];
        arena = Arena.ofConfined();
        heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
        segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
        for (int j = 0; j < SMALL_SIZE; j++) {
            intNums[j] = random.nextInt(-1000, 1000);
            longNums[j] = random.nextLong(-1000L, 1000L);
        }
        for (int j = 0; j < MEDIUM_SIZE; j++) {
            intNums[SMALL_SIZE + j] = random.nextInt(-100000, 100000);
            longNums[SMALL_SIZE + j] = random.nextLong(-100000L, 100000L);
        }
        for (int j = 0; j < HUGE_SIZE; j++) {
            intNums[SMALL_SIZE + MEDIUM_SIZE + j] = random.nextInt();
            longNums[SMALL_SIZE + MEDIUM_SIZE + j] = random.nextLong();
        }
        for (int i = intNums.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = intNums[i];
            intNums[i] = intNums[j];
            intNums[j] = temp;
        }
        for (int i = longNums.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            long temp = longNums[i];
            longNums[i] = longNums[j];
            longNums[j] = temp;
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        random = null;
        intNums = null;
        longNums = null;
        arena.close();
        heapWriteBuffer = null;
        segmentWriteBuffer = null;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteHeapInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            String s = Integer.toString(intNums[index]);
            heapWriteBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
            blackhole.consume(heapWriteBuffer.intPosition());
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteHeapLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            String s = Long.toString(longNums[index]);
            heapWriteBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
            blackhole.consume(heapWriteBuffer.intPosition());
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteSegmentInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            String s = Integer.toString(intNums[index]);
            segmentWriteBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
            blackhole.consume(segmentWriteBuffer.intPosition());
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteSegmentLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            String s = Long.toString(longNums[index]);
            segmentWriteBuffer.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
            blackhole.consume(segmentWriteBuffer.intPosition());
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jsonWriteHeapInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            JsonNumberUtil.writeInt(intNums[index], heapWriteBuffer);
            blackhole.consume(heapWriteBuffer.intPosition());
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jsonWriteHeapLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            JsonNumberUtil.writeLong(longNums[index], heapWriteBuffer);
            blackhole.consume(heapWriteBuffer.intPosition());
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jsonWriteSegmentInt(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            JsonNumberUtil.writeInt(intNums[index], segmentWriteBuffer);
            blackhole.consume(segmentWriteBuffer.longPosition());
            segmentWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jsonWriteSegmentLong(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            JsonNumberUtil.writeLong(longNums[index], segmentWriteBuffer);
            blackhole.consume(segmentWriteBuffer.longPosition());
            segmentWriteBuffer.reset();
        }
    }
}
