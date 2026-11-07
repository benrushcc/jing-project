package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.util.UtfUtil;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.WriteBuffer;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.io.ByteArrayOutputStream;

public class StringArraySerializationBench extends AbstractBench {
    private static final int BATCH = 1000;
    private final CBORMapper jacksonCborMapper = CBORMapper.builder().build();
    private final CborSerializer cborSerializer = new CborSerializer(CborSerializerOption.defaultOption());
    private ByteArrayOutputStream outputStream;
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
        outputStream = new ByteArrayOutputStream(BATCH * size * 4);
        writeBuffer = new HeapWriteBuffer(BATCH * size * 4);
        arr = new String[BATCH];
        for (int i = 0; i < BATCH; i++) {
            arr[i] = UtfUtil.randTypedString(type, size);
        }
    }

    @Benchmark
    public void jacksonCbor(Blackhole blackhole) {
        jacksonCborMapper.writeValue(outputStream, arr);
        blackhole.consume(outputStream.size());
        outputStream.reset();
    }

    @Benchmark
    public void jingCbor(Blackhole blackhole) {
        cborSerializer.serializeArray(arr, writeBuffer);
        blackhole.consume(writeBuffer.intPosition());
        writeBuffer.reset();
    }
}
