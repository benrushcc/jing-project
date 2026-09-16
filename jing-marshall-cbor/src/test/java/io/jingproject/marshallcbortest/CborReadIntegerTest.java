package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerException;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbor.CborSerializer;
import io.jingproject.marshallcbor.CborSerializerOption;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CborReadIntegerTest {
    private static final CborSerializer SERIALIZER = new CborSerializer(CborSerializerOption.defaultOption());
    private static final CborDeserializer DESERIALIZER = new CborDeserializer(CborDeserializerOption.defaultOption());
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
    public void testDeserializeIntArrayBoundary() {
        int[] array = DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex(
                "900017181818ff19010019ffff1a000100001a7fffffff" +
                "2037381838fe38ff39ffff3a000100003a7fffffff")));
        Assertions.assertArrayEquals(
                new int[]{0, 23, 24, 255, 256, 65535, 65536, Integer.MAX_VALUE,
                        -1, -24, -25, -255, -256, -65536, -65537, Integer.MIN_VALUE},
                array);

        array = DESERIALIZER.deserializeIntArray(new HeapReadBuffer(hex("80")));
        Assertions.assertArrayEquals(new int[0], array);
    }

    @Test
    public void testDeserializeLongArrayBoundary() {
        long[] array = DESERIALIZER.deserializeLongArray(new HeapReadBuffer(hex(
                "940017181818ff19010019ffff1a000100001affffffff" +
                "1b00000001000000001b7fffffffffffffff" +
                "2037381838fe38ff39ffff" +
                "3a000100003affffffff" +
                "3b00000001000000003b7fffffffffffffff")));
        Assertions.assertArrayEquals(
                new long[]{0L, 23L, 24L, 255L, 256L, 65535L, 65536L, 4294967295L,
                        4294967296L, Long.MAX_VALUE, -1L, -24L, -25L, -255L, -256L,
                        -65536L, -65537L, -4294967296L, -4294967297L, Long.MIN_VALUE},
                array);

        array = DESERIALIZER.deserializeLongArray(new HeapReadBuffer(hex("80")));
        Assertions.assertArrayEquals(new long[0], array);
    }

    @Test
    public void testRoundTripSingleIntValue() {
        int[] values = {0, 1, -1, 23, -24, 255, -255, 256, 65536, -65537, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int value : values) {
            int[] expected = {value};
            HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
            SERIALIZER.serializeIntArray(expected, writeBuffer);
            int[] actual = DESERIALIZER.deserializeIntArray(new HeapReadBuffer(writeBuffer.toByteArray()));
            Assertions.assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testRoundTripSingleLongValue() {
        long[] values = {0L, 1L, -1L, 24L, 256L, 65536L,
                -65536L, 4294967296L, -4294967297L, Long.MAX_VALUE, Long.MIN_VALUE};
        for (long value : values) {
            long[] expected = {value};
            HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
            SERIALIZER.serializeLongArray(expected, writeBuffer);
            long[] actual = DESERIALIZER.deserializeLongArray(new HeapReadBuffer(writeBuffer.toByteArray()));
            Assertions.assertArrayEquals(expected, actual);
        }
    }

    // ai 28-31 inside array elements: head byte 0x81, then illegal element byte
    @Test
    public void testDeserializeIllegalElementAi() {
        byte[] illegalElements = {(byte) 0x1C, (byte) 0x1D, (byte) 0x1E, (byte) 0x1F,
                (byte) 0x3C, (byte) 0x3D, (byte) 0x3E, (byte) 0x3F};
        for (byte element : illegalElements) {
            byte[] input = {(byte) 0x81, element};
            CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                    () -> DESERIALIZER.deserializeIntArray(new HeapReadBuffer(input)));
            Assertions.assertEquals("unsupported or illegal initial byte", ex.getMessage());

            ex = Assertions.assertThrows(CborDeserializerException.class,
                    () -> DESERIALIZER.deserializeLongArray(new HeapReadBuffer(input)));
            Assertions.assertEquals("unsupported or illegal initial byte", ex.getMessage());
        }
    }

    // ai 28-30 as an array length head (major 4): rejected by readArgument
    @Test
    public void testDeserializeIllegalLengthAi() {
        byte[] illegalHeads = {(byte) 0x9C, (byte) 0x9D, (byte) 0x9E};
        for (byte head : illegalHeads) {
            byte[] input = {head};
            CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                    () -> DESERIALIZER.deserializeIntArray(new HeapReadBuffer(input)));
            Assertions.assertEquals("unsupported or illegal initial byte", ex.getMessage());
        }
    }
}