package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import io.jingproject.marshalljsontest.entity.IntEntity;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 2, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=ser-int-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions=stackdepth=128"
//})
@Fork(3)
public class IntegerSerializationBench {
    private static final int BATCH = 64;
    private static final int BUFFER_SIZE = BATCH * 32;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final HeapWriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);
    private final Random random = ThreadLocalRandom.current();
    private final List<IntEntity> entities = new ArrayList<>(BATCH);

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(IntegerSerializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setup() {
        entities.clear();
        for (int i = 0; i < BATCH; i++) {
            IntEntity entity = new IntEntity();
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
