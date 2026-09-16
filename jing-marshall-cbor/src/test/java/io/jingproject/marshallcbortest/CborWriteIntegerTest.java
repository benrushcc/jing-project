package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborNumberUtil;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CborWriteIntegerTest {
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

    // write a head (initial byte + argument payload) into a fresh buffer
    private static byte[] headBytes(int majorType, long argument) {
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        CborNumberUtil.writeHead(majorType, argument, writeBuffer);
        return writeBuffer.toByteArray();
    }

    // negative integers encode the argument as -1 - value
    private static byte[] negativeHeadBytes(long value) {
        return headBytes(CborNumberUtil.TYPE_NEGATIVE, -1L - value);
    }

    private static void assertUnsignedHead(String expected, long value) {
        Assertions.assertArrayEquals(hex(expected), headBytes(CborNumberUtil.TYPE_UNSIGNED, value));
    }

    private static void assertNegativeHead(String expected, long value) {
        Assertions.assertArrayEquals(hex(expected), negativeHeadBytes(value));
    }

    @Test
    public void testWriteNonNegativeSingleValues() {
        assertUnsignedHead("00", 0L);
        assertUnsignedHead("17", 23L);
        assertUnsignedHead("1818", 24L);
        assertUnsignedHead("18ff", 255L);
        assertUnsignedHead("190100", 256L);
        assertUnsignedHead("19ffff", 65535L);
        assertUnsignedHead("1a00010000", 65536L);
        assertUnsignedHead("1affffffff", 4294967295L);
        assertUnsignedHead("1b0000000100000000", 4294967296L);
        assertUnsignedHead("1b7fffffffffffffff", Long.MAX_VALUE);
    }

    @Test
    public void testWriteNegativeSingleValues() {
        assertNegativeHead("20", -1L);
        assertNegativeHead("37", -24L);
        assertNegativeHead("3818", -25L);
        assertNegativeHead("38fe", -255L);
        assertNegativeHead("38ff", -256L);
        assertNegativeHead("39ffff", -65536L);
        assertNegativeHead("3a00010000", -65537L);
        assertNegativeHead("3affffffff", -4294967296L);
        assertNegativeHead("3b0000000100000000", -4294967297L);
        assertNegativeHead("3b7fffffffffffffff", Long.MIN_VALUE);
    }

    @Test
    public void testSerializeIntArrayBoundary() {
        int[] values = {0, 23, 24, 255, 256, 65535, 65536, Integer.MAX_VALUE, -1, -24, -25, Integer.MIN_VALUE};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeIntArray(values, writeBuffer);
        Assertions.assertArrayEquals(hex(
                "8c0017181818ff19010019ffff1a000100001a7fffffff203738183a7fffffff"),
                writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeIntArray(new int[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeLongArrayBoundary() {
        long[] values = {0L, 24L, 256L, 65536L, 4294967296L, Long.MAX_VALUE, -1L, -25L, -256L, -65536L,
                Long.MIN_VALUE};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeLongArray(values, writeBuffer);
        Assertions.assertArrayEquals(hex(
                "8b0018181901001a000100001b0000000100000000" +
                "1b7fffffffffffffff20381838ff39ffff3b7fffffffffffffff"),
                writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeLongArray(new long[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());
    }

    @Test
    public void testSerializeShortArrayBoundary() {
        short[] values = {0, 255, 256, Short.MAX_VALUE, -1, -256, Short.MIN_VALUE};
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeShortArray(values, writeBuffer);
        Assertions.assertArrayEquals(hex("870018ff190100197fff2038ff397fff"), writeBuffer.toByteArray());

        writeBuffer = new HeapWriteBuffer(SIZE);
        SERIALIZER.serializeShortArray(new short[0], writeBuffer);
        Assertions.assertArrayEquals(hex("80"), writeBuffer.toByteArray());
    }
}