package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import io.jingproject.marshalljsontest.TwiUtil;
import io.jingproject.marshalljsontest.twi.Twi;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 3)
public class TwiSerializationBench {
    private static final int SIZE = 819200;
    private final Twi twi = TwiUtil.deserializeTwiUsingJackson(TwiUtil.load());
    private final JsonMapper jsonMapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
    private final io.jingproject.marshalljson.old.JsonSerializer jsonOldSerializer = new io.jingproject.marshalljson.old.JsonSerializer(io.jingproject.marshalljson.old.JsonSerializerOption.defaultOption());
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final JsonSerializer jsonPoolSerializer = new JsonSerializer(JsonSerializerOption.builder().setPoolSize(1).build());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(SIZE);
    private final HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);

    @Benchmark
    public void jacksonSerialization(Blackhole blackhole) {
        jsonMapper.writeValue(outputStream, twi);
        blackhole.consume(outputStream.size());
        outputStream.reset();
    }

    @Benchmark
    public void jingOldSerialization(Blackhole blackhole) {
        jsonOldSerializer.serializeMarshallableObject(twi, Twi.class, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    @Benchmark
    public void jingDefaultSerialization(Blackhole blackhole) {
        jsonDefaultSerializer.serializeMarshallableObject(twi, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    @Benchmark
    public void jingPoolSerialization(Blackhole blackhole) {
        jsonPoolSerializer.serializeMarshallableObject(twi, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(TwiSerializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }
}
