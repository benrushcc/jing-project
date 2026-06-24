package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import io.jingproject.marshalljsontest.entity.SimpleEntity;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 3)
public class SimpleSerializationBench {
    private static final int BATCH = 10000;
    private static final int BUFFER_SIZE = 1024;
    private static final int STRING_SIZE = 16;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private SimpleEntity[] simpleEntities;
    private ThreadLocalRandom random;
    private JsonMapper jsonMapper;
    private JsonSerializer jsonDefaultSerializer;
    private io.jingproject.marshalljson.old.JsonSerializer oldJsonSerializer;
    private ByteArrayOutputStream byteArrayOutputStream;
    private WriteBuffer writeBuffer;

    @Setup(Level.Iteration)
    public void setup() {
        simpleEntities = new SimpleEntity[BATCH];
        random = ThreadLocalRandom.current();
        for (int i = 0; i < BATCH; i++) {
            int a = random.nextInt();
            long b = random.nextLong();
            float c = random.nextFloat() * a;
            double d = random.nextDouble() * b;
            StringBuilder sb = new StringBuilder(STRING_SIZE);
            for (int j = 0; j < STRING_SIZE; j++) {
                sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            simpleEntities[i] = new SimpleEntity(a, b, c, d, sb.toString());
        }
        jsonMapper = JsonMapper.builder().build();
        jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
        oldJsonSerializer = new io.jingproject.marshalljson.old.JsonSerializer(io.jingproject.marshalljson.old.JsonSerializerOption.defaultOption());
        byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
        writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        simpleEntities = null;
        random = null;
    }

    @Benchmark
    public void jacksonSerialization(Blackhole blackhole) {
        int index = random.nextInt(BATCH);
        jsonMapper.writeValue(byteArrayOutputStream, simpleEntities[index]);
        blackhole.consume(byteArrayOutputStream.size());
        byteArrayOutputStream.reset();
    }

    @Benchmark
    public void jingDefaultSerialization(Blackhole blackhole) {
        int index = random.nextInt(BATCH);
        jsonDefaultSerializer.serializeMarshallableObject(simpleEntities[index], writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    @Benchmark
    public void jingOldSerialization(Blackhole blackhole) {
        int index = random.nextInt(BATCH);
        oldJsonSerializer.serializeMarshallableObject(simpleEntities[index], SimpleEntity.class, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(SimpleSerializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
