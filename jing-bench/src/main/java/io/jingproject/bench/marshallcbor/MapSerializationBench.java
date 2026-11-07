package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class MapSerializationBench extends AbstractBench {
    private static final int BATCH = 1000;
    private static final int BUFFER_SIZE = 32 * BATCH;
    private final Map<String, Integer> integerMap = CborBenchData.integerMap(BATCH);
    private final CBORMapper jacksonCborMapper = CBORMapper.builder().build();
    private final CborSerializer cborSerializer = new CborSerializer(CborSerializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final HeapWriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);

    @Benchmark
    public void jacksonCborSerialization(Blackhole blackhole) {
        jacksonCborMapper.writeValue(outputStream, integerMap);
        blackhole.consume(outputStream.size());
        outputStream.reset();
    }

    @Benchmark
    public void jingCborSerialization(Blackhole blackhole) {
        cborSerializer.serializeMap(integerMap, String.class, Integer.class, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.reset();
    }
}
