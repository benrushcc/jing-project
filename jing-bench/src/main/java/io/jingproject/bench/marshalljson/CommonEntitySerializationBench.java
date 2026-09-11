package io.jingproject.bench.marshalljson;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.entity.CommonEntity;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ThreadLocalRandom;

//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=ser-simple-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions=stackdepth=128"
//})
public class CommonEntitySerializationBench extends AbstractBench {
    private static final int BATCH = 10000;
    private static final int BUFFER_SIZE = 1024;
    private static final int STRING_SIZE = 16;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final WriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);
    private ThreadLocalRandom random;
    private CommonEntity[] simpleEntities;

    @Setup(Level.Iteration)
    public void setup() {
        random = ThreadLocalRandom.current();
        simpleEntities = new CommonEntity[BATCH];
        for (int i = 0; i < BATCH; i++) {
            int a = random.nextInt();
            long b = random.nextLong();
            float c = random.nextFloat() * a;
            double d = random.nextDouble() * b;
            StringBuilder sb = new StringBuilder(STRING_SIZE);
            for (int j = 0; j < STRING_SIZE; j++) {
                sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            simpleEntities[i] = new CommonEntity(a, b, c, d, sb.toString());
        }
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        random = null;
        simpleEntities = null;
    }

    @Benchmark
    public void jacksonSerialization(Blackhole blackhole) {
        int index = random.nextInt(BATCH);
        jsonMapper.writeValue(byteArrayOutputStream, simpleEntities[index]);
        blackhole.consume(byteArrayOutputStream.size());
        byteArrayOutputStream.reset();
    }

    @Benchmark
    public void jingDefaultSerialization(Blackhole blackhole) {
        int index = random.nextInt(BATCH);
        jsonDefaultSerializer.serializeMarshallableObject(simpleEntities[index], writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }
}
