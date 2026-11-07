package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.twi.Twi;
import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
public class TwiSerializationBench extends AbstractBench {
    private static final int SIZE = 819200;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final CBORMapper jacksonCborMapper =
            CBORMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
    private final CborSerializer cborSerializer = new CborSerializer(CborSerializerOption.defaultOption());
    private final CborDeserializer cborDeserializer = new CborDeserializer(CborDeserializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(SIZE);
    private final HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
    private final Twi twi = jsonMapper.readValue(Twi.asString(), Twi.class);

    @Setup(Level.Trial)
    public void verify() {
        outputStream.reset();
        jacksonCborMapper.writeValue(outputStream, twi);
        byte[] jacksonBytes = outputStream.toByteArray();
        outputStream.reset();
        if (!twi.equals(jacksonCborMapper.readValue(jacksonBytes, Twi.class))) {
            throw new AssertionError("twi jackson cbor round trip mismatch");
        }
        HeapWriteBuffer buffer = new HeapWriteBuffer(SIZE);
        cborSerializer.serializeMarshallableObject(twi, buffer);
        Twi actual = cborDeserializer.deserializeMarshallableObject(
                Twi.class, new HeapReadBuffer(buffer.toByteArray()));
        if (!twi.equals(actual)) {
            throw new AssertionError("twi cbor round trip mismatch");
        }
    }

    @Benchmark
    public void jacksonCborSerialization(Blackhole blackhole) {
        jacksonCborMapper.writeValue(outputStream, twi);
        blackhole.consume(outputStream.size());
        outputStream.reset();
    }

    @Benchmark
    public void jingCborSerialization(Blackhole blackhole) {
        cborSerializer.serializeMarshallableObject(twi, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.reset();
    }
}
