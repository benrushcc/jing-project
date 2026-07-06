package io.jingproject.marshalljsontest.bench;

import com.google.common.base.Utf8;
import io.jingproject.marshalljson.JsonDeserializeUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 1, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@Fork(value = 1, jvmArgsAppend = {
//        "-Xbatch",
//        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:PrintAssemblyOptions=intel",
//        "-XX:CompileCommand=dontinline,io.jingproject.marshalljson.JsonDeserializeUtil::*",
//        "-XX:CompileCommand=PrintOptoAssembly,io.jingproject.marshalljson.JsonDeserializeUtil::*"
//})
 @Fork(3)
public class Utf8ValidationBench {
    private static final String[] ASCII_DATA = {"a"," abc", "something", "wtf", "why u bully me!", "zywoo", "tyloo", "elephant"};
    private static final String[] UTF_DATA = {"a"," abc", "something", "wtf", "why u bully me!", "zywoo", "tyloo", "éléphant","®", "↧","😨","😧","😦","😱","😫","😩"};
    private static final int BATCH = 1000;
    @SuppressWarnings("unused")
    @Param({"16", "64", "256"})
    private int N;
    private Random random;
    private List<byte[]> asciiList;
    private List<byte[]> utfList;
    private CharsetDecoder decoder;
    private CharBuffer charBuffer;

    @Setup(Level.Iteration)
    public void setup() {
        random = ThreadLocalRandom.current();
        asciiList = new ArrayList<>(BATCH);
        utfList = new ArrayList<>(BATCH);
        for (int i = 0; i < BATCH; i++) {
            StringBuilder sb1 = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            for(int j = 0; j < N; j++) {
                sb1.append(ASCII_DATA[random.nextInt(ASCII_DATA.length)]);
                sb2.append(UTF_DATA[random.nextInt(UTF_DATA.length)]);
            }
            asciiList.add(sb1.toString().getBytes(StandardCharsets.UTF_8));
            utfList.add(sb2.toString().getBytes(StandardCharsets.UTF_8));
        }
        decoder = StandardCharsets.UTF_8.newDecoder();
        charBuffer = CharBuffer.allocate(N * 32); // make sure buffer is big enough
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        random = null;
        asciiList = null;
        utfList = null;
        decoder = null;
        charBuffer = null;
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testJdkAsciiValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = asciiList.get(i);
            CoderResult cr = decoder.decode(ByteBuffer.wrap(data), charBuffer, true);
            blackhole.consume(cr);
            decoder.reset();
            charBuffer.clear();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testJdkUtfValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = utfList.get(i);
            CoderResult cr = decoder.decode(ByteBuffer.wrap(data), charBuffer, true);
            blackhole.consume(cr);
            decoder.reset();
            charBuffer.clear();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testGuavaAsciiValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = asciiList.get(i);
            blackhole.consume(Utf8.isWellFormed(data, 0, data.length));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testGuavaUtfValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = utfList.get(i);
            blackhole.consume(Utf8.isWellFormed(data, 0, data.length));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testVecAsciiValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = asciiList.get(i);
            blackhole.consume(JsonDeserializeUtil.validateUtf8(data, 0, data.length));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testVecUtfValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = utfList.get(i);
            blackhole.consume(JsonDeserializeUtil.validateUtf8(data, 0, data.length));
        }
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(Utf8ValidationBench.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
