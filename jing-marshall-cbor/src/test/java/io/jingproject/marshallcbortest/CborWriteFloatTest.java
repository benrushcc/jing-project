package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// write-side float encoding tests: floats must always use 0xfa + 4 big-endian
// bytes and doubles must always use 0xfb + 8 big-endian bytes, even when the
// value would fit into half precision. half encoding (0xf9) only appears on
// the deserialize path of the cbor reader.
public class CborWriteFloatTest {
    private static final CborSerializer SERIALIZER = new CborSerializer(CborSerializerOption.defaultOption());
    private static final int SIZE = 1024;

    private static byte[] hex(String expected) {
        int length = expected.length() / 2;
        byte[] r = new byte[length];
        for (int i = 0; i < length; i++) {
            int hi = Character.digit(expected.charAt(i * 2), 16);
            int lo = Character.digit(expected.charAt(i * 2 + 1), 16);
            r[i] = (byte) ((hi << 4) | lo);
        }
        return r;
    }

    @Test
    public void testSerializeFloatArray() {
        float[] arr = {1.0f, -1.0f, 0.0f, -0.0f};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeFloatArray(arr, writeBuffer);
        Assertions.assertArrayEquals(
                hex("84fa3f800000fabf800000fa00000000fa80000000"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeFloatSpecialValues() {
        float[] arr = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeFloatArray(arr, writeBuffer);
        Assertions.assertArrayEquals(
                hex("83fa7fc00000fa7f800000faff800000"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeFloatArrayBigEndian() {
        float[] arr = {1.0f, 0.5f, -1.5f, 100.0f};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeFloatArray(arr, writeBuffer);
        Assertions.assertArrayEquals(
                hex("84fa3f800000fa3f000000fabfc00000fa42c80000"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeFloatArrayUsesFloat32() {
        // half-representable values must still be written as 0xfa, never 0xf9
        float[] arr = {1.0f, 1.5f, -1.0f};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeFloatArray(arr, writeBuffer);
        Assertions.assertArrayEquals(
                hex("83fa3f800000fa3fc00000fabf800000"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeDoubleArray() {
        double[] arr = {1.0, -0.0};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeDoubleArray(arr, writeBuffer);
        Assertions.assertArrayEquals(
                hex("82fb3ff0000000000000fb8000000000000000"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeDoubleSpecialValues() {
        double[] arr = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeDoubleArray(arr, writeBuffer);
        Assertions.assertArrayEquals(
                hex("83fb7ff8000000000000fb7ff0000000000000fbfff0000000000000"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeDoubleArrayBigEndian() {
        double[] arr = {1.5, -2.25, 100.0};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeDoubleArray(arr, writeBuffer);
        Assertions.assertArrayEquals(
                hex("83fb3ff8000000000000fbc002000000000000fb4059000000000000"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeDoubleArrayUsesFloat64() {
        // half-representable doubles must still be written as 0xfb, never 0xf9
        double[] arr = {1.5, 1.0};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeDoubleArray(arr, writeBuffer);
        Assertions.assertArrayEquals(
                hex("82fb3ff8000000000000fb3ff0000000000000"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeEmptyFloatArray() {
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeFloatArray(new float[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeEmptyDoubleArray() {
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeDoubleArray(new double[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());
    }
}