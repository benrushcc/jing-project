package io.jingproject.bench.marshallcbor;

import io.jingproject.bench.AbstractBench;
import io.jingproject.bench.util.UtfUtil;
import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerOption;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class StringArrayDeserializationBench extends AbstractBench {
    private static final int BATCH = 1000;
    private final CBORMapper jacksonCborMapper = CBORMapper.builder().build();
    private final CborDeserializer cborDeserializer = new CborDeserializer(CborDeserializerOption.defaultOption());
    private ByteArrayOutputStream outputStream;
    private byte[] cborBytes;
    private HeapReadBuffer readBuffer;

    @Param({"8", "16", "32", "64"})
    @SuppressWarnings("unused")
    private int size;
    @Param({"empty", "ascii", "utf", "surr", "mostAscii"})
    @SuppressWarnings("unused")
    private String type;
    private String[] arr;

    @Setup(Level.Iteration)
    public void setup() {
        arr = new String[BATCH];
        for (int i = 0; i < BATCH; i++) {
            arr[i] = UtfUtil.randTypedString(type, size);
        }
        outputStream = new ByteArrayOutputStream(BATCH * size * 4);
        jacksonCborMapper.writeValue(outputStream, arr);
        cborBytes = outputStream.toByteArray();
        outputStream.reset();
        readBuffer = new HeapReadBuffer(cborBytes);
        String[] jacksonArray = jacksonCborMapper.readValue(cborBytes, String[].class);
        String[] cborArray = cborDeserializer.deserializeArray(String.class, new HeapReadBuffer(cborBytes));
        if (!Arrays.equals(arr, jacksonArray) || !Arrays.equals(arr, cborArray)) {
            throw new AssertionError("string array round trip mismatch");
        }
    }

    @Benchmark
    public void jacksonCbor(Blackhole blackhole) {
        String[] actual = jacksonCborMapper.readValue(cborBytes, String[].class);
        blackhole.consume(actual);
    }

    @Benchmark
    public void jingCbor(Blackhole blackhole) {
        readBuffer.reset();
        String[] actual = cborDeserializer.deserializeArray(String.class, readBuffer);
        blackhole.consume(actual);
    }
}
