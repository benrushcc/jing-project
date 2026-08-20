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
@Warmup(iterations = 3, time = 4000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 4000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=ser-simple-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions=stackdepth=128"
//})
@Fork(value = 3)
public class SimpleSerializationBench {
    private static final int BATCH = 10000;
    private static final int BUFFER_SIZE = 1024;
    private static final int STRING_SIZE = 16;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final WriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);
    private ThreadLocalRandom random;
    private SimpleEntity[] simpleEntities;

    @Setup(Level.Iteration)
    public void setup() {
        random = ThreadLocalRandom.current();
        simpleEntities = new SimpleEntity[BATCH];
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
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        random = null;
        simpleEntities = null;
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

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(SimpleSerializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
