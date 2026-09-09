package io.jingproject.bench.marshalljson;

import io.jingproject.bench.AbstractBench;
import io.jingproject.marshalljson.Utf8Validator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=utf-validation-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions:stackdepth=128"
//})
//@Fork(value = 1, jvmArgsAppend = {
//        "-Xbatch",
//        "-XX:-TieredCompilation",
//        "-XX:CompileCommand=print,io.jingproject.marshalljson.Utf8Validator::validateHeap",
//        "-XX:CompileCommand=option,io.jingproject.marshalljson.Utf8Validator::validateHeap,PrintInlining",
//        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:PrintAssemblyOptions=intel",
//})
public class Utf8ValidationBench extends AbstractBench {
    private static final String[] ASCII_DATA = {"a", " abc", "something", "wtf", "why u bully me!", "zywoo", "tyloo", "elephant"};
    private static final String[] UTF_DATA = {"a", " abc", "something", "wtf", "why u bully me!", "zywoo", "tyloo", "éléphant", "®", "↧", "😨", "😧", "😦", "😱", "😫", "😩"};
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
            for (int j = 0; j < N; j++) {
                sb1.append(ASCII_DATA[random.nextInt(ASCII_DATA.length)]);
                sb2.append(UTF_DATA[random.nextInt(UTF_DATA.length)]);
            }
            asciiList.add(sb1.toString().getBytes(StandardCharsets.UTF_8));
            utfList.add(sb2.toString().getBytes(StandardCharsets.UTF_8));
        }
        decoder = StandardCharsets.UTF_8.newDecoder();
        charBuffer = CharBuffer.allocate(N * 32);
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
    public void testScalarAsciiValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = asciiList.get(i);
            blackhole.consume(Utf8Validator.scalarValidateHeap(data, 0, data.length));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testScalarUtfValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = utfList.get(i);
            blackhole.consume(Utf8Validator.scalarValidateHeap(data, 0, data.length));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testVecAsciiValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = asciiList.get(i);
            blackhole.consume(Utf8Validator.validateHeap(data, 0, data.length));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void testVecUtfValidation(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            byte[] data = utfList.get(i);
            blackhole.consume(Utf8Validator.validateHeap(data, 0, data.length));
        }
    }
}
