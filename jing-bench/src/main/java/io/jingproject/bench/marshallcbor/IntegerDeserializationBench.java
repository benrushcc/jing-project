package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.entity.NumberEntity;
import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
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

public class IntegerDeserializationBench extends AbstractBench {
    private static final int BATCH = 64;
    private static final int BUFFER_SIZE = BATCH * 64;
    private final CBORMapper jacksonCborMapper = CBORMapper.builder().build();
    private final CborDeserializer cborDeserializer = new CborDeserializer(CborDeserializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final Random random = ThreadLocalRandom.current();
    private final List<NumberEntity> entities = new ArrayList<>(BATCH);
    private byte[][] cborBytes;
    private HeapReadBuffer[] readBuffers;

    @Setup(Level.Iteration)
    public void setup() {
        entities.clear();
        cborBytes = new byte[BATCH][];
        readBuffers = new HeapReadBuffer[BATCH];
        for (int i = 0; i < BATCH; i++) {
            NumberEntity entity = new NumberEntity();
            entity.setByteValue((byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE + 1));
            entity.setShortValue((short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE + 1));
            entity.setIntValue(random.nextInt());
            entity.setLongValue(random.nextLong());
            entities.add(entity);
            outputStream.reset();
            jacksonCborMapper.writeValue(outputStream, entity);
            cborBytes[i] = outputStream.toByteArray();
            readBuffers[i] = new HeapReadBuffer(cborBytes[i]);
            verify(entity, jacksonCborMapper.readValue(cborBytes[i], NumberEntity.class));
            verify(entity, cborDeserializer.deserializeMarshallableObject(
                    NumberEntity.class, new HeapReadBuffer(cborBytes[i])));
        }
    }

    private static void verify(NumberEntity expected, NumberEntity actual) {
        if (expected.getByteValue() != actual.getByteValue()
                || expected.getShortValue() != actual.getShortValue()
                || expected.getIntValue() != actual.getIntValue()
                || expected.getLongValue() != actual.getLongValue()) {
            throw new AssertionError("number deserialization mismatch");
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void jacksonCborDeserialization(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            NumberEntity entity = jacksonCborMapper.readValue(cborBytes[i], NumberEntity.class);
            blackhole.consume(entity);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void jingCborDeserialization(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            HeapReadBuffer readBuffer = readBuffers[i];
            readBuffer.reset();
            NumberEntity entity = cborDeserializer.deserializeMarshallableObject(NumberEntity.class, readBuffer);
            blackhole.consume(entity);
        }
    }
}
