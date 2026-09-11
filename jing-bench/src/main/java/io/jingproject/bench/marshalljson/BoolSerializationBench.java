package io.jingproject.bench.marshalljson;

import io.jingproject.bench.AbstractBench;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class BoolSerializationBench extends AbstractBench {
    private static final int BATCH = 1000;
    private static final int SIZE = 16000;
    private final List<Boolean> boolList = createBoolList();
    private final Map<String, Boolean> boolMap = createBoolMap();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(SIZE);
    private final HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);

    private List<Boolean> createBoolList() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Boolean> list = new ArrayList<>(BATCH);
        for (int i = 0; i < BATCH; i++) {
            list.add(random.nextBoolean());
        }
        return list;
    }

    private Map<String, Boolean> createBoolMap() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < BATCH; i++) {
            map.put(i + "", random.nextBoolean());
        }
        return map;
    }

    @Benchmark
    public void jacksonBoolMapSerialization(Blackhole blackhole) {
        jsonMapper.writeValue(outputStream, boolMap);
        blackhole.consume(outputStream.size());
        outputStream.reset();
    }

    @Benchmark
    public void jacksonBoolListSerialization(Blackhole blackhole) {
        jsonMapper.writeValue(outputStream, boolList);
        blackhole.consume(outputStream.size());
        outputStream.reset();
    }

    @Benchmark
    public void jingBoolListSerialization(Blackhole blackhole) {
        jsonDefaultSerializer.serializeCollection(boolList, Boolean.class, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.reset();
    }

    @Benchmark
    public void jingBoolMapSerialization(Blackhole blackhole) {
        jsonDefaultSerializer.serializeMap(boolMap, String.class, Boolean.class, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.reset();
    }
}
