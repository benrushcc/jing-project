package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.entity.StringEntity;
import io.jingproject.bench.util.UtfUtil;
import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class StringBeanDeserializationBench extends AbstractBench {
    private static final int BATCH = 1000;
    private static final int BUFFER_SIZE = 4096;
    private final CBORMapper jacksonCborMapper = CBORMapper.builder().build();
    private final CborDeserializer cborDeserializer = new CborDeserializer(CborDeserializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private StringEntity[] stringEntities;
    private byte[][] cborBytes;
    private HeapReadBuffer[] readBuffers;
    private Random random;

    @Param({"4", "16", "64", "256"})
    @SuppressWarnings("unused")
    private int size;
    @Param({"empty", "ascii", "utf", "surr", "mostAscii"})
    @SuppressWarnings("unused")
    private String type;

    @Setup(Level.Iteration)
    public void setup() {
        stringEntities = new StringEntity[BATCH];
        cborBytes = new byte[BATCH][];
        readBuffers = new HeapReadBuffer[BATCH];
        for (int i = 0; i < BATCH; i++) {
            String s1 = UtfUtil.randTypedString(type, size);
            String s2 = UtfUtil.randTypedString(type, size);
            StringEntity entity = new StringEntity(s1, s2);
            stringEntities[i] = entity;
            outputStream.reset();
            jacksonCborMapper.writeValue(outputStream, entity);
            cborBytes[i] = outputStream.toByteArray();
            readBuffers[i] = new HeapReadBuffer(cborBytes[i]);
            verify(entity, jacksonCborMapper.readValue(cborBytes[i], StringEntity.class));
            verify(entity, cborDeserializer.deserializeMarshallableObject(
                    StringEntity.class, new HeapReadBuffer(cborBytes[i])));
        }
        random = ThreadLocalRandom.current();
    }

    private static void verify(StringEntity expected, StringEntity actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("string bean deserialization mismatch");
        }
    }

    @Benchmark
    public void jacksonCbor(Blackhole blackhole) {
        int idx = random.nextInt(BATCH);
        StringEntity entity = jacksonCborMapper.readValue(cborBytes[idx], StringEntity.class);
        blackhole.consume(entity);
    }

    @Benchmark
    public void jingCbor(Blackhole blackhole) {
        int idx = random.nextInt(BATCH);
        HeapReadBuffer readBuffer = readBuffers[idx];
        readBuffer.reset();
        StringEntity entity = cborDeserializer.deserializeMarshallableObject(StringEntity.class, readBuffer);
        blackhole.consume(entity);
    }
}
