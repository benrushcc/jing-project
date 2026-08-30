package io.jingproject.marshalljsontest.bench;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.ReadBuffer;
import io.jingproject.marshalljson.JsonDeserializer;
import io.jingproject.marshalljson.JsonDeserializerOption;
import io.jingproject.marshalljsontest.TwiUtil;
import io.jingproject.marshalljsontest.twi.Twi;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(value = Mode.AverageTime)
@Warmup(iterations = 3, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=ser-twi-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions:stackdepth=128"
//})
@Fork(3)
public class TwiDeserializationBench {
    private byte[] bytes;
    private ReadBuffer readBuffer;
    private JsonMapper jsonMapper;
    private JsonDeserializer jsonDefaultDeserializer;

    @Setup(Level.Trial)
    public void setup() {
        bytes = TwiUtil.loadAsBytes();
        readBuffer = new HeapReadBuffer(bytes);
        jsonMapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
        jsonDefaultDeserializer = new JsonDeserializer(JsonDeserializerOption.defaultOption());
    }

    @Benchmark
    public void jacksonDeserialization(Blackhole blackhole) {
        Twi twi = jsonMapper.readValue(bytes, Twi.class);
        blackhole.consume(twi);
    }

    @Benchmark
    public void jingDefaultDeserialization(Blackhole blackhole) {
        Twi twi = jsonDefaultDeserializer.deserializeMarshallableObject(Twi.class, readBuffer);
        readBuffer.reset();
        blackhole.consume(twi);
    }

    static void main() throws RunnerException {
        Options opt = new OptionsBuilder().include(TwiDeserializationBench.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }
}
