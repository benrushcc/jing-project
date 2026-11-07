package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.entity.StringEntity;
import io.jingproject.bench.util.UtfUtil;
import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class StringBeanSerializationBench extends AbstractBench {
    private static final int BATCH = 1000;
    private static final int BUFFER_SIZE = 4096;
    private final CBORMapper jacksonCborMapper = CBORMapper.builder().build();
    private final CborSerializer cborSerializer = new CborSerializer(CborSerializerOption.defaultOption());
    private final CborDeserializer cborDeserializer = new CborDeserializer(CborDeserializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final WriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);
    private StringEntity[] stringEntities;
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
        for (int i = 0; i < BATCH; i++) {
            String s1 = UtfUtil.randTypedString(type, size);
            String s2 = UtfUtil.randTypedString(type, size);
            stringEntities[i] = new StringEntity(s1, s2);
        }
        HeapWriteBuffer buffer = new HeapWriteBuffer(BUFFER_SIZE);
        cborSerializer.serializeMarshallableObject(stringEntities[0], buffer);
        StringEntity actual = cborDeserializer.deserializeMarshallableObject(
                StringEntity.class, new HeapReadBuffer(buffer.toByteArray()));
        if (!stringEntities[0].equals(actual)) {
            throw new AssertionError("string bean round trip mismatch");
        }
        random = ThreadLocalRandom.current();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        stringEntities = null;
        random = null;
    }

    @Benchmark
    public void jacksonCbor(Blackhole blackhole) {
        int idx = random.nextInt(BATCH);
        jacksonCborMapper.writeValue(outputStream, stringEntities[idx]);
        blackhole.consume(outputStream.size());
        outputStream.reset();
    }

    @Benchmark
    public void jingCbor(Blackhole blackhole) {
        int idx = random.nextInt(BATCH);
        cborSerializer.serializeMarshallableObject(stringEntities[idx], writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.reset();
    }
}
