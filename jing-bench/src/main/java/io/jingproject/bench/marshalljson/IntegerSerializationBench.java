package io.jingproject.bench.marshalljson;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.entity.NumberEntity;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=ser-int-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions=stackdepth=128"
//})
public class IntegerSerializationBench extends AbstractBench {
    private static final int BATCH = 64;
    private static final int BUFFER_SIZE = BATCH * 32;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
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
            entities.add(entity);
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void jacksonSerialization(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            jsonMapper.writeValue(outputStream, entities.get(i));
            blackhole.consume(outputStream.size());
            outputStream.reset();
        }
    }

    @Benchmark
    @OperationsPerInvocation(BATCH)
    public void jingSerialization(Blackhole blackhole) {
        for (int i = 0; i < BATCH; i++) {
            jsonDefaultSerializer.serializeMarshallableObject(entities.get(i), writeBuffer);
            blackhole.consume(writeBuffer.intPosition());
            writeBuffer.setPosition(0);
        }
    }
}
