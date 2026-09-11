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
//        "-XX:CompileCommand=print,io.jingproject.marshalljson.JsonNumberUtil::writeDouble",
//        "-XX:CompileCommand=option,io.jingproject.marshalljson.JsonNumberUtil::writeDouble,PrintInlining",
//        "-XX:CompileCommand=print,io.jingproject.marshalljson.JsonNumberUtil::writeDecimalFpToHeap",
//        "-XX:CompileCommand=option,io.jingproject.marshalljson.JsonNumberUtil::writeDecimalFpToHeap,PrintInlining",
//        "-XX:CompileCommand=print,io.jingproject.marshalljson.JsonNumberUtil::toDecimalFp",
//        "-XX:CompileCommand=option,io.jingproject.marshalljson.JsonNumberUtil::toDecimalFp,PrintInlining",
//        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:PrintAssemblyOptions=intel",
//})
public class WriteFloatBench extends AbstractBench {
    private static final int BATCH_SIZE = 10000;
    private static final int BUFFER_SIZE = 32;
    private Random random;
    private float[] floats;
    private double[] doubles;
    private Arena arena;
    private HeapWriteBuffer heapWriteBuffer;
    private SegmentWriteBuffer segmentWriteBuffer;

    @Setup(Level.Iteration)
    public void setup() {
        random = ThreadLocalRandom.current();
        floats = new float[BATCH_SIZE];
        doubles = new double[BATCH_SIZE];
        arena = Arena.ofConfined();
        heapWriteBuffer = new HeapWriteBuffer(BUFFER_SIZE);
        segmentWriteBuffer = new SegmentWriteBuffer(arena, BUFFER_SIZE);
        for (int i = 0; i < BATCH_SIZE; ) {
            float f = Float.intBitsToFloat(random.nextInt());
            if (Float.isFinite(f)) {
                floats[i] = f;
                i++;
            }
        }
        for (int i = 0; i < BATCH_SIZE; ) {
            double f = Double.longBitsToDouble(random.nextLong());
            if (Double.isFinite(f)) {
                doubles[i] = f;
                i++;
            }
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        random = null;
        floats = null;
        doubles = null;
        arena.close();
        heapWriteBuffer = null;
        segmentWriteBuffer = null;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteHeapFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            float f = floats[index];
            byte[] bytes = Float.toString(f).getBytes(StandardCharsets.US_ASCII);
            heapWriteBuffer.writeBytes(bytes);
            blackhole.consume(heapWriteBuffer);
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteSegmentFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            float f = floats[index];
            byte[] bytes = Float.toString(f).getBytes(StandardCharsets.US_ASCII);
            segmentWriteBuffer.writeBytes(bytes);
            blackhole.consume(segmentWriteBuffer);
            segmentWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteHeapDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            double f = doubles[index];
            byte[] bytes = Double.toString(f).getBytes(StandardCharsets.US_ASCII);
            heapWriteBuffer.writeBytes(bytes);
            blackhole.consume(heapWriteBuffer);
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void jdkWriteSegmentDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            double f = doubles[index];
            byte[] bytes = Double.toString(f).getBytes(StandardCharsets.US_ASCII);
            segmentWriteBuffer.writeBytes(bytes);
            blackhole.consume(segmentWriteBuffer);
            segmentWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleWriteHeapFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            float f = floats[index];
            JsonNumberUtil.writeFloat(f, heapWriteBuffer);
            blackhole.consume(heapWriteBuffer);
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleWriteSegmentFloat(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            float f = floats[index];
            JsonNumberUtil.writeFloat(f, segmentWriteBuffer);
            blackhole.consume(segmentWriteBuffer);
            segmentWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleWriteHeapDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            double f = doubles[index];
            JsonNumberUtil.writeDouble(f, heapWriteBuffer);
            blackhole.consume(heapWriteBuffer);
            heapWriteBuffer.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public void uscaleWriteSegmentDouble(Blackhole blackhole) {
        for (int index = 0; index < BATCH_SIZE; index++) {
            double f = doubles[index];
            JsonNumberUtil.writeDouble(f, segmentWriteBuffer);
            blackhole.consume(segmentWriteBuffer);
            segmentWriteBuffer.reset();
        }
    }
}