package io.jingproject.bench.marshalljson;

import io.helidon.json.binding.JsonBinding;
import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.twi.Twi;
import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.ReadBuffer;
import io.jingproject.marshalljson.JsonDeserializer;
import io.jingproject.marshalljson.JsonDeserializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;


@Warmup(iterations = 3, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
//@Fork(value = 1, jvmArgsAppend = {
//        "-XX:StartFlightRecording=disk=true,dumponexit=true,filename=de-twi-%p-%t.jfr,settings=profile",
//        "-XX:FlightRecorderOptions:stackdepth=128"
//})
public class TwiDeserializationBench extends AbstractBench {
    private final byte[] bytes = Twi.asBytes();
    private final ReadBuffer readBuffer = new HeapReadBuffer(bytes);
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonDeserializer jsonDefaultDeserializer = new JsonDeserializer(JsonDeserializerOption.defaultOption());
    private final JsonBinding helidonJsonBinding = JsonBinding.create();

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

    @Benchmark
    public void helidonDeserialization(Blackhole blackhole) {
        Twi twi = helidonJsonBinding.deserialize(bytes, Twi.class);
        blackhole.consume(twi);
    }
}
