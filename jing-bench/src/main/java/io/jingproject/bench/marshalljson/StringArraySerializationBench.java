package io.jingproject.bench.marshalljson;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.util.UtfUtil;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshalljson.JsonIndentationLevel;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;

public class StringArraySerializationBench extends AbstractBench {
    private static final int BATCH = 1000;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final JsonSerializer jsonDefaultSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
    private final JsonSerializer jsonIndentSerializer = new JsonSerializer(JsonSerializerOption.builder().setIndentationLevel(JsonIndentationLevel.FOUR).build());
    private ByteArrayOutputStream byteArrayOutputStream;
    private WriteBuffer writeBuffer;

    @Param({"8", "16", "32", "64"})
    @SuppressWarnings("unused")
    private int size;
    @Param({"empty", "ascii", "utf", "surr", "mostAscii"})
    @SuppressWarnings("unused")
    private String type;
    private String[] arr;

    @Setup(Level.Iteration)
    public void setup() {
        byteArrayOutputStream = new ByteArrayOutputStream(BATCH * size * 2);
        writeBuffer = new HeapWriteBuffer(BATCH * size * 2);
        arr = new String[BATCH];
        for (int i = 0; i < BATCH; i++) {
            arr[i] = UtfUtil.randTypedString(type, size);
        }
    }

    @Benchmark
    public void jackson(Blackhole blackhole) {
        jsonMapper.writeValue(byteArrayOutputStream, arr);
        blackhole.consume(byteArrayOutputStream.size());
        byteArrayOutputStream.reset();
    }

    @Benchmark
    public void jingDefault(Blackhole blackhole) {
        jsonDefaultSerializer.serializeArray(arr, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }

    @Benchmark
    public void jingIndent(Blackhole blackhole) {
        jsonIndentSerializer.serializeArray(arr, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.setPosition(0);
    }
}
