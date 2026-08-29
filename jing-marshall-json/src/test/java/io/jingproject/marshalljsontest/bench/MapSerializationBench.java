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
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
public class MapSerializationBench {
    private static final int BATCH = 1000;
    private static final int BUFFER_SIZE = 16 * BATCH;
    private final Map<String, Integer> integerMap = createIntegerMap();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final HeapWriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(MapSerializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }

    private Map<String, Integer> createIntegerMap() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < BATCH; i++) {
            map.put(i + "", i);
        }
        return Map.copyOf(map);
    }

    @Benchmark
    public void jacksonSerialization(Blackhole blackhole) {
        jsonMapper.writeValue(outputStream, integerMap);
        blackhole.consume(outputStream.size());
        outputStream.reset();
    }

    @Benchmark
    public void jingSerialization(Blackhole blackhole) {
        jsonDefaultSerializer.serializeMap(integerMap, String.class, Integer.class, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }
}
