package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MapDeserializationBench extends AbstractBench {
    static {
        System.setProperty("jing.marshallcbor.maxmapsize", "1000");
    }

    private static final int BATCH = 1000;
    private static final int BUFFER_SIZE = 32 * BATCH;
    private static final TypeReference<Map<String, Integer>> MAP_TYPE = new TypeReference<>() {
    };
    private final Map<String, Integer> integerMap = CborBenchData.integerMap(BATCH);
    private final CBORMapper jacksonCborMapper = CBORMapper.builder().build();
    private final CborDeserializer cborDeserializer = new CborDeserializer(
            CborDeserializerOption.builder().setMaxMapElements(BATCH).build());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private byte[] cborBytes;
    private HeapReadBuffer readBuffer;

    @Setup
    public void setup() {
        jacksonCborMapper.writeValue(outputStream, integerMap);
        cborBytes = outputStream.toByteArray();
        outputStream.reset();
        readBuffer = new HeapReadBuffer(cborBytes);
        Map<String, Integer> jacksonMap = jacksonCborMapper.readValue(cborBytes, MAP_TYPE);
        Map<String, Integer> cborMap = cborDeserializer.deserializeMap(
                String.class, Integer.class, new HeapReadBuffer(cborBytes), HashMap::new);
        if (!integerMap.equals(jacksonMap) || !integerMap.equals(cborMap)) {
            throw new AssertionError("map round trip mismatch");
        }
    }

    @Benchmark
    public void jacksonCborDeserialization(Blackhole blackhole) {
        Map<String, Integer> map = jacksonCborMapper.readValue(cborBytes, MAP_TYPE);
        blackhole.consume(map);
    }

    @Benchmark
    public void jingCborDeserialization(Blackhole blackhole) {
        readBuffer.reset();
        Map<String, Integer> map = cborDeserializer.deserializeMap(
                String.class, Integer.class, readBuffer, HashMap::new);
        blackhole.consume(map);
    }
}
