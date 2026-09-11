package io.jingproject.bench.marshalljson;

import io.jingproject.bench.AbstractBench;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MapSerializationBench extends AbstractBench {
    private static final int BATCH = 1000;
    private static final int BUFFER_SIZE = 16 * BATCH;
    private final Map<String, Integer> integerMap = createIntegerMap();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final HeapWriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);

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
