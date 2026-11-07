package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.entity.NumberEntity;
import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class IntegerSerializationBench extends AbstractBench {
    private static final int BATCH = 64;
    private static final int BUFFER_SIZE = BATCH * 64;
    private final CBORMapper jacksonCborMapper = CBORMapper.builder().build();
    private final CborSerializer cborSerializer = new CborSerializer(CborSerializerOption.defaultOption());
    private final CborDeserializer cborDeserializer = new CborDeserializer(CborDeserializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final HeapWriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);
    private final Random random = ThreadLocalRandom.current();
    private final List<NumberEntity> entities = new ArrayList<>(BATCH);

    @Setup(Level.Iteration)
    public void setup() {
        entities.clear();
        for (int i = 0; i < BATCH; i++) {
            NumberEntity entity = new NumberEntity();
            entity.setByteValue((byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE + 1));
            entity.setShortValue((short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE + 1));
            entity.setIntValue(random.nextInt());
            entity.setLongValue(random.nextLong());
            HeapWriteBuffer buffer = new HeapWriteBuffer(BUFFER_SIZE);
            cborSerializer.serializeMarshallableObject(entity, buffer);
            NumberEntity actual = cborDeserializer.deserializeMarshallableObject(
                    NumberEntity.class, new HeapReadBuffer(buffer.toByteArray()));
            if (entity.getByteValue() != actual.getByteValue()
                    || entity.getShortValue() != actual.getShortValue()
                    || entity.getIntValue() != actual.getIntValue()
                    || entity.getLongValue() != actual.getLongValue()) {
                throw new AssertionError("cbor number round trip mismatch");
            }
            entities.add(entity);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void jacksonCborSerialization(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            jacksonCborMapper.writeValue(outputStream, entities.get(i));
            blackhole.consume(outputStream.size());
            outputStream.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void jingCborSerialization(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            cborSerializer.serializeMarshallableObject(entities.get(i), writeBuffer);
            blackhole.consume(writeBuffer.intPosition());
            writeBuffer.reset();
        }
    }
}
