package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import io.jingproject.marshalljsontest.UtfUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 3000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 2, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=ser-sarr-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions=stackdepth=128"
//})
@Fork(1)
public class StringArraySerializationBench {
    private static final int BATCH = 1000;
    private static final int SIZE = 64;
    private static final int BUFFER_SIZE = SIZE * BATCH * 8;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
    private final WriteBuffer writeBuffer = new HeapWriteBuffer(BUFFER_SIZE);
    @Param({"empty", "ascii", "utf", "surr", "mostAscii"})
    @SuppressWarnings("unused")
    private String type;
    private String[] arr;

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(StringArraySerializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }

    @Setup(Level.Iteration)
    public void setup() {
        arr = new String[BATCH];
        for (int i = 0; i < BATCH; i++) {
            arr[i] = UtfUtil.randTypedString(type, SIZE);
        }
    }

    @Benchmark
    public void jackson(Blackhole blackhole) {
        jsonMapper.writeValue(byteArrayOutputStream, arr);
        blackhole.consume(byteArrayOutputStream.size());
        byteArrayOutputStream.reset();
    }

    @Benchmark
    public void jing(Blackhole blackhole) {
        jsonDefaultSerializer.serializeArray(arr, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }
}
