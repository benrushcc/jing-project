package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=ser-str-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions=stackdepth=128"
//})
@Fork(3)
public class StringBeanSerializationBench {
    private static final int BATCH = 10000;
    private static final int BUFFER_SIZE = 2048;
    private static final int STRING_SIZE1 = 4;
    private static final int STRING_SIZE2 = 16;
    private static final int STRING_SIZE3 = 32;
    private static final int STRING_SIZE4 = 64;
    private static final int STRING_SIZE5 = 256;
    private StringEntity[] stringEntities;
    private ThreadLocalRandom random;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final WriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);

    @Setup(Level.Iteration)
    public void setup() {
        stringEntities = new StringEntity[BATCH];
        random = ThreadLocalRandom.current();
        for(int i = 0; i < BATCH; i++) {
            String s1 = randString(STRING_SIZE1);
            String s2 = randString(STRING_SIZE2);
            String s3 = randString(STRING_SIZE3);
            String s4 = randString(STRING_SIZE4);
            String s5 = randString(STRING_SIZE5);
            stringEntities[i] = new StringEntity(s1, s2, s3, s4, s5);
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        stringEntities = null;
        random = null;
    }

    private String randString(int size) {
        StringBuilder sb = new StringBuilder(size);
        while (sb.length() < size) {
            if(random.nextInt(2) == 0) {
                int cp = randomCodePoint();
                char[] chars = Character.toChars(cp);
                sb.append(chars);
            } else {
                char c = (char) random.nextInt(0x80);
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private int randomCodePoint() {
        for( ; ; ) {
            int cp = random.nextInt(0x110000);
            if (!Character.isSurrogate((char) cp)) {
                return cp;
            }
        }
    }

    @Benchmark
    public void jacksonSerialization(Blackhole blackhole) {
        int idx = random.nextInt(BATCH);
        jsonMapper.writeValue(byteArrayOutputStream, stringEntities[idx]);
        blackhole.consume(byteArrayOutputStream.size());
        byteArrayOutputStream.reset();
    }

    @Benchmark
    public void jingDefaultSerialization(Blackhole blackhole) {
        int idx = random.nextInt(BATCH);
        jsonDefaultSerializer.serializeMarshallableObject(stringEntities[idx], writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(StringBeanSerializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
