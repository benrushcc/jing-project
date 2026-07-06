package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.ReadBuffer;
import io.jingproject.marshalljson.JsonDeserializer;
import io.jingproject.marshalljson.JsonDeserializerOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Tag("view-output")
public class JsonDeserializationTest {
    private static final JsonDeserializer JSON_DESERIALIZER = new JsonDeserializer(JsonDeserializerOption.defaultOption());

    @Test
    public void testDeserializeByteArray() {
        String s = "[1,2,3,4,5]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = JSON_DESERIALIZER.deserializeByteArray(readBuffer);
        System.out.println(Arrays.toString(bytes));
    }

    @Test
    public void testDeserializeBooleanArray() {
        String s = "[true, false, true, false, true]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        boolean[] booleans = JSON_DESERIALIZER.deserializeBooleanArray(readBuffer);
        System.out.println(Arrays.toString(booleans));
    }

    @Test
    public void testDeserializeShortArray() {
        String s = "[123,234,345,456,567]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        short[] shorts = JSON_DESERIALIZER.deserializeShortArray(readBuffer);
        System.out.println(Arrays.toString(shorts));
    }

    @Test
    public void testDeserializeCharArray() {
        String s = "[\"a\",\"b\",\"c\",\"d\",\"e\"]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        char[] chars = JSON_DESERIALIZER.deserializeCharArray(readBuffer);
        System.out.println(Arrays.toString(chars));
    }

    @Test
    public void testDeserializeIntArray() {
        String s = "[123,234,345,456,567]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        int[] ints = JSON_DESERIALIZER.deserializeIntArray(readBuffer);
        System.out.println(Arrays.toString(ints));
    }

    @Test
    public void testDeserializeLongArray() {
        String s = "[123,234,345,456,567]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        long[] longs = JSON_DESERIALIZER.deserializeLongArray(readBuffer);
        System.out.println(Arrays.toString(longs));
    }

    @Test
    public void testDeserializeFloatArray() {
        String s = "[1.23,2.34,3.45,4.56,5.67]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        float[] floats = JSON_DESERIALIZER.deserializeFloatArray(readBuffer);
        System.out.println(Arrays.toString(floats));
    }

    @Test
    public void testDeserializeDoubleArray() {
        String s = "[1.23,2.34,3.45,4.56,5.67]";
        ReadBuffer readBuffer = new HeapReadBuffer(s.getBytes(StandardCharsets.UTF_8));
        double[] doubles = JSON_DESERIALIZER.deserializeDoubleArray(readBuffer);
        System.out.println(Arrays.toString(doubles));
    }
}
