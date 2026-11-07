package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.twi.Twi;
import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
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
public class TwiDeserializationBench extends AbstractBench {
    private static final int SIZE = 819200;
    private final byte[] jsonBytes = Twi.asBytes();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final CBORMapper jacksonCborMapper =
            CBORMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
    private final CborDeserializer cborDeserializer = new CborDeserializer(CborDeserializerOption.defaultOption());
    private final Twi twi = jsonMapper.readValue(jsonBytes, Twi.class);
    private final byte[] cborBytes = createCborBytes();
    private final HeapReadBuffer readBuffer = new HeapReadBuffer(cborBytes);

    private byte[] createCborBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(SIZE);
        jacksonCborMapper.writeValue(outputStream, twi);
        return outputStream.toByteArray();
    }

    @Setup(Level.Trial)
    public void verify() {
        Twi jacksonTwi = jacksonCborMapper.readValue(cborBytes, Twi.class);
        if (!twi.equals(jacksonTwi)) {
            throw new AssertionError("twi jackson cbor round trip mismatch");
        }
        readBuffer.reset();
        Twi cborTwi = cborDeserializer.deserializeMarshallableObject(Twi.class, readBuffer);
        if (!twi.equals(cborTwi)) {
            throw new AssertionError("twi cbor round trip mismatch");
        }
    }

    @Benchmark
    public void jacksonCborDeserialization(Blackhole blackhole) {
        Twi actual = jacksonCborMapper.readValue(cborBytes, Twi.class);
        blackhole.consume(actual);
    }

    @Benchmark
    public void jingCborDeserialization(Blackhole blackhole) {
        readBuffer.reset();
        Twi actual = cborDeserializer.deserializeMarshallableObject(Twi.class, readBuffer);
        blackhole.consume(actual);
    }
}
