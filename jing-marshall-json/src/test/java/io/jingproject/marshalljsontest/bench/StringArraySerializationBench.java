package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 2000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class StringArraySerializationBench {
    private static final int BATCH = 1000;
    private static final int LEN = 64;
    private static final int BUFFER_SIZE = 640;
    private Random random;
    private String[] asciiArr;
    private String[] utfArr;
    private String[] surrArr;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final WriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);

    @Setup(Level.Iteration)
    public void setup() {
        random = ThreadLocalRandom.current();
        asciiArr = new String[BATCH];
        utfArr = new String[BATCH];
        surrArr = new String[BATCH];
        StringBuilder sb = new StringBuilder(LEN * 2);
        for (int i = 0; i < BATCH; i++) {
            for (int j = 0; j < LEN; j++) {
                sb.append((char) random.nextInt(128));
            }
            asciiArr[i] = sb.toString();
            sb.setLength(0);
            for (int j = 0; j < LEN; j++) {
                int cp;
                do {
                    cp = random.nextInt(0x10000);
                } while (cp >= 0xD800 && cp <= 0xDFFF);
                sb.append((char) cp);
            }
            utfArr[i] = sb.toString();
            sb.setLength(0);
            for (int j = 0; j < LEN; j++) {
                int supplementary = 0x10000 + random.nextInt(0x100000);
                int high = 0xD800 | ((supplementary - 0x10000) >> 10);
                int low  = 0xDC00 | ((supplementary - 0x10000) & 0x3FF);
                sb.append((char) high);
                sb.append((char) low);
            }
            surrArr[i] = sb.toString();
            sb.setLength(0);
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        random = null;
        asciiArr = null;
        utfArr = null;
        surrArr = null;
    }

    @Benchmark
    public void jacksonAscii(Blackhole blackhole) {
        jsonMapper.writeValue(byteArrayOutputStream, asciiArr);
        blackhole.consume(byteArrayOutputStream.size());
        byteArrayOutputStream.reset();
    }

    @Benchmark
    public void jacksonUtf8(Blackhole blackhole) {
        jsonMapper.writeValue(byteArrayOutputStream, utfArr);
        blackhole.consume(byteArrayOutputStream.size());
        byteArrayOutputStream.reset();
    }

    @Benchmark
    public void jacksonSurr(Blackhole blackhole) {
        jsonMapper.writeValue(byteArrayOutputStream, surrArr);
        blackhole.consume(byteArrayOutputStream.size());
        byteArrayOutputStream.reset();
    }

    @Benchmark
    public void jingAscii(Blackhole blackhole) {
        jsonDefaultSerializer.serializeArray(asciiArr, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    @Benchmark
    public void jingUtf(Blackhole blackhole) {
        jsonDefaultSerializer.serializeArray(utfArr, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    @Benchmark
    public void jingSurr(Blackhole blackhole) {
        jsonDefaultSerializer.serializeArray(surrArr, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(StringArraySerializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
