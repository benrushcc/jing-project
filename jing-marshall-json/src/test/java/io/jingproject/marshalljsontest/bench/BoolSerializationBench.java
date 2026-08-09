package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class BoolSerializationBench {
    private static final int SIZE = 16000;
    private final Map<String, Boolean> boolMap = createBoolMap();
    private final JsonMapper jsonMapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(SIZE);
    private final HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(BoolSerializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }

    private Map<String, Boolean> createBoolMap() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put(i + "", random.nextInt(2) == 0);
        }
        return Map.copyOf(map);
    }

    @Benchmark
    public void jacksonSerialization(Blackhole blackhole) {
        jsonMapper.writeValue(outputStream, boolMap);
        blackhole.consume(outputStream.size());
        outputStream.reset();
    }

    @Benchmark
    public void jingSerialization(Blackhole blackhole) {
        jsonDefaultSerializer.serializeMap(boolMap, String.class, Boolean.class, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }
}
