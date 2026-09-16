package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshallcbor.CborDeserializerException;
import io.jingproject.marshallcbor.CborNumberUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

// RFC 8949 section 3.1 preferred serialization vectors: every head must use the
// shortest argument representation and ai 28-31 must be rejected.
public class CborNumberUtilRfc8949Test {
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

    // assert the encoded head bytes, then rebuild the argument payload (all
    // bytes after the initial byte) as a heap read buffer and verify that
    // readArgument restores the exact argument for the given ai
    private static void verifyVector(int majorType, long argument, String expectedHex) {
        byte[] expected = hex(expectedHex);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
        CborNumberUtil.writeHead(majorType, argument, writeBuffer);
        Assertions.assertArrayEquals(expected, writeBuffer.toByteArray());
        byte[] payload = Arrays.copyOfRange(expected, 1, expected.length);
        long roundTripped = CborNumberUtil.readArgument(
                CborNumberUtil.aiOf(expected[0]), new HeapReadBuffer(payload));
        Assertions.assertEquals(argument, roundTripped);
    }

    @Test
    public void testWriteHeadUnsignedIntegerVectors() {
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 0L, "00");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 1L, "01");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 10L, "0a");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 23L, "17");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 24L, "1818");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 100L, "1864");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 255L, "18ff");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 1000L, "1903e8");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 65535L, "19ffff");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 65536L, "1a00010000");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 1000000L, "1a000f4240");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 4294967295L, "1affffffff");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 4294967296L, "1b0000000100000000");
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, 9007199254740992L, "1b0020000000000000");
        // the unsigned 64-bit value 2^64 - 1
        verifyVector(CborNumberUtil.TYPE_UNSIGNED, -1L, "1bffffffffffffffff");
    }

    @Test
    public void testWriteHeadNegativeIntegerVectors() {
        // the major 1 argument is -1 - value, so argument 0 encodes value -1
        verifyVector(CborNumberUtil.TYPE_NEGATIVE, 0L, "20");
        verifyVector(CborNumberUtil.TYPE_NEGATIVE, 9L, "29");
        verifyVector(CborNumberUtil.TYPE_NEGATIVE, 99L, "3863");
        verifyVector(CborNumberUtil.TYPE_NEGATIVE, 999L, "3903e7");
        // value -2^32 encodes as argument 2^32 - 1
        verifyVector(CborNumberUtil.TYPE_NEGATIVE, 4294967295L, "3affffffff");
        // value -(2^32 + 1) encodes as argument 2^32
        verifyVector(CborNumberUtil.TYPE_NEGATIVE, 4294967296L, "3b0000000100000000");
        // value Long.MIN_VALUE encodes as argument Long.MAX_VALUE
        verifyVector(CborNumberUtil.TYPE_NEGATIVE, Long.MAX_VALUE, "3b7fffffffffffffff");
    }

    @Test
    public void testWriteHeadLengthVectors() {
        verifyVector(CborNumberUtil.TYPE_TEXT, 0L, "60");
        verifyVector(CborNumberUtil.TYPE_TEXT, 24L, "7818");
        verifyVector(CborNumberUtil.TYPE_TEXT, 100L, "7864");
        verifyVector(CborNumberUtil.TYPE_BYTES, 24L, "5818");
        verifyVector(CborNumberUtil.TYPE_ARRAY, 0L, "80");
        verifyVector(CborNumberUtil.TYPE_ARRAY, 24L, "9818");
        verifyVector(CborNumberUtil.TYPE_MAP, 0L, "a0");
        verifyVector(CborNumberUtil.TYPE_MAP, 24L, "b818");
    }

    @Test
    public void testReadArgumentRoundTripBoundaries() {
        long[] boundaries = {0L, 23L, 24L, 255L, 256L, 65535L, 65536L, 4294967295L,
                4294967296L, 9007199254740992L, Long.MAX_VALUE, -1L};
        for (int majorType = 0; majorType <= 1; majorType++) {
            for (long argument : boundaries) {
                HeapWriteBuffer writeBuffer = new HeapWriteBuffer(SIZE);
                CborNumberUtil.writeHead(majorType, argument, writeBuffer);
                byte[] encoded = writeBuffer.toByteArray();
                long roundTripped = CborNumberUtil.readArgument(
                        CborNumberUtil.aiOf(encoded[0]),
                        new HeapReadBuffer(Arrays.copyOfRange(encoded, 1, encoded.length)));
                Assertions.assertEquals(argument, roundTripped);
            }
        }
    }

    @Test
    public void testReadArgumentRejectsIllegalAi() {
        // the ai 28-30 forms are reserved and ai 31 signifies indefinite length;
        // none of them may be used as the argument of an integer head
        byte[] payload = {0, 0, 0, 0, 0, 0, 0, 0};
        for (int ai = 28; ai <= 31; ai++) {
            int currentAi = ai;
            CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                    () -> CborNumberUtil.readArgument(currentAi, new HeapReadBuffer(payload)));
            Assertions.assertEquals("unsupported or illegal initial byte", ex.getMessage());
        }
    }
}