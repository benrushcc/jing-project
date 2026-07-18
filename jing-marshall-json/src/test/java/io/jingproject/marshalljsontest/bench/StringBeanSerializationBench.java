package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import io.jingproject.marshalljsontest.UtfUtil;
import io.jingproject.marshalljsontest.entity.StringEntity;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 2, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=ser-str-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions=stackdepth=128"
//})
@Fork(1)
public class StringBeanSerializationBench {
    private static final int BATCH = 1000;
    private static final int BUFFER_SIZE = 1000;
    private StringEntity[] stringEntities;

    @Param({"4", "16", "64", "256"})
    @SuppressWarnings("unused")
    private int size;

    @Param({"empty", "ascii", "utf", "surr", "mostAscii"})
    @SuppressWarnings("unused")
    private String type;
    private Random random;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final WriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);

    @Setup(Level.Iteration)
    public void setup() {
        stringEntities = new StringEntity[BATCH];
        for(int i = 0; i < BATCH; i++) {
            String s1 = UtfUtil.randTypedString(type, size);
            String s2 = UtfUtil.randTypedString(type, size);
            stringEntities[i] = new StringEntity(s1, s2);
        }
        random = ThreadLocalRandom.current();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        stringEntities = null;
        random = null;
    }

    @Benchmark
    public void jackson(Blackhole blackhole) {
        int idx = random.nextInt(BATCH);
        jsonMapper.writeValue(byteArrayOutputStream, stringEntities[idx]);
        blackhole.consume(byteArrayOutputStream.size());
        byteArrayOutputStream.reset();
    }

    @Benchmark
    public void jing(Blackhole blackhole) {
        int idx = random.nextInt(BATCH);
        jsonDefaultSerializer.serializeMarshallableObject(stringEntities[idx], writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(StringBeanSerializationBench.class.getSimpleName())
                 .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }
}
