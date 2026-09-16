package io.jingproject.marshallcbortest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.marshallcbor.CborDeserializer;
import io.jingproject.marshallcbor.CborDeserializerException;
import io.jingproject.marshallcbor.CborDeserializerOption;
import io.jingproject.marshallcbor.CborNumberUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// read-side float/double tests: half (0xf9), float32 (0xfa) and float64 (0xfb)
// array elements are widened by the deserializer. bit-level assertions use
// floatToRawIntBits/doubleToRawLongBits so nan payloads and ±0 survive.
public class CborReadFloatTest {
    private static final CborDeserializer DESERIALIZER = new CborDeserializer(CborDeserializerOption.defaultOption());

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

    private static void assertFloatBits(float[] expected, float[] actual) {
        Assertions.assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            Assertions.assertEquals(Float.floatToRawIntBits(expected[i]), Float.floatToRawIntBits(actual[i]));
        }
    }

    private static void assertDoubleBits(double[] expected, double[] actual) {
        Assertions.assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            Assertions.assertEquals(Double.doubleToRawLongBits(expected[i]), Double.doubleToRawLongBits(actual[i]));
        }
    }

    @Test
    public void testReadFloatArrayFromHalf() {
        float[] actual = DESERIALIZER.deserializeFloatArray(new HeapReadBuffer(hex("84f93c00f93e00f9bc00f98000")));
        assertFloatBits(new float[]{1.0f, 1.5f, -1.0f, -0.0f}, actual);
    }

    @Test
    public void testReadFloatArrayFromFloat32() {
        float[] actual = DESERIALIZER.deserializeFloatArray(
                new HeapReadBuffer(hex("83fa3f800000fa00000000fa80000000")));
        assertFloatBits(new float[]{1.0f, 0.0f, -0.0f}, actual);
    }

    @Test
    public void testReadFloatArrayFromFloat64() {
        float[] actual = DESERIALIZER.deserializeFloatArray(new HeapReadBuffer(
                hex("85fb3ff0000000000000fb3ff8000000000000fbbff0000000000000fb8000000000000000fb0000000000000000")));
        assertFloatBits(new float[]{1.0f, 1.5f, -1.0f, -0.0f, 0.0f}, actual);
    }

    @Test
    public void testReadFloatArrayNaN() {
        // nan bit patterns must survive the read path unchanged
        float[] actual = DESERIALIZER.deserializeFloatArray(new HeapReadBuffer(hex("83f97e00fa7fa00001f97e01")));
        assertFloatBits(new float[]{Float.intBitsToFloat(0x7FC00000), Float.intBitsToFloat(0x7FA00001),
                Float.intBitsToFloat(0x7FC02000)}, actual);
    }

    @Test
    public void testReadFloatArrayEmpty() {
        float[] actual = DESERIALIZER.deserializeFloatArray(new HeapReadBuffer(hex("80")));
        assertFloatBits(new float[0], actual);
    }

    @Test
    public void testReadDoubleArray() {
        double[] actual = DESERIALIZER.deserializeDoubleArray(
                new HeapReadBuffer(hex("85f93e00f9bc00fa3f800000fb3ff0000000000000fbbff0000000000000")));
        assertDoubleBits(new double[]{1.5, -1.0, 1.0, 1.0, -1.0}, actual);
    }

    @Test
    public void testReadDoubleArrayNaN() {
        double[] actual = DESERIALIZER.deserializeDoubleArray(
                new HeapReadBuffer(hex("81fb7ff8000000000001")));
        assertDoubleBits(new double[]{Double.longBitsToDouble(0x7FF8000000000001L)}, actual);
    }

    @Test
    public void testReadDoubleArrayEmpty() {
        double[] actual = DESERIALIZER.deserializeDoubleArray(new HeapReadBuffer(hex("80")));
        assertDoubleBits(new double[0], actual);
    }

    @Test
    public void testHalfToFloatKnownValues() {
        Assertions.assertEquals(0x3F800000, Float.floatToRawIntBits(CborNumberUtil.halfToFloat((short) 0x3C00)));
        Assertions.assertEquals(0x3FC00000, Float.floatToRawIntBits(CborNumberUtil.halfToFloat((short) 0x3E00)));
        Assertions.assertEquals(0xBF800000, Float.floatToRawIntBits(CborNumberUtil.halfToFloat((short) 0xBC00)));
        Assertions.assertEquals(0x00000000, Float.floatToRawIntBits(CborNumberUtil.halfToFloat((short) 0x0000)));
        Assertions.assertEquals(0x80000000, Float.floatToRawIntBits(CborNumberUtil.halfToFloat((short) 0x8000)));
        Assertions.assertEquals(0x7F800000, Float.floatToRawIntBits(CborNumberUtil.halfToFloat((short) 0x7C00)));
        Assertions.assertEquals(0xFF800000, Float.floatToRawIntBits(CborNumberUtil.halfToFloat((short) 0xFC00)));
        Assertions.assertEquals(0x7FC00000, Float.floatToRawIntBits(CborNumberUtil.halfToFloat((short) 0x7E00)));
    }

    @Test
    public void testFloatToHalfKnownValues() {
        Assertions.assertEquals((short) 0x3C00, CborNumberUtil.floatToHalf(1.0f));
        Assertions.assertEquals((short) 0x3E00, CborNumberUtil.floatToHalf(1.5f));
        Assertions.assertEquals((short) 0xBC00, CborNumberUtil.floatToHalf(-1.0f));
        Assertions.assertEquals((short) 0x0000, CborNumberUtil.floatToHalf(0.0f));
        Assertions.assertEquals((short) 0x8000, CborNumberUtil.floatToHalf(-0.0f));
        Assertions.assertEquals((short) 0x7C00, CborNumberUtil.floatToHalf(Float.POSITIVE_INFINITY));
        Assertions.assertEquals((short) 0xFC00, CborNumberUtil.floatToHalf(Float.NEGATIVE_INFINITY));
        Assertions.assertEquals((short) 0x7E00, CborNumberUtil.floatToHalf(Float.NaN));
    }

    @Test
    public void testHalfRoundTripSpecialValues() {
        short[] patterns = {(short) 0x0000, (short) 0x8000, (short) 0x3C00, (short) 0xBC00, (short) 0x3E00,
                (short) 0x7C00, (short) 0xFC00, (short) 0x0001, (short) 0x03FF, (short) 0x7BFF, (short) 0x7E00};
        for (short pattern : patterns) {
            Assertions.assertEquals(pattern, CborNumberUtil.floatToHalf(CborNumberUtil.halfToFloat(pattern)));
        }
        // the chosen nan pattern must keep its payload bits across the round trip
        Assertions.assertEquals(0x7FC00000, Float.floatToRawIntBits(CborNumberUtil.halfToFloat((short) 0x7E00)));
    }

    @Test
    public void testReadFloatOverflow() {
        // float64 values too large for float32 must be rejected by the strict path
        CborDeserializerException ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeFloatArray(new HeapReadBuffer(hex("81fb7fefffffffffffff"))));
        Assertions.assertEquals("float value overflow : 1.7976931348623157E308", ex.getMessage());

        ex = Assertions.assertThrows(CborDeserializerException.class,
                () -> DESERIALIZER.deserializeFloatArray(new HeapReadBuffer(hex("81fb47f0000000000000"))));
        Assertions.assertEquals("float value overflow : 3.402823669209385E38", ex.getMessage());
    }

    @Test
    public void testReadFloatNearMaxNoOverflow() {
        // exact float max as float64 stays finite and must not throw
        float[] actual = DESERIALIZER.deserializeFloatArray(
                new HeapReadBuffer(hex("81fb47efffffe0000000")));
        Assertions.assertEquals(0x7F7FFFFF, Float.floatToRawIntBits(actual[0]));
    }
}